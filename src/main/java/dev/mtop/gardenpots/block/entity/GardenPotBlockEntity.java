package dev.mtop.gardenpots.block.entity;

import dev.mtop.gardenpots.GardenPots;
import dev.mtop.gardenpots.block.GardenPotHelpers;
import dev.mtop.gardenpots.block.context.CellContext;
import dev.mtop.gardenpots.compat.powerpots.PowerPotCellSupport;
import dev.mtop.gardenpots.energy.GardenEnergyStorage;
import dev.mtop.gardenpots.init.ModBlockEntities;
import dev.mtop.gardenpots.upgrade.GardenUpgradeSupport;
import net.darkhax.bookshelf.common.api.data.enchantment.EnchantmentLevel;
import net.darkhax.bookshelf.common.api.service.Services;
import net.darkhax.bookshelf.common.api.util.TickAccumulator;
import net.darkhax.botanypots.common.impl.BotanyPotsMod;
import net.darkhax.botanypots.common.impl.Helpers;
import net.darkhax.botanypots.common.impl.block.PotType;
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop;
import net.darkhax.botanypots.common.api.data.recipes.soil.Soil;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Owns the 3x3 {@link GardenPotCell} grid. {@link #tick} reimplements
 * {@code BotanyPotBlockEntity.tickPot}'s body per cell - see {@link GardenPotCell} and
 * {@link CellContext} for why that method can't just be called directly nine times.
 *
 * <p>Implements {@link Container} as a flat view over the grid so {@link dev.mtop.gardenpots.block.menu.GardenPotMenu}
 * can use plain {@code Slot}s: slots {@code 0..35} are the 9 cells' (potKind, soil, seed, tool)
 * quads, slots {@code 36..62} are the shared output bank.
 */
public class GardenPotBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int GRID_SIZE = 9;
    public static final int CLUSTER_SLOTS = 4;
    public static final int CELL_SLOTS = GRID_SIZE * CLUSTER_SLOTS;
    /** One shared harvest bank for every hopper-kind cell, instead of 9 separate 12-slot bins. */
    public static final int OUTPUT_SLOTS = 27;
    public static final int[] OUTPUT_SLOT_RANGE = IntStream.range(0, OUTPUT_SLOTS).toArray();
    /** 4 shared slots (Speed/Output/Energy/Fortune) applying to every cell at once, any pot kind -
     * see {@code dev.mtop.gardenpots.upgrade.GardenUpgradeSupport}, which is where the
     * slot-index-to-upgrade-type mapping lives. Energy only has any effect on Power-Pot-kind cells
     * (there's nothing else to reduce the draw of), but the other three apply universally. */
    public static final int UPGRADE_SLOTS = 4;
    public static final int CONTAINER_SIZE = CELL_SLOTS + OUTPUT_SLOTS + UPGRADE_SLOTS;
    public static final Component DEFAULT_NAME = Component.translatable("container.gardenpots.garden_pot");
    /** Ticks between export attempts once there's something to move - matches vanilla hopper's own cadence. */
    public static final float EXPORT_PERIOD = 8f;
    /** How often the client's copy of {@link #energy} gets refreshed. A cable's {@code receiveEnergy}
     * call only ever touches the server-side capability object directly - it never calls
     * {@link #markUpdated()}, so without a periodic nudge the client's display (GUI, Jade) just sits
     * frozen at whatever it last happened to sync to (0, if nothing else triggered a sync since the
     * block was placed) no matter how charged the real server-side buffer gets. */
    public static final float ENERGY_SYNC_PERIOD = 10f;

    // 9 cells' worth of potential Power Pots sharing one buffer - generous enough that a full grid of
    // top-tier Power Pots doesn't starve, cheap to raise later via config if that turns out wrong.
    public static final int ENERGY_CAPACITY = 100_000;
    public static final int ENERGY_MAX_RECEIVE = 2_000;

    private final GardenPotCell[] cells = new GardenPotCell[GRID_SIZE];
    // One reused context per cell instead of allocating a fresh one on every tick/render call - with
    // 10 gardens ticking at once that's 90 fewer short-lived allocations every game tick. Every call
    // site passes (null, null) for player/hand today (no interaction routes through here yet), so
    // reuse is safe; a future feature needing a player-bound context should build its own instead of
    // pulling from this array.
    private final CellContext[] contexts = new CellContext[GRID_SIZE];
    public NonNullList<ItemStack> output = NonNullList.withSize(OUTPUT_SLOTS, ItemStack.EMPTY);
    /** Speed/Output/Energy/Fortune, in that fixed order - our own items, always placeable regardless
     * of what else is installed (see {@code GardenUpgradeSupport#isUpgradeForSlot}). */
    public NonNullList<ItemStack> upgrades = NonNullList.withSize(UPGRADE_SLOTS, ItemStack.EMPTY);
    public final GardenEnergyStorage energy = new GardenEnergyStorage(ENERGY_CAPACITY, ENERGY_MAX_RECEIVE);
    // Set whenever a harvest lands in `output`; cleared once it's fully drained. Lets tick() skip the
    // export scan entirely on the common tick where there's nothing to move.
    protected boolean hasOutput = false;
    private final TickAccumulator exportCooldown = new TickAccumulator(0f);
    private final TickAccumulator energySyncCooldown = new TickAccumulator(0f);
    private int lastSyncedEnergy = -1;

    public GardenPotBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, ModBlockEntities.GARDEN_POT.get());
    }

    /** For subclasses (see {@code compat.dimstorage.DimGardenPotBlockEntity}) registering their own {@link BlockEntityType}. */
    protected GardenPotBlockEntity(BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<?> type) {
        super(type, pos, state);
        for (int i = 0; i < GRID_SIZE; i++) {
            this.cells[i] = new GardenPotCell();
            this.contexts[i] = new CellContext(this, i, null, null);
        }
    }

    public GardenPotCell getCell(int index) {
        return this.cells[index];
    }

    /** Whether the shared output bank has room for at least one more item - used to hold a finished
     * harvest rather than generate loot only to discard whatever doesn't fit. */
    public boolean hasFreeOutputSlot() {
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            if (this.output.get(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public CellContext getContext(int index) {
        return this.contexts[index];
    }

    // -- Container: a flat view over the 9 cells' (potKind, soil, seed, tool) quads + shared output --

    private ItemStack getCellSlot(GardenPotCell cell, int which) {
        return switch (which) {
            case 0 -> cell.potKind;
            case 1 -> cell.soil;
            case 2 -> cell.seed;
            case 3 -> cell.tool;
            default -> ItemStack.EMPTY;
        };
    }

    private void setCellSlot(GardenPotCell cell, int which, ItemStack stack) {
        switch (which) {
            case 0 -> cell.potKind = stack;
            case 1 -> cell.soil = stack;
            case 2 -> cell.seed = stack;
            case 3 -> cell.tool = stack;
            default -> {
            }
        }
        // Changing what's planted (or what pot it's planted in) invalidates progress, exactly like
        // AbstractBotanyPotBlockEntity#setItem does for its SOIL/SEED slots.
        if (which != 3) {
            cell.reset();
        }
        this.markUpdated();
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            if (!this.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    @Override
    public ItemStack getItem(int slot) {
        if (slot < CELL_SLOTS) {
            return this.getCellSlot(this.cells[slot / CLUSTER_SLOTS], slot % CLUSTER_SLOTS);
        }
        final int outSlot = slot - CELL_SLOTS;
        if (outSlot < OUTPUT_SLOTS) {
            return this.output.get(outSlot);
        }
        final int upgradeSlot = outSlot - OUTPUT_SLOTS;
        return upgradeSlot < UPGRADE_SLOTS ? this.upgrades.get(upgradeSlot) : ItemStack.EMPTY;
    }

    @NotNull
    @Override
    public ItemStack removeItem(int slot, int amount) {
        final ItemStack stack = this.getItem(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ItemStack split = stack.split(amount);
        if (!split.isEmpty()) {
            this.setChanged();
        }
        return split;
    }

    @NotNull
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        final ItemStack stack = this.getItem(slot);
        this.setItem(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot < CELL_SLOTS) {
            this.setCellSlot(this.cells[slot / CLUSTER_SLOTS], slot % CLUSTER_SLOTS, stack);
            return;
        }
        final int outSlot = slot - CELL_SLOTS;
        if (outSlot < OUTPUT_SLOTS) {
            this.output.set(outSlot, stack);
            this.setChanged();
            return;
        }
        final int upgradeSlot = outSlot - OUTPUT_SLOTS;
        if (upgradeSlot < UPGRADE_SLOTS) {
            this.upgrades.set(upgradeSlot, stack);
            this.setChanged();
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            this.setItem(i, ItemStack.EMPTY);
        }
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return DEFAULT_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInv, @NotNull Player player) {
        return dev.mtop.gardenpots.block.menu.GardenPotMenu.serverMenu(containerId, playerInv, this);
    }

    public int getRequiredGrowthTicks(int index) {
        final CellContext context = this.getContext(index);
        final Crop crop = context.getCrop();
        if (crop == null || this.level == null) {
            return -1;
        }
        return GardenPotHelpers.getRequiredGrowthTicks(this.getCell(index), context, this.level, crop, context.getSoil(), this.upgrades);
    }

    /** 0-1 growth progress for a cell - shared by {@code GardenPotRenderer} (in-world crop scale) and
     * {@code GardenPotMenu}'s synced progress bars, so both read off the same number. */
    public float getGrowthProgress(int index) {
        final int maxGrowth = this.getRequiredGrowthTicks(index);
        if (maxGrowth <= 0) {
            return 0f;
        }
        final float ticks = this.getCell(index).growthTime.getTicks();
        return Mth.clamp(Mth.clamp(ticks, 0, maxGrowth) / (float) maxGrowth, 0f, 1f);
    }

    public int getMaxComparatorLevel() {
        int max = 0;
        for (GardenPotCell cell : this.cells) {
            max = Math.max(max, cell.comparatorLevel);
        }
        return max;
    }

    public void updateComparator(int index, int newLevel) {
        final GardenPotCell cell = this.cells[index];
        if (newLevel != cell.comparatorLevel && !this.isRemoved() && this.level instanceof ServerLevel) {
            final int oldMax = this.getMaxComparatorLevel();
            cell.comparatorLevel = newLevel;
            if (this.getMaxComparatorLevel() != oldMax) {
                ((ServerLevel) this.level).updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
            }
        }
    }

    public void markUpdated() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    /**
     * Pushes the shared output bank toward wherever it's supposed to go. The base block exports to
     * the inventory below, exactly like a real Hopper Pot. {@code compat.dimstorage}'s
     * DimGardenPotBlockEntity overrides this to redirect into a bound Dim Storage frequency instead.
     *
     * <p>Only called from {@link #tick} when {@link #hasOutput} is set and the cooldown's up (see
     * there) - a garden with nothing harvested yet costs nothing here, and one that does export pays
     * for the "below" blockstate lookup + a 27-slot scan once every {@value #EXPORT_PERIOD} ticks
     * (vanilla hopper cadence) instead of every single tick.
     */
    protected void exportOutput(ServerLevel level) {
        final BlockPos below = this.worldPosition.below();
        if (level.getBlockState(below).isAir()) {
            return;
        }
        boolean anyLeft = false;
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            final ItemStack stack = this.output.get(slot);
            if (!stack.isEmpty()) {
                final ItemStack result = Services.GAMEPLAY.inventoryInsert(level, below, Direction.UP, stack);
                this.output.set(slot, result);
                anyLeft |= !result.isEmpty();
            }
        }
        this.hasOutput = anyLeft;
    }

    public void runFunction(ResourceLocation functionId) {
        if (this.level instanceof ServerLevel serverLevel) {
            final Optional<CommandFunction<CommandSourceStack>> function = serverLevel.getServer().getFunctions().get(functionId);
            final CommandSourceStack sourceStack = this.createCommandSourceStack();
            if (sourceStack != null) {
                function.ifPresentOrElse(func -> serverLevel.getServer().getFunctions().execute(func, sourceStack), () -> BotanyPotsMod.LOG.error("Garden pot at {} tried to run missing function {}. This is an issue with your datapacks.", this.worldPosition, functionId));
            }
        }
    }

    @Nullable
    private CommandSourceStack createCommandSourceStack() {
        if (this.level instanceof ServerLevel serverLevel) {
            return new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(this.worldPosition), Vec2.ZERO, serverLevel, 2, DEFAULT_NAME.getString(), DEFAULT_NAME, serverLevel.getServer(), null);
        }
        return null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GardenPotBlockEntity be) {
        if (be.isRemoved()) {
            return;
        }
        for (int i = 0; i < GRID_SIZE; i++) {
            tickCell(be, i);
        }
        if (be.hasOutput && level instanceof ServerLevel serverLevel) {
            be.exportCooldown.tickDown(level);
            if (be.exportCooldown.getTicks() <= 0) {
                be.exportOutput(serverLevel);
                be.exportCooldown.setTicks(EXPORT_PERIOD);
            }
        }
        if (level instanceof ServerLevel) {
            be.energySyncCooldown.tickDown(level);
            if (be.energySyncCooldown.getTicks() <= 0) {
                be.energySyncCooldown.setTicks(ENERGY_SYNC_PERIOD);
                final int stored = be.energy.getEnergyStored();
                if (stored != be.lastSyncedEnergy) {
                    be.lastSyncedEnergy = stored;
                    be.markUpdated();
                }
            }
        }
    }

    private static void tickCell(GardenPotBlockEntity owner, int index) {
        final GardenPotCell cell = owner.getCell(index);
        final Level level = owner.getLevel();
        if (level == null || cell.isEmpty() || !cell.enabled) {
            return;
        }

        final PotType potType = cell.getPotType();
        if (potType == PotType.WAXED) {
            cell.growthTime.setTicks(Float.MAX_VALUE);
            return;
        }

        // Confirmed by decompiling the real PowerPotBlockEntity#tickPot: it checks level.isClientSide
        // and skips its own energy draw on the client, same as this. Missing that guard here meant the
        // client ran this same tickCell (registered with no isClientSide split - see
        // GardenPotBlock#getTicker) and drained its own *unsynced* copy of `owner.energy` every client
        // tick, all the way to near-0, far faster than the periodic resync from the real (healthy,
        // cable-fed) server value could correct it - so the GUI/Jade display spent most of its time
        // showing that self-inflicted near-empty number instead of the true one.
        if (!level.isClientSide && GardenPots.isPowerPotsLoaded() && PowerPotCellSupport.isPowerPot(cell.potKind) && !PowerPotCellSupport.tryConsumeEnergy(owner.energy, cell.potKind, owner.upgrades)) {
            // Not enough FE in the shared buffer this tick - mirrors PowerPotBlockEntity#tickPot's own
            // early return when its (real, per-block) energy buffer can't cover the tier's per-tick cost.
            return;
        }

        final CellContext context = owner.getContext(index);
        if (cell.bonemealCooldown > 0) {
            cell.bonemealCooldown--;
        }

        final Soil soil = context.getSoil();
        if (soil != null) {
            soil.onTick(context, level);
        }

        final Crop crop = context.getCrop();
        if (crop != null) {
            crop.onTick(context, level);
            if (cell.growCooldown.getTicks() > 0) {
                cell.growCooldown.tickDown(level);
            }
            if (cell.growCooldown.getTicks() <= 0 && crop.isGrowthSustained(context, level)) {
                cell.growthTime.tickUp(level);
                crop.onGrowthTick(context, level);
                final int requiredGrowthTicks = GardenPotHelpers.getRequiredGrowthTicks(cell, context, level, crop, soil, owner.upgrades);
                if (cell.growthTime.getTicks() >= requiredGrowthTicks) {
                    owner.updateComparator(index, 15);
                    cell.growCooldown.setTicks(5f);
                    // Every cell auto-harvests into the shared output bank regardless of pot-kind -
                    // unlike a real pot, there's no in-world click target for one specific mini-pot to
                    // harvest it manually, so Basic-kind cells behave like Hopper-kind here.
                    if (crop.canHarvest(context, level)) {
                        if (level instanceof ServerLevel serverLevel && owner.hasFreeOutputSlot()) {
                            final int rolls = GardenPotHelpers.getLootRolls(cell, context, level, crop, soil, owner.upgrades);
                            for (int roll = 0; roll < rolls; roll++) {
                                crop.onHarvest(context, level, stack -> Services.GAMEPLAY.addItem(stack, owner.output, OUTPUT_SLOT_RANGE));
                            }
                            // Fortune Upgrade: a flat chance (stack count x 0.15, capped at 0.45 for
                            // 3 stacked) of one extra roll on top of the normal yield - applies to
                            // every cell regardless of pot kind, unlike the energy-cost reduction below.
                            final float fortuneChance = GardenUpgradeSupport.getFortuneChance(owner.upgrades);
                            if (fortuneChance > 0f && level.random.nextFloat() < fortuneChance) {
                                crop.onHarvest(context, level, stack -> Services.GAMEPLAY.addItem(stack, owner.output, OUTPUT_SLOT_RANGE));
                            }
                            owner.hasOutput = true;
                            if (BotanyPotsMod.CONFIG.get().gameplay.damage_harvest_tool && EnchantmentLevel.FIRST.get(Helpers.NEGATE_HARVEST_DAMAGE_TAG, cell.tool) <= 0) {
                                cell.tool.hurtAndBreak(1, serverLevel, null, stack -> {
                                });
                            }
                            level.gameEvent(GameEvent.BLOCK_CHANGE, owner.worldPosition, GameEvent.Context.of(owner.getBlockState()));
                        }
                        // Output bank was full (checked server-side only, but growthTime resets the
                        // same on both sides either way - see the client/server tick note on
                        // GardenPotBlockEntity#tick): the cycle's yield is simply never rolled, so
                        // nothing is generated only to be discarded. It just regrows and tries again.
                        cell.growthTime.reset();
                    }
                }
                else {
                    owner.updateComparator(index, Mth.ceil(14f * (cell.growthTime.getTicks() / requiredGrowthTicks)));
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final ListTag cellList = new ListTag();
        for (GardenPotCell cell : this.cells) {
            cellList.add(cell.save(registries));
        }
        tag.put("Cells", cellList);
        ContainerHelper.saveAllItems(tag, this.output, registries);
        // Own nested tag so its "Items" key doesn't collide with the output bank's.
        final CompoundTag upgradesTag = new CompoundTag();
        ContainerHelper.saveAllItems(upgradesTag, this.upgrades, registries);
        tag.put("Upgrades", upgradesTag);
        tag.put("Energy", this.energy.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        final ListTag cellList = tag.getList("Cells", Tag.TAG_COMPOUND);
        for (int i = 0; i < GRID_SIZE; i++) {
            this.cells[i] = new GardenPotCell();
            if (i < cellList.size()) {
                this.cells[i].load(cellList.getCompound(i), registries);
            }
        }
        this.output = NonNullList.withSize(OUTPUT_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.output, registries);
        this.upgrades = NonNullList.withSize(UPGRADE_SLOTS, ItemStack.EMPTY);
        if (tag.contains("Upgrades")) {
            ContainerHelper.loadAllItems(tag.getCompound("Upgrades"), this.upgrades, registries);
        }
        this.hasOutput = false;
        for (final ItemStack stack : this.output) {
            if (!stack.isEmpty()) {
                this.hasOutput = true;
                break;
            }
        }
        if (tag.contains("Energy")) {
            this.energy.deserializeNBT(registries, tag.get("Energy"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

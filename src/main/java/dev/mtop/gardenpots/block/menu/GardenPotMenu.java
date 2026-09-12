package dev.mtop.gardenpots.block.menu;

import dev.mtop.gardenpots.GardenPots;
import dev.mtop.gardenpots.block.context.CellContext;
import dev.mtop.gardenpots.block.entity.GardenPotBlockEntity;
import dev.mtop.gardenpots.block.entity.GardenPotCell;
import dev.mtop.gardenpots.init.ModMenus;
import dev.mtop.gardenpots.upgrade.GardenUpgradeSupport;
import com.mojang.datafixers.util.Pair;
import net.darkhax.bookshelf.common.api.menu.data.BlockPosData;
import net.darkhax.bookshelf.common.api.menu.slot.InputSlot;
import net.darkhax.bookshelf.common.api.menu.slot.OutputSlot;
import net.darkhax.botanypots.common.api.data.recipes.RecipeCache;
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop;
import net.darkhax.botanypots.common.api.data.recipes.soil.Soil;
import net.darkhax.botanypots.common.impl.BotanyPotsMod;
import net.darkhax.botanypots.common.impl.block.BotanyPotBlock;
import net.darkhax.botanypots.common.impl.block.menu.BotanyPotMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A 3x3 grid of (pot-kind, soil, seed, tool) input clusters, one per {@link GardenPotCell}, plus one
 * shared 27-slot output bank fed by every hopper-kind cell. Layout constants here are read by
 * {@link GardenPotScreen} to draw matching slot backgrounds.
 */
public class GardenPotMenu extends AbstractContainerMenu {

    public static final ResourceLocation EMPTY_SLOT_SOIL = BotanyPotsMod.id("item/empty_slot_soil");
    public static final ResourceLocation EMPTY_SLOT_SEED = BotanyPotsMod.id("item/empty_slot_seed");
    public static final ResourceLocation EMPTY_SLOT_HOE = ResourceLocation.withDefaultNamespace("item/empty_slot_hoe");
    // Faded versions of the 4 upgrade items' own icons (same gem shape, greyed out) - shown in an
    // empty upgrade slot the same way EMPTY_SLOT_SOIL/SEED/HOE hint at what an empty pot-cell slot
    // wants, see atlases/blocks.json for how these get into the block atlas Slot#getNoItemIcon reads.
    private static final ResourceLocation[] EMPTY_SLOT_UPGRADE = {
            ResourceLocation.fromNamespaceAndPath(GardenPots.MODID, "item/speed_upgrade_empty"),
            ResourceLocation.fromNamespaceAndPath(GardenPots.MODID, "item/output_upgrade_empty"),
            ResourceLocation.fromNamespaceAndPath(GardenPots.MODID, "item/energy_upgrade_empty"),
            ResourceLocation.fromNamespaceAndPath(GardenPots.MODID, "item/fortune_upgrade_empty")
    };

    // Layout, shared with GardenPotScreen.
    public static final int SLOT = 18;
    public static final int CLUSTER_GAP = 6;
    public static final int GRID_LEFT = 8;
    // Room for up to 3 stacked status lines under the title (energy, and - Dimensional Garden Pot
    // only - the bound frequency) before the pot grid starts.
    public static final int GRID_TOP = 36;
    public static final int OUTPUT_COLUMNS = 9;
    // +1 slot-width per cluster for the enable/disable toggle button (not a real item slot).
    public static final int CLUSTER_WIDTH = (GardenPotBlockEntity.CLUSTER_SLOTS + 1) * SLOT;
    public static final int TOGGLE_OFFSET = GardenPotBlockEntity.CLUSTER_SLOTS * SLOT;
    public static final int GRID_WIDTH = 3 * CLUSTER_WIDTH + 2 * CLUSTER_GAP;
    // Extra +10 (vs. the old top-row layout) leaves room for the "Output" label, which is now a plain
    // horizontal line above the bank instead of rotated in a side margin - needs clearance below the
    // last pot row's progress bar so the two don't draw on top of each other.
    public static final int OUTPUT_TOP = GRID_TOP + 3 * SLOT + 2 * CLUSTER_GAP + 20;
    // Left-aligned under the pot grid (not centered) - frees up the whole right side of the panel for
    // the vertical upgrade column instead of a symmetric, otherwise-empty margin on both sides.
    public static final int OUTPUT_LEFT = GRID_LEFT;
    // The 4 shared PowerPots upgrade slots, stacked vertically to the right of the output bank (one
    // row each, stat text immediately beside it) - empty (and, per their mayPlace, unusable) when
    // PowerPots isn't installed, but always present so the layout doesn't shift depending on what's
    // loaded. Left where there's room: output is only 9 wide but the pot grid above it is wider, so
    // this sits in that same span, to the right of the output bank.
    public static final int UPGRADE_COL_LEFT = OUTPUT_LEFT + OUTPUT_COLUMNS * SLOT + 10;
    public static final int UPGRADE_COL_TOP = OUTPUT_TOP;
    public static final int UPGRADE_TEXT_LEFT = UPGRADE_COL_LEFT + SLOT + 4;
    // The upgrade column (4 rows) is always a row taller than the output bank (3 rows) - everything
    // below has to clear *that*, not the output bank, or the separator/label end up drawn through the
    // 4th upgrade slot instead of below it.
    public static final int UPGRADE_COL_BOTTOM = UPGRADE_COL_TOP + GardenPotBlockEntity.UPGRADE_SLOTS * SLOT;
    public static final int SEPARATOR_Y = UPGRADE_COL_BOTTOM + 4;
    public static final int INV_LABEL_Y = SEPARATOR_Y + 8;
    public static final int INV_TOP = INV_LABEL_Y + 10;
    public static final int PANEL_WIDTH = GRID_WIDTH + GRID_LEFT * 2;
    public static final int PANEL_HEIGHT = INV_TOP + 4 * SLOT + 14;

    private final Container container;
    private final Inventory playerInv;
    private final ContainerData blockPos;
    private final ContainerData progress;
    /** List-position boundary between pot-side slots and player-inventory slots - computed live off
     * {@link #slots}' actual size rather than hardcoding {@code GardenPotBlockEntity.CONTAINER_SIZE},
     * since that boundary has moved before (the upgrade slots were briefly PowerPots-only and
     * conditionally registered) and this way a future layout change can't silently desync it again. */
    private final int inventoryStart;

    public static GardenPotMenu clientMenu(int containerId, Inventory playerInv) {
        return new GardenPotMenu(containerId, playerInv, new SimpleContainer(GardenPotBlockEntity.CONTAINER_SIZE),
                new SimpleContainerData(3), new SimpleContainerData(GardenPotBlockEntity.GRID_SIZE));
    }

    public static GardenPotMenu serverMenu(int containerId, Inventory playerInv, GardenPotBlockEntity pot) {
        return new GardenPotMenu(containerId, playerInv, pot, new BlockPosData(pot.getBlockPos()), new CellProgressData(pot));
    }

    public GardenPotMenu(int id, Inventory playerInv, Container container, ContainerData blockPos, ContainerData progress) {
        super(ModMenus.GARDEN_POT.get(), id);
        this.container = container;
        this.playerInv = playerInv;
        this.blockPos = blockPos;
        this.progress = progress;
        this.addDataSlots(this.blockPos);
        this.addDataSlots(this.progress);

        for (int i = 0; i < GardenPotBlockEntity.GRID_SIZE; i++) {
            final int col = i % 3;
            final int row = i / 3;
            final int x = GRID_LEFT + col * (CLUSTER_WIDTH + CLUSTER_GAP);
            final int y = GRID_TOP + row * (SLOT + CLUSTER_GAP);
            final int base = i * GardenPotBlockEntity.CLUSTER_SLOTS;

            this.addSlot(new PotKindSlot(container, base, x, y));
            this.addSlot(new InputSlot(container, base + 1, x + SLOT, y, EMPTY_SLOT_SOIL));
            this.addSlot(new InputSlot(container, base + 2, x + SLOT * 2, y, EMPTY_SLOT_SEED));
            this.addSlot(new InputSlot(container, base + 3, x + SLOT * 3, y, EMPTY_SLOT_HOE, stack -> stack.is(BotanyPotMenu.HARVEST_ITEM)));
        }

        for (int i = 0; i < GardenPotBlockEntity.OUTPUT_SLOTS; i++) {
            final int x = OUTPUT_LEFT + (i % OUTPUT_COLUMNS) * SLOT;
            final int y = OUTPUT_TOP + (i / OUTPUT_COLUMNS) * SLOT;
            this.addSlot(new OutputSlot(container, GardenPotBlockEntity.CELL_SLOTS + i, x, y));
        }

        // Our own upgrade items, base-mod feature - always registered, no optional-mod gate needed.
        for (int i = 0; i < GardenPotBlockEntity.UPGRADE_SLOTS; i++) {
            final int slotIndex = GardenPotBlockEntity.CELL_SLOTS + GardenPotBlockEntity.OUTPUT_SLOTS + i;
            this.addSlot(new UpgradeSlot(container, slotIndex, UPGRADE_COL_LEFT, UPGRADE_COL_TOP + i * SLOT, i));
        }
        this.inventoryStart = this.slots.size();

        // Same width as the output bank (9 wide), so center it the same way - OUTPUT_LEFT.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, OUTPUT_LEFT + col * SLOT, INV_TOP + row * SLOT));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, OUTPUT_LEFT + col * SLOT, INV_TOP + 3 * SLOT + 8));
        }
    }

    /** Restricts placement to items whose block is a {@link BotanyPotBlock} - Basic/Hopper/Waxed pots
     * and every addon pot that subclasses it (Botany Pots Tiers, PowerPots). */
    private static class PotKindSlot extends Slot {
        PotKindSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BotanyPotBlock;
        }
    }

    /** One of the 4 shared upgrade slots (Speed/Output/Energy/Fortune) - our own items, see
     * {@link dev.mtop.gardenpots.upgrade.GardenUpgradeItem}. */
    private static class UpgradeSlot extends Slot {
        private final int typeIndex;

        UpgradeSlot(Container container, int slot, int x, int y, int typeIndex) {
            super(container, slot, x, y);
            this.typeIndex = typeIndex;
        }

        // No getMaxStackSize() override here (unlike PotKindSlot) - the default already caps at the
        // held item's own max stack size, which is exactly what stacking Fortune Upgrade up to 3
        // needs (registered with stacksTo(3); the other three are stacksTo(1)).

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return GardenUpgradeSupport.isUpgradeForSlot(stack, this.typeIndex);
        }

        @NotNull
        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_SLOT_UPGRADE[this.typeIndex]);
        }
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int clickedSlotId) {
        final Slot clickedSlot = this.slots.get(clickedSlotId);
        if (!clickedSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final ItemStack clickedStack = clickedSlot.getItem();
        final ItemStack original = clickedStack.copy();

        if (clickedSlotId < this.inventoryStart) {
            // Pot-side slot -> player inventory.
            if (!this.moveItemStackTo(clickedStack, this.inventoryStart, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        }
        else {
            // Player inventory -> whichever single column (pot-kind/soil/seed/tool) this item
            // actually is, not "the first slot anywhere that'll take it" - see quickInsert.
            if (!this.quickInsert(clickedStack)) {
                return ItemStack.EMPTY;
            }
        }

        if (clickedStack.isEmpty()) {
            clickedSlot.set(ItemStack.EMPTY);
        }
        else {
            clickedSlot.setChanged();
        }
        if (clickedStack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        clickedSlot.onTake(player, clickedStack);
        return original;
    }

    /**
     * Routes a shift-clicked item from the player's inventory to the one slot *column* it actually
     * belongs in - previously this used a blanket {@code moveItemStackTo} across every pot-side slot,
     * which happily spread a single item into every column (soil into every seed slot too, since
     * seed/soil both accept anything by default) instead of just the one it matched.
     *
     * <p>The tool column is tag-gated already (same {@code HARVEST_ITEM} tag the slot itself uses),
     * and pot-kind is an exact type check, so both are unambiguous. Soil and seed have no such tag to
     * key off - BotanyPots recipes match by arbitrary ingredient predicate, not a shared item tag - so
     * those two ask the same live {@code Soil}/{@code Crop} recipe cache the real game uses to grow
     * them, exactly like {@code BotanyPotMenu}'s own quickMoveStack does upstream.
     */
    private boolean quickInsert(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BotanyPotBlock) {
            return this.insertIntoColumn(stack, 0);
        }
        if (stack.is(BotanyPotMenu.HARVEST_ITEM) && this.insertIntoColumn(stack, 3)) {
            return true;
        }
        for (int upgradeSlot = 0; upgradeSlot < GardenPotBlockEntity.UPGRADE_SLOTS; upgradeSlot++) {
            if (GardenUpgradeSupport.isUpgradeForSlot(stack, upgradeSlot)) {
                final int slotIndex = GardenPotBlockEntity.CELL_SLOTS + GardenPotBlockEntity.OUTPUT_SLOTS + upgradeSlot;
                return this.moveItemStackTo(stack, slotIndex, slotIndex + 1, false);
            }
        }
        final GardenPotBlockEntity pot = this.container instanceof GardenPotBlockEntity p ? p : null;
        final Level level = this.playerInv.player.level();
        if (pot != null) {
            final CellContext context = pot.getContext(0);
            final RecipeCache<Soil> soilCache = Soil.CACHE.apply(level);
            if (soilCache != null && soilCache.lookup(stack, context, level) != null && this.insertIntoColumn(stack, 1)) {
                return true;
            }
            final RecipeCache<Crop> cropCache = Crop.CACHE.apply(level);
            if (cropCache != null && cropCache.lookup(stack, context, level) != null && this.insertIntoColumn(stack, 2)) {
                return true;
            }
        }
        return false;
    }

    /** Tries every cell's slot at {@code column} (0=pot-kind, 1=soil, 2=seed, 3=tool) in order,
     * continuing past a full one to the next cell rather than stopping after the first slot - so a
     * stack bigger than one slot can hold still spreads across multiple cells of the *same* column. */
    private boolean insertIntoColumn(ItemStack stack, int column) {
        boolean movedAny = false;
        for (int cell = 0; cell < GardenPotBlockEntity.GRID_SIZE && !stack.isEmpty(); cell++) {
            final int slotIndex = cell * GardenPotBlockEntity.CLUSTER_SLOTS + column;
            if (this.moveItemStackTo(stack, slotIndex, slotIndex + 1, false)) {
                movedAny = true;
            }
        }
        return movedAny;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.container.stillValid(player);
    }

    /** Enable/disable toggle - {@code id} is the cell index. Wired up through vanilla's normal
     * button-click round trip (same mechanism a beacon or loom uses), not a real item slot. */
    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id >= 0 && id < GardenPotBlockEntity.GRID_SIZE && this.container instanceof GardenPotBlockEntity pot) {
            final GardenPotCell cell = pot.getCell(id);
            cell.enabled = !cell.enabled;
            pot.markUpdated();
            return true;
        }
        return false;
    }

    /** 0-100 growth progress for a cell, synced from the server every tick - see {@link CellProgressData}. */
    public int getCellProgress(int index) {
        return this.progress.get(index);
    }

    /** The item currently sitting in shared upgrade slot {@code index} (0=Speed, 1=Output, 2=Energy,
     * 3=Fortune), or empty if nothing is placed - used by {@link GardenPotScreen} to draw the stat
     * line beside each slot. */
    public ItemStack getUpgradeStack(int index) {
        if (index < 0 || index >= GardenPotBlockEntity.UPGRADE_SLOTS) {
            return ItemStack.EMPTY;
        }
        final int slotIndex = GardenPotBlockEntity.CELL_SLOTS + GardenPotBlockEntity.OUTPUT_SLOTS + index;
        return this.slots.get(slotIndex).getItem();
    }

    @Nullable
    public GardenPotBlockEntity getPot() {
        final BlockPos pos = this.blockPos instanceof BlockPosData posData ? posData.getPos() : BlockPosData.readPos(this.blockPos);
        return this.playerInv.player.level().getBlockEntity(pos) instanceof GardenPotBlockEntity pot ? pot : null;
    }
}

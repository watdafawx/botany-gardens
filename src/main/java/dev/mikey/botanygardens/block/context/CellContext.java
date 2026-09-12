package dev.mikey.botanygardens.block.context;

import dev.mikey.botanygardens.block.entity.GardenBlockEntity;
import dev.mikey.botanygardens.block.entity.GardenCell;
import net.darkhax.botanypots.common.api.context.BotanyPotContext;
import net.darkhax.botanypots.common.api.data.recipes.RecipeCache;
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop;
import net.darkhax.botanypots.common.api.data.recipes.soil.Soil;
import net.darkhax.botanypots.common.impl.BotanyPotsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-cell {@link BotanyPotContext}. Plays the same role {@code BlockEntityContext} plays for a real
 * pot, except {@code BotanyPotContext} is a plain interface (not tied to a concrete block entity), so
 * one instance of this can drive the recipe API for a single grid cell instead of a whole block.
 *
 * <p>All 9 cells in a {@link GardenBlockEntity} share the same real {@link net.minecraft.core.BlockPos}
 * (there's only one block in the world), so loot params / function execution / etc. all read
 * position and level off {@code owner}, not off any per-cell state.
 *
 * <p>One instance per cell is reused for the whole life of the block entity (see
 * {@link GardenBlockEntity#getContext}) rather than allocated fresh every tick/render call, and
 * {@link #getCrop()}/{@link #getSoil()} cache their recipe-lookup result keyed on the seed/soil
 * item's {@link Item} identity (a stable singleton per item type - safe against a slot's
 * {@code ItemStack} being mutated in place, e.g. a partial pickup shrinking its count, without a new
 * instance being written back). With 10 gardens x 9 cells this is the difference between 90 fresh
 * {@code RecipeCache} lookups a tick and close to zero once a cell's seed/soil stop changing.
 */
public class CellContext implements BotanyPotContext {

    private final GardenBlockEntity owner;
    private final int index;
    @Nullable
    private final Player player;
    @Nullable
    private final InteractionHand hand;

    @Nullable
    private Item cachedSoilKey;
    @Nullable
    private RecipeHolder<Soil> cachedSoil;
    @Nullable
    private Item cachedCropKey;
    @Nullable
    private RecipeHolder<Crop> cachedCrop;

    public CellContext(GardenBlockEntity owner, int index, @Nullable Player player, @Nullable InteractionHand hand) {
        this.owner = owner;
        this.index = index;
        this.player = player;
        this.hand = hand;
    }

    public GardenCell cell() {
        return this.owner.getCell(this.index);
    }

    @NotNull
    @Override
    public ItemStack getItem(int slotId) {
        final GardenCell cell = this.cell();
        return switch (slotId) {
            case 0 -> cell.soil;
            case 1 -> cell.seed;
            case 2 -> cell.tool;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public ItemStack getSoilItem() {
        return this.cell().soil;
    }

    @Override
    public ItemStack getSeedItem() {
        return this.cell().seed;
    }

    @Override
    public ItemStack getHarvestItem() {
        return this.cell().tool;
    }

    @Override
    public LootParams createLootParams(@Nullable BlockState state) {
        if (this.owner.getLevel() instanceof ServerLevel level) {
            final LootParams.Builder builder = new LootParams.Builder(level);
            builder.withParameter(LootContextParams.BLOCK_STATE, state != null ? state : level.getBlockState(this.owner.getBlockPos()));
            builder.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.owner.getBlockPos()));
            builder.withParameter(LootContextParams.TOOL, this.getInteractionItem());
            builder.withOptionalParameter(LootContextParams.THIS_ENTITY, this.player);
            builder.withOptionalParameter(LootContextParams.BLOCK_ENTITY, this.owner);
            builder.withOptionalParameter(LootContextParams.TOOL, this.getHarvestItem().isEmpty() ? BotanyPotsMod.CONFIG.get().gameplay.default_harvest_tool.apply(level) : this.getHarvestItem());
            return builder.create(LootContextParamSets.BLOCK);
        }
        throw new IllegalStateException("Can not create LootParams on the client!");
    }

    @Override
    public void runFunction(ResourceLocation functionId) {
        this.owner.runFunction(functionId);
    }

    @Override
    public @Nullable Player getPlayer() {
        return this.player;
    }

    @Override
    public ItemStack getInteractionItem() {
        return this.player != null && this.hand != null ? this.player.getItemInHand(this.hand) : ItemStack.EMPTY;
    }

    @Override
    public int getRequiredGrowthTicks() {
        return this.owner.getRequiredGrowthTicks(this.index);
    }

    @Override
    public boolean isServerThread() {
        Level level = this.owner.getLevel();
        if (level == null && this.player != null) {
            level = this.player.level();
        }
        return level != null && !level.isClientSide;
    }

    @Override
    public @Nullable Crop getCrop() {
        final Level level = this.owner.getLevel();
        if (level == null) {
            return null; // BE not attached yet/unloading - don't cache, retry once it is.
        }
        final RecipeCache<Crop> cache = Crop.CACHE.apply(level);
        if (cache == null) {
            return null; // Recipe cache not built yet - same, don't cache.
        }
        final ItemStack seed = this.getSeedItem();
        final Item key = seed.isEmpty() ? null : seed.getItem();
        if (key != this.cachedCropKey) {
            this.cachedCropKey = key;
            this.cachedCrop = cache.lookup(seed, this, level);
        }
        return this.cachedCrop != null ? this.cachedCrop.value() : null;
    }

    @Override
    public @Nullable Soil getSoil() {
        final Level level = this.owner.getLevel();
        if (level == null) {
            return null;
        }
        final RecipeCache<Soil> cache = Soil.CACHE.apply(level);
        if (cache == null) {
            return null;
        }
        final ItemStack soil = this.getSoilItem();
        final Item key = soil.isEmpty() ? null : soil.getItem();
        if (key != this.cachedSoilKey) {
            this.cachedSoilKey = key;
            this.cachedSoil = cache.lookup(soil, this, level);
        }
        return this.cachedSoil != null ? this.cachedSoil.value() : null;
    }
}

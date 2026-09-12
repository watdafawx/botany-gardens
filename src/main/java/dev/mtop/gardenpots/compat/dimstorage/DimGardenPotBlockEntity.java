package dev.mtop.gardenpots.compat.dimstorage;

import dev.mtop.gardenpots.block.entity.GardenPotBlockEntity;
import dev.mtop.gardenpots.block.entity.GardenPotCell;
import edivad.dimstorage.api.Frequency;
import edivad.dimstorage.manager.DimStorageManager;
import edivad.dimstorage.storage.DimChestStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.Objects;

/**
 * The Dimensional Garden Pot: every cell grows exactly like the base block, but the shared
 * {@link GardenPotBlockEntity#output} bank is drained into a bound Dim Storage frequency instead of
 * the inventory below - see {@code DimensionalBotanyPotBlockEntity} upstream, which this mirrors.
 */
public class DimGardenPotBlockEntity extends GardenPotBlockEntity {

    private Frequency frequency = new Frequency();
    private DimChestStorage cachedDimStorage;

    public DimGardenPotBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, DimStorageCompat.DIM_GARDEN_POT_BE.get());
    }

    public Frequency getFrequency() {
        if (this.frequency == null) {
            this.frequency = new Frequency();
        }
        return this.frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = Objects.requireNonNullElseGet(frequency, Frequency::new);
        this.cachedDimStorage = null;
        this.markUpdated();
    }

    @Override
    protected void exportOutput(ServerLevel level) {
        if (this.cachedDimStorage == null) {
            this.cachedDimStorage = (DimChestStorage) DimStorageManager.instance(level).getStorage(level.registryAccess(), this.frequency, "item");
        }
        final IItemHandler dimHandler = new InvWrapper(this.cachedDimStorage);
        boolean anyLeft = false;
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            final ItemStack stack = this.output.get(slot);
            if (!stack.isEmpty()) {
                final ItemStack remaining = ItemHandlerHelper.insertItemStacked(dimHandler, stack, false);
                if (remaining.getCount() != stack.getCount()) {
                    this.output.set(slot, remaining);
                    this.setChanged();
                }
                anyLeft |= !remaining.isEmpty();
            }
        }
        this.hasOutput = anyLeft;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        Frequency.CODEC.encodeStart(NbtOps.INSTANCE, this.getFrequency()).resultOrPartial(err -> {
        }).ifPresent(freqTag -> tag.put("Frequency", freqTag));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Frequency")) {
            this.frequency = Frequency.CODEC.parse(NbtOps.INSTANCE, tag.get("Frequency")).result().orElse(new Frequency());
        }
        this.cachedDimStorage = null;
    }
}

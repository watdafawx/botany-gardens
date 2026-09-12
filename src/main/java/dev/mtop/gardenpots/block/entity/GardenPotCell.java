package dev.mtop.gardenpots.block.entity;

import net.darkhax.bookshelf.common.api.util.TickAccumulator;
import net.darkhax.botanypots.common.impl.block.BotanyPotBlock;
import net.darkhax.botanypots.common.impl.block.PotType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One grid slot of a {@link GardenPotBlockEntity}. Mirrors the state {@code AbstractBotanyPotBlockEntity}
 * keeps for a single real pot (soil/seed/tool + growth/cooldown/comparator state), but re-owned per
 * cell instead of per block entity.
 *
 * <p>{@link #potKind} is the actual pot block-item the player dropped in (Basic/Hopper Pot, an
 * Elite/Ultra/Mega Pot from Botany Pots Tiers, a Power Pot, ...). It determines both the cell's
 * {@link PotType} and which registered {@link BotanyPotBlock}'s growth/yield modifier hooks apply -
 * see {@link dev.mtop.gardenpots.block.GardenPotHelpers}. An empty {@code potKind} means the cell is
 * inert (no pot chosen yet).
 */
public class GardenPotCell {

    public ItemStack potKind = ItemStack.EMPTY;
    public ItemStack soil = ItemStack.EMPTY;
    public ItemStack seed = ItemStack.EMPTY;
    public ItemStack tool = ItemStack.EMPTY;

    public TickAccumulator growthTime = new TickAccumulator(-1f);
    public int comparatorLevel = 0;
    public TickAccumulator growCooldown = new TickAccumulator(0f);
    public int bonemealCooldown = 0;
    /** Player-toggled pause - a disabled cell doesn't tick (no growth, no energy draw, no export) but keeps its contents and progress. */
    public boolean enabled = true;

    public boolean isEmpty() {
        return this.potKind.isEmpty();
    }

    /**
     * The registered pot {@link net.minecraft.world.level.block.Block} behind {@link #potKind}, if
     * it's a {@link BotanyPotBlock} (vanilla Basic/Hopper/Waxed, or any addon's pot block that
     * subclasses it - Botany Pots Tiers and PowerPots both do). {@code null} for an empty cell or an
     * item that isn't a botany pot block-item.
     */
    @Nullable
    public BotanyPotBlock getPotBlock() {
        return this.potKind.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BotanyPotBlock potBlock ? potBlock : null;
    }

    @Nullable
    public PotType getPotType() {
        final BotanyPotBlock potBlock = this.getPotBlock();
        return potBlock != null ? potBlock.type : null;
    }

    public boolean isHopper() {
        return this.getPotType() == PotType.HOPPER;
    }

    public void reset() {
        this.growthTime.reset();
        this.comparatorLevel = 0;
        this.bonemealCooldown = 0;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        if (!this.potKind.isEmpty()) {
            tag.put("PotKind", this.potKind.save(registries));
        }
        if (!this.soil.isEmpty()) {
            tag.put("Soil", this.soil.save(registries));
        }
        if (!this.seed.isEmpty()) {
            tag.put("Seed", this.seed.save(registries));
        }
        if (!this.tool.isEmpty()) {
            tag.put("Tool", this.tool.save(registries));
        }
        tag.putFloat("GrowthTime", this.growthTime.getTicks());
        tag.putInt("Comparator", this.comparatorLevel);
        tag.putFloat("GrowCooldown", this.growCooldown.getTicks());
        tag.putInt("BonemealCooldown", this.bonemealCooldown);
        tag.putBoolean("Enabled", this.enabled);
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        this.potKind = tag.contains("PotKind") ? ItemStack.parseOptional(registries, tag.getCompound("PotKind")) : ItemStack.EMPTY;
        this.soil = tag.contains("Soil") ? ItemStack.parseOptional(registries, tag.getCompound("Soil")) : ItemStack.EMPTY;
        this.seed = tag.contains("Seed") ? ItemStack.parseOptional(registries, tag.getCompound("Seed")) : ItemStack.EMPTY;
        this.tool = tag.contains("Tool") ? ItemStack.parseOptional(registries, tag.getCompound("Tool")) : ItemStack.EMPTY;
        this.growthTime = new TickAccumulator(tag.getFloat("GrowthTime"));
        this.comparatorLevel = tag.getInt("Comparator");
        this.growCooldown = new TickAccumulator(tag.getFloat("GrowCooldown"));
        this.bonemealCooldown = tag.getInt("BonemealCooldown");
        this.enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
    }
}

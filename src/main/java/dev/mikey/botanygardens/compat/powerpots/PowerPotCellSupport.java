package dev.mikey.botanygardens.compat.powerpots;

import com.leo.powerpots.block.PotTier;
import com.leo.powerpots.block.PowerPotBlock;
import com.leo.powerpots.config.Config;
import dev.mikey.botanygardens.energy.GardenEnergyStorage;
import dev.mikey.botanygardens.upgrade.GardenUpgradeSupport;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Reads a Power Pot's tier off the pot-kind item's own registry id - PowerPots registers one block
 * (and item) per tier, named {@code power_pot_<tier>}, and reads the tier back the exact same way
 * itself in {@code PowerPotBlock#appendHoverText} (so a fresh, never-placed item shows the right
 * tooltip) - not off {@code BLOCK_ENTITY_DATA}, which is only populated after a real placed
 * {@code PowerPotBlockEntity} is broken and doesn't exist on a freshly crafted/creative item. Cells
 * using the NBT-only approach never resolved a tier at all for that common case, which is why Power
 * Pots didn't work in practice - this is the fix.
 *
 * <p>This class is only about the Power Pot's own tier (speed/yield/energy-cost baked into the pot
 * item itself, distinct per tier) - the Garden Pot's shared upgrade bank is a separate, always-
 * available system now (see {@link GardenUpgradeSupport}, no optional-mod dependency at all), applied
 * on top of whatever a cell's own tier (if any) already grants.
 *
 * <p>Every call site that reaches this class is guarded by {@code BotanyGardens.isPowerPotsLoaded()}
 * (see that method's callers) so this class, and the {@code com.leo.powerpots} types it imports, is
 * only ever classloaded when PowerPots is actually present.
 */
public class PowerPotCellSupport {

    public static boolean isPowerPot(ItemStack potKindStack) {
        return potKindStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof PowerPotBlock;
    }

    public static PotTier getTier(ItemStack potKindStack) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(potKindStack.getItem());
        final String path = id.getPath();
        final char last = path.isEmpty() ? ' ' : path.charAt(path.length() - 1);
        if (!Character.isDigit(last)) {
            return PotTier.ZERO;
        }
        final int index = Character.digit(last, 10);
        return index >= 1 && index <= Config.INSTANCE.TIERS.size() ? Config.INSTANCE.TIERS.get(index - 1) : PotTier.ZERO;
    }

    /**
     * Mirrors {@code PowerPotBlockEntity#tickPot}'s own gate: needs at least a full tick's worth of
     * energy banked before it'll draw and allow growth this tick. {@code upgrades} is the Garden
     * Pot's shared upgrade bank - its Energy Upgrade reduces the draw the same way a real Power Pot's
     * own energy-upgrade would.
     */
    public static boolean tryConsumeEnergy(GardenEnergyStorage energy, ItemStack potKindStack, List<ItemStack> upgrades) {
        final int cost = Math.round(getTier(potKindStack).powerEachTick() * GardenUpgradeSupport.getEnergyModifier(upgrades));
        if (cost <= 0 || energy.getEnergyStored() >= cost) {
            energy.removeEnergy(cost);
            return true;
        }
        return false;
    }

    /** The Power Pot's own tier speed bonus - separate from (and stacks with) the Garden Pot's shared
     * Speed Upgrade, applied on top of this in {@code GardenHelpers}. */
    public static float getTierSpeedModifier(ItemStack potKindStack) {
        return getTier(potKindStack).speedModifier();
    }

    /** The Power Pot's own tier yield bonus - separate from (and stacks with) the Garden Pot's shared
     * Output Upgrade, applied on top of this in {@code GardenHelpers}. */
    public static float getTierYieldMultiplier(ItemStack potKindStack) {
        return getTier(potKindStack).itemAmountMultiplier();
    }
}

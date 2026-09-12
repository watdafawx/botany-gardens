package dev.mikey.botanygardens.upgrade;

import dev.mikey.botanygardens.config.ModConfig;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Math for the Garden Pot's own 4 shared upgrade slots (Speed/Output/Energy/Fortune) - see
 * {@link GardenUpgradeItem}. Entirely our own types, no optional-mod guard needed anywhere in this
 * class (unlike {@code compat.powerpots.PowerPotCellSupport}, which does need one).
 */
public class GardenUpgradeSupport {

    public static final int UPGRADE_SLOTS = 4;

    /** Whether {@code stack} is the one upgrade item that belongs in the Garden Pot's upgrade slot
     * {@code slotIndex} (0=Speed, 1=Output, 2=Energy, 3=Fortune) - slot index matches
     * {@link GardenUpgradeType}'s ordinal order. */
    public static boolean isUpgradeForSlot(ItemStack stack, int slotIndex) {
        return slotIndex >= 0 && slotIndex < UPGRADE_SLOTS
                && stack.getItem() instanceof GardenUpgradeItem upgradeItem
                && upgradeItem.getType().ordinal() == slotIndex;
    }

    public static float getSpeedModifier(List<ItemStack> upgrades) {
        return getModifier(upgrades, GardenUpgradeType.SPEED);
    }

    public static float getYieldMultiplier(List<ItemStack> upgrades) {
        return getModifier(upgrades, GardenUpgradeType.YIELD);
    }

    public static float getEnergyModifier(List<ItemStack> upgrades) {
        return getModifier(upgrades, GardenUpgradeType.ENERGY);
    }

    /** Fortune Upgrade stacks to 3 in its slot - each stacked copy adds another flat chance of one
     * bonus harvest roll, capped at {@link ModConfig#FORTUNE_UPGRADE_MAX_CHANCE} (0.45 = 3 stacked by
     * default), matching how PowerPots' own Fortune Upgrade works (not a per-roll yield multiplier). */
    public static float getFortuneChance(List<ItemStack> upgrades) {
        float chance = 0f;
        for (final ItemStack stack : upgrades) {
            if (stack.getItem() instanceof GardenUpgradeItem upgradeItem && upgradeItem.getType() == GardenUpgradeType.FORTUNE) {
                chance += upgradeItem.getModifier() * stack.getCount();
            }
        }
        return Math.min(chance, ModConfig.FORTUNE_UPGRADE_MAX_CHANCE.get().floatValue());
    }

    /** The percent bonus a slot's currently-held upgrade stack is granting, for the GUI's stat line
     * (0 when the slot is empty). Fortune reads as its bonus-roll chance; Speed/Output/Energy read as
     * the modifier's distance from a no-op multiplier of 1. */
    public static float getSlotBonusPercent(ItemStack stack) {
        if (!(stack.getItem() instanceof GardenUpgradeItem upgradeItem)) {
            return 0f;
        }
        if (upgradeItem.getType() == GardenUpgradeType.FORTUNE) {
            return Math.min(upgradeItem.getModifier() * stack.getCount(), ModConfig.FORTUNE_UPGRADE_MAX_CHANCE.get().floatValue()) * 100f;
        }
        return (upgradeItem.getModifier() - 1f) * 100f;
    }

    private static float getModifier(List<ItemStack> upgrades, GardenUpgradeType type) {
        float modifier = 1f;
        for (final ItemStack stack : upgrades) {
            if (stack.getItem() instanceof GardenUpgradeItem upgradeItem && upgradeItem.getType() == type) {
                modifier *= upgradeItem.getModifier();
            }
        }
        return modifier;
    }
}

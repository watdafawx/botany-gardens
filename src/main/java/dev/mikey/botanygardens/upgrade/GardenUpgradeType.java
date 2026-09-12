package dev.mikey.botanygardens.upgrade;

import dev.mikey.botanygardens.config.ModConfig;

/**
 * The 4 kinds of shared upgrade a Garden Pot's upgrade bank can hold - ordinal order matches the
 * upgrade column's slot order (see {@link GardenUpgradeSupport#UPGRADE_SLOTS}) and the GUI's stat
 * list, top to bottom.
 */
public enum GardenUpgradeType {
    SPEED,
    YIELD,
    ENERGY,
    FORTUNE;

    /** Read live from {@link ModConfig} on every call (not baked into the item) - safe regardless of
     * config-vs-item-registration ordering, and picks up a config reload without a restart. */
    public float getConfiguredModifier() {
        return switch (this) {
            case SPEED -> ModConfig.SPEED_UPGRADE_MODIFIER.get().floatValue();
            case YIELD -> ModConfig.OUTPUT_UPGRADE_MODIFIER.get().floatValue();
            case ENERGY -> ModConfig.ENERGY_UPGRADE_MODIFIER.get().floatValue();
            case FORTUNE -> ModConfig.FORTUNE_UPGRADE_CHANCE_PER_STACK.get().floatValue();
        };
    }
}

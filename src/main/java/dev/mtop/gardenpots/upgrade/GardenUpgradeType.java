package dev.mtop.gardenpots.upgrade;

/**
 * The 4 kinds of shared upgrade a Garden Pot's upgrade bank can hold - ordinal order matches the
 * upgrade column's slot order (see {@link GardenUpgradeSupport#UPGRADE_SLOTS}) and the GUI's stat
 * list, top to bottom.
 */
public enum GardenUpgradeType {
    SPEED,
    YIELD,
    ENERGY,
    FORTUNE
}

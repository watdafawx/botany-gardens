package dev.mtop.gardenpots.upgrade;

import net.minecraft.world.item.Item;

/**
 * Our own upgrade item - applies to every cell in a Garden Pot regardless of pot kind (Basic,
 * Hopper, Elite/Ultra/Mega, Power Pot), unlike PowerPots' own upgrade items, which only ever meant
 * anything for Power-Pot-kind cells. {@link #modifier} means different things per {@link #type}: a
 * multiplier for SPEED/YIELD, a multiplier on FE cost for ENERGY (only relevant on Power-Pot-kind
 * cells - a no-op otherwise), and a per-stacked-copy bonus-roll chance for FORTUNE.
 */
public class GardenUpgradeItem extends Item {

    private final GardenUpgradeType type;
    private final float modifier;

    public GardenUpgradeItem(GardenUpgradeType type, float modifier, int maxStackSize) {
        super(new Item.Properties().stacksTo(maxStackSize));
        this.type = type;
        this.modifier = modifier;
    }

    public GardenUpgradeType getType() {
        return this.type;
    }

    public float getModifier() {
        return this.modifier;
    }
}

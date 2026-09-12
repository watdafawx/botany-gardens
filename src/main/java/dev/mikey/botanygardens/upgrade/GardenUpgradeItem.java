package dev.mikey.botanygardens.upgrade;

import net.minecraft.world.item.Item;

/**
 * Our own upgrade item - applies to every cell in a Garden Pot regardless of pot kind (Basic,
 * Hopper, Elite/Ultra/Mega, Power Pot), unlike PowerPots' own upgrade items, which only ever meant
 * anything for Power-Pot-kind cells. {@link #getModifier()} means different things per {@link #type}:
 * a multiplier for SPEED/YIELD, a multiplier on FE cost for ENERGY (only relevant on Power-Pot-kind
 * cells - a no-op otherwise), and a per-stacked-copy bonus-roll chance for FORTUNE. Read live from
 * {@code ModConfig} (see {@link GardenUpgradeType#getConfiguredModifier()}) rather than baked in at
 * construction, so it's tunable via config.
 */
public class GardenUpgradeItem extends Item {

    private final GardenUpgradeType type;

    public GardenUpgradeItem(GardenUpgradeType type, int maxStackSize) {
        super(new Item.Properties().stacksTo(maxStackSize));
        this.type = type;
    }

    public GardenUpgradeType getType() {
        return this.type;
    }

    public float getModifier() {
        return this.type.getConfiguredModifier();
    }
}

package dev.mikey.botanygardens.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Every balance knob in the mod that's worth tuning without a recompile - written to
 * {@code config/botanygardens-common.toml}. Structural constants (grid size, slot counts, NBT keys)
 * stay as {@code static final} fields in their owning classes; only numbers a pack/server admin would
 * plausibly want to retune live here.
 */
public class ModConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue ENERGY_MAX_RECEIVE;
    public static final ModConfigSpec.IntValue ENERGY_SYNC_PERIOD;
    public static final ModConfigSpec.IntValue EXPORT_PERIOD;

    public static final ModConfigSpec.DoubleValue SPEED_UPGRADE_MODIFIER;
    public static final ModConfigSpec.DoubleValue OUTPUT_UPGRADE_MODIFIER;
    public static final ModConfigSpec.DoubleValue ENERGY_UPGRADE_MODIFIER;
    public static final ModConfigSpec.DoubleValue FORTUNE_UPGRADE_CHANCE_PER_STACK;
    public static final ModConfigSpec.DoubleValue FORTUNE_UPGRADE_MAX_CHANCE;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Shared FE buffer that Power-Pot-kind cells draw from (one per Garden Pot block).").push("energy");
        ENERGY_CAPACITY = builder
                .comment("Max FE the shared buffer can hold. Default sized for a full 3x3 of top-tier Power Pots.")
                .defineInRange("capacity", 100_000, 0, Integer.MAX_VALUE);
        ENERGY_MAX_RECEIVE = builder
                .comment("Max FE/t a cable can push into the buffer.")
                .defineInRange("maxReceive", 2_000, 0, Integer.MAX_VALUE);
        ENERGY_SYNC_PERIOD = builder
                .comment("Ticks between server->client resyncs of the energy buffer (GUI/Jade display lag).")
                .defineInRange("syncPeriodTicks", 10, 1, Integer.MAX_VALUE);
        builder.pop();

        EXPORT_PERIOD = builder
                .comment("Ticks between auto-export attempts once the shared output bank has something to move.")
                .defineInRange("exportPeriodTicks", 8, 1, Integer.MAX_VALUE);

        builder.comment("The 4 shared upgrade items (Speed/Output/Energy/Fortune) - apply to every cell regardless of pot kind.").push("upgrades");
        SPEED_UPGRADE_MODIFIER = builder
                .comment("Speed Upgrade: multiplier on growth rate.")
                .defineInRange("speedModifier", 1.5, 0.0, 100.0);
        OUTPUT_UPGRADE_MODIFIER = builder
                .comment("Output Upgrade: multiplier on harvest yield.")
                .defineInRange("outputModifier", 2.0, 0.0, 100.0);
        ENERGY_UPGRADE_MODIFIER = builder
                .comment("Energy Upgrade: multiplier on FE cost (Power-Pot-kind cells only).")
                .defineInRange("energyModifier", 0.75, 0.0, 10.0);
        FORTUNE_UPGRADE_CHANCE_PER_STACK = builder
                .comment("Fortune Upgrade: flat bonus-harvest-roll chance added per stacked copy.")
                .defineInRange("fortuneChancePerStack", 0.15, 0.0, 1.0);
        FORTUNE_UPGRADE_MAX_CHANCE = builder
                .comment("Fortune Upgrade: cap on the total bonus-roll chance (stacks to 3 by default).")
                .defineInRange("fortuneMaxChance", 0.45, 0.0, 1.0);
        builder.pop();

        SPEC = builder.build();
    }
}

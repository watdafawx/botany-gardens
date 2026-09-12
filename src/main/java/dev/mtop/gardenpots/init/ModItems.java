package dev.mtop.gardenpots.init;

import dev.mtop.gardenpots.GardenPots;
import dev.mtop.gardenpots.upgrade.GardenUpgradeItem;
import dev.mtop.gardenpots.upgrade.GardenUpgradeType;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GardenPots.MODID);

    public static final DeferredItem<BlockItem> GARDEN_POT = ITEMS.registerSimpleBlockItem(ModBlocks.GARDEN_POT);

    // The Garden Pot's own 4 shared upgrades - apply to every cell regardless of pot kind, unlike
    // PowerPots' own upgrade items (Power-Pot-kind cells only). Values mirror what PowerPots' own
    // upgrades did, so nothing changes numerically for anyone already using them.
    public static final DeferredItem<GardenUpgradeItem> SPEED_UPGRADE =
            ITEMS.register("speed_upgrade", () -> new GardenUpgradeItem(GardenUpgradeType.SPEED, 1.5f, 1));
    public static final DeferredItem<GardenUpgradeItem> OUTPUT_UPGRADE =
            ITEMS.register("output_upgrade", () -> new GardenUpgradeItem(GardenUpgradeType.YIELD, 2.0f, 1));
    public static final DeferredItem<GardenUpgradeItem> ENERGY_UPGRADE =
            ITEMS.register("energy_upgrade", () -> new GardenUpgradeItem(GardenUpgradeType.ENERGY, 0.75f, 1));
    public static final DeferredItem<GardenUpgradeItem> FORTUNE_UPGRADE =
            ITEMS.register("fortune_upgrade", () -> new GardenUpgradeItem(GardenUpgradeType.FORTUNE, 0.15f, 3));
}

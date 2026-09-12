package dev.mikey.botanygardens.init;

import dev.mikey.botanygardens.BotanyGardens;
import dev.mikey.botanygardens.upgrade.GardenUpgradeItem;
import dev.mikey.botanygardens.upgrade.GardenUpgradeType;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BotanyGardens.MODID);

    public static final DeferredItem<BlockItem> GARDEN = ITEMS.registerSimpleBlockItem(ModBlocks.GARDEN);

    // The Garden Pot's own 4 shared upgrades - apply to every cell regardless of pot kind, unlike
    // PowerPots' own upgrade items (Power-Pot-kind cells only). Modifier values live in ModConfig
    // (see GardenUpgradeType#getConfiguredModifier), not here.
    public static final DeferredItem<GardenUpgradeItem> SPEED_UPGRADE =
            ITEMS.register("speed_upgrade", () -> new GardenUpgradeItem(GardenUpgradeType.SPEED, 1));
    public static final DeferredItem<GardenUpgradeItem> OUTPUT_UPGRADE =
            ITEMS.register("output_upgrade", () -> new GardenUpgradeItem(GardenUpgradeType.YIELD, 1));
    public static final DeferredItem<GardenUpgradeItem> ENERGY_UPGRADE =
            ITEMS.register("energy_upgrade", () -> new GardenUpgradeItem(GardenUpgradeType.ENERGY, 1));
    public static final DeferredItem<GardenUpgradeItem> FORTUNE_UPGRADE =
            ITEMS.register("fortune_upgrade", () -> new GardenUpgradeItem(GardenUpgradeType.FORTUNE, 3));
}

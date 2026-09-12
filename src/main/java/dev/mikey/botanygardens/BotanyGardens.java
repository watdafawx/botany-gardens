package dev.mikey.botanygardens;

import dev.mikey.botanygardens.compat.dimstorage.DimStorageCompat;
import dev.mikey.botanygardens.config.ModConfig;
import dev.mikey.botanygardens.init.ModBlockEntities;
import dev.mikey.botanygardens.init.ModBlocks;
import dev.mikey.botanygardens.init.ModCreativeTab;
import dev.mikey.botanygardens.init.ModItems;
import dev.mikey.botanygardens.init.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Botany Gardens - an addon for {@code botanypots}. One block houses a 3x3 grid of virtual pots, each
 * accepting any pot-kind item the player owns (vanilla Basic/Hopper, Botany Pots Tiers'
 * Elite/Ultra/Mega, PowerPots' Power Pot), and shares one FE port between any Power-Pot-kind cells.
 *
 * <p>Botany Pots Tiers / PowerPots / DimStorage support lives entirely under {@code compat.*} and
 * is reached only from guarded call sites (never from an {@code @EventBusSubscriber}, since FML
 * loads annotated classes unconditionally regardless of whether the optional mod is present).
 */
@Mod(BotanyGardens.MODID)
public class BotanyGardens {

    public static final String MODID = "botanygardens";

    public static final String BOTANYPOTSTIERS = "botanypotstiers";
    public static final String POWERPOTS = "powerpots";
    public static final String DIMSTORAGE = "dimstorage";

    public static boolean isPowerPotsLoaded() {
        return ModList.get().isLoaded(POWERPOTS);
    }

    public static boolean isBotanyPotsTiersLoaded() {
        return ModList.get().isLoaded(BOTANYPOTSTIERS);
    }

    public static boolean isDimStorageLoaded() {
        return ModList.get().isLoaded(DIMSTORAGE);
    }

    public BotanyGardens(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        modBus.addListener(BotanyGardens::onRegisterCapabilities);
        DimStorageCompat.init(modBus);
        container.registerConfig(Type.COMMON, ModConfig.SPEC);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.GARDEN.get(), (be, side) -> be.energy);
        // The dimensional variant's block-entity type is a distinct DeferredHolder (only populated
        // when DimStorage is loaded) - it inherits `energy` from GardenBlockEntity, but NeoForge's
        // capability lookup is keyed per concrete BlockEntityType, so it needs its own registration
        // or FE cables never find a provider for it.
        if (BotanyGardens.isDimStorageLoaded() && DimStorageCompat.DIM_GARDEN_BE != null) {
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DimStorageCompat.DIM_GARDEN_BE.get(), (be, side) -> be.energy);
        }
    }
}

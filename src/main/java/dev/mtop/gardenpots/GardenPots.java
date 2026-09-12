package dev.mtop.gardenpots;

import dev.mtop.gardenpots.compat.dimstorage.DimStorageCompat;
import dev.mtop.gardenpots.init.ModBlockEntities;
import dev.mtop.gardenpots.init.ModBlocks;
import dev.mtop.gardenpots.init.ModCreativeTab;
import dev.mtop.gardenpots.init.ModItems;
import dev.mtop.gardenpots.init.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Garden Pots - an addon for {@code botanypots}. One block houses a 3x3 grid of virtual pots, each
 * accepting any pot-kind item the player owns (vanilla Basic/Hopper, Botany Pots Tiers'
 * Elite/Ultra/Mega, PowerPots' Power Pot), and shares one FE port between any Power-Pot-kind cells.
 *
 * <p>Botany Pots Tiers / PowerPots / DimStorage support lives entirely under {@code compat.*} and
 * is reached only from guarded call sites (never from an {@code @EventBusSubscriber}, since FML
 * loads annotated classes unconditionally regardless of whether the optional mod is present).
 */
@Mod(GardenPots.MODID)
public class GardenPots {

    public static final String MODID = "gardenpots";

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

    public GardenPots(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        modBus.addListener(GardenPots::onRegisterCapabilities);
        DimStorageCompat.init(modBus);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.GARDEN_POT.get(), (be, side) -> be.energy);
        // The dimensional variant's block-entity type is a distinct DeferredHolder (only populated
        // when DimStorage is loaded) - it inherits `energy` from GardenPotBlockEntity, but NeoForge's
        // capability lookup is keyed per concrete BlockEntityType, so it needs its own registration
        // or FE cables never find a provider for it.
        if (GardenPots.isDimStorageLoaded() && DimStorageCompat.DIM_GARDEN_POT_BE != null) {
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DimStorageCompat.DIM_GARDEN_POT_BE.get(), (be, side) -> be.energy);
        }
    }
}

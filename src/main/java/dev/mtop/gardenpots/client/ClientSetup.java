package dev.mtop.gardenpots.client;

import dev.mtop.gardenpots.GardenPots;
import dev.mtop.gardenpots.block.menu.GardenPotScreen;
import dev.mtop.gardenpots.compat.dimstorage.DimStorageCompat;
import dev.mtop.gardenpots.init.ModBlockEntities;
import dev.mtop.gardenpots.init.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = GardenPots.MODID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.GARDEN_POT.get(), GardenPotScreen::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.GARDEN_POT.get(), GardenPotRenderer::new);
        // The Dimensional variant is a distinct BlockEntityType (only populated when DimStorage is
        // loaded) - same class of bug as the energy capability earlier: registering only the base
        // type here left the Dimensional Garden Pot's cells with no renderer at all, so none of its
        // mini-pots/crops ever showed despite ticking and growing correctly.
        if (GardenPots.isDimStorageLoaded() && DimStorageCompat.DIM_GARDEN_POT_BE != null) {
            event.registerBlockEntityRenderer(DimStorageCompat.DIM_GARDEN_POT_BE.get(), GardenPotRenderer::new);
        }
    }
}

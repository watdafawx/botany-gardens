package dev.mikey.botanygardens.client;

import dev.mikey.botanygardens.BotanyGardens;
import dev.mikey.botanygardens.block.menu.GardenScreen;
import dev.mikey.botanygardens.compat.dimstorage.DimStorageCompat;
import dev.mikey.botanygardens.init.ModBlockEntities;
import dev.mikey.botanygardens.init.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = BotanyGardens.MODID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.GARDEN.get(), GardenScreen::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.GARDEN.get(), GardenRenderer::new);
        // The Dimensional variant is a distinct BlockEntityType (only populated when DimStorage is
        // loaded) - same class of bug as the energy capability earlier: registering only the base
        // type here left the Dimensional Garden Pot's cells with no renderer at all, so none of its
        // mini-pots/crops ever showed despite ticking and growing correctly.
        if (BotanyGardens.isDimStorageLoaded() && DimStorageCompat.DIM_GARDEN_BE != null) {
            event.registerBlockEntityRenderer(DimStorageCompat.DIM_GARDEN_BE.get(), GardenRenderer::new);
        }
    }
}

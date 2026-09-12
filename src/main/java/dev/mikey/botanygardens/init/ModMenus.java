package dev.mikey.botanygardens.init;

import dev.mikey.botanygardens.BotanyGardens;
import dev.mikey.botanygardens.block.menu.GardenMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BotanyGardens.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<GardenMenu>> GARDEN = MENUS.register("garden",
            () -> IMenuTypeExtension.create((id, inv, buf) -> GardenMenu.clientMenu(id, inv)));
}

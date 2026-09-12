package dev.mtop.gardenpots.init;

import dev.mtop.gardenpots.GardenPots;
import dev.mtop.gardenpots.block.menu.GardenPotMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, GardenPots.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<GardenPotMenu>> GARDEN_POT = MENUS.register("garden_pot",
            () -> IMenuTypeExtension.create((id, inv, buf) -> GardenPotMenu.clientMenu(id, inv)));
}

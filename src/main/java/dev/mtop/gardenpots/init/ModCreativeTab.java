package dev.mtop.gardenpots.init;

import dev.mtop.gardenpots.GardenPots;
import dev.mtop.gardenpots.compat.dimstorage.DimStorageCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GardenPots.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GARDEN_POTS = TABS.register("garden_pots",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gardenpots"))
                    .icon(() -> new ItemStack(ModItems.GARDEN_POT.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.GARDEN_POT.get());
                        if (DimStorageCompat.DIM_GARDEN_POT_ITEM != null) {
                            output.accept(DimStorageCompat.DIM_GARDEN_POT_ITEM.get());
                        }
                        output.accept(ModItems.SPEED_UPGRADE.get());
                        output.accept(ModItems.OUTPUT_UPGRADE.get());
                        output.accept(ModItems.ENERGY_UPGRADE.get());
                        output.accept(ModItems.FORTUNE_UPGRADE.get());
                    })
                    .build());
}

package dev.mikey.botanygardens.init;

import dev.mikey.botanygardens.BotanyGardens;
import dev.mikey.botanygardens.compat.dimstorage.DimStorageCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BotanyGardens.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GARDENS = TABS.register("gardens",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.botanygardens"))
                    .icon(() -> new ItemStack(ModItems.GARDEN.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.GARDEN.get());
                        if (DimStorageCompat.DIM_GARDEN_ITEM != null) {
                            output.accept(DimStorageCompat.DIM_GARDEN_ITEM.get());
                        }
                        output.accept(ModItems.SPEED_UPGRADE.get());
                        output.accept(ModItems.OUTPUT_UPGRADE.get());
                        output.accept(ModItems.ENERGY_UPGRADE.get());
                        output.accept(ModItems.FORTUNE_UPGRADE.get());
                    })
                    .build());
}

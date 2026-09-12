package dev.mikey.botanygardens.init;

import dev.mikey.botanygardens.BotanyGardens;
import dev.mikey.botanygardens.block.GardenBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Dimensional Garden Pot variant (compat/dimstorage) registers its own block the same way, from
 * its own compat package, once that phase lands - kept out of here so this class has no reason to
 * ever import DimStorage types.
 */
public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BotanyGardens.MODID);

    public static final DeferredBlock<GardenBlock> GARDEN = BLOCKS.registerBlock("garden",
            GardenBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(1.5f, 4.2f).noOcclusion());
}

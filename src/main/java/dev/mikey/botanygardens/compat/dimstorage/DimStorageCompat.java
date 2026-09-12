package dev.mikey.botanygardens.compat.dimstorage;

import dev.mikey.botanygardens.BotanyGardens;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the Dimensional Garden Pot block/item/block-entity, but only when DimStorage is actually
 * loaded - {@link #init} early-returns before any {@code DimGardenBlock}/{@code DimGardenBlockEntity}
 * method reference is ever resolved, so those classes (and the {@code edivad.dimstorage} types they
 * import) are only classloaded when the dependency they need is present.
 */
public class DimStorageCompat {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BotanyGardens.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BotanyGardens.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BotanyGardens.MODID);

    public static DeferredBlock<DimGardenBlock> DIM_GARDEN;
    public static DeferredItem<BlockItem> DIM_GARDEN_ITEM;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<DimGardenBlockEntity>> DIM_GARDEN_BE;

    public static void init(IEventBus modBus) {
        if (!BotanyGardens.isDimStorageLoaded()) {
            return;
        }

        DIM_GARDEN = BLOCKS.registerBlock("dimensional_garden", DimGardenBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5f, 4.2f).noOcclusion());
        DIM_GARDEN_ITEM = ITEMS.registerSimpleBlockItem(DIM_GARDEN);
        DIM_GARDEN_BE = BLOCK_ENTITIES.register("dimensional_garden",
                () -> BlockEntityType.Builder.of(DimGardenBlockEntity::new, DIM_GARDEN.get()).build(null));

        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}

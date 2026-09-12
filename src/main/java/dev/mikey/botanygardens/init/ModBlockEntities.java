package dev.mikey.botanygardens.init;

import dev.mikey.botanygardens.BotanyGardens;
import dev.mikey.botanygardens.block.entity.GardenBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BotanyGardens.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GardenBlockEntity>> GARDEN = BLOCK_ENTITIES.register("garden",
            () -> BlockEntityType.Builder.of(GardenBlockEntity::new, ModBlocks.GARDEN.get()).build(null));
}

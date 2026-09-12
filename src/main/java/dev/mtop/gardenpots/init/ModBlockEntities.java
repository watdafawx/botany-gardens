package dev.mtop.gardenpots.init;

import dev.mtop.gardenpots.GardenPots;
import dev.mtop.gardenpots.block.entity.GardenPotBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GardenPots.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GardenPotBlockEntity>> GARDEN_POT = BLOCK_ENTITIES.register("garden_pot",
            () -> BlockEntityType.Builder.of(GardenPotBlockEntity::new, ModBlocks.GARDEN_POT.get()).build(null));
}

package dev.mtop.gardenpots.compat.dimstorage;

import dev.mtop.gardenpots.block.GardenPotBlock;
import dev.mtop.gardenpots.block.entity.GardenPotBlockEntity;
import edivad.dimstorage.api.Frequency;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Right-clicking with a bound Dim Tablet (or another Dim-aware item) binds this pot's shared output
 * to that frequency instead of opening the grid GUI - mirrors {@code DimensionalBotanyPotBlock}'s own
 * {@code useItemOn} upstream.
 */
public class DimGardenPotBlock extends GardenPotBlock {

    public DimGardenPotBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new DimGardenPotBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, DimStorageCompat.DIM_GARDEN_POT_BE.get(), GardenPotBlockEntity::tick);
    }

    @NotNull
    @Override
    protected ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (DimStorageSupport.isValidFrequencyItem(stack) && level.getBlockEntity(pos) instanceof DimGardenPotBlockEntity pot) {
            final Frequency current = pot.getFrequency();
            if (current.hasOwner() && !current.canAccess(player)) {
                if (!level.isClientSide) {
                    player.sendSystemMessage(Component.translatable("gardenpots.chat.not_owner").withStyle(ChatFormatting.RED));
                }
                return ItemInteractionResult.FAIL;
            }
            final Frequency newFreq = DimStorageSupport.getFrequency(stack);
            if (!current.equals(newFreq) && !level.isClientSide) {
                pot.setFrequency(newFreq);
                player.sendSystemMessage(Component.translatable("gardenpots.chat.frequency_changed", newFreq.channel()));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}

package dev.mtop.gardenpots.compat.jade;

import dev.mtop.gardenpots.GardenPots;
import dev.mtop.gardenpots.block.GardenPotBlock;
import dev.mtop.gardenpots.block.entity.GardenPotBlockEntity;
import dev.mtop.gardenpots.block.entity.GardenPotCell;
import dev.mtop.gardenpots.compat.dimstorage.DimGardenPotBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

import java.util.List;

/**
 * Like every other pot mod in this family, a Jade hover tooltip - cells growing/total and, if the
 * block has any energy capacity, its FE buffer. Reads whatever the client already has synced (our
 * {@code getUpdateTag} sends the whole block entity), so no {@code IServerDataProvider} round trip
 * is needed.
 *
 * <p>Discovered via Jade's own {@code @WailaPlugin} annotation scan, not FML mod-loading - Jade only
 * instantiates this class if Jade itself is present, so no {@code ModList} guard is needed here
 * (unlike the {@code compat.powerpots}/{@code compat.dimstorage} packages, which use NeoForge's own
 * capability/registration events and do need one).
 */
@WailaPlugin
public class GardenPotJadePlugin implements IWailaPlugin, IBlockComponentProvider {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(GardenPots.MODID, "cells");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(this, GardenPotBlock.class);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        final BlockEntity be = accessor.getBlockEntity();
        if (!(be instanceof GardenPotBlockEntity pot)) {
            return;
        }

        int growing = 0;
        for (int i = 0; i < GardenPotBlockEntity.GRID_SIZE; i++) {
            final GardenPotCell cell = pot.getCell(i);
            if (!cell.isEmpty() && cell.enabled) {
                growing++;
            }
        }
        tooltip.add(Component.translatable("gardenpots.jade.cells", growing, GardenPotBlockEntity.GRID_SIZE));

        final IElementHelper helper = IElementHelper.get();
        for (int i = 0; i < GardenPotBlockEntity.GRID_SIZE; i++) {
            final GardenPotCell cell = pot.getCell(i);
            if (cell.isEmpty() || cell.seed.isEmpty()) {
                continue;
            }
            final Component label = cell.enabled
                    ? Component.literal(Math.round(pot.getGrowthProgress(i) * 100f) + "%")
                    : Component.translatable("gardenpots.jade.paused").withStyle(ChatFormatting.RED);
            tooltip.add(List.of(helper.smallItem(cell.seed), helper.text(label)));
        }

        if (pot.energy.getMaxEnergyStored() > 0) {
            tooltip.add(Component.translatable("gardenpots.jade.energy", pot.energy.getEnergyStored(), pot.energy.getMaxEnergyStored()));
        }

        // isDimStorageLoaded() must short-circuit before the instanceof - DimGardenPotBlockEntity
        // pulls in edivad.dimstorage types that don't exist without that mod present.
        if (GardenPots.isDimStorageLoaded() && pot instanceof DimGardenPotBlockEntity dimPot) {
            tooltip.add(Component.translatable("gardenpots.jade.frequency", dimPot.getFrequency().channel()));
        }
    }
}

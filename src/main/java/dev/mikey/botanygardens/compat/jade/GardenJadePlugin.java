package dev.mikey.botanygardens.compat.jade;

import dev.mikey.botanygardens.BotanyGardens;
import dev.mikey.botanygardens.block.GardenBlock;
import dev.mikey.botanygardens.block.entity.GardenBlockEntity;
import dev.mikey.botanygardens.block.entity.GardenCell;
import dev.mikey.botanygardens.compat.dimstorage.DimGardenBlockEntity;
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
public class GardenJadePlugin implements IWailaPlugin, IBlockComponentProvider {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(BotanyGardens.MODID, "cells");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(this, GardenBlock.class);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        final BlockEntity be = accessor.getBlockEntity();
        if (!(be instanceof GardenBlockEntity pot)) {
            return;
        }

        int growing = 0;
        for (int i = 0; i < GardenBlockEntity.GRID_SIZE; i++) {
            final GardenCell cell = pot.getCell(i);
            if (!cell.isEmpty() && cell.enabled) {
                growing++;
            }
        }
        tooltip.add(Component.translatable("botanygardens.jade.cells", growing, GardenBlockEntity.GRID_SIZE));

        final IElementHelper helper = IElementHelper.get();
        for (int i = 0; i < GardenBlockEntity.GRID_SIZE; i++) {
            final GardenCell cell = pot.getCell(i);
            if (cell.isEmpty() || cell.seed.isEmpty()) {
                continue;
            }
            final Component label = cell.enabled
                    ? Component.literal(Math.round(pot.getGrowthProgress(i) * 100f) + "%")
                    : Component.translatable("botanygardens.jade.paused").withStyle(ChatFormatting.RED);
            tooltip.add(List.of(helper.smallItem(cell.seed), helper.text(label)));
        }

        if (pot.energy.getMaxEnergyStored() > 0) {
            tooltip.add(Component.translatable("botanygardens.jade.energy", pot.energy.getEnergyStored(), pot.energy.getMaxEnergyStored()));
        }

        // isDimStorageLoaded() must short-circuit before the instanceof - DimGardenBlockEntity
        // pulls in edivad.dimstorage types that don't exist without that mod present.
        if (BotanyGardens.isDimStorageLoaded() && pot instanceof DimGardenBlockEntity dimPot) {
            tooltip.add(Component.translatable("botanygardens.jade.frequency", dimPot.getFrequency().channel()));
        }
    }
}

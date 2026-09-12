package dev.mikey.botanygardens.block;

import dev.mikey.botanygardens.BotanyGardens;
import dev.mikey.botanygardens.block.entity.GardenCell;
import dev.mikey.botanygardens.compat.powerpots.PowerPotCellSupport;
import dev.mikey.botanygardens.upgrade.GardenUpgradeSupport;
import net.darkhax.botanypots.common.api.context.BotanyPotContext;
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop;
import net.darkhax.botanypots.common.api.data.recipes.soil.Soil;
import net.darkhax.botanypots.common.impl.BotanyPotsMod;
import net.darkhax.botanypots.common.impl.Helpers;
import net.darkhax.botanypots.common.impl.block.BotanyPotBlock;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * {@code net.darkhax.botanypots.common.impl.Helpers}' growth/yield formulas, adapted for a
 * {@link GardenCell} instead of a real {@code BotanyPotBlockEntity}.
 *
 * <p>Can't just call {@code Helpers.getRequiredGrowthTicks}/{@code getTotalYield} directly: both
 * special-case {@code context instanceof BlockEntityContext} to pull the growth/yield modifier off
 * the real placed {@code BotanyPotBlock}, and our {@link dev.mikey.botanygardens.block.context.CellContext}
 * will never satisfy that check. Everything else in the formula (global modifier, soil modifier,
 * harvest-tool efficiency enchant/attribute) is reused verbatim from {@link Helpers} - only the
 * block-modifier term is re-sourced from {@link GardenCell#getPotBlock()}.
 */
public class GardenHelpers {

    public static int getRequiredGrowthTicks(GardenCell cell, BotanyPotContext context, Level level, Crop crop, @Nullable Soil soil, List<ItemStack> upgrades) {
        final float cropTime = crop.getRequiredGrowthTicks(context, level);
        float growthModifier = BotanyPotsMod.CONFIG.get().gameplay.global_growth_modifier;
        growthModifier += soil != null ? soil.getGrowthModifier(context, level) : 0f;
        growthModifier += Helpers.efficiencyModifier(level.registryAccess(), context.getHarvestItem());
        final BotanyPotBlock potBlock = cell.getPotBlock();
        if (potBlock != null) {
            growthModifier += potBlock.getGrowthModifier(context, level, crop, soil);
        }
        if (BotanyGardens.isPowerPotsLoaded() && PowerPotCellSupport.isPowerPot(cell.potKind)) {
            // PowerPotBlock#getGrowthModifier always returns 0 (its real speed boost lives on the
            // BlockEntity, which a virtual cell doesn't have) - read the tier directly instead. This is
            // the Power Pot's *own* bonus, separate from (and stacked with) the Garden Pot's shared
            // Speed Upgrade just below, which applies to every cell regardless of pot kind.
            growthModifier += PowerPotCellSupport.getTierSpeedModifier(cell.potKind) - 1f;
        }
        growthModifier += GardenUpgradeSupport.getSpeedModifier(upgrades) - 1f;
        return Mth.floor(cropTime / growthModifier);
    }

    public static float getTotalYield(GardenCell cell, BotanyPotContext context, Level level, Crop crop, @Nullable Soil soil, List<ItemStack> upgrades) {
        final float yieldScale = crop.getYieldScale(context, level);
        float yield = crop.getBaseYield(context, level);
        final BotanyPotBlock potBlock = cell.getPotBlock();
        if (potBlock != null) {
            yield += yieldScale * potBlock.getYieldModifier(context, level, crop, soil);
        }
        if (soil != null) {
            yield += yieldScale * soil.getYieldModifier(context, level);
        }
        if (BotanyGardens.isPowerPotsLoaded() && PowerPotCellSupport.isPowerPot(cell.potKind)) {
            // The Power Pot's own tier bonus - separate from (and stacked with) the shared Output
            // Upgrade just below, which applies to every cell regardless of pot kind.
            yield += yieldScale * (PowerPotCellSupport.getTierYieldMultiplier(cell.potKind) - 1f);
        }
        yield += yieldScale * (GardenUpgradeSupport.getYieldMultiplier(upgrades) - 1f);
        yield += yieldScale * (float) Helpers.getAttributeValue(Helpers.YIELD_MOD_ATTRIBUTE.get(), context.getHarvestItem(), 0f);
        return yield;
    }

    public static int getLootRolls(GardenCell cell, BotanyPotContext context, Level level, Crop crop, @Nullable Soil soil, List<ItemStack> upgrades) {
        return Helpers.determineRollCount(getTotalYield(cell, context, level, crop, soil, upgrades), level.getRandom());
    }
}

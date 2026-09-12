package dev.mikey.botanygardens.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.mikey.botanygardens.block.context.CellContext;
import dev.mikey.botanygardens.block.entity.GardenBlockEntity;
import dev.mikey.botanygardens.block.entity.GardenCell;
import net.darkhax.botanypots.common.api.data.display.render.DisplayRenderer;
import net.darkhax.botanypots.common.api.data.display.types.Display;
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop;
import net.darkhax.botanypots.common.api.data.recipes.soil.Soil;
import net.darkhax.botanypots.common.impl.block.BotanyPotBlock;
import net.darkhax.botanypots.common.impl.data.display.types.BasicOptions;
import net.darkhax.botanypots.common.impl.data.display.types.SimpleDisplayState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Renders every growing cell's crop clustered together near the pot's center - inside the same
 * inner-cavity footprint a real pot's one crop would occupy - reusing BotanyPots'
 * {@link DisplayRenderer} exactly like {@code BotanyPotRenderer} does for a real pot. The block looks
 * like one normal pot from outside; this is what turns "x cells are growing" into a visible bunch of
 * x small crops instead of a single plant.
 *
 * <p>{@code DisplayRenderer.renderState}'s {@code BotanyPotBlockEntity pot} parameter is passed
 * through to built-in renderer implementations (Simple/Entity/Phased/TexturedCube) but never
 * actually dereferenced by any of them - confirmed by reading their source - so it's safe to pass
 * {@code null} here instead of needing a real {@code BotanyPotBlockEntity} for every cell.
 */
public class GardenRenderer implements BlockEntityRenderer<GardenBlockEntity> {

    // Matches the real pot's inner-cavity width (BotanyPotBlock.SHAPE's "inner" box is 3..13 of 16 -
    // i.e. 0.625 wide) so a 3x3 cluster of small crops sits exactly where one normal crop would.
    private static final double CLUSTER_WIDTH = 0.625;
    private static final double CLUSTER_ORIGIN = 0.5 - CLUSTER_WIDTH / 2.0;
    private static final float CLUSTER_SCALE = (float) (CLUSTER_WIDTH / 3.0);

    private final BlockEntityRendererProvider.Context renderContext;

    public GardenRenderer(BlockEntityRendererProvider.Context ctx) {
        this.renderContext = ctx;
    }

    @Override
    public void render(GardenBlockEntity be, float tickDelta, @NotNull PoseStack pose, @NotNull MultiBufferSource bufferSource, int light, int overlay) {
        final Level level = be.getLevel();
        if (level == null) {
            return;
        }
        final BlockPos pos = be.getBlockPos();

        for (int i = 0; i < GardenBlockEntity.GRID_SIZE; i++) {
            final GardenCell cell = be.getCell(i);
            if (cell.isEmpty()) {
                continue;
            }

            final CellContext context = be.getContext(i);
            final Crop crop = context.getCrop();
            if (crop == null) {
                continue;
            }
            final Soil soil = context.getSoil();
            final float progress = be.getGrowthProgress(i);
            final boolean isGrowing = crop.isGrowthSustained(context, level);
            final float cropScale = 0.40f + Mth.clamp(0.60f * progress, 0f, 0.60f);

            pose.pushPose();
            pose.translate(CLUSTER_ORIGIN + CLUSTER_SCALE * (i % 3), 0, CLUSTER_ORIGIN + CLUSTER_SCALE * (i / 3));
            pose.scale(CLUSTER_SCALE, CLUSTER_SCALE, CLUSTER_SCALE);

            // The actual pot-kind block the player inserted, shrunk down as its own mini model - not
            // just a flat soil/crop overlay - so the cluster reads as "x little pots", matching that
            // block's own color/shape (a real Hopper Pot, an Elite Pot, ...) exactly.
            final BotanyPotBlock potBlock = cell.getPotBlock();
            if (potBlock != null) {
                this.renderContext.getBlockRenderDispatcher().renderSingleBlock(potBlock.defaultBlockState(), pose, bufferSource, light, overlay);
            }

            float lastOffset = 0.3984375f;
            if (isGrowing) {
                for (Display state : crop.getDisplayState(context, level)) {
                    lastOffset = DisplayRenderer.renderState(this.renderContext, state, pose, level, pos, tickDelta, bufferSource, light, overlay, null, progress, cropScale, lastOffset);
                }
            }

            final ItemStack soilItem = cell.soil;
            if (!soilItem.isEmpty()) {
                final Display soilRender = this.resolveSoilDisplay(context, level, soil, soilItem, crop, isGrowing);
                if (soilRender != null) {
                    pose.pushPose();
                    pose.scale(1f, 0.6375f, 1f);
                    DisplayRenderer.renderState(this.renderContext, soilRender, pose, level, pos, tickDelta, bufferSource, light, overlay, null, progress, 1f, 0f);
                    pose.popPose();
                }
            }

            pose.popPose();
        }
    }

    @Nullable
    private Display resolveSoilDisplay(CellContext context, Level level, @Nullable Soil soil, ItemStack soilItem, Crop crop, boolean isGrowing) {
        if (soil != null) {
            return soil.getDisplay(context, level);
        }
        if (isGrowing && soilItem.getItem() instanceof BlockItem blockItem) {
            return new SimpleDisplayState(blockItem.getBlock().defaultBlockState(), BasicOptions.ofDefault());
        }
        return null;
    }

    @Override
    public int getViewDistance() {
        return 32;
    }
}

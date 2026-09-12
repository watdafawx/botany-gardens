package dev.mikey.botanygardens.block.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.mikey.botanygardens.BotanyGardens;
import dev.mikey.botanygardens.block.entity.GardenBlockEntity;
import dev.mikey.botanygardens.compat.dimstorage.DimGardenBlockEntity;
import dev.mikey.botanygardens.upgrade.GardenUpgradeSupport;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Garden-themed placeholder background (filled rectangles + bevel, no texture asset) - functional,
 * not final art, but styled to read as "potting shed" rather than vanilla gray. A tiled procedural
 * wood texture was tried here and reverted - didn't read well at actual GUI scale regardless of how
 * subtle the grain was tuned. Slot positions come straight from {@link GardenMenu}'s layout
 * constants.
 */
public class GardenScreen extends AbstractContainerScreen<GardenMenu> {

    private static final int PANEL_COLOR = 0xFFD7C9A3;
    private static final int PANEL_BORDER = 0xFF4E3B26;
    private static final int SLOT_COLOR = 0xFF6B4F3B;
    private static final int POT_KIND_SLOT_COLOR = 0xFFC9A227;
    private static final int OUTPUT_SLOT_COLOR = 0xFF8A6D4F;
    private static final int SLOT_LIGHT_EDGE = 0xFF8E7454;
    private static final int SLOT_DARK_EDGE = 0xFF2E2115;
    // Lighter than SLOT_DARK_EDGE - just the soil/seed/tool slots' outline, so they read a touch
    // softer than the pot-kind/output/upgrade slots around them.
    private static final int SOFT_SLOT_EDGE = 0xFF5E4A36;
    private static final int LABEL_COLOR = 0xFF4E3B26;
    private static final int BAR_BG_COLOR = 0xFF3E2F22;
    private static final int BAR_FILL_COLOR = 0xFF5DAE4C;
    private static final int BAR_DISABLED_COLOR = 0xFF8A7B6B;
    private static final int BAR_HEIGHT = 3;
    private static final int TOGGLE_ON_COLOR = 0xFF5DAE4C;
    private static final int TOGGLE_OFF_COLOR = 0xFFA33A2E;
    private static final int TOGGLE_SIZE = 14;
    private static final int UPGRADE_VALUE_COLOR = 0xFF4E3B26;
    private static final int UPGRADE_EMPTY_COLOR = 0xFF8F7C68;

    private static final String[] UPGRADE_NAMES = {"Speed", "Output", "Energy", "Fortune"};

    public GardenScreen(GardenMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = GardenMenu.PANEL_WIDTH;
        this.imageHeight = GardenMenu.PANEL_HEIGHT;
        this.inventoryLabelY = GardenMenu.INV_LABEL_Y;
        this.titleLabelX = GardenMenu.GRID_LEFT;
    }

    /** {@code AbstractContainerScreen.render()} never calls {@code this.renderTooltip(...)} itself -
     * every vanilla screen (Hopper, Furnace, ...) overrides {@code render()} separately and calls it
     * after {@code super.render(...)}. Without this override our {@code renderTooltip} below was dead
     * code, never invoked - which is why every tooltip in this GUI, including vanilla's own item
     * hover tooltip, silently stopped appearing. */
    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.disableBlend();
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, PANEL_COLOR);
        graphics.renderOutline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, PANEL_BORDER);
        graphics.renderOutline(this.leftPos + 2, this.topPos + 2, this.imageWidth - 4, this.imageHeight - 4, SLOT_LIGHT_EDGE);

        for (int i = 0; i < GardenBlockEntity.GRID_SIZE; i++) {
            final int col = i % 3;
            final int row = i / 3;
            final int x = GardenMenu.GRID_LEFT + col * (GardenMenu.CLUSTER_WIDTH + GardenMenu.CLUSTER_GAP);
            final int y = GardenMenu.GRID_TOP + row * (GardenMenu.SLOT + GardenMenu.CLUSTER_GAP);
            this.slotSquare(graphics, x, y, POT_KIND_SLOT_COLOR);
            for (int s = 1; s < GardenBlockEntity.CLUSTER_SLOTS; s++) {
                this.slotSquare(graphics, x + s * GardenMenu.SLOT, y, SLOT_COLOR, SOFT_SLOT_EDGE);
            }
            final int progress = this.menu.getCellProgress(i);
            this.progressBar(graphics, x, y + GardenMenu.SLOT + 1, GardenBlockEntity.CLUSTER_SLOTS * GardenMenu.SLOT, progress);
            this.toggleButton(graphics, x + GardenMenu.TOGGLE_OFFSET, y, progress != CellProgressData.DISABLED);
        }

        // A short label above the output bank - plain and horizontal now that the bank is left-aligned
        // (not centered under the wider pot grid), so there's no longer a cramped margin to its left
        // for the old vertical version.
        graphics.drawString(this.font, Component.translatable("botanygardens.gui.output"),
                this.leftPos + GardenMenu.OUTPUT_LEFT, this.topPos + GardenMenu.OUTPUT_TOP - 10, LABEL_COLOR, false);

        for (int i = 0; i < GardenBlockEntity.OUTPUT_SLOTS; i++) {
            final int x = GardenMenu.OUTPUT_LEFT + (i % GardenMenu.OUTPUT_COLUMNS) * GardenMenu.SLOT;
            final int y = GardenMenu.OUTPUT_TOP + (i / GardenMenu.OUTPUT_COLUMNS) * GardenMenu.SLOT;
            this.slotSquare(graphics, x, y, OUTPUT_SLOT_COLOR);
        }

        // The Garden Pot's own 4 shared upgrade slots - stacked vertically to the right of the output
        // bank (see GardenMenu.UPGRADE_COL_LEFT/TOP), one stat line drawn directly beside each in
        // renderLabels instead of a routed connector - simpler, and there's no "which is which"
        // ambiguity since the text sits right next to its own slot, not off in a corner. Always drawn
        // now - these are our own items, not gated on any optional mod.
        for (int i = 0; i < GardenBlockEntity.UPGRADE_SLOTS; i++) {
            this.slotSquare(graphics, GardenMenu.UPGRADE_COL_LEFT, GardenMenu.UPGRADE_COL_TOP + i * GardenMenu.SLOT);
        }

        // Same X as the output bank - both are 9 wide, so they line up and read as one column.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.slotSquare(graphics, GardenMenu.OUTPUT_LEFT + col * GardenMenu.SLOT, GardenMenu.INV_TOP + row * GardenMenu.SLOT);
            }
        }
        for (int col = 0; col < 9; col++) {
            this.slotSquare(graphics, GardenMenu.OUTPUT_LEFT + col * GardenMenu.SLOT, GardenMenu.INV_TOP + 3 * GardenMenu.SLOT + 8);
        }

        // A quick separator between the garden's own inventory (everything above) and the player's -
        // an engraved-looking line (dark over light) rather than just bare gap, same bevel idea as
        // slotSquare's own edges. GardenMenu.SEPARATOR_Y is measured from the *upgrade column's*
        // bottom (4 rows), not the output bank's (3 rows) - the column is a row taller, so anchoring
        // this to the output bank instead drew the line straight through the 4th upgrade slot.
        final int sepY = this.topPos + GardenMenu.SEPARATOR_Y;
        final int sepLeft = this.leftPos + GardenMenu.GRID_LEFT;
        final int sepRight = this.leftPos + this.imageWidth - GardenMenu.GRID_LEFT;
        graphics.fill(sepLeft, sepY, sepRight, sepY + 1, SLOT_DARK_EDGE);
        graphics.fill(sepLeft, sepY + 1, sepRight, sepY + 2, SLOT_LIGHT_EDGE);

        // Undo the disableBlend() above - left disabled, it also suppressed every alpha-blended draw
        // for the rest of the frame, including vanilla's own item tooltips (renderComponentTooltip and
        // the item-hover tooltip both rely on blending being on).
        RenderSystem.enableBlend();
    }

    /** Stat text drawn directly beside each vertical upgrade slot (see {@link GardenMenu#UPGRADE_COL_LEFT}) -
     * no separate icon or connector needed, since "next to its own slot" already answers which is
     * which. Occupied slots show their current value, empty ones show a dim placeholder so all 4
     * types are always visible in the same order. */
    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        // FE indicator right under the title - shown whenever the pot has any energy capacity at all
        // (always true today; the port isn't gated on PowerPots being installed, same as Jade's own
        // energy line).
        final GardenBlockEntity pot = this.menu.getPot();
        int statusY = 16;
        if (pot != null && pot.energy.getMaxEnergyStored() > 0) {
            graphics.drawString(this.font, Component.translatable("botanygardens.gui.energy", pot.energy.getEnergyStored(), pot.energy.getMaxEnergyStored()),
                    GardenMenu.GRID_LEFT, statusY, LABEL_COLOR, false);
            statusY += 10;
        }
        // Dimensional Garden Pot only - which Dim Storage frequency this pot's output is bound to.
        if (BotanyGardens.isDimStorageLoaded() && pot instanceof DimGardenBlockEntity dimPot) {
            graphics.drawString(this.font, Component.translatable("botanygardens.gui.frequency", dimPot.getFrequency().channel()),
                    GardenMenu.GRID_LEFT, statusY, LABEL_COLOR, false);
        }

        for (int i = 0; i < GardenBlockEntity.UPGRADE_SLOTS; i++) {
            final ItemStack stack = this.menu.getUpgradeStack(i);
            final String value;
            final int color;
            if (stack.isEmpty()) {
                value = "-";
                color = UPGRADE_EMPTY_COLOR;
            } else {
                final int percent = Math.round(GardenUpgradeSupport.getSlotBonusPercent(stack));
                value = (percent >= 0 ? "+" : "") + percent + "%";
                color = UPGRADE_VALUE_COLOR;
            }
            final String line = UPGRADE_NAMES[i] + " " + value;
            final int y = GardenMenu.UPGRADE_COL_TOP + i * GardenMenu.SLOT + 5;
            graphics.drawString(this.font, line, GardenMenu.UPGRADE_TEXT_LEFT, y, color, false);
        }
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < GardenBlockEntity.GRID_SIZE; i++) {
            if (this.isHoveringToggle(i, mouseX, mouseY)) {
                final boolean enabled = this.menu.getCellProgress(i) != CellProgressData.DISABLED;
                graphics.renderComponentTooltip(this.font, List.of(
                        Component.translatable(enabled ? "botanygardens.tooltip.enabled" : "botanygardens.tooltip.disabled").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)
                ), mouseX, mouseY);
                return;
            }
        }
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()
                && this.hoveredSlot.index < GardenBlockEntity.CELL_SLOTS
                && this.hoveredSlot.index % GardenBlockEntity.CLUSTER_SLOTS == 0) {
            graphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("botanygardens.tooltip.pot_kind").withStyle(ChatFormatting.YELLOW),
                    Component.translatable("botanygardens.tooltip.pot_kind.desc").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
            return;
        }
        final int upgradeBase = GardenBlockEntity.CELL_SLOTS + GardenBlockEntity.OUTPUT_SLOTS;
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()
                && this.hoveredSlot.index >= upgradeBase && this.hoveredSlot.index < upgradeBase + GardenBlockEntity.UPGRADE_SLOTS) {
            final String[] names = {"speed", "output", "energy", "fortune"};
            final int slotIndex = this.hoveredSlot.index - upgradeBase;
            final List<Component> lines = new ArrayList<>(List.of(
                    Component.translatable("botanygardens.tooltip.upgrade." + names[slotIndex]).withStyle(ChatFormatting.AQUA),
                    Component.translatable("botanygardens.tooltip.upgrade.desc").withStyle(ChatFormatting.GRAY)
            ));
            if (slotIndex == 2) {
                lines.add(Component.translatable("botanygardens.tooltip.upgrade.energy.desc").withStyle(ChatFormatting.DARK_GRAY));
            }
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < GardenBlockEntity.GRID_SIZE; i++) {
            if (this.isHoveringToggle(i, (int) mouseX, (int) mouseY)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHoveringToggle(int cellIndex, int mouseX, int mouseY) {
        final int col = cellIndex % 3;
        final int row = cellIndex / 3;
        final int x = this.leftPos + GardenMenu.GRID_LEFT + col * (GardenMenu.CLUSTER_WIDTH + GardenMenu.CLUSTER_GAP) + GardenMenu.TOGGLE_OFFSET;
        final int y = this.topPos + GardenMenu.GRID_TOP + row * (GardenMenu.SLOT + GardenMenu.CLUSTER_GAP);
        return mouseX >= x && mouseX < x + TOGGLE_SIZE && mouseY >= y && mouseY < y + TOGGLE_SIZE;
    }

    // Pixel-drawn check mark (short down-stroke, long up-stroke) and X mark, each a path of 2x2 dots
    // in the toggle box's own 14x14 local space - graphics.fill only draws axis-aligned rects, so a
    // true diagonal isn't possible; stepping single pixels along the diagonal like this is the usual
    // pixel-art way to fake one.
    private static final int[][] CHECK_MARK_DOTS = {
            {2, 6}, {3, 7}, {4, 8}, {5, 9}, {6, 8}, {7, 7}, {8, 6}, {9, 5}, {10, 4}, {11, 3}
    };
    private static final int[][] X_MARK_DOTS = {
            {2, 2}, {3, 3}, {4, 4}, {5, 5}, {6, 6}, {7, 7}, {8, 8}, {9, 9}, {10, 10}, {11, 11},
            {11, 2}, {10, 3}, {9, 4}, {8, 5}, {7, 6}, {6, 7}, {5, 8}, {4, 9}, {3, 10}, {2, 11}
    };
    private static final int MARK_COLOR = 0xFFF5EEDD;

    /** Enable/disable indicator for one cell - green with a check mark when on, red with an X when
     * off, click toggles it (see {@link GardenMenu#clickMenuButton}). */
    private void toggleButton(GuiGraphics graphics, int x, int y, boolean enabled) {
        final int left = this.leftPos + x + (GardenMenu.SLOT - TOGGLE_SIZE) / 2;
        final int top = this.topPos + y + (GardenMenu.SLOT - TOGGLE_SIZE) / 2;
        graphics.fill(left - 1, top - 1, left + TOGGLE_SIZE + 1, top + TOGGLE_SIZE + 1, SLOT_DARK_EDGE);
        graphics.fill(left, top, left + TOGGLE_SIZE, top + TOGGLE_SIZE, enabled ? TOGGLE_ON_COLOR : TOGGLE_OFF_COLOR);
        for (final int[] dot : enabled ? CHECK_MARK_DOTS : X_MARK_DOTS) {
            graphics.fill(left + dot[0], top + dot[1], left + dot[0] + 2, top + dot[1] + 2, MARK_COLOR);
        }
    }

    private void progressBar(GuiGraphics graphics, int x, int y, int width, int percent) {
        final int left = this.leftPos + x;
        final int top = this.topPos + y;
        if (percent == CellProgressData.DISABLED) {
            graphics.fill(left, top, left + width, top + BAR_HEIGHT, BAR_DISABLED_COLOR);
            return;
        }
        graphics.fill(left, top, left + width, top + BAR_HEIGHT, BAR_BG_COLOR);
        final int filled = Math.round(width * Math.clamp(percent, 0, 100) / 100f);
        if (filled > 0) {
            graphics.fill(left, top, left + filled, top + BAR_HEIGHT, BAR_FILL_COLOR);
        }
    }

    private void slotSquare(GuiGraphics graphics, int x, int y) {
        this.slotSquare(graphics, x, y, SLOT_COLOR);
    }

    private void slotSquare(GuiGraphics graphics, int x, int y, int color) {
        this.slotSquare(graphics, x, y, color, SLOT_DARK_EDGE);
    }

    /** A slightly inset, beveled square - dark top/left, light bottom/right - so slots read as
     * pressed into the panel instead of flat rectangles. {@code outlineColor} is the outer border
     * only (the inner bevel highlight is always {@link #SLOT_LIGHT_EDGE}) - soil/seed/tool slots use
     * a lighter one than the pot-kind/output/upgrade slots around them. */
    private void slotSquare(GuiGraphics graphics, int x, int y, int color, int outlineColor) {
        final int left = this.leftPos + x - 1;
        final int top = this.topPos + y - 1;
        graphics.fill(left, top, left + 18, top + 18, outlineColor);
        graphics.fill(left + 1, top + 1, left + 17, top + 17, color);
        graphics.fill(left + 1, top + 16, left + 17, top + 17, SLOT_LIGHT_EDGE);
        graphics.fill(left + 16, top + 1, left + 17, top + 17, SLOT_LIGHT_EDGE);
    }
}

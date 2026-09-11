package com.codex.blacklight.client.screen;

import com.codex.blacklight.client.tuning.ClawTransformSettings;
import com.codex.blacklight.client.tuning.ClawTransformSettings.HandSide;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Live proportion editor; joint positions stay fixed to the animated skeleton. */
public final class ClawTunerScreen extends Screen {
    private static final int PANEL_WIDTH = 218;
    private static final int PANEL_HEIGHT = 171;
    private HandSide selectedSide = HandSide.RIGHT;
    private int panelX;
    private int panelY;
    private String status = "实时预览 · 关闭时自动保存";

    public ClawTunerScreen() {
        super(Component.literal("利爪模型调试器"));
    }

    @Override
    protected void init() {
        ClawTransformSettings.load();
        ClawTransformSettings.setPreview(true);
        panelX = Math.max(4, width - PANEL_WIDTH - 4);
        panelY = Math.max(8, (height - PANEL_HEIGHT) / 2);
        int x = panelX + 8;
        int contentWidth = PANEL_WIDTH - 16;

        addRenderableWidget(Button.builder(tabLabel(HandSide.RIGHT), button -> select(HandSide.RIGHT))
                .bounds(x, panelY + 34, 97, 19).build());
        addRenderableWidget(Button.builder(tabLabel(HandSide.LEFT), button -> select(HandSide.LEFT))
                .bounds(x + 105, panelY + 34, 97, 19).build());

        int y = panelY + 58;
        addSlider(x, y, contentWidth, "手臂粗细", 1.0D, 1.4D,
                () -> ClawTransformSettings.limbWidth(selectedSide),
                value -> ClawTransformSettings.setLimbWidth(selectedSide, value));
        addSlider(x, y + 23, contentWidth, "爪刃长度", 0.7D, 1.5D,
                () -> ClawTransformSettings.talonLength(selectedSide),
                value -> ClawTransformSettings.setTalonLength(selectedSide, value));

        int copyY = y + 47;
        addRenderableWidget(Button.builder(Component.literal("复制此手参数到另一手"), button -> {
            ClawTransformSettings.copy(selectedSide, selectedSide.opposite());
            status = "参数已复制到" + sideName(selectedSide.opposite());
        }).bounds(x, copyY, contentWidth, 19).build());

        int buttonY = copyY + 23;
        addRenderableWidget(Button.builder(Component.literal("保存"), button -> {
            ClawTransformSettings.save();
            status = "左右手参数已保存";
        }).bounds(x, buttonY, 61, 19).build());
        addRenderableWidget(Button.builder(Component.literal("重置此手"), button -> {
            ClawTransformSettings.reset(selectedSide);
            status = sideName(selectedSide) + "已重置";
            rebuildWidgets();
        }).bounds(x + 69, buttonY, 72, 19).build());
        addRenderableWidget(Button.builder(Component.literal("完成"), button -> onClose())
                .bounds(x + 149, buttonY, 53, 19).build());
    }

    private void addSlider(int x, int y, int width, String label, double min, double max,
                           DoubleSupplier getter, DoubleConsumer setter) {
        addRenderableWidget(new ValueSlider(x, y, width, label, min, max, getter, setter));
    }

    private void select(HandSide side) {
        if (selectedSide == side) return;
        selectedSide = side;
        status = "正在调整" + sideName(side);
        rebuildWidgets();
    }

    private Component tabLabel(HandSide side) {
        return Component.literal((selectedSide == side ? "● " : "○ ") + sideName(side));
    }

    private static String sideName(HandSide side) {
        return side == HandSide.RIGHT ? "右爪" : "左爪";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bottom = panelY + PANEL_HEIGHT;
        // 47% opacity keeps labels readable without hiding the character and claw silhouette.
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, bottom, 0x78090B10);
        graphics.renderOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xDDE23A3A);
        graphics.drawString(font, title, panelX + 8, panelY + 9, 0xFFFF6267, false);
        graphics.drawString(font, "K 关闭 · 左右独立 · 关节固定", panelX + 8, panelY + 21,
                0xFFE4E8EF, false);
        graphics.drawString(font, status, panelX + 8, bottom - 13, 0xFFFFFFFF, true);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_K) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        ClawTransformSettings.save();
        ClawTransformSettings.setPreview(false);
        super.onClose();
    }

    @Override
    public void removed() {
        ClawTransformSettings.setPreview(false);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final DoubleConsumer setter;

        private ValueSlider(int x, int y, int width, String label, double min, double max,
                            DoubleSupplier getter, DoubleConsumer setter) {
            super(x, y, width, 19, Component.empty(), normalize(getter.getAsDouble(), min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double current = min + value * (max - min);
            setMessage(Component.literal(label + "  " + String.format(Locale.ROOT, "%.3f", current)));
        }

        @Override
        protected void applyValue() {
            setter.accept(min + value * (max - min));
            updateMessage();
        }

        private static double normalize(double value, double min, double max) {
            return Math.max(0.0D, Math.min(1.0D, (value - min) / (max - min)));
        }
    }
}

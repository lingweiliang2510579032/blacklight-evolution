package com.codex.blacklight.client.screen;

import com.codex.blacklight.movement.MovementTuningSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Transparent live editor for traversal physics; values affect the integrated server immediately. */
public final class MovementTunerScreen extends Screen {
    private static final int PANEL_WIDTH = 230;
    private static final int PANEL_HEIGHT = 354;
    private int panelX;
    private int panelY;
    private String status = "实时生效 · F8 关闭";

    public MovementTunerScreen() { super(Component.literal("黑光移动引擎调试器")); }

    @Override
    protected void init() {
        MovementTuningSettings.load();
        panelX = Math.max(4, width - PANEL_WIDTH - 4);
        panelY = Math.max(4, (height - PANEL_HEIGHT) / 2);
        int x = panelX + 8;
        int w = PANEL_WIDTH - 16;
        int y = panelY + 35;

        add(x, y, w, "跑步加速度", 0.0D, 0.25D,
                MovementTuningSettings::sprintAcceleration, MovementTuningSettings::setSprintAcceleration);
        add(x, y += 23, w, "跑步极速/格每刻", 0.2D, 2.5D,
                MovementTuningSettings::sprintMaxSpeed, MovementTuningSettings::setSprintMaxSpeed);
        add(x, y += 23, w, "最小跳跃力度", 0.3D, 2.0D,
                MovementTuningSettings::jumpMinVelocity, MovementTuningSettings::setJumpMinVelocity);
        add(x, y += 23, w, "最大跳跃力度", 0.5D, 3.0D,
                MovementTuningSettings::jumpMaxVelocity, MovementTuningSettings::setJumpMaxVelocity);
        add(x, y += 23, w, "蓄力时长/刻", 1.0D, 60.0D,
                () -> MovementTuningSettings.jumpChargeTicks(), MovementTuningSettings::setJumpChargeTicks);
        add(x, y += 23, w, "墙面探测距离", 0.1D, 1.2D,
                MovementTuningSettings::wallDetectDistance, MovementTuningSettings::setWallDetectDistance);
        add(x, y += 23, w, "墙跑上升速度", 0.05D, 1.2D,
                MovementTuningSettings::wallClimbVelocity, MovementTuningSettings::setWallClimbVelocity);
        add(x, y += 23, w, "滑翔下落速度", 0.01D, 0.5D,
                MovementTuningSettings::glideFallSpeed, MovementTuningSettings::setGlideFallSpeed);
        add(x, y += 23, w, "滑翔极速", 0.1D, 2.0D,
                MovementTuningSettings::glideMaxSpeed, MovementTuningSettings::setGlideMaxSpeed);
        add(x, y += 23, w, "空中冲刺速度", 0.2D, 3.0D,
                MovementTuningSettings::airDashSpeed, MovementTuningSettings::setAirDashSpeed);
        add(x, y += 23, w, "空中冲刺时长", 1.0D, 20.0D,
                () -> MovementTuningSettings.airDashTicks(), MovementTuningSettings::setAirDashTicks);
        add(x, y += 23, w, "高速视野扩张", 0.0D, 0.5D,
                MovementTuningSettings::fovBoost, MovementTuningSettings::setFovBoost);

        int buttonY = panelY + PANEL_HEIGHT - 31;
        addRenderableWidget(Button.builder(Component.literal("保存参数"), button -> {
            MovementTuningSettings.save();
            status = "移动参数已保存";
        }).bounds(x, buttonY, 103, 19).build());
        addRenderableWidget(Button.builder(Component.literal("完成"), button -> onClose())
                .bounds(x + 111, buttonY, 103, 19).build());
    }

    private void add(int x, int y, int width, String label, double min, double max,
                     DoubleSupplier getter, DoubleConsumer setter) {
        addRenderableWidget(new ValueSlider(x, y, width, label, min, max, getter, setter));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0x6B090B10);
        graphics.renderOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xDDE23A3A);
        graphics.drawString(font, title, panelX + 8, panelY + 8, 0xFFFF6267, false);
        graphics.drawString(font, status, panelX + 8, panelY + 21, 0xFFE4E8EF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F8) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        MovementTuningSettings.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

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

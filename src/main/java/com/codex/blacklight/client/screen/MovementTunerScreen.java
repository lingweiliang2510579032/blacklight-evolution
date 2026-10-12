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
    private static final int PANEL_HEIGHT = 238;
    private int panelX;
    private int panelY;
    private String status = "调整时暂停 · F8 保存并关闭";
    private com.codex.blacklight.movement.MovementTuningSnapshot opening;
    private int page,parameterCount;

    public MovementTunerScreen() { super(Component.literal("黑光移动引擎调试器")); }

    @Override
    protected void init() {
        MovementTuningSettings.load();
        if(opening==null)opening=MovementTuningSettings.snapshot();
        parameterCount=0;
        panelX = Math.max(4, width - PANEL_WIDTH - 4);
        panelY = Math.max(4, (height - PANEL_HEIGHT) / 2);
        int x = panelX + 8;
        int w = PANEL_WIDTH - 16;
        int y = panelY + 35;

        add(x, y, w, "跑步加速度", 0.0D, 0.25D,
                MovementTuningSettings::sprintAcceleration, MovementTuningSettings::setSprintAcceleration);
        add(x, y += 23, w, "跑步极速/格每刻", 0.2D, 2.5D,
                MovementTuningSettings::sprintMaxSpeed, MovementTuningSettings::setSprintMaxSpeed);
        add(x, y += 23, w, "点按起跳/格每刻", 0.3D, 0.8D,
                MovementTuningSettings::jumpMinVelocity, MovementTuningSettings::setJumpMinVelocity);
        add(x, y += 23, w, "满蓄力起跳/格每刻", 0.8D, 1.65D,
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
        addRenderableWidget(Button.builder(Component.literal(page==0?"第 1 / 2 页 · 下一页":"第 2 / 2 页 · 上一页"),button->{
            page=1-page;rebuildWidgets();
        }).bounds(x,panelY+PANEL_HEIGHT-55,w,19).build());
        addRenderableWidget(Button.builder(Component.literal("保存关闭"), button -> onClose()).bounds(x,buttonY,68,19).build());
        addRenderableWidget(Button.builder(Component.literal("恢复默认"), button -> {
            MovementTuningSettings.applyServerSnapshot(MovementTuningSettings.MVP_DEFAULTS);
            status="已恢复默认 · 关闭时保存";
        }).bounds(x+73,buttonY,68,19).build());
        addRenderableWidget(Button.builder(Component.literal("撤销调整"), button -> {
            MovementTuningSettings.applyServerSnapshot(opening);status="已还原本次打开前的参数";
        }).bounds(x+146,buttonY,68,19).build());
    }

    private void add(int x, int y, int width, String label, double min, double max,
                     DoubleSupplier getter, DoubleConsumer setter) {
        int index=parameterCount++;
        var slider=new ValueSlider(x,panelY+35+(index%6)*23,width,label,min,max,getter,setter);
        slider.visible=index/6==page;slider.active=slider.visible;
        addRenderableWidget(slider);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for(var widget:children())if(widget instanceof ValueSlider slider)slider.refresh();
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0x6B090B10);
        graphics.renderOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xDDE23A3A);
        graphics.drawString(font, title, panelX + 8, panelY + 8, 0xFFFF6267, false);
        var player=net.minecraft.client.Minecraft.getInstance().player;
        int upgrades=player==null?0:player.getData(com.codex.blacklight.registry.ModAttachments.PLAYER_STATE).getMovementUpgradeMask();
        String hint=page==0?String.format(Locale.ROOT,"预计跳高 %.1f～%.1f 格（含升级）",
                MovementTuningSettings.estimatedJumpHeight(0,upgrades),MovementTuningSettings.estimatedJumpHeight(1,upgrades)):status;
        graphics.drawString(font, status.startsWith("保存失败")?status:hint, panelX + 8, panelY + 21, 0xFFE4E8EF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F8) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if(!MovementTuningSettings.save()){status="保存失败，请检查配置文件权限";return;}
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return true; }

    private static final class ValueSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final DoubleConsumer setter;
        private final DoubleSupplier getter;

        private ValueSlider(int x, int y, int width, String label, double min, double max,
                            DoubleSupplier getter, DoubleConsumer setter) {
            super(x, y, width, 19, Component.empty(), normalize(getter.getAsDouble(), min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.getter = getter;
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
            refresh();
        }

        private void refresh(){value=normalize(getter.getAsDouble(),min,max);updateMessage();}

        private static double normalize(double value, double min, double max) {
            return Math.max(0.0D, Math.min(1.0D, (value - min) / (max - min)));
        }
    }
}

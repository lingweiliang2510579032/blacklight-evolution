package com.codex.blacklight.client.screen;

import com.codex.blacklight.network.UpgradePurchasePayload;
import com.codex.blacklight.power.ClawUpgrade;
import com.codex.blacklight.power.MovementUpgrade;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.state.BlacklightState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The upgrade menu in the original game's shape: a full-screen dark command-console
 * panel with a left category rail, the upgrade list of the selected category in the
 * center and the EP balance up top. Purchases are server-authoritative.
 */
public final class UpgradeScreen extends Screen {
    private static final int BG = 0xF20A0C10;
    private static final int RAIL_BG = 0xFF0D0F16;
    private static final int RED = 0xFFE23A3A;
    private static final int RED_SOFT = 0xFF7A2026;
    private static final int TEXT = 0xFFE4E8EF;
    private static final int TEXT_DIM = 0xFF8A93A2;
    private static final int ROW_HOVER = 0x2EE23A3A;

    private enum Category {
        MOVEMENT("screen.blacklight.section.movement"),
        CLAW("screen.blacklight.section.claw");

        private final String langKey;
        Category(String langKey) { this.langKey = langKey; }
    }

    private Category category = Category.MOVEMENT;
    private final List<Row> rows = new ArrayList<>();
    private int railWidth;
    private int contentX;
    private int contentWidth;

    public UpgradeScreen() {
        super(Component.translatable("screen.blacklight.upgrades"));
    }

    @Override
    protected void init() {
        railWidth = Math.max(96, width / 7);
        contentX = railWidth + 24;
        contentWidth = width - contentX - 16;
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();
        clearWidgets();
        int y = 64;
        if (category == Category.MOVEMENT) {
            for (MovementUpgrade upgrade : MovementUpgrade.values()) {
                y = addRow(y, upgrade.id(), upgrade.epCost(),
                        upgrade.requires() == null ? null : upgrade.requires().id());
            }
        } else {
            for (ClawUpgrade upgrade : ClawUpgrade.values()) {
                y = addRow(y, upgrade.id(), upgrade.epCost(), null);
            }
        }
    }

    private int addRow(int y, String id, int cost, String requiresId) {
        Button button = Button.builder(Component.empty(), clicked -> buy(id))
                .bounds(width - 116, y - 2, 100, 20).build();
        addRenderableWidget(button);
        rows.add(new Row(id, cost, requiresId, button, y));
        return y + 30;
    }

    private void buy(String id) {
        PacketDistributor.sendToServer(new UpgradePurchasePayload(id));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BG);
        graphics.fill(0, 0, railWidth, height, RAIL_BG);
        graphics.fill(railWidth, 0, railWidth + 1, height, RED_SOFT);
        graphics.fill(0, 44, width, 45, RED_SOFT);

        graphics.drawString(font, title, 12, 10, RED, false);
        BlacklightState state = currentState();
        Component ep = Component.translatable("screen.blacklight.ep_balance",
                state == null ? 0 : state.getEvolutionPoints());
        graphics.drawString(font, ep, width - 12 - font.width(ep), 10, 0xFFFFD796, false);
        graphics.drawString(font, Component.translatable("screen.blacklight.upgrades_hint"),
                12, 28, TEXT_DIM, false);

        int railY = 64;
        for (Category entry : Category.values()) {
            boolean selected = entry == category;
            if (mouseX >= 8 && mouseX < railWidth - 8 && mouseY >= railY && mouseY < railY + 22) {
                graphics.fill(8, railY, railWidth - 8, railY + 22, ROW_HOVER);
            }
            graphics.drawString(font, Component.translatable(entry.langKey),
                    16, railY + 7, selected ? RED : TEXT_DIM, false);
            if (selected) graphics.fill(8, railY + 4, 10, railY + 18, RED);
            railY += 26;
        }

        for (Row row : rows) {
            if (mouseX >= contentX - 6 && mouseX < width - 8
                    && mouseY >= row.y - 4 && mouseY < row.y + 24) {
                graphics.fill(contentX - 6, row.y - 4, width - 8, row.y + 24, ROW_HOVER);
            }
            graphics.drawString(font, Component.translatable("upgrade.blacklight." + row.id),
                    contentX, row.y, TEXT, false);
            graphics.drawString(font, Component.translatable(
                    "upgrade.blacklight." + row.id + ".desc"), contentX, row.y + 11,
                    TEXT_DIM, false);
            refreshRow(row, state);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void refreshRow(Row row, BlacklightState state) {
        if (state == null) return;
        boolean owned = owned(state, row.id);
        boolean prereqMet = row.requiresId == null || owned(state, row.requiresId);
        boolean affordable = state.getEvolutionPoints() >= row.cost;
        if (owned) {
            row.button.active = false;
            row.button.setMessage(Component.translatable("screen.blacklight.state_owned"));
        } else if (!prereqMet) {
            row.button.active = false;
            row.button.setMessage(Component.translatable("screen.blacklight.state_requires",
                    Component.translatable("upgrade.blacklight." + row.requiresId)));
        } else {
            row.button.active = affordable;
            row.button.setMessage(Component.translatable("screen.blacklight.state_cost", row.cost));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int railY = 64;
        for (Category entry : Category.values()) {
            if (mouseX >= 8 && mouseX < railWidth - 8
                    && mouseY >= railY && mouseY < railY + 22) {
                if (entry != category) {
                    category = entry;
                    rebuildRows();
                }
                return true;
            }
            railY += 26;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean owned(BlacklightState state, String id) {
        ClawUpgrade claw = ClawUpgrade.byId(id);
        if (claw != null) return state.hasClawUpgrade(claw);
        MovementUpgrade movement = MovementUpgrade.byId(id);
        return movement != null && state.hasMovementUpgrade(movement);
    }

    private static BlacklightState currentState() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? null
                : minecraft.player.getData(ModAttachments.PLAYER_STATE);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_U) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static final class Row {
        private final String id;
        private final int cost;
        private final String requiresId;
        private final Button button;
        private final int y;

        private Row(String id, int cost, String requiresId, Button button, int y) {
            this.id = id;
            this.cost = cost;
            this.requiresId = requiresId;
            this.button = button;
            this.y = y;
        }
    }
}

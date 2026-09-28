package com.hapnoid.autohotbar.client.gui;

import com.hapnoid.autohotbar.config.ConfigManager;
import com.hapnoid.autohotbar.config.SlotConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Deliberately simple/functional rather than a pixel-perfect recreation of
 * the original screenshots: one button per hotbar slot. This keeps the GUI
 * code to plain Button/EditBox usage (see class comment in RuleEditScreen
 * for why that matters on this Minecraft version) so it's the part of the
 * mod least likely to need a post-build fix. Visual polish can be layered on
 * once this compiles and runs.
 */
public class ConfigScreen extends Screen {
    private static final int ROWS = 9;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 220;
    private static final int SPACING = 4;

    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("AutoHotbar Remake"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int startY = 20;
        int centerX = this.width / 2;

        for (int i = 1; i <= ROWS; i++) {
            int slotNumber = i;
            SlotConfig slotConfig = ConfigManager.get().slot(slotNumber);
            int y = startY + (i - 1) * (BUTTON_HEIGHT + SPACING);
            this.addRenderableWidget(Button.builder(
                    Component.literal("Slot " + slotNumber + "  -  " + slotConfig.rules.size() + " rule(s)"),
                    button -> this.minecraft.setScreen(new SlotRuleListScreen(this, slotNumber))
            ).bounds(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

        int doneY = startY + ROWS * (BUTTON_HEIGHT + SPACING) + SPACING;
        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
            ConfigManager.save();
            this.minecraft.setScreen(parent);
        }).bounds(centerX - BUTTON_WIDTH / 2, doneY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

package com.hapnoid.autohotbar.client.gui;

import com.hapnoid.autohotbar.config.ConfigManager;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The screen Mod Menu / the open-config keybind lands on first. Shows the
 * 9-slot hotbar strip from the screenshots - click a box to edit that
 * slot's rules. Clicks on the strip are handled with a direct mouseClicked
 * override (same proven pattern as ItemPickerScreen, which compiled and
 * runs correctly), not vanilla Button widgets, so the boxes can be drawn
 * exactly like the screenshots instead of looking like vanilla buttons.
 */
public class ConfigScreen extends Screen {
    private final Screen parent;
    private int stripLeft, stripTop;
    private int panelLeft, panelTop, panelWidth, panelHeight;

    public ConfigScreen(Screen parent) {
        super(Component.literal("AutoHotbar Remake"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int stripWidth = HotbarStripPreview.totalWidth();
        stripLeft = this.width / 2 - stripWidth / 2;
        stripTop = 60;

        panelLeft = stripLeft - 16;
        panelTop = 20;
        panelWidth = stripWidth + 32;
        panelHeight = HotbarStripPreview.BOX + 90;

        int buttonWidth = 160;
        int doneY = stripTop + HotbarStripPreview.BOX + 20;
        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
            ConfigManager.save();
            this.minecraft.setScreen(parent);
        }).bounds(this.width / 2 - buttonWidth / 2, doneY, buttonWidth, 20).build());

        int pl = panelLeft, pt = panelTop, pw = panelWidth, ph = panelHeight, sl = stripLeft, st = stripTop;
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen != this) return;
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) -> {
                GlassPanel.panel(graphics, pl, pt, pw, ph);
                graphics.text(this.font, "AutoHotbar Remake", pl + 8, pt + 8, GlassPanel.TEXT, true);
                graphics.text(this.font, "Click a slot below to edit its rules", pl + 8, pt + 22, GlassPanel.TEXT_DIM, false);
                HotbarStripPreview.draw(graphics, this.font, sl, st, 0);
            });
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int slot = HotbarStripPreview.slotAt(event.x(), event.y(), stripLeft, stripTop);
        if (slot > 0) {
            this.minecraft.setScreen(new SlotRuleListScreen(this, slot));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

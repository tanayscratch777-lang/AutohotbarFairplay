package com.hapnoid.autohotbar.client.gui;

import com.hapnoid.autohotbar.config.ConfigManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The 9-box hotbar strip from the screenshots, shared by ConfigScreen and SlotRuleListScreen. */
public final class HotbarStripPreview {
    public static final int BOX = 40;
    public static final int GAP = 4;

    private HotbarStripPreview() {
    }

    public static int totalWidth() {
        return 9 * BOX + 8 * GAP;
    }

    /** selectedSlot = 0 for "none currently open" (ConfigScreen), 1-9 to outline that box green. */
    public static void draw(GuiGraphicsExtractor g, Font font, int left, int top, int selectedSlot) {
        for (int i = 1; i <= 9; i++) {
            int x = left + (i - 1) * (BOX + GAP);
            boolean selected = i == selectedSlot;
            int ruleCount = ConfigManager.get().slot(i).rules.size();
            int border = selected ? 0xFF55FF55 : (ruleCount > 0 ? 0xFF66AAFF : 0xFF666666);

            g.fill(x, top, x + BOX, top + BOX, 0xA0101018);
            g.outline(x, top, BOX, BOX, border);
            g.text(font, String.valueOf(i), x + 3, top + 3, selected ? 0xFF55FF55 : GlassPanel.TEXT, true);

            String countLabel = ruleCount + (ruleCount == 1 ? " rule" : " rules");
            g.text(font, countLabel, x + BOX / 2 - font.width(countLabel) / 2, top + BOX - 11, GlassPanel.TEXT_DIM, false);
        }
    }

    /** Which slot (1-9, or 0 for none) the given screen-space point falls on. */
    public static int slotAt(double mouseX, double mouseY, int left, int top) {
        if (mouseY < top || mouseY >= top + BOX) return 0;
        for (int i = 1; i <= 9; i++) {
            int x = left + (i - 1) * (BOX + GAP);
            if (mouseX >= x && mouseX < x + BOX) return i;
        }
        return 0;
    }
}

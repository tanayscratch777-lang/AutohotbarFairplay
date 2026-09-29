package com.hapnoid.autohotbar.client.highlight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared drawing code for "move THIS item to hotbar slot N": a translucent colored
 * tint over the 16x16 item area, a solid border, and the key label in the corner.
 * Each destination slot gets its own color so several highlights are easy to tell apart.
 */
public final class SlotTint {
    // slot 1..9 -> distinct, readable colors (RGB)
    private static final int[] PALETTE = {
            0xFF5555, // 1 red
            0xFFAA33, // 2 orange
            0xFFEE44, // 3 yellow
            0x55DD55, // 4 green
            0x44DDDD, // 5 cyan
            0x5599FF, // 6 blue
            0xAA66FF, // 7 purple
            0xFF66CC, // 8 pink
            0xEEEEEE  // 9 white
    };

    private SlotTint() {
    }

    public static int rgbFor(int destSlot) {
        return PALETTE[Math.floorMod(destSlot - 1, PALETTE.length)];
    }

    /** itemX/itemY = top-left of the 16x16 item icon. */
    public static void draw(GuiGraphicsExtractor g, int itemX, int itemY, int destSlot, int matchedPriority) {
        int rgb = rgbFor(destSlot);
        // First-choice rule = stronger tint, fallback rules = fainter tint.
        int fillAlpha = matchedPriority <= 1 ? 0x88 : 0x55;
        int fill = (fillAlpha << 24) | rgb;
        int border = 0xFF000000 | rgb;

        g.fill(itemX, itemY, itemX + 16, itemY + 16, fill);
        g.fill(itemX - 1, itemY - 1, itemX + 17, itemY, border);          // top
        g.fill(itemX - 1, itemY + 16, itemX + 17, itemY + 17, border);    // bottom
        g.fill(itemX - 1, itemY, itemX, itemY + 16, border);              // left
        g.fill(itemX + 16, itemY, itemX + 17, itemY + 16, border);        // right

        String label = KeyLabelResolver.labelFor(destSlot);
        if (label.length() > 3) {
            label = label.substring(0, 3); // long key names ("Left Shift") won't fit in a slot
        }
        g.text(Minecraft.getInstance().font, label, itemX + 1, itemY + 1, 0xFFFFFFFF, true);
    }
}

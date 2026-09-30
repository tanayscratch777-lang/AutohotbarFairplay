package com.hapnoid.autohotbar.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The dark, translucent "glass" panel look from the original screenshots -
 * just alpha-blended fills, no actual GPU blur (that would need shader work
 * this environment can't safely attempt blind). Panel body + a lighter
 * 1px border, and a settled color scheme reused across every screen in this
 * package so they read as one consistent UI instead of each looking custom.
 */
public final class GlassPanel {
    public static final int PANEL_FILL = 0xC0142033;      // dark blue-grey, ~75% opaque
    public static final int PANEL_BORDER = 0x60FFFFFF;    // faint light border
    public static final int ROW_FILL = 0x50FFFFFF;        // subtle hover/row highlight
    public static final int ROW_FILL_SELECTED = 0x8055AAFF;
    public static final int TEXT = 0xFFEAEAEA;
    public static final int TEXT_DIM = 0xFFAAAAAA;
    public static final int ACCENT = 0xFF55DD55;

    private GlassPanel() {
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL_FILL);
        g.outline(x, y, w, h, PANEL_BORDER);
    }

    public static void row(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hovered) {
        if (hovered) {
            g.fill(x, y, x + w, y + h, ROW_FILL);
        }
    }

    /** Tab label with a colored underline when selected - same visual language as the screenshots. */
    public static void tabUnderline(GuiGraphicsExtractor g, int x, int y, int w, boolean selected) {
        if (selected) {
            g.fill(x, y, x + w, y + 2, ACCENT);
        }
    }
}

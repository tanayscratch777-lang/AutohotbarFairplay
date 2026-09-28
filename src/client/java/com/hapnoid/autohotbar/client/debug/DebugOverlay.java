package com.hapnoid.autohotbar.client.debug;

import com.hapnoid.autohotbar.client.highlight.HighlightState;
import com.hapnoid.autohotbar.rule.RuleEvaluator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Map;

public final class DebugOverlay {
    private static boolean visible = false;

    private DebugOverlay() {
    }

    public static void toggle() {
        visible = !visible;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        if (!visible) return;
        Minecraft client = Minecraft.getInstance();
        Map<Integer, RuleEvaluator.SlotResult> results = HighlightState.current();

        int x = 4;
        int y = 4;
        int lineHeight = client.font.lineHeight + 1;
        graphics.text(client.font, "AutoHotbar debug (last eval tick " + HighlightState.lastEvaluatedTick() + ")", x, y, 0xFFFFFF, true);
        y += lineHeight + 2;

        for (int slot = 1; slot <= 9; slot++) {
            RuleEvaluator.SlotResult r = results.get(slot);
            String line;
            if (r == null || (r.target == null && !r.isStop)) {
                line = "Slot " + slot + ": (no rule matched)";
            } else if (r.isStop) {
                line = "Slot " + slot + ": stop rule (priority " + r.matchedPriority + ") - left empty";
            } else {
                line = "Slot " + slot + ": rule #" + r.matchedPriority + " -> " + r.target.itemId;
            }
            graphics.text(client.font, line, x, y, 0xFFFFFF, true);
            y += lineHeight;
        }
    }
}

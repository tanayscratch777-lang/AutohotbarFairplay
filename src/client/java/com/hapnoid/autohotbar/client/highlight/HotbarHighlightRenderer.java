package com.hapnoid.autohotbar.client.highlight;

import com.hapnoid.autohotbar.rule.RuleEvaluator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Map;

/**
 * Renders over the vanilla hotbar using its well-known, long-stable layout:
 * a 182px-wide bar centered horizontally, 22px tall, sitting flush with the
 * bottom of the screen, with 9 equal 20px slots. This geometry has not
 * changed across Minecraft versions in a very long time, unlike the
 * rendering API method names around it - so it's a safe thing to hardcode.
 */
public final class HotbarHighlightRenderer {

    private static final int HOTBAR_WIDTH = 182;
    private static final int SLOT_SIZE = 20;
    private static final int BOTTOM_MARGIN = 4;

    private HotbarHighlightRenderer() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options == null) return;
        if (client.screen != null) return; // a screen (inventory/chest/etc.) is open - the screen overlay handles that case instead

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int hotbarLeft = screenWidth / 2 - HOTBAR_WIDTH / 2;
        int hotbarTop = screenHeight - SLOT_SIZE - BOTTOM_MARGIN;

        Map<Integer, RuleEvaluator.SlotResult> results = HighlightState.current();

        for (int slot = 1; slot <= 9; slot++) {
            RuleEvaluator.SlotResult result = results.get(slot);
            if (result == null || result.target == null) continue;
            // Only highlight if this slot doesn't already hold the target item -
            // no point telling the player to switch to what they're already holding.
            if (client.player.getInventory().getItem(slot - 1) == result.target.stack) continue;

            int x = hotbarLeft + (slot - 1) * SLOT_SIZE;
            int y = hotbarTop;
            int color = HighlightState.colorFor(result.matchedPriority);

            // Thin colored border around the slot.
            graphics.fill(x + 1, y, x + SLOT_SIZE - 1, y + 1, color);
            graphics.fill(x + 1, y + SLOT_SIZE - 2, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, color);
            graphics.fill(x, y, x + 1, y + SLOT_SIZE - 1, color);
            graphics.fill(x + SLOT_SIZE - 2, y, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, color);

            String label = KeyLabelResolver.labelFor(slot);
            int textWidth = client.font.width(label);
            graphics.drawString(client.font, label, x + SLOT_SIZE / 2 - textWidth / 2, y - 10, color, true);
        }
    }
}

package com.hapnoid.autohotbar.client.highlight;

import com.hapnoid.autohotbar.rule.RuleEvaluator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;

import java.util.Map;

/**
 * HUD version (no screen open). Same meaning as in inventory screens: the highlighted
 * item is the one you should MOVE, and the label is the key of the hotbar slot it
 * should go to. Only items that are currently IN the hotbar can be shown here - an item
 * still sitting in the main inventory has no spot on the HUD.
 *
 * Vanilla hotbar geometry: 182px bar centered, item icon for slot i at
 * x = left + 3 + i*20, y = screenHeight - 19.
 */
public final class HotbarHighlightRenderer {

    private static final int HOTBAR_WIDTH = 182;

    private HotbarHighlightRenderer() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options == null) return;
        if (client.screen != null) return; // inventory/chest/etc. open - the screen overlay handles it

        Inventory inv = client.player.getInventory();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int hotbarLeft = screenWidth / 2 - HOTBAR_WIDTH / 2;

        for (Map.Entry<Integer, RuleEvaluator.SlotResult> entry : HighlightState.current().entrySet()) {
            int destSlot = entry.getKey();
            RuleEvaluator.SlotResult result = entry.getValue();
            if (result == null || result.target == null) continue;

            // Already in the slot the rule wants - nothing to do.
            if (inv.getItem(destSlot - 1) == result.target.stack) continue;

            for (int i = 0; i < 9; i++) {
                if (inv.getItem(i) == result.target.stack) {
                    int itemX = hotbarLeft + 3 + i * 20;
                    int itemY = screenHeight - 19;
                    SlotTint.draw(graphics, itemX, itemY, destSlot, result.matchedPriority);
                    break;
                }
            }
        }
    }
}

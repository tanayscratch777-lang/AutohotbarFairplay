package com.hapnoid.autohotbar.client.highlight;

import com.hapnoid.autohotbar.client.mixin.ContainerScreenAccessor;
import com.hapnoid.autohotbar.rule.RuleEvaluator;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * ============================================================================
 *  RISK NOTE
 * ============================================================================
 * A real mod's published 26.1.2 upgrade changelog documented
 * "ScreenEvents.beforeRender() -> beforeExtract()" as part of this exact
 * Minecraft version's Fabric API rename wave. This class assumes the
 * matching afterRender() -> afterExtract() rename also happened. If
 * `./gradlew build` says it can't find `afterExtract`, try renaming the
 * single call below back to `afterRender` (and if the lambda's parameter
 * types then mismatch, change `GuiGraphicsExtractor`/`DeltaTracker` back to
 * `GuiGraphics`/`float tickDelta` to match) - that is the full extent of the
 * fix needed here.
 * ============================================================================
 */
public final class InventorySlotHighlighter {

    private static final int SLOT_ICON_SIZE = 16;

    private InventorySlotHighlighter() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                        drawHighlights(containerScreen, graphics));
            }
        });
    }

    private static void drawHighlights(AbstractContainerScreen<?> screen, net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        Map<Integer, RuleEvaluator.SlotResult> results = HighlightState.current();
        ContainerScreenAccessor accessor = (ContainerScreenAccessor) screen;
        int left = accessor.autohotbar$getLeftPos();
        int top = accessor.autohotbar$getTopPos();

        for (Slot slot : screen.getMenu().slots) {
            ItemStack stackInSlot = slot.getItem();
            for (Map.Entry<Integer, RuleEvaluator.SlotResult> entry : results.entrySet()) {
                RuleEvaluator.SlotResult result = entry.getValue();
                if (result.target == null || result.target.stack != stackInSlot) continue;

                int x = left + slot.x;
                int y = top + slot.y;
                int color = HighlightState.colorFor(result.matchedPriority);

                graphics.fill(x - 1, y - 1, x + SLOT_ICON_SIZE + 1, y, color);
                graphics.fill(x - 1, y + SLOT_ICON_SIZE, x + SLOT_ICON_SIZE + 1, y + SLOT_ICON_SIZE + 1, color);
                graphics.fill(x - 1, y - 1, x, y + SLOT_ICON_SIZE + 1, color);
                graphics.fill(x + SLOT_ICON_SIZE, y - 1, x + SLOT_ICON_SIZE + 1, y + SLOT_ICON_SIZE + 1, color);

                String label = KeyLabelResolver.labelFor(entry.getKey());
                graphics.text(net.minecraft.client.Minecraft.getInstance().font, label, x - 2, y - 10, color, true);
                break;
            }
        }
    }
}

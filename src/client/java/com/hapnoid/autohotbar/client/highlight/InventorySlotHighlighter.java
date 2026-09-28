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
        net.minecraft.world.entity.player.Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();

        Map<Integer, RuleEvaluator.SlotResult> results = HighlightState.current();
        ContainerScreenAccessor accessor = (ContainerScreenAccessor) screen;
        int left = accessor.autohotbar$getLeftPos();
        int top = accessor.autohotbar$getTopPos();

        for (Slot slot : screen.getMenu().slots) {
            ItemStack stackInSlot = slot.getItem();
            if (stackInSlot.isEmpty()) continue;

            for (Map.Entry<Integer, RuleEvaluator.SlotResult> entry : results.entrySet()) {
                RuleEvaluator.SlotResult result = entry.getValue();
                if (result.target == null || result.target.stack != stackInSlot) continue;

                // Already in the hotbar slot the rule wants? Nothing to do. (Checked against the
                // real player inventory, NOT slot indexes - the creative screen wraps slots and
                // reports different indexes, which is why this used to misfire there.)
                if (inv.getItem(entry.getKey() - 1) == stackInSlot) break;

                SlotTint.draw(graphics, left + slot.x, top + slot.y, entry.getKey(), result.matchedPriority);
                break;
            }
        }
    }
}

package com.hapnoid.autohotbar.client;

import com.hapnoid.autohotbar.AutoHotbarRemake;
import com.hapnoid.autohotbar.client.debug.DebugOverlay;
import com.hapnoid.autohotbar.client.gui.ConfigScreen;
import com.hapnoid.autohotbar.client.highlight.HighlightState;
import com.hapnoid.autohotbar.client.highlight.HotbarHighlightRenderer;
import com.hapnoid.autohotbar.client.highlight.InventorySlotHighlighter;
import com.hapnoid.autohotbar.config.ConfigManager;
import com.hapnoid.autohotbar.rule.RuleEvaluator;
import com.hapnoid.autohotbar.util.InventoryScanner;
import com.hapnoid.autohotbar.util.ItemCandidate;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 *  RISK NOTE: two identifiers below are educated guesses, everything else in
 *  this file is confirmed against current (26.1.2-specific) Fabric docs:
 *   1. `VanillaHudElements.HOTBAR` - the constant name/package for the
 *      vanilla hotbar HUD anchor. Confirmed pattern is
 *      VanillaHudElements.<NAME>; HOTBAR is the logical name but if the
 *      build can't find it, check net.fabricmc.fabric.api.client.rendering.v1
 *      (with or without the trailing .hud) for the exact constant name.
 *   2. The exact sub-package of VanillaHudElements itself (".hud" suffix
 *      included here) - drop it if the import doesn't resolve.
 *  Everything else here (KeyMapping.Category.register, KeyMappingHelper,
 *  HudElementRegistry.attachElementAfter, GuiGraphicsExtractor/DeltaTracker
 *  signature) is taken directly from the 26.1.2 Fabric documentation.
 * ============================================================================
 */
public class AutoHotbarRemakeClient implements ClientModInitializer {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(AutoHotbarRemake.MOD_ID, "main"));

    private KeyMapping openConfigKey;
    private KeyMapping toggleDebugKey;

    private long lastEvaluationTick = Long.MIN_VALUE;
    private long lastQuickHash = Long.MIN_VALUE;

    @Override
    public void onInitializeClient() {
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.autohotbar_remake.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_BACKSLASH,
                CATEGORY));

        toggleDebugKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.autohotbar_remake.toggle_debug",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                CATEGORY));

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(AutoHotbarRemake.MOD_ID, "hotbar_highlight"),
                HotbarHighlightRenderer::render);

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(AutoHotbarRemake.MOD_ID, "debug_overlay"),
                DebugOverlay::render);

        InventorySlotHighlighter.register();

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft client) {
        if (client.player == null) return;
        long tick = client.player.tickCount;

        // Cheap every-tick check (see InventoryScanner's design note); only run the
        // heavier rule evaluation when something actually changed, throttled so an
        // event storm (huge farm output) can't force more than one full re-evaluation
        // every N ticks.
        long hash = InventoryScanner.quickHash(client.player);
        boolean changed = hash != lastQuickHash;
        boolean throttleOk = (tick - lastEvaluationTick) >= ConfigManager.get().minTicksBetweenEvaluations;

        if (changed && throttleOk) {
            lastQuickHash = hash;
            lastEvaluationTick = tick;
            List<ItemCandidate> inventory = InventoryScanner.scan(client.player);
            Map<Integer, RuleEvaluator.SlotResult> results = RuleEvaluator.evaluate(ConfigManager.get(), inventory);
            HighlightState.update(results, tick);
        }

        while (openConfigKey.consumeClick()) {
            client.setScreen(new ConfigScreen(null));
        }
        while (toggleDebugKey.consumeClick()) {
            DebugOverlay.toggle();
        }
    }
}

package com.hapnoid.autohotbar.client.highlight;

import com.hapnoid.autohotbar.rule.RuleEvaluator;

import java.util.Collections;
import java.util.Map;

/** Simple shared holder, updated once per throttled evaluation, read every frame by renderers. */
public final class HighlightState {
    private static volatile Map<Integer, RuleEvaluator.SlotResult> latest = Collections.emptyMap();
    private static volatile long lastEvaluatedTick = -1;

    private HighlightState() {
    }

    public static void update(Map<Integer, RuleEvaluator.SlotResult> results, long tick) {
        latest = results;
        lastEvaluatedTick = tick;
    }

    public static Map<Integer, RuleEvaluator.SlotResult> current() {
        return latest;
    }

    public static long lastEvaluatedTick() {
        return lastEvaluatedTick;
    }

    /** Highlight color tier: rank 1 (highest priority match, i.e. lowest priority number) is brightest. */
    public static int colorFor(int matchedPriority) {
        return switch (Math.max(1, matchedPriority)) {
            case 1 -> 0xFF55FF55; // bright green
            case 2 -> 0xFFAAFF55; // yellow-green
            case 3 -> 0xFFFFFF55; // yellow
            case 4 -> 0xFFFFAA55; // orange
            default -> 0xFFFF5555; // red - a low-priority fallback rule won
        };
    }
}

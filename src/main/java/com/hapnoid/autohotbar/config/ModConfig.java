package com.hapnoid.autohotbar.config;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    public List<SlotConfig> slots = new ArrayList<>();

    /** Named, saved copies of `slots` the player can switch between (e.g. "PvP", "Farming"). */
    public List<RulePreset> presets = new ArrayList<>();

    /** Name of the currently-active preset, for display only; "slots" above is always the live one. */
    public String activePresetName = "Default";

    // ---- Performance tuning (see InventoryScanner) ----
    /** Minimum ticks between two full rule re-evaluations, even under an event storm. */
    public int minTicksBetweenEvaluations = 2;
    /** Debug overlay off by default; toggled in-game with its keybind, not saved as "on". */
    public transient boolean debugOverlayRuntimeOnly = false;

    public static ModConfig createDefault() {
        ModConfig cfg = new ModConfig();
        for (int i = 1; i <= 9; i++) {
            cfg.slots.add(new SlotConfig(i));
        }
        return cfg;
    }

    public SlotConfig slot(int number) {
        for (SlotConfig s : slots) {
            if (s.slotNumber == number) return s;
        }
        SlotConfig created = new SlotConfig(number);
        slots.add(created);
        return created;
    }
}

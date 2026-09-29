package com.hapnoid.autohotbar.config;

import java.util.ArrayList;
import java.util.List;

public class RulePreset {
    public String name;
    public List<SlotConfig> slots = new ArrayList<>();

    public RulePreset() {
    }

    public RulePreset(String name, List<SlotConfig> slots) {
        this.name = name;
        // deep-ish copy so editing the live config doesn't silently mutate the preset
        for (SlotConfig s : slots) {
            SlotConfig copy = new SlotConfig(s.slotNumber);
            copy.rules.addAll(s.rules);
            this.slots.add(copy);
        }
    }
}

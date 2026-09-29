package com.hapnoid.autohotbar.config;

import com.hapnoid.autohotbar.rule.Rule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SlotConfig {
    /** 1-9, matching the vanilla hotbar. */
    public int slotNumber;
    public List<Rule> rules = new ArrayList<>();

    public SlotConfig() {
    }

    public SlotConfig(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public List<Rule> rulesByPriority() {
        List<Rule> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator.comparingInt(r -> r.priority));
        return sorted;
    }
}

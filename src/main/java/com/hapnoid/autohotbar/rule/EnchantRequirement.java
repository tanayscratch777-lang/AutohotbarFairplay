package com.hapnoid.autohotbar.rule;

import java.util.ArrayList;
import java.util.List;

/**
 * A single required enchantment, e.g. "Depth Strider IV". {@link #anyOfGroupId}
 * lets several requirements be tagged as alternatives of each other ("any of
 * Silk Touch or Fortune III") - requirements sharing the same non-null group id
 * are satisfied if ANY one of them matches, instead of ALL of them.
 */
public class EnchantRequirement {
    /** Registry id of the enchantment, e.g. "minecraft:sharpness". */
    public String enchantmentId;
    public int minLevel = 1;
    /** Null = this requirement is mandatory (ANDed with the others). */
    public String anyOfGroupId = null;

    public EnchantRequirement() {
    }

    public EnchantRequirement(String enchantmentId, int minLevel) {
        this.enchantmentId = enchantmentId;
        this.minLevel = minLevel;
    }

    /**
     * Splits a flat requirement list into mandatory ones and any-of groups,
     * for the evaluator to check separately.
     */
    public static List<List<EnchantRequirement>> groupByAnyOf(List<EnchantRequirement> reqs) {
        List<List<EnchantRequirement>> groups = new ArrayList<>();
        java.util.Map<String, List<EnchantRequirement>> byGroup = new java.util.LinkedHashMap<>();
        for (EnchantRequirement r : reqs) {
            if (r.anyOfGroupId == null) {
                List<EnchantRequirement> solo = new ArrayList<>();
                solo.add(r);
                groups.add(solo);
            } else {
                byGroup.computeIfAbsent(r.anyOfGroupId, k -> new ArrayList<>()).add(r);
            }
        }
        groups.addAll(byGroup.values());
        return groups;
    }
}

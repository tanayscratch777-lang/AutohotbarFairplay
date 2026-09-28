package com.hapnoid.autohotbar.rule;

import java.util.ArrayList;
import java.util.List;

/**
 * One rule row (as seen in the "Add rule" screenshots). Deliberately a single
 * flat class rather than a class hierarchy so it serializes to/from JSON with
 * plain Gson - unused fields for a given {@link #kind} are just left null/default.
 */
public class Rule {
    public RuleKind kind = RuleKind.EMPTY;
    /** Lower number = higher priority within its slot, matching the original UI. */
    public int priority = 1;

    // ---- SPECIFIC ----
    public String specificItemId;                 // e.g. "minecraft:stone"
    public boolean mustBeEnchanted = false;
    public List<EnchantRequirement> requiredEnchants = new ArrayList<>();

    // ---- TYPE ----
    public Category category;
    public Variant variant;
    /** Optional hard filter: candidates must satisfy these to even be considered. */
    public List<EnchantRequirement> typeEnchantFilter = new ArrayList<>();

    // ---- CONDITIONAL ----
    public int ifSlot = 1;
    public int ifRule = 1;
    public ConditionalAction thenAction = ConditionalAction.USE;
    public int thenSlot = 1;
    public int thenRule = 1;
    public ConditionalAction elseAction = ConditionalAction.NEXT;
    public int elseSlot = 1;
    public int elseRule = 1;

    // ---- Feature: multi-slot fill ----
    /** 1-9 slot numbers (besides the slot this rule is defined on) that should also use this same rule. */
    public List<Integer> alsoFillSlots = new ArrayList<>();

    // ---- Feature: NBT / custom data matching (Specific rules) ----
    /** Optional custom model data value to require; null = don't check. */
    public Integer requiredCustomModelData;

    public static Rule empty(int priority) {
        Rule r = new Rule();
        r.kind = RuleKind.EMPTY;
        r.priority = priority;
        return r;
    }
}

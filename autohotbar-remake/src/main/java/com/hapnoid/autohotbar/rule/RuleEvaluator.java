package com.hapnoid.autohotbar.rule;

import com.hapnoid.autohotbar.config.ModConfig;
import com.hapnoid.autohotbar.config.SlotConfig;
import com.hapnoid.autohotbar.util.ItemCandidate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pure logic, no Minecraft dependency: given the current config and a list of
 * {@link ItemCandidate} snapshots (see InventoryScanner for how those get
 * built), decides which candidate item each of the 9 hotbar slots wants.
 */
public final class RuleEvaluator {

    public static final class SlotResult {
        public final int slotNumber;
        public final ItemCandidate target;      // null = slot should be empty / no match
        public final int matchedPriority;       // priority of the rule that matched, for highlight tiering
        public final boolean isStop;            // matched an Empty/stop rule specifically

        SlotResult(int slotNumber, ItemCandidate target, int matchedPriority, boolean isStop) {
            this.slotNumber = slotNumber;
            this.target = target;
            this.matchedPriority = matchedPriority;
            this.isStop = isStop;
        }
    }

    private RuleEvaluator() {
    }

    public static Map<Integer, SlotResult> evaluate(ModConfig config, List<ItemCandidate> inventory) {
        // Expand multi-slot-fill rules into a working copy of each slot's rule list.
        Map<Integer, List<Rule>> effectiveRules = buildEffectiveRuleLists(config);

        // Pass 1: for every (slot, rule) pair that is Specific or Type, independently
        // resolve whether it finds a candidate - Conditional rules reference these.
        Map<Long, Optional<ItemCandidate>> ruleMatchCache = new HashMap<>();
        for (Map.Entry<Integer, List<Rule>> e : effectiveRules.entrySet()) {
            for (Rule rule : e.getValue()) {
                if (rule.kind == RuleKind.SPECIFIC || rule.kind == RuleKind.TYPE) {
                    ruleMatchCache.put(key(e.getKey(), rule.priority), resolveDirect(rule, inventory));
                }
            }
        }

        // Pass 2: walk each slot's priority-ordered list, resolving Conditional/Empty
        // rules against the cache built above.
        Map<Integer, SlotResult> results = new HashMap<>();
        for (int slotNumber = 1; slotNumber <= 9; slotNumber++) {
            List<Rule> rules = effectiveRules.getOrDefault(slotNumber, List.of());
            SlotResult result = evaluateSlot(slotNumber, rules, ruleMatchCache);
            results.put(slotNumber, result);
        }
        return results;
    }

    private static SlotResult evaluateSlot(int slotNumber, List<Rule> rules, Map<Long, Optional<ItemCandidate>> cache) {
        for (Rule rule : rules) {
            switch (rule.kind) {
                case EMPTY -> {
                    return new SlotResult(slotNumber, null, rule.priority, true);
                }
                case SPECIFIC, TYPE -> {
                    Optional<ItemCandidate> found = cache.getOrDefault(key(slotNumber, rule.priority), Optional.empty());
                    if (found.isPresent()) {
                        return new SlotResult(slotNumber, found.get(), rule.priority, false);
                    }
                    // else fall through to the next rule in this slot
                }
                case CONDITIONAL -> {
                    boolean conditionMet = cache.getOrDefault(key(rule.ifSlot, rule.ifRule), Optional.empty()).isPresent();
                    ConditionalAction action = conditionMet ? rule.thenAction : rule.elseAction;
                    int branchSlot = conditionMet ? rule.thenSlot : rule.elseSlot;
                    int branchRule = conditionMet ? rule.thenRule : rule.elseRule;
                    switch (action) {
                        case USE -> {
                            Optional<ItemCandidate> found = cache.getOrDefault(key(branchSlot, branchRule), Optional.empty());
                            if (found.isPresent()) {
                                return new SlotResult(slotNumber, found.get(), rule.priority, false);
                            }
                            // referenced rule didn't find anything - fall to next rule in this slot
                        }
                        case STOP -> {
                            return new SlotResult(slotNumber, null, rule.priority, true);
                        }
                        case NEXT -> {
                            // fall through to next rule in this slot
                        }
                    }
                }
            }
        }
        return new SlotResult(slotNumber, null, 0, false);
    }

    /** Evaluates a single Specific/Type rule against the full inventory, independent of slot context. */
    private static Optional<ItemCandidate> resolveDirect(Rule rule, List<ItemCandidate> inventory) {
        List<ItemCandidate> candidates = new ArrayList<>();
        for (ItemCandidate c : inventory) {
            if (matchesRule(rule, c)) {
                candidates.add(c);
            }
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (rule.kind == RuleKind.SPECIFIC) {
            // Multiple stacks of the same specific item/enchant combo: prefer highest durability.
            candidates.sort(Comparator.comparingDouble((ItemCandidate c) -> c.durabilityPercent).reversed());
            return Optional.of(candidates.get(0));
        }
        // TYPE: rank by the requested variant.
        Comparator<ItemCandidate> byVariant = comparatorFor(rule);
        candidates.sort(byVariant);
        return Optional.of(candidates.get(0));
    }

    private static boolean matchesRule(Rule rule, ItemCandidate c) {
        if (rule.kind == RuleKind.SPECIFIC) {
            if (!c.itemId.equals(rule.specificItemId)) return false;
            if (rule.mustBeEnchanted && !c.isEnchanted()) return false;
            return satisfiesEnchantRequirements(rule.requiredEnchants, c);
        } else if (rule.kind == RuleKind.TYPE) {
            return categoryMatches(rule.category, c) && satisfiesEnchantRequirements(rule.typeEnchantFilter, c);
        }
        return false;
    }

    private static boolean categoryMatches(Category category, ItemCandidate c) {
        Category actual = com.hapnoid.autohotbar.util.ItemInspector.categoryOf(c.stack);
        return actual == category;
    }

    private static boolean satisfiesEnchantRequirements(List<EnchantRequirement> reqs, ItemCandidate c) {
        if (reqs == null || reqs.isEmpty()) return true;
        for (List<EnchantRequirement> group : EnchantRequirement.groupByAnyOf(reqs)) {
            boolean anySatisfied = false;
            for (EnchantRequirement r : group) {
                if (c.enchantLevel(r.enchantmentId) >= r.minLevel) {
                    anySatisfied = true;
                    break;
                }
            }
            if (!anySatisfied) return false;
        }
        return true;
    }

    private static Comparator<ItemCandidate> comparatorFor(Rule rule) {
        boolean isBlock = rule.category == Category.BLOCK;
        Variant variant = rule.variant == null ? Variant.BEST_MATERIAL : rule.variant;
        return switch (variant) {
            case BEST_MATERIAL -> Comparator.comparingInt((ItemCandidate c) -> c.tier.rank)
                    .thenComparingDouble(c -> c.combatScore)
                    .reversed();
            case MOST_DPS -> isBlock
                    ? Comparator.comparingDouble((ItemCandidate c) -> c.blockHardness).reversed()
                    : Comparator.comparingDouble((ItemCandidate c) -> c.combatScore).reversed();
            case WORST -> isBlock
                    ? Comparator.comparingDouble((ItemCandidate c) -> c.blockHardness)
                    : Comparator.comparingDouble((ItemCandidate c) -> c.combatScore);
            case BEST_CONDITION -> Comparator.comparingDouble((ItemCandidate c) -> c.durabilityPercent)
                    .thenComparingDouble(c -> c.combatScore)
                    .reversed();
        };
    }

    private static Map<Integer, List<Rule>> buildEffectiveRuleLists(ModConfig config) {
        Map<Integer, List<Rule>> effective = new HashMap<>();
        for (int i = 1; i <= 9; i++) {
            effective.put(i, new ArrayList<>());
        }
        for (SlotConfig slotConfig : config.slots) {
            for (Rule rule : slotConfig.rulesByPriority()) {
                effective.get(slotConfig.slotNumber).add(rule);
                for (Integer extraSlot : rule.alsoFillSlots) {
                    if (extraSlot >= 1 && extraSlot <= 9 && extraSlot != slotConfig.slotNumber) {
                        effective.get(extraSlot).add(rule);
                    }
                }
            }
        }
        // Re-sort each slot's expanded list by priority since fills may have been appended out of order.
        for (List<Rule> list : effective.values()) {
            list.sort(Comparator.comparingInt(r -> r.priority));
        }
        return effective;
    }

    private static long key(int slot, int rulePriority) {
        return ((long) slot << 32) | (rulePriority & 0xffffffffL);
    }
}

package com.hapnoid.autohotbar.rule;

import com.hapnoid.autohotbar.config.ModConfig;
import com.hapnoid.autohotbar.config.SlotConfig;
import com.hapnoid.autohotbar.util.ItemCandidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Pure logic, no Minecraft dependency: given the current config and a list of
 * {@link ItemCandidate} snapshots (see InventoryScanner), decides which
 * candidate item each of the 9 hotbar slots wants.
 *
 * ------------------------------------------------------------------------
 *  CLAIMING: once an item is picked as the winner for one slot, it is
 *  removed from the pool before later slots are resolved. Slots are
 *  processed in order 1 -> 9. This is what makes "slot 1: best sword" and
 *  "slot 2: best sword" resolve to the BEST and SECOND-BEST sword instead
 *  of both pointing at the same physical item.
 * ------------------------------------------------------------------------
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

    /** Threaded through one evaluate() call; not shared/reused across calls. */
    private static final class Context {
        final Map<Integer, List<Rule>> effectiveRules;
        final List<ItemCandidate> fullInventory;
        final Set<ItemCandidate> claimed = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<Long, Optional<ItemCandidate>> cache = new HashMap<>();

        Context(Map<Integer, List<Rule>> effectiveRules, List<ItemCandidate> fullInventory) {
            this.effectiveRules = effectiveRules;
            this.fullInventory = fullInventory;
        }

        List<ItemCandidate> unclaimedPool() {
            List<ItemCandidate> pool = new ArrayList<>(fullInventory.size());
            for (ItemCandidate c : fullInventory) {
                if (!claimed.contains(c)) pool.add(c);
            }
            return pool;
        }

        Rule findRule(int slot, int priority) {
            for (Rule r : effectiveRules.getOrDefault(slot, List.of())) {
                if (r.priority == priority) return r;
            }
            return null;
        }
    }

    private RuleEvaluator() {
    }

    public static Map<Integer, SlotResult> evaluate(ModConfig config, List<ItemCandidate> inventory) {
        Context ctx = new Context(buildEffectiveRuleLists(config), inventory);
        Map<Integer, SlotResult> results = new HashMap<>();

        // ------------------------------------------------------------------
        // STABILIZATION PASS: if two or more slots share the exact same
        // primary Type rule (e.g. slot 1 and slot 2 both "best sword"), the
        // plain slot-order claiming below always gives slot 1 the #1 item,
        // forcing a swap prompt every time the player happens to be holding
        // them in the "wrong" slot - even when the right SET of items is
        // already split between the two slots. This pass checks that case
        // first: if the items currently sitting in a group of equivalent
        // slots are ALREADY exactly the top-N best available (just not
        // necessarily in canonical rank order), keep them exactly where
        // they are instead of prompting a purely cosmetic swap. It only
        // looks at each slot's PRIMARY (lowest-priority-number) rule, so a
        // slot whose main rule isn't Type, or whose Type rule differs
        // (different category/variant/enchant filter) from the others,
        // isn't affected.
        // ------------------------------------------------------------------
        for (List<Integer> group : groupBySignature(ctx).values()) {
            if (group.size() >= 2) {
                stabilizeGroupIfAlreadyOptimal(group, ctx, results);
            }
        }

        for (int slotNumber = 1; slotNumber <= 9; slotNumber++) {
            if (results.containsKey(slotNumber)) continue; // already decided by the stabilization pass
            List<Rule> rules = ctx.effectiveRules.getOrDefault(slotNumber, List.of());
            SlotResult result = evaluateSlot(slotNumber, rules, ctx);
            results.put(slotNumber, result);
            if (result.target != null) {
                ctx.claimed.add(result.target);
            }
        }
        return results;
    }

    private static Map<String, List<Integer>> groupBySignature(Context ctx) {
        Map<String, List<Integer>> groups = new HashMap<>();
        for (int slot = 1; slot <= 9; slot++) {
            List<Rule> rules = ctx.effectiveRules.getOrDefault(slot, List.of());
            if (rules.isEmpty()) continue;
            Rule primary = rules.get(0); // rulesByPriority()-derived lists are already priority-sorted
            if (primary.kind != RuleKind.TYPE) continue;
            String sig = signatureOf(primary);
            groups.computeIfAbsent(sig, k -> new ArrayList<>()).add(slot);
        }
        return groups;
    }

    private static String signatureOf(Rule rule) {
        StringBuilder sb = new StringBuilder();
        sb.append(rule.category).append('|').append(rule.variant == null ? Variant.BEST_MATERIAL : rule.variant);
        List<String> parts = new ArrayList<>();
        for (EnchantRequirement r : rule.typeEnchantFilter) {
            parts.add(r.enchantmentId + ":" + r.minLevel + ":" + r.anyOfGroupId);
        }
        Collections.sort(parts);
        sb.append('|').append(String.join(",", parts));
        return sb.toString();
    }

    private static void stabilizeGroupIfAlreadyOptimal(List<Integer> group, Context ctx, Map<Integer, SlotResult> results) {
        Map<Integer, Rule> primaryRuleBySlot = new HashMap<>();
        for (int slot : group) {
            primaryRuleBySlot.put(slot, ctx.effectiveRules.get(slot).get(0));
        }
        Rule sampleRule = primaryRuleBySlot.values().iterator().next();

        // What SHOULD occupy these slots collectively, best-first, ignoring which
        // specific slot each one lands in.
        List<ItemCandidate> candidates = new ArrayList<>();
        for (ItemCandidate c : ctx.unclaimedPool()) {
            if (matchesRule(sampleRule, c)) candidates.add(c);
        }
        candidates.sort(comparatorFor(sampleRule));
        if (candidates.size() < group.size()) return; // not enough valid items to even fill the group - let normal resolution report the shortfall
        Set<ItemCandidate> targetSet = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < group.size(); i++) targetSet.add(candidates.get(i));

        // What's ALREADY sitting in these slots right now.
        Map<Integer, ItemCandidate> currentBySlot = new HashMap<>();
        for (int slot : group) {
            ItemCandidate current = currentItemInHotbarSlot(slot, ctx);
            if (current == null || ctx.claimed.contains(current) || !matchesRule(primaryRuleBySlot.get(slot), current)) {
                return; // this slot doesn't already hold a valid, unclaimed match - not stable, fall back to normal ranking
            }
            currentBySlot.put(slot, current);
        }
        Set<ItemCandidate> currentSet = Collections.newSetFromMap(new IdentityHashMap<>());
        currentSet.addAll(currentBySlot.values());
        if (!currentSet.equals(targetSet)) {
            return; // the right ITEMS aren't all already here (an upgrade exists, or a duplicate) - let normal ranking handle it
        }

        // Already optimal as a set - keep everyone exactly where they are.
        for (int slot : group) {
            ItemCandidate current = currentBySlot.get(slot);
            results.put(slot, new SlotResult(slot, current, primaryRuleBySlot.get(slot).priority, false));
            ctx.claimed.add(current);
        }
    }

    private static ItemCandidate currentItemInHotbarSlot(int slotNumber, Context ctx) {
        int inventoryIndex = slotNumber - 1;
        for (ItemCandidate c : ctx.fullInventory) {
            if (c.inventoryIndex == inventoryIndex) return c;
        }
        return null;
    }

    private static SlotResult evaluateSlot(int slotNumber, List<Rule> rules, Context ctx) {
        for (Rule rule : rules) {
            switch (rule.kind) {
                case EMPTY -> {
                    return new SlotResult(slotNumber, null, rule.priority, true);
                }
                case SPECIFIC, TYPE -> {
                    Optional<ItemCandidate> found = resolveCached(slotNumber, rule.priority, ctx);
                    if (found.isPresent()) {
                        return new SlotResult(slotNumber, found.get(), rule.priority, false);
                    }
                    // else fall through to the next rule in this slot
                }
                case CONDITIONAL -> {
                    boolean conditionMet = resolveCached(rule.ifSlot, rule.ifRule, ctx).isPresent();
                    ConditionalAction action = conditionMet ? rule.thenAction : rule.elseAction;
                    int branchSlot = conditionMet ? rule.thenSlot : rule.elseSlot;
                    int branchRule = conditionMet ? rule.thenRule : rule.elseRule;
                    switch (action) {
                        case USE -> {
                            Optional<ItemCandidate> found = resolveCached(branchSlot, branchRule, ctx);
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

    /**
     * Resolves one Specific/Type rule (by its owning slot + priority) against the
     * CURRENTLY unclaimed pool, caching the result. Safe to call for a slot that
     * hasn't been "reached" yet by the outer loop (e.g. a Conditional referencing
     * a later slot) - it just resolves that rule on demand, still against
     * whatever is unclaimed at that point.
     */
    private static Optional<ItemCandidate> resolveCached(int slot, int priority, Context ctx) {
        long key = key(slot, priority);
        Optional<ItemCandidate> cached = ctx.cache.get(key);
        if (cached != null) return cached;

        Rule rule = ctx.findRule(slot, priority);
        Optional<ItemCandidate> result;
        if (rule == null || (rule.kind != RuleKind.SPECIFIC && rule.kind != RuleKind.TYPE)) {
            result = Optional.empty();
        } else {
            result = resolveDirect(rule, ctx.unclaimedPool());
        }
        ctx.cache.put(key, result);
        return result;
    }

    /** Evaluates a single Specific/Type rule against the given (already-unclaimed) pool. */
    private static Optional<ItemCandidate> resolveDirect(Rule rule, List<ItemCandidate> pool) {
        List<ItemCandidate> candidates = new ArrayList<>();
        for (ItemCandidate c : pool) {
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
        candidates.sort(comparatorFor(rule));
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
        for (List<Rule> list : effective.values()) {
            list.sort(Comparator.comparingInt(r -> r.priority));
        }
        return effective;
    }

    private static long key(int slot, int rulePriority) {
        return ((long) slot << 32) | (rulePriority & 0xffffffffL);
    }
}

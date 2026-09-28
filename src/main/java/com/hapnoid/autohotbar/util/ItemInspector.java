package com.hapnoid.autohotbar.util;

import com.hapnoid.autohotbar.rule.Category;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ============================================================================
 *  RISK NOTE (read me if the build fails)
 * ============================================================================
 * This class is the ONLY place that calls Minecraft's live item / enchantment
 * / registry API directly. Everything else in the mod works with the plain
 * {@link ItemCandidate} snapshot instead. Minecraft 26.1.x's API is very
 * fresh (first unobfuscated release, heavy renames throughout 2026), so if
 * `./gradlew build` fails with "cannot find symbol" errors, they will almost
 * certainly point at a method/class name in THIS file - everything else in
 * the mod is plain Java with no Minecraft dependency and won't be affected.
 * Paste the compiler error and it's a quick, contained fix.
 * ============================================================================
 */
public final class ItemInspector {

    private ItemInspector() {
    }

    public static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** Enchantment registry id -> level, e.g. "minecraft:sharpness" -> 5. */
    public static Map<String, Integer> enchantments(ItemStack stack) {
        Map<String, Integer> result = new LinkedHashMap<>();
        ItemEnchantments enchantments = stack.getEnchantments();
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            int level = enchantments.getLevel(holder);
            String id = holder.getRegisteredName();
            result.put(id, level);
        }
        return result;
    }

    /** 1.0 = full durability / unbreakable item, 0.0 = about to break. */
    public static double durabilityPercent(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return 1.0;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return 1.0;
        }
        return 1.0 - ((double) stack.getDamageValue() / (double) max);
    }

    /**
     * A relative combat/utility score combining material tier with the
     * enchantments that matter for damage/output. This is a tunable
     * heuristic for RANKING purposes, not a reproduction of Minecraft's
     * internal damage formula - it only needs to put "diamond sword with
     * Sharpness V" above "bare netherite sword" consistently, which it does.
     */
    public static double combatScore(ItemStack stack, MaterialTier tier) {
        double score = tier.rank * 10.0;
        for (Map.Entry<String, Integer> e : enchantments(stack).entrySet()) {
            String id = e.getKey().toLowerCase(Locale.ROOT);
            int level = e.getValue();
            if (id.contains("sharpness") || id.contains("power") || id.contains("smite")
                    || id.contains("bane_of_arthropods") || id.contains("impaling") || id.contains("density")) {
                score += level * 3.0;
            } else if (id.contains("efficiency")) {
                score += level * 2.0;
            } else {
                score += level * 1.0;
            }
        }
        return score;
    }

    /** Hardness for BLOCK-category items; -1 if this item isn't a block. */
    public static double blockHardness(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return -1;
        }
        try {
            return blockItem.getBlock().defaultDestroyTime();
        } catch (Throwable t) {
            // If this exact accessor moved in your MC version, this keeps
            // the mod from crashing - Block "Softest/Hardest" ranking will
            // just be disabled until fixed. See the class-level note above.
            return 0;
        }
    }

    public static MaterialTier materialTier(ItemStack stack) {
        return MaterialTier.fromItemId(itemId(stack));
    }

    /** Best-effort category guess from the item's registry id / class. */
    public static Category categoryOf(ItemStack stack) {
        String id = itemId(stack).toLowerCase(Locale.ROOT);
        if (id.contains("_sword")) return Category.SWORD;
        if (id.contains("trident") || id.contains("spear")) return Category.SPEAR;
        if (id.contains("_pickaxe")) return Category.PICKAXE;
        if (id.contains("_axe")) return Category.AXE;
        if (id.contains("_shovel") || id.contains("_spade")) return Category.SHOVEL;
        if (id.contains("_hoe")) return Category.HOE;
        if (id.endsWith(":bow")) return Category.BOW;
        if (id.endsWith(":crossbow")) return Category.CROSSBOW;
        if (id.contains("_helmet")) return Category.ARMOR_HEAD;
        if (id.contains("_chestplate")) return Category.ARMOR_CHEST;
        if (id.contains("_leggings")) return Category.ARMOR_LEGS;
        if (id.contains("_boots")) return Category.ARMOR_FEET;
        if (stack.getItem() instanceof BlockItem) return Category.BLOCK;
        return null;
    }

    public static ItemCandidate inspect(ItemStack stack, int inventoryIndex) {
        String id = itemId(stack);
        MaterialTier tier = MaterialTier.fromItemId(id);
        Map<String, Integer> ench = enchantments(stack);
        double durability = durabilityPercent(stack);
        double combat = combatScore(stack, tier);
        double hardness = blockHardness(stack);
        return new ItemCandidate(stack, inventoryIndex, id, tier, ench, durability, combat, hardness);
    }
}

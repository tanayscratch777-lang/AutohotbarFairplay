package com.hapnoid.autohotbar.util;

import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * A pre-computed snapshot of one inventory slot's item, so the rule engine
 * never has to touch Minecraft's live ItemStack/registry API directly - all
 * of that lives in {@link ItemInspector}. If a future MC version renames
 * something, only ItemInspector needs to change.
 */
public final class ItemCandidate {
    public final ItemStack stack;
    /** Index into the player's combined inventory (see InventoryScanner). */
    public final int inventoryIndex;
    public final String itemId;          // e.g. "minecraft:diamond_sword"
    public final MaterialTier tier;
    public final Map<String, Integer> enchantments; // enchantment id -> level
    public final double durabilityPercent; // 1.0 = full durability, unbreakable items = 1.0
    public final double combatScore;     // tier + enchantment contributions, for MOST_DPS/WORST
    public final double blockHardness;   // only meaningful for BLOCK category, else -1

    public ItemCandidate(ItemStack stack, int inventoryIndex, String itemId, MaterialTier tier,
                          Map<String, Integer> enchantments, double durabilityPercent,
                          double combatScore, double blockHardness) {
        this.stack = stack;
        this.inventoryIndex = inventoryIndex;
        this.itemId = itemId;
        this.tier = tier;
        this.enchantments = enchantments;
        this.durabilityPercent = durabilityPercent;
        this.combatScore = combatScore;
        this.blockHardness = blockHardness;
    }

    public boolean isEnchanted() {
        return !enchantments.isEmpty();
    }

    public int enchantLevel(String enchantId) {
        return enchantments.getOrDefault(enchantId, 0);
    }
}

package com.hapnoid.autohotbar.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 *  DESIGN NOTE: why this is a cheap per-tick poll, not a pickup/insert hook
 * ============================================================================
 * The spec calls for "trigger on every inventory add (pickup, chest transfer)
 * but stay fast even at 100,000+ items/sec". Hooking every possible insertion
 * path (item pickup, hopper transfer, shift-click, chest withdraw, etc.) would
 * mean a mixin into several different vanilla methods - fragile on a toolchain
 * this fresh, and pointless: a farm producing 100k items/sec is still only
 * touching the player's 36 inventory slots (items stack up to 64, and once
 * full slots just stop changing), NOT firing 100k separate events that matter
 * to us. So instead, every client tick we compute a tiny hash of the 36 slots
 * (just item id + count + damage - no registry/enchantment lookups, so this
 * is effectively free) and only run the real rule evaluation (which does the
 * heavier enchantment/scoring work) when that hash actually changed AND at
 * most once every {@code minTicksBetweenEvaluations} ticks. This gives
 * effectively-instant reaction to real changes with a per-tick cost that is
 * CONSTANT regardless of farm throughput.
 * ============================================================================
 */
public final class InventoryScanner {

    private InventoryScanner() {
    }

    /** Cheap enough to call every client tick. */
    public static long quickHash(Player player) {
        Inventory inv = player.getInventory();
        long hash = 1125899906842597L;
        int size = inv.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inv.getItem(i);
            int itemHash = stack.isEmpty() ? 0 : System.identityHashCode(stack.getItem());
            hash = 31 * hash + itemHash;
            hash = 31 * hash + stack.getCount();
            hash = 31 * hash + (stack.isDamageableItem() ? stack.getDamageValue() : 0);
        }
        return hash;
    }

    /** The heavier scan - only call this when quickHash() indicates something changed. */
    public static List<ItemCandidate> scan(Player player) {
        Inventory inv = player.getInventory();
        List<ItemCandidate> result = new ArrayList<>();
        int size = inv.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            result.add(ItemInspector.inspect(stack, i));
        }
        return result;
    }
}

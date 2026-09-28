package com.hapnoid.autohotbar.rule;

import java.util.Locale;

/**
 * Broad item categories a "Type" rule can target. Matching is done against
 * the item's registry id / class in {@link com.hapnoid.autohotbar.util.ItemInspector},
 * not against a hardcoded item list, so modded items are picked up too as
 * long as their id contains the expected keyword (e.g. "*_sword").
 */
public enum Category {
    SWORD,
    SPEAR,      // trident and any modded spear-like weapon
    AXE,
    PICKAXE,
    SHOVEL,
    HOE,
    BOW,
    CROSSBOW,
    BLOCK,      // any placeable block item - used for "Softest" variant etc.
    ARMOR_HEAD,
    ARMOR_CHEST,
    ARMOR_LEGS,
    ARMOR_FEET;

    public String displayName() {
        String s = name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}

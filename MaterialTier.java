package com.hapnoid.autohotbar.util;

/** Netherite > Diamond > Iron > Stone > Gold > Wood, as shown in the original UI. */
public enum MaterialTier {
    NETHERITE(6),
    DIAMOND(5),
    IRON(4),
    STONE(3),
    GOLD(2),
    WOOD(1),
    OTHER(0);

    public final int rank;

    MaterialTier(int rank) {
        this.rank = rank;
    }

    /** Best-effort guess from an item's registry id, e.g. "minecraft:diamond_sword". */
    public static MaterialTier fromItemId(String itemId) {
        String id = itemId.toLowerCase(java.util.Locale.ROOT);
        if (id.contains("netherite")) return NETHERITE;
        if (id.contains("diamond")) return DIAMOND;
        if (id.contains("iron")) return IRON;
        if (id.contains("stone")) return STONE;
        if (id.contains("gold")) return GOLD; // covers "golden_" and "gold_"
        if (id.contains("wood") || id.contains("wooden")) return WOOD;
        return OTHER;
    }
}

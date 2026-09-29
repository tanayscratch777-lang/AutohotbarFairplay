package com.hapnoid.autohotbar.rule;

/**
 * How to rank candidates within a {@link Category} to pick a winner.
 *
 * BEST_MATERIAL - raw material tier only (Netherite > Diamond > Iron > Stone > Gold > Wood),
 *                 ignoring enchantments. Useful when you specifically want "my best netherite
 *                 gear" regardless of what's enchanted.
 * MOST_DPS      - effective combat score: material tier contribution PLUS enchantment
 *                 contributions (Sharpness/Power/Efficiency/etc). A enchanted lower-tier
 *                 item can outrank an unenchanted higher-tier one here.
 * WORST         - the inverse of MOST_DPS (or, for BLOCK category, highest hardness instead
 *                 of lowest) - lowest effective score. Useful for junk/scrap picking.
 * BEST_CONDITION- highest remaining durability percentage, tiebreaker MOST_DPS.
 *
 * For the BLOCK category, "MOST_DPS"/"WORST" are reinterpreted as hardness ranking
 * (see ItemInspector) since blocks don't deal damage - WORST = lowest hardness ("Softest"),
 * MOST_DPS = highest hardness ("Hardest").
 */
public enum Variant {
    BEST_MATERIAL,
    MOST_DPS,
    WORST,
    BEST_CONDITION
}

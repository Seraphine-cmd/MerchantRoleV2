package com.bap80.merchantrole;

import java.util.Locale;

/**
 * The full progression chain. Peasants is the starting rank; every other rank
 * is earned by completing the task list for that target rank.
 */
public enum Rank {
    PEASANTS("Peasants", "peasants"),
    MINER_I("Miner I", "miner-i"),
    MINER_II("Miner II", "miner-ii"),
    FIGHTER_I("Fighter I", "fighter-i"),
    FIGHTER_II("Fighter II", "fighter-ii"),
    EXPLORER_I("Explorer I", "explorer-i"),
    EXPLORER_II("Explorer II", "explorer-ii"),
    FARMER_I("Farmer I", "farmer-i"),
    FARMER_II("Farmer II", "farmer-ii");

    private final String displayName;
    private final String configKey;

    Rank(String displayName, String configKey) {
        this.displayName = displayName;
        this.configKey = configKey;
    }

    public String displayName() {
        return displayName;
    }

    public String configKey() {
        return configKey;
    }

    /** The next rank in the chain, or null at the end. */
    public Rank next() {
        int index = ordinal() + 1;
        return index < values().length ? values()[index] : null;
    }

    /** True for Miner II -> Fighter I and the other tier changes. */
    public boolean isRankupTransition() {
        return name().endsWith("_II") && next() != null;
    }

    /** True for Peasants -> Miner I and every I -> II step. */
    public boolean isUpgradeTransition() {
        return this == PEASANTS || (name().endsWith("_I") && next() != null);
    }

    public static Rank fromKey(String key) {
        if (key == null || key.isBlank()) {
            return PEASANTS;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (Rank rank : values()) {
            if (rank.configKey.equals(normalized) || rank.name().equalsIgnoreCase(normalized)) {
                return rank;
            }
        }
        return PEASANTS;
    }
}

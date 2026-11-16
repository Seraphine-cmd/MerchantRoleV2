package com.bap80.merchantrole;

import org.bukkit.entity.Player;

public record ProgressContext(MerchantRolePlugin plugin,
                              Player player,
                              PlayerProfile profile,
                              Rank targetRank) {

    public int goal(String taskId, int fallback) {
        return plugin.getConfig().getInt(
                "requirements." + targetRank.configKey() + "." + taskId, fallback);
    }

    public int counter(String key) {
        return profile.counter(key);
    }

    public int max(String key) {
        return profile.counter(key);
    }

    public boolean flag(String key) {
        return profile.hasFlag(key);
    }
}

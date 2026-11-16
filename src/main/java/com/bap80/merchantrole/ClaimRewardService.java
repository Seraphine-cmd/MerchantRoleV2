package com.bap80.merchantrole;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Optional GriefPrevention hook for the 200-claim reward. GriefPrevention is
 * not a hard dependency; when it is missing the reward is remembered and
 * retried later.
 */
public final class ClaimRewardService {
    private final MerchantRolePlugin plugin;

    public ClaimRewardService(MerchantRolePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean available() {
        return Bukkit.getPluginManager().isPluginEnabled("GriefPrevention");
    }

    public boolean grant(Player player, int blocks) {
        if (blocks <= 0) {
            return true;
        }
        Plugin griefPrevention = Bukkit.getPluginManager().getPlugin("GriefPrevention");
        if (griefPrevention == null || !griefPrevention.isEnabled()) {
            return false;
        }

        try {
            Class<?> mainClass = griefPrevention.getClass();
            Field instanceField = mainClass.getField("instance");
            Object instance = instanceField.get(null);

            Field dataStoreField = mainClass.getField("dataStore");
            Object dataStore = dataStoreField.get(instance);

            Method getPlayerData = dataStore.getClass().getMethod("getPlayerData", UUID.class);
            Object playerData = getPlayerData.invoke(dataStore, player.getUniqueId());

            Method getAccrued = playerData.getClass().getMethod("getAccruedClaimBlocks");
            Method setAccrued = playerData.getClass().getMethod("setAccruedClaimBlocks", int.class);

            int current = ((Number) getAccrued.invoke(playerData)).intValue();
            setAccrued.invoke(playerData, current + blocks);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Could not grant GriefPrevention claim blocks: "
                    + exception.getMessage());
            return false;
        }
    }
}

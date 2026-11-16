package com.bap80.merchantrole;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Optional LuckPerms integration. The plugin stores rank progress itself; the
 * LuckPerms group is kept in sync so other plugins can use rank permissions.
 */
public final class LuckPermsHook {
    private final MerchantRolePlugin plugin;

    public LuckPermsHook(MerchantRolePlugin plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.settings().luckPermsEnabled()
                && Bukkit.getPluginManager().isPluginEnabled("LuckPerms");
    }

    public void grant(Player player, Rank rank) {
        if (!enabled()) {
            return;
        }
        String group = plugin.settings().group(rank);
        if (group == null || group.isBlank()) {
            return;
        }
        dispatch("lp user " + player.getName() + " parent add " + group);
    }

    public void changeGroup(Player player, Rank oldRank, Rank newRank) {
        if (!enabled()) {
            return;
        }
        String oldGroup = plugin.settings().group(oldRank);
        String newGroup = plugin.settings().group(newRank);
        if (newGroup == null || newGroup.isBlank()) {
            return;
        }

        if (plugin.settings().luckPermsUseParentSet()) {
            dispatch("lp user " + player.getName() + " parent set " + newGroup);
            return;
        }

        if (oldGroup != null && !oldGroup.isBlank() && !oldGroup.equalsIgnoreCase(newGroup)) {
            dispatch("lp user " + player.getName() + " parent remove " + oldGroup);
        }
        dispatch("lp user " + player.getName() + " parent add " + newGroup);
    }

    /**
     * Removes stale MerchantRole groups, then adds the current one. This keeps
     * external permission changes from leaving a player in an old rank.
     */
    public void sync(Player player, Rank current) {
        if (!enabled()) {
            return;
        }
        String currentGroup = plugin.settings().group(current);
        for (Rank rank : Rank.values()) {
            if (rank == current) {
                continue;
            }
            String group = plugin.settings().group(rank);
            if (group != null && !group.isBlank()
                    && (currentGroup == null || !group.equalsIgnoreCase(currentGroup))) {
                dispatch("lp user " + player.getName() + " parent remove " + group);
            }
        }
        if (currentGroup != null && !currentGroup.isBlank()) {
            dispatch("lp user " + player.getName() + " parent add " + currentGroup);
        }
    }

    private void dispatch(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}

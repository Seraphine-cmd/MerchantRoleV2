package com.bap80.merchantrole;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {
    private final MerchantRolePlugin plugin;

    public PlayerConnectionListener(MerchantRolePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean isNew = plugin.store().get(player.getUniqueId()) == null;
        PlayerProfile profile = plugin.store().getOrCreate(player);

        if (isNew) {
            profile.setRank(Rank.PEASANTS);
            plugin.store().markDirty(profile);
            plugin.luckPerms().grant(player, Rank.PEASANTS);
            player.sendMessage(plugin.settings().prefix() + "§aWelcome! You start as §ePeasants§a.");
            player.sendMessage(plugin.settings().prefix()
                    + "§7Use §e/roles progress §7to see your first tasks (Miner I).");
        } else if (plugin.settings().syncGroupsOnJoin()) {
            plugin.luckPerms().sync(player, profile.rank());
        }

        plugin.retryPendingClaims(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.store().flush();
    }
}

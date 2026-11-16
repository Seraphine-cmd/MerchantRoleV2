package com.bap80.merchantrole;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class MerchantRolePlugin extends JavaPlugin {
    private PluginSettings settings;
    private PlayerDataStore store;
    private ProgressService progress;
    private EconomyService economy;
    private ClaimRewardService claims;
    private LuckPermsHook luckPerms;
    private WorldGuardHook worldGuard;
    private RolesCommand rolesCommand;
    private BukkitTask scanTask;
    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new PluginSettings(this);

        store = new PlayerDataStore(this);
        store.load();

        economy = new EconomyService(this);
        economy.refresh();
        claims = new ClaimRewardService(this);
        luckPerms = new LuckPermsHook(this);
        worldGuard = new WorldGuardHook(this);
        progress = new ProgressService(this);
        rolesCommand = new RolesCommand(this);

        PluginCommand command = getCommand("roles");
        if (command == null) {
            getLogger().severe("The /roles command is missing from plugin.yml.");
        } else {
            command.setExecutor(rolesCommand);
            command.setTabCompleter(rolesCommand);
        }

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockProgressListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatProgressListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractionProgressListener(this), this);

        scheduleTasks();
        getLogger().info("MerchantRole enabled. Economy provider: " + economy.providerName());
        if (!economy.available()) {
            getLogger().warning("No economy provider found. Paid upgrades will be unavailable until QuickShop-Hikari/Vault is configured.");
        }
    }

    private void scheduleTasks() {
        if (scanTask != null) {
            scanTask.cancel();
        }
        if (saveTask != null) {
            saveTask.cancel();
        }

        long scanInterval = settings.inventoryScanIntervalTicks();
        scanTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerProfile profile = store.get(player.getUniqueId());
                if (profile != null) {
                    progress.scanInventory(player, profile);
                }
            }
        }, scanInterval, scanInterval);

        long saveInterval = 20L * settings.saveIntervalSeconds();
        saveTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            store.flush();
            for (Player player : Bukkit.getOnlinePlayers()) {
                retryPendingClaims(player);
            }
        }, saveInterval, saveInterval);
    }

    public void reloadPlugin() {
        reloadConfig();
        settings = new PluginSettings(this);
        economy.refresh();
        scheduleTasks();
    }

    public void grantRewards(Player player, PlayerProfile profile, Rank target) {
        PluginSettings.Reward reward = settings.reward(target);
        String prefix = settings.prefix();

        if (reward.money() > 0) {
            if (economy.available() && economy.deposit(player, reward.money())) {
                player.sendMessage(prefix + "§aReward: §e" + economy.format(reward.money()));
            } else {
                getLogger().warning("Could not deposit the money reward for " + player.getName()
                        + " (" + reward.money() + ").");
                player.sendMessage(prefix + "§cThe money reward could not be deposited. Please contact an admin.");
            }
        }

        if (reward.claimBlocks() > 0) {
            if (claims.grant(player, reward.claimBlocks())) {
                player.sendMessage(prefix + "§aReward: §e" + reward.claimBlocks() + " claim blocks");
            } else {
                int pending = profile.counter("pending_claim_blocks") + reward.claimBlocks();
                profile.setCounter("pending_claim_blocks", pending);
                player.sendMessage(prefix + "§e" + reward.claimBlocks()
                        + " claim blocks are pending until GriefPrevention is available.");
            }
        }

        for (String rawCommand : reward.commands()) {
            String command = rawCommand
                    .replace("%player%", player.getName())
                    .replace("%uuid%", player.getUniqueId().toString())
                    .replace("%rank%", target.displayName())
                    .replace("%rank_key%", target.configKey());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    public void retryPendingClaims(Player player) {
        PlayerProfile profile = store.get(player.getUniqueId());
        if (profile == null) {
            return;
        }
        int pending = profile.counter("pending_claim_blocks");
        if (pending <= 0 || !claims.grant(player, pending)) {
            return;
        }
        profile.setCounter("pending_claim_blocks", 0);
        player.sendMessage(settings.prefix() + "§aYour pending reward of §e" + pending
                + " claim blocks §ahas been granted.");
    }

    @Override
    public void onDisable() {
        if (scanTask != null) {
            scanTask.cancel();
        }
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (store != null) {
            store.flush();
        }
    }

    public PluginSettings settings() {
        return settings;
    }

    public PlayerDataStore store() {
        return store;
    }

    public ProgressService progress() {
        return progress;
    }

    public EconomyService economy() {
        return economy;
    }

    public ClaimRewardService claims() {
        return claims;
    }

    public LuckPermsHook luckPerms() {
        return luckPerms;
    }

    public WorldGuardHook worldGuard() {
        return worldGuard;
    }
}

package com.bap80.merchantrole;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PluginSettings {
    public record Reward(double money, int claimBlocks, List<String> commands) {
    }

    private final int inventoryScanIntervalTicks;
    private final int saveIntervalSeconds;
    private final String rankupRegion;
    private final boolean syncGroupsOnJoin;
    private final String prefix;
    private final double upgradeCost;
    private final boolean firstUpgradeFree;
    private final double rankupBaseCost;
    private final double rankupIncrease;
    private final boolean luckPermsEnabled;
    private final boolean luckPermsUseParentSet;
    private final Map<Rank, String> groups;
    private final Map<Rank, Reward> rewards;

    public PluginSettings(MerchantRolePlugin plugin) {
        inventoryScanIntervalTicks = Math.max(20,
                plugin.getConfig().getInt("settings.inventory-scan-interval-ticks", 100));
        saveIntervalSeconds = Math.max(5,
                plugin.getConfig().getInt("settings.save-interval-seconds", 30));
        rankupRegion = plugin.getConfig().getString("settings.rankup-region", "");
        syncGroupsOnJoin = plugin.getConfig().getBoolean("settings.sync-groups-on-join", true);
        prefix = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("settings.prefix", "&6[MerchantRole] &r"));

        upgradeCost = Math.max(0, plugin.getConfig().getDouble("economy.upgrade-cost", 5000));
        firstUpgradeFree = plugin.getConfig().getBoolean("economy.first-upgrade-free", true);
        rankupBaseCost = Math.max(0, plugin.getConfig().getDouble("economy.rankup-base-cost", 10000));
        rankupIncrease = Math.max(0,
                plugin.getConfig().getDouble("economy.rankup-increase-per-rankup", 5000));

        luckPermsEnabled = plugin.getConfig().getBoolean("luckperms.enabled", true);
        luckPermsUseParentSet = plugin.getConfig().getBoolean("luckperms.use-parent-set", false);

        groups = new EnumMap<>(Rank.class);
        for (Rank rank : Rank.values()) {
            String fallback = rank.configKey().replace('-', '_');
            groups.put(rank, plugin.getConfig().getString(
                    "luckperms.groups." + rank.configKey(), fallback));
        }

        rewards = new EnumMap<>(Rank.class);
        for (Rank rank : Rank.values()) {
            double defaultMoney = switch (rank) {
                case FIGHTER_I -> 10000;
                case EXPLORER_I -> 15000;
                default -> 0;
            };
            int defaultClaim = rank == Rank.FARMER_I ? 200 : 0;
            String path = "rewards." + rank.configKey();
            double money = Math.max(0, plugin.getConfig().getDouble(path + ".money", defaultMoney));
            int claim = Math.max(0, plugin.getConfig().getInt(path + ".claim-blocks", defaultClaim));
            List<String> commands = new ArrayList<>(plugin.getConfig().getStringList(path + ".commands"));
            rewards.put(rank, new Reward(money, claim, List.copyOf(commands)));
        }
    }

    public int inventoryScanIntervalTicks() {
        return inventoryScanIntervalTicks;
    }

    public int saveIntervalSeconds() {
        return saveIntervalSeconds;
    }

    public String rankupRegion() {
        return rankupRegion;
    }

    public boolean syncGroupsOnJoin() {
        return syncGroupsOnJoin;
    }

    public String prefix() {
        return prefix;
    }

    public double upgradeCost(Rank current, Rank target) {
        if (current == Rank.PEASANTS && firstUpgradeFree) {
            return 0;
        }
        return upgradeCost;
    }

    public double rankupCost(PlayerProfile profile) {
        return rankupBaseCost + (rankupIncrease * profile.rankupCount());
    }

    public boolean luckPermsEnabled() {
        return luckPermsEnabled;
    }

    public boolean luckPermsUseParentSet() {
        return luckPermsUseParentSet;
    }

    public String group(Rank rank) {
        return groups.getOrDefault(rank, "");
    }

    public Reward reward(Rank rank) {
        return rewards.getOrDefault(rank, new Reward(0, 0, List.of()));
    }
}

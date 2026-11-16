package com.bap80.merchantrole;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Loads and saves players.yml. All methods are called from the main server
 * thread, matching Bukkit's configuration access rules.
 */
public final class PlayerDataStore {
    private final MerchantRolePlugin plugin;
    private final File file;
    private final Map<UUID, PlayerProfile> profiles = new LinkedHashMap<>();

    public PlayerDataStore(MerchantRolePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public void load() {
        profiles.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }

        for (String rawUuid : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(rawUuid);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoring invalid UUID in players.yml: " + rawUuid);
                continue;
            }

            ConfigurationSection section = players.getConfigurationSection(rawUuid);
            if (section == null) {
                continue;
            }

            PlayerProfile profile = new PlayerProfile(uuid);
            profile.setName(section.getString("name", ""));
            profile.setRank(Rank.fromKey(section.getString("rank", "peasants")));
            profile.setRankupCount(section.getInt("rankup-count", 0));

            ConfigurationSection counters = section.getConfigurationSection("counters");
            if (counters != null) {
                for (String key : counters.getKeys(false)) {
                    profile.setCounter(key, counters.getInt(key, 0));
                }
            }

            for (String flag : section.getStringList("flags")) {
                profile.setFlag(flag);
            }

            ConfigurationSection sets = section.getConfigurationSection("sets");
            if (sets != null) {
                for (String key : sets.getKeys(false)) {
                    for (String value : sets.getStringList(key)) {
                        profile.addToSet(key, value);
                    }
                }
            }

            profile.clearDirty();
            profiles.put(uuid, profile);
        }
    }

    public PlayerProfile get(UUID uuid) {
        return profiles.get(uuid);
    }

    public PlayerProfile getOrCreate(Player player) {
        PlayerProfile profile = profiles.get(player.getUniqueId());
        if (profile == null) {
            profile = new PlayerProfile(player.getUniqueId());
            profile.setName(player.getName());
            profile.markDirty();
            profiles.put(player.getUniqueId(), profile);
            return profile;
        }
        profile.setName(player.getName());
        return profile;
    }

    public Collection<PlayerProfile> all() {
        return Collections.unmodifiableCollection(profiles.values());
    }

    public void markDirty(PlayerProfile profile) {
        if (profile != null) {
            profile.markDirty();
        }
    }

    /** Writes the whole file when at least one profile has changed. */
    public void flush() {
        boolean dirty = profiles.values().stream().anyMatch(PlayerProfile::isDirty);
        if (!dirty) {
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        for (PlayerProfile profile : profiles.values()) {
            ConfigurationSection section = yaml.createSection("players." + profile.uuid());
            section.set("name", profile.name());
            section.set("rank", profile.rank().configKey());
            section.set("rankup-count", profile.rankupCount());
            section.set("counters", new LinkedHashMap<>(profile.counters()));
            section.set("flags", new ArrayList<>(profile.flags()));

            if (!profile.sets().isEmpty()) {
                ConfigurationSection sets = section.createSection("sets");
                for (Map.Entry<String, java.util.Set<String>> entry : profile.sets().entrySet()) {
                    sets.set(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }
        }

        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create the plugin data folder.");
            }
            yaml.save(file);
            for (PlayerProfile profile : profiles.values()) {
                profile.clearDirty();
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save players.yml: " + exception.getMessage());
        }
    }

    /** Used by the command layer to show every task as complete after the final rank. */
    public List<PlayerProfile> snapshot() {
        return new ArrayList<>(profiles.values());
    }
}

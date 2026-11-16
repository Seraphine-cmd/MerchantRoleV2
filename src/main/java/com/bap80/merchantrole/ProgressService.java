package com.bap80.merchantrole;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ProgressService {
    private static final String TRIM_SUFFIX = "_ARMOR_TRIM_SMITHING_TEMPLATE";
    private static final String SHERD_SUFFIX = "_POTTERY_SHERD";

    private final MerchantRolePlugin plugin;

    public ProgressService(MerchantRolePlugin plugin) {
        this.plugin = plugin;
    }

    public Rank targetRank(PlayerProfile profile) {
        return profile.rank().next();
    }

    public List<TaskResult> results(Player player, PlayerProfile profile) {
        Rank target = targetRank(profile);
        if (target == null) {
            return List.of();
        }
        scanInventory(player, profile);
        ProgressContext context = new ProgressContext(plugin, player, profile, target);
        List<TaskResult> results = new ArrayList<>();
        for (TaskDefinition definition : TaskRegistry.forRank(target)) {
            results.add(new TaskResult(definition, definition.evaluator().apply(context)));
        }
        return results;
    }

    public boolean isComplete(Player player, PlayerProfile profile) {
        List<TaskResult> results = results(player, profile);
        return !results.isEmpty() && results.stream().allMatch(TaskResult::complete);
    }

    /**
     * Updates "highest ever held" counters and unique-item sets. This is
     * intentionally cheap: one inventory pass per player per scan interval.
     */
    public void scanInventory(Player player, PlayerProfile profile) {
        Map<Material, Integer> counts = new EnumMap<>(Material.class);

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            Material material = item.getType();
            counts.merge(material, item.getAmount(), Integer::sum);

            if (ItemUtils.DIAMOND_TOOLS.contains(material)) {
                profile.addToSet("diamond_tools", material.name());
            }
            if (ItemUtils.DIAMOND_ARMOR.contains(material)) {
                profile.addToSet("diamond_armor", material.name());
            }
            if (material == Material.DIAMOND_SWORD) {
                profile.addToSet("diamond_sword", material.name());
            }

            String name = material.name();
            if (name.endsWith(TRIM_SUFFIX)) {
                String trim = name.substring(0, name.length() - TRIM_SUFFIX.length())
                        .toLowerCase(Locale.ROOT);
                profile.addToSet("armor_trims", trim);
            }
            if (name.endsWith(SHERD_SUFFIX)) {
                String sherd = name.substring(0, name.length() - SHERD_SUFFIX.length())
                        .toLowerCase(Locale.ROOT);
                profile.addToSet("pottery_sherds", sherd);
            }

            if (item.getItemMeta() instanceof ArmorMeta armorMeta && armorMeta.hasTrim()) {
                String pattern = armorMeta.getTrim().getPattern().getKey().getKey();
                profile.addToSet("armor_trims", pattern.toLowerCase(Locale.ROOT));
            }
        }

        profile.maxCounter("max.diamond", counts.getOrDefault(Material.DIAMOND, 0));
        profile.maxCounter("max.raw_iron", counts.getOrDefault(Material.RAW_IRON, 0));
        profile.maxCounter("max.raw_gold", counts.getOrDefault(Material.RAW_GOLD, 0));
        profile.maxCounter("max.raw_copper", counts.getOrDefault(Material.RAW_COPPER, 0));
        profile.maxCounter("max.lapis_lazuli", counts.getOrDefault(Material.LAPIS_LAZULI, 0));
        profile.maxCounter("max.heart_of_the_sea", counts.getOrDefault(Material.HEART_OF_THE_SEA, 0));
        profile.maxCounter("max.sniffer_egg", counts.getOrDefault(Material.SNIFFER_EGG, 0));
        profile.maxCounter("max.milk_honey",
                counts.getOrDefault(Material.MILK_BUCKET, 0)
                        + counts.getOrDefault(Material.HONEY_BOTTLE, 0));
        profile.maxCounter("max.torchflower_seeds",
                counts.getOrDefault(Material.TORCHFLOWER_SEEDS, 0)
                        + counts.getOrDefault(Material.PITCHER_POD, 0));
    }
}

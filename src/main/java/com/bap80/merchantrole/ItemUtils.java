package com.bap80.merchantrole;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ItemUtils {
    public static final Set<Material> DIAMOND_TOOLS = EnumSet.of(
            Material.DIAMOND_PICKAXE,
            Material.DIAMOND_AXE,
            Material.DIAMOND_SHOVEL,
            Material.DIAMOND_SWORD
    );

    public static final Set<Material> DIAMOND_ARMOR = EnumSet.of(
            Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_BOOTS
    );

    private ItemUtils() {
    }

    public static boolean hasDiamondTool(Player player, Material material) {
        if (player.getInventory().contains(material)) {
            return true;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAllDiamondTools(Player player) {
        for (Material material : DIAMOND_TOOLS) {
            if (!hasDiamondTool(player, material)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasAllDiamondArmor(Player player) {
        for (Material material : DIAMOND_ARMOR) {
            if (!hasDiamondTool(player, material)) {
                return false;
            }
        }
        return true;
    }

    public static int countHighTierTools(Player player) {
        int count = 0;
        for (Material material : DIAMOND_TOOLS) {
            if (hasHighTierTool(player, material)) {
                count++;
            }
        }
        return count;
    }

    public static String highTierToolStatus(Player player) {
        List<String> parts = new ArrayList<>();
        for (Material material : DIAMOND_TOOLS) {
            String name = switch (material) {
                case DIAMOND_PICKAXE -> "Pickaxe";
                case DIAMOND_AXE -> "Axe";
                case DIAMOND_SHOVEL -> "Shovel";
                default -> "Sword";
            };
            parts.add(name + ":" + (hasHighTierTool(player, material) ? "yes" : "no"));
        }
        return String.join(", ", parts);
    }

    public static boolean hasHighTierTool(Player player, Material material) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && hasHighTierEnchant(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasHighTierEnchant(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        Map<Enchantment, Integer> enchantments = item.getEnchantments();
        return level(enchantments, Enchantment.EFFICIENCY) >= 4
                || level(enchantments, Enchantment.FORTUNE) >= 2
                || level(enchantments, Enchantment.SILK_TOUCH) >= 1
                || level(enchantments, Enchantment.UNBREAKING) >= 3
                || level(enchantments, Enchantment.MENDING) >= 1
                || level(enchantments, Enchantment.SHARPNESS) >= 4
                || level(enchantments, Enchantment.SMITE) >= 4
                || level(enchantments, Enchantment.BANE_OF_ARTHROPODS) >= 4
                || level(enchantments, Enchantment.LOOTING) >= 2
                || level(enchantments, Enchantment.FIRE_ASPECT) >= 1
                || level(enchantments, Enchantment.SWEEPING_EDGE) >= 2
                || level(enchantments, Enchantment.POWER) >= 4
                || level(enchantments, Enchantment.PUNCH) >= 2
                || level(enchantments, Enchantment.FLAME) >= 1
                || level(enchantments, Enchantment.INFINITY) >= 1
                || level(enchantments, Enchantment.LOYALTY) >= 2
                || level(enchantments, Enchantment.IMPALING) >= 3
                || level(enchantments, Enchantment.RIPTIDE) >= 2;
    }

    public static boolean hasRequiredSwordEnchants(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_SWORD) {
            return false;
        }
        Map<Enchantment, Integer> enchantments = item.getEnchantments();
        return level(enchantments, Enchantment.SHARPNESS) >= 5
                && level(enchantments, Enchantment.FIRE_ASPECT) >= 2
                && level(enchantments, Enchantment.LOOTING) >= 3
                && level(enchantments, Enchantment.MENDING) >= 1
                && level(enchantments, Enchantment.UNBREAKING) >= 3
                && level(enchantments, Enchantment.SWEEPING_EDGE) >= 3;
    }

    public static String requiredSwordStatus(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_SWORD) {
            return "Hold a Diamond Sword with Sharpness V, Fire Aspect II, Looting III, Mending, Unbreaking III and Sweeping Edge III";
        }
        Map<Enchantment, Integer> enchantments = item.getEnchantments();
        return "Sharpness " + level(enchantments, Enchantment.SHARPNESS) + "/5, "
                + "Fire Aspect " + level(enchantments, Enchantment.FIRE_ASPECT) + "/2, "
                + "Looting " + level(enchantments, Enchantment.LOOTING) + "/3, "
                + "Mending " + level(enchantments, Enchantment.MENDING) + "/1, "
                + "Unbreaking " + level(enchantments, Enchantment.UNBREAKING) + "/3, "
                + "Sweeping Edge " + level(enchantments, Enchantment.SWEEPING_EDGE) + "/3";
    }

    private static int level(Map<Enchantment, Integer> enchantments, Enchantment enchantment) {
        return enchantments.getOrDefault(enchantment, 0);
    }
}

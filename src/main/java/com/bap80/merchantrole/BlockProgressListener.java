package com.bap80.merchantrole;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public final class BlockProgressListener implements Listener {
    private final MerchantRolePlugin plugin;

    public BlockProgressListener(MerchantRolePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = plugin.store().getOrCreate(player);
        Block block = event.getBlock();
        Material type = block.getType();

        if (type == Material.DIAMOND_ORE || type == Material.DEEPSLATE_DIAMOND_ORE) {
            profile.addCounter("mined_diamond_ore", 1);
        }
        if (isSapling(type)) {
            profile.removeFromSet("pending_saplings", locationKey(block.getLocation()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        PlayerProfile profile = plugin.store().getOrCreate(player);
        BlockState state = event.getBlockState();
        Material broken = state.getType();
        boolean mature = isMature(state);

        for (Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            Material dropped = stack.getType();
            int amount = stack.getAmount();

            if (mature && isPrimaryCrop(broken) && isPrimaryCropDrop(dropped)) {
                profile.addCounter("primary_crops", amount);
            }
            if ((broken == Material.PUMPKIN || broken == Material.MELON)
                    && (dropped == Material.PUMPKIN || dropped == Material.MELON
                    || dropped == Material.MELON_SLICE)) {
                profile.addCounter("pumpkins_melons", amount);
            }
            if (broken == Material.NETHER_WART && mature && dropped == Material.NETHER_WART) {
                profile.addCounter("nether_wart", amount);
            }
            if ((broken == Material.SUGAR_CANE && dropped == Material.SUGAR_CANE)
                    || (broken == Material.CACTUS && dropped == Material.CACTUS)) {
                profile.addCounter("sugar_cane_cactus", amount);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = plugin.store().getOrCreate(player);
        Block block = event.getBlockPlaced();
        Material type = block.getType();

        if (isSapling(type)) {
            profile.addCounter("plant_saplings", 1);
            profile.addToSet("pending_saplings", locationKey(block.getLocation()));
        }
        if (type == Material.SNIFFER_EGG) {
            profile.addToSet("pending_sniffer_eggs", locationKey(block.getLocation()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        Location grownAt = event.getLocation();
        for (PlayerProfile profile : plugin.store().all()) {
            String match = null;
            for (String key : new HashSet<>(profile.set("pending_saplings"))) {
                Location plantedAt = parseLocation(key);
                if (plantedAt != null && near(grownAt, plantedAt)) {
                    match = key;
                    break;
                }
            }
            if (match != null) {
                profile.removeFromSet("pending_saplings", match);
                profile.addCounter("grow_saplings", 1);
            }
        }
    }

    private boolean isMature(BlockState state) {
        BlockData data = state.getBlockData();
        return data instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge();
    }

    private boolean isPrimaryCrop(Material material) {
        return material == Material.WHEAT
                || material == Material.CARROTS
                || material == Material.POTATOES
                || material == Material.BEETROOTS;
    }

    private boolean isPrimaryCropDrop(Material material) {
        return material == Material.WHEAT
                || material == Material.CARROT
                || material == Material.POTATO
                || material == Material.BEETROOT;
    }

    private boolean isSapling(Material material) {
        String name = material.name();
        return name.endsWith("_SAPLING") || material == Material.MANGROVE_PROPAGULE;
    }

    private boolean near(Location first, Location second) {
        return first.getWorld() != null
                && first.getWorld().equals(second.getWorld())
                && Math.abs(first.getBlockX() - second.getBlockX()) <= 3
                && Math.abs(first.getBlockY() - second.getBlockY()) <= 3
                && Math.abs(first.getBlockZ() - second.getBlockZ()) <= 3;
    }

    static String locationKey(Location location) {
        return location.getWorld().getName() + "|" + location.getBlockX()
                + "|" + location.getBlockY() + "|" + location.getBlockZ();
    }

    static Location parseLocation(String key) {
        String[] parts = key.split("\\|");
        if (parts.length != 4) {
            return null;
        }
        try {
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
            if (world == null) {
                return null;
            }
            return new Location(world, Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}

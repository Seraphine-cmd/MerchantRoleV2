package com.bap80.merchantrole;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;

public final class CombatProgressListener implements Listener {
    private final MerchantRolePlugin plugin;

    public CombatProgressListener(MerchantRolePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || event.getEntity() instanceof Player) {
            return;
        }

        PlayerProfile profile = plugin.store().getOrCreate(killer);
        Entity entity = event.getEntity();

        if (entity.getType() == EntityType.ZOMBIE && entity instanceof Zombie zombie) {
            if (zombie.isBaby()) {
                profile.addCounter("kill_baby_zombies", 1);
            } else {
                profile.addCounter("kill_zombies", 1);
            }
        }
        if (entity.getType() == EntityType.SKELETON) {
            profile.addCounter("kill_skeletons", 1);
        }

        if (isHostile(entity)) {
            ItemStack hand = killer.getInventory().getItemInMainHand();
            if (hand.getType() == Material.DIAMOND_SWORD && ItemUtils.hasRequiredSwordEnchants(hand)) {
                profile.addCounter("enchanted_sword_kills", 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) {
            return;
        }
        EntityType child = event.getEntity().getType();
        PlayerProfile profile = plugin.store().getOrCreate(player);

        if (child == EntityType.COW || child == EntityType.SHEEP
                || child == EntityType.PIG || child == EntityType.CHICKEN) {
            profile.addCounter("breed_animals", 1);
        }
        if (isMount(child)) {
            profile.addCounter("breed_tame_mounts", 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) {
            return;
        }
        if (isMount(event.getEntity().getType())) {
            plugin.store().getOrCreate(player).addCounter("breed_tame_mounts", 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        if (event.getEntity() instanceof Sheep) {
            plugin.store().getOrCreate(event.getPlayer()).addCounter("shear_sheep", 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSnifferSpawn(CreatureSpawnEvent event) {
        if (event.getEntity().getType() != EntityType.SNIFFER) {
            return;
        }

        Location spawnedAt = event.getLocation();
        for (PlayerProfile profile : plugin.store().all()) {
            String match = null;
            for (String key : new HashSet<>(profile.set("pending_sniffer_eggs"))) {
                Location egg = BlockProgressListener.parseLocation(key);
                if (egg != null && near(spawnedAt, egg, 4)) {
                    match = key;
                    break;
                }
            }
            if (match != null) {
                profile.removeFromSet("pending_sniffer_eggs", match);
                profile.setFlag("hatched_sniffer_egg");
                return;
            }
        }

        // Fallback for eggs that were not placed by this plugin: credit the
        // nearest player within 16 blocks.
        Player nearest = null;
        double best = 16 * 16;
        for (Player player : spawnedAt.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(spawnedAt);
            if (distance <= best) {
                best = distance;
                nearest = player;
            }
        }
        if (nearest != null) {
            plugin.store().getOrCreate(nearest).setFlag("hatched_sniffer_egg");
        }
    }

    private boolean isHostile(Entity entity) {
        return entity instanceof Monster
                || entity.getType() == EntityType.SLIME
                || entity.getType() == EntityType.MAGMA_CUBE
                || entity.getType() == EntityType.GHAST;
    }

    private boolean isMount(EntityType type) {
        return type == EntityType.HORSE
                || type == EntityType.LLAMA
                || type == EntityType.TRADER_LLAMA
                || type == EntityType.CAMEL;
    }

    private boolean near(Location first, Location second, int radius) {
        return first.getWorld() != null
                && first.getWorld().equals(second.getWorld())
                && Math.abs(first.getBlockX() - second.getBlockX()) <= radius
                && Math.abs(first.getBlockY() - second.getBlockY()) <= radius
                && Math.abs(first.getBlockZ() - second.getBlockZ()) <= radius;
    }
}

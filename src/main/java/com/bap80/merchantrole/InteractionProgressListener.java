package com.bap80.merchantrole;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class InteractionProgressListener implements Listener {
    private static final Set<Material> COOKED_FOOD = EnumSet.of(
            Material.COOKED_BEEF,
            Material.COOKED_PORKCHOP,
            Material.COOKED_CHICKEN,
            Material.COOKED_MUTTON,
            Material.COOKED_RABBIT,
            Material.COOKED_COD,
            Material.COOKED_SALMON,
            Material.BAKED_POTATO,
            Material.BREAD,
            Material.CAKE,
            Material.COOKIE,
            Material.PUMPKIN_PIE
    );

    private final MerchantRolePlugin plugin;
    private final Map<UUID, Long> vaultInteractions = new HashMap<>();

    public InteractionProgressListener(MerchantRolePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Material block = event.getBlock().getType();
        if (block != Material.FURNACE
                && block != Material.BLAST_FURNACE
                && block != Material.SMOKER) {
            return;
        }

        Player player = event.getPlayer();
        PlayerProfile profile = plugin.store().getOrCreate(player);
        if (event.getItemType() == Material.IRON_INGOT) {
            profile.addCounter("smelt_raw_iron", event.getItemAmount());
        }
        if (COOKED_FOOD.contains(event.getItemType())) {
            profile.addCounter("cook_food", event.getItemAmount());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        if (result != null && COOKED_FOOD.contains(result.getType())) {
            plugin.store().getOrCreate(player).addCounter("cook_food", result.getAmount());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null
                || event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block.getType() != Material.VAULT) {
            return;
        }

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long previous = vaultInteractions.put(player.getUniqueId(), now);
        if (previous != null && now - previous < 750L) {
            return;
        }

        PlayerProfile profile = plugin.store().getOrCreate(player);
        profile.setFlag("looted_trial_chamber");

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (mainHand.getType() == Material.OMINOUS_TRIAL_KEY
                || offHand.getType() == Material.OMINOUS_TRIAL_KEY) {
            profile.addCounter("ominous_vaults", 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getType() != InventoryType.COMPOSTER) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null || clicked.getType() != InventoryType.COMPOSTER) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() != Material.BONE_MEAL) {
            return;
        }

        InventoryAction action = event.getAction();
        int amount = switch (action) {
            case PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE, MOVE_TO_OTHER_INVENTORY ->
                    current.getAmount();
            default -> 0;
        };
        if (amount > 0) {
            plugin.store().getOrCreate(player).addCounter("bone_meal", amount);
        }
    }
}

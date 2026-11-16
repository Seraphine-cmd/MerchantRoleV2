package com.bap80.merchantrole;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.entity.Player;

public final class WorldGuardHook {
    private final MerchantRolePlugin plugin;

    public WorldGuardHook(MerchantRolePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isRankupAllowed(Player player) {
        String requiredRegion = plugin.settings().rankupRegion();
        if (requiredRegion == null || requiredRegion.isBlank()) {
            return true;
        }

        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform()
                    .getRegionContainer().createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(
                    BukkitAdapter.adapt(player.getLocation()));
            for (ProtectedRegion region : regions) {
                if (region.getId().equalsIgnoreCase(requiredRegion)) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("WorldGuard region check failed: " + exception.getMessage());
            return false;
        }
    }
}

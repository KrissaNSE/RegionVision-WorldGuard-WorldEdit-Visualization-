package com.regionvision.utils;

import com.regionvision.RegionVisionPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WorldGuardUtil {

    private final RegionVisionPlugin plugin;

    public WorldGuardUtil(RegionVisionPlugin plugin) {
        this.plugin = plugin;
    }

    public ProtectedRegion getRegionAt(Location loc) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(loc.getWorld()));
        if (regions == null) return null;

        ApplicableRegionSet set = regions.getApplicableRegions(BukkitAdapter.asBlockVector(loc));
        return set.getRegions().stream()
                .max((r1, r2) -> Integer.compare(r1.getPriority(), r2.getPriority()))
                .orElse(null);
    }

    public ProtectedRegion getRegionByName(World world, String name) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(world));
        if (regions == null) return null;
        return regions.getRegion(name);
    }
    
    // New Helper for Tab Completion
    public Set<String> getRegionNames(World world) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(world));
        if (regions == null) return Collections.emptySet();
        return regions.getRegions().keySet();
    }

    public Set<ProtectedRegion> getRegionsNear(Location center, int radius) {
        Set<ProtectedRegion> result = new HashSet<>();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(center.getWorld()));
        if (regions == null) return result;

        BlockVector3 min = BukkitAdapter.asBlockVector(center.clone().subtract(radius, radius, radius));
        BlockVector3 max = BukkitAdapter.asBlockVector(center.clone().add(radius, radius, radius));
        
        for (ProtectedRegion region : regions.getRegions().values()) {
             if (region.getMinimumPoint().getX() <= max.getX() && region.getMaximumPoint().getX() >= min.getX() &&
                 region.getMinimumPoint().getZ() <= max.getZ() && region.getMaximumPoint().getZ() >= min.getZ()) {
                 result.add(region);
             }
        }
        return result;
    }

    public boolean canBuild(Player player, Location loc) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        var query = container.createQuery();
        var localPlayer = com.sk89q.worldguard.bukkit.WorldGuardPlugin.inst().wrapPlayer(player);
        return query.testState(BukkitAdapter.adapt(loc), localPlayer, Flags.BUILD);
    }
    
    public boolean isMemberOrOwner(Player player, ProtectedRegion region) {
        var localPlayer = com.sk89q.worldguard.bukkit.WorldGuardPlugin.inst().wrapPlayer(player);
        return region.isMember(localPlayer);
    }
}
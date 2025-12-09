package com.regionvision.listeners;

import com.regionvision.RegionVisionPlugin;
import com.regionvision.managers.PermanentRegionManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class RegionEnterListener implements Listener {

    private final RegionVisionPlugin plugin;
    // Track which permanent regions the player is currently inside to avoid spamming the message
    private final ConcurrentMap<UUID, Set<String>> playerRegions = new ConcurrentHashMap<>();

    public RegionEnterListener(RegionVisionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Optimization: Only run if the player moved to a different block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        PermanentRegionManager pm = plugin.getPermanentRegionManager();
        if (pm == null) return;

        // Get WorldGuard regions at new location
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
        if (regionManager == null) return;

        ApplicableRegionSet set = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(event.getTo()));
        
        Set<String> currentRegions = set.getRegions().stream()
                .map(ProtectedRegion::getId)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> previousRegions = playerRegions.getOrDefault(player.getUniqueId(), new HashSet<>());

        // Check for newly entered regions
        for (String regionId : currentRegions) {
            // If we weren't in it before, and it is a tracked permanent region
            if (!previousRegions.contains(regionId) && pm.isPermanent(regionId)) {
                triggerNotification(player, regionId);
            }
        }

        // Update cache
        if (currentRegions.isEmpty()) {
            playerRegions.remove(player.getUniqueId());
        } else {
            playerRegions.put(player.getUniqueId(), currentRegions);
        }
    }

    private void triggerNotification(Player player, String regionId) {
        PermanentRegionManager pm = plugin.getPermanentRegionManager();
        // We iterate to find the settings because our map key might differ in case slightly, 
        // though we enforced lowercase in manager. 
        // Best to just fetch from manager directly.
        PermanentRegionManager.RegionSettings settings = pm.getAllRegions().stream()
                .filter(s -> s.regionId.equalsIgnoreCase(regionId))
                .findFirst()
                .orElse(null);

        if (settings == null || "NONE".equalsIgnoreCase(settings.notificationType)) return;

        Component message = MiniMessage.miniMessage().deserialize(settings.notificationMessage);

        switch (settings.notificationType.toUpperCase()) {
            case "ACTION_BAR" -> player.sendActionBar(message);
            case "TITLE" -> {
                Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000));
                Title title = Title.title(message, Component.empty(), times);
                player.showTitle(title);
            }
            case "BOSS_BAR" -> {
                BossBar bossBar = BossBar.bossBar(message, 1.0f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
                player.showBossBar(bossBar);
                // Hide boss bar after 5 seconds
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.hideBossBar(bossBar), 100L);
            }
        }
    }
}
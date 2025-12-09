package com.regionvision.managers;

import com.regionvision.RegionVisionPlugin;
import com.regionvision.utils.GeometryUtil;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VisualizerManager {

    private final RegionVisionPlugin plugin;
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> activeSelectionTasks = new ConcurrentHashMap<>();
    
    private final Map<UUID, Long> staticRegionRequestIds = new ConcurrentHashMap<>();
    private final Map<UUID, Long> selectionRequestIds = new ConcurrentHashMap<>();

    private static final float PARTICLE_SIZE_REGION = 2.5f; 
    private static final float PARTICLE_SIZE_SELECTION = 2.0f;
    
    private BukkitTask globalPermanentTask;

    public VisualizerManager(RegionVisionPlugin plugin) {
        this.plugin = plugin;
        startGlobalPermanentTask();
    }

    private void startGlobalPermanentTask() {
        globalPermanentTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            var manager = plugin.getPermanentRegionManager();
            if (manager == null) return;

            for (PermanentRegionManager.RegionSettings settings : manager.getAllRegions()) {
                // Skip if particles are disabled for this region
                if (!settings.showParticles) continue;

                List<Location> points = manager.getCachedGeometry(settings.regionId);
                if (points == null || points.isEmpty()) continue;

                Particle.DustOptions options = new Particle.DustOptions(settings.color, PARTICLE_SIZE_REGION);
                
                org.bukkit.World w = Bukkit.getWorld(settings.worldName);
                if (w == null) continue;

                for (Player p : w.getPlayers()) {
                    if (points.get(0).distanceSquared(p.getLocation()) < (settings.viewDistance * settings.viewDistance)) {
                        for (Location loc : points) {
                            p.spawnParticle(Particle.DUST, loc, 1, options);
                        }
                    }
                }
            }
        }, 20L, 10L);
    }

    public void showRegion(Player player, ProtectedRegion region) {
        clearPlayerParticles(player);

        long requestId = System.nanoTime();
        staticRegionRequestIds.put(player.getUniqueId(), requestId);

        final boolean canBuild = plugin.getWorldGuardUtil().isMemberOrOwner(player, region);
        final Color color = canBuild ? getColor("allowed") : getColor("denied");
        final double density = plugin.getConfig().getDouble("visualizer.particle-density", 0.25);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Vector min = new Vector(region.getMinimumPoint().getX(), region.getMinimumPoint().getY(), region.getMinimumPoint().getZ());
            Vector max = new Vector(region.getMaximumPoint().getX(), region.getMaximumPoint().getY(), region.getMaximumPoint().getZ());

            List<Location> points = GeometryUtil.getCuboidWireframe(player.getWorld(), min, max, density);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                
                if (staticRegionRequestIds.getOrDefault(player.getUniqueId(), -1L) != requestId) return;

                BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    if (!player.isOnline()) return;
                    Particle.DustOptions dustOptions = new Particle.DustOptions(color, PARTICLE_SIZE_REGION);
                    for (Location loc : points) {
                        player.spawnParticle(Particle.DUST, loc, 1, dustOptions);
                    }
                }, 0L, 10L); 

                activeTasks.put(player.getUniqueId(), task);

                long duration = plugin.getConfig().getLong("visualizer.duration", 15) * 20L;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (activeTasks.get(player.getUniqueId()) == task) {
                        task.cancel();
                        activeTasks.remove(player.getUniqueId());
                    }
                }, duration);
            });
        });
    }

    public void updateSelectionVisualization(Player player) {
        if (!plugin.hasWorldEdit()) return;

        if (activeSelectionTasks.containsKey(player.getUniqueId())) {
            activeSelectionTasks.get(player.getUniqueId()).cancel();
            activeSelectionTasks.remove(player.getUniqueId());
        }
        
        long requestId = System.nanoTime();
        selectionRequestIds.put(player.getUniqueId(), requestId);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                com.sk89q.worldedit.LocalSession session = com.sk89q.worldedit.WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
                if (session == null) return;
                
                Region selection = session.getSelection(session.getSelectionWorld());
                if (selection == null || selection.getArea() < 2) return;

                Vector min = new Vector(selection.getMinimumPoint().getX(), selection.getMinimumPoint().getY(), selection.getMinimumPoint().getZ());
                Vector max = new Vector(selection.getMaximumPoint().getX(), selection.getMaximumPoint().getY(), selection.getMaximumPoint().getZ());
                
                double density = plugin.getConfig().getDouble("visualizer.particle-density", 0.25);
                List<Location> points = GeometryUtil.getCuboidWireframe(player.getWorld(), min, max, density);
                Color color = getColor("selection");

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    
                    if (selectionRequestIds.getOrDefault(player.getUniqueId(), -1L) != requestId) return;

                    BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                        if (!player.isOnline()) {
                            BukkitTask t = activeSelectionTasks.remove(player.getUniqueId());
                            if(t != null) t.cancel();
                            return;
                        }
                        
                        if (player.getInventory().getItemInMainHand().getType() != Material.WOODEN_AXE) {
                            return;
                        }

                        Particle.DustOptions dustOptions = new Particle.DustOptions(color, PARTICLE_SIZE_SELECTION);
                        for (Location loc : points) {
                            player.spawnParticle(Particle.DUST, loc, 1, dustOptions);
                        }
                    }, 0L, 10L); 

                    activeSelectionTasks.put(player.getUniqueId(), task);
                });
                
            } catch (IncompleteRegionException ignored) {
            }
        });
    }

    public void clearPlayerParticles(Player player) {
        if (activeTasks.containsKey(player.getUniqueId())) {
            activeTasks.get(player.getUniqueId()).cancel();
            activeTasks.remove(player.getUniqueId());
        }
        staticRegionRequestIds.remove(player.getUniqueId());
    }
    
    public void clearAllParticlesForPlayer(Player player) {
        clearPlayerParticles(player);
        if (activeSelectionTasks.containsKey(player.getUniqueId())) {
            activeSelectionTasks.get(player.getUniqueId()).cancel();
            activeSelectionTasks.remove(player.getUniqueId());
        }
        selectionRequestIds.remove(player.getUniqueId());
    }

    public void stopAll() {
        if (globalPermanentTask != null) globalPermanentTask.cancel();
        activeTasks.values().forEach(BukkitTask::cancel);
        activeSelectionTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
        activeSelectionTasks.clear();
        staticRegionRequestIds.clear();
        selectionRequestIds.clear();
    }

    private Color getColor(String key) {
        int r = plugin.getConfig().getInt("colors." + key + ".r");
        int g = plugin.getConfig().getInt("colors." + key + ".g");
        int b = plugin.getConfig().getInt("colors." + key + ".b");
        return Color.fromRGB(r, g, b);
    }
}
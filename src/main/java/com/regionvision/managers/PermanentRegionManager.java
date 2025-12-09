package com.regionvision.managers;

import com.regionvision.RegionVisionPlugin;
import com.regionvision.utils.GeometryUtil;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PermanentRegionManager {

    private final RegionVisionPlugin plugin;
    private final File file;
    private FileConfiguration config;
    
    private final Map<String, List<Location>> geometryCache = new ConcurrentHashMap<>();
    private final Map<String, RegionSettings> regions = new ConcurrentHashMap<>();

    public PermanentRegionManager(RegionVisionPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "permanent_regions.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        regions.clear();
        geometryCache.clear();

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            RegionSettings settings = new RegionSettings(
                key,
                section.getString("world"),
                Color.fromRGB(
                    section.getInt("color.r", 255),
                    section.getInt("color.g", 0),
                    section.getInt("color.b", 0)
                ),
                section.getDouble("density", 0.5),
                section.getInt("view-distance", 50),
                section.getString("notification-type", "NONE"),
                section.getString("notification-message", ""),
                section.getBoolean("show-particles", true) // Default to showing particles
            );
            regions.put(key.toLowerCase(), settings);
            cacheGeometry(settings);
        }
    }

    public void addRegion(ProtectedRegion region, World world) {
        String id = region.getId();
        RegionSettings settings = new RegionSettings(
            id,
            world.getName(),
            Color.RED,
            0.5,
            50,
            "NONE",
            "",
            true
        );
        regions.put(id.toLowerCase(), settings);
        saveRegionToDisk(settings);
        cacheGeometry(settings);
    }

    public void removeRegion(String regionId) {
        regions.remove(regionId.toLowerCase());
        geometryCache.remove(regionId.toLowerCase());
        config.set(regionId.toLowerCase(), null);
        saveConfig();
    }

    public void updateColor(String regionId, int r, int g, int b) {
        RegionSettings settings = regions.get(regionId.toLowerCase());
        if (settings != null) {
            settings.color = Color.fromRGB(r, g, b);
            saveRegionToDisk(settings);
        }
    }
    
    public void updateDensity(String regionId, double density) {
        RegionSettings settings = regions.get(regionId.toLowerCase());
        if (settings != null) {
            settings.density = density;
            saveRegionToDisk(settings);
            cacheGeometry(settings);
        }
    }
    
    public void updateViewDistance(String regionId, int distance) {
        RegionSettings settings = regions.get(regionId.toLowerCase());
        if (settings != null) {
            settings.viewDistance = distance;
            saveRegionToDisk(settings);
        }
    }
    
    public void updateNotification(String regionId, String type, String message) {
        RegionSettings settings = regions.get(regionId.toLowerCase());
        if (settings != null) {
            settings.notificationType = type;
            settings.notificationMessage = message;
            saveRegionToDisk(settings);
        }
    }

    public void updateParticles(String regionId, boolean state) {
        RegionSettings settings = regions.get(regionId.toLowerCase());
        if (settings != null) {
            settings.showParticles = state;
            saveRegionToDisk(settings);
        }
    }

    public Collection<RegionSettings> getAllRegions() {
        return regions.values();
    }
    
    public List<Location> getCachedGeometry(String regionId) {
        return geometryCache.get(regionId.toLowerCase());
    }
    
    public boolean isPermanent(String regionId) {
        return regions.containsKey(regionId.toLowerCase());
    }

    private void cacheGeometry(RegionSettings settings) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            World world = Bukkit.getWorld(settings.worldName);
            if (world == null) return;
            
            ProtectedRegion region = plugin.getWorldGuardUtil().getRegionByName(world, settings.regionId);
            if (region == null) return;

            Vector min = new Vector(region.getMinimumPoint().getX(), region.getMinimumPoint().getY(), region.getMinimumPoint().getZ());
            Vector max = new Vector(region.getMaximumPoint().getX(), region.getMaximumPoint().getY(), region.getMaximumPoint().getZ());

            List<Location> points = GeometryUtil.getCuboidWireframe(world, min, max, settings.density);
            geometryCache.put(settings.regionId.toLowerCase(), points);
        });
    }

    private void saveRegionToDisk(RegionSettings s) {
        String path = s.regionId.toLowerCase();
        config.set(path + ".world", s.worldName);
        config.set(path + ".color.r", s.color.getRed());
        config.set(path + ".color.g", s.color.getGreen());
        config.set(path + ".color.b", s.color.getBlue());
        config.set(path + ".density", s.density);
        config.set(path + ".view-distance", s.viewDistance);
        config.set(path + ".notification-type", s.notificationType);
        config.set(path + ".notification-message", s.notificationMessage);
        config.set(path + ".show-particles", s.showParticles);
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class RegionSettings {
        public String regionId;
        public String worldName;
        public Color color;
        public double density;
        public int viewDistance;
        public String notificationType;
        public String notificationMessage;
        public boolean showParticles;

        public RegionSettings(String regionId, String worldName, Color color, double density, int viewDistance, String notificationType, String notificationMessage, boolean showParticles) {
            this.regionId = regionId;
            this.worldName = worldName;
            this.color = color;
            this.density = density;
            this.viewDistance = viewDistance;
            this.notificationType = notificationType;
            this.notificationMessage = notificationMessage;
            this.showParticles = showParticles;
        }
    }
}
package com.regionvision;

import com.regionvision.listeners.RegionEnterListener;
import com.regionvision.listeners.WorldEditListener;
import com.regionvision.managers.CommandManager;
import com.regionvision.managers.PermanentRegionManager;
import com.regionvision.managers.VisualizerManager;
import com.regionvision.utils.WorldGuardUtil;
import org.bstats.bukkit.Metrics; 
import org.bukkit.plugin.java.JavaPlugin;

public class RegionVisionPlugin extends JavaPlugin {

    private static RegionVisionPlugin instance;
    private VisualizerManager visualizerManager;
    private PermanentRegionManager permanentRegionManager;
    private WorldGuardUtil worldGuardUtil;
    private boolean hasWorldGuard = false;
    private boolean hasWorldEdit = false;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // --- bStats Metrics ---
        int pluginId = 28269; 
        Metrics metrics = new Metrics(this, pluginId);
        
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.hasWorldGuard = true;
            this.worldGuardUtil = new WorldGuardUtil(this);
            getLogger().info("Hooked into WorldGuard!");
        } else {
            getLogger().warning("WorldGuard not found! Region features disabled.");
        }

        if (getServer().getPluginManager().getPlugin("WorldEdit") != null) {
            this.hasWorldEdit = true;
            getLogger().info("Hooked into WorldEdit!");
        } else {
            getLogger().warning("WorldEdit not found! Selection features disabled.");
        }

        this.permanentRegionManager = new PermanentRegionManager(this);
        this.visualizerManager = new VisualizerManager(this);
        
        // Register Command & Tab Completer
        CommandManager cmdManager = new CommandManager(this);
        getCommand("regionvision").setExecutor(cmdManager);
        getCommand("regionvision").setTabCompleter(cmdManager);

        // Register Listeners
        if (hasWorldEdit) {
            getServer().getPluginManager().registerEvents(new WorldEditListener(this), this);
        }
        
        // Register Region Enter Listener
        getServer().getPluginManager().registerEvents(new RegionEnterListener(this), this);
    }

    @Override
    public void onDisable() {
        if (visualizerManager != null) {
            visualizerManager.stopAll();
        }
        instance = null;
    }

    public static RegionVisionPlugin getInstance() {
        return instance;
    }

    public VisualizerManager getVisualizerManager() {
        return visualizerManager;
    }
    
    public PermanentRegionManager getPermanentRegionManager() {
        return permanentRegionManager;
    }

    public WorldGuardUtil getWorldGuardUtil() {
        return worldGuardUtil;
    }

    public boolean hasWorldGuard() {
        return hasWorldGuard;
    }

    public boolean hasWorldEdit() {
        return hasWorldEdit;
    }
}
package com.regionvision.listeners;

import com.regionvision.RegionVisionPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

public class WorldEditListener implements Listener {

    private final RegionVisionPlugin plugin;
    private final NamespacedKey toggleKey;

    public WorldEditListener(RegionVisionPlugin plugin) {
        this.plugin = plugin;
        this.toggleKey = new NamespacedKey(plugin, "selection_visual_enabled");
    }

    @EventHandler
    public void onWandInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        
        // Check if player has visualization toggled on
        if (!isToggledOn(player)) return;

        // Check if item is a wand (usually Wooden Axe, but best to check loosely or check WE config)
        // For simplicity in this demo, we check if they have permission to make selections
        if (!player.hasPermission("worldedit.selection.pos")) return;

        // Trigger update
        // We delay slightly to allow WE to process the event first
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getVisualizerManager().updateSelectionVisualization(player);
        }, 2L);
    }

    public boolean isToggledOn(Player player) {
        // Default to true if not set
        return !player.getPersistentDataContainer().has(toggleKey, PersistentDataType.BYTE) ||
               player.getPersistentDataContainer().get(toggleKey, PersistentDataType.BYTE) == 1;
    }

    public void setToggle(Player player, boolean state) {
        player.getPersistentDataContainer().set(toggleKey, PersistentDataType.BYTE, (byte) (state ? 1 : 0));
    }
}

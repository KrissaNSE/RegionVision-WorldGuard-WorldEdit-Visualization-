package com.regionvision.managers;

import com.regionvision.RegionVisionPlugin;
import com.regionvision.listeners.WorldEditListener;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final RegionVisionPlugin plugin;
    private final List<String> colorNames = Arrays.asList(
            "WHITE", "SILVER", "GRAY", "BLACK", "RED", "MAROON", 
            "YELLOW", "OLIVE", "LIME", "GREEN", "AQUA", "TEAL", 
            "BLUE", "NAVY", "FUCHSIA", "PURPLE", "ORANGE"
    );

    public CommandManager(RegionVisionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "show" -> handleShow(player);
            case "clear" -> handleClear(player);
            case "toggle" -> handleToggle(player, args);
            case "info" -> handleInfo(player, args);
            case "view" -> handleView(player, args);
            case "near" -> handleNear(player, args);
            case "perm" -> handlePerm(player, args);
            case "reload" -> handleReload(player);
            default -> sendHelp(player);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String[] subCommands = {"show", "clear", "toggle", "info", "view", "near", "perm", "reload"};
            StringUtil.copyPartialMatches(args[0], Arrays.asList(subCommands), completions);
        } 
        else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "toggle" -> StringUtil.copyPartialMatches(args[1], List.of("selection"), completions);
                case "info", "view" -> {
                    Set<String> regions = plugin.getWorldGuardUtil().getRegionNames(player.getWorld());
                    StringUtil.copyPartialMatches(args[1], regions, completions);
                }
                case "perm" -> {
                    String[] permActions = {"add", "remove", "color", "density", "distance", "message", "particles"};
                    StringUtil.copyPartialMatches(args[1], Arrays.asList(permActions), completions);
                }
            }
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("perm")) {
                Set<String> regions = plugin.getWorldGuardUtil().getRegionNames(player.getWorld());
                StringUtil.copyPartialMatches(args[2], regions, completions);
            }
        }
        else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("perm")) {
                if (args[1].equalsIgnoreCase("message")) {
                    StringUtil.copyPartialMatches(args[3], List.of("ACTION_BAR", "BOSS_BAR", "TITLE", "NONE"), completions);
                } else if (args[1].equalsIgnoreCase("color")) {
                    StringUtil.copyPartialMatches(args[3].toUpperCase(), colorNames, completions);
                } else if (args[1].equalsIgnoreCase("particles")) {
                    StringUtil.copyPartialMatches(args[3], List.of("true", "false"), completions);
                }
            }
        }

        Collections.sort(completions);
        return completions;
    }

    private void handlePerm(Player player, String[] args) {
        if (!player.hasPermission("regionvision.admin")) {
            player.sendMessage(parse(plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(parse("<red>Usage: /rv perm <add|remove|color|density|distance|message|particles> <region> ..."));
            return;
        }

        String action = args[1].toLowerCase();
        String regionName = args[2];
        PermanentRegionManager pm = plugin.getPermanentRegionManager();

        switch (action) {
            case "add" -> {
                ProtectedRegion region = plugin.getWorldGuardUtil().getRegionByName(player.getWorld(), regionName);
                if (region == null) {
                    player.sendMessage(parse("<red>Region not found in your current world."));
                    return;
                }
                pm.addRegion(region, player.getWorld());
                player.sendMessage(parse("<green>Region <white>" + regionName + "<green> is now permanently visible!"));
            }
            case "remove" -> {
                if (!pm.isPermanent(regionName)) {
                    player.sendMessage(parse("<red>That region is not marked as permanent."));
                    return;
                }
                pm.removeRegion(regionName);
                player.sendMessage(parse("<green>Region removed from permanent view."));
            }
            case "color" -> {
                if (args.length == 4) {
                    Color c = getColorByName(args[3]);
                    if (c != null) {
                        pm.updateColor(regionName, c.getRed(), c.getGreen(), c.getBlue());
                        player.sendMessage(parse("<green>Color updated to " + args[3].toUpperCase() + "!"));
                        return;
                    } else {
                        player.sendMessage(parse("<red>Invalid color name. Use TAB to see options or use RGB."));
                        return;
                    }
                }
                if (args.length < 6) {
                    player.sendMessage(parse("<red>Usage: /rv perm color <region> <NAME> OR <r> <g> <b>"));
                    return;
                }
                try {
                    int r = Integer.parseInt(args[3]);
                    int g = Integer.parseInt(args[4]);
                    int b = Integer.parseInt(args[5]);
                    pm.updateColor(regionName, r, g, b);
                    player.sendMessage(parse("<green>Color updated!"));
                } catch (NumberFormatException e) {
                    player.sendMessage(parse("<red>RGB values must be integers (0-255)."));
                }
            }
            case "density" -> {
                if (args.length < 4) {
                    player.sendMessage(parse("<red>Usage: /rv perm density <region> <0.1-1.0>"));
                    return;
                }
                try {
                    double d = Double.parseDouble(args[3]);
                    pm.updateDensity(regionName, d);
                    player.sendMessage(parse("<green>Density updated!"));
                } catch (NumberFormatException e) {
                    player.sendMessage(parse("<red>Density must be a number (0.1 - 1.0)."));
                }
            }
            case "distance" -> {
                if (args.length < 4) {
                    player.sendMessage(parse("<red>Usage: /rv perm distance <region> <blocks>"));
                    return;
                }
                try {
                    int dist = Integer.parseInt(args[3]);
                    pm.updateViewDistance(regionName, dist);
                    player.sendMessage(parse("<green>View distance updated!"));
                } catch (NumberFormatException e) {
                    player.sendMessage(parse("<red>Distance must be a number."));
                }
            }
            case "message" -> {
                if (args.length < 5) {
                    player.sendMessage(parse("<red>Usage: /rv perm message <region> <ACTION_BAR|BOSS_BAR|TITLE|NONE> <text...>"));
                    return;
                }
                String type = args[3].toUpperCase();
                String message = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                
                if (!List.of("ACTION_BAR", "BOSS_BAR", "TITLE", "NONE").contains(type)) {
                    player.sendMessage(parse("<red>Invalid type. Use ACTION_BAR, BOSS_BAR, TITLE, or NONE."));
                    return;
                }
                
                pm.updateNotification(regionName, type, message);
                player.sendMessage(parse("<green>Region entry notification updated!"));
            }
            case "particles" -> {
                if (args.length < 4) {
                    player.sendMessage(parse("<red>Usage: /rv perm particles <region> <true|false>"));
                    return;
                }
                boolean state = Boolean.parseBoolean(args[3]);
                pm.updateParticles(regionName, state);
                String status = state ? "<green>enabled" : "<red>disabled";
                player.sendMessage(parse("<green>Particles for region <white>" + regionName + "<green> are now " + status + "."));
            }
            default -> player.sendMessage(parse("<red>Unknown perm command."));
        }
    }

    private void handleShow(Player player) {
        if (!player.hasPermission("regionvision.use")) {
            player.sendMessage(parse(plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        if (!plugin.hasWorldGuard()) {
            player.sendMessage(Component.text("WorldGuard is not installed.", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        ProtectedRegion region = plugin.getWorldGuardUtil().getRegionAt(player.getLocation());
        if (region == null) {
            player.sendMessage(parse(plugin.getConfig().getString("messages.no-region-found")));
            return;
        }

        player.sendMessage(parse(plugin.getConfig().getString("messages.region-shown")
                .replace("%region%", region.getId())));
        plugin.getVisualizerManager().showRegion(player, region);
    }

    private void handleClear(Player player) {
        if (!player.hasPermission("regionvision.use")) return;
        plugin.getVisualizerManager().clearAllParticlesForPlayer(player);
        player.sendMessage(parse(plugin.getConfig().getString("messages.prefix") + "Visualizations cleared."));
    }

    private void handleToggle(Player player, String[] args) {
        if (!player.hasPermission("regionvision.toggle")) {
            player.sendMessage(parse(plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("selection")) {
            WorldEditListener listener = new WorldEditListener(plugin); 
            boolean current = listener.isToggledOn(player);
            listener.setToggle(player, !current);
            
            String msg = !current ? "messages.selection-enabled" : "messages.selection-disabled";
            player.sendMessage(parse(plugin.getConfig().getString(msg)));
            
            if (current) {
                plugin.getVisualizerManager().clearAllParticlesForPlayer(player);
            }
        } else {
            player.sendMessage(Component.text("Usage: /rv toggle selection", net.kyori.adventure.text.format.NamedTextColor.RED));
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (!player.hasPermission("regionvision.info")) return;

        ProtectedRegion region;
        if (args.length > 1) {
            region = plugin.getWorldGuardUtil().getRegionByName(player.getWorld(), args[1]);
        } else {
            region = plugin.getWorldGuardUtil().getRegionAt(player.getLocation());
        }

        if (region == null) {
            player.sendMessage(parse(plugin.getConfig().getString("messages.no-region-found")));
            return;
        }

        player.sendMessage(Component.text("-------------------------", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));
        player.sendMessage(parse("<green>Region: <white>" + region.getId()));
        player.sendMessage(parse("<green>Owners: <gray>" + region.getOwners().toPlayersString()));
        player.sendMessage(parse("<green>Members: <gray>" + region.getMembers().toPlayersString()));
        player.sendMessage(parse("<green>Priority: <gray>" + region.getPriority()));
        player.sendMessage(Component.text("-------------------------", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));
    }

    private void handleView(Player player, String[] args) {
        if (!player.hasPermission("regionvision.view.remote")) return;
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /rv view <region_name>", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        ProtectedRegion region = plugin.getWorldGuardUtil().getRegionByName(player.getWorld(), args[1]);
        if (region == null) {
            player.sendMessage(parse(plugin.getConfig().getString("messages.no-region-found")));
            return;
        }

        plugin.getVisualizerManager().showRegion(player, region);
        player.sendMessage(parse(plugin.getConfig().getString("messages.region-shown").replace("%region%", region.getId())));
    }

    private void handleNear(Player player, String[] args) {
        if (!player.hasPermission("regionvision.near")) return;

        int radius = 30;
        if (args.length > 1) {
            try {
                radius = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid radius.", net.kyori.adventure.text.format.NamedTextColor.RED));
                return;
            }
        }

        int max = plugin.getConfig().getInt("visualizer.max-near-radius", 100);
        if (radius > max) radius = max;

        int finalRadius = radius;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<ProtectedRegion> regions = plugin.getWorldGuardUtil().getRegionsNear(player.getLocation(), finalRadius);
            
            if (regions.isEmpty()) {
                player.sendMessage(parse(plugin.getConfig().getString("messages.no-region-found")));
                return;
            }

            player.sendMessage(parse("<green>Found " + regions.size() + " regions. Visualizing..."));
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (ProtectedRegion r : regions) {
                    plugin.getVisualizerManager().showRegion(player, r);
                }
            });
        });
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("regionvision.admin")) return;
        plugin.reloadConfig();
        player.sendMessage(parse(plugin.getConfig().getString("messages.reloaded")));
    }

    private void sendHelp(Player player) {
        player.sendMessage(parse("<gold>--- RegionVision Help ---"));
        player.sendMessage(parse("<yellow>/rv show <gray>- Show region you are standing in"));
        player.sendMessage(parse("<yellow>/rv clear <gray>- Clear particles"));
        player.sendMessage(parse("<yellow>/rv perm add <name> <gray>- Make region permanently visible"));
        player.sendMessage(parse("<yellow>/rv perm color <name> <NAME|RGB> <gray>- Set custom color"));
        player.sendMessage(parse("<yellow>/rv perm density <name> <0.1-1.0> <gray>- Set particle density"));
        player.sendMessage(parse("<yellow>/rv perm message <name> <TYPE> <text> <gray>- Set entry notification"));
        player.sendMessage(parse("<yellow>/rv perm particles <name> <true|false> <gray>- Toggle particles"));
        player.sendMessage(parse("<yellow>/rv info [name] <gray>- Region info"));
        player.sendMessage(parse("<yellow>/rv toggle selection <gray>- Toggle WE visualizer"));
    }

    private Component parse(String msg) {
        if (msg == null) return Component.empty();
        return MiniMessage.miniMessage().deserialize(msg);
    }

    private Color getColorByName(String name) {
        switch (name.toUpperCase()) {
            case "RED": return Color.RED;
            case "BLUE": return Color.BLUE;
            case "GREEN": return Color.GREEN;
            case "YELLOW": return Color.YELLOW;
            case "AQUA": return Color.AQUA;
            case "WHITE": return Color.WHITE;
            case "BLACK": return Color.BLACK;
            case "GRAY": return Color.GRAY;
            case "PURPLE": return Color.PURPLE;
            case "ORANGE": return Color.ORANGE;
            case "LIME": return Color.LIME;
            case "MAROON": return Color.MAROON;
            case "NAVY": return Color.NAVY;
            case "OLIVE": return Color.OLIVE;
            case "TEAL": return Color.TEAL;
            case "FUCHSIA": return Color.FUCHSIA;
            case "SILVER": return Color.SILVER;
            default: return null;
        }
    }
}
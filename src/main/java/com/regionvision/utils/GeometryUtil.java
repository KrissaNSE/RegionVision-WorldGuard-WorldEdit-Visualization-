package com.regionvision.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles geometric calculations for visualizing cuboids.
 * Uses Java 21 features.
 */
public class GeometryUtil {

    public record CuboidBounds(Vector min, Vector max) {}

    /**
     * Generates a list of locations representing the wireframe of a cuboid.
     * This method is computationally intensive and should be called asynchronously.
     */
    public static List<Location> getCuboidWireframe(World world, Vector min, Vector max, double step) {
        List<Location> particles = new ArrayList<>();
        
        double minX = min.getX();
        double minY = min.getY();
        double minZ = min.getZ();
        double maxX = max.getX() + 1.0; // Expand to cover full block
        double maxY = max.getY() + 1.0;
        double maxZ = max.getZ() + 1.0;

        // Bottom Rectangle
        particles.addAll(getLine(world, minX, minY, minZ, maxX, minY, minZ, step));
        particles.addAll(getLine(world, minX, minY, minZ, minX, minY, maxZ, step));
        particles.addAll(getLine(world, maxX, minY, minZ, maxX, minY, maxZ, step));
        particles.addAll(getLine(world, minX, minY, maxZ, maxX, minY, maxZ, step));

        // Top Rectangle
        particles.addAll(getLine(world, minX, maxY, minZ, maxX, maxY, minZ, step));
        particles.addAll(getLine(world, minX, maxY, minZ, minX, maxY, maxZ, step));
        particles.addAll(getLine(world, maxX, maxY, minZ, maxX, maxY, maxZ, step));
        particles.addAll(getLine(world, minX, maxY, maxZ, maxX, maxY, maxZ, step));

        // Vertical Pillars
        particles.addAll(getLine(world, minX, minY, minZ, minX, maxY, minZ, step));
        particles.addAll(getLine(world, maxX, minY, minZ, maxX, maxY, minZ, step));
        particles.addAll(getLine(world, minX, minY, maxZ, minX, maxY, maxZ, step));
        particles.addAll(getLine(world, maxX, minY, maxZ, maxX, maxY, maxZ, step));

        return particles;
    }

    private static List<Location> getLine(World world, double x1, double y1, double z1, double x2, double y2, double z2, double step) {
        List<Location> points = new ArrayList<>();
        Vector start = new Vector(x1, y1, z1);
        Vector end = new Vector(x2, y2, z2);
        Vector vector = end.clone().subtract(start);
        double length = vector.length();
        
        if (length == 0) return points;

        // Calculate the step vector once
        Vector stepVector = vector.clone().normalize().multiply(step);
        
        // Iterate by adding the step vector to the current point
        for (double covered = 0; covered < length; covered += step) {
            points.add(start.clone().toLocation(world));
            start.add(stepVector);
        }
        
        return points;
    }
}
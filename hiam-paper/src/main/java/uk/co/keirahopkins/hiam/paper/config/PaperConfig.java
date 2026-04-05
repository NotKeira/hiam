package uk.co.keirahopkins.hiam.paper.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.paper.PaperPlugin;

public class PaperConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(PaperConfig.class);
    
    private final PaperPlugin plugin;
    private FileConfiguration config;
    
    private Location loginSpawn;
    private Location mainSpawn;
    
    public PaperConfig(PaperPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void load() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        loadSpawnLocations();
        
        logger.info("Paper configuration loaded successfully");
    }
    
    private void loadSpawnLocations() {
        // Load login spawn
        if (config.contains("spawns.login") || config.contains("auth.spawn.login")) {
            String path = config.contains("spawns.login") ? "spawns.login" : "auth.spawn.login";
            loginSpawn = loadLocation(path);
            if (loginSpawn == null) {
                logger.warn("Failed to load login spawn location");
            }
        } else {
            logger.warn("Login spawn not configured");
        }
        
        // Load main spawn
        if (config.contains("spawns.main") || config.contains("auth.spawn.post")) {
            String path = config.contains("spawns.main") ? "spawns.main" : "auth.spawn.post";
            mainSpawn = loadLocation(path);
            if (mainSpawn == null) {
                logger.warn("Failed to load main spawn location");
            }
        } else {
            logger.warn("Main spawn not configured");
        }
    }
    
    private Location loadLocation(String path) {
        try {
            String worldName = config.getString(path + ".world");
            double x = config.getDouble(path + ".x");
            double y = config.getDouble(path + ".y");
            double z = config.getDouble(path + ".z");
            float yaw = (float) config.getDouble(path + ".yaw", 0.0);
            float pitch = (float) config.getDouble(path + ".pitch", 0.0);
            
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                logger.error("World '{}' not found for location {}", worldName, path);
                return null;
            }
            
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            logger.error("Error loading location from path: {}", path, e);
            return null;
        }
    }
    
    public void saveLocation(String path, Location location) {
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
        plugin.saveConfig();
        
        // Reload the location
        if (path.equals("spawns.login")) {
            loginSpawn = location;
        } else if (path.equals("spawns.main")) {
            mainSpawn = location;
        }
    }
    
    public Location getLoginSpawn() {
        return loginSpawn;
    }
    
    public Location getMainSpawn() {
        return mainSpawn;
    }
    
    public void reload() {
        load();
    }
}

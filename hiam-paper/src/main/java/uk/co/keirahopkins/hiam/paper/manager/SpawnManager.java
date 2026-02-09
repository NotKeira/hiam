package uk.co.keirahopkins.hiam.paper.manager;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.paper.config.PaperConfig;

public class SpawnManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SpawnManager.class);
    
    private final PaperConfig config;
    
    public SpawnManager(PaperConfig config) {
        this.config = config;
    }
    
    public void teleportToLoginSpawn(Player player) {
        Location loginSpawn = config.getLoginSpawn();
        if (loginSpawn != null) {
            player.teleport(loginSpawn);
            logger.debug("Teleported {} to login spawn", player.getName());
        } else {
            logger.warn("Cannot teleport {} to login spawn - not configured", player.getName());
        }
    }
    
    public void teleportToMainSpawn(Player player) {
        Location mainSpawn = config.getMainSpawn();
        if (mainSpawn != null) {
            player.teleport(mainSpawn);
            logger.debug("Teleported {} to main spawn", player.getName());
        } else {
            logger.warn("Cannot teleport {} to main spawn - not configured", player.getName());
        }
    }
    
    public boolean hasLoginSpawn() {
        return config.getLoginSpawn() != null;
    }
    
    public boolean hasMainSpawn() {
        return config.getMainSpawn() != null;
    }
}

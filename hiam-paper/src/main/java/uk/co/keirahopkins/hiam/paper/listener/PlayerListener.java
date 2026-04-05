package uk.co.keirahopkins.hiam.paper.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import io.papermc.paper.event.player.AsyncChatEvent;
import uk.co.keirahopkins.hiam.paper.PaperPlugin;

public class PlayerListener implements Listener {
    
    private final PaperPlugin plugin;
    
    public PlayerListener(PaperPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getFreezeManager().isIgnored(player)) {
            // Freeze player on join
            plugin.getFreezeManager().freeze(player);
        }
        
        // Teleport to login spawn if configured
        if (plugin.getSpawnManager().hasLoginSpawn()) {
            plugin.getSpawnManager().teleportToLoginSpawn(player);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isBlocked(event.getPlayer())) {
            // Allow head movement but not position change
            if (event.getFrom().getX() != event.getTo().getX() ||
                event.getFrom().getY() != event.getTo().getY() ||
                event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (!isBlocked(player)) {
            return;
        }
        
        String command = event.getMessage().toLowerCase().split(" ")[0];
        
        // Allow only auth-related commands
        if (!command.equals("/register") && 
            !command.equals("/login") && 
            !command.equals("/reg") && 
            !command.equals("/l") &&
            !command.equals("/premium") &&
            !command.equals("/offline") &&
            !command.equals("/hiam")) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (isBlocked(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (isBlocked(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isBlocked(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isBlocked(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (isBlocked(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (isBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isBlocked(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        if (isBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private boolean isBlocked(Player player) {
        return plugin.getFreezeManager().isFrozen(player)
            && !plugin.getFreezeManager().isIgnored(player);
    }
}

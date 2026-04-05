package uk.co.keirahopkins.hiam.gate.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.gate.GatePlugin;
import uk.co.keirahopkins.hiam.gate.manager.MessagingService;

public class GateListener implements Listener {
    
    private static final Logger logger = LoggerFactory.getLogger(GateListener.class);
    private static final String BYPASS_PERMISSION = "hiam.gate.bypass";
    
    private final GatePlugin plugin;
    private final MessagingService messagingService;
    
    public GateListener(GatePlugin plugin, MessagingService messagingService) {
        this.plugin = plugin;
        this.messagingService = messagingService;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (player.hasPermission(BYPASS_PERMISSION)) {
            logger.debug("Player {} bypassed gate check (has bypass permission)", player.getName());
            return;
        }
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            messagingService.checkAuthentication(player, authenticated -> {
                if (!authenticated) {
                    Component kickMessage = Component.text()
                        .append(Component.text("Authentication Required", NamedTextColor.RED))
                        .append(Component.newline())
                        .append(Component.newline())
                        .append(Component.text("Please authenticate with Helix IAM first.", NamedTextColor.GRAY))
                        .append(Component.newline())
                        .append(Component.text("Connect to the proxy and use /login or /register", NamedTextColor.YELLOW))
                        .build();

                    player.kick(kickMessage);
                    logger.info("Kicked unauthenticated player: {}", player.getName());
                } else {
                    logger.debug("Player {} authenticated successfully", player.getName());
                }
            });
        }, 10L);
    }
}

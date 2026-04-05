package uk.co.keirahopkins.hiam.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.velocity.config.Config;
import java.util.Optional;

public class ProxyConnectListener {
    private static final Logger logger = LoggerFactory.getLogger(ProxyConnectListener.class);
    
    private final Config config;
    private final PostLoginEventListener postLoginEventListener;
    private final ProxyServer server;

    public ProxyConnectListener(Config config, PostLoginEventListener postLoginEventListener, ProxyServer server) {
        this.config = config;
        this.postLoginEventListener = postLoginEventListener;
        this.server = server;
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        String targetServerName = event.getOriginalServer().getServerInfo().getName();
        String authServerName = config.getRoutingAuthServer();

        boolean isAuthenticated = postLoginEventListener.isAuthenticated(player.getUniqueId());
        boolean onAuthServer = player.getCurrentServer()
            .map(ServerConnection::getServer)
            .map(connection -> connection.getServerInfo().getName().equals(authServerName))
            .orElse(false);

        if (isAuthenticated && targetServerName.equals(authServerName)) {
            String postLoginTarget = config.getRoutingPostLoginTarget();
            String fallbackServer = config.getRoutingFallbackServer();

            if (postLoginTarget != null && !postLoginTarget.isBlank() && !postLoginTarget.equals(authServerName)) {
                Optional<RegisteredServer> target = server.getServer(postLoginTarget);
                if (target.isPresent()) {
                    event.setResult(ServerPreConnectEvent.ServerResult.allowed(target.get()));
                    logger.debug("Redirecting authenticated player {} to post-login target {}", player.getUsername(), postLoginTarget);
                    return;
                }

                if (fallbackServer != null && !fallbackServer.isBlank()) {
                    Optional<RegisteredServer> fallback = server.getServer(fallbackServer);
                    if (fallback.isPresent()) {
                        event.setResult(ServerPreConnectEvent.ServerResult.allowed(fallback.get()));
                        logger.warn("Post-login target '{}' missing; redirecting {} to fallback '{}'", postLoginTarget, player.getUsername(), fallbackServer);
                        return;
                    }
                }

                logger.error("Post-login target '{}' not found and fallback unavailable", postLoginTarget);
            }
        }

        if (!isAuthenticated && !targetServerName.equals(authServerName) && !onAuthServer) {
            player.sendMessage(
                Component.text("You must authenticate before accessing other servers.")
                    .color(NamedTextColor.RED)
            );

            Optional<RegisteredServer> authServerOpt = server.getServer(authServerName);
            if (authServerOpt.isPresent()) {
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(authServerOpt.get()));
                logger.debug("Redirecting unauthenticated player {} to auth server", player.getUsername());
            } else {
                logger.error("Auth server '{}' not found in Velocity config", authServerName);
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
            }
        }
    }
}

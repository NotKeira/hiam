package uk.co.keirahopkins.hiam.gate.manager;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.gate.HelixIAMGate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MessagingService implements PluginMessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(MessagingService.class);
    private static final String CHANNEL = "hiam:auth";
    private static final String CHECK_AUTH_SUBCHANNEL = "CheckAuth";
    private static final String AUTH_RESPONSE_SUBCHANNEL = "AuthResponse";
    private static final String PREMIUM_SUCCESS_SUBCHANNEL = "PremiumSuccess";
    private static final String PREMIUM_FAIL_SUBCHANNEL = "PremiumFail";
    private static final String OFFLINE_SUCCESS_SUBCHANNEL = "OfflineSuccess";
    private static final String OFFLINE_FAIL_SUBCHANNEL = "OfflineFail";
    
    private final HelixIAMGate plugin;
    private final Map<UUID, Consumer<Boolean>> pendingChecks;
    
    public MessagingService(HelixIAMGate plugin) {
        this.plugin = plugin;
        this.pendingChecks = new ConcurrentHashMap<>();
    }
    
    public void initialize() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        logger.info("Plugin messaging channel registered: {}", CHANNEL);
    }
    
    public void shutdown() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL);
        pendingChecks.clear();
    }
    
    public void checkAuthentication(Player player, Consumer<Boolean> callback) {
        UUID playerId = player.getUniqueId();
        pendingChecks.put(playerId, callback);
        
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(CHECK_AUTH_SUBCHANNEL);
        out.writeUTF(playerId.toString());
        
        player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
        logger.debug("Sent auth check request for player: {}", player.getName());
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Consumer<Boolean> timeoutCallback = pendingChecks.remove(playerId);
            if (timeoutCallback != null) {
                logger.warn("Auth check timed out for player: {}, defaulting to unauthenticated", player.getName());
                timeoutCallback.accept(false);
            }
        }, 100L);
    }

    public void sendPremiumRequest(Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("EnablePremium");
        out.writeUTF(player.getName());
        player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
        logger.debug("Sent premium request for player: {}", player.getName());
    }

    public void sendOfflineRequest(Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("DisablePremium");
        out.writeUTF(player.getName());
        player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
        logger.debug("Sent offline request for player: {}", player.getName());
    }
    
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) {
            return;
        }

        if (message == null) {
            return;
        }
        
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        
        if (AUTH_RESPONSE_SUBCHANNEL.equals(subchannel)) {
            String playerIdStr = in.readUTF();
            boolean authenticated = in.readBoolean();
            
            UUID playerId = UUID.fromString(playerIdStr);
            Consumer<Boolean> callback = pendingChecks.remove(playerId);
            
            if (callback != null) {
                callback.accept(authenticated);
                logger.debug("Received auth response for {}: {}", player.getName(), authenticated);
            }
            return;
        }

        if (PREMIUM_SUCCESS_SUBCHANNEL.equals(subchannel)) {
            player.sendMessage(net.kyori.adventure.text.Component.text("Premium mode enabled", net.kyori.adventure.text.format.NamedTextColor.GREEN));
            return;
        }

        if (PREMIUM_FAIL_SUBCHANNEL.equals(subchannel)) {
            String reason = in.readUTF();
            String text = reason == null || reason.isBlank() ? "Premium mode failed" : reason;
            player.sendMessage(net.kyori.adventure.text.Component.text(text, net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        if (OFFLINE_SUCCESS_SUBCHANNEL.equals(subchannel)) {
            String reason = in.readUTF();
            String text = reason == null || reason.isBlank() ? "Password authentication enabled" : reason;
            player.sendMessage(net.kyori.adventure.text.Component.text(text, net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            return;
        }

        if (OFFLINE_FAIL_SUBCHANNEL.equals(subchannel)) {
            String reason = in.readUTF();
            String text = reason == null || reason.isBlank() ? "Offline mode failed" : reason;
            player.sendMessage(net.kyori.adventure.text.Component.text(text, net.kyori.adventure.text.format.NamedTextColor.RED));
        }
    }
}

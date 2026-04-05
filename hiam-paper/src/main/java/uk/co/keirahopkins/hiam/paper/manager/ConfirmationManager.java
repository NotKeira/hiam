package uk.co.keirahopkins.hiam.paper.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import uk.co.keirahopkins.hiam.paper.PaperPlugin;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfirmationManager {
    
    private static final int TOKEN_LENGTH = 6;
    private static final long TOKEN_EXPIRY_MS = 30_000; // 30 seconds
    private static final String TOKEN_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    private final SecureRandom random = new SecureRandom();
    private final Map<String, ConfirmationData> confirmations = new HashMap<>();
    
    public String createConfirmation(Player player, ConfirmationType type, Runnable action) {
        String token = generateToken();
        
        ConfirmationData data = new ConfirmationData(
            player.getUniqueId(),
            type,
            action,
            System.currentTimeMillis() + TOKEN_EXPIRY_MS
        );
        
        confirmations.put(token, data);
        
        // Schedule auto-expiry
        Bukkit.getScheduler().runTaskLater(PaperPlugin.getInstance(), () -> {
            confirmations.remove(token);
        }, TOKEN_EXPIRY_MS / 50); // Convert ms to ticks
        
        return token;
    }
    
    public boolean confirm(Player player, String token) {
        ConfirmationData data = confirmations.get(token);
        
        if (data == null) {
            return false;
        }
        
        if (!data.playerUuid.equals(player.getUniqueId())) {
            return false;
        }
        
        if (System.currentTimeMillis() > data.expiryTime) {
            confirmations.remove(token);
            return false;
        }
        
        confirmations.remove(token);
        data.action.run();
        return true;
    }
    
    public boolean cancel(Player player, String token) {
        ConfirmationData data = confirmations.get(token);
        
        if (data == null) {
            return false;
        }
        
        if (!data.playerUuid.equals(player.getUniqueId())) {
            return false;
        }
        
        confirmations.remove(token);
        return true;
    }
    
    public ConfirmationType getType(String token) {
        ConfirmationData data = confirmations.get(token);
        return data != null ? data.type : null;
    }
    
    private String generateToken() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(TOKEN_CHARS.charAt(random.nextInt(TOKEN_CHARS.length())));
        }
        return token.toString();
    }
    
    public void clearAll() {
        confirmations.clear();
    }
    
    private static class ConfirmationData {
        final UUID playerUuid;
        final ConfirmationType type;
        final Runnable action;
        final long expiryTime;
        
        ConfirmationData(UUID playerUuid, ConfirmationType type, Runnable action, long expiryTime) {
            this.playerUuid = playerUuid;
            this.type = type;
            this.action = action;
            this.expiryTime = expiryTime;
        }
    }
    
    public enum ConfirmationType {
        PLAYER_PREMIUM,
        PLAYER_OFFLINE,
        ADMIN_FORCE_RESET,
        ADMIN_RESET_PASSWORD,
        ADMIN_CLEAR_PASSWORD,
        ADMIN_SET_PREMIUM,
        ADMIN_SET_OFFLINE
    }
}

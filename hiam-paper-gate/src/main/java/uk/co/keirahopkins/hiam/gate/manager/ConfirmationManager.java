package uk.co.keirahopkins.hiam.gate.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import uk.co.keirahopkins.hiam.gate.GatePlugin;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfirmationManager {

    private static final int TOKEN_LENGTH = 6;
    private static final long TOKEN_EXPIRY_MS = 30_000L;
    private static final String TOKEN_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final SecureRandom random = new SecureRandom();
    private final Map<String, ConfirmationData> confirmations = new HashMap<>();

    public String createConfirmation(Player player, ConfirmationType type, Runnable action) {
        String token = generateToken();

        confirmations.put(token, new ConfirmationData(
            player.getUniqueId(),
            type,
            action,
            System.currentTimeMillis() + TOKEN_EXPIRY_MS
        ));

        Bukkit.getScheduler().runTaskLater(GatePlugin.getInstance(), () -> {
            confirmations.remove(token);
        }, TOKEN_EXPIRY_MS / 50L);

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

    public void clearAll() {
        confirmations.clear();
    }

    private String generateToken() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(TOKEN_CHARS.charAt(random.nextInt(TOKEN_CHARS.length())));
        }
        return token.toString();
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
        PLAYER_OFFLINE
    }
}

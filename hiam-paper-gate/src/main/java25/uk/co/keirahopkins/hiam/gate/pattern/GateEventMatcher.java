package uk.co.keirahopkins.hiam.gate.pattern;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public class GateEventMatcher {

    public static String extractPlayerName(PlayerJoinEvent event) {
        return switch (event) {
            case PlayerJoinEvent e when e.getPlayer() != null -> e.getPlayer().getName();
            default -> "Unknown";
        };
    }

    public static boolean shouldBypassGate(Player player) {
        return switch (player) {
            case Player p when p.hasPermission("hiam.gate.bypass") -> true;
            default -> false;
        };
    }
}

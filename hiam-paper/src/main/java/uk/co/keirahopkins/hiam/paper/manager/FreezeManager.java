package uk.co.keirahopkins.hiam.paper.manager;

import org.bukkit.entity.Player;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FreezeManager {
    
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Set<UUID> ignoredPlayers = new HashSet<>();
    
    public void freeze(Player player) {
        if (isIgnored(player)) {
            return;
        }
        frozenPlayers.add(player.getUniqueId());
    }
    
    public void unfreeze(Player player) {
        frozenPlayers.remove(player.getUniqueId());
    }
    
    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }

    public void ignore(Player player) {
        ignoredPlayers.add(player.getUniqueId());
        frozenPlayers.remove(player.getUniqueId());
    }

    public void unignore(Player player) {
        ignoredPlayers.remove(player.getUniqueId());
    }

    public boolean isIgnored(Player player) {
        return ignoredPlayers.contains(player.getUniqueId());
    }
    
    public void clearAll() {
        frozenPlayers.clear();
        ignoredPlayers.clear();
    }
}

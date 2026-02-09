package uk.co.keirahopkins.hiam.paper.manager;

import org.bukkit.entity.Player;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FreezeManager {
    
    private final Set<UUID> frozenPlayers = new HashSet<>();
    
    public void freeze(Player player) {
        frozenPlayers.add(player.getUniqueId());
    }
    
    public void unfreeze(Player player) {
        frozenPlayers.remove(player.getUniqueId());
    }
    
    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }
    
    public void clearAll() {
        frozenPlayers.clear();
    }
}

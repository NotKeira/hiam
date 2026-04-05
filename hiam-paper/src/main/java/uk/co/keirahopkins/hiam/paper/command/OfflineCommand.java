package uk.co.keirahopkins.hiam.paper.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import uk.co.keirahopkins.hiam.paper.PaperPlugin;
import uk.co.keirahopkins.hiam.paper.manager.ConfirmationManager.ConfirmationType;

public class OfflineCommand implements CommandExecutor {
    
    private final PaperPlugin plugin;
    
    public OfflineCommand(PaperPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player is authenticated (not frozen)
        if (plugin.getFreezeManager().isFrozen(player)) {
            player.sendMessage(Component.text("You must be authenticated to use this command!", NamedTextColor.RED));
            return true;
        }
        
        // Create confirmation
        String token = plugin.getConfirmationManager().createConfirmation(player, ConfirmationType.PLAYER_OFFLINE, () -> {
            plugin.getMessagingService().sendOfflineRequest(player);
            player.sendMessage(Component.text("Keeping password authentication...", NamedTextColor.YELLOW));
        });
        
        // Show confirmation prompt
        plugin.getAuthPromptManager().showPlayerConfirmation(player, token, ConfirmationType.PLAYER_OFFLINE);
        
        return true;
    }
}

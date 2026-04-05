package uk.co.keirahopkins.hiam.paper.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import uk.co.keirahopkins.hiam.paper.PaperPlugin;

public class LoginCommand implements CommandExecutor {
    
    private final PaperPlugin plugin;
    
    public LoginCommand(PaperPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player is frozen (not authenticated)
        if (!plugin.getFreezeManager().isFrozen(player)) {
            player.sendMessage(Component.text("You are already authenticated!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /login <password>", NamedTextColor.RED));
            return true;
        }
        
        String password = args[0];
        
        // Send login request to Velocity
        plugin.getMessagingService().sendLoginRequest(player, password);
        player.sendMessage(Component.text("Logging in...", NamedTextColor.YELLOW));
        
        return true;
    }
}

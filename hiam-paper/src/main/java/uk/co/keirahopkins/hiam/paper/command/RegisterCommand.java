package uk.co.keirahopkins.hiam.paper.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import uk.co.keirahopkins.hiam.paper.PaperPlugin;

public class RegisterCommand implements CommandExecutor {
    
    private final PaperPlugin plugin;
    
    public RegisterCommand(PaperPlugin plugin) {
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
            player.sendMessage(Component.text("Usage: /register <password>", NamedTextColor.RED));
            return true;
        }
        
        String password = args[0];
        
        // Basic password validation
        if (password.length() < 6) {
            player.sendMessage(Component.text("Password must be at least 6 characters long", NamedTextColor.RED));
            return true;
        }
        
        if (password.length() > 128) {
            player.sendMessage(Component.text("Password is too long (max 128 characters)", NamedTextColor.RED));
            return true;
        }
        
        // Send registration request to Velocity
        plugin.getMessagingService().sendRegisterRequest(player, password);
        player.sendMessage(Component.text("Registering...", NamedTextColor.YELLOW));
        
        return true;
    }
}

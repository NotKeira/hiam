package uk.co.keirahopkins.hiam.paper.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import uk.co.keirahopkins.hiam.paper.PaperPlugin;

public class ChangePasswordCommand implements CommandExecutor {
    
    private final PaperPlugin plugin;
    
    public ChangePasswordCommand(PaperPlugin plugin) {
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
        
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /changepass <old password> <new password>", NamedTextColor.RED));
            return true;
        }
        
        String oldPassword = args[0];
        String newPassword = args[1];
        
        // Basic password validation
        if (newPassword.length() < 6) {
            player.sendMessage(Component.text("New password must be at least 6 characters long", NamedTextColor.RED));
            return true;
        }
        
        if (newPassword.length() > 128) {
            player.sendMessage(Component.text("New password is too long (max 128 characters)", NamedTextColor.RED));
            return true;
        }
        
        // Send change password request to Velocity
        plugin.getMessagingService().sendChangePasswordRequest(player, oldPassword, newPassword);
        player.sendMessage(Component.text("Changing password...", NamedTextColor.YELLOW));
        
        return true;
    }
}

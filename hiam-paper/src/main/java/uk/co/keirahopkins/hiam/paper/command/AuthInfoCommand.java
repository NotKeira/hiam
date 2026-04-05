package uk.co.keirahopkins.hiam.paper.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import uk.co.keirahopkins.hiam.paper.PaperPlugin;

public class AuthInfoCommand implements CommandExecutor {
    
    private final PaperPlugin plugin;
    
    public AuthInfoCommand(PaperPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("hiam.admin.authinfo")) {
            player.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /authinfo <player>", NamedTextColor.RED));
            return true;
        }
        
        String targetName = args[0];
        
        // Send auth info request to Velocity
        plugin.getMessagingService().sendAuthInfoRequest(player, targetName);
        player.sendMessage(Component.text("Fetching authentication info for " + targetName + "...", NamedTextColor.YELLOW));
        
        return true;
    }
}

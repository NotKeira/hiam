package uk.co.keirahopkins.hiam.paper.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import uk.co.keirahopkins.hiam.paper.HelixIAMPaper;

public class SetLoginSpawnCommand implements CommandExecutor {
    
    private final HelixIAMPaper plugin;
    
    public SetLoginSpawnCommand(HelixIAMPaper plugin) {
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
        if (!player.hasPermission("hiam.admin.setloginspawn")) {
            player.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
            return true;
        }
        
        // Save current location as login spawn
        plugin.getPaperConfig().saveLocation("spawns.login", player.getLocation());
        player.sendMessage(Component.text("Login spawn set to your current location!", NamedTextColor.GREEN));
        
        return true;
    }
}

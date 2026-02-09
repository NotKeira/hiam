package uk.co.keirahopkins.hiam.gate.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import uk.co.keirahopkins.hiam.gate.HelixIAMGate;

public class HiamCommand implements CommandExecutor {

    private final HelixIAMGate plugin;

    public HiamCommand(HelixIAMGate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /hiam <confirm|cancel> <token>", NamedTextColor.RED));
            return true;
        }

        String action = args[0].toLowerCase();
        String token = args[1];

        switch (action) {
            case "confirm":
                if (plugin.getConfirmationManager().confirm(player, token)) {
                    player.sendMessage(Component.text("Action confirmed!", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Invalid or expired token", NamedTextColor.RED));
                }
                return true;
            case "cancel":
                if (plugin.getConfirmationManager().cancel(player, token)) {
                    player.sendMessage(Component.text("Action cancelled", NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("Invalid or expired token", NamedTextColor.RED));
                }
                return true;
            default:
                player.sendMessage(Component.text("Usage: /hiam <confirm|cancel> <token>", NamedTextColor.RED));
                return true;
        }
    }
}

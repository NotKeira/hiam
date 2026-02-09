package uk.co.keirahopkins.hiam.gate.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import uk.co.keirahopkins.hiam.gate.HelixIAMGate;
import uk.co.keirahopkins.hiam.gate.manager.ConfirmationManager.ConfirmationType;

public class OfflineCommand implements CommandExecutor {

    private final HelixIAMGate plugin;

    public OfflineCommand(HelixIAMGate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players", NamedTextColor.RED));
            return true;
        }

        if (!sender.hasPermission("hiam.player.offline")) {
            sender.sendMessage(Component.text("You do not have permission to use this command", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        String token = plugin.getConfirmationManager().createConfirmation(player, ConfirmationType.PLAYER_OFFLINE, () -> {
            plugin.getMessagingService().sendOfflineRequest(player);
            player.sendMessage(Component.text("Disabling premium mode...", NamedTextColor.YELLOW));
        });

        plugin.getAuthPromptManager().showPlayerConfirmation(player, token, ConfirmationType.PLAYER_OFFLINE);
        return true;
    }
}

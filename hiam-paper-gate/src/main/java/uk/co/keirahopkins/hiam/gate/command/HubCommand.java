package uk.co.keirahopkins.hiam.gate.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import uk.co.keirahopkins.hiam.gate.HelixIAMGate;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class HubCommand implements CommandExecutor {

    private static final String CHANNEL = "BungeeCord";
    private static final String TARGET = "lobby";

    private final HelixIAMGate plugin;

    public HubCommand(HelixIAMGate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        sendToServer(player, TARGET);
        return true;
    }

    private void sendToServer(Player player, String server) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(stream)) {
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, CHANNEL, stream.toByteArray());
        } catch (IOException e) {
            player.sendMessage(Component.text("Failed to send you to the lobby", NamedTextColor.RED));
        }
    }
}

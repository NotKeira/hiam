package uk.co.keirahopkins.hiam.gate.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import uk.co.keirahopkins.hiam.gate.GatePlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AuthCommand implements CommandExecutor {

    private final GatePlugin plugin;

    public AuthCommand(GatePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "confirm":
                return handleConfirm(player, args);
            case "cancel":
                return handleCancel(player, args);
            case "changepass":
                return handleChangePassword(player, args);
            case "admin":
                return handleAdmin(player, args);
            default:
                showHelp(player);
                return true;
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text("=== Helix IAM Commands ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/hiam confirm <token>", NamedTextColor.YELLOW)
            .append(Component.text(" - Confirm action", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/hiam cancel <token>", NamedTextColor.YELLOW)
            .append(Component.text(" - Cancel action", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/hiam changepass <old> <new>", NamedTextColor.YELLOW)
            .append(Component.text(" - Change your password", NamedTextColor.GRAY)));

        if (player.hasPermission("hiam.admin.override")) {
            player.sendMessage(Component.text("/hiam admin override [player] <authServer|currentServer|off>", NamedTextColor.YELLOW)
                .append(Component.text(" - Override auth checks", NamedTextColor.GRAY)));
        }
    }

    private boolean handleConfirm(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /hiam confirm <token>", NamedTextColor.RED));
            return true;
        }

        String token = args[1];
        if (plugin.getConfirmationManager().confirm(player, token)) {
            player.sendMessage(Component.text("Action confirmed!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Invalid or expired token", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleCancel(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /hiam cancel <token>", NamedTextColor.RED));
            return true;
        }

        String token = args[1];
        if (plugin.getConfirmationManager().cancel(player, token)) {
            player.sendMessage(Component.text("Action cancelled", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("Invalid or expired token", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleChangePassword(Player player, String[] args) {
        if (!player.hasPermission("hiam.player.changepass")) {
            player.sendMessage(Component.text("You don't have permission to change your password", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /hiam changepass <old password> <new password>", NamedTextColor.RED));
            return true;
        }

        String oldPassword = args[1];
        String newPassword = args[2];

        if (newPassword.length() < 6) {
            player.sendMessage(Component.text("New password must be at least 6 characters long", NamedTextColor.RED));
            return true;
        }

        if (newPassword.length() > 128) {
            player.sendMessage(Component.text("New password is too long (max 128 characters)", NamedTextColor.RED));
            return true;
        }

        plugin.getMessagingService().sendChangePasswordRequest(player, oldPassword, newPassword);
        player.sendMessage(Component.text("Changing password...", NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("hiam.admin.override")) {
            player.sendMessage(Component.text("You don't have permission to use admin override", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3 || !"override".equalsIgnoreCase(args[1])) {
            player.sendMessage(Component.text("Usage: /hiam admin override [player] <authServer|currentServer|off>", NamedTextColor.RED));
            return true;
        }

        String target = args[2].toLowerCase();
        String targetPlayer = player.getName();
        String mode = target;

        // Check if third arg is a player name (4+ args means we have player + mode)
        if (args.length >= 4) {
            targetPlayer = args[2];
            mode = args[3].toLowerCase();
        }

        if (!mode.equals("authserver") && !mode.equals("currentserver") && !mode.equals("off")) {
            player.sendMessage(Component.text("Usage: /hiam admin override [player] <authServer|currentServer|off>", NamedTextColor.RED));
            return true;
        }

        plugin.getMessagingService().sendAdminOverrideRequest(player, targetPlayer, mode);
        if (mode.equals("authserver")) {
            player.sendMessage(Component.text("Sending " + targetPlayer + " to the auth server...", NamedTextColor.YELLOW));
            if (targetPlayer.equalsIgnoreCase(player.getName())) {
                sendToServer(player, "auth");
            }
        } else if (mode.equals("off")) {
            player.sendMessage(Component.text("Admin override disabled for " + targetPlayer, NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("Applying admin override to " + targetPlayer + "...", NamedTextColor.YELLOW));
        }
        return true;
    }

    private void sendToServer(Player player, String server) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(stream)) {
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, "BungeeCord", stream.toByteArray());
        } catch (IOException e) {
            player.sendMessage(Component.text("Failed to send you to the auth server", NamedTextColor.RED));
        }
    }
}

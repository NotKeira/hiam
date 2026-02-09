package uk.co.keirahopkins.hiam.paper.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import uk.co.keirahopkins.hiam.paper.HelixIAMPaper;
import uk.co.keirahopkins.hiam.paper.manager.ConfirmationManager.ConfirmationType;

import java.util.UUID;

public class HiamCommand implements CommandExecutor {
    
    private final HelixIAMPaper plugin;
    
    public HiamCommand(HelixIAMPaper plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                showHelp(player);
            } else {
                showConsoleHelp(sender);
            }
            return true;
        }
        
        String subCommand = args[0].toLowerCase();

        if (!(sender instanceof Player)) {
            return handleConsoleCommand(sender, subCommand, args);
        }

        Player player = (Player) sender;

        switch (subCommand) {
            case "info":
                return handleInfo(player);
            case "admin":
                return handleAdminPlayer(player, args);
            case "confirm":
                return handleConfirm(player, args);
            case "cancel":
                return handleCancel(player, args);
            default:
                showHelp(player);
                return true;
        }
    }
    
    private void showHelp(Player player) {
        player.sendMessage(Component.text("=== Helix IAM Commands ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/hiam info", NamedTextColor.YELLOW)
            .append(Component.text(" - Show plugin information", NamedTextColor.GRAY)));
        
        if (player.hasPermission("hiam.admin")) {
            player.sendMessage(Component.text("/hiam admin <subcommand>", NamedTextColor.YELLOW)
                .append(Component.text(" - Admin commands", NamedTextColor.GRAY)));
        }
    }
    
    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage(Component.text("=== Helix IAM Paper ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Version: ", NamedTextColor.YELLOW)
            .append(Component.text(plugin.getPluginMeta().getVersion(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Auth Server Plugin for Helix IAM", NamedTextColor.GRAY));
        return true;
    }

    private boolean handleAdminPlayer(Player player, String[] args) {
        if (!player.hasPermission("hiam.admin")) {
            player.sendMessage(Component.text("You don't have permission to use admin commands", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            showAdminHelp(player);
            return true;
        }
        
        String adminSubCommand = args[1].toLowerCase();
        
        switch (adminSubCommand) {
            case "forcereset":
                return handleForceReset(player, args);
            case "resetpassword":
                return handleResetPassword(player, args);
            case "clearpassword":
                return handleClearPassword(player, args);
            case "setpremium":
                return handleSetPremium(player, args);
            case "setoffline":
                return handleSetOffline(player, args);
            case "confirm":
                return handleAdminConfirm(player, args);
            case "cancel":
                return handleAdminCancel(player, args);
            default:
                showAdminHelp(player);
                return true;
        }
    }

    private boolean handleConsoleCommand(CommandSender sender, String subCommand, String[] args) {
        switch (subCommand) {
            case "info":
                return handleInfo(sender);
            case "admin":
                return handleAdminConsole(sender, args);
            default:
                showConsoleHelp(sender);
                return true;
        }
    }

    private void showConsoleHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Helix IAM Console Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/hiam info", NamedTextColor.YELLOW)
            .append(Component.text(" - Show plugin information", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hiam admin <subcommand>", NamedTextColor.YELLOW)
            .append(Component.text(" - Admin commands", NamedTextColor.GRAY)));
    }

    private boolean handleAdminConsole(CommandSender sender, String[] args) {
        if (args.length < 2) {
            showConsoleAdminHelp(sender);
            return true;
        }

        String adminSubCommand = args[1].toLowerCase();

        switch (adminSubCommand) {
            case "forcereset":
                return handleForceResetConsole(sender, args);
            case "resetpassword":
                return handleResetPasswordConsole(sender, args);
            case "clearpassword":
                return handleClearPasswordConsole(sender, args);
            case "setpremium":
                return handleSetPremiumConsole(sender, args);
            case "setoffline":
                return handleSetOfflineConsole(sender, args);
            default:
                showConsoleAdminHelp(sender);
                return true;
        }
    }

    private void showConsoleAdminHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Helix IAM Admin Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/hiam admin forcereset <player>", NamedTextColor.YELLOW)
            .append(Component.text(" - Force reset player account", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hiam admin resetpassword <player> <new password>", NamedTextColor.YELLOW)
            .append(Component.text(" - Reset player password", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hiam admin clearpassword <player>", NamedTextColor.YELLOW)
            .append(Component.text(" - Clear player password", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hiam admin setpremium <player> <uuid>", NamedTextColor.YELLOW)
            .append(Component.text(" - Set player to premium mode", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hiam admin setoffline <player>", NamedTextColor.YELLOW)
            .append(Component.text(" - Set player to offline mode", NamedTextColor.GRAY)));
    }
    
    private void showAdminHelp(Player player) {
        player.sendMessage(Component.text("=== Helix IAM Admin Commands ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/hiam admin forcereset <player>", NamedTextColor.YELLOW)
            .append(Component.text(" - Force reset player account", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/hiam admin resetpassword <player>", NamedTextColor.YELLOW)
            .append(Component.text(" - Reset player password", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/hiam admin clearpassword <player>", NamedTextColor.YELLOW)
            .append(Component.text(" - Clear player password", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/hiam admin setpremium <player>", NamedTextColor.YELLOW)
            .append(Component.text(" - Set player to premium mode", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/hiam admin setoffline <player>", NamedTextColor.YELLOW)
            .append(Component.text(" - Set player to offline mode", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/hiam admin confirm <token>", NamedTextColor.YELLOW)
            .append(Component.text(" - Confirm action", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/hiam admin cancel <token>", NamedTextColor.YELLOW)
            .append(Component.text(" - Cancel action", NamedTextColor.GRAY)));
    }
    
    private boolean handleForceReset(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /hiam admin forcereset <player>", NamedTextColor.RED));
            return true;
        }
        
        String targetName = args[2];
        
        // Create confirmation
        String token = plugin.getConfirmationManager().createConfirmation(player, ConfirmationType.ADMIN_FORCE_RESET, () -> {
            plugin.getMessagingService().sendAdminForceReset(player, null, targetName);
            player.sendMessage(Component.text("Force reset request sent for " + targetName, NamedTextColor.GREEN));
        });
        
        // Show confirmation prompt
        plugin.getAuthPromptManager().showConfirmation(player, token, ConfirmationType.ADMIN_FORCE_RESET);
        
        return true;
    }
    
    private boolean handleResetPassword(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(Component.text("Usage: /hiam admin resetpassword <player> <new password>", NamedTextColor.RED));
            return true;
        }
        
        String targetName = args[2];
        String newPassword = args[3];
        
        // Create confirmation
        String token = plugin.getConfirmationManager().createConfirmation(player, ConfirmationType.ADMIN_RESET_PASSWORD, () -> {
            plugin.getMessagingService().sendAdminResetPassword(player, null, targetName, newPassword);
            player.sendMessage(Component.text("Password reset request sent for " + targetName, NamedTextColor.GREEN));
        });
        
        // Show confirmation prompt
        plugin.getAuthPromptManager().showConfirmation(player, token, ConfirmationType.ADMIN_RESET_PASSWORD);
        
        return true;
    }
    
    private boolean handleClearPassword(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /hiam admin clearpassword <player>", NamedTextColor.RED));
            return true;
        }
        
        String targetName = args[2];
        
        // Create confirmation
        String token = plugin.getConfirmationManager().createConfirmation(player, ConfirmationType.ADMIN_CLEAR_PASSWORD, () -> {
            plugin.getMessagingService().sendAdminClearPassword(player, null, targetName);
            player.sendMessage(Component.text("Password clear request sent for " + targetName, NamedTextColor.GREEN));
        });
        
        // Show confirmation prompt
        plugin.getAuthPromptManager().showConfirmation(player, token, ConfirmationType.ADMIN_CLEAR_PASSWORD);
        
        return true;
    }
    
    private boolean handleSetPremium(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /hiam admin setpremium <player>", NamedTextColor.RED));
            return true;
        }
        
        String targetName = args[2];
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUuid = target != null ? target.getUniqueId() : null;
        if (targetUuid == null && args.length >= 4) {
            targetUuid = parseUuid(args[3]);
        }

        if (targetUuid == null) {
            player.sendMessage(Component.text("Usage: /hiam admin setpremium <player> <uuid>", NamedTextColor.RED));
            return true;
        }

        UUID finalTargetUuid = targetUuid;
        
        // Create confirmation
        String token = plugin.getConfirmationManager().createConfirmation(player, ConfirmationType.ADMIN_SET_PREMIUM, () -> {
            plugin.getMessagingService().sendAdminSetPremium(player, finalTargetUuid, targetName);
            player.sendMessage(Component.text("Set premium request sent for " + targetName, NamedTextColor.GREEN));
        });
        
        // Show confirmation prompt
        plugin.getAuthPromptManager().showConfirmation(player, token, ConfirmationType.ADMIN_SET_PREMIUM);
        
        return true;
    }
    
    private boolean handleSetOffline(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /hiam admin setoffline <player>", NamedTextColor.RED));
            return true;
        }
        
        String targetName = args[2];
        
        // Create confirmation
        String token = plugin.getConfirmationManager().createConfirmation(player, ConfirmationType.ADMIN_SET_OFFLINE, () -> {
            plugin.getMessagingService().sendAdminSetOffline(player, null, targetName);
            player.sendMessage(Component.text("Set offline request sent for " + targetName, NamedTextColor.GREEN));
        });
        
        // Show confirmation prompt
        plugin.getAuthPromptManager().showConfirmation(player, token, ConfirmationType.ADMIN_SET_OFFLINE);
        
        return true;
    }
    
    private boolean handleAdminConfirm(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /hiam admin confirm <token>", NamedTextColor.RED));
            return true;
        }
        
        String token = args[2];
        
        if (plugin.getConfirmationManager().confirm(player, token)) {
            player.sendMessage(Component.text("Action confirmed!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Invalid or expired token", NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleAdminCancel(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /hiam admin cancel <token>", NamedTextColor.RED));
            return true;
        }
        
        String token = args[2];
        
        if (plugin.getConfirmationManager().cancel(player, token)) {
            player.sendMessage(Component.text("Action cancelled", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("Invalid or expired token", NamedTextColor.RED));
        }
        
        return true;
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

    private boolean handleForceResetConsole(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /hiam admin forcereset <player>", NamedTextColor.RED));
            return true;
        }

        Player messenger = getMessengerPlayer(sender);
        if (messenger == null) {
            sender.sendMessage(Component.text("No online players on the auth server to send the request", NamedTextColor.RED));
            return true;
        }

        String targetName = args[2];
        plugin.getMessagingService().sendAdminForceReset(messenger, null, targetName);
        sender.sendMessage(Component.text("Force reset request sent for " + targetName, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleResetPasswordConsole(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /hiam admin resetpassword <player> <new password>", NamedTextColor.RED));
            return true;
        }

        Player messenger = getMessengerPlayer(sender);
        if (messenger == null) {
            sender.sendMessage(Component.text("No online players on the auth server to send the request", NamedTextColor.RED));
            return true;
        }

        String targetName = args[2];
        String newPassword = args[3];
        plugin.getMessagingService().sendAdminResetPassword(messenger, null, targetName, newPassword);
        sender.sendMessage(Component.text("Password reset request sent for " + targetName, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleClearPasswordConsole(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /hiam admin clearpassword <player>", NamedTextColor.RED));
            return true;
        }

        Player messenger = getMessengerPlayer(sender);
        if (messenger == null) {
            sender.sendMessage(Component.text("No online players on the auth server to send the request", NamedTextColor.RED));
            return true;
        }

        String targetName = args[2];
        plugin.getMessagingService().sendAdminClearPassword(messenger, null, targetName);
        sender.sendMessage(Component.text("Password clear request sent for " + targetName, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetPremiumConsole(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /hiam admin setpremium <player> <uuid>", NamedTextColor.RED));
            return true;
        }

        Player messenger = getMessengerPlayer(sender);
        if (messenger == null) {
            sender.sendMessage(Component.text("No online players on the auth server to send the request", NamedTextColor.RED));
            return true;
        }

        String targetName = args[2];
        UUID targetUuid = parseUuid(args[3]);
        if (targetUuid == null) {
            sender.sendMessage(Component.text("Invalid UUID", NamedTextColor.RED));
            return true;
        }

        plugin.getMessagingService().sendAdminSetPremium(messenger, targetUuid, targetName);
        sender.sendMessage(Component.text("Set premium request sent for " + targetName, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetOfflineConsole(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /hiam admin setoffline <player>", NamedTextColor.RED));
            return true;
        }

        Player messenger = getMessengerPlayer(sender);
        if (messenger == null) {
            sender.sendMessage(Component.text("No online players on the auth server to send the request", NamedTextColor.RED));
            return true;
        }

        String targetName = args[2];
        plugin.getMessagingService().sendAdminSetOffline(messenger, null, targetName);
        sender.sendMessage(Component.text("Set offline request sent for " + targetName, NamedTextColor.GREEN));
        return true;
    }

    private Player getMessengerPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        return Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

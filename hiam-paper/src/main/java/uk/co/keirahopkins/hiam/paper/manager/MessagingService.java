package uk.co.keirahopkins.hiam.paper.manager;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.paper.HelixIAMPaper;

import java.io.*;
import java.util.UUID;

public class MessagingService implements PluginMessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(MessagingService.class);
    private static final String CHANNEL = "hiam:auth";
    
    private final HelixIAMPaper plugin;
    
    public MessagingService(HelixIAMPaper plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) {
            return;
        }
        
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String subchannel = in.readUTF();
            String data = "";
            if (in.available() > 0) {
                data = in.readUTF();
            }
            
            switch (subchannel) {
                case "AuthSuccess" -> handleAuthSuccess(player);
                case "RegisterSuccess" -> handleRegisterSuccess(player, data);
                case "RegisterFail" -> handleRegisterFail(player, data);
                case "LoginSuccess" -> handleAuthSuccess(player);
                case "LoginFail" -> handleLoginFail(player, data);
                case "ChangePasswordSuccess" -> handleChangePasswordSuccess(player);
                case "ChangePasswordFail" -> handleChangePasswordFail(player, data);
                case "PremiumSuccess" -> handlePremiumSuccess(player);
                case "PremiumFail" -> handlePremiumFail(player, data);
                case "OfflineSuccess" -> handleOfflineSuccess(player, data);
                case "OfflineFail" -> handleOfflineFail(player, data);
                case "AuthInfoResponse" -> handleAuthInfoResponse(player, data);
                case "AuthInfoFail" -> handleAuthInfoFail(player, data);
                case "ShowPremiumPrompt" -> handleShowPremiumPrompt(player);
                default -> logger.warn("Unknown plugin message subchannel: {}", subchannel);
            }
        } catch (IOException e) {
            logger.error("Error reading plugin message", e);
        }
    }
    
    private void handleAuthSuccess(Player player) {
        // Unfreeze player
        plugin.getFreezeManager().unfreeze(player);
        
        // Teleport to main spawn
        if (plugin.getSpawnManager().hasMainSpawn()) {
            plugin.getSpawnManager().teleportToMainSpawn(player);
        }
        
        logger.info("Player {} authenticated successfully", player.getName());
    }

    private void handleRegisterSuccess(Player player, String data) {
        handleAuthSuccess(player);
        if ("true".equalsIgnoreCase(data)) {
            handleShowPremiumPrompt(player);
        } else {
            player.sendMessage(net.kyori.adventure.text.Component.text("Registration successful", net.kyori.adventure.text.format.NamedTextColor.GREEN));
        }
    }

    private void handleRegisterFail(Player player, String reason) {
        String message = reason == null || reason.isBlank() ? "Registration failed" : reason;
        player.sendMessage(net.kyori.adventure.text.Component.text(message, net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    private void handleLoginFail(Player player, String reason) {
        String message = reason == null || reason.isBlank() ? "Login failed" : reason;
        player.sendMessage(net.kyori.adventure.text.Component.text(message, net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    private void handleChangePasswordSuccess(Player player) {
        player.sendMessage(net.kyori.adventure.text.Component.text("Password updated successfully", net.kyori.adventure.text.format.NamedTextColor.GREEN));
    }

    private void handleChangePasswordFail(Player player, String reason) {
        String message = reason == null || reason.isBlank() ? "Password change failed" : reason;
        player.sendMessage(net.kyori.adventure.text.Component.text(message, net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    private void handlePremiumSuccess(Player player) {
        player.sendMessage(net.kyori.adventure.text.Component.text("Premium mode enabled", net.kyori.adventure.text.format.NamedTextColor.GREEN));
    }

    private void handlePremiumFail(Player player, String reason) {
        String message = reason == null || reason.isBlank() ? "Premium mode failed" : reason;
        player.sendMessage(net.kyori.adventure.text.Component.text(message, net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    private void handleOfflineSuccess(Player player, String reason) {
        String message = reason == null || reason.isBlank()
            ? "Password authentication enabled"
            : reason;
        player.sendMessage(net.kyori.adventure.text.Component.text(message, net.kyori.adventure.text.format.NamedTextColor.YELLOW));
    }

    private void handleOfflineFail(Player player, String reason) {
        String message = reason == null || reason.isBlank() ? "Offline mode failed" : reason;
        player.sendMessage(net.kyori.adventure.text.Component.text(message, net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    private void handleAuthInfoResponse(Player player, String data) {
        String info = data == null || data.isBlank() ? "No auth info returned" : data;
        player.sendMessage(net.kyori.adventure.text.Component.text("=== Helix IAM Auth Info ===", net.kyori.adventure.text.format.NamedTextColor.GOLD));
        for (String line : info.split("\n")) {
            if (!line.isBlank()) {
                player.sendMessage(net.kyori.adventure.text.Component.text(line, net.kyori.adventure.text.format.NamedTextColor.GRAY));
            }
        }
    }

    private void handleAuthInfoFail(Player player, String reason) {
        String message = reason == null || reason.isBlank() ? "Auth info lookup failed" : reason;
        player.sendMessage(net.kyori.adventure.text.Component.text(message, net.kyori.adventure.text.format.NamedTextColor.RED));
    }
    
    private void handleShowPremiumPrompt(Player player) {
        plugin.getAuthPromptManager().showPremiumPrompt(player);
    }
    
    public void sendRegisterRequest(Player player, String password) {
        sendMessage(player, "Register", player.getName(), password);
    }
    
    public void sendLoginRequest(Player player, String password) {
        sendMessage(player, "Login", player.getName(), password);
    }
    
    public void sendChangePasswordRequest(Player player, String oldPassword, String newPassword) {
        sendMessage(player, "ChangePassword", player.getName(), oldPassword, newPassword);
    }
    
    public void sendPremiumRequest(Player player) {
        sendMessage(player, "EnablePremium", player.getName());
    }
    
    public void sendOfflineRequest(Player player) {
        sendMessage(player, "DisablePremium", player.getName());
    }
    
    public void sendAdminForceReset(Player admin, UUID targetUuid, String targetName) {
        sendMessage(admin, "AdminForceReset", targetName);
    }
    
    public void sendAdminResetPassword(Player admin, UUID targetUuid, String targetName, String newPassword) {
        sendMessage(admin, "AdminResetPassword", targetName, newPassword);
    }
    
    public void sendAdminClearPassword(Player admin, UUID targetUuid, String targetName) {
        sendMessage(admin, "AdminClearPassword", targetName);
    }
    
    public void sendAdminSetPremium(Player admin, UUID targetUuid, String targetName) {
        sendMessage(admin, "AdminSetPremium", targetName, targetUuid.toString());
    }
    
    public void sendAdminSetOffline(Player admin, UUID targetUuid, String targetName) {
        sendMessage(admin, "AdminSetOffline", targetName);
    }
    
    public void sendAuthInfoRequest(Player player, String targetName) {
        sendMessage(player, "AuthInfo", targetName);
    }
    
    private void sendMessage(Player player, String subchannel, String... fields) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(stream)) {
            
            out.writeUTF(subchannel);
            if (fields != null) {
                for (String field : fields) {
                    out.writeUTF(field == null ? "" : field);
                }
            }
            
            player.sendPluginMessage(plugin, CHANNEL, stream.toByteArray());
            logger.debug("Sent {} message for player {}", subchannel, player.getName());
            
        } catch (IOException e) {
            logger.error("Error sending plugin message", e);
        }
    }
}

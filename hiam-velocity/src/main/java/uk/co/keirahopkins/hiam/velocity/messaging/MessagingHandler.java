package uk.co.keirahopkins.hiam.velocity.messaging;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;
import uk.co.keirahopkins.hiam.velocity.HelixIAMVelocity;
import uk.co.keirahopkins.hiam.velocity.database.AccountRepository;
import uk.co.keirahopkins.hiam.velocity.database.CredentialRepository;
import uk.co.keirahopkins.hiam.velocity.database.SessionRepository;
import uk.co.keirahopkins.hiam.velocity.model.Account;
import uk.co.keirahopkins.hiam.velocity.model.Session;
import uk.co.keirahopkins.hiam.velocity.security.PasswordHasher;
import uk.co.keirahopkins.hiam.velocity.util.ClientDetector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles plugin messaging between Velocity and Paper servers.
 */
public class MessagingHandler {
    
    private static final MinecraftChannelIdentifier CHANNEL = 
        MinecraftChannelIdentifier.from("hiam:auth");
    
    private final HelixIAMVelocity plugin;
    private final Logger logger;
    private final AccountRepository accountRepository;
    private final CredentialRepository credentialRepository;
    private final SessionRepository sessionRepository;
    private final PasswordHasher passwordHasher;
    
    public MessagingHandler(HelixIAMVelocity plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.accountRepository = plugin.getAccountRepository();
        this.credentialRepository = plugin.getCredentialRepository();
        this.sessionRepository = plugin.getSessionRepository();
        this.passwordHasher = plugin.getPasswordHasher();
    }
    
    public void register() {
        plugin.getServer().getChannelRegistrar().register(CHANNEL);
        plugin.getServer().getEventManager().register(plugin, this);
        logger.info("Plugin messaging channel registered: {}", CHANNEL.getId());
    }
    
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) {
            return;
        }
        
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }
        
        ServerConnection source = (ServerConnection) event.getSource();
        Player player = source.getPlayer();
        
        try {
            byte[] data = event.getData();
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            
            String messageType = in.readUTF();
            
            switch (messageType) {
                case "Register" -> handleRegister(player, in);
                case "Login" -> handleLogin(player, in);
                case "ChangePassword" -> handleChangePassword(player, in);
                case "EnablePremium" -> handleEnablePremium(player, in);
                case "DisablePremium" -> handleDisablePremium(player, in);
                case "CheckAuth" -> handleCheckAuth(source, player, in);
                case "AuthInfo" -> handleAuthInfo(player, in);
                case "AdminForceReset" -> handleAdminForceReset(in);
                case "AdminResetPassword" -> handleAdminResetPassword(in);
                case "AdminClearPassword" -> handleAdminClearPassword(in);
                case "AdminSetPremium" -> handleAdminSetPremium(in);
                case "AdminSetOffline" -> handleAdminSetOffline(in);
                default -> logger.warn("Unknown message type: {}", messageType);
            }
            
        } catch (IOException e) {
            logger.error("Error handling plugin message from player: {}", player.getUsername(), e);
        }
    }
    
    private void handleRegister(Player player, DataInputStream in) throws IOException {
        String username = readUtfOrDefault(in, player.getUsername());
        String password = readUtfOrDefault(in, "");
        if (password.isBlank()) {
            sendResponse(player, "RegisterFail", "Password missing");
            return;
        }
        
        logger.info("Processing registration for: {}", username);
        
        // Check if account already exists
        Optional<Account> existing = accountRepository.findByUsername(username.toLowerCase());
        if (existing.isPresent()) {
            sendResponse(player, "RegisterFail", "Account already exists");
            return;
        }
        
        // Create account
        Account account = accountRepository.create(username.toLowerCase());
        
        // Store password credential
        String passwordHash = passwordHasher.hash(password);
        credentialRepository.upsertPasswordHash(account.getId(), passwordHash);

        account.setLastLoginAt(Instant.now());
        account.setLastIp(player.getRemoteAddress().getAddress().getHostAddress());
        accountRepository.save(account);
        logger.info("Account registered: {}", username);

        // Create session so registration completes authentication
        createSession(player, account, "PASSWORD", false);
        
        // Check if Java client to send premium prompt
        boolean isJava = ClientDetector.isJavaClient(player);
        
        sendResponse(player, "RegisterSuccess", isJava ? "true" : "false");
        if (!isJava) {
            routePostLogin(player);
        }
    }
    
    private void handleLogin(Player player, DataInputStream in) throws IOException {
        String username = readUtfOrDefault(in, player.getUsername());
        String password = readUtfOrDefault(in, "");
        if (password.isBlank()) {
            sendResponse(player, "LoginFail", "Password missing");
            return;
        }
        
        logger.info("Processing login for: {}", username);
        
        Optional<Account> accountOpt = accountRepository.findByUsername(username.toLowerCase());
        if (accountOpt.isEmpty()) {
            sendResponse(player, "LoginFail", "Account not found");
            return;
        }
        
        Account account = accountOpt.get();

        // Verify password (works for all client types, including Eagler)
        Optional<String> storedHash = credentialRepository.getPasswordHash(account.getId());
        if (storedHash.isEmpty()) {
            sendResponse(player, "LoginFail", "Password not set for this account");
            return;
        }
        if (!passwordHasher.verify(storedHash.get(), password)) {
            sendResponse(player, "LoginFail", "Incorrect password");
            return;
        }
        
        // Create session
        createSession(player, account, "PASSWORD", false);
        
        // Update last login
        account.setLastLoginAt(Instant.now());
        account.setLastIp(player.getRemoteAddress().getAddress().getHostAddress());
        accountRepository.save(account);
        
        logger.info("Login successful: {}", username);
        sendResponse(player, "LoginSuccess", "");
        routePostLogin(player);
    }
    
    private void handleChangePassword(Player player, DataInputStream in) throws IOException {
        String username = readUtfOrDefault(in, player.getUsername());
        String oldPassword = readUtfOrDefault(in, "");
        String newPassword = readUtfOrDefault(in, "");
        if (oldPassword.isBlank() || newPassword.isBlank()) {
            sendResponse(player, "ChangePasswordFail", "Missing password values");
            return;
        }
        
        logger.info("Processing password change for: {}", username);
        
        Optional<Account> accountOpt = accountRepository.findByUsername(username.toLowerCase());
        if (accountOpt.isEmpty()) {
            sendResponse(player, "ChangePasswordFail", "Account not found");
            return;
        }
        
        Account account = accountOpt.get();
        
        Optional<String> storedHash = credentialRepository.getPasswordHash(account.getId());
        if (storedHash.isEmpty()) {
            sendResponse(player, "ChangePasswordFail", "Password not set for this account");
            return;
        }

        // Verify old password
        if (!passwordHasher.verify(storedHash.get(), oldPassword)) {
            sendResponse(player, "ChangePasswordFail", "Incorrect old password");
            return;
        }
        
        // Hash new password
        String newPasswordHash = passwordHasher.hash(newPassword);
        credentialRepository.upsertPasswordHash(account.getId(), newPasswordHash);
        
        logger.info("Password changed: {}", username);
        sendResponse(player, "ChangePasswordSuccess", "");
    }
    
    private void handleEnablePremium(Player player, DataInputStream in) throws IOException {
        String username = readUtfOrDefault(in, player.getUsername());
        
        logger.info("Processing premium enable for: {}", username);
        
        // Check if Java client
        if (!ClientDetector.isJavaClient(player)) {
            sendResponse(player, "PremiumFail", "Premium mode is only for Java Edition");
            return;
        }
        
        Optional<Account> accountOpt = accountRepository.findByUsername(username.toLowerCase());
        if (accountOpt.isEmpty()) {
            sendResponse(player, "PremiumFail", "Account not found");
            return;
        }
        
        Account account = accountOpt.get();
        
        // Add premium credential (keep password for non-Java clients)
        credentialRepository.upsertPremiumUuid(account.getId(), player.getUniqueId());
        
        // Create premium session
        createSession(player, account, "PREMIUM", true);
        
        logger.info("Premium mode enabled: {}", username);
        sendResponse(player, "PremiumSuccess", "");
        routePostLogin(player);
    }
    
    private void handleDisablePremium(Player player, DataInputStream in) throws IOException {
        String username = readUtfOrDefault(in, player.getUsername());
        
        logger.info("Processing premium disable for: {}", username);
        
        Optional<Account> accountOpt = accountRepository.findByUsername(username.toLowerCase());
        if (accountOpt.isEmpty()) {
            sendResponse(player, "OfflineFail", "Account not found");
            return;
        }
        
        Account account = accountOpt.get();
        
        // Remove premium credential
        credentialRepository.clearPremiumUuid(account.getId());
        
        logger.info("Offline mode enabled: {}", username);
        sendResponse(player, "OfflineSuccess", "Password required - contact admin");
    }
    
    private void handleCheckAuth(ServerConnection source, Player player, DataInputStream in) throws IOException {
        String playerId = readUtfOrDefault(in, player.getUniqueId().toString());
        logger.debug("Checking auth for: {}", player.getUsername());
        
        boolean authenticated = plugin.getPostLoginEventListener().isAuthenticated(UUID.fromString(playerId));

        sendAuthResponse(source, playerId, authenticated);
    }
    
    private void handleAdminForceReset(DataInputStream in) throws IOException {
        String targetUsername = readUtfOrDefault(in, "");
        if (targetUsername.isBlank()) {
            logger.warn("Force reset missing username");
            return;
        }
        
        logger.info("Processing force reset for: {}", targetUsername);
        
        Optional<Account> accountOpt = accountRepository.findByUsername(targetUsername.toLowerCase());
        if (accountOpt.isEmpty()) {
            logger.warn("Account not found for force reset: {}", targetUsername);
            return;
        }
        
        Account account = accountOpt.get();
        credentialRepository.clearPassword(account.getId());
        credentialRepository.clearPremiumUuid(account.getId());
        
        // Delete all sessions
        sessionRepository.deleteAllForAccount(account.getId());
        
        logger.info("Account force reset: {}", targetUsername);
    }
    
    private void handleAdminResetPassword(DataInputStream in) throws IOException {
        String targetUsername = readUtfOrDefault(in, "");
        String newPassword = readUtfOrDefault(in, "");
        if (targetUsername.isBlank() || newPassword.isBlank()) {
            logger.warn("Password reset missing username or password");
            return;
        }
        
        logger.info("Processing password reset for: {}", targetUsername);
        
        Optional<Account> accountOpt = accountRepository.findByUsername(targetUsername.toLowerCase());
        if (accountOpt.isEmpty()) {
            logger.warn("Account not found for password reset: {}", targetUsername);
            return;
        }
        
        Account account = accountOpt.get();
        String newPasswordHash = passwordHasher.hash(newPassword);
        credentialRepository.upsertPasswordHash(account.getId(), newPasswordHash);
        
        logger.info("Password reset: {}", targetUsername);
    }
    
    private void handleAdminClearPassword(DataInputStream in) throws IOException {
        String targetUsername = readUtfOrDefault(in, "");
        if (targetUsername.isBlank()) {
            logger.warn("Clear password missing username");
            return;
        }
        
        logger.info("Processing password clear for: {}", targetUsername);
        
        Optional<Account> accountOpt = accountRepository.findByUsername(targetUsername.toLowerCase());
        if (accountOpt.isEmpty()) {
            logger.warn("Account not found for password clear: {}", targetUsername);
            return;
        }
        
        Account account = accountOpt.get();
        credentialRepository.clearPassword(account.getId());
        
        logger.info("Password cleared: {}", targetUsername);
    }
    
    private void handleAdminSetPremium(DataInputStream in) throws IOException {
        String targetUsername = readUtfOrDefault(in, "");
        String uuidString = readUtfOrDefault(in, "");
        if (targetUsername.isBlank() || uuidString.isBlank()) {
            logger.warn("Set premium missing username or uuid");
            return;
        }
        
        logger.info("Processing set premium for: {}", targetUsername);
        
        Optional<Account> accountOpt = accountRepository.findByUsername(targetUsername.toLowerCase());
        if (accountOpt.isEmpty()) {
            logger.warn("Account not found for set premium: {}", targetUsername);
            return;
        }
        
        Account account = accountOpt.get();
        credentialRepository.upsertPremiumUuid(account.getId(), UUID.fromString(uuidString));
        
        logger.info("Account set to premium: {}", targetUsername);
    }
    
    private void handleAdminSetOffline(DataInputStream in) throws IOException {
        String targetUsername = readUtfOrDefault(in, "");
        if (targetUsername.isBlank()) {
            logger.warn("Set offline missing username");
            return;
        }
        
        logger.info("Processing set offline for: {}", targetUsername);
        
        Optional<Account> accountOpt = accountRepository.findByUsername(targetUsername.toLowerCase());
        if (accountOpt.isEmpty()) {
            logger.warn("Account not found for set offline: {}", targetUsername);
            return;
        }
        
        Account account = accountOpt.get();
        credentialRepository.clearPremiumUuid(account.getId());
        
        logger.info("Account set to offline: {}", targetUsername);
    }

    private void handleAuthInfo(Player player, DataInputStream in) throws IOException {
        String targetUsername = readUtfOrDefault(in, "");
        if (targetUsername.isBlank()) {
            sendResponse(player, "AuthInfoFail", "Target username missing");
            return;
        }

        Optional<Account> accountOpt = accountRepository.findByUsername(targetUsername.toLowerCase());
        if (accountOpt.isEmpty()) {
            sendResponse(player, "AuthInfoFail", "Account not found for " + targetUsername);
            return;
        }

        Account account = accountOpt.get();
        StringBuilder info = new StringBuilder();
        info.append("Username: ").append(account.getUsername()).append("\n");
        Optional<UUID> premiumUuid = credentialRepository.getPremiumUuid(account.getId());
        boolean hasPassword = credentialRepository.hasPassword(account.getId());
        info.append("Has Password: ").append(hasPassword).append("\n");
        info.append("Premium UUID: ").append(premiumUuid.isEmpty() ? "(none)" : premiumUuid.get()).append("\n");
        info.append("Last IP: ").append(account.getLastIp() == null ? "(unknown)" : account.getLastIp()).append("\n");
        info.append("Last Login: ").append(account.getLastLoginAt() == null ? "(never)" : account.getLastLoginAt()).append("\n");
        info.append("Created: ").append(account.getCreatedAt() == null ? "(unknown)" : account.getCreatedAt()).append("\n");
        info.append("Updated: ").append(account.getUpdatedAt() == null ? "(unknown)" : account.getUpdatedAt());

        sendResponse(player, "AuthInfoResponse", info.toString());
    }
    
    private void createSession(Player player, Account account, String authType, boolean isPremium) {
        int ttlMinutes = plugin.getConfig().getSessionTtlMinutes();
        Instant expiresAt = Instant.now().plusSeconds(ttlMinutes * 60L);
        
        Session session = new Session(
            UUID.randomUUID(),
            account.getId(),
            Instant.now(),
            expiresAt,
            player.getRemoteAddress().getAddress().getHostAddress(),
            player.getProtocolVersion().toString(),
            authType,
            isPremium
        );
        
        sessionRepository.save(session);
        plugin.getSessionManager().cacheSession(account.getId(), session);
        plugin.getPostLoginEventListener().markAuthenticated(player.getUniqueId());
    }

    private void routePostLogin(Player player) {
        String primary = plugin.getConfig().getRoutingPostLoginTarget();
        String fallback = plugin.getConfig().getRoutingFallbackServer();

        if (primary == null || primary.isBlank()) {
            logger.warn("Post-login target not configured");
            return;
        }

        plugin.getServer().getServer(primary).ifPresentOrElse(server -> {
            player.createConnectionRequest(server).fireAndForget();
        }, () -> {
            if (fallback != null && !fallback.isBlank()) {
                plugin.getServer().getServer(fallback).ifPresentOrElse(fallbackServer -> {
                    logger.warn("Post-login target '{}' not found; sending {} to fallback '{}'", primary, player.getUsername(), fallback);
                    player.createConnectionRequest(fallbackServer).fireAndForget();
                }, () -> logger.error("Fallback server '{}' not found for player {}", fallback, player.getUsername()));
            } else {
                logger.error("Post-login target '{}' not found and fallback not configured", primary);
            }
        });
    }
    
    private String readUtfOrDefault(DataInputStream in, String defaultValue) throws IOException {
        if (in.available() <= 0) {
            return defaultValue;
        }
        String value = in.readUTF();
        return value == null ? defaultValue : value;
    }

    private void sendResponse(Player player, String messageType, String data) {
        try {
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(msgBytes);
            
            out.writeUTF(messageType);
            out.writeUTF(data == null ? "" : data);
            
            player.getCurrentServer().ifPresent(server -> {
                server.sendPluginMessage(CHANNEL, msgBytes.toByteArray());
            });
            
        } catch (IOException e) {
            logger.error("Error sending plugin message to player: {}", player.getUsername(), e);
        }
    }

    private void sendAuthResponse(ServerConnection source, String playerId, boolean authenticated) {
        try {
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(msgBytes);
            
            out.writeUTF("AuthResponse");
            out.writeUTF(playerId);
            out.writeBoolean(authenticated);

            logger.debug("Sending AuthResponse for {} -> {} (authenticated={})", playerId, source.getServerInfo().getName(), authenticated);
            source.sendPluginMessage(CHANNEL, msgBytes.toByteArray());
        } catch (IOException e) {
            logger.error("Error sending auth response for player ID: {}", playerId, e);
        }
    }
}

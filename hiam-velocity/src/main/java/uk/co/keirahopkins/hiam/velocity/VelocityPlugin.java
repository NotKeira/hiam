package uk.co.keirahopkins.hiam.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import uk.co.keirahopkins.hiam.velocity.config.Config;
import uk.co.keirahopkins.hiam.velocity.database.AccountRepository;
import uk.co.keirahopkins.hiam.velocity.database.DatabaseManager;
import uk.co.keirahopkins.hiam.velocity.database.CredentialRepository;
import uk.co.keirahopkins.hiam.velocity.database.SessionRepository;
import uk.co.keirahopkins.hiam.velocity.listener.PostLoginEventListener;
import uk.co.keirahopkins.hiam.velocity.listener.PreLoginEventListener;
import uk.co.keirahopkins.hiam.velocity.listener.ProxyConnectListener;
import uk.co.keirahopkins.hiam.velocity.manager.RateLimiter;
import uk.co.keirahopkins.hiam.velocity.manager.SessionManager;
import uk.co.keirahopkins.hiam.velocity.messaging.MessagingHandler;
import uk.co.keirahopkins.hiam.velocity.security.PasswordHasher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Plugin(
    id = "hiam-velocity",
    name = "Helix IAM - Velocity",
    version = "2.3.2",
    description = "Velocity plugin for Helix IAM authentication",
    authors = {"Keira Hopkins"}
)
public class VelocityPlugin {
    
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    
    private Config config;
    private DatabaseManager databaseManager;
    private AccountRepository accountRepository;
    private CredentialRepository credentialRepository;
    private SessionRepository sessionRepository;
    private SessionManager sessionManager;
    private RateLimiter rateLimiter;
    private PasswordHasher passwordHasher;
    private MessagingHandler messagingHandler;
    private ScheduledExecutorService scheduler;
    
    private PreLoginEventListener preLoginEventListener;
    private PostLoginEventListener postLoginEventListener;
    private ProxyConnectListener serverPreConnectEventListener;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("Starting up Helix IAM Velocity plugin...");
        
        try {
            initializeDataDirectory();
            loadConfiguration();
            initializeDatabase();
            initializeRepositories();
            initializeManagers();
            initializeMessaging();
            initializeListeners();
            startScheduledTasks();
            
            logger.info("Helix IAM Velocity plugin started successfully");
        } catch (Exception e) {
            logger.error("Failed to start Helix IAM Velocity plugin", e);
            throw new RuntimeException("Failed to start plugin", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Shutting down Helix IAM Velocity plugin...");
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (passwordHasher != null) {
            passwordHasher.cleanup();
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        logger.info("Helix IAM Velocity plugin shut down successfully");
    }

    private void initializeDataDirectory() throws IOException {
        if (!Files.exists(dataDirectory)) {
            Files.createDirectories(dataDirectory);
            logger.info("Created data directory: {}", dataDirectory);
        }
        
        Path configPath = dataDirectory.resolve("config.yml");
        if (!Files.exists(configPath)) {
            try (InputStream is = getClass().getResourceAsStream("/config/config.yml")) {
                if (is != null) {
                    Files.copy(is, configPath);
                    logger.info("Created default config file: {}", configPath);
                } else {
                    logger.warn("Default config resource not found");
                }
            }
        }
    }

    private void loadConfiguration() throws IOException {
        Path configPath = dataDirectory.resolve("config.yml");
        this.config = new Config(configPath);
        logger.info("Configuration loaded from: {}", configPath);
    }

    private void initializeDatabase() throws SQLException, IOException {
        this.databaseManager = new DatabaseManager(config);
        
        if (!databaseManager.isConnected()) {
            throw new SQLException("Failed to establish database connection");
        }
        
        logger.info("Database connection established");
        
        databaseManager.runMigrations();
        logger.info("Database migrations completed");
    }

    private void initializeRepositories() {
        this.accountRepository = new AccountRepository(databaseManager);
        this.credentialRepository = new CredentialRepository(databaseManager);
        this.sessionRepository = new SessionRepository(databaseManager);
        logger.info("Repositories initialized");
    }

    private void initializeManagers() {
        this.passwordHasher = new PasswordHasher();
        this.sessionManager = new SessionManager(sessionRepository);
        this.rateLimiter = new RateLimiter(
            databaseManager,
            config.getLoginMaxAttempts(),
            config.getLoginLockoutMinutes()
        );
        logger.info("Managers initialized");
    }

    private void initializeMessaging() {
        this.messagingHandler = new MessagingHandler(this);
        messagingHandler.register();
        logger.info("Plugin messaging initialized");
    }

    private void initializeListeners() {
        this.preLoginEventListener = new PreLoginEventListener(rateLimiter);
        this.postLoginEventListener = new PostLoginEventListener(accountRepository, credentialRepository);
        this.serverPreConnectEventListener = new ProxyConnectListener(config, postLoginEventListener, server);
        
        server.getEventManager().register(this, preLoginEventListener);
        server.getEventManager().register(this, postLoginEventListener);
        server.getEventManager().register(this, serverPreConnectEventListener);
        
        logger.info("Event listeners registered");
    }

    private void startScheduledTasks() {
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sessionManager.cleanupExpiredSessions();
            } catch (Exception e) {
                logger.error("Error during session cleanup", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                rateLimiter.cleanup();
            } catch (Exception e) {
                logger.error("Error during rate limiter cleanup", e);
            }
        }, 1, 1, TimeUnit.HOURS);
        
        logger.info("Scheduled tasks started");
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Config getConfig() {
        return config;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public AccountRepository getAccountRepository() {
        return accountRepository;
    }

    public CredentialRepository getCredentialRepository() {
        return credentialRepository;
    }

    public SessionRepository getSessionRepository() {
        return sessionRepository;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public PasswordHasher getPasswordHasher() {
        return passwordHasher;
    }

    public PostLoginEventListener getPostLoginEventListener() {
        return postLoginEventListener;
    }
}

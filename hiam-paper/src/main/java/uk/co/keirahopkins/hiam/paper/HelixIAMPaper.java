package uk.co.keirahopkins.hiam.paper;

import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.paper.command.*;
import uk.co.keirahopkins.hiam.paper.config.PaperConfig;
import uk.co.keirahopkins.hiam.paper.listener.PlayerListener;
import uk.co.keirahopkins.hiam.paper.manager.*;

public class HelixIAMPaper extends JavaPlugin {
    
    private static final Logger logger = LoggerFactory.getLogger(HelixIAMPaper.class);
    
    private static HelixIAMPaper instance;
    
    private PaperConfig paperConfig;
    private FreezeManager freezeManager;
    private SpawnManager spawnManager;
    private MessagingService messagingService;
    private ConfirmationManager confirmationManager;
    private AuthPromptManager authPromptManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        logger.info("Enabling Helix IAM Paper Plugin...");
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize configuration
        paperConfig = new PaperConfig(this);
        paperConfig.load();
        
        // Initialize managers
        freezeManager = new FreezeManager();
        spawnManager = new SpawnManager(paperConfig);
        messagingService = new MessagingService(this);
        confirmationManager = new ConfirmationManager();
        authPromptManager = new AuthPromptManager();
        
        // Register plugin messaging channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, "hiam:auth");
        getServer().getMessenger().registerIncomingPluginChannel(this, "hiam:auth", messagingService);
        
        // Register event listeners
        registerListeners();
        
        // Register commands
        registerCommands();
        
        logger.info("Helix IAM Paper Plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        logger.info("Disabling Helix IAM Paper Plugin...");
        
        // Unregister plugin messaging channels
        getServer().getMessenger().unregisterIncomingPluginChannel(this, "hiam:auth");
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "hiam:auth");
        
        // Clear all frozen players
        freezeManager.clearAll();
        
        // Cancel all pending confirmations
        confirmationManager.clearAll();
        
        logger.info("Helix IAM Paper Plugin disabled successfully!");
    }
    
    private void registerListeners() {
        PlayerListener playerListener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);
    }
    
    private void registerCommands() {
        // Player commands
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("changepass").setExecutor(new ChangePasswordCommand(this));
        getCommand("premium").setExecutor(new PremiumCommand(this));
        getCommand("offline").setExecutor(new OfflineCommand(this));
        
        // Admin commands
        getCommand("authinfo").setExecutor(new AuthInfoCommand(this));
        getCommand("setloginspawn").setExecutor(new SetLoginSpawnCommand(this));
        getCommand("setmainspawn").setExecutor(new SetMainSpawnCommand(this));
        getCommand("hiam").setExecutor(new HiamCommand(this));
    }
    
    public static HelixIAMPaper getInstance() {
        return instance;
    }
    
    public PaperConfig getPaperConfig() {
        return paperConfig;
    }
    
    public FreezeManager getFreezeManager() {
        return freezeManager;
    }
    
    public SpawnManager getSpawnManager() {
        return spawnManager;
    }
    
    public MessagingService getMessagingService() {
        return messagingService;
    }
    
    public ConfirmationManager getConfirmationManager() {
        return confirmationManager;
    }
    
    public AuthPromptManager getAuthPromptManager() {
        return authPromptManager;
    }
}

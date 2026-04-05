package uk.co.keirahopkins.hiam.gate;

import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.keirahopkins.hiam.gate.command.AuthCommand;
import uk.co.keirahopkins.hiam.gate.command.HubCommand;
import uk.co.keirahopkins.hiam.gate.command.OfflineCommand;
import uk.co.keirahopkins.hiam.gate.command.PremiumCommand;
import uk.co.keirahopkins.hiam.gate.listener.GateListener;
import uk.co.keirahopkins.hiam.gate.manager.AuthPromptManager;
import uk.co.keirahopkins.hiam.gate.manager.ConfirmationManager;
import uk.co.keirahopkins.hiam.gate.manager.MessagingService;

public class GatePlugin extends JavaPlugin {
    
    private static final Logger logger = LoggerFactory.getLogger(GatePlugin.class);
    private static GatePlugin instance;
    private MessagingService messagingService;
    private ConfirmationManager confirmationManager;
    private AuthPromptManager authPromptManager;
    
    @Override
    public void onEnable() {
        instance = this;
        messagingService = new MessagingService(this);
        messagingService.initialize();

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        confirmationManager = new ConfirmationManager();
        authPromptManager = new AuthPromptManager();
        
        getServer().getPluginManager().registerEvents(
            new GateListener(this, messagingService), 
            this
        );

        getCommand("premium").setExecutor(new PremiumCommand(this));
        getCommand("offline").setExecutor(new OfflineCommand(this));
        
        AuthCommand hiamCommand = new AuthCommand(this);
        getCommand("hiam").setExecutor(hiamCommand);
        getCommand("hiam").setTabCompleter(new uk.co.keirahopkins.hiam.gate.command.AuthCommandCompleter());
        
        getCommand("hub").setExecutor(new HubCommand(this));
        
        logger.info("Helix IAM Gate enabled - lightweight auth gate active");
    }
    
    @Override
    public void onDisable() {
        if (messagingService != null) {
            messagingService.shutdown();
        }
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
        if (confirmationManager != null) {
            confirmationManager.clearAll();
        }
        logger.info("Helix IAM Gate disabled");
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

    public static GatePlugin getInstance() {
        return instance;
    }
}

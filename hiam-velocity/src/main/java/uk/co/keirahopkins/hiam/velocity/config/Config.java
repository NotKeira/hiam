package uk.co.keirahopkins.hiam.velocity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    
    private final Map<String, Object> config;

    public Config(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            throw new IOException("Config file not found: " + configPath);
        }

        try (InputStream input = Files.newInputStream(configPath)) {
            Yaml yaml = new Yaml();
            this.config = yaml.load(input);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T get(String path, T defaultValue) {
        String[] parts = path.split("\\.");
        Object current = config;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
                if (current == null) {
                    return defaultValue;
                }
            } else {
                return defaultValue;
            }
        }
        
        try {
            return (T) current;
        } catch (ClassCastException e) {
            logger.warn("Config value type mismatch for path: {}", path);
            return defaultValue;
        }
    }

    public String getDatabaseHost() {
        return get("database.host", "localhost");
    }

    public int getDatabasePort() {
        return get("database.port", 5432);
    }

    public String getDatabaseName() {
        return get("database.name", "helixiam");
    }

    public String getDatabaseUser() {
        return get("database.user", "hxnt_srv");
    }

    public String getDatabasePassword() {
        return get("database.password", "CHANGE_ME");
    }

    public String getDatabaseSchema() {
        return get("database.schema", "");
    }

    public boolean getDatabaseSsl() {
        return get("database.ssl", false);
    }

    public int getDatabasePoolSize() {
        return get("database.poolSize", 10);
    }

    public boolean getDatabaseInitDb() {
        return get("database.initDb", true);
    }

    public int getSessionTtlMinutes() {
        return get("session.ttlMinutes", 720);
    }

    public boolean getSessionIpLock() {
        return get("session.ipLock", true);
    }

    public int getLoginTimeoutSeconds() {
        return get("login.timeoutSeconds", 60);
    }

    public int getLoginMaxAttempts() {
        return get("login.maxAttempts", 5);
    }

    public int getLoginLockoutMinutes() {
        return get("login.lockoutMinutes", 15);
    }

    public String getRoutingAuthServer() {
        return get("routing.authServer", "auth");
    }

    public String getRoutingPostLoginTarget() {
        return get("routing.postLoginTarget", "lobby");
    }

    public String getRoutingFallbackServer() {
        return get("routing.fallbackServer", "survival");
    }

    public boolean getPremiumEnableAutoPrompt() {
        return get("premium.enableAutoPrompt", true);
    }

    public List<String> getCommandsAllowedPreAuth() {
        return get("commands.allowedPreAuth", List.of("/register", "/login", "/l", "/r", "/changepass", "/help"));
    }

    public boolean getPermissionsPlayerPremiumDefault() {
        return get("permissions.playerPremiumDefault", true);
    }

    public boolean getPermissionsPlayerOfflineDefault() {
        return get("permissions.playerOfflineDefault", true);
    }

    public boolean getPermissionsAdminDefaultOpOnly() {
        return get("permissions.adminDefaultOpOnly", true);
    }

    public int getSecurityMinPasswordLength() {
        return get("security.minPasswordLength", 8);
    }

    public boolean getSecurityRequireConfirmation() {
        return get("security.requireConfirmation", true);
    }

    public String getMessagingChannel() {
        return get("messaging.channel", "hiam:auth");
    }
}

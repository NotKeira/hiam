package uk.co.keirahopkins.hiam.velocity.util;

import com.velocitypowered.api.proxy.Player;

public class ClientDetector {
    
    public enum ClientType {
        JAVA,
        EAGLERCRAFT,
        UNKNOWN
    }

    public static ClientType detectClient(Player player) {
        String brand = player.getClientBrand();
        
        if (brand != null) {
            String brandLower = brand.toLowerCase();
            
            if (brandLower.contains("eagler")) {
                return ClientType.EAGLERCRAFT;
            }
            
            if (brandLower.contains("vanilla") || brandLower.contains("forge") || 
                brandLower.contains("fabric") || brandLower.contains("quilt") ||
                brandLower.contains("paper") || brandLower.contains("spigot")) {
                return ClientType.JAVA;
            }
        }
        
        return ClientType.UNKNOWN;
    }

    public static boolean isJavaClient(Player player) {
        ClientType type = detectClient(player);
        return type == ClientType.JAVA || type == ClientType.UNKNOWN;
    }

    public static boolean isEaglerClient(Player player) {
        return detectClient(player) == ClientType.EAGLERCRAFT;
    }

    public static String getUserAgent(Player player) {
        ClientType type = detectClient(player);
        String brand = player.getClientBrand();
        
        return switch (type) {
            case JAVA -> "Java/" + (brand != null ? brand : "vanilla");
            case EAGLERCRAFT -> "Eaglercraft/" + (brand != null ? brand : "unknown");
            case UNKNOWN -> "Unknown/" + (brand != null ? brand : "unknown");
        };
    }
}

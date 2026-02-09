package uk.co.keirahopkins.hiam.paper.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import uk.co.keirahopkins.hiam.paper.manager.ConfirmationManager.ConfirmationType;

public class AuthPromptManager {

    public AuthPromptManager() {
    }
    
    public void showPremiumPrompt(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("You're connecting from a Java Edition client!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Would you like to enable premium mode?", NamedTextColor.YELLOW));
        player.sendMessage(Component.empty());
        
        Component enableButton = Component.text("[Enable Premium]", NamedTextColor.GREEN, TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/premium"))
            .hoverEvent(HoverEvent.showText(Component.text("Click to enable premium mode\nYou won't need a password anymore!", NamedTextColor.GRAY)));
        
        Component keepButton = Component.text("[Keep Password]", NamedTextColor.RED, TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/offline"))
            .hoverEvent(HoverEvent.showText(Component.text("Click to keep using password authentication", NamedTextColor.GRAY)));
        
        player.sendMessage(Component.text()
            .append(enableButton)
            .append(Component.text("  "))
            .append(keepButton)
            .build());
        
        player.sendMessage(Component.empty());
    }
    
    public void showConfirmation(Player player, String token, ConfirmationType type) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("⚠ Confirmation Required", NamedTextColor.RED, TextDecoration.BOLD));
        player.sendMessage(Component.text(getConfirmationMessage(type), NamedTextColor.YELLOW));
        player.sendMessage(Component.empty());
        
        Component confirmButton = Component.text("[CONFIRM]", NamedTextColor.GREEN, TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/hiam admin confirm " + token))
            .hoverEvent(HoverEvent.showText(Component.text("Click to confirm this action", NamedTextColor.GRAY)));
        
        Component cancelButton = Component.text("[CANCEL]", NamedTextColor.RED, TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/hiam admin cancel " + token))
            .hoverEvent(HoverEvent.showText(Component.text("Click to cancel this action", NamedTextColor.GRAY)));
        
        player.sendMessage(Component.text()
            .append(confirmButton)
            .append(Component.text("  "))
            .append(cancelButton)
            .build());
        
        player.sendMessage(Component.text("Token expires in 30 seconds", NamedTextColor.GRAY, TextDecoration.ITALIC));
        player.sendMessage(Component.empty());
    }
    
    public void showPlayerConfirmation(Player player, String token, ConfirmationType type) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("⚠ Confirmation Required", NamedTextColor.RED, TextDecoration.BOLD));
        player.sendMessage(Component.text(getConfirmationMessage(type), NamedTextColor.YELLOW));
        player.sendMessage(Component.empty());
        
        Component confirmButton = Component.text("[CONFIRM]", NamedTextColor.GREEN, TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/hiam confirm " + token))
            .hoverEvent(HoverEvent.showText(Component.text("Click to confirm this action", NamedTextColor.GRAY)));
        
        Component cancelButton = Component.text("[CANCEL]", NamedTextColor.RED, TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/hiam cancel " + token))
            .hoverEvent(HoverEvent.showText(Component.text("Click to cancel this action", NamedTextColor.GRAY)));
        
        player.sendMessage(Component.text()
            .append(confirmButton)
            .append(Component.text("  "))
            .append(cancelButton)
            .build());
        
        player.sendMessage(Component.text("Token expires in 30 seconds", NamedTextColor.GRAY, TextDecoration.ITALIC));
        player.sendMessage(Component.empty());
    }
    
    private String getConfirmationMessage(ConfirmationType type) {
        switch (type) {
            case PLAYER_PREMIUM:
                return "Are you sure you want to enable premium mode?";
            case PLAYER_OFFLINE:
                return "Are you sure you want to keep password authentication?";
            case ADMIN_FORCE_RESET:
                return "Are you sure you want to force reset this player's account?";
            case ADMIN_RESET_PASSWORD:
                return "Are you sure you want to reset this player's password?";
            case ADMIN_CLEAR_PASSWORD:
                return "Are you sure you want to clear this player's password?";
            case ADMIN_SET_PREMIUM:
                return "Are you sure you want to set this player to premium mode?";
            case ADMIN_SET_OFFLINE:
                return "Are you sure you want to set this player to offline mode?";
            default:
                return "Are you sure you want to perform this action?";
        }
    }
}

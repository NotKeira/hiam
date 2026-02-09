package uk.co.keirahopkins.hiam.gate.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import uk.co.keirahopkins.hiam.gate.manager.ConfirmationManager.ConfirmationType;

public class AuthPromptManager {

    public void showPlayerConfirmation(Player player, String token, ConfirmationType type) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("\u26A0 Confirmation Required", NamedTextColor.RED, TextDecoration.BOLD));
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
            default:
                return "Are you sure you want to perform this action?";
        }
    }
}

package uk.co.keirahopkins.hiam.paper.pattern;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSenderMatcher {

    public static String getCommanderType(CommandSender sender) {
        return switch (sender) {
            case Player player -> "Player: " + player.getName();
            case _ -> "Console";
        };
    }

    public static boolean isPlayerCommand(CommandSender sender) {
        return sender instanceof Player;
    }
}

package uk.co.keirahopkins.hiam.gate.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AuthCommandCompleter implements TabCompleter {

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            // /hiam <subcommand>
            List<String> completions = new ArrayList<>();
            completions.add("changepass");
            completions.add("confirm");
            completions.add("cancel");
            if (player.hasPermission("hiam.admin.override")) {
                completions.add("admin");
            }
            return completions.stream()
                .filter(c -> c.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("changepass")) {
            // /hiam changepass <old> <new> - no completion
            return new ArrayList<>();
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("confirm")) {
            // /hiam confirm <token> - no completion
            return new ArrayList<>();
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("cancel")) {
            // /hiam cancel <token> - no completion
            return new ArrayList<>();
        }

        if (args[0].equalsIgnoreCase("admin")) {
            if (args.length == 2) {
                // /hiam admin <subcommand>
                List<String> completions = new ArrayList<>();
                if (player.hasPermission("hiam.admin.override")) {
                    completions.add("override");
                }
                return completions.stream()
                    .filter(c -> c.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }

            if (args.length >= 2 && args[1].equalsIgnoreCase("override")) {
                if (player.hasPermission("hiam.admin.override")) {
                    if (args.length == 3) {
                        // /hiam admin override <player>
                        return getPlayerCompletions(args[2]);
                    } else if (args.length == 4) {
                        // /hiam admin override <player> <mode>
                        List<String> modes = Arrays.asList("authServer", "currentServer", "off");
                        return modes.stream()
                            .filter(m -> m.startsWith(args[3]))
                            .collect(Collectors.toList());
                    }
                }
            }
        }

        return new ArrayList<>();
    }

    private List<String> getPlayerCompletions(String partial) {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
            .collect(Collectors.toList());
    }
}

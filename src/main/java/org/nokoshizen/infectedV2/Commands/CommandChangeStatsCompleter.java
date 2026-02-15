package org.nokoshizen.infectedV2.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class CommandChangeStatsCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("kills");
            suggestions.add("deaths");
            suggestions.add("gamesplayed");
            suggestions.add("gameswon");
            suggestions.add("gameswonashuman");
            suggestions.add("gameswonaszombie");
            suggestions.add("gamesloosed");
            suggestions.add("firstz");
            suggestions.add("all");
            suggestions.add("killsgueri");
            suggestions.add("killssurvivant");
            suggestions.add("killscarnage");
        }
        return suggestions;
    }
}

package org.nokoshizen.infectedV2.Commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nokoshizen.infectedV2.InfectedLogic;
import org.nokoshizen.infectedV2.InfectedV2;
import org.nokoshizen.infectedV2.SQLGestion;

public class CommandChangeStats implements CommandExecutor {
    SQLGestion sqlGestion;
    InfectedLogic  infectedLogic;

    public CommandChangeStats(InfectedV2 infectedV2, SQLGestion sqlGestion, InfectedLogic infectedLogic) {
        this.sqlGestion = sqlGestion;
        this.infectedLogic = infectedLogic;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Vous avez besoin d'être opérateur pour exécuter cette commande.");
            return true;
        }

        String argument = args.length > 0 ? args[0].toLowerCase() : "";
        String argument2 = args.length > 1 ? args[1].toLowerCase() : "";

        if (args.length != 2) return true;

        if (!argument2.matches("-?\\d+")) {
            player.sendMessage(ChatColor.RED + "La commande n'accepte que des numéros");
            return true;
        }

        switch (argument) {
            case "kills":
                sqlGestion.setKillStats(player, Integer.parseInt(argument2));
                break;
            case "deaths":
                sqlGestion.setDeathStats(player, Integer.parseInt(argument2));
                break;
            case "gamesplayed":
                sqlGestion.setGamePlayedStats(player, Integer.parseInt(argument2));
                break;
            case "gameswon":
                sqlGestion.setGameWonsStats(player, Integer.parseInt(argument2));
                break;
            case "gameswonashuman":
                sqlGestion.setGameWonsAsHumanStats(player, Integer.parseInt(argument2));
                break;
            case "gameswonaszombie":
                sqlGestion.setGameWonAsZombieStats(player, Integer.parseInt(argument2));
                break;
            case "gamesloosed":
                sqlGestion.setGameLoosedsStats(player, Integer.parseInt(argument2));
                break;
            case "firstz":
                sqlGestion.setFirstZombieStats(player, Integer.parseInt(argument2));
                break;
            case "all":
                sqlGestion.deletePlayerByName(player);
                break;
            case "killsgueri":
                infectedLogic.initializeKillsG = Integer.parseInt(argument2);
                break;
            case "killssurvivant":
                infectedLogic.initializeKillsS = Integer.parseInt(argument2);
                break;
            case "killscarnage":
                infectedLogic.initializeKillsC = Integer.parseInt(argument2);
                break;
        }

        return true;
    }
}

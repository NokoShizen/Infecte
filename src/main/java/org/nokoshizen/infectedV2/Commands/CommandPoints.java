package org.nokoshizen.infectedV2.Commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nokoshizen.infectedV2.InfectedV2;
import org.nokoshizen.infectedV2.SQLGestion;

public class CommandPoints implements CommandExecutor {
    SQLGestion sqlGestion;

    public CommandPoints(InfectedV2 infectedV2, SQLGestion sqlGestion) {
        this.sqlGestion = sqlGestion;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Vous avez besoin d'être opérateur pour exécuter cette commande.");
            return true;
        }

        String argument = args.length > 0 ? args[0].toLowerCase() : "";

        if (args.length > 1) return true;

        if (!argument.matches("-?\\d+")) {
            player.sendMessage(ChatColor.RED + "La commande n'accepte que des numéros");
            return true;
        }

        sqlGestion.setPoints(player, Integer.parseInt(argument));


        return true;
    }
}

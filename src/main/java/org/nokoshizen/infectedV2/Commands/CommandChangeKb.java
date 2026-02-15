package org.nokoshizen.infectedV2.Commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nokoshizen.infectedV2.InfectedLogic;
import org.nokoshizen.infectedV2.InfectedV2;

public class CommandChangeKb implements CommandExecutor {
    InfectedLogic infectedLogic;

    public CommandChangeKb(InfectedV2 infectedV2, InfectedLogic infectedLogic) {
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

        if (!argument2.matches("-?\\d+(\\.\\d+)?")) {
            player.sendMessage(ChatColor.RED + "La commande n'accepte que des numéros");
            return true;
        }

        switch (argument) {
            case "horizontal":
                infectedLogic.kbHorizontal = Double.parseDouble(argument2);
                break;
            case "vertical":
                infectedLogic.kbVertical = Double.parseDouble(argument2);
                break;
        }

        return true;
    }
}

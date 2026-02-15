package org.nokoshizen.infectedV2;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeamExpansion extends PlaceholderExpansion {
    InfectedV2 plugin;

    public TeamExpansion(InfectedV2 plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "customrank";
    }

    @Override
    public @NotNull String getAuthor() {
        return "TonNom";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        if (identifier.equalsIgnoreCase("team")) {

            if (plugin.inGameTeamHuman.contains(player)) {
                return "0";
            } else if (plugin.inGameTeamSkeleton.contains(player)) {
                return "1";
            } else if (plugin.inGameTeamPigman.contains(player)) {
                return "2";
            } else if (plugin.inGameTeamZombie.contains(player)) {
                return "3";
            } else {
                return "4";
            }
        }
        return null;
    }
}

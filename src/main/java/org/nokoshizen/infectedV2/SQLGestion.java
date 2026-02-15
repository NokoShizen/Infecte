package org.nokoshizen.infectedV2;

import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLGestion {
    InfectedV2 plugin;

    public SQLGestion(InfectedV2 plugin) {
        this.plugin = plugin;
    }

    public void initializePlayerStats(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            String name = player.getName();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "INSERT INTO player_stats (uuid, name, points, kills, deaths, games_played, games_won, games_won_as_human, games_won_as_zombie, games_loosed, first_zombie, guerison, total_survived_time, play_time, average_time_survived, choosen_chestplate) " +
                            "VALUES (?, ?, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9) " +
                            "ON DUPLICATE KEY UPDATE uuid = uuid"
            );
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deletePlayerByName(Player player) {
        try {
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "DELETE FROM player_stats WHERE name = ?"
            );
            ps.setString(1, player.getName());
            ps.close();

            initializePlayerStats(player);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void Kill(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET kills = kills + 1 WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setKillStats(Player player, int stats) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET kills = " +  stats + " WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void Death(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET deaths = deaths + 1 WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setDeathStats(Player player, int stats) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET deaths = " +  stats + " WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void GamePlayed(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET games_played = games_played + 1 WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setGamePlayedStats(Player player, int stats) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET games_played = " +  stats + " WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void GameWons(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET games_won = games_won + 1 WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setGameWonsStats(Player player, int stats) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET games_won = " +  stats + " WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void GameWonsAsHuman(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET games_won_as_human = games_won_as_human + 1 WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setGameWonsAsHumanStats(Player player, int stats) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET games_won_as_human = " +  stats + " WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void GameWonsAsZombie(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET games_won_as_zombie = games_won_as_zombie + 1 WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setGameWonAsZombieStats(Player player, int stats) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET games_won_as_zombie = " +  stats + " WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void GameLooseds(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET games_loosed = games_loosed + 1 WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setGameLoosedsStats(Player player, int stats) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET games_loosed = " +  stats + " WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void FirstZombie(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET first_zombie = first_zombie + 1 WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setFirstZombieStats(Player player, int stats) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET first_zombie = " +  stats + " WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void TotalSurvivedTime(Player player, int seconds) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET total_survived_time = total_survived_time + " + seconds + " WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void PlayTime(Player player, int seconds) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET play_time = play_time + " + seconds + " WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void AverageTimeSurvived(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats " +
                            "SET average_time_survived = total_survived_time / " +
                            "COALESCE(NULLIF(games_played - first_zombie + guerison, 0), 1) " +
                            "WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void Guerison(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET guerison = guerison + 1 WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getPoints(Player player) {
        try {
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "SELECT points FROM player_stats WHERE uuid = ?"
            );
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void addPoints(Player player, int points) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET points = points + " + points + " WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setPoints(Player player, int points) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET points = " + points + " WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setChoosenChestplate(Player player, int number) {
        try {
            String uuid = player.getUniqueId().toString();
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "UPDATE player_stats SET choosen_chestplate = " + number + " WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getChoosenChestplate(Player player) {
        try {
            PreparedStatement ps = plugin.connection.prepareStatement(
                    "SELECT choosen_chestplate FROM player_stats WHERE uuid = ?"
            );
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 9;
    }
}

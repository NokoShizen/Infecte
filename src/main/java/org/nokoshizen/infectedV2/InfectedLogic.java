package org.nokoshizen.infectedV2;


import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class InfectedLogic {
    InfectedV2 plugin;
    public int timeLeft;
    public int totalTime;
    public int timeGuerisonExpire = 120;
    private int taskId = -1;
    private int regenZombieTask = -1;
    public int initializeKillsG = 3;
    public int initializeKillsS = 4;
    public int initializeKillsC = 4;
    private int killsGuerison = initializeKillsG;
    private int killsCarnage = initializeKillsC;
    private int killsSurvivant = initializeKillsS;

    private List<Player> asSurvivantQuest = new ArrayList<>();
    private List<Player> asGuerisonQuest = new ArrayList<>();
    private List<Player> asCarnageQuest = new ArrayList<>();

    private int ptsSurvival = 5;
    private int ptsWin = 40;
    private int ptsZKills = 3;
    private int ptsQuete = 10;

    public double kbHorizontal = 1.3;
    public double kbVertical = 0.2;

    private final String[] chtpltColors = {
            "#A87752", "#A52B3B", "#A34E8D",
            "#6D4BA0", "#4F549E", "#39869B",
            "#439975", "#719949", "#C4AA29"
    };

    public String humanColor = "#008E8E";
    public String zombieColor = "#008E00";
    public String humanheart = "#94e6cd";
    public String zombieheart = "#7de66e";

    public String pigmanColor = "#927dae";
    public String skeletonColor = "#a53d3f";

    public String survivantQuest = "#2da59d";
    public String carnageQuest = "#927dae";
    public String guerisonQuest = "#7ca72e";

    private boolean isFirstZChoosed = false;

    public class DamageInfo {
        public double damage;
        public long lastHitTime;
        public BukkitTask expiryTask;
    }

    public final Map<UUID, Map<UUID, DamageInfo>> damageLog = new HashMap<>();
    public final Map<UUID, BukkitTask> resetTimers = new HashMap<>();
    public final Map<Player, Player> tracker = new HashMap<>();
    private final Random random = new Random();

    private final SQLGestion sqlGestion;
    private final SkullUtils skullUtils;

    public InfectedLogic(InfectedV2 plugin, SkullUtils skullUtils) {
        this.plugin = plugin;
        this.sqlGestion = new SQLGestion(plugin);
        this.skullUtils = skullUtils;
    }

    public void GameLogic() {
        StoppingTask();

        plugin.isGameStarted = true;
        plugin.inGameHumanTime.clear();
        plugin.inGameTeamZombie.clear();
        plugin.inGameFirstZombies.clear();

        plugin.inGameTeamSkeleton.clear();
        plugin.inGameTeamPigman.clear();

        plugin.inGamePlayerPoints.clear();
        plugin.inGamePlayerKills.clear();
        plugin.inGameZombieKills.clear();
        plugin.inGameHumanKills.clear();

        asSurvivantQuest.clear();
        asGuerisonQuest.clear();
        asCarnageQuest.clear();

        killsGuerison = initializeKillsG;
        killsCarnage = initializeKillsC;
        killsSurvivant = initializeKillsS;

        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.inGamePlayers.add(player);
            HumanLogic(player);
            if (player.getGameMode() != GameMode.CREATIVE) player.setGameMode(GameMode.ADVENTURE);
        }

        totalTime = (int) (plugin.GAME_PLAYTIME / 1000);
        timeLeft = (int) (plugin.GAME_PLAYTIME / 1000);

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            timeLeft--;
            float progress = (float) timeLeft / totalTime;

            long adventurePlayers = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getGameMode() != GameMode.CREATIVE)
                    .count();

            if (adventurePlayers <= 1) {
                Bukkit.broadcastMessage(plugin.infectedMessage + ChatColor.of(plugin.generalTextChatColor) + "Oh... Il n'y a plus assez de " + ChatColor.DARK_GREEN + "Joueurs" + ChatColor.of(plugin.generalTextChatColor) + " dans la partie...");
                DownTimeLogic();
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                ScoreBoardGestion(player, "0");
                TABGestion(player);

                if (player.getHealth() > 0 && player.getHealth() < player.getMaxHealth() && plugin.inGameTeamHuman.contains(player)) {
                    double newHealth = Math.min(player.getHealth() + 1.0, player.getMaxHealth());
                    player.setHealth(newHealth);
                }

                if (plugin.inGameTeamHuman.contains(player)) {
                    int humanTime = plugin.inGameHumanTime.getOrDefault(player.getUniqueId(), 0) + 1;
                    plugin.inGameHumanTime.put(player.getUniqueId(), humanTime);

                    HumanPointAttribute(player);

                    if (timeLeft % 5 == 0) {
                        Inventory playerInv = player.getInventory();
                        ItemStack item = playerInv.getItem(8);

                        int amount;
                        if (item != null) {
                            amount = item.getAmount();
                        } else amount = 0;

                        if (amount < 4) {
                            playerInv.setItem(8, new ItemStack(Material.ARROW, amount + 1));
                        }
                    }
                } else if (plugin.inGameTeamZombie.contains(player)) {

                    int zombieTime = plugin.inGameZombieTime.getOrDefault(player.getUniqueId(), 0) + 1;
                    plugin.inGameZombieTime.put(player.getUniqueId(), zombieTime);

                    if (timeLeft % 5 == 0 && plugin.inGameTeamSkeleton.contains(player)) {
                        Inventory playerInv = player.getInventory();
                        ItemStack item = playerInv.getItem(8);

                        int amount;
                        if (item != null) {
                            amount = item.getAmount();
                        } else amount = 0;


                        if (amount < 20) {
                            playerInv.setItem(8, new ItemStack(Material.ARROW, amount + 1));
                        }
                    }
                }
                if (plugin.inGameFirstZombies.contains(player.getUniqueId()) && timeLeft == timeGuerisonExpire) {
                    player.sendMessage(ChatColor.of(guerisonQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(guerisonQuest) + "---" +
                            ChatColor.of(guerisonQuest) + "\n❤ Quête Guerison \nVous n'avez pas réussie à faire votre quête. " + ChatColor.of("#a35252") + "✕\n" +
                            ChatColor.of(guerisonQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(guerisonQuest) + "---");
                    player.playSound(player.getLocation(), Sound.ENTITY_CAMEL_DASH, 0.5f, 1.0f);
                }

                if (timeLeft == totalTime - plugin.GAME_CHOOSINGZOMBIES + 1) {
                    player.setHealth(player.getMaxHealth());
                }

                player.setLevel(timeLeft);
                player.setExp(progress);
            }

            if (plugin.inGameTeamZombie.size() == 0) {
                if (timeLeft < totalTime - plugin.GAME_CHOOSINGZOMBIES) {
                    Bukkit.broadcastMessage(plugin.infectedMessage + ChatColor.of(plugin.generalTextChatColor) + "Oh ! Il n'y a plus de " + ChatColor.DARK_GREEN + "Zombie" + ChatColor.of(plugin.generalTextChatColor) + " dans la partie !");
                    ZombiesSelection();
                }
            }

            if (timeLeft == totalTime - plugin.GAME_CHOOSINGZOMBIES) {
                ZombiesSelection();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ZOGLIN_DEATH, 0.2f, 0f);
                    if (plugin.inGameTeamHuman.contains(player)) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.sendMessage(ChatColor.of(survivantQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(survivantQuest) + "---" +
                                    ChatColor.of(survivantQuest) + "\n❤ Quête Survivant \nTue des zombies pour gagner des points de vie !\n" +
                                    ChatColor.of(survivantQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(survivantQuest) + "---");
                        }, 200L);
                    }
                }
            }

            if ((timeLeft <= 5 && timeLeft > 0) || timeLeft == 30 || timeLeft == 90) {
                String secondeText = (timeLeft == 1) ? "seconde." : "secondes.";

                String timeDisplay;
                if (timeLeft == 90) {
                    timeDisplay = "<color:#8BB8BA>1</color> minute et <color:#8BB8BA>30</color> secondes.";
                } else {
                    timeDisplay = "<color:#8BB8BA>" + timeLeft + "</color> " + secondeText;
                }

                Component startingMessage = MiniMessage.miniMessage().deserialize(
                        "<!italic><gradient:" + plugin.generalTextChatColor + ":#E5A45E>Fin de la partie dans " +
                                timeDisplay + "</gradient>"
                ).decoration(TextDecoration.BOLD, false);

                Component prefix = Component.text(plugin.infectedMessage)
                        .decorate(TextDecoration.BOLD);

                Component finalMessage = prefix.append(startingMessage);

                Bukkit.broadcast(finalMessage);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_COPPER_BULB_TURN_ON, 10f, 1f);
                }
            }

            // Win ZOMBIE
            if (plugin.inGameTeamHuman.size() == 0) {
                for (Player player : plugin.inGamePlayers) {

                    if (plugin.inGameFirstZombies.contains(player.getUniqueId())) {
                        sqlGestion.GameWons(player);
                        sqlGestion.GameWonsAsZombie(player);
                    } else if (plugin.inGameTeamHuman.contains(player) || (plugin.inGameTeamZombie.contains(player) && !plugin.inGameFirstZombies.contains(player))) {
                        sqlGestion.GameLooseds(player);
                    }
                }
                Bukkit.broadcastMessage(plugin.infectedMessage + ChatColor.of(plugin.generalTextChatColor) + "Les " + ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Zombies" + ChatColor.of(plugin.generalTextChatColor) + " ont remportés la partie !");
                DownTimeLogic();
                return;
            }

            //Win HUMAIN
            if (timeLeft <= 0) {
                if (plugin.inGameTeamHuman.size() == 1) {
                    Bukkit.broadcastMessage(plugin.infectedMessage + ChatColor.of(plugin.generalTextChatColor) + "L'" + ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Humain" + ChatColor.of(plugin.generalTextChatColor) + " à remporté la partie !");
                } else {
                    Bukkit.broadcastMessage(plugin.infectedMessage + ChatColor.of(plugin.generalTextChatColor) + "Les " + ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Humains" + ChatColor.of(plugin.generalTextChatColor) + " ont remportés la partie !");
                }
                for (Player player : plugin.inGamePlayers) {
                    if (plugin.inGameTeamHuman.contains(player)) {
                        sqlGestion.GameWons(player);
                        sqlGestion.GameWonsAsHuman(player);
                    } else if (plugin.inGameTeamZombie.contains(player)) {
                        sqlGestion.GameLooseds(player);
                    }
                }
                DownTimeLogic();
                return;
            }

        }, 20L, 20L);

        regenZombieTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {

            for (Player player : plugin.inGameTeamZombie) {
                if (player != null && player.getHealth() > 0 && player.getHealth() < player.getMaxHealth()) {
                    double newHealth = Math.min(player.getHealth() + 1.0, player.getMaxHealth());
                    player.setHealth(newHealth);
                }

                if (plugin.inGameTeamHuman.size() <= 0) return;
                Player playerTarget = tracker.getOrDefault(player, plugin.inGameTeamHuman.get(random.nextInt(plugin.inGameTeamHuman.size())));
                TrackerActionBar(player, playerTarget);
            }
        }, 10L, 10L);
    }

    private void printDamageLogs() {
        Bukkit.getConsoleSender().sendMessage("=== DamageLog ===");
        for (UUID victimId : damageLog.keySet()) {
            Player victim = Bukkit.getPlayer(victimId);
            String victimName = (victim != null) ? victim.getName() : victimId.toString();

            Map<UUID, DamageInfo> attackers = damageLog.get(victimId);
            Bukkit.getConsoleSender().sendMessage("Victime: " + victimName);

            for (Map.Entry<UUID, DamageInfo> entry : attackers.entrySet()) {
                Player attacker = Bukkit.getPlayer(entry.getKey());
                String attackerName = (attacker != null) ? attacker.getName() : entry.getKey().toString();

                DamageInfo info = entry.getValue();
                Bukkit.getConsoleSender().sendMessage("  - Attaquant: " + attackerName +
                        ", Dégâts cumulés: " + info.damage +
                        ", LastHit: " + (System.currentTimeMillis() - info.lastHitTime) + "ms ago");
            }
        }

        Bukkit.getConsoleSender().sendMessage("=== ResetTimers ===");
        for (UUID victimId : resetTimers.keySet()) {
            Player victim = Bukkit.getPlayer(victimId);
            String victimName = (victim != null) ? victim.getName() : victimId.toString();
            Bukkit.getConsoleSender().sendMessage("Victime avec timer actif: " + victimName);
        }
    }

    public void DownTimeLogic() {
        StoppingTask();
        plugin.isGameStarted = false;
        plugin.choosenMap = "Spawn";


        timeLeft = (int) (plugin.GAME_DOWNTIME / 1000);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (timeLeft <= 0) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.getInventory().clear();
                    sqlGestion.GamePlayed(player);
                    sqlGestion.TotalSurvivedTime(player, plugin.inGameHumanTime.getOrDefault(player.getUniqueId(), 0));
                    sqlGestion.PlayTime(player, (plugin.inGameHumanTime.getOrDefault(player.getUniqueId(), 0) + plugin.inGameZombieTime.getOrDefault(player.getUniqueId(), 0)));

                    sqlGestion.AverageTimeSurvived(player);

                    sqlGestion.addPoints(player, plugin.inGamePlayerPoints.getOrDefault(player.getUniqueId(), 0));

                    Location location = plugin.mapSelection.get(plugin.choosenMap);

                    player.teleport(location);
                }
                StartingAndStoppingLogic();
                return;
            }
            timeLeft--;
        }, 20L, 20L);
    }

    public void StartingAndStoppingLogic() {
        StoppingTask();
        plugin.isGameStarted = false;
        isFirstZChoosed = false;
        plugin.isWaitingRoom = true;

        plugin.inGamePlayers.clear();
        plugin.inGameTeamHuman.clear();
        plugin.inGameTeamZombie.clear();
        plugin.inGameFirstZombies.clear();
        tracker.clear();


        totalTime = (int) (plugin.GAME_WAITINGTIME / 1000);
        timeLeft = (int) (plugin.GAME_WAITINGTIME / 1000);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            float progress = (float) timeLeft / totalTime;

            long adventurePlayers = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getGameMode() != GameMode.CREATIVE)
                    .count();

            if (adventurePlayers <= 1) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ScoreBoardGestion(player, "-");
                    TABGestion(player);
                    WaitingRoomLogic(player);
                    player.setHealth(player.getMaxHealth());
                    player.setLevel(0);
                    player.setExp(0);
                }
                plugin.isThereIsMoreThan2Players = false;
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                WaitingRoomLogic(player);
                TABGestion(player);
                ScoreBoardGestion(player, String.valueOf(timeLeft));
                player.setLevel(timeLeft);
                player.setExp(progress);
                if (timeLeft <= 5 && timeLeft > 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_COPPER_BULB_TURN_ON, 10f, 1f);
                }
            }

            if (timeLeft <= 5 && timeLeft > 0) {
                String secondeText = (timeLeft == 1) ? "seconde." : "secondes.";
                Component startingMessage = MiniMessage.miniMessage().deserialize(
                        "<!italic><gradient:" + plugin.generalTextChatColor + ":#E5A45E>Lancement de la partie dans <color:#8BB8BA>" +
                                timeLeft +
                                "</color> " + secondeText + "</gradient>"
                ).decoration(TextDecoration.BOLD, false);
                Component prefix = Component.text(plugin.infectedMessage)
                        .decorate(TextDecoration.BOLD);
                Component finalMessage = prefix.append(startingMessage);
                Bukkit.broadcast(finalMessage);
            }

            if (timeLeft <= 0) {
                plugin.isWaitingRoom = false;
                chooseRandomMap();
                List<Player> playersToTeleport = new ArrayList<>(Bukkit.getOnlinePlayers());
                Location location = plugin.mapSelection.get(plugin.choosenMap);

                Bukkit.broadcastMessage(ChatColor.of(plugin.generalTextChatColor) + "La map qui à été choisie est " + ChatColor.of("#7d3825") + plugin.choosenMap + ChatColor.of(plugin.generalTextChatColor) + "!");

                if (location == null) {
                    Bukkit.getLogger().warning("[InfectedV2] Aucune localisation trouvée pour la carte choisie !");
                    return;
                }
                location.getWorld().getChunkAt(location).load();

                new BukkitRunnable() {
                    int index = 0;

                    @Override
                    public void run() {
                        if (index >= playersToTeleport.size()) {
                            cancel();
                            Bukkit.broadcastMessage(ChatColor.of(plugin.generalTextChatColor) + "Tous les joueurs ont été téléportés ! Les zombies arrivent dans " + plugin.GAME_CHOOSINGZOMBIES + " secondes.");

                            GameLogic();
                            return;
                        }

                        Player player = playersToTeleport.get(index);

                        if (player != null && player.isOnline()) {
                            player.teleport(location);
                            player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSE, 1f, 0f);
                        }

                        double progress = ((index + 1) / (double) playersToTeleport.size()) * 100;
                        String progressText = String.format("Téléportation des joueurs... (%.1f%%)", progress);
                        ScoreBoardGestion(player, String.valueOf(progress));

                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.setInvulnerable(false);
                            p.sendActionBar(Component.text(progressText).color(TextColor.fromHexString("#2584a5")));
                        }

                        index++;
                    }
                }.runTaskTimer(plugin, 0L, 4L);
                return;
            }

            timeLeft--;
        }, 20L, 20L);
    }

    public void StoppingTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (regenZombieTask != -1) {
            Bukkit.getScheduler().cancelTask(regenZombieTask);
            regenZombieTask = -1;
        }
    }

    public void ZombiesSelection() {
        int zombieCount;
        int playerInGameCount = plugin.inGamePlayers.size();

        if (playerInGameCount <= 11) {
            zombieCount = 1;
        } else if (playerInGameCount <= 19) {
            if (plugin.inGameTeamZombie.size() == 1) {
                zombieCount = 1;
            } else {
                zombieCount = 2;
            }
        } else {
            if (plugin.inGameTeamZombie.size() < Math.min(10, ((playerInGameCount - 10) / 10) + 2)) {
                zombieCount = Math.min(10, ((playerInGameCount - 10) / 10) + 2) - plugin.inGameTeamZombie.size();
            } else {
                zombieCount = Math.min(10, ((playerInGameCount - 10) / 10) + 2);
            }
        }

        List<Player> shuffled = new ArrayList<>(plugin.inGameTeamHuman);
        Collections.shuffle(shuffled);


        List<Player> chosenZombies = shuffled.subList(0, Math.min(zombieCount, shuffled.size()));
        List<String> zombieNames = new ArrayList<>();

        for (Player zombie : chosenZombies) {
            sqlGestion.FirstZombie(zombie);
            plugin.inGameFirstZombies.add(zombie.getUniqueId());
            InfectionLogic(zombie);
            plugin.inGameTeamHuman.remove(zombie);
            zombieNames.add(ChatColor.DARK_GREEN + zombie.getName());
        }
        String namesList = String.join(ChatColor.of(plugin.generalTextChatColor) + ", ", zombieNames);
        String startingText = (zombieCount == 1) ? "Le joueur choisis " : "Les joueurs choisis ";
        String infected = (zombieCount == 1) ? ChatColor.DARK_GREEN + "Infecté " + ChatColor.of(plugin.generalTextChatColor) + "est " : ChatColor.DARK_GREEN + "Infectés " + ChatColor.of(plugin.generalTextChatColor) + "sont ";
        if (!isFirstZChoosed) {
            Bukkit.broadcastMessage(plugin.infectedMessage + ChatColor.of(plugin.generalTextChatColor) + startingText + "en tant que premier " + infected + namesList + ChatColor.of(plugin.generalTextChatColor) + ".");
            isFirstZChoosed = true;
        } else {
            Bukkit.broadcastMessage(plugin.infectedMessage + ChatColor.of(plugin.generalTextChatColor) + startingText + "en tant que remplacant " + infected + namesList + ChatColor.of(plugin.generalTextChatColor) + ".");
        }
    }

    public void InfectionLogic(Player player) {
        if (!plugin.inGameTeamZombie.contains(player)) {
            plugin.inGameTeamZombie.add(player);
        }
        plugin.inGameTeamHuman.remove(player);
        player.setMaxHealth(plugin.inGameTeamSkeleton.contains(player) ? 40 : plugin.inGameTeamPigman.contains(player) ? 20 :  28);
        player.setHealth(player.getMaxHealth());

        RandomTargetGeneration(player);
        InventoryGestion(player);

        MobDisguise zombieDisguise = new MobDisguise(plugin.inGameTeamSkeleton.contains(player) ? DisguiseType.SKELETON : plugin.inGameTeamPigman.contains(player) ? DisguiseType.ZOMBIFIED_PIGLIN :  DisguiseType.ZOMBIE);

        zombieDisguise.setHearSelfDisguise(true);
        zombieDisguise.setViewSelfDisguise(false);
        LivingWatcher watcher = (LivingWatcher) zombieDisguise.getWatcher();

        watcher.setCustomName(plugin.inGameTeamSkeleton.contains(player) ? ChatColor.of(skeletonColor) + "[Squelette] " + ChatColor.DARK_GREEN + player.getName() : plugin.inGameTeamPigman.contains(player) ? ChatColor.of(pigmanColor) + "[Pig Zombie] " + ChatColor.DARK_GREEN + player.getName() : ChatColor.of(zombieColor) + "[Zombie] " + ChatColor.DARK_GREEN + player.getName());

        watcher.setCustomNameVisible(true);

        DisguiseAPI.disguiseToAll(player, zombieDisguise);

        TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
        NameTagManager tagPlayer = TabAPI.getInstance().getNameTagManager();
        TabListFormatManager tabListPlayer = TabAPI.getInstance().getTabListFormatManager();

        tagPlayer.setPrefix(tabPlayer, plugin.inGameTeamSkeleton.contains(player) ? ChatColor.of(skeletonColor) + "[Squelette] " + ChatColor.DARK_GREEN + "" : plugin.inGameTeamPigman.contains(player) ? ChatColor.of(pigmanColor) + "[Pig Zombie] " + ChatColor.DARK_GREEN + "" : ChatColor.of(zombieColor) + "[Zombie] " + ChatColor.DARK_GREEN + "");
        tabListPlayer.setPrefix(tabPlayer, plugin.inGameTeamSkeleton.contains(player) ? ChatColor.of(skeletonColor) + "[Squelette] " + ChatColor.DARK_GREEN + "" : plugin.inGameTeamPigman.contains(player) ? ChatColor.of(pigmanColor) + "[Pig Zombie] " + ChatColor.DARK_GREEN + "" : ChatColor.of(zombieColor) + "[Zombie] " + ChatColor.DARK_GREEN + "");

        if (!asCarnageQuest.contains(player)) {
            if (plugin.inGameFirstZombies.contains(player.getUniqueId())) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage(ChatColor.of(guerisonQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(guerisonQuest) + "---" +
                            ChatColor.of(guerisonQuest) + "\n❤ Quête Guerison \nTue des humains pour revenir à la vie !\n" +
                            ChatColor.of(guerisonQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(guerisonQuest) + "---");
                }, 100L);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage(ChatColor.of(carnageQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(carnageQuest) + "---" +
                            ChatColor.of(carnageQuest) + "\n❤ Quête Carnage \nTue des humains pour gagner des avantages !\n" +
                            ChatColor.of(carnageQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(carnageQuest) + "---");
                }, 100L);
            }
        } else {
            if (plugin.inGameTeamPigman.contains(player)) {
                Bukkit.broadcastMessage(ChatColor.of(carnageQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(carnageQuest) + "---" +
                        ChatColor.of(carnageQuest) + "\n❤ Quête Carnage" + ChatColor.of("#2584a5") + " (+" + ptsQuete + " points) \n" + ChatColor.of(carnageQuest) + player.getName() +" à fait un carnage, il devient un Pig Zombie !\n" +
                        ChatColor.of(carnageQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(carnageQuest) + "---");
            } else if (plugin.inGameTeamSkeleton.contains(player)) {
                Bukkit.broadcastMessage(ChatColor.of(carnageQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(carnageQuest) + "---" +
                        ChatColor.of(carnageQuest) + "\n❤ Quête Carnage" + ChatColor.of("#2584a5") + " (+" + ptsQuete + " points) \n" + ChatColor.of(carnageQuest) + player.getName() +" à fait un carnage, il devient un Squelette !\n" +
                        ChatColor.of(carnageQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(carnageQuest) + "---");
            }
        }
    }

    public void HumanLogic(Player player) {
        if (!plugin.inGameTeamHuman.contains(player)) {
            plugin.inGameTeamHuman.add(player);
        }
        plugin.inGameFirstZombies.remove(player.getUniqueId());
        plugin.inGameTeamZombie.remove(player);
        DisguiseAPI.undisguiseToAll(player);

        player.setMaxHealth(30);
        player.setHealth(player.getMaxHealth());

        InventoryGestion(player);

        int i = sqlGestion.getChoosenChestplate(player);
        ChestPlateChoosen(player, i);

        TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
        NameTagManager tagPlayer = TabAPI.getInstance().getNameTagManager();
        TabListFormatManager tabListPlayer = TabAPI.getInstance().getTabListFormatManager();

        tagPlayer.setPrefix(tabPlayer, ChatColor.of(humanColor) + "[Humain] " + ChatColor.DARK_AQUA + "");
        tabListPlayer.setPrefix(tabPlayer, ChatColor.of(humanColor) + "[Humain] " + ChatColor.DARK_AQUA + "");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (timeLeft <= totalTime - plugin.GAME_CHOOSINGZOMBIES) {
                player.sendMessage(ChatColor.of(survivantQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(survivantQuest) + "---" +
                        ChatColor.of(survivantQuest) + "\n❤ Quête Survivant \nTue des zombies pour gagner des points de vie !\n" +
                        ChatColor.of(survivantQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(survivantQuest) + "---");
            }
        }, 200L);
    }

    public void WaitingRoomLogic(Player player) {
        plugin.inGamePlayers.remove(player);
        plugin.inGameTeamHuman.remove(player);
        plugin.inGameTeamZombie.remove(player);
        DisguiseAPI.undisguiseToAll(player);

        player.setMaxHealth(20);
        player.setHealth(player.getMaxHealth());

        TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
        NameTagManager tagPlayer = TabAPI.getInstance().getNameTagManager();
        TabListFormatManager tabListPlayer = TabAPI.getInstance().getTabListFormatManager();

        tagPlayer.setPrefix(tabPlayer, ChatColor.GRAY + "");
        tabListPlayer.setPrefix(tabPlayer, ChatColor.GRAY + "");
    }

    public void DeathCosmetic(Player player, String titleMessage) {

        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) return;

        Firework fw = world.spawn(location, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        FireworkEffect effect = null;

        if (plugin.inGameTeamHuman.contains(player)) {
            //DeathZombieHead(player);
            player.sendTitle("", titleMessage, 10, 70, 20);
            effect = FireworkEffect.builder()
                    .withColor(Color.fromRGB(94, 153, 109))
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .flicker(true)
                    .trail(false)
                    .build();
        } else if (plugin.inGameTeamZombie.contains(player)) {
            effect = FireworkEffect.builder()
                    .withColor(Color.fromRGB(224, 36, 26))
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .flicker(true)
                    .trail(false)
                    .build();
        }

        meta.addEffect(effect);
        meta.setPower(0);
        fw.setFireworkMeta(meta);
        Bukkit.getScheduler().runTaskLater(plugin, fw::detonate, 7L);
    }

    public void DeathZombieHead(Player player) {
        java.awt.Color[][] pixel = new java.awt.Color[8][8];

        try {
            InputStream stream = plugin.getResource("zombie.png");
            if (stream == null) {
                plugin.getLogger().severe("Impossible de trouver zombie.png dans les ressources !");
                return;
            }
            BufferedImage skin = ImageIO.read(stream);
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int rgb = skin.getRGB(8 + x, 8 + y);
                    pixel[y][x] = new java.awt.Color(rgb, true);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "Impossible de charger la texture du zombie !");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                builder.append(ChatColor.of(pixel[y][x]) + "█");
            }
            builder.append("\n");
        }
        player.sendMessage(builder.toString());
    }

    public void InGameDamage(Player victim, Player attacker, EntityDamageByEntityEvent event) {
        int inGamePlayers = plugin.inGameTeamZombie.size() + plugin.inGameTeamHuman.size();
        int inGameZombie = plugin.inGameTeamZombie.size();
        float zombiePourcent = (float) inGameZombie / inGamePlayers;

        UUID victimId = victim.getUniqueId();
        UUID damagerId = attacker.getUniqueId();

        boolean attackerIsZombie = false;
        double damage = 2.0;

        if (plugin.inGameTeamZombie.contains(attacker)) attackerIsZombie = true;

        int nbPlayersInGame;
        if (inGamePlayers <= 14) nbPlayersInGame = 1;
        else if (inGamePlayers <= 24) nbPlayersInGame = 2;
        else nbPlayersInGame = 3;

        damage = damageInGame(nbPlayersInGame, damage, attackerIsZombie, zombiePourcent);

        damage *= 2;

        if (event.isCritical() && plugin.inGameTeamHuman.contains(attacker)) {
            damage *= 1.3;
            attacker.sendTitle("", ChatColor.of("#bfa345") + "" + ChatColor.BOLD + "⚡", 10, 30, 20);
        }
        event.setDamage(damage);

        Map<UUID, DamageInfo> attackers = damageLog.computeIfAbsent(victimId, k -> new HashMap<>());

        DamageInfo info = attackers.get(damagerId);
        if (info == null) {
            info = new DamageInfo();
            attackers.put(damagerId, info);
        }

        info.damage += event.getFinalDamage();
        info.lastHitTime = System.currentTimeMillis();

        if (info.expiryTask != null) {
            info.expiryTask.cancel();
        }

        info.expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            attackers.remove(damagerId);
        }, 20L * 30);

        if (resetTimers.containsKey(victimId)) {
            resetTimers.get(victimId).cancel();
        }

        resetTimers.put(victimId, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            damageLog.remove(victimId);
            resetTimers.remove(victimId);
        }, 20L * 30));

        Vector kb = victim.getLocation().toVector().subtract(attacker.getLocation().toVector());
        double lengthSquared = kb.lengthSquared();

        if (lengthSquared > 0) {
            kb.normalize().multiply(kbHorizontal);
            kb.setY(kbVertical);

            if (Double.isFinite(kb.getX()) && Double.isFinite(kb.getY()) && Double.isFinite(kb.getZ())) {
                victim.setVelocity(kb);
            }
        }
    }

    public double damageInGame(int playerGroup, double damage, boolean attackerIsZombie, float zombiePourcent) {

        switch (playerGroup) {
            case 1 -> { // <=14 joueurs
                if (attackerIsZombie) {
                    if (zombiePourcent < 0.25) damage = 7.0;
                    else if (zombiePourcent < 0.50) damage = 5.9;
                    else if (zombiePourcent < 0.67) damage = 5.2;
                    else if (zombiePourcent < 0.75) damage = 4.7;
                    else damage = 4.1;
                } else {
                    if (zombiePourcent < 0.50) damage = 3.5;
                    else if (zombiePourcent < 0.67) damage = 4.0;
                    else damage = 5.0;
                }
            }
            case 2 -> { // 15-24 joueurs
                if (attackerIsZombie) {
                    if (zombiePourcent < 0.25) damage = 7.0;
                    else if (zombiePourcent < 0.50) damage = 5.9;
                    else if (zombiePourcent < 0.67) damage = 5.2;
                    else if (zombiePourcent < 0.75) damage = 4.7;
                    else damage = 4.1;
                } else {
                    if (zombiePourcent < 0.50) damage = 3.5;
                    else if (zombiePourcent < 0.67) damage = 4.0;
                    else if (zombiePourcent < 0.75) damage = 5.0;
                    else damage = 7.5;
                }
            }
            case 3 -> { // >=25 joueurs
                if (attackerIsZombie) {
                    if (zombiePourcent < 0.25) damage = 7.0;
                    else if (zombiePourcent < 0.50) damage = 5.9;
                    else if (zombiePourcent < 0.67) damage = 5.2;
                    else if (zombiePourcent < 0.75) damage = 4.7;
                    else if (zombiePourcent < 0.90) damage = 4.1;
                    else damage = 3.8;
                } else {
                    if (zombiePourcent < 0.50) damage = 3.5;
                    else if (zombiePourcent < 0.67) damage = 4.0;
                    else if (zombiePourcent < 0.75) damage = 5.0;
                    else if (zombiePourcent < 0.90) damage = 7.5;
                    else damage = 14.0;
                }
            }
        }

        return damage;
    }

    public void handlePlayerDeath(Player player) {
        UUID victimId = player.getUniqueId();
        String victimMessage = plugin.getColoredName(player);

        List<String> attackerNames = new ArrayList<>();

        // Récupère les attaquants connus du damageLog
        if (damageLog.containsKey(victimId)) {
            Map<UUID, DamageInfo> attackers = damageLog.get(victimId);

            for (UUID attackerId : attackers.keySet()) {
                Player attacker = Bukkit.getPlayer(attackerId);

                sqlGestion.Kill(attacker);
                int kills = plugin.inGamePlayerKills.getOrDefault(attackerId, 0);
                kills += 1;
                plugin.inGamePlayerKills.put(attackerId, kills);

                if (plugin.inGameTeamZombie.contains(attacker) && !plugin.inGameFirstZombies.contains(attackerId)) {
                    int killsZ = plugin.inGameZombieKills.getOrDefault(attackerId, 0);
                    killsZ += 1;
                    plugin.inGameZombieKills.put(attackerId, killsZ);

                    if (killsZ >= killsCarnage && !asCarnageQuest.contains(attacker)) {
                        asCarnageQuest.add(attacker);
                        killsCarnage += 1;

                        World world = attacker.getWorld();

                        int carnageType = random.nextInt(2);
                        if (carnageType == 0) {
                            plugin.inGameTeamPigman.add(attacker);
                        } else if (carnageType == 1) {
                            plugin.inGameTeamSkeleton.add(attacker);
                        }

                        int points = plugin.inGamePlayerPoints.getOrDefault(attackerId, 0);
                        points += ptsQuete;
                        PointsActionBar(attacker, ptsQuete, "Carnage");
                        plugin.inGamePlayerPoints.put(attackerId, points);

                        InfectionLogic(attacker);
                        world.playSound(attacker.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.0f);
                    }
                }

                if (plugin.inGameTeamHuman.contains(attacker)) {
                    int killsH = plugin.inGameHumanKills.getOrDefault(attackerId, 0);
                    killsH += 1;
                    plugin.inGameHumanKills.put(attackerId, killsH);

                    if (killsH >= killsSurvivant && !asSurvivantQuest.contains(attacker)) {
                        asSurvivantQuest.add(attacker);
                        killsSurvivant += 1;
                        attacker.setMaxHealth(40);
                        attacker.setHealth(attacker.getMaxHealth());
                        Location location = attacker.getLocation();
                        attacker.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);

                        int points = plugin.inGamePlayerPoints.getOrDefault(attackerId, 0);
                        points += ptsQuete;
                        PointsActionBar(attacker, ptsQuete, "Survivant");
                        plugin.inGamePlayerPoints.put(attackerId, points);

                        attacker.sendMessage(ChatColor.of(survivantQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(survivantQuest) + "---" +
                                ChatColor.of(survivantQuest) + "\n❤ Quête Survivant " + ChatColor.of("#2584a5") + "(+" + ptsQuete + " points) \n" + ChatColor.of(survivantQuest) +"Vous avez réussie votre quête, vous gagnez 5" + ChatColor.of("#a35252") + "❤" + ChatColor.of(survivantQuest) + " !\n" +
                                ChatColor.of(survivantQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(survivantQuest) + "---");
                    }
                }


                if (attacker != null) {
                    attackerNames.add(plugin.getColoredName(attacker));
                    int points = plugin.inGamePlayerPoints.getOrDefault(attackerId, 0);

                    if (plugin.inGameTeamZombie.contains(attacker)) {
                        points += ptsZKills;
                        PointsActionBar(attacker, ptsZKills, "Z Kill");
                    }

                    if (kills >= killsGuerison && plugin.inGameFirstZombies.contains(attackerId) && !asGuerisonQuest.contains(attacker) && timeLeft >= timeGuerisonExpire) {
                        asGuerisonQuest.add(attacker);
                        HumanLogic(attacker);
                        sqlGestion.Guerison(attacker);

                        attacker.sendMessage(ChatColor.of(guerisonQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(guerisonQuest) + "---" +
                                ChatColor.of(guerisonQuest) + "\n❤ Quête Guerison \nVous avez réussie votre quête, vous redevenez humain !\n" +
                                ChatColor.of(guerisonQuest) + "---" + ChatColor.of("#FFFFFF") + "------------------------------" + ChatColor.of(guerisonQuest) + "---");

                        points += ptsQuete;
                        PointsActionBar(attacker, ptsQuete, "Guerison");

                        Location location = attacker.getLocation();
                        World world = location.getWorld();

                        world.playSound(location, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 0f);


                        Firework fw = world.spawn(location, Firework.class);
                        FireworkMeta meta = fw.getFireworkMeta();
                        FireworkEffect effect;

                        effect = FireworkEffect.builder()
                                .withColor(Color.fromRGB(193, 124, 60))
                                .with(FireworkEffect.Type.BALL_LARGE)
                                .flicker(true)
                                .trail(false)
                                .build();
                        meta.addEffect(effect);
                        meta.setPower(0);
                        fw.setFireworkMeta(meta);
                        Bukkit.getScheduler().runTaskLater(plugin, fw::detonate, 7L);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            Bukkit.broadcastMessage(plugin.infectedMessage + ChatColor.of("#dfcc48") + attacker.getName() + ChatColor.of(plugin.generalTextChatColor) + " est redevenue un " + ChatColor.DARK_AQUA + "humain " + ChatColor.of(plugin.generalTextChatColor) + "en ayant fait " + killsGuerison + " kills !");
                        }, 20L);
                    }

                    plugin.inGamePlayerPoints.put(attackerId, points);
                } else {
                    String offlineName = Bukkit.getOfflinePlayer(attackerId).getName();
                    if (offlineName != null) attackerNames.add(ChatColor.GRAY + offlineName);
                }
            }
        }

        // Si la event est un joueur direct et qu'on n'a rien dans damageLog
        EntityDamageEvent cause = player.getLastDamageCause();
        if (attackerNames.isEmpty() && cause instanceof EntityDamageByEntityEvent ede) {
            Entity damager = ede.getDamager();
            if (damager instanceof Player killer) {
                attackerNames.add(plugin.getColoredName(killer));
            }
        }

        if (attackerNames.size() > 3) {
            attackerNames = attackerNames.subList(0, 3);
        }

        ChatColor textColor = ChatColor.of(plugin.generalTextChatColor);
        String attackersMsg = "";

        if (attackerNames.size() == 1) {
            attackersMsg = attackerNames.get(0);
        } else if (attackerNames.size() == 2) {
            attackersMsg = attackerNames.get(0) + textColor + " et " + attackerNames.get(1);
        } else if (attackerNames.size() == 3) {
            attackersMsg = attackerNames.get(0) + textColor + ", "
                    + attackerNames.get(1) + textColor + " et "
                    + attackerNames.get(2);
        }

        //Title Message
        String titleMessage;
        if (!attackerNames.isEmpty()) {
            titleMessage = ChatColor.GREEN + "Dévoré par " + attackersMsg + ChatColor.GREEN + ".";
        } else {
            titleMessage = ChatColor.GREEN + "Vous avez pourris.";
        }

        DeathCosmetic(player, titleMessage);

        //Chat Message

        String chatMessage;
        if (!attackerNames.isEmpty()) {
            chatMessage = victimMessage + textColor + " a été tué par " + attackersMsg + textColor + ".";
        } else {
            chatMessage = victimMessage + textColor + " est mort de cause naturelle.";
        }

        Bukkit.broadcastMessage(chatMessage);

        // Nettoyage après la mort
        damageLog.remove(victimId);
        if (resetTimers.containsKey(victimId)) {
            resetTimers.get(victimId).cancel();
            resetTimers.remove(victimId);
        }
    }

    public String chooseRandomMap() {
        List<String> availableMaps = new ArrayList<>(plugin.mapSelection.keySet());
        availableMaps.remove("Spawn");

        if (availableMaps.isEmpty()) {
            Bukkit.getLogger().warning("Aucune map disponible !");
            return "";
        }

        Random random = new Random();
        plugin.choosenMap = availableMaps.get(random.nextInt(availableMaps.size()));

        return plugin.choosenMap;
    }

    public void HumanPointAttribute(Player player) {
        int points = 0;
        String type = "n";

        if (timeLeft <= 255 && timeLeft >= 45 && timeLeft % 15 == 0) {
            points = plugin.inGamePlayerPoints.getOrDefault(player.getUniqueId(), 0);
            points += ptsSurvival;
            type = "Survie";
        }

        if (timeLeft == 0) {
            points = plugin.inGamePlayerPoints.getOrDefault(player.getUniqueId(), 0);
            points += ptsWin;
            type = "Win";
        }

        if (points > 0) {
            plugin.inGamePlayerPoints.put(player.getUniqueId(), points);
            switch (type) {
                case "Survie":
                    PointsActionBar(player, ptsSurvival, type);
                    break;
                case "Win":
                    PointsActionBar(player, ptsWin, type);
                    break;
            }

        }
    }

    public void PointsActionBar(Player player, int points, String type) {
        player.sendActionBar(ChatColor.of("#2584a5") + "+" + points + " points (" + type + ")");
    }

    public void TrackerActionBar(Player trackerPlayer, Player targetPlayer) {
        if (trackerPlayer != null && targetPlayer != null) {
            Location trackerLoc = trackerPlayer.getLocation();
            Location targetLoc = targetPlayer.getLocation();
            double dx = targetLoc.getX() - trackerLoc.getX();
            double dz = targetLoc.getZ() - trackerLoc.getZ();

            double angleToTarget = Math.toDegrees(Math.atan2(dz, dx));

            double yaw = trackerLoc.getYaw();

            double relativeAngle = angleToTarget - yaw;

            relativeAngle = (relativeAngle + 360) % 360;

            String direction = getDirectionArrow(relativeAngle);
            double distance = trackerLoc.distance(targetLoc);

            trackerPlayer.sendActionBar(
                    ChatColor.of("#67a0a5") + direction +
                            ChatColor.GRAY + " Cible : " +
                            ChatColor.of("#67a0a5") + targetPlayer.getName() +
                            ChatColor.WHITE + " | " + Math.round(distance) + " blocs"
            );
        }
    }

    private String getDirectionArrow(double angle) {
        if (angle >= 337.5 || angle < 22.5) return "⬅";      // Devant
        if (angle >= 22.5 && angle < 67.5) return "↖";       // Devant-droite
        if (angle >= 67.5 && angle < 112.5) return "⬆";      // Droite
        if (angle >= 112.5 && angle < 157.5) return "↗";     // Derrière-droite
        if (angle >= 157.5 && angle < 202.5) return "➡";     // Derrière
        if (angle >= 202.5 && angle < 247.5) return "↘";     // Derrière-gauche
        if (angle >= 247.5 && angle < 292.5) return "⬇";     // Gauche
        if (angle >= 292.5 && angle < 337.5) return "↙";     // Devant-gauche
        return "?";
    }

    public void InventoryGestion(Player player) {
        player.getAttribute(Attribute.ARMOR).setBaseValue(0.0);

        PlayerInventory inventory = player.getInventory();
        inventory.clear();

        if (plugin.inGameTeamHuman.contains(player)) {

            for (int i = 0; i < 7; i++) inventory.setItem(i, new ItemStack(Material.STICK));

            ItemStack bow = new ItemStack(Material.BOW);
            ItemMeta bowMeta = bow.getItemMeta();
            bowMeta.setUnbreakable(true);
            bowMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            bow.setItemMeta(bowMeta);
            inventory.setItem(7, bow);

            for (int i = 9; i <= 17; i++) {
                ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
                LeatherArmorMeta meta = (LeatherArmorMeta) chestplate.getItemMeta();
                meta.setColor(ColorFromHex(chtpltColors[i - 9]));
                meta.setDisplayName(ChatColor.of(chtpltColors[i - 9]) + "" + ChatColor.BOLD + "Plastron N°" + (i - 8));
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                chestplate.setItemMeta(meta);
                inventory.setItem(i, chestplate);
            }
        } else if (plugin.inGameTeamZombie.contains(player)) {
            if (plugin.inGameTeamHuman.size() <= 0) {
                inventory.clear();
                return;
            }
            InventoryZombie(player);
        }

        player.updateInventory();
    }

    public void RandomTargetGeneration(Player player) {
        if (plugin.inGameTeamHuman.size() <= 0) {
            Inventory inventory = player.getInventory();
            inventory.clear();
            return;
        }
        tracker.remove(player);
        Player target = plugin.inGameTeamHuman.get(random.nextInt(plugin.inGameTeamHuman.size()));
        tracker.put(player, target);
    }

    public void InventoryZombie(Player player) {
        PlayerInventory inventory = player.getInventory();
        if (plugin.inGameTeamHuman.size() <= 0) return;

        Player target = tracker.getOrDefault(player, plugin.inGameTeamHuman.get(random.nextInt(plugin.inGameTeamHuman.size())));
        ItemStack head = skullUtils.getSkull(String.valueOf(target.getUniqueId()));
        ItemMeta meta = head.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + target.getName());
        head.setItemMeta(meta);
        inventory.setItem(4, head);

        if (plugin.inGameTeamPigman.contains(player)) {
            for (int i = 0; i <= 8; i++) {
                if (i != 4) {
                    inventory.setItem(i, new ItemStack(Material.STICK));
                }
            }
        } else if (plugin.inGameTeamSkeleton.contains(player)) {

            for (int i = 0; i <= 7; i++) {
                if (i != 4) {
                    inventory.setItem(i, new ItemStack(Material.BOW));
                }
            }
        }
    }

    public Color ColorFromHex(String hex) {
        int r = Integer.valueOf(hex.substring(1, 3), 16);
        int g = Integer.valueOf(hex.substring(3, 5), 16);
        int b = Integer.valueOf(hex.substring(5, 7), 16);
        return Color.fromRGB(r, g, b);
    }

    public void ChestPlateChoosen(Player player, int slot) {
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) chestplate.getItemMeta();

        meta.setColor(ColorFromHex(chtpltColors[slot - 9]));
        meta.setDisplayName(ChatColor.of(chtpltColors[slot - 9]) + "" + ChatColor.BOLD + "Plastron N°" + (slot - 8));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        chestplate.setItemMeta(meta);

        player.getInventory().setChestplate(chestplate);
    }

    public void ScoreBoardGestion(Player player, String progressBar) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective objective = board.registerNewObjective(
                "infecte",
                "dummy",
                Component.text("   " + plugin.ipServer + "   ").color(TextColor.fromHexString("#7bcf32")).decoration(TextDecoration.BOLD, true)
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

        Component line10 = Component.text("");
        Component line9 = Component.text("");
        Component line8 = Component.text("");
        Component line7 = Component.text("");
        Component line6 = Component.text("");
        Component line5 = Component.text("");
        Component line4 = Component.text("");
        Component line3 = Component.text("");
        Component line2 = Component.text("");
        Component line1 = Component.text("");

        if (plugin.isGameStarted) {
            Component teamType = Component.text(plugin.inGameTeamZombie.contains(player) ? "Zombie" : plugin.inGameTeamHuman.contains(player) ? "Humain" : "Spectateur").color(TextColor.fromHexString(plugin.inGameTeamZombie.contains(player) ? zombieColor : plugin.inGameTeamHuman.contains(player) ? humanColor : "#AAAAAA"));
            Component logoType = Component.text(plugin.inGameTeamZombie.contains(player) || plugin.inGameTeamHuman.contains(player) ? "⦁ " : "\uD83D\uDC7B ").color(TextColor.fromHexString(plugin.inGameTeamZombie.contains(player) ? zombieheart : plugin.inGameTeamHuman.contains(player) ? humanheart : "#FFFFFF"));

            line9 = logoType.append(teamType);
            line7 = Component.text(plugin.inGameTeamHuman.size() > 1 ? "Humains: " : "Humain: ").append(Component.text(plugin.inGameTeamHuman.size(), TextColor.fromHexString("#eaa43d")));
            line6 = Component.text(plugin.inGameTeamZombie.size() > 1 ? "Zombies: " : "Zombie: ").append(Component.text(plugin.inGameTeamZombie.size(), TextColor.fromHexString("#eaa43d")));
            line4 = Component.text("Points: ").append(Component.text("+" + plugin.inGamePlayerPoints.getOrDefault(player.getUniqueId(), 0), TextColor.fromHexString(humanheart)));

            int kills = plugin.inGamePlayerKills.getOrDefault(player.getUniqueId(), 0);
            if (kills > 0)
                line3 = Component.text("Kills: ").append(Component.text(kills, TextColor.fromHexString("#eaa43d")));


            Component barComponent = Component.text("[", TextColor.fromHexString("#FFFFFF"));

            if (plugin.inGameTeamHuman.contains(player)) {
                if (asSurvivantQuest.contains(player)) {
                    barComponent = barComponent.append(Component.text("✔", TextColor.fromHexString("#81a365")));
                } else {
                    int killsH = plugin.inGameHumanKills.getOrDefault(player.getUniqueId(), 0);
                    double progress = (double) killsH / killsSurvivant;
                    double progression = Math.round(progress * 100);
                    barComponent = barComponent.append(Component.text(progression + "%", TextColor.fromHexString("#eaa43d")));
                }
            } else if (plugin.inGameTeamZombie.contains(player) && !plugin.inGameFirstZombies.contains(player.getUniqueId())) {
                if (asCarnageQuest.contains(player)) {
                    barComponent = barComponent.append(Component.text("✔", TextColor.fromHexString("#81a365")));
                } else {
                    int killsZ = plugin.inGameZombieKills.getOrDefault(player.getUniqueId(), 0);
                    double progress = (double) killsZ / killsCarnage;
                    double progression = Math.round(progress * 100);
                    barComponent = barComponent.append(Component.text(progression + "%", TextColor.fromHexString("#eaa43d")));
                }
            } else if (plugin.inGameFirstZombies.contains(player.getUniqueId())) {
                if (timeLeft >= timeGuerisonExpire) {
                    if (asGuerisonQuest.contains(player)) {
                        barComponent = barComponent.append(Component.text("✔", TextColor.fromHexString("#81a365")));
                    } else {
                        double progress = (double) kills / killsGuerison;
                        double progression = Math.round(progress * 100);
                        barComponent = barComponent.append(Component.text(progression + "%", TextColor.fromHexString("#eaa43d")));
                    }
                } else barComponent = barComponent.append(Component.text("✕", TextColor.fromHexString("#a35252")));
            }

            barComponent = barComponent.append(Component.text("]", TextColor.fromHexString("#FFFFFF")));


            line1 = plugin.inGameTeamHuman.contains(player) ? Component.text("❤ Survivant", TextColor.fromHexString(humanheart)).append(Component.text(": ", TextColor.fromHexString("#FFFFFF")).append(barComponent))
                    : plugin.inGameTeamZombie.contains(player) && !plugin.inGameFirstZombies.contains(player.getUniqueId()) ? Component.text("❤ Carnage", TextColor.fromHexString("#97365b")).append(Component.text(": ", TextColor.fromHexString("#FFFFFF")).append(barComponent))
                    : Component.text("❤ Guerison", TextColor.fromHexString("#d7b225")).append(Component.text(": ", TextColor.fromHexString("#FFFFFF")).append(barComponent));

            Score score4 = objective.getScore("4");
            score4.customName(line4);
            score4.setScore(4);

            Score score3 = objective.getScore("3");
            score3.customName(line3);
            score3.setScore(3);

            Score score2 = objective.getScore("2");
            score2.customName(line2);
            score2.setScore(2);

            Score score1 = objective.getScore("1");
            score1.customName(line1);
            score1.setScore(1);
        } else if (plugin.isWaitingRoom) {
            line9 = Component.text("Jeu", TextColor.fromHexString("#AAAAAA")).append(Component.text(ChatColor.WHITE + ": Infecté"));
            line8 = Component.text("Joueurs", TextColor.fromHexString("#AAAAAA")).append(Component.text(ChatColor.WHITE + ": ").append(Component.text(Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers(), TextColor.fromHexString(zombieheart))));
            line6 = Component.text("Début", TextColor.fromHexString("#AAAAAA")).append(Component.text(": ", TextColor.fromHexString("#FFFFFF"))).append(Component.text(progressBar, TextColor.fromHexString("#8BB8BA"))).append(Component.text(" secondes", TextColor.fromHexString("#FFFFFF")));
        } else {
            line9 = Component.text("Jeu", TextColor.fromHexString("#AAAAAA")).append(Component.text(ChatColor.WHITE + ": Infecté"));
            line8 = Component.text("Joueurs", TextColor.fromHexString("#AAAAAA")).append(Component.text(ChatColor.WHITE + ": ").append(Component.text(Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers(), TextColor.fromHexString(zombieheart))));
            line6 = Component.text(String.format("Téléportation... (%.1f%%)", Double.parseDouble(progressBar)), TextColor.fromHexString("#AAAAAA"));
        }

        Score score10 = objective.getScore("10");
        score10.customName(line10);
        score10.setScore(10);

        Score score9 = objective.getScore("9");
        score9.customName(line9);
        score9.setScore(9);

        Score score8 = objective.getScore("8");
        score8.customName(line8);
        score8.setScore(8);

        Score score7 = objective.getScore("7");
        score7.customName(line7);
        score7.setScore(7);

        Score score6 = objective.getScore("6");
        score6.customName(line6);
        score6.setScore(6);

        Score score5 = objective.getScore("5");
        score5.customName(line5);
        score5.setScore(5);

        player.setScoreboard(board);
    }

    public void TABGestion(Player player) {

        String adresse = "                              " + plugin.ipServer + "                              ";
        if (plugin.isGameStarted) {
            int inGamePlayers = plugin.inGameTeamZombie.size() + plugin.inGameTeamHuman.size();
            int nbPlayersInGame;
            if (inGamePlayers <= 14) nbPlayersInGame = 1;
            else if (inGamePlayers <= 24) nbPlayersInGame = 2;
            else nbPlayersInGame = 3;

            int inGameZombie = plugin.inGameTeamZombie.size();
            float zombiePourcent = (float) inGameZombie / inGamePlayers;

            double humanDamage = damageInGame(nbPlayersInGame, 0, false, zombiePourcent);
            double zombieDamage = damageInGame(nbPlayersInGame, 0, true, zombiePourcent);
            double humanProtection = 0.65;
            double zombieProtection = 1.00;

            double humanDamagePerSecond = humanDamage / zombieProtection;
            String formatHuman = String.format("%.1f", humanDamagePerSecond);
            double zombieDamagePerSecond = zombieDamage / humanProtection;
            String formatZombie = String.format("%.1f", zombieDamagePerSecond);

            player.sendPlayerListHeaderAndFooter(
                    Component.text(adresse, TextColor.fromHexString("#7bcf32")),
                    Component.empty()
                            .appendNewline()
                            .append(Component.text("[Humain] Dégâts: " + humanDamage, TextColor.fromHexString(humanheart)).append(Component.text("❤ ", TextColor.fromHexString("#b82418"))).append(Component.text("- Hit protection : " + String.format("%.2f", humanProtection) + "s ", TextColor.fromHexString(humanheart))).append(Component.text("(DPS: " + formatHuman + ")", TextColor.fromHexString("#AAAAAA"))))
                            .appendNewline()
                            .append(Component.text("[Zombie] Dégâts: " + zombieDamage, TextColor.fromHexString(zombieheart)).append(Component.text("❤ ", TextColor.fromHexString("#b82418"))).append(Component.text("- Hit protection : " + String.format("%.2f", zombieProtection) + "s ", TextColor.fromHexString(zombieheart))).append(Component.text("(DPS: " + formatZombie + ")", TextColor.fromHexString("#AAAAAA"))))
            );
            return;
        }
        player.sendPlayerListHeaderAndFooter(
                Component.text(adresse, TextColor.fromHexString("#7bcf32")),
                Component.empty()
                        .appendNewline()
                        .append(Component.text("En attente du lancement de la partie...", TextColor.fromHexString("#AAAAAA")))
        );

    }
}

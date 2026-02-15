package org.nokoshizen.infectedV2;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.destroystokyo.paper.util.SneakyThrow;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.nokoshizen.infectedV2.Commands.*;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public final class InfectedV2 extends JavaPlugin implements Listener {

    public final List<Player> inGamePlayers = new ArrayList();
    public final List<Player> inGameTeamHuman = new ArrayList();
    public final List<Player> inGameTeamZombie = new ArrayList();
    public final List<Player> inGameTeamPigman = new ArrayList();
    public final List<Player> inGameTeamSkeleton = new ArrayList();
    public final List<UUID> inGameFirstZombies = new ArrayList();
    public final Map<String, Location> mapSelection = new HashMap();
    public final Map<UUID, Integer> inGamePlayerKills = new HashMap();
    public final Map<UUID, Integer> inGameZombieKills = new HashMap();
    public final Map<UUID, Integer> inGameHumanKills = new HashMap();
    public final Map<UUID, Integer> inGameHumanTime = new HashMap();
    public final Map<UUID, Integer> inGameZombieTime = new HashMap();
    public final Map<UUID, Integer> inGamePlayerPoints = new HashMap();

    private final Map<UUID, Boolean> stepCooldown = new HashMap<>();

    private final Map<UUID, Integer> clickCounts = new HashMap<>();
    private final Map<UUID, Long> lastClickTime = new HashMap<>();

    public String choosenMap = "Spawn";

    public String generalTextChatColor = "#CE8E46";
    public String palier0 = "#8c2495";
    public String palier1 = "#9e9e9e";
    public String palier2 = "#d0ad3e";
    public String palier3 = "#ff5847";
    public String palier4 = "#990400";
    public String infectedMessage = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Infecté » ";

    public boolean isThereIsMoreThan2Players = false;
    public boolean isGameStarted = false;
    public boolean isWaitingRoom = false;
    public static final long GAME_WAITINGTIME = 30 * 1000;
    public static final long GAME_DOWNTIME = 10 * 1000;
    public static final int GAME_CHOOSINGZOMBIES = 20;
    public static final long GAME_PLAYTIME = 5 * 60 * 1000 + 1;

    public Connection connection;
    public String ipServer = "Infected.fr";

    private ProtocolManager protocolManager;
    private final Set<UUID> skipHitSound = new HashSet<>();

    private final SkullUtils skullUtils = new SkullUtils();
    private final InfectedLogic infectedLogic = new InfectedLogic(this, skullUtils);
    private final SQLGestion sqlGestion = new SQLGestion(this);


    @Override
    public void onEnable() {

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new TeamExpansion(this).register();
        } else {
            getLogger().warning("PlaceholderAPI not found!");
        }

        connectToDatabase();

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.NATURAL_REGENERATION, false));
        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.DO_WEATHER_CYCLE, false));
        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false));
        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.DO_MOB_LOOT, false));
        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.DO_MOB_SPAWNING, false));
        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.LOCATOR_BAR, false));
        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false));

        getCommand("points").setExecutor(new CommandPoints(this, sqlGestion));
        getCommand("changestats").setExecutor(new CommandChangeStats(this, sqlGestion, infectedLogic));
        getCommand("changestats").setTabCompleter(new CommandChangeStatsCompleter());
        getCommand("changekb").setExecutor(new CommandChangeKb(this, infectedLogic));
        getCommand("changekb").setTabCompleter(new CommandChangeKbCompleter());

        // Maps
        mapSelection.put("Spawn", new Location(Bukkit.getWorld("world"), 0.5, 1.2, 0.5));
        //Harran
        mapSelection.put("Harran", new Location(Bukkit.getWorld("world"), 500.5, 70.2, 0.5));
        //Nature
        mapSelection.put("Nature", new Location(Bukkit.getWorld("world"), 1000.5, 70.2, 0.5));
        //Ravin
        mapSelection.put("Ravin", new Location(Bukkit.getWorld("world"), 1500.5, 70.2, 0.5));

        protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(this,
                PacketType.Play.Server.NAMED_SOUND_EFFECT) {

            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (skipHitSound.contains(player.getUniqueId())) {
                    event.setCancelled(true);
                    skipHitSound.remove(player.getUniqueId());
                }
            }
        });

    }


    @Override
    public void onDisable() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void connectToDatabase() {
        String host = "127.0.0.1";
        String port = "3306";

        String database = "infectedv2";
        String user = "root";
        String password = "7401";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Connexion
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + database +
                            "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                    user,
                    password
            );

            // Création automatique de la table si elle n'existe pas
            PreparedStatement ps = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS player_stats (" +
                            "uuid VARCHAR(36) PRIMARY KEY, " +
                            "name VARCHAR(36) NOT NULL, " +
                            "points INT DEFAULT 0, " +
                            "kills INT DEFAULT 0, " +
                            "deaths INT DEFAULT 0, " +
                            "games_played INT DEFAULT 0, " +
                            "games_won INT DEFAULT 0, " +
                            "games_won_as_human INT DEFAULT 0, " +
                            "games_won_as_zombie INT DEFAULT 0, " +
                            "games_loosed INT DEFAULT 0, " +
                            "first_zombie INT DEFAULT 0, " +
                            "guerison INT DEFAULT 0, " +
                            "total_survived_time INT DEFAULT 0, " +
                            "play_time INT DEFAULT 0, " +
                            "average_time_survived INT DEFAULT 0, " +
                            "choosen_chestplate INT DEFAULT 0" +
                            ");"
            );
            ps.executeUpdate();
            ps.close();

            getLogger().info("Connexion à la base de données réussie !");
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("Impossible de se connecter à la base de données !");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getInventory().clear();
        player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(2);

        sqlGestion.initializePlayerStats(player);

        Location location = mapSelection.get(choosenMap);
        player.teleport(location);

        player.setGameMode(GameMode.ADVENTURE);
        int onlinePlayers = Bukkit.getOnlinePlayers().size();

        if (isGameStarted) {
            event.setJoinMessage(null);
        } else {
            event.setJoinMessage(ChatColor.GRAY + player.getName() + ChatColor.of(generalTextChatColor) + " à rejoint la partie.");
            if (!isThereIsMoreThan2Players && onlinePlayers >= 2) {
                infectedLogic.StartingAndStoppingLogic();
                isThereIsMoreThan2Players = true;
            } else {
                player.setLevel(0);
                player.setExp(0);
            }
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            player.setHealth(player.getMaxHealth());
            if (isGameStarted) {
                if (!inGamePlayers.contains(player)) {
                    inGamePlayers.add(player);
                }
                if (infectedLogic.timeLeft < infectedLogic.totalTime - GAME_CHOOSINGZOMBIES) {
                    infectedLogic.InfectionLogic(player);
                } else {
                    infectedLogic.HumanLogic(player);
                }
            } else {
                infectedLogic.WaitingRoomLogic(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isGameStarted) {
            event.setQuitMessage(null);
            if (inGameTeamHuman.contains(player)) inGameTeamHuman.remove(player);
            if (inGameFirstZombies.contains(player)) inGameFirstZombies.remove(player);
            if (inGameTeamZombie.contains(player)) inGameTeamZombie.remove(player);

            if (infectedLogic.tracker.containsKey(player)) {
                infectedLogic.tracker.remove(player);
                return;
            }

            List<Player> traqueurs = findAllTraqueursOf(player, infectedLogic.tracker);
            for (Player traqueur : traqueurs) {
                infectedLogic.tracker.remove(traqueur);
                infectedLogic.RandomTargetGeneration(traqueur);
                infectedLogic.InventoryZombie(traqueur);
            }

        } else {
            event.setQuitMessage(ChatColor.GRAY + player.getName() + ChatColor.of("#CE8E46") + " à quitté la partie.");
        }
    }

    private List<Player> findAllTraqueursOf(Player target, Map<Player, Player> tracker) {
        return tracker.entrySet()
                .stream()
                .filter(e -> e.getValue().equals(target))
                .map(Map.Entry::getKey)
                .toList(); // en Java 16+, sinon use Collectors.toList()
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!isGameStarted) {
            if (event.getEntity() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.ADVENTURE) return;

        Action action = event.getAction();
        Block clicked = event.getClickedBlock();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (clicked == null) return;

        if (clicked.getType().isInteractable() || clicked.getState() instanceof InventoryHolder) {
            event.setCancelled(true);
        }

        if (item != null && item.getType() == Material.BOW &&
                (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) {
            return;
        }


        if (action == Action.RIGHT_CLICK_BLOCK) {

            if (isTrapdoor(clicked) || isDoor(clicked)) {

                Location loc = player.getLocation();
                Block blockBelow = loc.clone().subtract(0, 1, 0).getBlock();
                Block blockAbove = clicked.getLocation().clone().add(0, 1, 0).getBlock();

                boolean isTrapOrDoor = blockAbove.getBlockData() instanceof TrapDoor || blockAbove.getBlockData() instanceof Door;
                if (!isTrapOrDoor) return;

                if (player.getLocation().distance(clicked.getLocation().add(0.5, 0, 0.5)) > 2) return;

                if (clicked.getBlockData() instanceof TrapDoor trapdoor) {
                    handleTrapOrDoor(player, blockBelow, trapdoor.getFacing());
                } else if (clicked.getBlockData() instanceof Door door) {
                    handleTrapOrDoor(player, blockBelow, door.getFacing());
                }

                long now = System.currentTimeMillis();
                long lastClick = lastClickTime.getOrDefault(player.getUniqueId(), 0L);
                int count = clickCounts.getOrDefault(player.getUniqueId(), 0);

                if (now - lastClick > 20000) {
                    count = 0;
                }

                count++;
                clickCounts.put(player.getUniqueId(), count);
                lastClickTime.put(player.getUniqueId(), now);

                if (count >= 12) {
                    player.kick();
                } else if (count >= 8) {
                    clickCounts.remove(player.getUniqueId());
                    lastClickTime.remove(player.getUniqueId());
                    player.setHealth(0.0);
                } else if (count >= 4) {
                    player.sendMessage(ChatColor.RED + "Si vous continuez vous allez être tué.");
                }
            }
        }
    }

    private void handleTrapOrDoor(Player player, Block belowBlock, BlockFace facing) {
        Block blockSide1;
        Block blockSide2;

        if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
            // Axe X
            blockSide1 = belowBlock.getRelative(0, 0, -1);
            blockSide2 = belowBlock.getRelative(0, 0, 1);
        } else {
            // Axe Z
            blockSide1 = belowBlock.getRelative(-1, 0, 0);
            blockSide2 = belowBlock.getRelative(1, 0, 0);
        }

        if (isTrapdoor(blockSide1) || isTrapdoor(blockSide2) ||
                isDoor(blockSide1) || isDoor(blockSide2) ||
                isTrapdoor(belowBlock) || isDoor(belowBlock)) {

            Location safe = player.getLocation().clone();
            safe.setY(belowBlock.getY() + 0.5);
            player.teleport(safe);
        }
    }

    private boolean isTrapdoor(Block block) {
        Material type = block.getType();
        return switch (type) {
            case OAK_TRAPDOOR, SPRUCE_TRAPDOOR, BIRCH_TRAPDOOR, JUNGLE_TRAPDOOR,
                 ACACIA_TRAPDOOR, DARK_OAK_TRAPDOOR, MANGROVE_TRAPDOOR,
                 CRIMSON_TRAPDOOR, WARPED_TRAPDOOR -> true;
            default -> false;
        };
    }

    private boolean isDoor(Block block) {
        Material type = block.getType();
        return switch (type) {
            case OAK_DOOR, SPRUCE_DOOR, BIRCH_DOOR, JUNGLE_DOOR,
                 ACACIA_DOOR, DARK_OAK_DOOR, MANGROVE_DOOR,
                 CRIMSON_DOOR, WARPED_DOOR -> true;
            default -> false;
        };
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            player.setSneaking(false);
        }
    }

//    @EventHandler
//    public void onEntityDamage(EntityDamageEvent event) {
//        if (event.getEntity() instanceof Player player) {
//            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
//                event.setDamage(event.getDamage() * 0.5);
//            }
//        }
//    }

    @EventHandler
    public void onPlayerSneaking(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        player.setSneaking(false);
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework) {
            event.setCancelled(true);
        }

        if (event.getDamager() instanceof Arrow arrow) {
            if (arrow.getShooter() instanceof Player attacker) {
                Player victim = (Player) event.getEntity();


                boolean victimIsZombie = inGameTeamZombie.contains(victim);
                boolean attackerIsZombie = inGameTeamZombie.contains(attacker);

                if ((victimIsZombie && attackerIsZombie) || (!victimIsZombie && !attackerIsZombie)) {
                    event.setCancelled(true);
                    skipHitSound.add(victim.getUniqueId());
                    skipHitSound.add(attacker.getUniqueId());
                    return;
                }

                if (victim.getNoDamageTicks() > 0) {
                    event.setCancelled(true);
                    return;
                }

                event.setDamage(event.getDamage());

                int ticks = victimIsZombie ? 20 : 13;
                Bukkit.getScheduler().runTask(this, () -> {
                    victim.setNoDamageTicks(ticks);
                });

                Disguise disguise = DisguiseAPI.getDisguise(victim);
                if (disguise != null && disguise.getType() == DisguiseType.ZOMBIFIED_PIGLIN) {
                    for (Player p : victim.getWorld().getPlayers()) {
                        if (!p.equals(victim)) {
                            p.playSound(victim.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_HURT, 0.5F, 1F);
                        }
                    }
                }
            }
            return;
        }

        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        boolean wasSprinting = attacker.isSprinting();

        if (attacker.isOnline() && attacker.getFoodLevel() > 6 && wasSprinting && !attacker.isDead()) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                attacker.setSprinting(true);
            }, 1L);
        }

        if (!inGamePlayers.contains(victim) || !inGamePlayers.contains(attacker)) {
            skipHitSound.add(victim.getUniqueId());
            skipHitSound.add(attacker.getUniqueId());
            return;
        }

        boolean victimIsZombie = inGameTeamZombie.contains(victim);
        boolean attackerIsZombie = inGameTeamZombie.contains(attacker);

        if (victimIsZombie && attackerIsZombie) {
            event.setCancelled(true);
            skipHitSound.add(victim.getUniqueId());
            skipHitSound.add(attacker.getUniqueId());
            return;
        }

        if (!victimIsZombie && !attackerIsZombie) {
            event.setCancelled(true);
            skipHitSound.add(victim.getUniqueId());
            skipHitSound.add(attacker.getUniqueId());
            return;
        }

        if (victim.getNoDamageTicks() > 0) {
            event.setCancelled(true);
            return;
        }

        Disguise disguise = DisguiseAPI.getDisguise(victim);
        if (disguise != null && disguise.getType() == DisguiseType.ZOMBIFIED_PIGLIN) {
            for (Player p : victim.getWorld().getPlayers()) {
                if (!p.equals(victim)) {
                    p.playSound(victim.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_HURT, 0.5F, 1F);
                }
            }
        }

        if (inGamePlayers.contains(victim)) {
            infectedLogic.InGameDamage(victim, attacker, event);
        }

        int ticks = victimIsZombie ? 20 : 13;
        Bukkit.getScheduler().runTask(this, () -> {
            victim.setNoDamageTicks(ticks);
        });
    }

    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            player.setSneaking(false);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        event.setCancelled(true);

        sqlGestion.Death(player);

        infectedLogic.handlePlayerDeath(player);

        player.setHealth(player.getMaxHealth());
        if (isGameStarted) {
            Location deathLocation = player.getLocation();
            World world = deathLocation.getWorld();

            if (inGameTeamHuman.contains(player)) {
                if (infectedLogic.timeLeft >= infectedLogic.totalTime - 19) {
                    player.teleport(deathLocation);
                    return;
                }

                inGameTeamHuman.remove(player);
                player.teleport(deathLocation);
                world.playSound(deathLocation, Sound.ENTITY_ZOMBIE_INFECT, 1.5f, 0f);
                infectedLogic.InfectionLogic(player);

                List<Player> traqueurs = findAllTraqueursOf(player, infectedLogic.tracker);
                for (Player traqueur : traqueurs) {
                    infectedLogic.tracker.remove(traqueur);
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (!inGameTeamHuman.contains(traqueur)) {
                            infectedLogic.RandomTargetGeneration(traqueur);
                            infectedLogic.InventoryZombie(traqueur);
                        }
                    }, 30L);
                }

            } else {
                Location location = mapSelection.get(choosenMap);
                player.teleport(location);
            }
        }
    }

    public String getColoredName(Player player) {
        if (inGameTeamZombie.contains(player)) return ChatColor.DARK_GREEN + player.getName();
        if (inGameTeamHuman.contains(player)) return ChatColor.DARK_AQUA + player.getName();
        return player.getName();
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (isGameStarted) {
            Player player = event.getPlayer();
            if (inGameTeamZombie.contains(player)) {
                MobDisguise zombieDisguise = new MobDisguise(DisguiseType.ZOMBIE);
                zombieDisguise.setViewSelfDisguise(false);
                DisguiseAPI.disguiseToAll(player, zombieDisguise);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (player.getGameMode() == GameMode.CREATIVE) return;
        event.setCancelled(true);

        int slot = event.getSlot();
        if (slot < 9 || slot > 17) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;
        if (clickedItem.getType() != Material.LEATHER_CHESTPLATE) return;

        if (!(clickedItem.getItemMeta() instanceof LeatherArmorMeta meta)) return;

        infectedLogic.ChestPlateChoosen(player, slot);
        sqlGestion.setChoosenChestplate(player, slot);
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
    }


    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        event.setCancelled(true);

        TextColor tagColor;
        TextColor nameColor;

        if (inGameTeamZombie.contains(player)) {
            tagColor = TextColor.fromHexString(infectedLogic.zombieColor);
            nameColor = TextColor.color(0x00AA00);
        } else if (inGameTeamHuman.contains(player)) {
            tagColor = TextColor.fromHexString(infectedLogic.humanColor);
            nameColor = TextColor.color(0x00AAAA);
        } else {
            tagColor = TextColor.fromHexString(infectedLogic.humanColor);
            nameColor = TextColor.color(0x00AAAA);
        }

        int points = sqlGestion.getPoints(player);
        TextColor crochetColor = TextColor.fromHexString("#85827e");

        Component pointsComp = Component.text("[", crochetColor)
                .append(formatPoints(points))
                .append(Component.text("]", crochetColor));

        Component nameComp = Component.text()
                .append(Component.text("[", tagColor))
                .append(Component.text(
                        inGameTeamZombie.contains(player) ? "Zombie" : "Humain",
                        tagColor))
                .append(Component.text("] ", tagColor))
                .append(Component.text(player.getName(), nameColor))
                .build();

        String msg = event.getMessage();
        boolean isTeamChat = msg.startsWith("@");

        if (isTeamChat) msg = msg.substring(1).trim();

        Component messageComp = Component.text(msg, TextColor.fromHexString("#FFFFFF"));
        Component prefix = Component.text(isTeamChat ? "@" : "", TextColor.fromHexString("#6ba1d0"));

        Component completeMessage = Component.text()
                .append(pointsComp)
                .append(nameComp)
                .append(Component.text(": ", TextColor.fromHexString("#FFFFFF")))
                .append(prefix)
                .append(messageComp)
                .build();

        if (!isTeamChat) {
            if (inGameTeamZombie.contains(player)) {
                for (Player p : inGameTeamZombie) p.sendMessage(completeMessage);
                return;
            } else if (inGameTeamHuman.contains(player)) {
                for (Player p : inGameTeamHuman) p.sendMessage(completeMessage);
                return;
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(completeMessage);
            }
        } else {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(completeMessage);
            }
        }
    }

    public Component formatPoints(int points) {
        String formatted;
        TextColor color;

        if (points >= 3_000_000) {
            double value = Math.floor(points / 1_000_000.0 * 100) / 100;
            formatted = String.format("%.2fM", value);
            color = TextColor.fromHexString(palier4);
        } else if (points >= 1_000_000) {
            double value = Math.floor(points / 1_000_000.0 * 100) / 100;
            formatted = String.format("%.2fM", value);
            color = TextColor.fromHexString(palier3);
        } else if (points >= 100_000) {
            long value = points / 1_000;
            formatted = value + "K";
            color = TextColor.fromHexString(palier2);
        } else if (points >= 1_000) {
            long value = points / 1_000;
            formatted = value + "K";
            color = TextColor.fromHexString(palier1);
        } else if (points >= 0) {
            formatted = String.valueOf(points);
            color = TextColor.fromHexString(palier1);
        } else {
            formatted = String.valueOf(points);
            color = TextColor.fromHexString(palier0);
        }

        return Component.text(formatted, color);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        Disguise disguise = DisguiseAPI.getDisguise(player);
        if (disguise == null) return;

        if (event.getFrom().distanceSquared(event.getTo()) == 0) return;


        if (disguise.getType() == DisguiseType.ZOMBIFIED_PIGLIN) {
            if (!player.isOnGround() || player.isSneaking()) return;

            if (stepCooldown.getOrDefault(player.getUniqueId(), false)) return;


            for (Player p : player.getWorld().getPlayers()) {
                if (p.equals(player)) continue;
                p.playSound(
                        player.getLocation(),
                        Sound.ENTITY_ZOMBIE_STEP,
                        0.1f,
                        1.0f
                );
            }


            stepCooldown.put(player.getUniqueId(), true);


            Bukkit.getScheduler().runTaskLater(this, () ->
                            stepCooldown.put(player.getUniqueId(), false),
                    8L
            );
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onArrowLand(PlayerPickupArrowEvent event) {
        Arrow arrow = (Arrow) event.getArrow();
        Player player = event.getPlayer();

        if (arrow.getShooter() instanceof Player shooter) {

            if (!shooter.getUniqueId().equals(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            ItemStack arrowSlot = player.getInventory().getItem(8);

            int arrowCount = 0;
            if (arrowSlot != null && arrowSlot.getType() == Material.ARROW) {
                arrowCount = arrowSlot.getAmount();
            }

            if (arrowCount >= 4 && inGameTeamHuman.contains(shooter)) {
                event.setCancelled(true);
                return;
            } else if (arrowCount >= 20 && inGameTeamSkeleton.contains(shooter)) {
                event.setCancelled(true);
                return;
            }

            if (arrow.getLifetimeTicks() <= 20) return;

            player.getInventory().setItem(8, new ItemStack(Material.ARROW, arrowCount + 1));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2F, 1F);
            event.setCancelled(true);
            arrow.remove();
        }
    }

    @EventHandler
    public void onPlayerShootArrow(EntityShootBowEvent  event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        arrow.setLifetimeTicks(20 * 20);
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();

        if (!player.isOp()) {
            List<String> commandesCachees = Arrays.asList(
                    "points",
                    "changestats",
                    "changekb"
            );

            for (String cmd : commandesCachees) {
                event.getCommands().remove(cmd);
            }
        }
    }

}

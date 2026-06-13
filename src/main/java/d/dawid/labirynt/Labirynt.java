package d.dawid.labirynt;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class Labirynt extends JavaPlugin implements Listener {

    private final HashMap<UUID, Long> startTimes = new HashMap<>();
    private final java.util.ArrayList<Location> allMetaLocations = new java.util.ArrayList<>();
    private final HashMap<UUID, Location> startLocations = new HashMap<>();
    private final java.util.ArrayList<Location> boardLocations = new java.util.ArrayList<>();

    private DatabaseManager db;
    private final Random random = new Random();

    private static final long TICKS_24H = 1728000L;
    private static final String[] REWARDS = {
            "give %s diamond 5",
            "give %s diamond 3",
            "give %s diamond 1"
    };

    @Override
    public void onEnable() {
        this.db = new DatabaseManager(getDataFolder());
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().runTaskTimer(this, this::updateLeaderboard, 100L, 600L);
        getServer().getScheduler().runTaskTimer(this, this::dailyReset, TICKS_24H, TICKS_24H);

        getLogger().info("Wlasny Plugin Labirynt v1.0 uruchomiony!");
    }

    @Override
    public void onDisable() {
        if (db != null) db.close();
    }

    private void dailyReset() {
        getLogger().info("[Labirynt] Rozpoczynam dzienny reset...");
        List<DatabaseManager.TopEntry> top3 = db.getTop3();

        if (!top3.isEmpty()) {
            getServer().broadcastMessage("§6§l[LABIRYNT] §eDzienny reset wyników!");
            for (DatabaseManager.TopEntry entry : top3) {
                String medal = switch (entry.rank) {
                    case 1 -> "§6✦ 1. miejsce";
                    case 2 -> "§7✦ 2. miejsce";
                    case 3 -> "§c✦ 3. miejsce";
                    default -> entry.rank + ".";
                };
                getServer().broadcastMessage(medal + " §e" + entry.name
                        + " §7- §f" + String.format("%.2f", entry.time) + "s");
            }
        }

        for (DatabaseManager.TopEntry entry : top3) {
            Player player = getServer().getPlayer(entry.uuid);
            int rewardIndex = entry.rank - 1;

            if (rewardIndex < REWARDS.length) {
                String rewardCmd = String.format(REWARDS[rewardIndex], entry.name);
                if (player != null && player.isOnline()) {
                    getServer().dispatchCommand(getServer().getConsoleSender(), rewardCmd);
                    player.sendMessage("§6§l★ Gratulacje! §eOtrzymujesz nagrodę!");
                }
                db.logReward(entry.uuid, entry.name, entry.rank, rewardCmd);
            }
        }

        db.archiveAndReset();
        updateLeaderboard();
    }

    private void updateLeaderboard() {
        if (boardLocations.isEmpty()) return;

        List<DatabaseManager.TopEntry> top = db.getTop3();
        while (top.size() < 3) top.add(null);

        // Zaktualizuj wszystkie wygenerowane na serwerze tablice!
        for (Location boardLoc : boardLocations) {
            World world = boardLoc.getWorld();
            if (world == null) continue;

            // TABLICZKA 1 (Górna)
            Block b1 = world.getBlockAt(boardLoc.clone().add(0, 1, 0));
            b1.setType(Material.OAK_SIGN);

            Rotatable rot1 = (Rotatable) b1.getBlockData();
            rot1.setRotation(BlockFace.NORTH);
            b1.setBlockData(rot1);

            if (b1.getState() instanceof Sign sign) {
                sign.getSide(Side.FRONT).setLine(0, "§6§lTOP LABIRYNT");
                sign.getSide(Side.FRONT).setLine(1, top.get(0) != null ? "§61. §e" + truncate(top.get(0).name, 11) : "§7---");
                sign.getSide(Side.FRONT).setLine(2, top.get(0) != null ? "§f   " + String.format("%.2f", top.get(0).time) + "s" : "");
                sign.getSide(Side.FRONT).setLine(3, top.get(1) != null ? "§72. §e" + truncate(top.get(1).name, 11) : "§7---");
                sign.update();
            }

            // TABLICZKA 2 (Dolna)
            Block b2 = world.getBlockAt(boardLoc);
            b2.setType(Material.OAK_SIGN);

            Rotatable rot2 = (Rotatable) b2.getBlockData();
            rot2.setRotation(BlockFace.NORTH);
            b2.setBlockData(rot2);

            if (b2.getState() instanceof Sign sign) {
                sign.getSide(Side.FRONT).setLine(0, top.get(1) != null ? "§f   " + String.format("%.2f", top.get(1).time) + "s" : "");
                sign.getSide(Side.FRONT).setLine(1, top.get(2) != null ? "§c3. §e" + truncate(top.get(2).name, 11) : "§7---");
                sign.getSide(Side.FRONT).setLine(2, top.get(2) != null ? "§f   " + String.format("%.2f", top.get(2).time) + "s" : "");
                sign.getSide(Side.FRONT).setLine(3, "§8/labirynt top");
                sign.update();
            }
        }
    }

    private String truncate(String name, int maxLen) {
        if (name == null) return "";
        if (name.length() <= maxLen) return name;
        return name.substring(0, maxLen - 1) + ".";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("labirynt") && args.length == 1) {
            if (sender.hasPermission("labirynt.admin")) {
                return List.of("top", "mytime", "reset", "restart");
            }
            return List.of("top", "mytime", "restart");
        }
        return List.of();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("labirynt")) return false;

        if (args.length > 0 && args[0].equalsIgnoreCase("top")) {
            sender.sendMessage("§8--- §6§lTOP 5 REKORDÓW §8---");
            List<String> records = db.getTopRecords();
            if (records.isEmpty()) sender.sendMessage("§7Brak wyników. Bądź pierwszy!");
            else for (String record : records) sender.sendMessage(record);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("mytime")) {
            if (!(sender instanceof Player player)) return true;
            double best = db.getPersonalBest(player.getUniqueId());
            if (best < 0) player.sendMessage("§7Nie masz jeszcze rekordu.");
            else player.sendMessage("§6Twój rekord: §f" + String.format("%.2f", best) + "s");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("labirynt.admin")) {
                sender.sendMessage("§cNie masz uprawnień!");
                return true;
            }
            dailyReset();
            sender.sendMessage("§aZresetowano bazę danych i rozdano nagrody!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("restart")) {
            if (!(sender instanceof Player player)) return true;
            if (startLocations.containsKey(player.getUniqueId())) {
                startTimes.remove(player.getUniqueId());
                player.teleport(startLocations.get(player.getUniqueId()));
                player.sendMessage("§ePomyślnie cofnięto! Wejdź na złoty blok, aby zacząć od nowa.");
            } else {
                player.sendMessage("§cNajpierw musisz wygenerować labirynt komendą /labirynt!");
            }
            return true;
        }

        if (!(sender instanceof Player player)) return true;

        player.sendMessage("§aBudowanie autorskiego labiryntu...");

        Location loc = player.getLocation().getBlock().getLocation();
        World world = loc.getWorld();

        MazeGenerator generator = new MazeGenerator(10, 10);
        int[][] grid = generator.getGrid();

        for (int x = 0; x < grid.length; x++) {
            for (int z = 0; z < grid[0].length; z++) {
                Location blockLoc = loc.clone().add(x, 0, z);

                world.getBlockAt(blockLoc.clone().add(0, 2, 0)).setType(Material.STONE_BRICKS);

                if (grid[x][z] == 1) {
                    world.getBlockAt(blockLoc).setType(Material.STONE_BRICKS);
                    world.getBlockAt(blockLoc.clone().add(0, 1, 0)).setType(Material.STONE_BRICKS);
                } else {
                    world.getBlockAt(blockLoc.clone().add(0, -1, 0)).setType(Material.MOSS_BLOCK);
                    world.getBlockAt(blockLoc).setType(Material.AIR);
                    world.getBlockAt(blockLoc.clone().add(0, 1, 0)).setType(Material.AIR);
                    if (random.nextInt(15) == 0) {
                        world.getBlockAt(blockLoc).setType(Material.TORCH);
                    }
                }
            }
        }

        int endGX = generator.getEndGridX();
        int endGZ = generator.getEndGridZ();
        Location metaLoc = loc.clone().add(endGX, -1, endGZ);
        world.getBlockAt(metaLoc).setType(Material.DIAMOND_BLOCK);
        allMetaLocations.add(metaLoc);

        int startGX = generator.getStartGridX();
        int startGZ = generator.getStartGridZ();
        Location startTrigger = loc.clone().add(startGX, -1, startGZ);
        world.getBlockAt(startTrigger).setType(Material.GOLD_BLOCK);

        world.getBlockAt(loc.clone().add(startGX, -1, 0)).setType(Material.MOSS_BLOCK);
        world.getBlockAt(loc.clone().add(startGX,  0, 0)).setType(Material.AIR);
        world.getBlockAt(loc.clone().add(startGX,  1, 0)).setType(Material.AIR);

        for (int i = -1; i >= -3; i--) {
            Location corridor = loc.clone().add(startGX, 0, i);
            world.getBlockAt(corridor.clone().add(0, -1, 0)).setType(Material.MOSS_BLOCK);
            world.getBlockAt(corridor).setType(Material.AIR);
            world.getBlockAt(corridor.clone().add(0, 1, 0)).setType(Material.AIR);
        }

        boardLocations.add(loc.clone().add(startGX - 2, 0, -1));
        updateLeaderboard();

        Location spawnLoc = loc.clone().add(startGX + 0.5, 0, -4);
        spawnLoc.setYaw(0);
        player.teleport(spawnLoc);

        startLocations.put(player.getUniqueId(), spawnLoc);

        player.sendMessage("§e§lWejdź do labiryntu żeby uruchomić timer!");
        return true;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location pLoc = player.getLocation();

        // Sprawdzamy Y-1 (chodzenie) oraz Y-2 (wysoki skok/lot)
        Material m1 = pLoc.clone().subtract(0, 1, 0).getBlock().getType();
        Material m2 = pLoc.clone().subtract(0, 2, 0).getBlock().getType();

        // START
        if ((m1 == Material.GOLD_BLOCK || m2 == Material.GOLD_BLOCK) && !startTimes.containsKey(uuid)) {
            startTimes.put(uuid, System.currentTimeMillis());
            startLocations.put(uuid, pLoc.clone());
            player.sendMessage("§a§l⏱ TIMER WYSTARTOWAŁ! Biegnij!");
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            return;
        }

        // META
        if ((m1 == Material.DIAMOND_BLOCK || m2 == Material.DIAMOND_BLOCK) && startTimes.containsKey(uuid)) {

            // Sprawdzamy czy blok, na którym stoi gracz, jest jedną z globalnych met na serwerze
            boolean isOfficialMeta = false;
            for (Location meta : allMetaLocations) {
                if (meta.getBlockX() == pLoc.getBlockX() && meta.getBlockZ() == pLoc.getBlockZ()) {
                    isOfficialMeta = true;
                    break;
                }
            }

            if (isOfficialMeta) {
                long ms = System.currentTimeMillis() - startTimes.get(uuid);
                double seconds = ms / 1000.0;

                player.sendMessage("§b§l★ GRATULACJE! §7Twój czas: §f§l" + String.format("%.2f", seconds) + "s");

                db.saveRecord(uuid, player.getName(), seconds);

                double best = db.getPersonalBest(uuid);
                if (best == seconds) {
                    getServer().broadcastMessage("§6§l[LABIRYNT] §e" + player.getName()
                            + " §7ustanowił nowy rekord: §f§l" + String.format("%.2f", seconds) + "s!");
                }

                updateLeaderboard();

                startTimes.remove(uuid);
                startLocations.remove(uuid); // Czyścimy punkt restartu
            }
        }
    }

    // ANTY-CHEAT 1: Blokada niszczenia ścian
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (startTimes.containsKey(player.getUniqueId())) {
            if (!player.hasPermission("labirynt.admin")) {
                event.setCancelled(true);
                player.sendMessage("§c§lHej! §7W labiryncie nie można niszczyć bloków!");
            }
        }
    }

    // ANTY-CHEAT 2: Blokada zamurowywania korytarzy
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (startTimes.containsKey(player.getUniqueId())) {
            if (!player.hasPermission("labirynt.admin")) {
                event.setCancelled(true);
                player.sendMessage("§c§lHej! §7W labiryncie nie można stawiać bloków!");
            }
        }
    }

    // ANTY-CHEAT 3: Anulowanie timera po wpisaniu zewnętrznego /tp
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (startTimes.containsKey(uuid)) {
            // Jeśli teleportacja nastąpiła przez zwykłą komendę admina (np. /tp), przerywamy licznik
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
                startTimes.remove(uuid);
                startLocations.remove(uuid);
                player.sendMessage("§eUżyłeś teleportacji! Twój bieg w labiryncie został anulowany.");
            }
        }
    }

    // ANTY-CHEAT 4: Śmierć gracza przerywa tykanie stopera
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        if (startTimes.containsKey(uuid)) {
            startTimes.remove(uuid);
            startLocations.remove(uuid);
            player.sendMessage("§cZginąłeś! Twój czas w labiryncie został anulowany.");
        }
    }

    // OPTYMALIZACJA RAM: Wyczyszczenie HashMap po opuszczeniu serwera
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        startTimes.remove(uuid);
        startLocations.remove(uuid);
    }
}
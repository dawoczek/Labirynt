package d.dawid.labirynt;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private Connection connection;

    public DatabaseManager(File dataFolder) {
        if (!dataFolder.exists()) dataFolder.mkdir();

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + new File(dataFolder, "records.db"));

            try (Statement statement = connection.createStatement()) {
                // Tabela aktywnych wyników (resetowana co 24h)
                statement.execute("CREATE TABLE IF NOT EXISTS maze_records (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "player_name TEXT, " +
                        "best_time REAL)");

                // Tabela archiwum (nigdy nie kasowana, do dokumentacji projektu)
                statement.execute("CREATE TABLE IF NOT EXISTS maze_archive (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "uuid TEXT, " +
                        "player_name TEXT, " +
                        "best_time REAL, " +
                        "reset_date TEXT)");  // data resetu np. "2026-05-09"

                // Tabela logów nagród
                statement.execute("CREATE TABLE IF NOT EXISTS reward_log (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "uuid TEXT, " +
                        "player_name TEXT, " +
                        "rank INTEGER, " +
                        "reward TEXT, " +
                        "rewarded_at TEXT)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Zapisz wynik gracza (tylko jeśli lepszy)
    public void saveRecord(UUID uuid, String name, double time) {
        try {
            PreparedStatement check = connection.prepareStatement(
                    "SELECT best_time FROM maze_records WHERE uuid = ?");
            check.setString(1, uuid.toString());
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                double oldTime = rs.getDouble("best_time");
                if (time < oldTime) {
                    PreparedStatement update = connection.prepareStatement(
                            "UPDATE maze_records SET best_time = ?, player_name = ? WHERE uuid = ?");
                    update.setDouble(1, time);
                    update.setString(2, name);
                    update.setString(3, uuid.toString());
                    update.executeUpdate();
                }
            } else {
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO maze_records (uuid, player_name, best_time) VALUES (?, ?, ?)");
                insert.setString(1, uuid.toString());
                insert.setString(2, name);
                insert.setDouble(3, time);
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // TOP 5 wyników bieżącego dnia (do tablicy i komendy)
    public List<String> getTopRecords() {
        List<String> top = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT player_name, best_time FROM maze_records ORDER BY best_time ASC LIMIT 5");
            ResultSet rs = ps.executeQuery();
            int rank = 1;
            while (rs.next()) {
                String medal = switch (rank) {
                    case 1 -> "§6✦";
                    case 2 -> "§7✦";
                    case 3 -> "§c✦";
                    default -> "§8 " + rank + ".";
                };
                top.add(medal + " §e" + rs.getString("player_name")
                        + " §7- §f" + String.format("%.2f", rs.getDouble("best_time")) + "s");
                rank++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top;
    }

    // Pobierz TOP 3 graczy jako obiekty (do rozdania nagród przed resetem)
    public List<TopEntry> getTop3() {
        List<TopEntry> top = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT uuid, player_name, best_time FROM maze_records ORDER BY best_time ASC LIMIT 3");
            ResultSet rs = ps.executeQuery();
            int rank = 1;
            while (rs.next()) {
                top.add(new TopEntry(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("player_name"),
                        rs.getDouble("best_time"),
                        rank++
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top;
    }

    // Osobisty rekord gracza (do /labirynt mytime)
    public double getPersonalBest(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT best_time FROM maze_records WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("best_time");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Przenieś aktualne wyniki do archiwum, potem wyczyść
    public void archiveAndReset() {
        try {
            // Pobierz dzisiejszą datę
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd")
                    .format(new java.util.Date());

            // Skopiuj wszystkie rekordy do archiwum
            PreparedStatement archive = connection.prepareStatement(
                    "INSERT INTO maze_archive (uuid, player_name, best_time, reset_date) " +
                    "SELECT uuid, player_name, best_time, ? FROM maze_records");
            archive.setString(1, today);
            archive.executeUpdate();

            // Wyczyść aktywne wyniki
            connection.createStatement().execute("DELETE FROM maze_records");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Zapisz log nagrody
    public void logReward(UUID uuid, String name, int rank, String reward) {
        try {
            String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date());
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO reward_log (uuid, player_name, rank, reward, rewarded_at) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setInt(3, rank);
            ps.setString(4, reward);
            ps.setString(5, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Klasa pomocnicza do trzymania danych TOP 3
    public static class TopEntry {
        public final UUID uuid;
        public final String name;
        public final double time;
        public final int rank;

        public TopEntry(UUID uuid, String name, double time, int rank) {
            this.uuid = uuid;
            this.name = name;
            this.time = time;
            this.rank = rank;
        }
    }
}

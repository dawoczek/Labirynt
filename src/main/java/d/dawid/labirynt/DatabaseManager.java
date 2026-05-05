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
                statement.execute("CREATE TABLE IF NOT EXISTS maze_records (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "player_name TEXT, " +
                        "best_time REAL)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Zapisywanie nowego rekordu gracza
    public void saveRecord(UUID uuid, String name, double time) {
        try {
            PreparedStatement check = connection.prepareStatement(
                    "SELECT best_time FROM maze_records WHERE uuid = ?");
            check.setString(1, uuid.toString());
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                double oldTime = rs.getDouble("best_time");
                // Nadpisujemy tylko jeśli nowy wynik jest lepszy
                if (time < oldTime) {
                    PreparedStatement update = connection.prepareStatement(
                            "UPDATE maze_records SET best_time = ?, player_name = ? WHERE uuid = ?");
                    update.setDouble(1, time);
                    update.setString(2, name); // aktualizujemy też nick (może się zmienić)
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

    // Pobieranie TOP 5 wyników
    public List<String> getTopRecords() {
        List<String> top = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT player_name, best_time FROM maze_records ORDER BY best_time ASC LIMIT 5");
            ResultSet rs = ps.executeQuery();
            int rank = 1;
            while (rs.next()) {
                String line = "§6" + rank + ". §e" + rs.getString("player_name")
                        + " §7- §f" + String.format("%.2f", rs.getDouble("best_time")) + "s";
                top.add(line);
                rank++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top;
    }

    // Pobieranie osobistego rekordu gracza (do /labirynt mytime)
    public double getPersonalBest(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT best_time FROM maze_records WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("best_time");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // -1 oznacza brak rekordu
    }

    // Reset wszystkich wyników (do użycia przez scheduler co 24h)
    public void resetAllRecords() {
        try (Statement st = connection.createStatement()) {
            st.execute("DELETE FROM maze_records");
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
}
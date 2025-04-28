package minecraftdiscord.database;

import minecraftdiscord.MinecraftDiscordPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

public class DatabaseManager {

    private final MinecraftDiscordPlugin plugin;
    private final Logger logger;
    private Connection connection;

    public DatabaseManager(MinecraftDiscordPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void initialize() {
        // データベースディレクトリを作成
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // SQLiteに接続
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + new File(dataFolder, "playerdata.db"));

            // テーブルを作成
            createTables();

            logger.info("データベースに接続しました。");
        } catch (ClassNotFoundException | SQLException e) {
            logger.severe("データベース接続中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        Statement statement = connection.createStatement();

        // プレイヤーデータテーブル
        statement.execute(
                "CREATE TABLE IF NOT EXISTS players (" +
                        "minecraft_uuid TEXT PRIMARY KEY, " +
                        "minecraft_name TEXT, " +
                        "discord_id TEXT, " +
                        "registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")"
        );

        // 統計データテーブル
        statement.execute(
                "CREATE TABLE IF NOT EXISTS player_stats (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "minecraft_uuid TEXT, " +
                        "date TEXT, " +
                        "kills INTEGER DEFAULT 0, " +
                        "deaths INTEGER DEFAULT 0, " +
                        "distance_traveled DOUBLE DEFAULT 0, " +
                        "daily_distance DOUBLE DEFAULT 0, " +
                        "play_time_minutes INTEGER DEFAULT 0, " +
                        "achievements_count INTEGER DEFAULT 0, " +
                        "last_x DOUBLE DEFAULT 0, " +
                        "last_y DOUBLE DEFAULT 0, " +
                        "last_z DOUBLE DEFAULT 0, " +
                        "FOREIGN KEY (minecraft_uuid) REFERENCES players (minecraft_uuid), " +
                        "UNIQUE (minecraft_uuid, date)" +
                        ")"
        );

        // 実績テーブル
        statement.execute(
                "CREATE TABLE IF NOT EXISTS player_achievements (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "minecraft_uuid TEXT, " +
                        "achievement_key TEXT, " +
                        "unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (minecraft_uuid) REFERENCES players (minecraft_uuid), " +
                        "UNIQUE (minecraft_uuid, achievement_key)" +
                        ")"
        );

        statement.close();
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("データベース接続が閉じられました。");
            }
        } catch (SQLException e) {
            logger.severe("データベース接続を閉じる際にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // プレイヤー登録
    public boolean registerPlayer(String minecraftUuid, String minecraftName, String discordId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO players (minecraft_uuid, minecraft_name, discord_id) VALUES (?, ?, ?)"
            );
            ps.setString(1, minecraftUuid);
            ps.setString(2, minecraftName);
            ps.setString(3, discordId);
            ps.executeUpdate();
            ps.close();

            // 今日の統計データがなければ作成
            ensureTodayStats(minecraftUuid);

            return true;
        } catch (SQLException e) {
            logger.severe("プレイヤー登録中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 今日の統計データを確保
    private void ensureTodayStats(String minecraftUuid) throws SQLException {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO player_stats (minecraft_uuid, date) VALUES (?, ?)"
        );
        ps.setString(1, minecraftUuid);
        ps.setString(2, today);
        ps.executeUpdate();
        ps.close();
    }

    // Discord IDからMinecraft UUIDを取得
    public String getMinecraftUuidByDiscordId(String discordId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT minecraft_uuid FROM players WHERE discord_id = ?"
            );
            ps.setString(1, discordId);
            ResultSet rs = ps.executeQuery();

            String uuid = null;
            if (rs.next()) {
                uuid = rs.getString("minecraft_uuid");
            }

            rs.close();
            ps.close();
            return uuid;
        } catch (SQLException e) {
            logger.severe("Discord IDからMinecraft UUIDを取得中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // キル数を更新
    public void incrementKills(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            ensureTodayStats(uuid);

            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE player_stats SET kills = kills + 1 WHERE minecraft_uuid = ? AND date = ?"
            );
            ps.setString(1, uuid);
            ps.setString(2, today);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            logger.severe("キル数更新中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 死亡回数を更新
    public void incrementDeaths(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            ensureTodayStats(uuid);

            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE player_stats SET deaths = deaths + 1 WHERE minecraft_uuid = ? AND date = ?"
            );
            ps.setString(1, uuid);
            ps.setString(2, today);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            logger.severe("死亡回数更新中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 移動距離を更新
    public void updateDistanceTraveled(Player player, double distance) {
        try {
            String uuid = player.getUniqueId().toString();
            ensureTodayStats(uuid);

            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE player_stats SET distance_traveled = distance_traveled + ?, daily_distance = daily_distance + ? WHERE minecraft_uuid = ? AND date = ?"
            );
            ps.setDouble(1, distance);
            ps.setDouble(2, distance);
            ps.setString(3, uuid);
            ps.setString(4, today);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            logger.severe("移動距離更新中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // プレイ時間を更新
    public void updatePlayTime(Player player, int minutes) {
        try {
            String uuid = player.getUniqueId().toString();
            ensureTodayStats(uuid);

            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE player_stats SET play_time_minutes = play_time_minutes + ? WHERE minecraft_uuid = ? AND date = ?"
            );
            ps.setInt(1, minutes);
            ps.setString(2, uuid);
            ps.setString(3, today);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            logger.severe("プレイ時間更新中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 実績を追加
    public void addAchievement(Player player, String achievementKey) {
        try {
            String uuid = player.getUniqueId().toString();

            // 実績を追加
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR IGNORE INTO player_achievements (minecraft_uuid, achievement_key) VALUES (?, ?)"
            );
            ps.setString(1, uuid);
            ps.setString(2, achievementKey);
            int inserted = ps.executeUpdate();
            ps.close();

            // 新しい実績だったら実績カウントを増やす
            if (inserted > 0) {
                ensureTodayStats(uuid);

                String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

                ps = connection.prepareStatement(
                        "UPDATE player_stats SET achievements_count = achievements_count + 1 WHERE minecraft_uuid = ? AND date = ?"
                );
                ps.setString(1, uuid);
                ps.setString(2, today);
                ps.executeUpdate();
                ps.close();
            }
        } catch (SQLException e) {
            logger.severe("実績追加中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 座標を更新
    public void updatePosition(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            ensureTodayStats(uuid);

            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE player_stats SET last_x = ?, last_y = ?, last_z = ? WHERE minecraft_uuid = ? AND date = ?"
            );
            ps.setDouble(1, player.getLocation().getX());
            ps.setDouble(2, player.getLocation().getY());
            ps.setDouble(3, player.getLocation().getZ());
            ps.setString(4, uuid);
            ps.setString(5, today);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            logger.severe("座標更新中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // プレイヤーの統計データを取得
    public PlayerData getPlayerStats(String minecraftUuid) {
        try {
            ensureTodayStats(minecraftUuid);

            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            PreparedStatement ps = connection.prepareStatement(
                    "SELECT p.minecraft_name, ps.* FROM players p " +
                            "JOIN player_stats ps ON p.minecraft_uuid = ps.minecraft_uuid " +
                            "WHERE p.minecraft_uuid = ? AND ps.date = ?"
            );
            ps.setString(1, minecraftUuid);
            ps.setString(2, today);
            ResultSet rs = ps.executeQuery();

            PlayerData data = null;
            if (rs.next()) {
                data = new PlayerData();
                data.setMinecraftUuid(minecraftUuid);
                data.setMinecraftName(rs.getString("minecraft_name"));
                data.setKills(rs.getInt("kills"));
                data.setDeaths(rs.getInt("deaths"));
                data.setDistanceTraveled(rs.getDouble("distance_traveled"));
                data.setDailyDistance(rs.getDouble("daily_distance"));
                data.setPlayTimeMinutes(rs.getInt("play_time_minutes"));
                data.setAchievementsCount(rs.getInt("achievements_count"));
                data.setLastX(rs.getDouble("last_x"));
                data.setLastY(rs.getDouble("last_y"));
                data.setLastZ(rs.getDouble("last_z"));
            }

            rs.close();
            ps.close();
            return data;
        } catch (SQLException e) {
            logger.severe("プレイヤー統計取得中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ランキング取得（キル数、移動距離、実績）
    public List<PlayerData> getTopPlayers(String type, String period) {
        List<PlayerData> playerDataList = new ArrayList<>();

        try {
            // 期間の日付範囲を計算
            LocalDate today = LocalDate.now();
            LocalDate startDate;

            if ("weekly".equalsIgnoreCase(period)) {
                startDate = today.minusDays(7);
            } else if ("monthly".equalsIgnoreCase(period)) {
                startDate = today.minusMonths(1);
            } else {
                return playerDataList; // 無効な期間
            }

            String startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String endDateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

            // クエリを作成
            String statColumn;
            switch (type.toLowerCase()) {
                case "kill":
                    statColumn = "SUM(ps.kills)";
                    break;
                case "distance":
                    statColumn = "SUM(ps.distance_traveled)";
                    break;
                case "achievements":
                    statColumn = "SUM(ps.achievements_count)";
                    break;
                default:
                    return playerDataList; // 無効なタイプ
            }

            String query = "SELECT p.minecraft_name, p.minecraft_uuid, " + statColumn + " as total " +
                    "FROM players p " +
                    "JOIN player_stats ps ON p.minecraft_uuid = ps.minecraft_uuid " +
                    "WHERE ps.date BETWEEN ? AND ? " +
                    "GROUP BY p.minecraft_uuid " +
                    "ORDER BY total DESC " +
                    "LIMIT 10";

            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, startDateStr);
            ps.setString(2, endDateStr);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                PlayerData data = new PlayerData();
                data.setMinecraftUuid(rs.getString("minecraft_uuid"));
                data.setMinecraftName(rs.getString("minecraft_name"));

                // 統計値を設定
                double total = rs.getDouble("total");
                switch (type.toLowerCase()) {
                    case "kill":
                        data.setKills((int) total);
                        break;
                    case "distance":
                        data.setDistanceTraveled(total);
                        break;
                    case "achievements":
                        data.setAchievementsCount((int) total);
                        break;
                }

                playerDataList.add(data);
            }

            rs.close();
            ps.close();
        } catch (SQLException e) {
            logger.severe("ランキング取得中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }

        return playerDataList;
    }

    // 全プレイヤーの統計を取得
    public List<PlayerData> getAllPlayersStats() {
        List<PlayerData> playerDataList = new ArrayList<>();

        try {
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            PreparedStatement ps = connection.prepareStatement(
                    "SELECT p.minecraft_name, ps.* FROM players p " +
                            "JOIN player_stats ps ON p.minecraft_uuid = ps.minecraft_uuid " +
                            "WHERE ps.date = ?"
            );
            ps.setString(1, today);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                PlayerData data = new PlayerData();
                data.setMinecraftUuid(rs.getString("minecraft_uuid"));
                data.setMinecraftName(rs.getString("minecraft_name"));
                data.setKills(rs.getInt("kills"));
                data.setDeaths(rs.getInt("deaths"));
                data.setDistanceTraveled(rs.getDouble("distance_traveled"));
                data.setDailyDistance(rs.getDouble("daily_distance"));
                data.setPlayTimeMinutes(rs.getInt("play_time_minutes"));
                data.setAchievementsCount(rs.getInt("achievements_count"));

                playerDataList.add(data);
            }

            rs.close();
            ps.close();
        } catch (SQLException e) {
            logger.severe("全プレイヤー統計取得中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }

        return playerDataList;
    }

    // 日次統計をリセット
    public void resetDailyStats() {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE player_stats SET daily_distance = 0 WHERE date = ?"
            );
            ps.setString(1, today);
            ps.executeUpdate();
            ps.close();

            logger.info("日次統計がリセットされました。");
        } catch (SQLException e) {
            logger.severe("日次統計リセット中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
package minecraftdiscord.listeners;

import minecraftdiscord.MinecraftDiscordPlugin;
import minecraftdiscord.database.DatabaseManager;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatListener implements Listener {

    private final MinecraftDiscordPlugin plugin;
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Long> joinTimes = new HashMap<>();

    public PlayerStatListener(MinecraftDiscordPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 接続時間を記録
        joinTimes.put(uuid, System.currentTimeMillis());

        // 最初の位置を記録
        lastLocations.put(uuid, player.getLocation());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // プレイ時間の計算と保存
        if (joinTimes.containsKey(uuid)) {
            long joinTime = joinTimes.get(uuid);
            long quitTime = System.currentTimeMillis();
            long playTimeMillis = quitTime - joinTime;
            int playTimeMinutes = (int) (playTimeMillis / (1000 * 60));

            // プレイ時間を更新
            if (playTimeMinutes > 0) {
                DatabaseManager dbManager = plugin.getDatabaseManager();
                dbManager.updatePlayTime(player, playTimeMinutes);
            }

            // データを削除
            joinTimes.remove(uuid);
        }

        // 最後の位置を保存
        DatabaseManager dbManager = plugin.getDatabaseManager();
        dbManager.updatePosition(player);

        // ロケーションキャッシュをクリア
        lastLocations.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location from = event.getFrom();
        Location to = event.getTo();

        // プレイヤーが実際に移動した場合（x/y/z座標の変化）
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            // 前回の位置がある場合
            if (lastLocations.containsKey(uuid)) {
                Location lastLoc = lastLocations.get(uuid);

                // 移動距離の計算（3Dユークリッド距離）
                double distance = 0;
                // 同じワールドの場合のみ計算
                if (lastLoc.getWorld().equals(to.getWorld())) {
                    distance = lastLoc.distance(to);
                }

                // 距離が大きすぎる場合はテレポートと判断して無視
                if (distance > 0 && distance < 100) {
                    DatabaseManager dbManager = plugin.getDatabaseManager();
                    dbManager.updateDistanceTraveled(player, distance);
                }
            }

            // 現在の位置を保存
            lastLocations.put(uuid, to.clone());
        }

        // 10ブロック移動ごとに座標を保存（パフォーマンスのため）
        if (lastLocations.containsKey(uuid)) {
            Location lastSavedLoc = lastLocations.get(uuid);
            if (lastSavedLoc.distanceSquared(to) > 100) { // 10ブロックの二乗 = 100
                DatabaseManager dbManager = plugin.getDatabaseManager();
                dbManager.updatePosition(player);
                lastLocations.put(uuid, to.clone());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // プレイヤーが敵を倒した場合
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();

            // ボス以外の敵モブのみをカウント
            if (event.getEntityType() != EntityType.PLAYER &&
                    event.getEntityType() != EntityType.ARMOR_STAND &&
                    event.getEntityType() != EntityType.ITEM_FRAME) {

                DatabaseManager dbManager = plugin.getDatabaseManager();
                dbManager.incrementKills(killer);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        DatabaseManager dbManager = plugin.getDatabaseManager();
        dbManager.incrementDeaths(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        String advancementKey = event.getAdvancement().getKey().toString();

        // レシピ解放は除外
        if (!advancementKey.contains("recipes")) {
            DatabaseManager dbManager = plugin.getDatabaseManager();
            dbManager.addAchievement(player, advancementKey);
        }
    }
}
package minecraftdiscord.scheduler;

import minecraftdiscord.MinecraftDiscordPlugin;
import minecraftdiscord.database.DatabaseManager;
import minecraftdiscord.database.PlayerData;
import minecraftdiscord.discord.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DailyStatsTask {

    private final MinecraftDiscordPlugin plugin;
    private final Logger logger;

    public DailyStatsTask(MinecraftDiscordPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void schedule() {
        // 毎日24:00（00:00）に実行するようにスケジュール
        long initialDelay = getMillisUntilMidnight();
        long period = TimeUnit.DAYS.toMillis(1); // 24時間ごと

        // タスクをスケジュール
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    logger.info("日次統計タスクを実行中...");

                    // 統計データを処理
                    processDailyStats();

                    logger.info("日次統計タスクが完了しました。");
                } catch (Exception e) {
                    logger.severe("日次統計タスク実行中にエラーが発生しました: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskTimerAsynchronously(plugin, initialDelay / 50, period / 50); // Bukkitのtickに変換（1 tick = 50ms）

        logger.info("日次統計タスクがスケジュールされました。次回実行まで: " + formatDuration(initialDelay));
    }

    private void processDailyStats() {
        DatabaseManager dbManager = plugin.getDatabaseManager();
        DiscordBot discordBot = plugin.getDiscordBot();

        // 全プレイヤーの統計を取得
        List<PlayerData> allPlayerStats = dbManager.getAllPlayersStats();

        if (!allPlayerStats.isEmpty()) {
            // Discord にメッセージを送信
            TextChannel channel = discordBot.getChatBridgeChannel();
            if (channel != null) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("本日のサーバー統計");
                embed.setColor(Color.MAGENTA);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                String today = LocalDateTime.now().format(formatter);
                embed.setDescription("**" + today + "**の統計情報");

                StringBuilder playerStatsBuilder = new StringBuilder();
                for (PlayerData data : allPlayerStats) {
                    String playerStats = String.format("**%s**\n" +
                                    "キル: %d | 死亡: %d | 移動距離: %sブロック | プレイ時間: %s\n",
                            data.getMinecraftName(),
                            data.getKills(),
                            data.getDeaths(),
                            data.getFormattedDailyDistance(),
                            data.getFormattedPlayTime());

                    // Discordのメッセージには文字数制限があるので、長すぎる場合は分割する
                    if (playerStatsBuilder.length() + playerStats.length() > 1900) {
                        embed.addField("プレイヤー統計", playerStatsBuilder.toString(), false);
                        playerStatsBuilder = new StringBuilder();
                    }

                    playerStatsBuilder.append(playerStats).append("\n");
                }

                if (playerStatsBuilder.length() > 0) {
                    embed.addField("プレイヤー統計", playerStatsBuilder.toString(), false);
                }

                embed.setFooter("毎日の統計 | 自動生成", null);

                channel.sendMessageEmbeds(embed.build()).queue();

                // 日次距離統計をリセット
                dbManager.resetDailyStats();
            }
        }
    }

    private long getMillisUntilMidnight() {
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(zoneId);

        return ChronoUnit.MILLIS.between(now, midnight);
    }

    private String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        return String.format("%02d時間%02d分%02d秒", hours, minutes, seconds);
    }
}
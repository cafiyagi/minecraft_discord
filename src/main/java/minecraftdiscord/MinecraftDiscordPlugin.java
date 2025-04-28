package minecraftdiscord;

import minecraftdiscord.commands.PosCommand;
import minecraftdiscord.database.DatabaseManager;
import minecraftdiscord.discord.DiscordBot;
import minecraftdiscord.listeners.MinecraftChatListener;
import minecraftdiscord.listeners.PlayerStatListener;
import minecraftdiscord.scheduler.DailyStatsTask;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Logger;

public class MinecraftDiscordPlugin extends JavaPlugin {

    private static MinecraftDiscordPlugin instance;
    private DiscordBot discordBot;
    private DatabaseManager databaseManager;
    private Logger logger;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        // インスタンスを保存
        instance = this;
        logger = getLogger();

        // 設定ファイルを保存
        saveDefaultConfig();
        config = getConfig();

        // データベースマネージャーを初期化
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Discord Botを初期化
        String botToken = config.getString("discord.token");
        String guildId = config.getString("discord.guild_id");

        if (botToken == null || botToken.isEmpty()) {
            logger.severe("Discord botトークンが設定されていません。config.ymlを確認してください。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        discordBot = new DiscordBot(this, botToken, guildId);
        discordBot.start();

        // コマンドを登録
        Objects.requireNonNull(getCommand("pos")).setExecutor(new PosCommand(this));

        // リスナーを登録
        getServer().getPluginManager().registerEvents(new MinecraftChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerStatListener(this), this);

        // 毎日の統計タスクをスケジュール
        new DailyStatsTask(this).schedule();

        logger.info("MinecraftDiscordPluginが有効化されました！");
    }

    @Override
    public void onDisable() {
        // Discord Botをシャットダウン
        if (discordBot != null) {
            discordBot.shutdown();
        }

        // データベース接続をクローズ
        if (databaseManager != null) {
            databaseManager.close();
        }

        logger.info("MinecraftDiscordPluginが無効化されました！");
    }

    // 他のクラスからインスタンスにアクセスするためのgetter
    public static MinecraftDiscordPlugin getInstance() {
        return instance;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
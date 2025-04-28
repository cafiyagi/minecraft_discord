package com.example.minecraftdiscord.discord;

import com.example.minecraftdiscord.MinecraftDiscordPlugin;
import com.example.minecraftdiscord.discord.commands.*;
import com.example.minecraftdiscord.listeners.DiscordChatListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.EnumSet;
import java.util.logging.Logger;

public class DiscordBot {

    private final MinecraftDiscordPlugin plugin;
    private final Logger logger;
    private final String token;
    private final String guildId;
    private JDA jda;
    private TextChannel chatBridgeChannel;

    public DiscordBot(MinecraftDiscordPlugin plugin, String token, String guildId) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.token = token;
        this.guildId = guildId;
    }

    public void start() {
        try {
            // JDAの構築とイベント設定
            jda = JDABuilder.createDefault(token)
                    .setActivity(Activity.playing("Minecraft"))
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT
                    )
                    .enableCache(EnumSet.of(
                            CacheFlag.MEMBER_OVERRIDES,
                            CacheFlag.VOICE_STATE,
                            CacheFlag.EMOTE
                    ))
                    .build();

            // 起動を待機
            jda.awaitReady();

            // チャットブリッジチャンネルを設定
            String channelId = plugin.getConfig().getString("discord.chat_bridge_channel");
            if (channelId != null && !channelId.isEmpty()) {
                chatBridgeChannel = jda.getTextChannelById(channelId);
                if (chatBridgeChannel == null) {
                    logger.warning("設定されたチャットブリッジチャンネルIDが無効です: " + channelId);
                }
            }

            // Discordのスラッシュコマンドを登録
            registerCommands();

            // チャットリスナーを登録
            jda.addEventListener(new DiscordChatListener(plugin));

            logger.info("Discord Botが正常に起動しました!");
        } catch (Exception e) {
            logger.severe("Discord Botの起動中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerCommands() {
        if (guildId == null || guildId.isEmpty()) {
            logger.warning("Discord guild IDが設定されていないため、スラッシュコマンドを登録できません。");
            return;
        }

        try {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) {
                logger.warning("指定されたIDのギルドが見つかりません: " + guildId);
                return;
            }

            // スラッシュコマンドを登録
            guild.updateCommands().addCommands(
                    Commands.slash("regist", "マインクラフトIDを登録する")
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "minecraft_id", "あなたのマインクラフトID", true),
                    Commands.slash("count", "サーバー内で倒した敵の数や死亡回数、移動距離などを表示"),
                    Commands.slash("time", "サーバー内でのプレイ時間を表示"),
                    Commands.slash("rec", "獲得した実績を表示"),
                    Commands.slash("pos", "現在の座標を表示"),
                    Commands.slash("top", "プレイヤーのランキングを表示")
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "type", "ランキングタイプ (kill, distance, achievements)", true)
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "period", "期間 (weekly, monthly)", true)
            ).queue();

            // コマンドハンドラを登録
            jda.addEventListener(
                    new RegistCommand(plugin),
                    new CountCommand(plugin),
                    new TimeCommand(plugin),
                    new RecCommand(plugin),
                    new PosCommand(plugin),
                    new TopCommand(plugin)
            );

            logger.info("Discord スラッシュコマンドが正常に登録されました！");
        } catch (Exception e) {
            logger.severe("Discord コマンドの登録中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            logger.info("Discord Botがシャットダウンしました。");
        }
    }

    public void sendMessageToMinecraft(String message) {
        plugin.getServer().broadcastMessage("[Discord] " + message);
    }

    public void sendMessageToDiscord(String message) {
        if (chatBridgeChannel != null) {
            chatBridgeChannel.sendMessage(message).queue();
        }
    }

    public JDA getJda() {
        return jda;
    }

    public TextChannel getChatBridgeChannel() {
        return chatBridgeChannel;
    }
}
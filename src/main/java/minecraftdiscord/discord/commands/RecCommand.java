package minecraftdiscord.discord.commands;

import minecraftdiscord.MinecraftDiscordPlugin;
import minecraftdiscord.database.DatabaseManager;
import minecraftdiscord.database.PlayerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;

public class RecCommand extends ListenerAdapter {

    private final MinecraftDiscordPlugin plugin;

    public RecCommand(MinecraftDiscordPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("rec")) return;

        event.deferReply().queue();

        String discordId = event.getUser().getId();
        DatabaseManager dbManager = plugin.getDatabaseManager();

        // Discord IDからMinecraft UUIDを取得
        String minecraftUuid = dbManager.getMinecraftUuidByDiscordId(discordId);

        if (minecraftUuid == null) {
            event.getHook().editOriginal("あなたはMinecraftアカウントと連携されていません。`/regist`コマンドで連携してください。").queue();
            return;
        }

        // プレイヤーの統計データを取得
        PlayerData playerData = dbManager.getPlayerStats(minecraftUuid);

        if (playerData == null) {
            event.getHook().editOriginal("統計データの取得に失敗しました。もう一度お試しください。").queue();
            return;
        }

        // 実績情報を表示
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(playerData.getMinecraftName() + "の実績");
        embed.setColor(Color.YELLOW);
        embed.addField("獲得実績数", String.valueOf(playerData.getAchievementsCount()), false);
        // 注：実績の詳細リストが必要な場合はここで追加
        embed.setFooter("統計情報 | " + java.time.LocalDate.now(), null);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }
}
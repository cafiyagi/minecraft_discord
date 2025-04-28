package minecraftdiscord.discord.commands;

import minecraftdiscord.MinecraftDiscordPlugin;
import minecraftdiscord.database.DatabaseManager;
import minecraftdiscord.database.PlayerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;

public class TimeCommand extends ListenerAdapter {

    private final MinecraftDiscordPlugin plugin;

    public TimeCommand(MinecraftDiscordPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("time")) return;

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

        // プレイ時間情報を表示
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(playerData.getMinecraftName() + "のプレイ時間");
        embed.setColor(Color.BLUE);
        embed.addField("総プレイ時間", playerData.getFormattedPlayTime(), false);
        embed.setFooter("統計情報 | " + java.time.LocalDate.now(), null);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }
}
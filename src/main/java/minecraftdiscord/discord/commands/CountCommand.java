package com.example.minecraftdiscord.discord.commands;

import com.example.minecraftdiscord.MinecraftDiscordPlugin;
import com.example.minecraftdiscord.database.DatabaseManager;
import com.example.minecraftdiscord.database.PlayerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;

public class CountCommand extends ListenerAdapter {

    private final MinecraftDiscordPlugin plugin;

    public CountCommand(MinecraftDiscordPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("count")) return;

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

        // 統計情報を表示
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(playerData.getMinecraftName() + "の統計情報");
        embed.setColor(Color.GREEN);
        embed.addField("敵を倒した回数", String.valueOf(playerData.getKills()), true);
        embed.addField("死亡回数", String.valueOf(playerData.getDeaths()), true);
        embed.addField("移動総距離", playerData.getFormattedDistance() + "ブロック", true);
        embed.addField("本日の移動距離", playerData.getFormattedDailyDistance() + "ブロック", true);
        embed.setFooter("統計情報 | " + java.time.LocalDate.now(), null);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }
}
package com.example.minecraftdiscord.discord.commands;

import com.example.minecraftdiscord.MinecraftDiscordPlugin;
import com.example.minecraftdiscord.database.DatabaseManager;
import com.example.minecraftdiscord.database.PlayerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;

public class PosCommand extends ListenerAdapter {

    private final MinecraftDiscordPlugin plugin;

    public PosCommand(MinecraftDiscordPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("pos")) return;

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
            event.getHook().editOriginal("座標データの取得に失敗しました。もう一度お試しください。").queue();
            return;
        }

        // 座標情報を表示
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(playerData.getMinecraftName() + "の最後の位置");
        embed.setColor(Color.ORANGE);
        embed.addField("座標", playerData.getFormattedPosition(), false);
        embed.setFooter("座標情報 | " + java.time.LocalDate.now(), null);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }
}
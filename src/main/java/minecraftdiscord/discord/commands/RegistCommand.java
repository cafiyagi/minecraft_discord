package com.example.minecraftdiscord.discord.commands;

import com.example.minecraftdiscord.MinecraftDiscordPlugin;
import com.example.minecraftdiscord.database.DatabaseManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class RegistCommand extends ListenerAdapter {

    private final MinecraftDiscordPlugin plugin;

    public RegistCommand(MinecraftDiscordPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("regist")) return;

        // コマンドの処理を遅延させる（Discord APIの制限による）
        event.deferReply().queue();

        String minecraftId = event.getOption("minecraft_id").getAsString();
        String discordId = event.getUser().getId();

        // Bukkitの処理はメインスレッドで実行する必要がある
        Bukkit.getScheduler().runTask(plugin, () -> {
            // MinecraftのプレイヤーUUIDを検索
            OfflinePlayer player = Bukkit.getOfflinePlayer(minecraftId);

            if (player == null || !player.hasPlayedBefore()) {
                // プレイヤーが見つからない場合
                event.getHook().editOriginal("プレイヤー「" + minecraftId + "」は見つかりませんでした。正確なMinecraft IDを入力してください。").queue();
                return;
            }

            // データベースに登録
            UUID uuid = player.getUniqueId();
            DatabaseManager dbManager = plugin.getDatabaseManager();
            boolean success = dbManager.registerPlayer(uuid.toString(), player.getName(), discordId);

            if (success) {
                event.getHook().editOriginal("**登録成功！**\nMinecraft ID: " + player.getName() + " を Discord ID: " + event.getUser().getAsTag() + " に連携しました！").queue();
            } else {
                event.getHook().editOriginal("登録に失敗しました。もう一度お試しください。").queue();
            }
        });
    }
}
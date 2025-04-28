package com.example.minecraftdiscord.listeners;

import com.example.minecraftdiscord.MinecraftDiscordPlugin;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordChatListener extends ListenerAdapter {

    private final MinecraftDiscordPlugin plugin;

    public DiscordChatListener(MinecraftDiscordPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Botからのメッセージは無視
        if (event.getAuthor().isBot()) return;

        // 設定されたチャンネル以外は無視
        TextChannel configChannel = plugin.getDiscordBot().getChatBridgeChannel();
        if (configChannel == null || event.getChannel().getIdLong() != configChannel.getIdLong()) return;

        // メッセージを取得
        String username = event.getAuthor().getName();
        String content = event.getMessage().getContentDisplay();

        // マインクラフトに送信
        plugin.getDiscordBot().sendMessageToMinecraft(username + ": " + content);
    }
}
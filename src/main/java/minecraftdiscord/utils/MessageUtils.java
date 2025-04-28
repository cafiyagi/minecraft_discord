package com.example.minecraftdiscord.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.bukkit.ChatColor;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageUtils {

    // Discordメッセージの最大文字数
    private static final int MAX_DISCORD_CONTENT_LENGTH = 2000;
    private static final int MAX_EMBED_DESCRIPTION_LENGTH = 4096;
    private static final int MAX_EMBED_FIELD_VALUE_LENGTH = 1024;

    // タイムスタンプのフォーマッター
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    /**
     * Minecraft用のメッセージをフォーマット
     */
    public static String formatMinecraftMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * 長いDiscordメッセージを最大長に制限
     */
    public static String truncateDiscordMessage(String message) {
        if (message.length() <= MAX_DISCORD_CONTENT_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_DISCORD_CONTENT_LENGTH - 3) + "...";
    }

    /**
     * 標準的なDiscord埋め込みを作成
     */
    public static EmbedBuilder createDefaultEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title);

        if (description != null && !description.isEmpty()) {
            // 説明が長すぎる場合は切り詰める
            if (description.length() > MAX_EMBED_DESCRIPTION_LENGTH) {
                description = description.substring(0, MAX_EMBED_DESCRIPTION_LENGTH - 3) + "...";
            }
            embed.setDescription(description);
        }

        embed.setColor(Color.GREEN);
        embed.setFooter("MinecraftDiscord | " + LocalDateTime.now().format(TIMESTAMP_FORMATTER), null);

        return embed;
    }

    /**
     * エラー用のDiscord埋め込みを作成
     */
    public static MessageEmbed createErrorEmbed(String title, String errorMessage) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title);

        if (errorMessage != null && !errorMessage.isEmpty()) {
            // エラーメッセージが長すぎる場合は切り詰める
            if (errorMessage.length() > MAX_EMBED_DESCRIPTION_LENGTH) {
                errorMessage = errorMessage.substring(0, MAX_EMBED_DESCRIPTION_LENGTH - 3) + "...";
            }
            embed.setDescription(errorMessage);
        }

        embed.setColor(Color.RED);
        embed.setFooter("エラー | " + LocalDateTime.now().format(TIMESTAMP_FORMATTER), null);

        return embed.build();
    }

    /**
     * 埋め込みフィールドの値を安全に追加（長すぎる場合は分割）
     */
    public static void addFieldSafely(EmbedBuilder embed, String name, String value, boolean inline) {
        if (value == null || value.isEmpty()) {
            value = "N/A";
        }

        // 値が最大長を超える場合は分割
        if (value.length() > MAX_EMBED_FIELD_VALUE_LENGTH) {
            int parts = (int) Math.ceil((double) value.length() / MAX_EMBED_FIELD_VALUE_LENGTH);

            for (int i = 0; i < parts; i++) {
                int start = i * MAX_EMBED_FIELD_VALUE_LENGTH;
                int end = Math.min(start + MAX_EMBED_FIELD_VALUE_LENGTH, value.length());

                String partValue = value.substring(start, end);
                String partName = (i == 0) ? name : name + " (続き " + (i + 1) + ")";

                embed.addField(partName, partValue, inline);
            }
        } else {
            embed.addField(name, value, inline);
        }
    }

    /**
     * DiscordメッセージからMinecraftコマンドとして不適切な文字を削除
     */
    public static String sanitizeForMinecraft(String message) {
        // 改行を空白に置き換え
        message = message.replace("\n", " ")
                // セクション記号（§）を削除
                .replace("§", "")
                // 制御文字を削除
                .replaceAll("[\\p{Cntrl}]", "");

        return message;
    }
}
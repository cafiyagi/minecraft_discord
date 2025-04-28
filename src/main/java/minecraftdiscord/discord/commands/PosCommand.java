package minecraftdiscord.discord.commands;

import minecraftdiscord.MinecraftDiscordPlugin;
import minecraftdiscord.database.DatabaseManager;
import minecraftdiscord.database.PlayerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;
import java.util.List;

public class TopCommand extends ListenerAdapter {

    private final MinecraftDiscordPlugin plugin;

    public TopCommand(MinecraftDiscordPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("top")) return;

        event.deferReply().queue();

        String type = event.getOption("type").getAsString().toLowerCase();
        String period = event.getOption("period").getAsString().toLowerCase();

        // タイプと期間の検証
        if (!isValidType(type) || !isValidPeriod(period)) {
            event.getHook().editOriginal("無効なパラメータです。タイプは `kill`, `distance`, `achievements` のいずれか、期間は `weekly` または `monthly` を指定してください。").queue();
            return;
        }

        DatabaseManager dbManager = plugin.getDatabaseManager();
        List<PlayerData> topPlayers = dbManager.getTopPlayers(type, period);

        if (topPlayers.isEmpty()) {
            event.getHook().editOriginal("ランキングデータがありません。もう少し待ってから試してください。").queue();
            return;
        }

        // ランキング表示
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(getJapaneseTitle(type, period));
        embed.setColor(getColorByType(type));

        StringBuilder description = new StringBuilder();
        for (int i = 0; i < topPlayers.size(); i++) {
            PlayerData player = topPlayers.get(i);
            description.append(String.format("**%d位**: %s - %s\n",
                    i + 1,
                    player.getMinecraftName(),
                    getFormattedValue(player, type)));
        }

        embed.setDescription(description.toString());
        embed.setFooter("ランキング | " + java.time.LocalDate.now(), null);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private boolean isValidType(String type) {
        return type.equals("kill") || type.equals("distance") || type.equals("achievements");
    }

    private boolean isValidPeriod(String period) {
        return period.equals("weekly") || period.equals("monthly");
    }

    private String getJapaneseTitle(String type, String period) {
        String typeStr;
        switch (type) {
            case "kill":
                typeStr = "キル数";
                break;
            case "distance":
                typeStr = "移動距離";
                break;
            case "achievements":
                typeStr = "実績獲得数";
                break;
            default:
                typeStr = "";
        }

        String periodStr = period.equals("weekly") ? "週間" : "月間";

        return periodStr + typeStr + "ランキング";
    }

    private Color getColorByType(String type) {
        switch (type) {
            case "kill":
                return Color.RED;
            case "distance":
                return Color.BLUE;
            case "achievements":
                return Color.YELLOW;
            default:
                return Color.GREEN;
        }
    }

    private String getFormattedValue(PlayerData player, String type) {
        switch (type) {
            case "kill":
                return player.getKills() + "キル";
            case "distance":
                return player.getFormattedDistance() + "ブロック";
            case "achievements":
                return player.getAchievementsCount() + "個";
            default:
                return "";
        }
    }
}
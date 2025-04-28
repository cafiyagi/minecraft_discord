package minecraftdiscord.listeners;

import minecraftdiscord.MinecraftDiscordPlugin;
import minecraftdiscord.discord.DiscordBot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class MinecraftChatListener implements Listener {

    private final MinecraftDiscordPlugin plugin;

    public MinecraftChatListener(MinecraftDiscordPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Discordにチャットを送信
        DiscordBot discordBot = plugin.getDiscordBot();
        if (discordBot != null) {
            String formattedMessage = String.format("**%s**: %s", player.getName(), message);
            discordBot.sendMessageToDiscord(formattedMessage);
        }
    }
}
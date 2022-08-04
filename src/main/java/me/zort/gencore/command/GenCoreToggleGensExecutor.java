package me.zort.gencore.command;

import me.zort.gencore.GenCore;
import me.zort.gencore.cache.PlayerDataCache;
import me.zort.gencore.configuration.ConfigurationRegistry;
import me.zort.gencore.configuration.messages.ConfigurationImplMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

// Permissions: gencore.togglegens
public class GenCoreToggleGensExecutor implements CommandExecutor {

    private final GenCore plugin = GenCore.getSingleton();
    private final ConfigurationImplMessages messages = ConfigurationRegistry.getConfiguration(ConfigurationImplMessages.class)
            .get();
    private final PlayerDataCache playerCache = plugin.getPlayerCache();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)) {
            messages.send(sender, ConfigurationImplMessages.Message.SENDER_IS_NOT_PLAYER);
            return true;
        }
        if(!sender.hasPermission("gencore.togglegens")) {
            messages.send(sender, ConfigurationImplMessages.Message.NO_PERMISSION);
            return true;
        }
        Player p = (Player) sender;
        String nick = p.getName();
        playerCache.modifyPlayerDataAsync(nick, playerData -> {
            boolean generatorsEnabled = playerData.isGeneratorsEnabled();
            playerData.setGeneratorsEnabled(!generatorsEnabled);
        }).whenComplete((data, ex) -> {
            if(ex != null || data == null) {
                messages.send(sender, ConfigurationImplMessages.Message.SOMETHING_WENT_WRONG);
                return;
            }
            boolean val = data.isGeneratorsEnabled();
            messages.send(sender,
                    val ? ConfigurationImplMessages.Message.GENS_ENABLED : ConfigurationImplMessages.Message.GENS_DISABLED);
        });
        return true;
    }

}

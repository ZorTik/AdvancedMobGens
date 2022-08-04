package me.zort.gencore.command;

import me.zort.gencore.configuration.ConfigurationRegistry;
import me.zort.gencore.configuration.config.ConfigurationImplConfig;
import me.zort.gencore.configuration.messages.ConfigurationImplMessages;
import me.zort.gencore.gui.virtual.VirtualStorageGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GenCoreStorageExecutor implements CommandExecutor {

    private final ConfigurationImplConfig config = ConfigurationRegistry.getConfiguration(ConfigurationImplConfig.class).get();
    private final ConfigurationImplMessages messages = ConfigurationRegistry.getConfiguration(ConfigurationImplMessages.class).get();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)) {
            messages.send(sender, ConfigurationImplMessages.Message.SENDER_IS_NOT_PLAYER);
            return true;
        }
        Player p = (Player) sender;
        new VirtualStorageGUI(config).open(p);
        return true;
    }

}

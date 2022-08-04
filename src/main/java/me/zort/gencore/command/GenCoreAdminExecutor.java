package me.zort.gencore.command;

import me.zort.gencore.GenCore;
import me.zort.gencore.GenCoreListener;
import me.zort.gencore.cache.PlayerDataCache;
import me.zort.gencore.configuration.ConfigurationRegistry;
import me.zort.gencore.configuration.config.ConfigurationImplConfig;
import me.zort.gencore.configuration.messages.ConfigurationImplMessages;
import me.zort.gencore.data.entity.PlayerData;
import me.zort.gencore.object.Pair;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

// Permissions: gencore.admin
public class GenCoreAdminExecutor implements CommandExecutor {

    private final GenCore plugin = GenCore.getSingleton();
    private final ConfigurationImplConfig config = ConfigurationRegistry.getConfiguration(ConfigurationImplConfig.class)
            .get();
    private final ConfigurationImplMessages messages = ConfigurationRegistry.getConfiguration(ConfigurationImplMessages.class)
            .get();
    private final PlayerDataCache playerCache = plugin.getPlayerCache();
    private final GenCoreListener listener = GenCoreListener.getSingleton();
    private final Logger logger = plugin.getLogger();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.hasPermission("gencore.admin")) {
            messages.send(sender, ConfigurationImplMessages.Message.NO_PERMISSION);
            return true;
        }
        if(args.length == 3 && args[0].equalsIgnoreCase("modifygenlimit")) {
            String nick = args[1];
            String modString = args[2];
            int mod;
            try {
                mod = Integer.parseInt(modString);
            } catch(NumberFormatException ex) {
                messages.send(sender, ConfigurationImplMessages.Message.PARAM_IS_NOT_NUM);
                return true;
            }
            PlayerData pd = playerCache.getPlayerData(nick);
            if(pd != null && pd.getGeneratorsLimit() + mod < 0) {
                messages.send(sender, ConfigurationImplMessages.Message.NUM_RES_CANT_BE_LOWER_THAN_ZERO);
                return true;
            }
            playerCache.modifyPlayerDataAsync(nick, playerData -> {
                int generatorsLimit = playerData.getGeneratorsLimit();
                playerData.setGeneratorsLimit(generatorsLimit + mod);
            }).whenComplete((data, ex) -> {
                if(ex != null || data == null) {
                    messages.send(sender, ConfigurationImplMessages.Message.SOMETHING_WENT_WRONG);
                    return;
                }
                messages.send(sender, ConfigurationImplMessages.Message.GEN_LIMIT_CHANGES, new Pair<>("%player%", nick), new Pair<>("%amount%", mod));
            });
        } else if(args.length == 3 && args[0].equalsIgnoreCase("setmodifier")) {
            String nick = args[1];
            String modString = args[2];
            double mod;
            try {
                mod = Double.parseDouble(modString);
            } catch(NumberFormatException ex) {
                messages.send(sender, ConfigurationImplMessages.Message.PARAM_IS_NOT_DOUBLE);
                return true;
            }
            PlayerData pd = playerCache.getPlayerData(nick);
            if(pd != null && mod < 0.0) {
                messages.send(sender, ConfigurationImplMessages.Message.NUM_LOWER_THAN_ZERO);
                return true;
            }
            playerCache.modifyPlayerDataAsync(nick, playerData -> playerData.setStorageModifier(mod)).whenComplete((data, ex) -> {
                if(ex != null || data == null) {
                    messages.send(sender, ConfigurationImplMessages.Message.SOMETHING_WENT_WRONG);
                    return;
                }
                messages.send(sender, ConfigurationImplMessages.Message.MODIFIER_CHANGED, new Pair<>("%player%", nick), new Pair<>("%amount%", mod));
            });
        } else if(args.length == 2 && args[0].equalsIgnoreCase("genlimit")) {
            String nick = args[1];
            PlayerData playerData = playerCache.getPlayerData(nick);
            if(playerData == null) {
                messages.send(sender, ConfigurationImplMessages.Message.SOMETHING_WENT_WRONG);
                return true;
            }
            messages.send(sender, ConfigurationImplMessages.Message.GEN_LIMIT, new Pair<>("%player%", nick), new Pair<>("%limit%", playerData.getGeneratorsLimit()));
        } else if(args.length == 2 && args[0].equalsIgnoreCase("gensenabled")) {
            String nick = args[1];
            PlayerData playerData = playerCache.getPlayerData(nick);
            if(playerData == null) {
                messages.send(sender, ConfigurationImplMessages.Message.SOMETHING_WENT_WRONG);
                return true;
            }
            messages.send(sender, ConfigurationImplMessages.Message.GENS_ENABLED_INFO, new Pair<>("%player%", nick), new Pair<>("%status%", (playerData.isGeneratorsEnabled() ? "enabled" : "disabled")));
        } else if(args.length >= 2 && args[0].equalsIgnoreCase("givegenitem")) {
            String nick = args[1];
            String amountString = args.length == 2
                    ? String.valueOf(1)
                    : args[2];
            int amount;
            try {
                amount = Integer.parseInt(amountString);
            } catch(NumberFormatException ex) {
                messages.send(sender, ConfigurationImplMessages.Message.PARAM_IS_NOT_NUM);
                return true;
            }
            if(amount <= 0) {
                messages.send(sender, ConfigurationImplMessages.Message.NUM_LOWER_THAN_ONE);
                return true;
            }
            Player p = Bukkit.getPlayer(nick);
            if(p == null || !p.isOnline()) {
                messages.send(sender, ConfigurationImplMessages.Message.PLAYER_NOT_ONLINE, new Pair<>("%player%", nick));
                return true;
            }
            ItemStack item = listener.generatorItem();
            if(item == null) {
                messages.send(sender, ConfigurationImplMessages.Message.SOMETHING_WENT_WRONG);
                logger.info("Configured generator item in config is not valid! Check your configuration, please.");
                return true;
            }
            for(int i = 0; i < amount; i++) {
                p.getInventory().addItem(item);
            }
            messages.send(sender, ConfigurationImplMessages.Message.GIVEN_GEN_ITEM, new Pair<>("%player%", nick));
            messages.send(p, ConfigurationImplMessages.Message.OBTAINED_GEN_ITEM);
        } else if(args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            config.reload();
            messages.reload();
            messages.send(sender, ConfigurationImplMessages.Message.RELOADED);
        } else {
            messages.send(sender, ConfigurationImplMessages.Message.ADMIN_HELP);
        }
        return true;
    }

}

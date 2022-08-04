package me.zort.gencore.configuration.messages;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import me.zort.gencore.configuration.Configuration;
import me.zort.gencore.object.Pair;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigurationImplMessages implements Configuration {

    private final File file;
    @Nullable
    private FileConfiguration config;

    private final Map<Message, String> messagesCache;

    public ConfigurationImplMessages(File file) {
        this.file = file;
        this.config = null;
        this.messagesCache = Maps.newConcurrentMap();
    }

    public void send(CommandSender sender, Message type, Pair<String, ?>... args) {
        if(type.isList()) {
            getMessageList(type).stream()
                    .map(s -> {
                        String r = s;
                        for(Pair<String, ?> pair : args) {
                            r = r.replaceAll(pair.getFirst(), String.valueOf(pair.getSecond()));
                        }
                        return r;
                    })
                    .forEach(sender::sendMessage);
            return;
        }
        String r = getMessage(type);
        for(Pair<String, ?> pair : args) {
            r = r.replaceAll(pair.getFirst(), String.valueOf(pair.getSecond()));
        }
        sender.sendMessage(r);
    }

    public String getSingle(Message type, Pair<String, ?>... args) {
        if(type.isList()) return "";
        String r = getMessage(type);
        for(Pair<String, ?> pair : args) {
            r = r.replaceAll(pair.getFirst(), String.valueOf(pair.getSecond()));
        }
        return r;
    }

    public List<String> getMessageList(Message type) {
        return getMessageList(type, true);
    }

    public List<String> getMessageList(Message type, boolean colored) {
        String key = type.getKey();
        if(!isLoaded() || !config.contains(key)) return Lists.newArrayList();
        if(config.isSet(key)) {
            return config.getStringList(key).stream()
                    .map(s -> {
                        if (colored) {
                            return color(s);
                        }
                        return s;
                    })
                    .collect(Collectors.toList());
        } else return Lists.newArrayList(color(config.getString(key)));
    }

    public String getMessage(Message type) {
        return getMessage(type, true);
    }

    public String getMessage(Message type, boolean colored) {
        if(!isLoaded()) return "";
        assert config != null;
        String raw = config.getString(type.getKey(), type.getDef());
        return colored
                ? color(raw)
                : raw;
    }

    @Override
    public boolean reload() {
        if(!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        boolean edited = false;
        for(Message message : Message.values()) {
            if(!config.contains(message.getKey())) {
                String def = message.getDef();
                Object o = def;
                if(message.isList()) {
                    o = Arrays.stream(def.split("\n")).collect(Collectors.toList());
                }
                config.set(message.getKey(), o);
                edited = true;
            }
        }
        if(edited) {
            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        this.config = config;
        return true;
    }

    @Override
    public boolean isLoaded() {
        return config != null;
    }

    @Override
    public File getFile() {
        return file;
    }

    private String color(String s) {
        if(s == null) return null;
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Getter
    public enum Message {

        NO_PERMISSION("no-permission", "&c&l[!] &cYou don't have enough permission to do this!"),
        SOMETHING_WENT_WRONG("something-went-wrong", "&c&l[!] &cSomething went wrong!"),
        PLAYER_NOT_ONLINE("player-not-online", "&c&l[!] &cPlayer &4%player% &csi not online!"),
        SENDER_IS_NOT_PLAYER("sender-s-not-player", "&c&l[!] &cThis command can be used only as player!"),
        PARAM_IS_NOT_DOUBLE("param-is-not-double", "&c&l[!] &cParameter is not number or decimal number!"),
        PARAM_IS_NOT_NUM("param-is-not-num", "&c&l[!] &cParameter needs to be number!"),
        NUM_LOWER_THAN_ZERO("num-lower-than-zero", "&c&l[!] &cNumber cannot be lower than zero!"),
        NUM_LOWER_THAN_ONE("num-lower-than-one", "&c&l[!] &cNumber needs to be higher than 0!"),
        NUM_RES_CANT_BE_LOWER_THAN_ZERO("num-res-cant-be-lower-than-zero", "&c&l[!] &cResulting number can't be lower than zero!"),
        LIMIT_REACHED("limit-reached", "&c&l[!] &cYou have reached your generators limit!"),
        CANT_BREAK_OTHERS_GENS("cant-break-others-gens", "&c&l[!] &cYou can't break other's spawners!"),
        SOLD_ITEM("sold-item", "&6&l[!] &6Successfully sold item for &e$%price%&6!"),
        GENS_ENABLED("gens-enabled", "&a&l[!] &aYou have successfully &2enabled &ayour gens!"),
        GENS_DISABLED("gens-enabled", "&a&l[!] &aYou have successfully &cdisabled &ayour gens!"),
        MODIFIER_CHANGED("modifier-changed", "&a&l[!] &aModifier changed for player &6%player% &ato &e%amount%&a!"),
        GEN_LIMIT_CHANGES("gen-limit-changed", "&a&l[!] &aGenerators limit successfully changed for player &6%player% &aby &e%amount%&a!"),
        GEN_LIMIT("gen-limit", "&e&l[!] &eGen limit of player &6%player% &eis &a%limit%"),
        GENS_ENABLED_INFO("gen-enabled-info", "&e&l[!] &ePlayer &6%player% &ehas gens &6%status%"),
        GIVEN_GEN_ITEM("given-gen-item", "&a&l[!] &aSuccessfully gave generator item to player &6%player%&a!"),
        OBTAINED_GEN_ITEM("obtained-gen-item", "&a&l[!] &aObtained generator item!"),
        PLACED_GENERATOR("placed-generator", "&e&l[!] &eYou have placed new spawner!"),
        BROKEN_GENERATOR("broken-generator", "&e&l[!] &eYou have broke this spawner!"),
        GENERATOR_LEVEL_UP("generator-level-up", "&e&l[!] &eGenerator has been leveled up to level &6%level%&e!"),
        GENERATOR_MAX_LEVEL("generator-max-level", "&c&l[!] &cThis generator is on maximum level!"),
        RELOADED("reloaded", "&a&l[!] &aConfiguration has been reloaded!"),
        ENTITY_STACK_NAME("entity-stack-name", "&e%amount%x &6%name%"),
        ADMIN_HELP("admin-help", "&e&lGENCORE ADMIN&r &6~\n&6&l* &f/gencoreadmin &amodifygenlimit &6<nick> <change>\n&6&l* &f/gencoreadmin &asetmodifier &6<nick> <num>\n&6&l* &f/gencoreadmin &agenlimit &6<nick>\n&6&l* &f/gencoreadmin &agensenabled &6<nick>\n&6&l* &f/gencoreadmin &agivegenitem &6<nick> &b[amount]\n&6&l* &f/gencoreadmin &areload");

        private final String key;
        private final String def;

        Message(String key, String def) {
            this.key = key;
            this.def = def;
        }

        public boolean isList() {
            return def.contains("\n");
        }

    }

}

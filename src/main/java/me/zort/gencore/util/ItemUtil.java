package me.zort.gencore.util;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ItemUtil {

    public static ItemStack editNBT(ItemStack item, Consumer<NBTItem> consumer) {
        NBTItem nbtItem = getNBTItem(item);
        consumer.accept(nbtItem);
        return nbtItem.getItem();
    }

    public static <T> T getFromNBT(ItemStack item, Function<NBTItem, T> function) {
        NBTItem nbtItem = getNBTItem(item);
        return function.apply(nbtItem);
    }

    private static NBTItem getNBTItem(ItemStack item) {
        return new NBTItem(item);
    }

    @Nullable
    public static ItemStack fromConfigurationSection(ConfigurationSection section) {
        if(!section.contains("type")) {
            return null;
        }
        String typeString = section.getString("type");
        int amount = section.getInt("amount", 1);
        int data = section.getInt("data", 0);
        @Nullable
        ConfigurationSection cMeta = section.contains("meta")
                ? section.getConfigurationSection("meta")
                : null;
        Material material = Material.matchMaterial(typeString);
        if(material == null) {
            return null;
        }
        ItemStack stack = new ItemStack(material, amount, (short) data);
        ItemMeta meta = stack.getItemMeta();
        if(meta != null && cMeta != null) {
            if(cMeta.contains("display-name")) {
                String displayName = cMeta.getString("display-name");
                meta.setDisplayName(color(displayName));
            }
            if(cMeta.contains("lore")) {
                List<String> lore = cMeta.getStringList("lore")
                        .stream()
                        .map(ItemUtil::color)
                        .collect(Collectors.toList());
                meta.setLore(lore);
            }
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static String color(String s) {
        if(s == null) return null;
        return ChatColor.translateAlternateColorCodes('&', s);
    }

}

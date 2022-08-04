package me.zort.gencore.configuration.config;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.zort.gencore.configuration.ConfigurationValidatorInternal;
import me.zort.gencore.configuration.ConfiguredObject;
import org.apache.commons.lang.math.RandomUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Getter
public class ConfiguredMob extends ConfiguredObject {

    private EntityType entityType;
    private List<LootItem> loot;

    public ConfiguredMob(ConfigurationSection section) {
        super(section);
    }

    @Nullable
    @Override
    public String checkValid() {
        ConfigurationSection section = getSection();
        String mobName = getMobName();
        String errorOnValidation = ConfigurationValidatorInternal.validateLivingMobType(mobName);
        if(errorOnValidation != null) {
            return errorOnValidation;
        }
        if(section.contains("loot")) {
            ConfigurationSection lootSection = section.getConfigurationSection("loot");
            assert lootSection != null;
            Set<String> itemKeys = lootSection.getKeys(false);
            List<LootItem> itemList = itemKeys.stream()
                    .filter(s -> lootSection.contains(s) && lootSection.isConfigurationSection(s))
                    .map(s -> new LootItem(lootSection.getConfigurationSection(s)))
                    .collect(Collectors.toList());
            List<String> errors = itemList.stream()
                    .map(lootItem -> {
                        String init = lootItem.init();
                        if(init == null) return null;
                        return lootItem.getSection().getName() + ": " + init;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if(!errors.isEmpty()) {
                Joiner joiner = Joiner.on("; ");
                loot = Lists.newArrayList();
                return "[" + joiner.join(errors) + "]";
            } else {
                loot = itemList;
            }
        }
        return null;
    }

    @Override
    public void load() {
        String mobName = getMobName();
        this.entityType = EntityType.valueOf(mobName);
    }

    public List<LootItem> randomLoot(int amount) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double randomDouble = random.nextDouble(100.0);
        List<LootItem> possibleLoot = loot.stream()
                .filter(item -> item.getChance() >= randomDouble)
                .collect(Collectors.toList());
        if(possibleLoot.isEmpty()) {
            List<LootItem> temp = new ArrayList<>(loot);
            for(int i = 0; i < amount; i++) {
                if(temp.isEmpty()) {
                    break;
                }
                int iRand = random.nextInt(temp.size());
                LootItem lootItem = temp.get(iRand);
                possibleLoot.add(lootItem);
                temp.remove(lootItem);
            }
        }
        List<LootItem> result = Lists.newArrayList();
        for(int i = 0; i < amount; i++) {
            if(possibleLoot.isEmpty()) {
                break;
            }
            int iRand = random.nextInt(possibleLoot.size());
            LootItem lootItem = possibleLoot.get(iRand);
            result.add(lootItem);
        }
        return result;
    }

    public String getMobName() {
        return getSection().getName().toUpperCase();
    }

    public static class LootItem {

        private final ConfigurationSection s;

        private String type;
        private Material typeEnum;
        private int amount;
        private short data;
        @Getter
        private double sellPrice;
        @Getter
        private double chance;
        private Meta meta;

        private boolean success = false;

        private LootItem(ConfigurationSection s) {
            this.s = s;
        }

        public String init() {
            if(!s.contains("type") || !s.contains("chance")) {
                return "Item " + s.getName() + " does not contain type or chance!";
            }
            type = s.getString("type").toUpperCase();
            Material material = Material.matchMaterial(type);
            if(material == null) {
                return "Material " + type.toUpperCase() + " does not exist!";
            }
            typeEnum = material;
            amount = s.getInt("amount", 1);
            data = (short) s.getInt("data", 0);
            sellPrice = s.getDouble("sell-price", 0.0);
            chance = s.getDouble("chance");
            Meta meta;
            if(s.contains("meta")) {
                meta = new Meta(s.getConfigurationSection("meta"));
            } else {
                meta = new Meta(null, null, Lists.newArrayList());
            }
            this.meta = meta;
            if(meta.getSection() != null) {
                String errorMessage = meta.init();
                success = errorMessage == null;
                return errorMessage;
            } else {
                success = true;
                return null;
            }
        }

        public boolean isValid() {
            return success;
        }

        @Nullable
        public ItemStack toItem() {
            if(!isValid()) {
                return null;
            }
            ItemStack item = new ItemStack(typeEnum, amount, data);
            ItemMeta meta = item.getItemMeta();
            this.meta.apply(meta);
            item.setItemMeta(meta);
            return item;
        }

        public String getName() {
            return getSection().getName();
        }

        public ConfigurationSection getSection() {
            return s;
        }

        @AllArgsConstructor
        public static class Meta {

            private final ConfigurationSection s;

            @Getter(onMethod_ = { @Nullable })
            private String displayName = null;
            @Getter
            private List<String> lore = Lists.newArrayList();

            private Meta(ConfigurationSection s) {
                this.s = s;
            }

            public String init() {
                if(s.contains("display-name")) {
                    displayName = color(s.getString("display-name"));
                }
                if(s.contains("lore")) {
                    lore = s.getStringList("lore")
                            .stream()
                            .map(ConfiguredMob::color)
                            .collect(Collectors.toList());
                }
                return null;
            }

            public ConfigurationSection getSection() {
                return s;
            }

            public void apply(ItemMeta meta) {
                if(displayName != null) {
                    meta.setDisplayName(displayName);
                }
                if(!lore.isEmpty()) {
                    meta.setLore(lore);
                }
            }

        }

    }

    private static String color(String s) {
        if(s == null) return null;
        return ChatColor.translateAlternateColorCodes('&', s);
    }

}

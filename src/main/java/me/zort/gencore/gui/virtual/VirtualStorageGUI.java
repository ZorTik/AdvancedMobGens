package me.zort.gencore.gui.virtual;

import me.zort.gencore.GenCore;
import me.zort.gencore.cache.PlayerDataCache;
import me.zort.gencore.configuration.ConfigurationRegistry;
import me.zort.gencore.configuration.config.ConfigurationImplConfig;
import me.zort.gencore.configuration.config.ConfiguredMob;
import me.zort.gencore.configuration.messages.ConfigurationImplMessages;
import me.zort.gencore.data.entity.PlayerData;
import me.zort.gencore.gui.GUI;
import me.zort.gencore.gui.GUIElement;
import me.zort.gencore.object.Pair;
import me.zort.gencore.util.ItemUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class VirtualStorageGUI extends GUI {

    private final ConfigurationSection section;
    private String nick;
    private int currentPageIndex = 0;
    private int maxPageIndex = 0;

    public VirtualStorageGUI(ConfigurationImplConfig config) {
        this(config.getConfigurationSection("virtual-storage").get());
    }

    public VirtualStorageGUI(ConfigurationSection section) {
        super(section.getString("title", ""), section.getInt("rows", 6));
        this.section = section;
    }

    @Override
    public void update() {
        GenCore plugin = GenCore.getSingleton();
        ConfigurationImplMessages messages = ConfigurationRegistry.getConfiguration(ConfigurationImplMessages.class).get();
        Inventory inv = getInv();
        Map<Integer, GUIElement> elements = getElements();
        elements.clear();
        if(inv.getSize() > 9) {
            PlayerDataCache playerCache = plugin.getPlayerCache();
            PlayerData playerData = playerCache.getPlayerData(nick);
            if(playerData != null) {
                List<ConfiguredMob.LootItem> items = playerData.getVirtualStorageItems();
                ConfiguredMob.LootItem[] itemsArray = items.toArray(new ConfiguredMob.LootItem[0]);
                maxPageIndex = itemsArray.length / (inv.getSize() - 9);
                for(int i = currentPageIndex * (getInv().getSize() - 9); i < (currentPageIndex + 1) * (getInv().getSize() - 9); i++) {
                    if(itemsArray.length <= i) {
                        break;
                    }
                    ConfiguredMob.LootItem item = itemsArray[i];
                    ItemStack itemStack = item.toItem();
                    if(itemStack != null) {
                        if(itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null) {
                            ItemMeta meta = itemStack.getItemMeta();
                            List<String> lore = meta.getLore();
                            meta.setLore(
                                    lore.stream()
                                            .map(s -> s.replaceAll("%modifier%", String.valueOf(playerData.getStorageModifier())))
                                            .collect(Collectors.toList())
                            );
                            itemStack.setItemMeta(meta);
                        }
                        for(int slot = 0; slot < inv.getSize() - 9; slot++) {
                            if(getElementOn(slot) == null) {
                                GUIElement element = GUIElement.builder()
                                        .item(itemStack)
                                        .action(p -> {
                                            Economy eco = plugin.getEconomy();
                                            setFrozen(true);
                                            CompletableFuture.supplyAsync(() -> {
                                                PlayerData success = playerCache.modifyPlayerData(nick, playerData1 -> {
                                                    playerData1.getVirtualStorage().remove(item.getName());
                                                });
                                                if(success != null) {
                                                    return eco.depositPlayer(p, item.getSellPrice() * playerData.getStorageModifier());
                                                }
                                                return null;
                                            }).whenComplete((response, ex) -> {
                                                if(ex != null || response == null) {
                                                    messages.send(p, ConfigurationImplMessages.Message.SOMETHING_WENT_WRONG);
                                                    return;
                                                }
                                                Bukkit.getScheduler().runTask(plugin, this::update);
                                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                                    setFrozen(false);
                                                }, 20L);
                                                messages.send(p, ConfigurationImplMessages.Message.SOLD_ITEM, new Pair<>("%price%", item.getSellPrice() * playerData.getStorageModifier()));
                                            });
                                        })
                                        .build();
                                setElement(slot, element);
                                break;
                            }
                        }
                    }
                }
            }
        }
        ItemStack prevItem = ItemUtil.fromConfigurationSection(section.getConfigurationSection("previous-page-item"));
        ItemStack nextItem = ItemUtil.fromConfigurationSection(section.getConfigurationSection("next-page-item"));
        if(prevItem != null && currentPageIndex > 0) {
            setElement(inv.getSize() - 6, GUIElement.builder()
                    .item(prevItem)
                    .action(p -> {
                        currentPageIndex--;
                        update();
                    })
                    .build());
        }
        if(nextItem != null && currentPageIndex < maxPageIndex) {
            setElement(inv.getSize() - 4, GUIElement.builder()
                    .item(nextItem)
                    .action(p -> {
                        currentPageIndex++;
                        update();
                    })
                    .build());
        }
        super.update();
    }

    @Override
    public void open(Player p) {
        nick = p.getName();
        update();
        super.open(p);
    }

}

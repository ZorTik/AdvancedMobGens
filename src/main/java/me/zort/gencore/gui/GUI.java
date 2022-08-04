package me.zort.gencore.gui;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class GUI {

    @Getter
    private final Inventory inv;
    @Getter
    private final Map<Integer, GUIElement> elements;
    @Setter
    @Getter
    private boolean frozen;

    public GUI(String title, int rows) {
        if(rows < 1 || rows > 6) {
            throw new RuntimeException("Invalid number of rows!");
        }
        this.inv = Bukkit.createInventory(null, rows * 9, title);
        this.elements = Maps.newConcurrentMap();
    }

    public void update() {
        inv.clear();
        elements.forEach((slot, el) -> {
            if(slot >= 0 && slot < inv.getSize()) {
                inv.setItem(slot, el.getItem());
            }
        });
    }

    public void setElement(int slot, GUIElement element) {
        elements.put(slot, element);
    }

    public void open(Player p) {
        update();
        p.openInventory(inv);
        GUIRegistry.OPENED_GUIS.put(p.getName(), this);
    }

    @Nullable
    public GUIElement getElementOn(int index) {
        return elements.get(index);
    }

}

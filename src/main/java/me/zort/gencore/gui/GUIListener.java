package me.zort.gencore.gui;

import me.zort.gencore.GenCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Optional;

public class GUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        HumanEntity p = e.getWhoClicked();
        String nick = p.getName();
        Optional<GUI> guiOptional = GUIRegistry.getGuiBy(nick);
        if(p instanceof Player && guiOptional.isPresent()) {
            int slot = e.getSlot();
            GUI gui = guiOptional.get();
            GUIElement element = gui.getElementOn(slot);
            if(element != null) {
                e.setCancelled(true);
                if(!gui.isFrozen()) {
                    gui.setFrozen(true);
                    Bukkit.getScheduler().runTaskLater(GenCore.getSingleton(), () -> {
                        try {
                            element.invoke((Player) p);
                        } catch(Exception ex) {
                            ex.printStackTrace();
                        }
                        gui.setFrozen(false);
                    }, 5L);
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        HumanEntity p = e.getPlayer();
        String nick = p.getName();
        GUIRegistry.OPENED_GUIS.remove(nick);
    }

}

package me.zort.gencore.gui;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class GUIElement {

    private ItemStack item;
    private Consumer<Player> action;

    public void invoke(Player p) {
        action.accept(p);
    }

}

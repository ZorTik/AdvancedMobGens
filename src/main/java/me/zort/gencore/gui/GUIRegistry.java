package me.zort.gencore.gui;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Optional;

public class GUIRegistry {

    protected static final Map<String, GUI> OPENED_GUIS = Maps.newConcurrentMap();

    public static Optional<GUI> getGuiBy(String nickname) {
        return Optional.ofNullable(OPENED_GUIS.get(nickname));
    }

}

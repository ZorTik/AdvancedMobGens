package me.zort.gencore.configuration;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class ConfiguredObject {

    private ConfigurationSection section;

    public ConfiguredObject(ConfigurationSection section) {
        this.section = section;
    }

    @Nullable
    public abstract String checkValid();
    public abstract void load();

}

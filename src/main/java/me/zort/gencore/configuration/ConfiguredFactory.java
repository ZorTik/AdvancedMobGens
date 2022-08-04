package me.zort.gencore.configuration;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ConfiguredFactory<T> {

    @Nullable
    T create(ConfigurationSection section);

}

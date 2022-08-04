package me.zort.gencore.configuration;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ConfigurationRegistry {

    private static final List<Configuration> CONFIGURATIONS_CACHE = Collections.synchronizedList(Lists.newArrayList());

    public static void register(Configuration configuration) {
        Class<? extends Configuration> clazz = configuration.getClass();
        Optional<? extends Configuration> currentConfigurationOptional = getConfiguration(clazz);
        if(currentConfigurationOptional.isPresent()) {
            Configuration currentConfiguration = currentConfigurationOptional.get();
            CONFIGURATIONS_CACHE.remove(currentConfiguration);
        }
        CONFIGURATIONS_CACHE.add(configuration);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Configuration> Optional<T> getConfiguration(Class<T> configurationClass) {
        return CONFIGURATIONS_CACHE.stream()
                .filter(configuration -> configuration.getClass().equals(configurationClass))
                .map(configuration -> {
                    @SuppressWarnings("unchecked")
                    T configurationCasted = (T) configuration;
                    return configurationCasted;
                })
                .findFirst();
    }

    public static void clear() {
        CONFIGURATIONS_CACHE.clear();
    }

}

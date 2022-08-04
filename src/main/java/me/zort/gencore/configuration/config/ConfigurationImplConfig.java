package me.zort.gencore.configuration.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.zort.gencore.configuration.Configuration;
import me.zort.gencore.configuration.ConfigurationRegistry;
import me.zort.gencore.configuration.ConfiguredFactory;
import me.zort.gencore.configuration.ConfiguredObject;
import me.zort.gencore.configuration.exception.ConfigurationException;
import me.zort.gencore.util.MapBuilder;
import me.zort.gencore.validator.ConfigurationValidator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConfigurationImplConfig implements Configuration {

    public static final Map<ConfiguredListType, ConfiguredFactory<? extends ConfiguredObject>> DEFINED_FACTORIES = MapBuilder.of(
            new HashMap<ConfiguredListType, ConfiguredFactory<? extends ConfiguredObject>>()
    )
            .record(ConfiguredListType.MOBS, ConfiguredMob::new)
            .record(ConfiguredListType.TIERS, section -> {
                Optional<ConfigurationImplConfig> configurationOptional = ConfigurationRegistry.getConfiguration(ConfigurationImplConfig.class);
                if(!configurationOptional.isPresent()) {
                    return null;
                }
                ConfigurationImplConfig configuration = configurationOptional.get();
                return new ConfiguredTier(section, configuration);
            })
            .build();

    private FileConfiguration config;
    private final File file;
    private final Logger logger;

    private final Map<ConfiguredListType, ConfigurationSection> sectionsDefinedInternal;
    private final Map<ConfiguredListType, List<? extends ConfiguredObject>> configuredEntriesInternal;

    public ConfigurationImplConfig(File configFile, Logger logger) {
        ConfigurationValidator.validate(this, configFile);
        this.file = configFile;
        this.logger = logger;
        this.sectionsDefinedInternal = Maps.newConcurrentMap();
        this.configuredEntriesInternal = Maps.newConcurrentMap();
    }

    public boolean reload() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        return reload(config);
    }

    public boolean reload(FileConfiguration config) {
        this.config = config;
        register(ConfiguredListType.MOBS, cs("mobs"));
        register(ConfiguredListType.TIERS, cs("tiers"));
        ConfiguredListType[] typeValues = ConfiguredListType.values();
        for(ConfiguredListType type : typeValues) {
            if(!reload(type)) {
                logger.info("Cannot load " + type.getTitle());
                return false;
            }
        }
        return true;
    }

    public boolean reload(ConfiguredListType type) {
        ConfiguredFactory<? extends ConfiguredObject> configuredFactory = DEFINED_FACTORIES.get(type);
        if(configuredFactory == null) {
            return false;
        }
        return reload(type, configuredFactory);
    }

    private boolean reload(ConfiguredListType type, ConfiguredFactory<? extends ConfiguredObject> factory) {
        ConfigurationSection section = sectionsDefinedInternal.get(type);
        if(section == null) {
            return false;
        }
        return reload(type, factory, section);
    }

    private boolean reload(ConfiguredListType type, ConfiguredFactory<? extends ConfiguredObject> factory, ConfigurationSection sectionParent) {
        List<ConfigurationSection> sections = csi(sectionParent);
        List<ConfiguredObject> configuredList = sections.stream()
                .map(factory::create)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(Lists::newLinkedList));
        configuredEntriesInternal.put(type, configuredList);
        List<ConfigurationInvalidInternal> invalidList = configuredList.stream()
                .map(obj -> new ConfigurationInvalidInternal(obj, obj.checkValid()))
                .filter(configurationInvalid -> configurationInvalid.getErrorMessage() != null)
                .collect(Collectors.toList());
        if(invalidList.isEmpty()) {
            configuredList.forEach(configuredObject -> {
                try {
                    configuredObject.load();
                } catch(Exception ex) {
                    throw new ConfigurationException(ex.getMessage(), this);
                }
            });
            return true;
        } else {
            configuredEntriesInternal.remove(type);
            invalidList.forEach(configurationInvalid -> {
                logger.info(configurationInvalid.constructErrorLine());
            });
            return false;
        }
    }

    public boolean hasMob(EntityType entityType) {
        return getMobBy(entityType).isPresent();
    }

    @SuppressWarnings("unchecked")
    public Optional<ConfiguredMob> getMobBy(EntityType entityType) {
        List<ConfiguredMob> mobList = (List<ConfiguredMob>) get(ConfiguredListType.MOBS);
        return mobList.stream()
                .filter(mob -> mob.getEntityType().equals(entityType))
                .findFirst();
    }

    public Optional<ConfiguredMob.LootItem> getItemBy(String name) {
        return getAllLootItems().stream()
                .filter(item -> item.getSection().getName().equals(name))
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    public List<ConfiguredMob.LootItem> getAllLootItems() {
        List<ConfiguredMob> mobList = (List<ConfiguredMob>) get(ConfiguredListType.MOBS);
        return mobList.stream()
                .flatMap(mob -> mob.getLoot().stream())
                .collect(Collectors.toList());
    }

    public Optional<@NotNull ConfigurationSection> getConfigurationSection(String path) {
        ConfigurationValidator.checkLoaded(this);
        return Optional.ofNullable(config.getConfigurationSection(path));
    }

    public Optional<@NotNull String> getString(String path) {
        ConfigurationValidator.checkLoaded(this);
        return Optional.ofNullable(config.getString(path));
    }

    public Optional<@NotNull Integer> getInt(String path) {
        ConfigurationValidator.checkLoaded(this);
        return Optional.of(config.getInt(path));
    }

    public Optional<@NotNull Double> getDouble(String path) {
        ConfigurationValidator.checkLoaded(this);
        return Optional.of(config.getDouble(path));
    }

    public Optional<@NotNull Boolean> getBoolean(String path) {
        ConfigurationValidator.checkLoaded(this);
        return Optional.of(config.getBoolean(path));
    }

    public List<? extends ConfiguredObject> get(ConfiguredListType type) {
        return configuredEntriesInternal.getOrDefault(type, Collections.emptyList());
    }

    private List<ConfigurationSection> csi(ConfigurationSection parent) {
        Set<String> keys = parent.getKeys(false);
        List<ConfigurationSection> result = Lists.newLinkedList();
        for(String key : keys) {
            String path = parent.getCurrentPath() + "." + key;
            if(config.isConfigurationSection(path)) {
                result.add(cs(path));
            }
        }
        return result;
    }

    private ConfigurationSection cs(String path) {
        return config.getConfigurationSection(path);
    }

    private void register(ConfiguredListType type, ConfigurationSection section) {
        sectionsDefinedInternal.put(type, section);
    }

    @Override
    public boolean isLoaded() {
        return config != null;
    }

    @Override
    public File getFile() {
        return file;
    }

    @RequiredArgsConstructor
    @Data
    private static class ConfigurationInvalidInternal {

        private final ConfiguredObject configuredObject;
        private final String errorMessage;

        public String constructErrorLine() {
            return "Object " + getConfiguredObject().getSection().getName() + ": " + getErrorMessage();
        }

    }

    public enum ConfiguredListType {

        MOBS("mobs"),
        TIERS("tiers");

        @Getter
        private final String title;

        ConfiguredListType(String title) {
            this.title = title;
        }

    }

}

package me.zort.gencore;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.j256.ormlite.support.ConnectionSource;
import lombok.Getter;
import me.zort.gencore.cache.PlayerDataCache;
import me.zort.gencore.cache.SpawnedEntityCache;
import me.zort.gencore.command.GenCoreAdminExecutor;
import me.zort.gencore.command.GenCoreStorageExecutor;
import me.zort.gencore.command.GenCoreToggleGensExecutor;
import me.zort.gencore.configuration.ConfigurationRegistry;
import me.zort.gencore.configuration.config.ConfigurationImplConfig;
import me.zort.gencore.configuration.messages.ConfigurationImplMessages;
import me.zort.gencore.data.Entity;
import me.zort.gencore.data.JdbcConnector;
import me.zort.gencore.data.accessor.ChunkGeneratorReferenceAccessor;
import me.zort.gencore.data.connection.sqlite.SQLiteConnectionSource;
import me.zort.gencore.data.connection.sqlite.SQLiteConnector;
import me.zort.gencore.data.connection.sqlite.SQLiteCredentials;
import me.zort.gencore.data.entity.PlayerData;
import me.zort.gencore.gui.GUIListener;
import me.zort.gencore.lifecycle.ScheduledTaskChain;
import me.zort.gencore.lifecycle.TaskChainImpl;
import me.zort.gencore.lifecycle.generator.MobCleanupTask;
import me.zort.gencore.lifecycle.generator.MobSpawnTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Getter
public final class GenCore extends JavaPlugin {

    private static GenCore INSTANCE;
    public static Gson GSON = new GsonBuilder()
            .enableComplexMapKeySerialization()
            .create();

    public static GenCore getSingleton() {
        return INSTANCE;
    }

    private Economy economy;
    private JdbcConnector<? extends ConnectionSource, ?> connector;
    private ChunkGeneratorReferenceAccessor refAccessor;
    private PlayerDataCache playerCache;
    private SpawnedEntityCache entityCache;
    private TaskChainImpl taskChain;
    private File configFile;

    @Override
    public void onEnable() {
        INSTANCE = this;
        RegisteredServiceProvider<Economy> economyService = getServer().getServicesManager().getRegistration(Economy.class);
        if(economyService == null) {
            getLogger().info("Vault or Vault economy hook is not registered!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.economy = economyService.getProvider();
        if(!(configFile = new File(getDataFolder().getAbsolutePath() + "/config.yml")).exists()) {
            configFile.getParentFile().mkdirs();
            saveResource("config.yml", true);
        }
        ConfigurationImplConfig config = new ConfigurationImplConfig(configFile, getLogger());
        ConfigurationRegistry.register(config);
        ConfigurationImplMessages messages = new ConfigurationImplMessages(new File(getDataFolder().getAbsolutePath() + "/messages.yml"));
        ConfigurationRegistry.register(messages);
        if(!config.reload() || !messages.reload()) {
            getLogger().info("There was error while loading configuration! Please check it!");
            return;
        }
        Optional<ConfigurationSection> dataSourceSectionOptional = config.getConfigurationSection("data-source");
        if(!dataSourceSectionOptional.isPresent()) {
            getLogger().info("Data source section in configuration is not set!");
            return;
        }
        ConfigurationSection dataSourceSection = dataSourceSectionOptional.get();
        String dataSourceType = dataSourceSection.getString("type");
        String url = dataSourceSection.getString("url");
        JdbcConnector<? extends ConnectionSource, ?> connector = null;
        if(dataSourceType.equalsIgnoreCase("sqlite")) {
            getLogger().info("Found SQLite configuration! Trying to connect...");
            List<Class<? extends Entity<?>>> initEntities = Lists.newArrayList();
            initEntities.add(PlayerData.class);
            SQLiteConnector sqLiteConnector = new SQLiteConnector(initEntities, getLogger());
            SQLiteCredentials credentials = SQLiteCredentials.of(
                    url
            );
            SQLiteConnectionSource connectionSource = sqLiteConnector.connect(credentials);
            if(connectionSource != null) {
                connector = sqLiteConnector;
            }
        }
        if(connector == null) {
            getLogger().info("Could not connect to data source! Please check your configuration.");
            return;
        }
        this.connector = connector;
        Optional<ConfigurationSection> taskChainSectionOptional = config.getConfigurationSection("task-chain");
        if(!taskChainSectionOptional.isPresent()) {
            getLogger().info("Task chain section in configuration is not present!");
            return;
        }
        this.playerCache = new PlayerDataCache(connector, this);
        ChunkGeneratorReferenceAccessor refAccessor = new ChunkGeneratorReferenceAccessor(connector, getLogger());
        Bukkit.getServer().getWorlds().forEach(w -> {
            for(Chunk c : w.getLoadedChunks()) {
                try {
                    refAccessor.register(c);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        this.refAccessor = refAccessor;
        playerCache.registerAccessor(PlayerDataCache.Event.CACHE_LOAD, PlayerData.class, refAccessor);
        playerCache.registerAccessor(PlayerDataCache.Event.MODIFICATION, PlayerData.class, refAccessor);
        this.entityCache = new SpawnedEntityCache(connector, this);
        ConfigurationSection taskChainSection = taskChainSectionOptional.get();
        int taskChainInterval = taskChainSection.getInt("interval");
        this.taskChain = new ScheduledTaskChain(this, taskChainInterval);
        taskChain.appendAfter(new MobSpawnTask(playerCache, entityCache, config));
        taskChain.appendAfter(new MobCleanupTask(connector));
        taskChain.appendAfter(entityCache);
        ((ScheduledTaskChain) taskChain).start();
        getServer().getPluginManager().registerEvents(new GenCoreListener(), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getCommand("gencoreadmin").setExecutor(new GenCoreAdminExecutor());
        getCommand("togglegens").setExecutor(new GenCoreToggleGensExecutor());
        getCommand("storage").setExecutor(new GenCoreStorageExecutor());
        Bukkit.getOnlinePlayers().forEach(p -> {
            PlayerData playerData = playerCache.getPlayerData(p);
            if(playerData != null) {
                refAccessor.access(playerData);
            }
        });
    }

    @Override
    public void onDisable() {
        if(playerCache != null) playerCache.close();
        ConfigurationRegistry.clear();
        HandlerList.unregisterAll(this);
    }

}

package me.zort.gencore.cache;

import com.google.common.collect.Maps;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import lombok.Getter;
import me.zort.gencore.data.JdbcConnector;
import me.zort.gencore.data.entity.PlayerData;
import me.zort.gencore.object.PubAccessRegistrar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Getter
public class PlayerDataCache extends PubAccessRegistrar<PlayerDataCache.Event> {

    private final ThreadPoolExecutor executorService;
    private final Dao<PlayerData, String> playerDataDao;
    private final Logger logger;
    private final Map<String, PlayerData> cacheMap;

    public PlayerDataCache(JdbcConnector<? extends ConnectionSource, ?> connector, Plugin plugin) {
        this(connector, plugin.getLogger());
    }

    public PlayerDataCache(JdbcConnector<? extends ConnectionSource, ?> connector, Logger logger) {
        this.playerDataDao = connector.getOrCreateDao(PlayerData.class);
        this.logger = logger;
        this.cacheMap = Maps.newConcurrentMap();
        Objects.requireNonNull(playerDataDao, "Cannot load player data dao!");
        this.executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    public CompletableFuture<@Nullable PlayerData> modifyPlayerDataAsync(String nick, Consumer<PlayerData> consumer) {
        return CompletableFuture.supplyAsync(() -> modifyPlayerData(nick, consumer), executorService)
                .whenComplete((data, ex) -> {
                    if(ex != null) {
                        ex.printStackTrace();
                    }
                });
    }

    public PlayerData modifyPlayerData(String nick, Consumer<PlayerData> consumer) {
        PlayerData playerData = query(nick);
        if(playerData == null) {
            logger.info("Cannot update " + nick + "'s data because they do not exist!");
            return null;
        }
        consumer.accept(playerData);
        invokeAccessors(Event.MODIFICATION, playerData);
        int rowsChanged;
        try {
            rowsChanged = playerDataDao.update(playerData);
            if(rowsChanged > 0) {
                cacheMap.put(nick, playerData);
                return playerData;
            } else logger.info("Cannot modify " + nick + "'s data! (" + rowsChanged + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public CompletableFuture<@Nullable PlayerData> getPlayerDataAsync(Player player) {
        Objects.requireNonNull(player);
        return getPlayerDataAsync(player.getName());
    }

    public CompletableFuture<@Nullable PlayerData> getPlayerDataAsync(String nick) {
        return CompletableFuture.supplyAsync(() -> getPlayerData(nick), executorService);
    }

    @Nullable
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getName());
    }

    @Nullable
    public PlayerData getPlayerData(String nick) {
        PlayerData playerData = cacheMap.get(nick);
        if(playerData == null) {
            if((playerData = query(nick)) == null) {
                return null;
            }
            invokeAccessors(Event.CACHE_LOAD, playerData);
            cacheMap.put(nick, playerData);
        }
        return playerData;
    }

    public Map<String, PlayerData> getDataCopy() {
        return new HashMap<>(cacheMap);
    }

    public PlayerData clean(String nick) {
        return clean(nick, true);
    }

    public PlayerData clean(String nick, boolean async) {
        PlayerData removeState = cacheMap.remove(nick);
        if(removeState != null) {
            if(invokeAccessors(Event.CACHE_REMOVAL, removeState)) {
                if(async) {
                    CompletableFuture.runAsync(() -> save(removeState), executorService);
                } else { save(removeState); }
            }
        }
        return removeState;
    }

    public void close() {
        getExecutorService().shutdownNow();
    }

    @Nullable
    private PlayerData query(String nick) {
        PlayerData playerData;
        try {
            playerData = playerDataDao.queryForId(nick);
            if(playerData == null) {
                playerData = new PlayerData();
                playerData.setNickname(nick);
                invokeAccessors(Event.CREATION, playerData);
                int amountChanged = playerDataDao.create(playerData);
                if(amountChanged > 0) {
                    return playerData;
                }
            }
            return playerData;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean save(PlayerData playerData) {
        try {
            playerDataDao.update(playerData);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public enum Event {

        CREATION,
        MODIFICATION,
        CACHE_LOAD,
        CACHE_REMOVAL

    }

}

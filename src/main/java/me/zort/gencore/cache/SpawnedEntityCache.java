package me.zort.gencore.cache;

import com.google.common.collect.Maps;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import lombok.Getter;
import me.zort.gencore.GenCore;
import me.zort.gencore.configuration.ConfigurationRegistry;
import me.zort.gencore.configuration.config.ConfigurationImplConfig;
import me.zort.gencore.configuration.config.ConfiguredMob;
import me.zort.gencore.data.JdbcConnector;
import me.zort.gencore.data.entity.SpawnedEntity;
import me.zort.gencore.lifecycle.ScheduledTask;
import me.zort.gencore.lifecycle.TaskMeta;
import me.zort.gencore.object.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Getter
public class SpawnedEntityCache extends ScheduledTask {

    private final ThreadPoolExecutor executorService;
    private final Dao<SpawnedEntity, String> entityDao;
    private final ConfigurationImplConfig config;
    private final Logger logger;
    private final Map<String, SpawnedEntity> cacheMap;

    public SpawnedEntityCache(JdbcConnector<? extends ConnectionSource, ?> connector, Plugin plugin) {
        this(connector, ConfigurationRegistry.getConfiguration(ConfigurationImplConfig.class).get(), plugin.getLogger());
    }

    public SpawnedEntityCache(JdbcConnector<? extends ConnectionSource, ?> connector, ConfigurationImplConfig config, Logger logger) {
        super(new TaskMeta(0, Predicate.TRUE, false), 20*60L);
        this.entityDao = connector.getOrCreateDao(SpawnedEntity.class);
        this.config = config;
        this.logger = logger;
        this.cacheMap = Maps.newConcurrentMap();
        Objects.requireNonNull(entityDao, "Cannot load player data dao!");
        this.executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run(long uptime) {
        List<ConfiguredMob> configuredMobs = (List<ConfiguredMob>) config.get(ConfigurationImplConfig.ConfiguredListType.MOBS);
        Class<?>[] entityTypeClasses = Arrays.stream(EntityType.values())
                .filter(entityType -> configuredMobs.stream()
                        .anyMatch(mob -> mob.getEntityType().equals(entityType)))
                .map(EntityType::getEntityClass)
                .collect(Collectors.toList()).toArray(new Class<?>[0]);
        List<String> entitiesUuids = Bukkit.getWorlds().stream()
                .flatMap(w -> {
                    Collection<Entity> entities = w.getEntitiesByClasses(entityTypeClasses);
                    return entities.stream()
                            .map(e -> e.getUniqueId().toString());
                })
                .collect(Collectors.toList());
        Bukkit.getScheduler().runTaskAsynchronously(GenCore.getSingleton(), () -> {
            try {
                List<SpawnedEntity> spawnedEntities = entityDao.query(
                        entityDao.queryBuilder().where()
                                .in("id", entitiesUuids)
                                .prepare()
                );
                cacheMap.entrySet().removeIf(entry -> spawnedEntities.stream()
                        .noneMatch(se -> se.getId().equals(entry.getKey())));
                spawnedEntities.forEach(e -> cacheMap.put(e.getId(), e));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<@Nullable SpawnedEntity> modifyEntityAsync(String uuid, String owner, Consumer<SpawnedEntity> consumer) {
        return CompletableFuture.supplyAsync(() -> modifyEntity(uuid, owner, consumer), executorService)
                .whenComplete((data, ex) -> {
                    if(ex != null) {
                        ex.printStackTrace();
                    }
                });
    }

    public SpawnedEntity modifyEntity(String uuid, String owner, Consumer<SpawnedEntity> consumer) {
        SpawnedEntity entity = query(uuid, owner);
        if(entity == null) {
            logger.info("Cannot update " + uuid + "'s data because they do not exist!");
            return null;
        }
        consumer.accept(entity);
        int rowsChanged;
        try {
            rowsChanged = entityDao.update(entity);
            if(rowsChanged > 0) {
                cacheMap.put(uuid, entity);
                return entity;
            } else logger.info("Cannot modify " + uuid + "'s data! (" + rowsChanged + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<SpawnedEntity> getEntities(Chunk c, @Nullable EntityType entityType) {
        return Arrays.stream(c.getEntities())
                .map(e -> {
                    if(entityType != null && !entityType.equals(e.getType())) {
                        return null;
                    }
                    return cacheMap.get(e.getUniqueId().toString());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public CompletableFuture<@Nullable SpawnedEntity> getEntityAsync(String uuid, String owner) {
        return CompletableFuture.supplyAsync(() -> getEntity(uuid, owner), executorService);
    }

    @Nullable
    public SpawnedEntity getEntity(String uuid, @Nullable String owner) {
        SpawnedEntity entity = cacheMap.get(uuid);
        if(entity == null && owner != null) {
            if((entity = query(uuid, owner)) == null) {
                return null;
            }
            cacheMap.put(uuid, entity);
        }
        return entity;
    }

    public Map<String, SpawnedEntity> getDataCopy() {
        return new HashMap<>(cacheMap);
    }

    public SpawnedEntity clean(String nick) {
        return clean(nick, true);
    }

    public SpawnedEntity clean(String nick, boolean async) {
        return cacheMap.remove(nick);
    }

    public void close() {
        getExecutorService().shutdownNow();
    }

    @Nullable
    public SpawnedEntity query(String uuid, String owner) {
        SpawnedEntity entity;
        try {
            entity = entityDao.queryForId(uuid);
            if(entity == null) {
                entity = new SpawnedEntity();
                entity.setId(uuid);
                entity.setOwner(owner);
                int amountChanged = entityDao.create(entity);
                if(amountChanged > 0) {
                    return entity;
                }
            }
            return entity;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean save(SpawnedEntity playerData) {
        try {
            entityDao.update(playerData);
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

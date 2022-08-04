package me.zort.gencore.lifecycle.generator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import me.zort.gencore.GenCore;
import me.zort.gencore.cache.PlayerDataCache;
import me.zort.gencore.cache.SpawnedEntityCache;
import me.zort.gencore.configuration.ConfigurationRegistry;
import me.zort.gencore.configuration.config.ConfigurationImplConfig;
import me.zort.gencore.configuration.config.ConfiguredMob;
import me.zort.gencore.configuration.config.ConfiguredTier;
import me.zort.gencore.configuration.messages.ConfigurationImplMessages;
import me.zort.gencore.data.JdbcConnector;
import me.zort.gencore.data.companion.Generator;
import me.zort.gencore.data.entity.PlayerData;
import me.zort.gencore.data.entity.SpawnedEntity;
import me.zort.gencore.lifecycle.ScheduledTask;
import me.zort.gencore.lifecycle.TaskMeta;
import me.zort.gencore.object.Pair;
import me.zort.gencore.object.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MobSpawnTask extends ScheduledTask {

    private final PlayerDataCache playerCache;
    private final SpawnedEntityCache entityCache;
    private final ConfigurationImplConfig config;
    private final ConfigurationImplMessages messages = ConfigurationRegistry.getConfiguration(ConfigurationImplMessages.class)
            .get();

    public MobSpawnTask(PlayerDataCache playerCache, SpawnedEntityCache entityCache, ConfigurationImplConfig config) {
        super(new TaskMeta(0, Predicate.TRUE, false), 20*10L);
        this.playerCache = playerCache;
        this.entityCache = entityCache;
        this.config = config;
    }

    @Override
    public void run(long uptime) {
        Map<String, PlayerData> playerDataCache = playerCache.getDataCopy();
        Map<SpawnedEntity, Entity> toUpdate = Maps.newConcurrentMap();
        playerDataCache.values().forEach(playerData -> {
            try {
                List<Generator> activeGenerators = playerData.getActiveGenerators();
                activeGenerators.forEach(generator -> {
                    Location bukkitLocation = generator.getBukkitLocation();
                    if(bukkitLocation != null && bukkitLocation.getWorld() != null) {
                        ConfiguredTier tier = generator.getTier();
                        ConfiguredMob mob;
                        if(tier != null && (mob = tier.getMob()) != null) {
                            EntityType entityType = mob.getEntityType();
                            Location loc = bukkitLocation.clone().add(0.0, 1.0, 0.0);
                            Chunk c = loc.getChunk();
                            List<SpawnedEntity> entities = entityCache.getEntities(c, entityType);
                            System.out.println("Entities: " + entities.size());
                            Optional<SpawnedEntity> entityHolderOptional = entities.stream()
                                    .filter(e -> {
                                        boolean b = e.getAmount() < config.getInt("max-entity-stack-amount").orElse(10);
                                        System.out.println("Amount: " + e.getAmount());
                                        System.out.println("Max stack: " + config.getInt("max-entity-stack-amount").orElse(10));
                                        System.out.println(b);
                                        return b;
                                    })
                                    .min(Comparator.comparingInt(SpawnedEntity::getAmount));
                            System.out.println("Entity holder: " + entityHolderOptional.isPresent());
                            if(entityHolderOptional.isPresent()) {
                                SpawnedEntity entityHolder = entityHolderOptional.get();
                                entityCache.modifyEntityAsync(entityHolder.getId(), entityHolder.getOwner(), se -> {
                                    se.setAmount(se.getAmount() + 1);
                                }).whenComplete((se, ex) -> {
                                    if(se == null || ex != null) {
                                        GenCore.getSingleton().getLogger().info("Cannot stack entity " + (se != null ? se.getId() : "unknown") + "! An unexpected error occured.");
                                        return;
                                    }
                                    Bukkit.getScheduler().runTask(GenCore.getSingleton(), () -> {
                                        Entity entity = se.toEntity();
                                        if(entity != null) {
                                            entity.setCustomNameVisible(true);
                                            String name = messages.getSingle(ConfigurationImplMessages.Message.ENTITY_STACK_NAME, new Pair<>("%amount%", se.getAmount()), new Pair<>("%name%", entity.getType().getName()));
                                            entity.setCustomName(name);
                                        }
                                    });
                                });
                            } else {
                                Entity entity = bukkitLocation.getWorld().spawnEntity(bukkitLocation.clone().add(0.0, 1.0, 0.0), entityType);
                                SpawnedEntity spawnedEntity = new SpawnedEntity();
                                spawnedEntity.setId(entity.getUniqueId().toString());
                                spawnedEntity.setOwner(playerData.getNickname());
                                toUpdate.put(spawnedEntity, entity);
                            }
                        }
                    }
                });
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        });
        Bukkit.getScheduler().runTaskAsynchronously(GenCore.getSingleton(), () -> {
            JdbcConnector<? extends ConnectionSource, ?> connector = GenCore.getSingleton().getConnector();
            Dao<SpawnedEntity, String> dao = connector.getOrCreateDao(SpawnedEntity.class);
            if(dao == null) {
                GenCore.getSingleton().getLogger().info("Cannot get dao for spawned entities!");
                toUpdate.values().forEach(Entity::remove);
                return;
            }
            toUpdate.forEach((se, e) -> {
                try {
                    Dao.CreateOrUpdateStatus status = dao.createOrUpdate(se);
                    int numRows = status.getNumLinesChanged();
                    if(numRows == 0) {
                        Bukkit.getScheduler().runTask(GenCore.getSingleton(), e::remove);
                    } else {
                        entityCache.getEntity(se.getId(), se.getOwner());
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });
        });
    }
}

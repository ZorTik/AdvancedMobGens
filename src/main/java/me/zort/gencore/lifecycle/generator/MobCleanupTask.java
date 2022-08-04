package me.zort.gencore.lifecycle.generator;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import me.zort.gencore.GenCore;
import me.zort.gencore.data.JdbcConnector;
import me.zort.gencore.data.entity.SpawnedEntity;
import me.zort.gencore.lifecycle.ScheduledTask;
import me.zort.gencore.lifecycle.TaskMeta;
import me.zort.gencore.object.Predicate;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MobCleanupTask extends ScheduledTask {

    private final JdbcConnector<? extends ConnectionSource, ?> connector;

    public MobCleanupTask(JdbcConnector<? extends ConnectionSource, ?> connector) {
        super(new TaskMeta(0, Predicate.TRUE, true), 20*60*10L);
        this.connector = connector;
    }

    @Override
    public void run(long uptime) {
        Dao<SpawnedEntity, String> dao = connector.getOrCreateDao(SpawnedEntity.class);
        if(dao != null) {
            List<SpawnedEntity> entities;
            try {
                entities = dao.queryForAll();
                Bukkit.getScheduler().runTask(GenCore.getSingleton(), () -> {
                    List<String> toRem = entities.stream()
                            .map(SpawnedEntity::getId)
                            .filter(id -> Bukkit.getEntity(UUID.fromString(id)) == null)
                            .collect(Collectors.toList());
                    Bukkit.getScheduler().runTaskAsynchronously(GenCore.getSingleton(), () -> {
                        try {
                            dao.deleteIds(toRem);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}

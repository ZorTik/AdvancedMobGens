package me.zort.gencore.data.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;
import me.zort.gencore.data.Entity;
import org.bukkit.Bukkit;

import java.util.UUID;

@DatabaseTable(tableName = "gencore_spawned_entities")
@Setter
@Getter
public class SpawnedEntity implements Entity<String> {

    @DatabaseField(unique = true, id = true)
    private String id;

    @DatabaseField
    private int amount = 1;

    @DatabaseField
    private String owner;

    public org.bukkit.entity.Entity toEntity() {
        return Bukkit.getEntity(UUID.fromString(id));
    }

}

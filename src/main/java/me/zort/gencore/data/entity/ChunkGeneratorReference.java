package me.zort.gencore.data.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;
import me.zort.gencore.data.Entity;

@DatabaseTable(tableName = "gencore_references")
@Setter
public class ChunkGeneratorReference implements Entity<Integer> {

    @DatabaseField(unique = true, generatedId = true)
    private int id;

    @DatabaseField
    @Getter
    private String worldName;
    @DatabaseField
    @Getter
    private int xChunkIndex;
    @DatabaseField
    @Getter
    private int zChunkIndex;
    @DatabaseField
    @Getter
    private String nickname;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkGeneratorReference that = (ChunkGeneratorReference) o;
        if (xChunkIndex != that.xChunkIndex) return false;
        if (zChunkIndex != that.zChunkIndex) return false;
        if (!worldName.equals(that.worldName)) return false;
        return nickname.equals(that.nickname);
    }

    @Override
    public int hashCode() {
        int result = worldName.hashCode();
        result = 31 * result + xChunkIndex;
        result = 31 * result + zChunkIndex;
        result = 31 * result + nickname.hashCode();
        return result;
    }

}

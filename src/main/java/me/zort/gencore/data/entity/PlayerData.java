package me.zort.gencore.data.entity;

import com.google.common.collect.Lists;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;
import me.zort.gencore.configuration.ConfigurationRegistry;
import me.zort.gencore.configuration.config.ConfigurationImplConfig;
import me.zort.gencore.configuration.config.ConfiguredMob;
import me.zort.gencore.data.Entity;
import me.zort.gencore.data.companion.Generator;
import me.zort.gencore.data.persister.PlayerDataGeneratorsPersister;
import me.zort.gencore.data.persister.PlayerDataVirtualStoragePersister;
import me.zort.gencore.object.SerializableLoc;
import me.zort.gencore.validator.Validator;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@DatabaseTable(tableName = "gencore_players")
@Setter
@Getter
public class PlayerData implements Entity<String> {

    @DatabaseField(unique = true, id = true)
    private String nickname;
    @DatabaseField(columnDefinition = "MEDIUMTEXT", persisterClass = PlayerDataVirtualStoragePersister.class)
    private List<String> virtualStorage = Collections.synchronizedList(Lists.newArrayList());
    @DatabaseField(columnDefinition = "MEDIUMTEXT", persisterClass = PlayerDataGeneratorsPersister.class)
    private List<Generator> generators = Collections.synchronizedList(Lists.newArrayList());
    @DatabaseField
    private boolean generatorsEnabled = true;
    @DatabaseField
    private int generatorsLimit = 1;
    @DatabaseField
    private double storageModifier = 1.0;

    public void addToVirtualStorage(String lootItemName) {
        virtualStorage.add(lootItemName);
    }

    public List<ConfiguredMob.LootItem> getVirtualStorageItems() {
        ConfigurationImplConfig config = ConfigurationRegistry.getConfiguration(ConfigurationImplConfig.class).get();
        return virtualStorage.stream()
                .map(config::getItemBy)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public Optional<Generator> getGeneratorBy(Location loc) {
        return getGeneratorBy(SerializableLoc.of(loc));
    }

    public Optional<Generator> getGeneratorBy(SerializableLoc loc) {
        return generators.stream()
                .filter(gen -> gen.getLocation().isSameBlock(loc))
                .findFirst();
    }

    public List<Generator> getActiveGenerators(Location from, double distance) {
        Validator.requireNonNulls(from, from.getWorld());
        return getActiveGenerators()
                .stream()
                .filter(gen -> {
                    Location loc = gen.getBukkitLocation();
                    return loc.getWorld().equals(from.getWorld()) && from.distance(loc) <= distance;
                })
                .collect(Collectors.toList());
    }

    public List<Generator> getActiveGenerators() {
        if(!generatorsEnabled || Bukkit.getPlayer(nickname) == null) {
            return Collections.emptyList();
        }
        return generators.stream()
                .filter(gen -> {
                    Location loc = gen.getBukkitLocation();
                    if(loc == null) return false;
                    Chunk chunk = loc.getChunk();
                    return chunk.isLoaded();
                })
                .collect(Collectors.toList());

    }

    public int getGeneratorsAmount() {
        return generators.size();
    }

    @Override
    public String getId() {
        return nickname;
    }

}

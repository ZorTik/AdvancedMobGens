package me.zort.gencore.data.companion;

import lombok.Data;
import me.zort.gencore.configuration.ConfigurationRegistry;
import me.zort.gencore.configuration.config.ConfigurationImplConfig;
import me.zort.gencore.configuration.config.ConfiguredTier;
import me.zort.gencore.object.SerializableLoc;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@Data
public class Generator implements Serializable {

    private SerializableLoc location;
    private int tierIndex;

    public boolean upgrade() {
        if(getNextTier() == null) {
            return false;
        }
        tierIndex++;
        return true;
    }

    public boolean isGenBlock(Block b) {
        return isGenBlock(b.getLocation());
    }

    public boolean isGenBlock(Location loc) {
        return location.isSameBlock(loc);
    }

    @Nullable
    public Location getBukkitLocation() {
        return location.toLocation();
    }

    @Nullable
    public ConfiguredTier getTier() {
        return getTier(tierIndex);
    }

    @Nullable
    public ConfiguredTier getNextTier() {
        return getTier(tierIndex + 1);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public ConfiguredTier getTier(int tierIndex) {
        Optional<ConfigurationImplConfig> configOptional = ConfigurationRegistry.getConfiguration(ConfigurationImplConfig.class);
        if(!configOptional.isPresent()) {
            return null;
        }
        ConfigurationImplConfig config = configOptional.get();
        List<ConfiguredTier> tiers = (List<ConfiguredTier>) config.get(ConfigurationImplConfig.ConfiguredListType.TIERS);
        if(tiers.size() <= tierIndex) {
            return null;
        }
        return tiers.get(tierIndex);
    }

    public boolean canUpgrade() {
        return getNextTier() != null;
    }

}

package me.zort.gencore.configuration.config;

import lombok.Getter;
import me.zort.gencore.configuration.ConfigurationValidatorInternal;
import me.zort.gencore.configuration.ConfiguredObject;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@Getter
public class ConfiguredTier extends ConfiguredObject {

    private final ConfigurationImplConfig config;

    private ConfiguredMob mob;
    private double upgradeCost;

    public ConfiguredTier(ConfigurationSection section, ConfigurationImplConfig config) {
        super(section);
        this.config = config;
    }

    @Nullable
    @Override
    public String checkValid() {
        ConfigurationSection section = getSection();
        String mob = section.getString("mob");
        if(mob == null) {
            return "Tier needs to have a mob set!";
        }
        String errorOnValidation = ConfigurationValidatorInternal.validateLivingMobType(mob);
        if(errorOnValidation != null) {
            return errorOnValidation;
        }
        if(!loadMob(mob.toUpperCase())) {
            this.mob = null;
            return "Mob type " + mob.toUpperCase() + " is not configured!";
        }
        return null;
    }

    @Override
    public void load() {
        upgradeCost = config.getDouble("upgrade-cost").orElse(0.0);
    }

    @SuppressWarnings("unchecked")
    private boolean loadMob(String mobName) {
        try {
            List<ConfiguredMob> configuredMobs = (List<ConfiguredMob>) config.get(ConfigurationImplConfig.ConfiguredListType.MOBS);
            Optional<ConfiguredMob> mobOptional = configuredMobs.stream()
                    .filter(mob -> mob.getMobName().equals(mobName))
                    .findFirst();
            if(!mobOptional.isPresent()) {
                return false;
            }
            mob = mobOptional.get();
        } catch(Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

}

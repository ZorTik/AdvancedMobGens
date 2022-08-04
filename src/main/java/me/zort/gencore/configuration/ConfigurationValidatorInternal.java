package me.zort.gencore.configuration;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public final class ConfigurationValidatorInternal {

    @Nullable
    public static String validateLivingMobType(String mobType) {
        boolean entityTypeValid = false;
        try {
            EntityType entityType = EntityType.valueOf(mobType.toUpperCase());
            entityTypeValid = entityType.isAlive();
        } catch(IllegalArgumentException ignored) {}
        if(!entityTypeValid) {
            return "Mob type needs to exist and be living!";
        }
        return null;
    }

}

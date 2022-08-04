package me.zort.gencore.object;

import lombok.Builder;
import lombok.Data;
import me.zort.gencore.validator.Validator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

@Builder
@Data
public class SerializableLoc implements Serializable {

    public static SerializableLoc of(Location loc) {
        Validator.requireNonNulls(loc, loc.getWorld());
        return SerializableLoc.builder()
                .world(loc.getWorld().getName())
                .x(loc.getX())
                .y(loc.getY())
                .z(loc.getZ())
                .pitch(loc.getPitch())
                .yaw(loc.getYaw())
                .build();

    }

    private String world;
    private double x;
    private double y;
    private double z;
    private float pitch;
    private float yaw;

    public boolean isSameBlock(Location l) {
        return isSameBlock(SerializableLoc.of(l));
    }

    public boolean isSameBlock(SerializableLoc l) {
        double xf = x - (x % 1.0);
        double yf = y - (y % 1.0);
        double zf = z - (z % 1.0);
        double lxf = l.getX() - (l.getX() % 1.0);
        double lyf = l.getY() - (l.getY() % 1.0);
        double lzf = l.getZ() - (l.getZ() % 1.0);
        return xf == lxf && yf == lyf && zf == lzf;
    }

    @Nullable
    public Location toLocation() {
        Validator.requireNonNulls(world, x, y, z, pitch, yaw);
        World world = Bukkit.getWorld(this.world);
        if(world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

}

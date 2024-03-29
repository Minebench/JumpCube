package de.kaleidox.jumpcube.util;

import de.kaleidox.jumpcube.JumpCube;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class BukkitUtil {
    private static final UUID NULL_UUID = new UUID(0, 0);

    private BukkitUtil() {
    }

    public static int schedule(Runnable runnable, long time, TimeUnit unit) {
        assert JumpCube.instance != null;

        return Bukkit.getScheduler()
                .scheduleSyncDelayedTask(JumpCube.instance, runnable, unit.toSeconds(time) * 20);
    }

    public static UUID getUuid(CommandSender cmdSender) {
        if (cmdSender instanceof Player player)
            return player.getUniqueId();

        return NULL_UUID;
    }

    public static World getWorld(CommandSender cmdSender) {
        if (cmdSender instanceof Entity entity)
            return entity.getWorld();
        if (cmdSender instanceof BlockCommandSender blockCommandSender)
            return blockCommandSender.getBlock().getWorld();

        return Bukkit.getWorlds().get(0);
    }

    public static Location getLocation(CommandSender cmdSender) {
        if (cmdSender instanceof Entity entity)
            return entity.getLocation();
        if (cmdSender instanceof BlockCommandSender blockCommandSender)
            return blockCommandSender.getBlock().getLocation();

        return new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
    }

    public static Player getPlayer(CommandSender cmdSender) {
        if (cmdSender instanceof Player player)
            return player;

        throw new AssertionError("Sender is not online!");
    }

    public static Optional<Material> getMaterial(@Nullable String name) {
        if (name == null) return Optional.empty();

        Material val = Material.getMaterial(name);
        if (val != null) return Optional.of(val);

        for (Material value : Material.values())
            if (value.name().equalsIgnoreCase(name))
                return Optional.of(value);
        return Optional.empty();
    }
}

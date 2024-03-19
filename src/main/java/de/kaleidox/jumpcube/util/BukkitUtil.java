package de.kaleidox.jumpcube.util;

import de.kaleidox.jumpcube.JumpCube;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class BukkitUtil {
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

        throw new AssertionError("Sender is not online!");
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

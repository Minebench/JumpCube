package de.kaleidox.jumpcube.game.listener;

import de.kaleidox.jumpcube.JumpCube;
import de.kaleidox.jumpcube.cube.Cube;
import de.kaleidox.jumpcube.game.GameManager;
import de.kaleidox.jumpcube.util.WorldUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.comroid.cmdr.spigot.SpigotCmdr;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import static de.kaleidox.jumpcube.chat.Chat.message;
import static de.kaleidox.jumpcube.util.WorldUtil.*;

public class PlayerListener extends ListenerBase implements Listener {
    private final GameManager manager;

    public PlayerListener(GameManager manager, Cube cube) {
        super(cube);
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location moveTo = event.getTo();
        if (moveTo == null) return;
        int[][] expand = expandVert(cube.getPositions());
        if (!isInside(event.getPlayer().getWorld(), xyz(moveTo))) return;
        if (!manager.joined.contains(event.getPlayer().getUniqueId())
                && !event.getPlayer().hasPermission(JumpCube.Permission.ADMIN)) {
            event.setCancelled(true);
            message(event.getPlayer(), SpigotCmdr.WarnColorizer, "Use '/jumpcube join' to join the cube!");
            return;
        }

        if (moveTo.getBlockY() >= cube.getHeight())
            manager.conclude(event.getPlayer());
        if (!manager.activeGame)
            if (inside(WorldUtil.retract(expand, 3), xyz(moveTo))) {
                event.setCancelled(true);
                message(event.getPlayer(), SpigotCmdr.ErrorColorizer, "The game didn't start yet!");
            }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getTo() != null) {
            int[] after = xyz(event.getTo());
            if (isInside(event.getPlayer().getWorld(), after)
                    && manager.activeGame
                    || !manager.joined.contains(event.getPlayer().getUniqueId()) ) {
                if (event.getPlayer().hasPermission(JumpCube.Permission.TELEPORT)) return;
                event.setCancelled(true);
                message(event.getPlayer(), SpigotCmdr.WarnColorizer, "Use '/jumpcube join' to join the cube!");
                return;
            }
        }

        int[] before = xyz(event.getFrom());
        if (isInside(event.getPlayer().getWorld(), before)
                && manager.activeGame
                && !manager.leaving.contains(event.getPlayer().getUniqueId())) {
            if (event.getPlayer().hasPermission(JumpCube.Permission.TELEPORT)) return;
            event.setCancelled(true);
            message(event.getPlayer(), SpigotCmdr.WarnColorizer, "Use '/jumpcube leave' to leave the cube!");
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (JumpCube.instance.getConfig().getBoolean("leave-on-quit", true)) {
            Player player = event.getPlayer();
            int[] xyz = xyz(player.getLocation());
            if (isInside(player.getWorld(), xyz)
                    && manager.activeGame
                    && manager.joined.contains(player.getUniqueId()))
                manager.leave(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerSpawnLocationEvent event) {
        Player player = event.getPlayer();
        int[] xyz = xyz(player.getLocation());
        if (isInside(player.getWorld(), xyz)
                && !manager.activeGame
                || !manager.joined.contains(player.getUniqueId())) {
            event.setSpawnLocation(manager.getOutLocation(event.getPlayer()));
        }
    }

    private boolean isInside(@NotNull World world, int[] xyz) {
        return world.equals(cube.getWorld()) && inside(expandVert(cube.getPositions()), xyz);
    }
}

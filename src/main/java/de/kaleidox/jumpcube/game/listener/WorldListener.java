package de.kaleidox.jumpcube.game.listener;

import de.kaleidox.jumpcube.cube.Cube;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.comroid.cmdr.spigot.SpigotCmdr;
import org.jetbrains.annotations.NotNull;

import static de.kaleidox.jumpcube.chat.Chat.message;
import static de.kaleidox.jumpcube.util.WorldUtil.*;

public class WorldListener extends ListenerBase implements Listener {
    public WorldListener(Cube cube) {
        super(cube);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockDamageEvent event) {
        if (!isInside(event.getBlock().getWorld(), xyz(event.getBlock().getLocation()))) return;

        if (event.getBlock().getType() != cube.getBlockBar().getPlaceable()) {
            event.setCancelled(true);
            message(event.getPlayer(), SpigotCmdr.WarnColorizer, "Don't destroy the cube!");
        } else {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isInside(event.getBlock().getWorld(), xyz(event.getBlock().getLocation()))) return;

        if (event.getBlock().getType() != cube.getBlockBar().getPlaceable()) {
            event.setCancelled(true);
            message(event.getPlayer(), SpigotCmdr.WarnColorizer, "Don't destroy the cube!");
        } else {
            event.setCancelled(false);
            message(event.getPlayer(), SpigotCmdr.HintColorizer, "Here's your joker!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isInside(event.getBlock().getWorld(), xyz(event.getBlock().getLocation()))) return;

        if (event.getBlockPlaced().getType() != cube.getBlockBar().getPlaceable()) {
            event.setCancelled(true);
            message(event.getPlayer(), SpigotCmdr.WarnColorizer, "You can only place %s!",
                    cube.getBlockBar().getPlaceable().name().toLowerCase());
        } else {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getItem() == null) return;
        if (!isInside(event.getPlayer().getWorld(), xyz(event.getPlayer().getLocation()))) return;

        switch (event.getItem().getType()) {
            case WATER:
            case WATER_BUCKET:
            case LAVA:
            case LAVA_BUCKET:
                event.setCancelled(true);
            default:
                event.setCancelled(false);
        }
    }

    private boolean isInside(@NotNull World world, int[] xyz) {
        return world.equals(cube.getWorld()) && inside(expandVert(cube.getPositions()), xyz);
    }
}

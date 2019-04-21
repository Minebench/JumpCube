package de.kaleidox.jumpcube.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import de.kaleidox.jumpcube.JumpCube;
import de.kaleidox.jumpcube.chat.Chat;
import de.kaleidox.jumpcube.cube.ExistingCube;
import de.kaleidox.jumpcube.exception.GameRunningException;
import de.kaleidox.jumpcube.game.listener.PlayerListener;
import de.kaleidox.jumpcube.game.listener.WorldListener;
import de.kaleidox.jumpcube.interfaces.Initializable;
import de.kaleidox.jumpcube.interfaces.Startable;
import de.kaleidox.jumpcube.util.BukkitUtil;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.Nullable;

import static java.util.concurrent.TimeUnit.SECONDS;
import static de.kaleidox.jumpcube.JumpCube.Permission.DEBUG_NOTIFY;
import static de.kaleidox.jumpcube.chat.Chat.broadcast;
import static de.kaleidox.jumpcube.chat.Chat.message;
import static de.kaleidox.jumpcube.chat.MessageLevel.HINT;
import static de.kaleidox.jumpcube.chat.MessageLevel.INFO;
import static de.kaleidox.jumpcube.chat.MessageLevel.WARN;
import static de.kaleidox.jumpcube.util.WorldUtil.location;
import static de.kaleidox.jumpcube.util.WorldUtil.xyz;

public class GameManager implements Startable, Initializable {
    public final List<UUID> leaving = new ArrayList<>();
    private final Map<UUID, PrevLoc> prevLocations = new ConcurrentHashMap<>();
    private final ExistingCube cube;
    private final List<UUID> attemptedJoin = new ArrayList<>();
    public final List<Player> joined = new ArrayList<>();
    private final int baseTime = 30;
    public boolean activeGame = false;
    private int remaining = 30;
    @Nullable private ScheduledExecutorService scheduler;
    private AtomicReference<ScheduledFuture<?>> timeBroadcastFuture;

    public GameManager(ExistingCube cube) {
        this.cube = cube;
    }

    public void join(CommandSender sender) {
        if (activeGame) throw new GameRunningException("A game is active in that cube!");

        UUID uuid = BukkitUtil.getUuid(sender);

        if (attemptedJoin.removeIf(id -> id.equals(uuid))) {
            // join user
            message(sender, INFO, "Joining cube %s...", cube.getCubeName());
            Player player = BukkitUtil.getPlayer(sender);
            player.getInventory().remove(cube.getBlockBar().getPlaceable());
            prevLocations.put(player.getUniqueId(), new PrevLoc(player));
            cube.teleportIn(player);
            joined.add(player);
            if (scheduler == null) startTimer();
            Chat.broadcast(DEBUG_NOTIFY, INFO, "Generating cube...");
            cube.generate();
        } else {
            // warn user
            message(sender, WARN, "Warning: You might die in the game! " +
                    "If you still want to play, use the command again. You will also lose any item of type %s " +
                    "from your inventory!", cube.getBlockBar().getPlaceable().name());

            attemptedJoin.add(uuid);
        }
    }

    @Override
    public void start() {
        activeGame = true;
        //cube.start();
    }

    @Override
    public void init() {
        assert JumpCube.instance != null;
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();

        pluginManager.registerEvents(new WorldListener(cube), JumpCube.instance);
        pluginManager.registerEvents(new PlayerListener(this, cube), JumpCube.instance);
    }

    public void conclude(@Nullable Player player) {
        if (activeGame) {
            scheduler = null;
            activeGame = false;

            if (player != null) {
                broadcast(HINT, "%s has reached the goal!", player.getDisplayName());
                joined.forEach(this::tpOut);
                joined.clear();
                leaving.clear();
            } else broadcast(HINT, "All players left the cube. The game has ended.");
        }
    }

    public void leave(CommandSender sender) {
        UUID uuid = BukkitUtil.getUuid(sender);
        Player player = BukkitUtil.getPlayer(sender);

        tpOut(player);

        leaving.remove(uuid);
        joined.remove(player);

        if (joined.size() == 0) conclude(null);
    }

    private void tpOut(Player player) {
        leaving.add(player.getUniqueId());
        PrevLoc pl = prevLocations.get(player.getUniqueId());

        player.teleport(location(pl.world, pl.location));
    }

    private void startTimer() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        assert baseTime % 10 == 0;

        IntStream.range(1, baseTime / 10)
                .forEach(x -> {
                    scheduler.schedule(new BroadcastRemaining(x), baseTime - x, SECONDS);
                    scheduler.schedule(new BroadcastRemaining(x * 10), baseTime - (x * 10), SECONDS);
                });

        scheduler.schedule(cube.manager::start, baseTime, SECONDS);
        new BroadcastRemaining(baseTime).run();
    }

    private class PrevLoc {
        private final World world;
        private final int[] location;

        private PrevLoc(Player player) {
            this.world = player.getWorld();
            this.location = xyz(player.getLocation());
        }
    }

    private class BroadcastRemaining implements Runnable {
        private final int val;

        private BroadcastRemaining(int val) {
            this.val = val;
        }

        @Override
        public void run() {
            broadcast(INFO, "Time remaining until cube %s will start: %s seconds", cube.getCubeName(), val);
        }
    }
}

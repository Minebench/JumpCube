package de.kaleidox.jumpcube.game;

import de.kaleidox.jumpcube.JumpCube;
import de.kaleidox.jumpcube.chat.Chat;
import de.kaleidox.jumpcube.cube.ExistingCube;
import de.kaleidox.jumpcube.exception.GameRunningException;
import de.kaleidox.jumpcube.game.listener.PlayerListener;
import de.kaleidox.jumpcube.game.listener.WorldListener;
import de.kaleidox.jumpcube.util.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.comroid.api.Initializable;
import org.comroid.api.Startable;
import org.comroid.cmdr.spigot.SpigotCmdr;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static de.kaleidox.jumpcube.JumpCube.Permission.DEBUG_NOTIFY;
import static de.kaleidox.jumpcube.chat.Chat.broadcast;
import static de.kaleidox.jumpcube.chat.Chat.message;
import static de.kaleidox.jumpcube.util.WorldUtil.location;
import static de.kaleidox.jumpcube.util.WorldUtil.xyz;
import static java.util.concurrent.TimeUnit.SECONDS;

public class GameManager implements Startable, Initializable {
    public final Set<UUID> leaving = new HashSet<>();
    public final Set<UUID> joined = new HashSet<>();
    private final Map<UUID, PrevLoc> prevLocations = new ConcurrentHashMap<>();
    private final ExistingCube cube;
    private final Set<UUID> attemptedJoin = new HashSet<>();
    private final int baseTime = 30;
    public boolean activeGame = false;
    private int remaining = 30;
    @Nullable
    private ScheduledExecutorService scheduler;
    private AtomicReference<ScheduledFuture<?>> timeBroadcastFuture;

    public GameManager(ExistingCube cube) {
        this.cube = cube;
    }

    public void join(Player player) {
        if (activeGame) throw new GameRunningException("A game is active in that cube!");

        if (attemptedJoin.remove(player.getUniqueId())) {
            // join user
            message(player, SpigotCmdr.InfoColorizer, "Joining cube %s...", cube.getCubeName());
            if (!player.hasPermission(JumpCube.Permission.BRING_PLACEABLE))
                player.getInventory().remove(cube.getBlockBar().getPlaceable().getMaterial());
            prevLocations.put(player.getUniqueId(), new PrevLoc(player));
            joined.add(player.getUniqueId());
            cube.teleportIn(player);
            if (scheduler == null) startTimer();
            Chat.broadcast(DEBUG_NOTIFY, SpigotCmdr.InfoColorizer, "Generating cube...");
            if (joined.size() == 1) cube.generate();
        } else {
            // warn user
            message(player, SpigotCmdr.WarnColorizer, "Warning: You might die in the game! " +
                    "If you still want to play, use the command again");

            if (!player.hasPermission(JumpCube.Permission.BRING_PLACEABLE))
                message(player, SpigotCmdr.WarnColorizer, "You will also lose any item of type %s " +
                        "from your inventory!", cube.getBlockBar().getPlaceable().getMaterial().name());

            attemptedJoin.add(player.getUniqueId());
        }
    }

    @Override
    public void start() {
        activeGame = true;
        cube.start();
    }

    @Override
    public void initialize() {
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
                broadcast(SpigotCmdr.HintColorizer, "%s has reached the goal!", player.getDisplayName());
                joined.forEach(this::tpOut);
                for (String command : JumpCube.instance.getConfig().getStringList("price-commands")) {
                    JumpCube.instance.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                            command.replace("%player%", player.getName()));
                }
            } else broadcast(SpigotCmdr.HintColorizer, "All players left the cube. The game has ended.");
            joined.clear();
            leaving.clear();
        }
    }

    public void leave(CommandSender sender) {
        UUID uuid = BukkitUtil.getUuid(sender);
        Player player = BukkitUtil.getPlayer(sender);

        tpOut(player);

        leaving.remove(uuid);
        joined.remove(uuid);

        if (joined.size() == 0) conclude(null);
    }

    private void tpOut(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            tpOut(player);
        }
    }

    private void tpOut(Player player) {
        leaving.add(player.getUniqueId());
        player.teleport(getOutLocation(player));
    }

    public Location getOutLocation(Player player) {
        PrevLoc pl = prevLocations.get(player.getUniqueId());
        if (pl != null) {
            return location(pl.world, pl.location);
        }
        return cube.getWorld().getSpawnLocation();
    }

    private void startTimer() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        assert baseTime % 10 == 0;

        IntStream.range(1, baseTime / 10)
                .forEach(x -> {
                    scheduler.schedule(new BroadcastRemaining(x), baseTime - x, SECONDS);
                    scheduler.schedule(new BroadcastRemaining(x * 10), baseTime - (x * 10), SECONDS);
                });

        scheduler.schedule(() -> Bukkit.getScheduler().getMainThreadExecutor(JumpCube.instance).execute(this::start), baseTime, SECONDS);
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
            Bukkit.getScheduler().getMainThreadExecutor(JumpCube.instance).execute(
                    () -> broadcast(SpigotCmdr.InfoColorizer, "Time remaining until cube %s will start: %s seconds", cube.getCubeName(), val));
        }
    }
}

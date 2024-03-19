package de.kaleidox.jumpcube.cube;

import de.kaleidox.jumpcube.JumpCube;
import de.kaleidox.jumpcube.exception.DuplicateCubeException;
import de.kaleidox.jumpcube.exception.NoSuchCubeException;
import de.kaleidox.jumpcube.game.GameManager;
import de.kaleidox.jumpcube.interfaces.Generatable;
import de.kaleidox.jumpcube.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.comroid.api.Initializable;
import org.comroid.api.Startable;
import org.comroid.cmdr.spigot.SpigotCmdr;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static de.kaleidox.jumpcube.chat.Chat.message;
import static de.kaleidox.jumpcube.cube.BlockBar.MaterialGroup.*;
import static de.kaleidox.jumpcube.util.MathUtil.dist;
import static de.kaleidox.jumpcube.util.MathUtil.mid;
import static de.kaleidox.jumpcube.util.WorldUtil.mid;
import static de.kaleidox.jumpcube.util.WorldUtil.xyz;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.System.nanoTime;
import static org.bukkit.Material.*;

public class ExistingCube implements Cube, Generatable, Startable, Initializable {
    private final static Map<String, Cube> instances = new ConcurrentHashMap<>();
    private static final BlockData AIR_DATA = AIR.createBlockData();
    private static final BlockData LAVA_DATA = LAVA.createBlockData();
    private static final BlockData GLASS_DATA = GLASS.createBlockData();
    private static final BlockData GLASS_PANE_DATA = GLASS_PANE.createBlockData();
    public final GameManager manager;
    private final String name;
    private final World world;
    private final int[][] pos;
    private final int minX, maxX, minZ, maxZ;
    private final BlockBar bar;
    @Deprecated
    private final int galleryHeight = 19;
    private final double density = 0.183; // todo Add changeable density
    @Deprecated
    private final int height = 110; // todo Add changeable height
    private final double spacing = 0.22;
    private int[][] tpPos;
    private int tpCycle = -1;
    private long startNanos = -1;

    @Override
    public String getCubeName() {
        return name;
    }

    @Override
    public int[][] getPositions() {
        return pos;
    }

    @Override
    public int getGalleryHeight() {
        int galleryHeight = JumpCube.instance.getConfig().getInt("cubes." + getCubeName() + ".gallery.height", -1);
        return galleryHeight == -1 ? JumpCube.instance.getConfig().getInt("cube.defaults.gallery.height") : galleryHeight;
    }

    @Override
    public int getHeight() {
        int height = JumpCube.instance.getConfig().getInt("cubes." + getCubeName() + ".height", -1);
        return height == -1 ? JumpCube.instance.getConfig().getInt("cube.defaults.height") : height;
    }

    @Override
    public int getBottom() {
        int bottom = JumpCube.instance.getConfig().getInt("cubes." + getCubeName() + ".bottom", -1);
        return bottom == -1 ? JumpCube.instance.getConfig().getInt("cube.defaults.bottom") : bottom;
    }

    @Override
    public BlockBar getBlockBar() {
        return bar;
    }

    @Override
    public World getWorld() {
        return world;
    }

    public static Stream<String> getNames() {
        return instances.values().stream().map(Cube::getCubeName);
    }

    private ExistingCube(String name, World world, int[][] positions, BlockBar bar) {
        if (instances.containsKey(name)) throw new DuplicateCubeException(name);

        this.name = name;
        this.world = world;
        this.pos = positions;
        this.bar = bar;

        minX = min(pos[0][0], pos[1][0]);
        maxX = max(pos[0][0], pos[1][0]);
        minZ = min(pos[0][2], pos[1][2]);
        maxZ = max(pos[0][2], pos[1][2]);

        this.manager = new GameManager(this);

        instances.put(name, this);

        initialize();
    }

    @Nullable
    public static ExistingCube get(String name) {
        return (ExistingCube) instances.get(name);
    }

    public static boolean exists(String name) {
        return instances.containsKey(name);
    }

    public static Cube getSelection(Player player) throws NoSuchCubeException {
        assert JumpCube.instance != null;

        return Optional.ofNullable(JumpCube.instance.selections.get(player.getUniqueId()))
                .orElseGet(() -> {
                    Cube sel = null;
                    if (instances.size() == 0)
                        return null;
                    if (instances.size() == 1)
                        sel = instances.entrySet().iterator().next().getValue();
                    if (sel == null)
                        sel = instances.values()
                                .stream()
                                .filter(cube -> cube.getWorld().equals(player.getWorld()))
                                .min(Comparator.comparingDouble(cube -> WorldUtil.dist(
                                        mid(cube.getPositions()),
                                        xyz(player.getLocation())
                                )))
                                .orElseThrow(() -> new NoSuchCubeException(player));
                    JumpCube.instance.selections.put(player.getUniqueId(), sel);
                    message(player, SpigotCmdr.InfoColorizer, "Cube %s was automatically selected!", sel.getCubeName());
                    return sel;
                });
    }

    public static ExistingCube load(final FileConfiguration config, String name, @Nullable BlockBar bar)
            throws DuplicateCubeException {
        final String basePath = "cubes." + name + ".";

        if (bar == null) bar = BlockBar.create(config, basePath + "bar.");

        // get world
        World world = Bukkit.getWorld(Objects.requireNonNull(config.getString(basePath + "world"),
                "No world defined for cube: " + name));

        // get positions
        int[][] locs = new int[2][3];

        locs[0][0] = config.getInt(basePath + "pos1.x");
        locs[0][1] = config.getInt(basePath + "pos1.y");
        locs[0][2] = config.getInt(basePath + "pos1.z");

        locs[1][0] = config.getInt(basePath + "pos2.x");
        locs[1][1] = config.getInt(basePath + "pos2.y");
        locs[1][2] = config.getInt(basePath + "pos2.z");

        assert world != null : "Unknown world: " + config.getString(basePath + "world");

        return new ExistingCube(name, world, locs, bar);
    }

    @Override
    public void delete() {
        assert JumpCube.instance != null;

        // remove from maps
        instances.remove(name, this);
        JumpCube.instance.selections.forEach((key, value) -> {
            if (value == this)
                JumpCube.instance.selections.remove(key, value);
        });
    }

    public void teleportIn(Player player) {
        if (++tpCycle >= tpPos.length) tpCycle = 0;
        Location location = WorldUtil.location(world, tpPos[tpCycle]);
        player.teleport(location.add(0, 1.2, 0));
    }

    public void generateFull() {
        startNanos = nanoTime();

        final int maxY = max(pos[0][1], pos[1][1]);

        final int height = getHeight();
        final int bottom = getBottom();
        final int galleryHeight = getGalleryHeight();

        int x, y, z;

        for (x = minX + 1; x < maxX; x++)
            for (z = minZ + 1; z < maxZ; z++)
                for (y = height; y > bottom; y--)
                    world.getBlockAt(x, y, z).setBlockData(AIR_DATA);

        for (int off : new int[]{0, 1, 2}) {
            final int minXloop = minX + off;
            final int maxXloop = maxX - off;
            final int minZloop = minZ + off;
            final int maxZloop = maxZ - off;

            for (x = minXloop; x <= maxXloop; x++)
                for (z = minZloop; z <= maxZloop; z++) {
                    if (x == minXloop || x == maxXloop || z == minZloop || z == maxZloop) {
                        if (off == 0)
                            for (y = maxY; y > bottom; y--) {
                                Block block = world.getBlockAt(x, y, z);
                                if (y < 50 || block.getType() != AIR)
                                    block.setBlockData(bar.getRandomBlockData(WALLS));
                            }
                        else {
                            world.getBlockAt(x, galleryHeight, z).setBlockData(bar.getRandomBlockData(GALLERY));
                            if (off == 1)
                                world.getBlockAt(x, galleryHeight + 3, z).setBlockData(GLASS_DATA);
                        }
                    }

                    if (off == 1) {
                        world.getBlockAt(x, bottom + 1, z).setBlockData(LAVA_DATA);
                        world.getBlockAt(x, bottom + 2, z).setBlockData(LAVA_DATA);
                        world.getBlockAt(x, bottom + 3, z).setBlockData(LAVA_DATA);
                    }
                }
        }

        final int spaceX = (int) (Math.abs(pos[1][0] - pos[0][0]) * spacing);
        final int spaceZ = (int) (Math.abs(pos[1][2] - pos[0][2]) * spacing);

        IntStream.range(2, mid(spaceX, spaceZ))
                .forEach(off -> {
                    final int minXoff = minX + off;
                    final int maxXoff = maxX - off;
                    final int minZoff = minZ + off;
                    final int maxZoff = maxZ - off;

                    int x_, z_;

                    for (x_ = minXoff; x_ <= maxXoff; x_++)
                        for (z_ = minZoff; z_ <= maxZoff; z_++)
                            if (off == 2 && (x_ == minXoff || x_ == maxXoff || z_ == minZoff || z_ == maxZoff))
                                // renew glass panes
                                world.getBlockAt(x_, galleryHeight + 1, z_).setBlockData(GLASS_PANE_DATA);
                            else // remove gallery extensions
                                world.getBlockAt(x_, galleryHeight, z_).setBlockData(AIR_DATA);
                });

        generate();
    }

    @Override
    public void generate() {
        if (startNanos == -1) startNanos = nanoTime();

        final int spaceX = (int) (Math.abs(pos[1][0] - pos[0][0]) * spacing);
        final int spaceZ = (int) (Math.abs(pos[1][2] - pos[0][2]) * spacing);

        int x, y, z;
        final int height = getHeight();
        final int bottom = getBottom();

        for (x = minX + spaceX; x <= maxX - spaceX; x++)
            for (z = minZ + spaceZ; z <= maxZ - spaceZ; z++)
                for (y = bottom + 10; y < height; y++)
                    if (JumpCube.rng.nextDouble() % 1 > density) world.getBlockAt(x, y, z).setBlockData(AIR_DATA);
                    else world.getBlockAt(x, y, z).setBlockData(bar.getRandomBlockData(CUBE));

        assert JumpCube.instance != null;
        JumpCube.instance.getLogger().info("Cube " + name + " was generated, took "
                + (nanoTime() - startNanos) + " nanoseconds.");
        startNanos = -1;

        start();
    }

    @Override
    public void start() {
        JumpCube.instance.getLogger().fine("gen bridgeZ");
        final int spaceX = (int) (Math.abs(pos[1][0] - pos[0][0]) * spacing);
        final int spaceZ = (int) (Math.abs(pos[1][2] - pos[0][2]) * spacing);

        final int midX = mid(minX, maxX);
        final int midZ = mid(minZ, maxZ);

        final int xDistA = dist(midX, minX);
        final int xDistB = dist(midX, maxX);
        final int zDistA = dist(midZ, minZ);
        final int zDistB = dist(midZ, maxZ);

        final int galleryHeight = getGalleryHeight();

        JumpCube.instance.getLogger().fine("spaceZ = " + spaceZ);

        generateGallery(spaceZ, midX, xDistA, xDistB);
        generateGallery(spaceX, midZ, zDistA, zDistB);

        world.getBlockAt(midX, galleryHeight + 1, minZ + 2).setBlockData(AIR_DATA);
        world.getBlockAt(midX, galleryHeight + 1, maxZ - 2).setBlockData(AIR_DATA);
        world.getBlockAt(minX + 2, galleryHeight + 1, midZ).setBlockData(AIR_DATA);
        world.getBlockAt(maxZ - 2, galleryHeight + 1, midZ).setBlockData(AIR_DATA);
    }

    private void generateGallery(int space, int mid, int distA, int distB) {
        final int galleryHeight = getGalleryHeight();

        int otherX = (mid - Integer.compare(distA, distB));
        (otherX > mid ? IntStream.range(mid, otherX)
                : (otherX == mid ? IntStream.range(otherX, mid + 1)
                : IntStream.range(otherX, mid)))
                .forEach(xBridge -> IntStream.range(2, space)
                        .flatMap(zOff -> IntStream.of(minZ, maxZ)
                                .map(z -> z == minZ ? z + zOff : z - zOff))
                        .forEach(zBridge -> world.getBlockAt(xBridge, galleryHeight, zBridge)
                                .setBlockData(bar.getRandomBlockData(GALLERY))));
    }

    @Override
    public void initialize() {
        manager.initialize();

        final int galleryHeight = getGalleryHeight();

        this.tpPos = new int[][]{
                new int[]{minX + 1, galleryHeight + 1, minZ + 1},
                new int[]{maxX - 1, galleryHeight + 1, maxZ - 1},
                new int[]{minX + 1, galleryHeight + 1, maxZ - 1},
                new int[]{maxX - 1, galleryHeight + 1, minZ + 1}
        };
    }

    public final static class Commands {
        public static void regenerate(CommandSender sender, Cube sel, boolean full) {
            sel.getBlockBar().validate();
            if (full) ((ExistingCube) sel).generateFull();
            else ((ExistingCube) sel).generate();

            message(sender, SpigotCmdr.InfoColorizer, "Cube was regenerated!");
        }
    }
}

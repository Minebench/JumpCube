package de.kaleidox.jumpcube.cube;

import java.util.Arrays;
import java.util.Objects;

import de.kaleidox.jumpcube.exception.InvalidBlockBarException;
import de.kaleidox.jumpcube.util.BukkitUtil;
import de.kaleidox.jumpcube.interfaces.Generatable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import static de.kaleidox.jumpcube.JumpCube.rng;

public class BlockBar implements Generatable {
    private static Material[] configMaterials = new Material[8];
    private final World world;
    private final @Range(from = 3, to = 3) int[] xyz;
    private final Material[] materials;

    public BlockBar(Player player) {
        this(player, null);
    }

    public BlockBar(Player player, @Nullable Material[] materials) throws InvalidBlockBarException {
        if (materials != null) this.materials = materials;
        else this.materials = Arrays.copyOf(configMaterials, configMaterials.length);

        this.world = player.getWorld();
        this.xyz = BukkitUtil.getXYZ(player.getLocation());

        generate();
        validate();
    }

    private BlockBar(World world, int[] xyz) {
        this.world = world;
        this.xyz = xyz;
        this.materials = new Material[8];

        refresh();
    }

    @Override
    public void generate() {
        final int[] c = {0};

        for (int addY : new int[]{1, 0})
            for (int addX : new int[]{1, 2, 3, 4})
                world.getBlockAt(xyz[0] + addX, xyz[1] + addY, xyz[2]).setType(materials[c[0]++]);
    }

    public Material getRandomMaterial(@MagicConstant(valuesFromClass = MaterialGroup.class) int group) {
        switch (group) {
            case MaterialGroup.CUBE:
                if (rng.nextDouble() % 1 < 0.985) return materials[rng.nextInt(3)];
                return materials[3];
            case MaterialGroup.WALLS:
                return materials[rng.nextInt(3) + 4];
            case MaterialGroup.GALLERY:
                return materials[7];
        }

        return Material.LIGHT_GRAY_WOOL;
    }

    public void validate() throws InvalidBlockBarException {
        refresh();

        for (int i = 0; i < materials.length; i++) {
            if (!materials[i].isSolid())
                throw new InvalidBlockBarException(materials[i], InvalidBlockBarException.Cause.NON_SOLID);
            if (i != 3 && materials[i].isInteractable())
                throw new InvalidBlockBarException(materials[i], InvalidBlockBarException.Cause.INTERACTABLE);
        }
    }

    public void refresh() {
        final int[] c = {0};

        for (int addY : new int[]{1, 0})
            for (int addX : new int[]{1, 2, 3, 4})
                materials[c[0]++] = world.getBlockAt(xyz[0] + addX, xyz[1] + addY, xyz[2]).getType();
    }

    public void save(final FileConfiguration config, final String basePath) {
        // set world
        config.set(basePath + "world", world.getName());

        // set base position
        config.set(basePath + "pos.x", xyz[0]);
        config.set(basePath + "pos.y", xyz[1]);
        config.set(basePath + "pos.z", xyz[2]);
    }

    public static void initConfig(FileConfiguration config) {
        configMaterials[0] = BukkitUtil.getMaterial(config.getString("cube.defaults.bar.a"))
                .orElse(Material.RED_WOOL);
        configMaterials[1] = BukkitUtil.getMaterial(config.getString("cube.defaults.bar.b"))
                .orElse(Material.YELLOW_WOOL);
        configMaterials[2] = BukkitUtil.getMaterial(config.getString("cube.defaults.bar.c"))
                .orElse(Material.BLUE_WOOL);
        configMaterials[3] = BukkitUtil.getMaterial(config.getString("cube.defaults.bar.placeable"))
                .orElse(Material.PUMPKIN);
        configMaterials[4] = BukkitUtil.getMaterial(config.getString("cube.defaults.bar.aFix"))
                .orElse(Material.RED_CONCRETE);
        configMaterials[5] = BukkitUtil.getMaterial(config.getString("cube.defaults.bar.bFix"))
                .orElse(Material.YELLOW_CONCRETE);
        configMaterials[6] = BukkitUtil.getMaterial(config.getString("cube.defaults.bar.cFix"))
                .orElse(Material.BLUE_CONCRETE);
        configMaterials[7] = BukkitUtil.getMaterial(config.getString("cube.defaults.bar.dFix"))
                .orElse(Material.LIGHT_GRAY_CONCRETE);
    }

    public static BlockBar create(FileConfiguration config, String basePath) {
        World world = Bukkit.getWorld(Objects.requireNonNull(config.getString(basePath + "world"),
                "No world defined for bar!"));
        int[] xyz = new int[]{
                config.getInt(basePath + "pos.x"),
                config.getInt(basePath + "pos.y"),
                config.getInt(basePath + "pos.z")
        };

        assert world != null : "Unknown world: " + config.getString(basePath + "world");

        return new BlockBar(world, xyz);
    }

    public final static class MaterialGroup {
        public static final int CUBE = 0;
        public static final int WALLS = 1;
        public static final int GALLERY = 2;
    }
}

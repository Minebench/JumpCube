package de.kaleidox.jumpcube;

import de.kaleidox.jumpcube.cmd.JumpCubeCommand;
import de.kaleidox.jumpcube.cube.BlockBar;
import de.kaleidox.jumpcube.cube.Cube;
import de.kaleidox.jumpcube.cube.ExistingCube;
import de.kaleidox.jumpcube.util.BukkitUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.comroid.cmdr.spigot.SpigotCmdr;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static de.kaleidox.jumpcube.chat.Chat.message;

public final class JumpCube extends SpigotCmdr {
    public static final Random rng = new Random();
    @Nullable
    public static JumpCube instance;

    public Map<UUID, Cube> selections = new ConcurrentHashMap<>();
    private Logger logger;

    @Override
    public String getChatPrefix() {
        return ChatColor.DARK_GRAY + "[" +
                ChatColor.BLUE + "JumpCube" +
                ChatColor.DARK_GRAY + "] ";
    }

    public static Stream<String> getCubeNames() {
        return ExistingCube.getNames();
    }

    public static boolean validateSelection(CommandSender sender, Cube sel) {
        if (sel == null) {
            message(sender, ErrorColorizer, "No cube selected!");
            return false;
        }
        if (!(sel instanceof ExistingCube)) {
            message(sender, ErrorColorizer, "Cube %s is not finished!", sel.getCubeName());
            return false;
        }
        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();

        this.logger = getLogger();

        registerCommands(JumpCubeCommand.class);

        saveConfig();
        saveDefaultConfig();

        final FileConfiguration config = getConfig();
        if (!config.isSet("leave-on-quit"))
            config.set("leave-on-quit", true);
        if (!config.isSet("cube.defaults.bar.a"))
            config.set("cube.defaults.bar.a", "red_wool");
        if (!config.isSet("cube.defaults.bar.b"))
            config.set("cube.defaults.bar.b", "yellow_wool");
        if (!config.isSet("cube.defaults.bar.c"))
            config.set("cube.defaults.bar.c", "blue_wool");
        if (!config.isSet("cube.defaults.bar.placeable"))
            config.set("cube.defaults.bar.placeable", "pumpkin");
        if (!config.isSet("cube.defaults.bar.aFix"))
            config.set("cube.defaults.bar.aFix", "red_concrete");
        if (!config.isSet("cube.defaults.bar.bFix"))
            config.set("cube.defaults.bar.bFix", "yellow_concrete");
        if (!config.isSet("cube.defaults.bar.cFix"))
            config.set("cube.defaults.bar.cFix", "blue_concrete");
        if (!config.isSet("cube.defaults.bar.dFix"))
            config.set("cube.defaults.bar.dFix", "light_gray_concrete");
        if (!config.isSet("cube.defaults.bottom"))
            config.set("cube.defaults.bottom", -60);
        if (!config.isSet("cube.defaults.height"))
            config.set("cube.defaults.height", 120);
        if (!config.isSet("cube.defaults.gallery.height"))
            config.set("cube.defaults.gallery.height", -20);

        saveConfig();

        BlockBar.initConfig(config);

        logger.info("JumpCube loaded!");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        instance = null;

        logger.info("JumpCube disabled!");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;

        final FileConfiguration config = getConfig();

        Optional.ofNullable(config.getString("cubes.created"))
                .map(str -> str.split(";"))
                .map(Arrays::asList)
                .ifPresent(list -> list.forEach(cubeName -> {
                    try {
                        ExistingCube.load(config, cubeName, null);
                        logger.info("Loaded cube: " + cubeName);
                    } catch (Throwable t) {
                        logger.throwing(ExistingCube.class.getName(), "load", t);
                    }
                }));

        logger.info("JumpCube enabled!");
        logger.info("Please report bugs at https://github.com/burdoto/jumpcube/issues");
    }

    public boolean checkPerm(CommandSender sender, String permission) {
        if (sender.hasPermission(permission))
            return true;
        else {
            messagePerm(sender, permission);
            return false;
        }
    }

    private void messagePerm(CommandSender sender, String permission) {
        message(BukkitUtil.getPlayer(sender), ErrorColorizer, "You are missing the permission: %s", permission);
    }

    public static final class Permission {
        public static final String USER = "jumpcube.user";

        public static final String START_EARLY = "jumpcube.vip.earlystart";
        public static final String BRING_PLACEABLE = "jumpcube.vip.bringplaceable";

        public static final String TELEPORT = "jumpcube.mod.teleport";
        public static final String REGENERATE = "jumpcube.mod.regenerate";

        public static final String ADMIN = "jumpcube.admin";
        public static final String DEBUG_NOTIFY = "jumpcube.admin.debug";
    }
}

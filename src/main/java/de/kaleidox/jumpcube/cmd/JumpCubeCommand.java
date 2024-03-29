package de.kaleidox.jumpcube.cmd;

import de.kaleidox.jumpcube.JumpCube;
import de.kaleidox.jumpcube.cube.BlockBar;
import de.kaleidox.jumpcube.cube.Cube;
import de.kaleidox.jumpcube.cube.CubeCreationTool;
import de.kaleidox.jumpcube.cube.ExistingCube;
import de.kaleidox.jumpcube.exception.NoSuchCubeException;
import de.kaleidox.jumpcube.util.BukkitUtil;
import org.bukkit.command.CommandSender;
import org.comroid.cmdr.model.Command;

import java.util.UUID;

import static de.kaleidox.jumpcube.chat.Chat.message;
import static org.comroid.cmdr.spigot.SpigotCmdr.ErrorColorizer;
import static org.comroid.cmdr.spigot.SpigotCmdr.InfoColorizer;
import static org.comroid.cmdr.spigot.SpigotCmdr.WarnColorizer;

@Command(name = "jumpcube")
@Command.Alias("jc")
@Command.Default("version")
public class JumpCubeCommand {
    @Command
    public static String version() {
        return InfoColorizer.makeMessage("JumpCube version %s", JumpCube.instance.getDescription().getVersion());
    }

    @Command
    public static String reload() {
        JumpCube.instance.reloadConfig();
        BlockBar.initConfig(JumpCube.instance.getConfig());
        return WarnColorizer.makeMessage("Partially reloaded the config. Please restart the server if you want"
                + " to edit cube-specific values as not everything can be reloaded yet!");
    }

    @Command
    public static void create(JumpCube pl, CommandSender sender, @Command.Arg String name) {
        if (!pl.checkPerm(sender, JumpCube.Permission.ADMIN)) return;
        UUID senderUuid = BukkitUtil.getUuid(sender);
        Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));

        if (ExistingCube.exists(name)) {
            message(sender, ErrorColorizer, "A cube with the name %s already exists!", name);
            return;
        }

        if (sel instanceof CubeCreationTool && !((CubeCreationTool) sel).isReady()) {
            // delete old, nonready selection first
            sel.delete();
            pl.selections.remove(senderUuid);
        }

        CubeCreationTool creationTool = new CubeCreationTool(BukkitUtil.getPlayer(sender));
        creationTool.setName(name);
        pl.selections.put(senderUuid, creationTool);
        message(sender, InfoColorizer, "Cube %s creation started!", name);
    }

    @Command
    @Command.Alias("sel")
    public static void select(JumpCube pl, CommandSender sender, @Command.Arg(autoComplete = "°getCubeNames") String name) {
        if (!pl.checkPerm(sender, JumpCube.Permission.USER)) return;
        UUID senderUuid = BukkitUtil.getUuid(sender);
        Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));

        if (sel != null && sel.getCubeName().equals(name)) {
            message(sender, InfoColorizer, "Cube %s is already selected!", name);
            return;
        }
        if (!ExistingCube.exists(name)) throw new NoSuchCubeException(name);

        ExistingCube cube = ExistingCube.get(name);
        assert cube != null;
        pl.selections.put(senderUuid, cube);
        message(sender, InfoColorizer, "Cube %s selected!", name);
    }

    @Command
    public static void pos(JumpCube pl, CommandSender sender, @Command.Arg(autoComplete = {"1", "2"}) int n) {
        if (!pl.checkPerm(sender, JumpCube.Permission.ADMIN)) return;
        Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));

        if (sel == null)
            throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
        CubeCreationTool.Commands.pos(sender, sel, n);
    }

    @Command
    public static void bar(JumpCube pl, CommandSender sender) {
        if (!pl.checkPerm(sender, JumpCube.Permission.ADMIN)) return;
        Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));

        if (sel == null)
            throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
        CubeCreationTool.Commands.bar(sender, sel);
    }

    @Command
    public static void confirm(JumpCube pl, CommandSender sender) {
        if (!pl.checkPerm(sender, JumpCube.Permission.ADMIN)) return;
        Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));

        if (sel == null)
            throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
        ExistingCube cube = CubeCreationTool.Commands.confirm(sender, sel);
        if (cube != null) {
            pl.selections.put(BukkitUtil.getUuid(sender), cube);
        }
    }

    @Command
    @Command.Alias("regen")
    public static void regenerate(JumpCube pl, CommandSender sender, @Command.Arg(required = false, autoComplete = {"true", "false"}) boolean full) {
        if (!pl.checkPerm(sender, JumpCube.Permission.REGENERATE)) return;
        Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));

        if (sel == null)
            throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
        if (!JumpCube.validateSelection(sender, sel)) return;
        ExistingCube.Commands.regenerate(sender, sel, full);
    }

    @Command
    public static void join(JumpCube pl, CommandSender sender) {
        if (!pl.checkPerm(sender, JumpCube.Permission.USER)) return;
        Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));
        if (sel == null)
            throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
        if (!JumpCube.validateSelection(sender, sel)) return;
        ((ExistingCube) sel).manager.join(sender);
    }

    @Command
    public static void leave(JumpCube pl, CommandSender sender) {
        Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));
        if (sel == null)
            throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
        if (!JumpCube.validateSelection(sender, sel)) return;
        ((ExistingCube) sel).manager.leave(sender);
    }

    @Command
    public static void start(JumpCube pl, CommandSender sender) {
        if (!pl.checkPerm(sender, JumpCube.Permission.START_EARLY)) return;
        Cube sel = ExistingCube.getSelection(BukkitUtil.getPlayer(sender));
        if (sel == null)
            throw new NoSuchCubeException(BukkitUtil.getPlayer(sender));
        if (!JumpCube.validateSelection(sender, sel)) return;
        ((ExistingCube) sel).manager.start();
    }
}

package de.kaleidox.jumpcube.exception;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.comroid.cmdr.spigot.InnerCommandException;
import org.comroid.cmdr.spigot.SpigotCmdr;

public final class NoSuchCubeException extends InnerCommandException {
    public NoSuchCubeException(Player selectForPlayer) {
        super("Could not auto-select cube for player: " + selectForPlayer.getDisplayName() + "" +
                "\nPlease use " + ChatColor.LIGHT_PURPLE + "/jc select <Name>" +
                SpigotCmdr.ExceptionColorizer.getPrimaryColor() + " to select a cube.");
    }

    public NoSuchCubeException(String name) {
        super(SpigotCmdr.ErrorColorizer, "No cube with name " + ChatColor.BLUE + name + SpigotCmdr.ErrorColorizer.getPrimaryColor() + " could be found.");
    }
}

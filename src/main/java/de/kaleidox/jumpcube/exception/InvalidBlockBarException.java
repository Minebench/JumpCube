package de.kaleidox.jumpcube.exception;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.comroid.cmdr.spigot.InnerCommandException;

public final class InvalidBlockBarException extends InnerCommandException {
    public InvalidBlockBarException(Material errorMaterial, Cause cause) {
        super("Invalid block: " + errorMaterial.name() + " is not a valid block; it is " + cause.s);
    }

    public InvalidBlockBarException(BlockData errorBlockData, Cause cause) {
        super("Invalid block: " + errorBlockData.getAsString() + " is not a valid block; it is " + cause.s);
    }

    public enum Cause {
        NON_SOLID("not solid"),
        INTERACTABLE("interactable (only placeables can be interactable)");

        private final String s;

        Cause(String s) {
            this.s = s;
        }
    }
}
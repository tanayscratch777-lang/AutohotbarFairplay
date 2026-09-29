package com.hapnoid.autohotbar.client.highlight;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Shows the player's ACTUAL remapped key for a hotbar slot (e.g. "P" if they
 * rebound slot 2 to P), falling back to the plain slot number if the key is
 * unbound. `Minecraft.options.keyHotbarSlots` is the vanilla array of the 9
 * hotbar keybinds (index 0 = slot 1 ... index 8 = slot 9) - this array has
 * existed under this name for a very long time and is a low-risk lookup.
 */
public final class KeyLabelResolver {

    private KeyLabelResolver() {
    }

    /** slotNumber is 1-9. */
    public static String labelFor(int slotNumber) {
        Minecraft client = Minecraft.getInstance();
        if (client.options == null) {
            return String.valueOf(slotNumber);
        }
        KeyMapping[] hotbarKeys = client.options.keyHotbarSlots;
        int index = slotNumber - 1;
        if (hotbarKeys == null || index < 0 || index >= hotbarKeys.length) {
            return String.valueOf(slotNumber);
        }
        try {
            String label = hotbarKeys[index].getTranslatedKeyMessage().getString();
            if (label == null || label.isBlank() || label.toLowerCase(java.util.Locale.ROOT).contains("unknown")) {
                return String.valueOf(slotNumber);
            }
            return label;
        } catch (Throwable t) {
            // If this exact accessor moved in your MC version, fall back cleanly
            // instead of crashing the highlight overlay - see ItemInspector's
            // class-level note for the general pattern used throughout this mod.
            return String.valueOf(slotNumber);
        }
    }
}

package com.hapnoid.autohotbar.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The lowest-risk kind of mixin: an @Accessor just exposes an existing field
 * publicly, so it only depends on the target class name and field name
 * (leftPos/topPos have been stable in Mojang's own naming for a very long
 * time), not on any method signature or injection point. Used by
 * InventorySlotHighlighter to know where the currently-open screen's slot
 * grid starts on screen.
 */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos")
    int autohotbar$getLeftPos();

    @Accessor("topPos")
    int autohotbar$getTopPos();
}

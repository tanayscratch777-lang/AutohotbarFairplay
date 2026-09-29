package com.hapnoid.autohotbar.client.mixin;

import com.hapnoid.autohotbar.client.highlight.InventorySlotHighlighter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ============================================================================
 *  OPTIONAL / SOFT-FAIL MIXIN - fine if this one doesn't apply
 * ============================================================================
 * Per the NeoForge/Fabric 26.1.2 screen docs, a container screen draws in
 * strata: background -> contents/widgets -> labels (extractLabels) -> carried
 * (cursor-held) item -> tooltip. Our highlight was drawing at the very end
 * (after tooltip and the carried item), which is why it visually sat ON TOP
 * of both. Injecting at the tail of extractLabels draws it in the same slot
 * vanilla item-count labels use - after normal slot contents, but before the
 * held item and tooltip get drawn on top of everything.
 *
 * This is marked "required": false in its mixin config specifically because
 * @Inject depends on an exact method signature guess (name + parameter
 * types), which is riskier than the plain field-accessor mixin elsewhere in
 * this mod. If the guess is wrong, Fabric Loader will just skip this mixin
 * (soft warning in the log) instead of crashing the whole mod - the
 * highlight will keep working exactly as it does today (drawn on top of
 * tooltips) until this is fixed, rather than breaking anything.
 * ============================================================================
 */
@Mixin(AbstractContainerScreen.class)
public class ContainerScreenLabelsMixin {

    @Inject(method = "extractLabels", at = @At("TAIL"))
    private void autohotbar$drawHighlightBeforeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        InventorySlotHighlighter.drawHighlights((AbstractContainerScreen<?>) (Object) this, graphics);
        InventorySlotHighlighter.markHandledByMixinThisFrame();
    }
}

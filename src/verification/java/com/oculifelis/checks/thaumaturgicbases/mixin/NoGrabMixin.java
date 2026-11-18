package com.oculifelis.checks.thaumaturgicbases.mixin;

import com.oculifelis.checks.thaumaturgicbases.VerificationClient;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only the separate development check mod loads this mouse guard. */
@Mixin(MouseHandler.class)
public final class NoGrabMixin {
    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true, require = 1)
    private void checks$noGrab(CallbackInfo ci) {
        VerificationClient.noteGrabCancellation();
        ci.cancel();
    }
}

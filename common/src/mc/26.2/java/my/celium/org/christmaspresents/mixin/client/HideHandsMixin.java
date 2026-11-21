package my.celium.org.christmaspresents.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While you open a present, the present renderer draws both of your arms doing the unwrapping,
 * so the normal first-person hand and held item step aside (26.2).
 */
@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
public abstract class HideHandsMixin {
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void christmaspresents$hideHands(CallbackInfo ci) {
        if (my.celium.org.christmaspresents.PresentGate.on()
                && my.celium.org.christmaspresents.client.PresentHands.hideVanillaHands()) {
            ci.cancel();
        }
    }
}

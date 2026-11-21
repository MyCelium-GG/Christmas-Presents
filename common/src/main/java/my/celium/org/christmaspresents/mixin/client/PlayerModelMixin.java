package my.celium.org.christmaspresents.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** A player opening a present holds both arms out to it. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void christmaspresents$openingArms(AvatarRenderState state, CallbackInfo ci) {
        if (!my.celium.org.christmaspresents.PresentGate.on()) {
            return;
        }
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        my.celium.org.christmaspresents.client.PresentHands.poseArms(state.id, model.rightArm, model.leftArm);
    }
}

package my.celium.org.christmaspresents.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;

/**
 * The wrapping menu is a vanilla hopper menu (five slots), so no new menu type has to be
 * registered on any loader. A hopper menu titled with the present key opens our screen instead.
 */
@Mixin(MenuScreens.class)
public abstract class MenuScreensMixin {
    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void christmaspresents$presentScreen(MenuType<?> type, Minecraft minecraft, int containerId,
            Component title, CallbackInfo ci) {
        if (type == MenuType.HOPPER && my.celium.org.christmaspresents.PresentGate.on()
                && my.celium.org.christmaspresents.client.ChristmasPresentsClient.openScreen(minecraft, containerId, title)) {
            ci.cancel();
        }
    }
}

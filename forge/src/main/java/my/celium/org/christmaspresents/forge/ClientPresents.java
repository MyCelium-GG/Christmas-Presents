package my.celium.org.christmaspresents.forge;

import my.celium.org.client.ClientHooks;
import my.celium.org.christmaspresents.ChristmasPresentsMod;
import my.celium.org.christmaspresents.client.PresentRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.ModLoadingContext;

/** Client-only: the settings screen and the present renderer. A dedicated server never loads this class. */
public final class ClientPresents {
    private ClientPresents() {
    }

    public static void register() {
        my.celium.org.christmaspresents.client.ChristmasPresentsClient.hooks();
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (Minecraft minecraft, Screen parent) -> ClientHooks.createConfigScreen(parent, ChristmasPresentsMod.ID)));
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event ->
                event.registerBlockEntityRenderer(ChristmasPresentsMod.presentEntity(), PresentRenderer::new));
    }
}

package my.celium.org.christmaspresents.neoforge;

import my.celium.org.client.ClientHooks;
import my.celium.org.christmaspresents.ChristmasPresentsMod;
import my.celium.org.christmaspresents.client.PresentRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only: the settings screen and the present renderer. A dedicated server never loads this class. */
public final class ClientPresents {
    private ClientPresents() {
    }

    public static void register(IEventBus modBus) {
        my.celium.org.christmaspresents.client.ChristmasPresentsClient.hooks();
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (ModContainer ignored, Screen parent) -> ClientHooks.createConfigScreen(parent, ChristmasPresentsMod.ID));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(ChristmasPresentsMod.presentEntity(), PresentRenderer::new));
    }
}

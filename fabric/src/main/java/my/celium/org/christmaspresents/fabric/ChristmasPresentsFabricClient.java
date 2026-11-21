package my.celium.org.christmaspresents.fabric;

import my.celium.org.fabric.FabricNetworking;
import my.celium.org.christmaspresents.client.ChristmasPresentsClient;
import net.fabricmc.api.ClientModInitializer;

/** Fabric client entrypoint: the present renderer and the client side of the packets. */
public final class ChristmasPresentsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("mycel")) {
            return;
        }
        ChristmasPresentsClient.init();
        FabricNetworking.flushClient();
    }
}

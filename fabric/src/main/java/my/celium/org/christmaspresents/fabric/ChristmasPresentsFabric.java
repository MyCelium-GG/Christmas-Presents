package my.celium.org.christmaspresents.fabric;

import my.celium.org.fabric.FabricNetworking;
import my.celium.org.christmaspresents.ChristmasPresentsMod;
import net.fabricmc.api.ModInitializer;

public final class ChristmasPresentsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("mycel")) {
            my.celium.org.christmaspresents.MissingMycel.logOnce();
            net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register(
                    (handler, sender, server) -> my.celium.org.christmaspresents.MissingMycel.tell(handler.getPlayer()));
            return;
        }
        my.celium.org.christmaspresents.PresentGate.open();
        ChristmasPresentsMod.init();
        FabricNetworking.flush();
    }
}

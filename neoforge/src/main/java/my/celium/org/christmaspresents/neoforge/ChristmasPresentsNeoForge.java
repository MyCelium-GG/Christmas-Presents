package my.celium.org.christmaspresents.neoforge;

import my.celium.org.christmaspresents.ChristmasPresentsMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * NeoForge entrypoint: server-safe bootstrap. Client classes (the settings screen) stay in {@link ClientPresents},
 * because a dedicated server cannot even verify a class that mentions them.
 */
@Mod(ChristmasPresentsMod.ID)
public final class ChristmasPresentsNeoForge {
    public ChristmasPresentsNeoForge(IEventBus modBus,
            @SuppressWarnings("unused") ModContainer container) {
        if (!net.neoforged.fml.ModList.get().isLoaded("mycel")) {
            my.celium.org.christmaspresents.MissingMycel.logOnce();
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                    (net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) -> {
                if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                    my.celium.org.christmaspresents.MissingMycel.tell(player);
                }
            });
            return;
        }
        my.celium.org.christmaspresents.PresentGate.open();
        ChristmasPresentsMod.init();
        if (FMLEnvironment.getDist().isClient()) {
            ClientPresents.register(modBus);
        }
    }
}

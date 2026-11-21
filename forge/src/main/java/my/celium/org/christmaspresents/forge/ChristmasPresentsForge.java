package my.celium.org.christmaspresents.forge;

import my.celium.org.christmaspresents.ChristmasPresentsMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Forge entrypoint: server-safe bootstrap. Client classes (the settings screen) stay in {@link ClientPresents},
 * because a dedicated server cannot even verify a class that mentions them.
 */
@Mod(ChristmasPresentsMod.ID)
public final class ChristmasPresentsForge {
    public ChristmasPresentsForge(@SuppressWarnings("unused") FMLJavaModLoadingContext context) {
        if (!net.minecraftforge.fml.ModList.isLoaded("mycel")) {
            my.celium.org.christmaspresents.MissingMycel.logOnce();
            net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
                if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                    my.celium.org.christmaspresents.MissingMycel.tell(player);
                }
            });
            return;
        }
        my.celium.org.christmaspresents.PresentGate.open();
        ChristmasPresentsMod.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientPresents.register();
        }
    }
}

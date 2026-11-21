package my.celium.org.christmaspresents.client;

import my.celium.org.christmaspresents.ChristmasPresentsMod;
import my.celium.org.christmaspresents.mixin.client.BlockEntityRenderersInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.MenuType;

/** Client setup, the same on every loader. */
public final class ChristmasPresentsClient {
    private ChristmasPresentsClient() {
    }

    /** Fabric's client entrypoint: registries are ready, so the renderer goes straight in. */
    public static void init() {
        hooks();
        BlockEntityRenderersInvoker.christmaspresents$register(ChristmasPresentsMod.presentEntity(), PresentRenderer::new);
    }

    /** Client hooks that need no registry, for every loader. */
    public static void hooks() {
        my.celium.org.christmaspresents.PresentBlockEntity.openingHook = PresentHands::tick;
    }

    /**
     * Opens the wrapping menu if {@code title} is the present's. The title's arguments are the
     * present's block position.
     *
     * @return whether it was a present menu
     */
    public static boolean openScreen(Minecraft minecraft, int containerId, Component title) {
        if (!(title.getContents() instanceof TranslatableContents contents)
                || !ChristmasPresentsMod.MENU_TITLE.equals(contents.getKey()) || minecraft.player == null) {
            return false;
        }
        BlockPos pos = null;
        Object[] args = contents.getArgs();
        if (args.length == 3 && args[0] instanceof Number x && args[1] instanceof Number y && args[2] instanceof Number z) {
            pos = new BlockPos(x.intValue(), y.intValue(), z.intValue());
        }
        HopperMenu menu = MenuType.HOPPER.create(containerId, minecraft.player.getInventory());
        PresentScreen screen = new PresentScreen(menu, minecraft.player.getInventory(),
                Component.translatable(ChristmasPresentsMod.MENU_TITLE), pos);
        minecraft.player.containerMenu = menu;
        ClientCompat.setScreen(minecraft, screen);
        return true;
    }
}

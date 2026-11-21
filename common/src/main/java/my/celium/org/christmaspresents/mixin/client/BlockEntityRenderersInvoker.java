package my.celium.org.christmaspresents.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Vanilla's own renderer list, the same on every loader. */
@Mixin(BlockEntityRenderers.class)
public interface BlockEntityRenderersInvoker {
    @Invoker("register")
    static <T extends BlockEntity, S extends BlockEntityRenderState> void christmaspresents$register(
            BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
        throw new AssertionError();
    }
}

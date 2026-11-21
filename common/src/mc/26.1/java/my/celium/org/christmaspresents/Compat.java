package my.celium.org.christmaspresents;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.function.BiFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The few things that differ between Minecraft 26.1, 26.2 and 26.3 (this copy: 26.1). */
final class Compat {
    private Compat() {
    }

    /** In 26.1 the factory interface is package-private, so it is implemented by a proxy. */
    @SuppressWarnings("unchecked")
    static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory,
            Block block) {
        try {
            Class<?> supplier = Class.forName("net.minecraft.world.level.block.entity.BlockEntityType$BlockEntitySupplier");
            Object proxy = Proxy.newProxyInstance(supplier.getClassLoader(), new Class<?>[] {supplier},
                    (self, method, args) -> switch (method.getName()) {
                        case "create" -> factory.apply((BlockPos) args[0], (BlockState) args[1]);
                        case "hashCode" -> System.identityHashCode(self);
                        case "equals" -> self == args[0];
                        default -> "PresentBlockEntitySupplier";
                    });
            Constructor<?> constructor = BlockEntityType.class.getDeclaredConstructor(supplier, Set.class);
            constructor.setAccessible(true);
            return (BlockEntityType<T>) constructor.newInstance(proxy, Set.of(block));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create the present's block entity type", e);
        }
    }
}

package my.celium.org.christmaspresents;

import java.util.Set;
import java.util.function.BiFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The few things that differ between Minecraft 26.1, 26.2 and 26.3 (this copy: 26.3). */
final class Compat {
    private Compat() {
    }

    static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory,
            Block block) {
        return new BlockEntityType<>(factory::apply, Set.of(block));
    }
}

package my.celium.org.christmaspresents;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The present block. Drawn entirely by its block entity renderer (so it can animate), with a
 * hitbox that matches the chosen box shape.
 *
 * <ul>
 *   <li>Not wrapped yet: right-click opens the wrapping menu.</li>
 *   <li>Wrapped: the person it is for right-clicks to open it; everyone else is told who it is for.
 *       Whoever wrapped it can sneak + right-click to unwrap it again and change something.</li>
 * </ul>
 */
public final class PresentBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<PresentStyle.Shape> SHAPE = EnumProperty.create("shape", PresentStyle.Shape.class);

    private static final Map<PresentStyle.Shape, VoxelShape> NORTH_SOUTH = new EnumMap<>(PresentStyle.Shape.class);
    private static final Map<PresentStyle.Shape, VoxelShape> EAST_WEST = new EnumMap<>(PresentStyle.Shape.class);

    static {
        for (PresentStyle.Shape shape : PresentStyle.Shape.values()) {
            double top = shape.height + 0.5 + (shape == PresentStyle.Shape.SMALL ? 2 : 3);
            NORTH_SOUTH.put(shape, box(shape.width, shape.depth, top));
            EAST_WEST.put(shape, box(shape.depth, shape.width, top));
        }
    }

    public PresentBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, PresentStyle.Shape.CUBE));
    }

    private static VoxelShape box(double width, double depth, double top) {
        double x = (16 - width - 1) / 2;
        double z = (16 - depth - 1) / 2;
        return Block.box(x, 0, z, 16 - x, Math.min(16, top), 16 - z);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        PresentStyle style = PresentItems.styleOf(context.getItemInHand());
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(SHAPE, style.shape());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return (facing.getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST).get(state.getValue(SHAPE));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PresentBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ChristmasPresentsMod.presentEntity()) {
            return null;
        }
        return level.isClientSide()
                ? (lvl, pos, st, be) -> PresentBlockEntity.clientTick(lvl, pos, st, (PresentBlockEntity) be)
                : (lvl, pos, st, be) -> PresentBlockEntity.serverTick(lvl, pos, st, (PresentBlockEntity) be);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PresentBlockEntity present)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !ChristmasPresentsMod.isModEnabled()) {
            return InteractionResult.PASS;
        }
        if (present.opening()) {
            return InteractionResult.CONSUME;
        }
        if (!present.wrapped()) {
            openMenu(serverPlayer, present, pos);
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && present.isFrom(player)) {
            present.unwrapForEditing();
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0f, 1.2f);
            openMenu(serverPlayer, present, pos);
            return InteractionResult.CONSUME;
        }
        if (!present.isFor(player) && !ChristmasPresentsMod.anyoneCanOpen()) {
            turnAway(serverPlayer, present, level, pos, state);
            return InteractionResult.CONSUME;
        }
        String wait = Calendar.waitMessage();
        if (wait != null) {
            serverPlayer.sendOverlayMessage(Component.translatable(wait, Calendar.daysUntilChristmas())
                    .withStyle(ChatFormatting.GOLD));
            return InteractionResult.CONSUME;
        }
        present.startOpening((ServerLevel) level, serverPlayer);
        return InteractionResult.CONSUME;
    }

    /** Not your present: a bump, a locked sound, the present wobbles and says who it is for. */
    private static void turnAway(ServerPlayer player, PresentBlockEntity present, Level level, BlockPos pos, BlockState state) {
        double dx = pos.getX() + 0.5 - player.getX();
        double dz = pos.getZ() + 0.5 - player.getZ();
        if (dx * dx + dz * dz < 1.0E-4) {
            dx = -player.getLookAngle().x;
            dz = -player.getLookAngle().z;
        }
        double length = Math.sqrt(dx * dx + dz * dz);
        player.setDeltaMovement(-dx / length * 0.45, 0.28, -dz / length * 0.45);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.blockEvent(pos, state.getBlock(), 1, 0);
        player.sendOverlayMessage(Component.translatable("christmaspresents.not_for_you", present.to())
                .withStyle(ChatFormatting.RED));
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }

    private static void openMenu(ServerPlayer player, PresentBlockEntity present, BlockPos pos) {
        // The title carries the block position, so the client's menu knows which present it edits.
        Component title = Component.translatable(ChristmasPresentsMod.MENU_TITLE, pos.getX(), pos.getY(), pos.getZ());
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new HopperMenu(id, inventory, present), title));
    }
}

package my.celium.org.christmaspresents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.HopperMenu;

/** What the wrapping menu sends to the server. */
public final class PresentPackets {
    private PresentPackets() {
    }

    /**
     * Client to server: the present's new look and who it is for. Sent on every change in the
     * menu, so everyone nearby sees the present change while it is being wrapped.
     */
    public record Edit(BlockPos pos, int paper, int ribbon, String pattern, String shape, String bow, String to, String lock)
            implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ChristmasPresentsMod.ID, "edit"));
        public static final StreamCodec<FriendlyByteBuf, Edit> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBlockPos(p.pos);
                    buf.writeInt(p.paper);
                    buf.writeInt(p.ribbon);
                    buf.writeUtf(p.pattern, 16);
                    buf.writeUtf(p.shape, 16);
                    buf.writeUtf(p.bow, 16);
                    buf.writeUtf(p.to, 32);
                    buf.writeUtf(p.lock, 16);
                },
                buf -> new Edit(buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readUtf(16), buf.readUtf(16),
                        buf.readUtf(16), buf.readUtf(32), buf.readUtf(16)));

        public static Edit of(BlockPos pos, PresentStyle style, String to, Calendar.Lock lock) {
            return new Edit(pos, style.paper(), style.ribbon(), style.pattern().id(), style.shape().id(),
                    style.bow().id(), to, lock.id());
        }

        PresentStyle style() {
            return new PresentStyle(paper, ribbon, PresentStyle.Pattern.of(pattern), PresentStyle.Shape.of(shape),
                    PresentStyle.Bow.of(bow));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: the Wrap button. */
    public record Wrap(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Wrap> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ChristmasPresentsMod.ID, "wrap"));
        public static final StreamCodec<FriendlyByteBuf, Wrap> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBlockPos(p.pos), buf -> new Wrap(buf.readBlockPos()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void handleEdit(Edit packet, ServerPlayer player) {
        PresentBlockEntity present = editable(player, packet.pos());
        if (present != null) {
            present.setStyle(packet.style(), packet.to(), Calendar.Lock.of(packet.lock()));
        }
    }

    static void handleWrap(Wrap packet, ServerPlayer player) {
        PresentBlockEntity present = editable(player, packet.pos());
        if (present == null) {
            return;
        }
        present.wrap(player);
        player.closeContainer();
        player.level().playSound(null, packet.pos(), SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.level().playSound(null, packet.pos(), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, 1.3f);
        ChristmasPresentsStats.wrapped();
    }

    /** The present at {@code pos}, if this player has its wrapping menu open right next to it. */
    private static PresentBlockEntity editable(ServerPlayer player, BlockPos pos) {
        if (!(player.containerMenu instanceof HopperMenu) || !ChristmasPresentsMod.isModEnabled()) {
            return null;
        }
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64) {
            return null;
        }
        if (player.level().getBlockEntity(pos) instanceof PresentBlockEntity present && !present.wrapped()
                && !present.opening()) {
            return present;
        }
        return null;
    }
}

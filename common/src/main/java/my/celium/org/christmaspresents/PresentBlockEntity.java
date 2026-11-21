package my.celium.org.christmaspresents;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A present: up to five gifts, its look, who it is for and from, and whether it is wrapped. Once
 * opening starts, {@link #openStart} holds the game time it began; clients play {@link Unwrap}'s
 * animation from it and the server hands out the gifts at the end.
 */
public final class PresentBlockEntity extends BlockEntity implements Container {
    public static final int SIZE = 5;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private PresentStyle style = PresentStyle.DEFAULT;
    private String to = "";
    private String from = "";
    private boolean wrapped;
    private long openStart = -1;
    private UUID opener;
    private long wobbleStart = -100;

    /**
     * Set by the client side: called every client tick while a present is opening, to walk the
     * opener up to it. Null on a dedicated server.
     */
    public static volatile BiConsumer<PresentBlockEntity, Float> openingHook;

    public PresentBlockEntity(BlockPos pos, BlockState state) {
        super(ChristmasPresentsMod.presentEntity(), pos, state);
    }

    public PresentStyle style() {
        // The shape lives in the block state, so the hitbox always matches.
        BlockState state = getBlockState();
        if (state.hasProperty(PresentBlock.SHAPE)) {
            return style.withShape(state.getValue(PresentBlock.SHAPE));
        }
        return style;
    }

    public String to() {
        return to;
    }

    public String from() {
        return from;
    }

    public boolean wrapped() {
        return wrapped;
    }

    public boolean opening() {
        return openStart >= 0;
    }

    /** Ticks since opening started, with the partial tick; negative while closed. */
    public float openTime(float partialTick) {
        if (openStart < 0 || level == null) {
            return -1;
        }
        return Math.max(0, level.getGameTime() - openStart + partialTick);
    }

    /** Who is opening this present, or null. */
    public UUID opener() {
        return opener;
    }

    /** Ticks since someone was turned away (the present wobbles), or a large number. */
    public float wobbleTime(float partialTick) {
        return level == null ? 100 : level.getGameTime() - wobbleStart + partialTick;
    }

    public List<ItemStack> gifts() {
        return items;
    }

    // ---- changes made from the wrapping menu (server)

    void setStyle(PresentStyle value, String recipient) {
        style = value;
        to = Names.clean(recipient);
        if (level != null && getBlockState().getValue(PresentBlock.SHAPE) != value.shape()) {
            level.setBlock(worldPosition, getBlockState().setValue(PresentBlock.SHAPE, value.shape()), 3);
        }
        sync();
    }

    void wrap(ServerPlayer player) {
        wrapped = true;
        from = player.getGameProfile().name();
        sync();
    }

    void unwrapForEditing() {
        wrapped = false;
        sync();
    }

    boolean isFor(Player player) {
        return to.isEmpty() || to.equalsIgnoreCase(player.getGameProfile().name());
    }

    boolean isFrom(Player player) {
        return from.equalsIgnoreCase(player.getGameProfile().name());
    }

    void startOpening(ServerLevel serverLevel, ServerPlayer player) {
        openStart = serverLevel.getGameTime();
        opener = player.getUUID();
        sync();
        PresentSounds.play(serverLevel, worldPosition, "present.unwrap", 1.0f);
        if (!from.isEmpty() && !from.equalsIgnoreCase(player.getGameProfile().name())) {
            player.sendOverlayMessage(Component.translatable("christmaspresents.from_message", from)
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    // ---- ticking

    static void serverTick(Level level, BlockPos pos, BlockState state, PresentBlockEntity present) {
        if (present.openStart < 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long t = level.getGameTime() - present.openStart;
        if (t == Unwrap.LID_POP) {
            PresentSounds.play(serverLevel, pos, "present.pop", 1.0f);
        } else if (t == Unwrap.GIFTS) {
            PresentSounds.play(serverLevel, pos, "present.reveal", 1.0f);
        } else if (t >= Unwrap.TICKS) {
            present.finishOpening(serverLevel);
        }
    }

    static void clientTick(Level level, BlockPos pos, BlockState state, PresentBlockEntity present) {
        if (present.openStart < 0) {
            return;
        }
        BiConsumer<PresentBlockEntity, Float> hook = openingHook;
        if (hook != null) {
            hook.accept(present, (float) (level.getGameTime() - present.openStart));
        }
        if (level.getGameTime() - present.openStart != Unwrap.LID_POP) {
            return;
        }
        PresentStyle look = present.style();
        double top = look.shape().height / 16.0 + 0.2;
        for (int i = 0; i < 24; i++) {
            int color = i % 3 == 0 ? look.ribbon() : i % 3 == 1 ? look.paper() : 0xFFFFFF;
            double vx = (level.getRandom().nextDouble() - 0.5) * 0.25;
            double vy = 0.1 + level.getRandom().nextDouble() * 0.25;
            double vz = (level.getRandom().nextDouble() - 0.5) * 0.25;
            level.addParticle(new DustParticleOptions(color, 1.0f), pos.getX() + 0.5, pos.getY() + top,
                    pos.getZ() + 0.5, vx, vy, vz);
        }
    }

    private void finishOpening(ServerLevel serverLevel) {
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.85;
        double z = worldPosition.getZ() + 0.5;
        List<ItemStack> gifts = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            if (!items.get(i).isEmpty()) {
                gifts.add(items.get(i));
                items.set(i, ItemStack.EMPTY);
            }
        }
        for (int i = 0; i < gifts.size(); i++) {
            double angle = gifts.size() == 1 ? 0 : i * Math.PI * 2 / gifts.size();
            double spread = gifts.size() == 1 ? 0 : 0.18;
            ItemEntity entity = new ItemEntity(serverLevel, x + Math.cos(angle) * spread, y,
                    z + Math.sin(angle) * spread, gifts.get(i), Math.cos(angle) * 0.04, 0.22, Math.sin(angle) * 0.04);
            entity.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(entity);
        }
        serverLevel.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.2f);
        openStart = -1;
        opener = null;
        serverLevel.removeBlock(worldPosition, false);
        ChristmasPresentsStats.opened();
    }

    private void sync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    // ---- saving

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        style = new PresentStyle(input.getIntOr("Paper", PresentStyle.DEFAULT.paper()),
                input.getIntOr("Ribbon", PresentStyle.DEFAULT.ribbon()),
                PresentStyle.Pattern.of(input.getStringOr("Pattern", "plain")),
                PresentStyle.Shape.CUBE,
                PresentStyle.Bow.of(input.getStringOr("Bow", "bow")));
        to = input.getStringOr("To", "");
        from = input.getStringOr("From", "");
        wrapped = input.getBooleanOr("Wrapped", false);
        openStart = input.getLongOr("OpenStart", -1L);
        opener = input.read("Opener", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items, false);
        saveLook(output);
    }

    private void saveLook(ValueOutput output) {
        output.putInt("Paper", style.paper());
        output.putInt("Ribbon", style.ribbon());
        output.putString("Pattern", style.pattern().id());
        output.putString("Bow", style.bow().id());
        output.putString("To", to);
        output.putString("From", from);
        output.putBoolean("Wrapped", wrapped);
        output.putLong("OpenStart", openStart);
        if (opener != null) {
            output.store("Opener", UUIDUtil.CODEC, opener);
        }
    }

    /** Block event 1: someone who is not the recipient tried to open it; the present wobbles. */
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) {
            wobbleStart = level == null ? 0 : level.getGameTime();
            return true;
        }
        return super.triggerEvent(id, param);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Clients get the look always, but the gifts only once opening starts, so nobody can peek. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        saveLook(output);
        if (openStart >= 0) {
            ContainerHelper.saveAllItems(output, items, true);
        }
        return output.buildResult();
    }

    // ---- item components: a picked-up present keeps everything

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        ItemContainerContents contents = components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        items.clear();
        contents.copyInto(items);
        CustomModelData model = components.get(DataComponents.CUSTOM_MODEL_DATA);
        if (model != null) {
            style = PresentItems.styleOf(model);
        }
        CustomData data = components.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag tag = data.copyTag();
            to = tag.getStringOr("To", "");
            from = tag.getStringOr("From", "");
            wrapped = tag.getBooleanOr("Wrapped", false);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        components.set(DataComponents.CUSTOM_MODEL_DATA, PresentItems.modelData(style()));
        CompoundTag tag = new CompoundTag();
        tag.putString("To", to);
        tag.putString("From", from);
        tag.putBoolean("Wrapped", wrapped);
        components.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        ItemLore lore = PresentItems.lore(to, from, wrapped);
        if (!lore.lines().isEmpty()) {
            components.set(DataComponents.LORE, lore);
        }
        if (!to.isEmpty()) {
            components.set(DataComponents.ITEM_NAME, Component.translatable("item.christmaspresents.present.for", to));
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        for (String key : new String[] {"Items", "Paper", "Ribbon", "Pattern", "Bow", "To", "From", "Wrapped", "OpenStart", "Opener"}) {
            output.discard(key);
        }
    }

    // ---- Container (the wrapping menu's five slots)

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, slot, count);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return !wrapped && Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }
}

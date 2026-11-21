package my.celium.org.christmaspresents;

import java.lang.reflect.Constructor;
import java.util.List;

import my.celium.org.Mycel;
import my.celium.org.config.ConfigBuilder;
import my.celium.org.config.ConfigValue;
import my.celium.org.config.MycelConfig;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.network.MycelNetwork;
import my.celium.org.network.PacketDefinition;
import my.celium.org.registry.DeferredEntry;
import my.celium.org.registry.MycelRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Christmas Presents: wrap gifts in a present you design yourself (paper, pattern, ribbon, box
 * shape, bow), write who it is for, and watch it unwrap itself when they open it.
 */
public final class ChristmasPresentsMod {
    public static final String ID = "christmaspresents";
    public static final String MENU_TITLE = "container.christmaspresents.present";

    public static final ModMetadata META = ModMetadata.builder(ID, "Christmas Presents", "1.0.0")
            .author("MyCelium")
            .website("https://www.curseforge.com/minecraft/mc-mods/christmas-presents")
            .updateUrl("https://wiki.mycelium-gg.org/updates/christmaspresents.json")
            .build();

    public static MycelConfig config;
    public static ConfigValue<Boolean> enabled;
    public static ConfigValue<Boolean> onlyOnChristmas;
    public static ConfigValue<Boolean> anyoneCanOpen;

    public static PacketDefinition<PresentPackets.Edit> edit;
    public static PacketDefinition<PresentPackets.Wrap> wrap;

    private static DeferredEntry<Block> presentBlock;
    private static DeferredEntry<Item> presentItem;
    private static DeferredEntry<BlockEntityType<?>> presentEntity;

    private ChristmasPresentsMod() {
    }

    /** Called once from each loader's entrypoint. */
    public static void init() {
        Mycel.initialize();
        Mycel.registerMod(META);

        presentBlock = MycelRegistry.blocks(META).registerWithKey("present", key -> new PresentBlock(
                BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.COLOR_RED).strength(0.5f)
                        .sound(SoundType.WOOL).noOcclusion()));
        presentItem = MycelRegistry.items(META).registerWithKey("present", key -> new BlockItem(presentBlock.get(),
                new Item.Properties().setId(key).useBlockDescriptionPrefix().stacksTo(1)
                        .component(DataComponents.CUSTOM_MODEL_DATA, PresentItems.modelData(PresentStyle.DEFAULT))
                        .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                        .component(DataComponents.TOOLTIP_DISPLAY, PresentItems.HIDE_CONTENTS)));
        presentEntity = registry(BuiltInRegistries.BLOCK_ENTITY_TYPE, Registries.BLOCK_ENTITY_TYPE)
                .register("present", () -> Compat.blockEntityType(PresentBlockEntity::new, presentBlock.get()));

        edit = PacketDefinition.toServer(PresentPackets.Edit.TYPE, PresentPackets.Edit.CODEC,
                (packet, context) -> PresentPackets.handleEdit(packet, context.sender()));
        wrap = PacketDefinition.toServer(PresentPackets.Wrap.TYPE, PresentPackets.Wrap.CODEC,
                (packet, context) -> PresentPackets.handleWrap(packet, context.sender()));
        MycelNetwork.register(META, edit);
        MycelNetwork.register(META, wrap);

        ConfigBuilder builder = Mycel.config(META);
        enabled = builder.booleanValue("enabled", true,
                "Turns Christmas Presents on or off. Presents stay where they are either way.");
        onlyOnChristmas = builder.booleanValue("onlyOnChristmas", false,
                "Presents can only be opened from December 24 to 26 (the server's date). Until then, opening one says how many days are left.");
        anyoneCanOpen = builder.booleanValue("anyoneCanOpen", false,
                "Anyone can open any present, not only the player whose name is on it.");
        config = builder.build();
        ChristmasPresentsStats.start();
    }

    /**
     * Mycel's registry helper only offers items, blocks and entity types, but the loader bridge
     * underneath works for any registry; this reaches it for block entity types.
     */
    @SuppressWarnings("unchecked")
    private static <T> MycelRegistry<T> registry(Registry<T> registry, ResourceKey<Registry<T>> key) {
        try {
            Constructor<?> constructor = MycelRegistry.class.getDeclaredConstructor(ModMetadata.class, Registry.class,
                    ResourceKey.class);
            constructor.setAccessible(true);
            return (MycelRegistry<T>) constructor.newInstance(META, registry, key);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Mycel's registry helper changed", e);
        }
    }

    public static Block presentBlock() {
        return presentBlock.get();
    }

    public static Item presentItem() {
        return presentItem.get();
    }

    @SuppressWarnings("unchecked")
    public static BlockEntityType<PresentBlockEntity> presentEntity() {
        return (BlockEntityType<PresentBlockEntity>) presentEntity.get();
    }

    /** A few ready-made looks for the creative menu. */
    public static List<ItemStack> creativeItems() {
        return List.of(
                PresentItems.stack(PresentStyle.DEFAULT),
                PresentItems.stack(new PresentStyle(0x2E7D32, 0xC62828, PresentStyle.Pattern.STRIPES,
                        PresentStyle.Shape.TALL, PresentStyle.Bow.BIG_BOW)),
                PresentItems.stack(new PresentStyle(0x0D2B6B, 0xC0C7CF, PresentStyle.Pattern.STARS,
                        PresentStyle.Shape.WIDE, PresentStyle.Bow.ROSETTE)),
                PresentItems.stack(new PresentStyle(0xFAFAFA, 0xC62828, PresentStyle.Pattern.DOTS,
                        PresentStyle.Shape.SMALL, PresentStyle.Bow.KNOT)));
    }

    public static boolean isModEnabled() {
        return Mycel.isEnabled(ID) && (enabled == null || enabled.get());
    }

    static boolean onlyOnChristmas() {
        return onlyOnChristmas != null && onlyOnChristmas.get();
    }

    static boolean anyoneCanOpen() {
        return anyoneCanOpen != null && anyoneCanOpen.get();
    }
}

package my.celium.org.christmaspresents;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;

/** The present as an item: its look in custom model data, and its tooltip. */
public final class PresentItems {
    /** Hides the list of gifts inside from the tooltip, so a present stays a surprise. */
    public static final TooltipDisplay HIDE_CONTENTS = new TooltipDisplay(false,
            new java.util.LinkedHashSet<>(Set.of(DataComponents.CONTAINER)));

    private PresentItems() {
    }

    /** Strings: shape, pattern, bow. Colours: paper, ribbon. The item model reads the same. */
    public static CustomModelData modelData(PresentStyle style) {
        return new CustomModelData(List.of(), List.of(),
                List.of(style.shape().id(), style.pattern().id(), style.bow().id()),
                List.of(style.paper(), style.ribbon()));
    }

    public static PresentStyle styleOf(CustomModelData data) {
        Integer paper = data.getColor(0);
        Integer ribbon = data.getColor(1);
        return new PresentStyle(
                paper == null ? PresentStyle.DEFAULT.paper() : paper,
                ribbon == null ? PresentStyle.DEFAULT.ribbon() : ribbon,
                PresentStyle.Pattern.of(data.getString(1)),
                PresentStyle.Shape.of(data.getString(0)),
                PresentStyle.Bow.of(data.getString(2)));
    }

    public static PresentStyle styleOf(ItemStack stack) {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return data == null ? PresentStyle.DEFAULT : styleOf(data);
    }

    public static ItemLore lore(String to, String from, boolean wrapped) {
        List<Component> lines = new ArrayList<>();
        if (!to.isEmpty()) {
            lines.add(line("christmaspresents.tooltip.to", to));
        }
        if (wrapped && !from.isEmpty()) {
            lines.add(line("christmaspresents.tooltip.from", from));
        }
        if (!wrapped) {
            lines.add(Component.translatable("christmaspresents.tooltip.unwrapped")
                    .withStyle(style -> style.withItalic(false)).withStyle(ChatFormatting.GRAY));
        }
        return new ItemLore(lines);
    }

    private static Component line(String key, String name) {
        return Component.translatable(key, Component.literal(name).withStyle(ChatFormatting.WHITE))
                .withStyle(style -> style.withItalic(false)).withStyle(ChatFormatting.GOLD);
    }

    /** A present item with the given look, for the creative menu. */
    public static ItemStack stack(PresentStyle style) {
        ItemStack stack = new ItemStack(ChristmasPresentsMod.presentItem());
        stack.set(DataComponents.CUSTOM_MODEL_DATA, modelData(style));
        return stack;
    }
}

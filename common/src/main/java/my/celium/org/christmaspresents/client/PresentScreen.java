package my.celium.org.christmaspresents.client;

import com.mojang.blaze3d.platform.InputConstants;

import my.celium.org.christmaspresents.Calendar;
import my.celium.org.christmaspresents.ChristmasPresentsMod;
import my.celium.org.christmaspresents.Names;
import my.celium.org.christmaspresents.PresentBlockEntity;
import my.celium.org.christmaspresents.PresentItems;
import my.celium.org.christmaspresents.PresentPackets;
import my.celium.org.christmaspresents.PresentStyle;
import my.celium.org.network.MycelNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HopperMenu;

/**
 * The wrapping menu: vanilla's hopper layout for the five gift slots on the left, and on the
 * right everything about the look, a live preview and the name of whoever the present is for.
 * Every change is sent to the server straight away, so the real present changes too.
 */
public final class PresentScreen extends AbstractContainerScreen<HopperMenu> {
    private static final Identifier HOPPER = Identifier.withDefaultNamespace("textures/gui/container/hopper.png");
    private static final int PANEL_X = 180;
    private static final int PANEL_W = 140;
    private static final int SWATCH = 9;

    private final BlockPos pos;
    private PresentStyle style = PresentStyle.DEFAULT;
    private String to = "";
    private Calendar.Lock lock = Calendar.Lock.ANY_TIME;
    private Button patternButton;
    private Button lockButton;
    private Button shapeButton;
    private Button bowButton;
    private EditBox nameBox;

    public PresentScreen(HopperMenu menu, Inventory inventory, Component title, BlockPos pos) {
        super(menu, inventory, title, 176 + 4 + PANEL_W, 158);
        this.pos = pos;
        this.inventoryLabelY = 133 - 94;
        if (pos != null && inventory.player.level().getBlockEntity(pos) instanceof PresentBlockEntity present) {
            style = present.style();
            to = present.to();
            lock = present.lock();
        }
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + PANEL_X;
        int y = topPos;
        for (int i = 0; i < PresentStyle.COLORS.size(); i++) {
            int color = PresentStyle.COLORS.get(i);
            int sx = x + 6 + (i % 8) * SWATCH;
            addRenderableWidget(new Swatch(sx, y + 17 + (i / 8) * SWATCH, color, false));
            addRenderableWidget(new Swatch(sx, y + 49 + (i / 8) * SWATCH, color, true));
        }
        patternButton = addRenderableWidget(Button.builder(Component.empty(), b -> change(style.withPattern(
                PresentStyle.cycle(style.pattern(), 1)))).bounds(x + 5, y + 70, 64, 18).build());
        shapeButton = addRenderableWidget(Button.builder(Component.empty(), b -> change(style.withShape(
                PresentStyle.cycle(style.shape(), 1)))).bounds(x + 71, y + 70, 64, 18).build());
        bowButton = addRenderableWidget(Button.builder(Component.empty(), b -> change(style.withBow(
                PresentStyle.cycle(style.bow(), 1)))).bounds(x + 5, y + 90, 64, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("christmaspresents.menu.surprise"), b -> surprise())
                .bounds(x + 71, y + 90, 64, 18)
                .tooltip(Tooltip.create(Component.translatable("christmaspresents.menu.surprise.tip")))
                .build());
        lockButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            lock = PresentStyle.cycle(lock, 1);
            refreshLabels();
            send();
        }).bounds(x + 5, y + 110, 130, 18)
                .tooltip(Tooltip.create(Component.translatable("christmaspresents.menu.lock.tip")))
                .build());
        nameBox = new EditBox(font, x + 24, y + 134, 110, 14, Component.translatable("christmaspresents.menu.to"));
        nameBox.setMaxLength(Names.MAX);
        nameBox.setValue(to);
        nameBox.setHint(Component.translatable("christmaspresents.menu.to.hint"));
        nameBox.setResponder(value -> {
            String clean = Names.clean(value);
            if (!clean.equals(to)) {
                to = clean;
                send();
            }
        });
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("christmaspresents.menu.wrap"), b -> wrap())
                .bounds(leftPos + 8, topPos + 137, 160, 20)
                .tooltip(Tooltip.create(Component.translatable("christmaspresents.menu.wrap.tip")))
                .build());
        refreshLabels();
    }

    private void refreshLabels() {
        patternButton.setMessage(Component.translatable("christmaspresents.pattern." + style.pattern().id()));
        shapeButton.setMessage(Component.translatable("christmaspresents.shape." + style.shape().id()));
        bowButton.setMessage(Component.translatable("christmaspresents.bow." + style.bow().id()));
        lockButton.setMessage(Component.translatable("christmaspresents.menu.lock",
                Component.translatable("christmaspresents.lock." + lock.id())));
    }

    private void change(PresentStyle next) {
        style = next;
        refreshLabels();
        send();
    }

    /** A random look, for when you can't decide. */
    private void surprise() {
        RandomSource random = RandomSource.create();
        int paper = PresentStyle.COLORS.get(random.nextInt(PresentStyle.COLORS.size()));
        int ribbon;
        do {
            ribbon = PresentStyle.COLORS.get(random.nextInt(PresentStyle.COLORS.size()));
        } while (ribbon == paper);
        PresentStyle.Pattern[] patterns = PresentStyle.Pattern.values();
        PresentStyle.Shape[] shapes = PresentStyle.Shape.values();
        PresentStyle.Bow[] bows = PresentStyle.Bow.values();
        change(new PresentStyle(paper, ribbon, patterns[random.nextInt(patterns.length)],
                shapes[random.nextInt(shapes.length)], bows[random.nextInt(bows.length)]));
    }

    private void send() {
        if (pos != null) {
            MycelNetwork.sendToServer(ChristmasPresentsMod.edit, PresentPackets.Edit.of(pos, style, to, lock));
        }
    }

    private void wrap() {
        if (pos != null) {
            send();
            MycelNetwork.sendToServer(ChristmasPresentsMod.wrap, new PresentPackets.Wrap(pos));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // While typing a name, letters (like E) must not close the menu.
        if (nameBox != null && nameBox.isFocused() && event.key() != InputConstants.KEY_ESCAPE) {
            nameBox.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, HOPPER, leftPos, topPos, 0, 0, 176, 133, 256, 256);
        int x = leftPos + PANEL_X;
        int y = topPos;
        panel(graphics, x, y, PANEL_W, 155);
        graphics.text(font, Component.translatable("christmaspresents.menu.paper"), x + 6, y + 6, 0xFF404040, false);
        graphics.text(font, Component.translatable("christmaspresents.menu.ribbon"), x + 6, y + 38, 0xFF404040, false);
        graphics.text(font, Component.translatable("christmaspresents.menu.to"), x + 6, y + 137, 0xFF404040, false);

        // The preview: the present item with the chosen look, big.
        int px = x + 84;
        int py = y + 6;
        graphics.fill(px, py, px + 50, py + 58, 0xFF8B8B8B);
        graphics.fill(px + 1, py + 1, px + 50, py + 58, 0xFF373737);
        graphics.fill(px + 1, py + 1, px + 49, py + 57, 0xFF5E6A78);
        graphics.pose().pushMatrix();
        graphics.pose().translate(px + 25, py + 30);
        graphics.pose().scale(2.9f, 2.9f);
        graphics.item(PresentItems.stack(style), -8, -8);
        graphics.pose().popMatrix();
    }

    /** A plain vanilla-looking panel: light grey with a white and dark grey bevel. */
    private static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        graphics.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        graphics.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
        graphics.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        graphics.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
    }

    /** One colour square: paper colours on top, ribbon colours below. */
    private final class Swatch extends AbstractWidget {
        private final int color;
        private final boolean ribbon;

        Swatch(int x, int y, int color, boolean ribbon) {
            super(x, y, SWATCH, SWATCH, Component.empty());
            this.color = color;
            this.ribbon = ribbon;
        }

        private boolean selected() {
            return (ribbon ? style.ribbon() : style.paper()) == color;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int border = selected() ? 0xFFFFFFFF : isHovered() ? 0xFFE0E0E0 : 0xFF555555;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF000000 | color);
            if (selected()) {
                graphics.fill(getX() + 3, getY() + 3, getX() + width - 3, getY() + height - 3,
                        brightness(color) > 140 ? 0xFF202020 : 0xFFFFFFFF);
            }
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            change(ribbon ? style.withRibbon(color) : style.withPaper(color));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.literal(String.format("#%06X", color)));
        }
    }

    private static int brightness(int rgb) {
        return (((rgb >> 16) & 0xFF) * 299 + ((rgb >> 8) & 0xFF) * 587 + (rgb & 0xFF) * 114) / 1000;
    }
}

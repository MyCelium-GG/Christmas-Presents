package my.celium.org.christmaspresents.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

import my.celium.org.christmaspresents.ChristmasPresentsMod;
import my.celium.org.christmaspresents.PresentBlock;
import my.celium.org.christmaspresents.PresentBlockEntity;
import my.celium.org.christmaspresents.PresentStyle;
import my.celium.org.christmaspresents.Unwrap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Draws presents and plays their opening animation. */
public final class PresentRenderer implements BlockEntityRenderer<PresentBlockEntity, PresentRenderer.State> {
    private static final Identifier RIBBON = texture("ribbon");
    private static final Identifier TAG = texture("tag");

    private final ItemModelResolver items;
    private final Font font;
    private final PresentHands.Arms wideArms;
    private final PresentHands.Arms slimArms;

    public PresentRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.itemModelResolver();
        this.font = context.font();
        this.wideArms = new PresentHands.Arms(context.entityModelSet().bakeLayer(ModelLayers.PLAYER));
        this.slimArms = new PresentHands.Arms(context.entityModelSet().bakeLayer(ModelLayers.PLAYER_SLIM));
    }

    static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(ChristmasPresentsMod.ID, "textures/block/present_" + name + ".png");
    }

    /** What one frame needs, copied from the block entity. */
    public static final class State extends BlockEntityRenderState {
        PresentStyle style = PresentStyle.DEFAULT;
        Direction facing = Direction.NORTH;
        float time = -1;
        float wobble = 100;
        boolean mine;
        String to = "";
        final List<ItemStackRenderState> gifts = new ArrayList<>();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PresentBlockEntity present, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(present, state, partialTick, camera, breakProgress);
        state.style = present.style();
        state.facing = present.getBlockState().getValue(PresentBlock.FACING);
        state.time = present.openTime(partialTick);
        state.wobble = present.wobbleTime(partialTick);
        var me = net.minecraft.client.Minecraft.getInstance().player;
        state.mine = me != null && me.getUUID().equals(present.opener());
        state.to = present.to();
        state.gifts.clear();
        if (state.time >= Unwrap.GIFTS) {
            int seed = (int) present.getBlockPos().asLong();
            for (ItemStack stack : present.gifts()) {
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStackRenderState gift = new ItemStackRenderState();
                items.updateForTopItem(gift, stack, ItemDisplayContext.GROUND, present.getLevel(), null, seed++);
                state.gifts.add(gift);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        PresentModel model = PresentModel.of(state.style.shape());
        int light = state.lightCoords;

        float unwrap = state.time < 0 ? -1 : state.time - Unwrap.APPROACH;
        if (state.mine && state.time >= 0) {
            PresentHands.submitArms(pose, collector, state.blockPos, state.style.shape(), state.time, light, wideArms, slimArms);
        }

        pose.pushPose();
        pose.translate(0.5f, 0, 0.5f);
        ClientCompat.rotate(pose, new Quaternionf().rotationY((float) Math.toRadians(-state.facing.toYRot())));
        if (state.wobble < 12) {
            // Turned away: the present shakes, like a decorated pot that was bumped.
            float p = state.wobble / 12f;
            float tilt = (float) Math.sin(p * Math.PI * 4) * 0.12f * (1 - p);
            ClientCompat.rotate(pose, new Quaternionf().rotationZ(tilt));
        }
        pose.pushPose();
        pose.scale(-1, -1, 1);
        model.submit(collector, pose, RenderTypes.entityCutout(texture(state.style.pattern().id())),
                RenderTypes.entityCutout(RIBBON), 0xFF000000 | state.style.paper(), 0xFF000000 | state.style.ribbon(),
                state.style.bow(), unwrap, light);
        boolean showTag = !state.to.isEmpty() && unwrap < 0;
        if (showTag) {
            ClientCompat.part(collector, model.tag, pose, RenderTypes.entityCutout(TAG), light,
                    OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
        pose.popPose();
        if (showTag) {
            submitName(state, pose, collector, light);
        }
        submitGifts(state, unwrap, pose, collector, light);
        pose.popPose();
    }

    /** The recipient's name on the tag, as big as fits. */
    private void submitName(State state, PoseStack pose, SubmitNodeCollector collector, int light) {
        PresentStyle.Shape shape = state.style.shape();
        PresentModel model = PresentModel.of(shape);
        float cardTop = shape.height + 0.5f - shape.lid - 0.4f;
        FormattedCharSequence text = Component.literal(state.to).getVisualOrderText();
        int width = Math.max(1, font.width(text));
        float scale = Math.min(0.011f, (model.tagWidth - 0.8f) / 16f / width);
        pose.pushPose();
        pose.translate(0, (cardTop - 2) / 16f, (shape.depth / 2f + 0.47f) / 16f);
        ClientCompat.rotate(pose, new Quaternionf().rotationZ(PresentModel.TAG_TILT));
        pose.scale(scale, -scale, scale);
        collector.submitText(pose, -width / 2f, -font.lineHeight / 2f + 1, text, false, Font.DisplayMode.POLYGON_OFFSET,
                light, 0xFF8E1B1B, 0, 0);
        pose.popPose();
    }

    /** The gifts rising out of the open box, spinning. */
    private void submitGifts(State state, float unwrap, PoseStack pose, SubmitNodeCollector collector, int light) {
        int count = state.gifts.size();
        if (count == 0) {
            return;
        }
        float rise = Unwrap.giftRise(unwrap);
        float size = Unwrap.giftScale(unwrap) * 1.6f;
        float spin = Unwrap.giftSpin(unwrap);
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2 / count + spin * 0.3;
            float radius = count == 1 ? 0 : 3.2f;
            pose.pushPose();
            pose.translate((float) Math.cos(angle) * radius / 16f, (1.5f + rise) / 16f, (float) Math.sin(angle) * radius / 16f);
            ClientCompat.rotate(pose, new Quaternionf().rotationY(spin + i));
            pose.scale(size, size, size);
            state.gifts.get(i).submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}

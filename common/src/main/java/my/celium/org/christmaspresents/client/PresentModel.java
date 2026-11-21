package my.celium.org.christmaspresents.client;

import java.util.EnumMap;
import java.util.Map;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

import my.celium.org.christmaspresents.PresentStyle;
import my.celium.org.christmaspresents.Unwrap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * The present's pieces for one box shape. Every piece is its own model part built around its
 * own hinge, and is never changed after baking: models are shared by every present of that
 * shape and drawn at the end of the frame, so all movement goes into the pose stack instead.
 *
 * <p>Coordinates are entity-style pixels: y grows downwards, the floor is y = 0 and the front
 * of the present is +z. The renderer flips them into the world.
 */
final class PresentModel {
    private static final Map<PresentStyle.Shape, PresentModel> CACHE = new EnumMap<>(PresentStyle.Shape.class);

    /** A side of the box: where its hinge is and which way it folds open. */
    private record Side(float x, float z, boolean aroundX, float sign, ModelPart paper, ModelPart ribbon) {
    }

    final PresentStyle.Shape shape;
    private final ModelPart bottom;
    private final Side[] sides;
    private final ModelPart lidPaper;
    private final ModelPart lidRibbon;
    private final Map<PresentStyle.Bow, ModelPart> bows = new EnumMap<>(PresentStyle.Bow.class);
    /** The name tag hangs a little crooked. */
    static final float TAG_TILT = 0.1f;

    final ModelPart tag;
    final float tagWidth;
    private final float lidY;

    static PresentModel of(PresentStyle.Shape shape) {
        return CACHE.computeIfAbsent(shape, PresentModel::new);
    }

    private static ModelPart part(CubeListBuilder cubes) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("piece", cubes, PartPose.ZERO);
        return mesh.getRoot().bake(64, 64);
    }

    private PresentModel(PresentStyle.Shape shape) {
        this.shape = shape;
        float w = shape.width;
        float h = shape.height;
        float d = shape.depth;
        float lid = shape.lid;
        lidY = -(h + 0.5f);

        bottom = part(CubeListBuilder.create().addBox(-w / 2, -0.5f, -d / 2, w, 0.5f, d));
        sides = new Side[] {
                // front (+z) and back (-z) fold around x; right (+x) and left (-x) around z
                new Side(0, d / 2, true, -1, part(CubeListBuilder.create().addBox(-w / 2, -h, -0.5f, w, h, 0.5f)),
                        part(CubeListBuilder.create().addBox(-1, -h, 0, 2, h, 0.15f))),
                new Side(0, -d / 2, true, 1, part(CubeListBuilder.create().addBox(-w / 2, -h, 0, w, h, 0.5f)),
                        part(CubeListBuilder.create().addBox(-1, -h, -0.15f, 2, h, 0.15f))),
                new Side(w / 2, 0, false, 1, part(CubeListBuilder.create().addBox(-0.5f, -h, -d / 2 + 0.5f, 0.5f, h, d - 1)),
                        part(CubeListBuilder.create().addBox(0, -h, -1, 0.15f, h, 2))),
                new Side(-w / 2, 0, false, -1, part(CubeListBuilder.create().addBox(0, -h, -d / 2 + 0.5f, 0.5f, h, d - 1)),
                        part(CubeListBuilder.create().addBox(-0.15f, -h, -1, 0.15f, h, 2))),
        };
        lidPaper = part(CubeListBuilder.create().addBox(-(w + 1) / 2, 0, -(d + 1) / 2, w + 1, lid, d + 1));
        lidRibbon = part(CubeListBuilder.create()
                .addBox(-(w + 1.3f) / 2, -0.12f, -1, w + 1.3f, lid + 0.12f, 2)
                .addBox(-1, -0.18f, -(d + 1.3f) / 2, 2, lid + 0.18f, d + 1.3f));

        float s = shape == PresentStyle.Shape.SMALL ? 0.75f : 1f;
        bows.put(PresentStyle.Bow.BOW, bow(s));
        bows.put(PresentStyle.Bow.BIG_BOW, bow(s * 1.55f));
        bows.put(PresentStyle.Bow.ROSETTE, rosette(s));
        bows.put(PresentStyle.Bow.KNOT, knot(s));

        // The name tag: a little card hanging from the front ribbon, just below the lid.
        MeshDefinition tagMesh = new MeshDefinition();
        tagWidth = Math.min(8, w - 0.5f);
        tagMesh.getRoot().addOrReplaceChild("card", CubeListBuilder.create().addBox(-tagWidth / 2, 0, 0, tagWidth, 4, 0.2f),
                PartPose.offsetAndRotation(0, -(h + 0.5f - lid) + 0.4f, d / 2 + 0.25f, 0, 0, TAG_TILT));
        tag = tagMesh.getRoot().bake(64, 64);
    }

    /** Two loops leaning outwards, a knot in the middle and two tails lying on the lid. */
    private static ModelPart bow(float s) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition bow = mesh.getRoot();
        bow.addOrReplaceChild("knot", CubeListBuilder.create().addBox(-1.2f * s, -1.4f * s, -1.2f * s, 2.4f * s, 1.4f * s, 2.4f * s),
                PartPose.ZERO);
        bow.addOrReplaceChild("loop_l", CubeListBuilder.create().addBox(-4.2f * s, -2.8f * s, -0.8f * s, 4.2f * s, 2.8f * s, 1.6f * s),
                PartPose.offsetAndRotation(-0.6f * s, -0.7f * s, 0, 0, 0.18f, 0.45f));
        bow.addOrReplaceChild("loop_r", CubeListBuilder.create().addBox(0, -2.8f * s, -0.8f * s, 4.2f * s, 2.8f * s, 1.6f * s),
                PartPose.offsetAndRotation(0.6f * s, -0.7f * s, 0, 0, -0.18f, -0.45f));
        tails(bow, s);
        return mesh.getRoot().bake(64, 64);
    }

    /** A round pom-pom of eight loops around a centre. */
    private static ModelPart rosette(float s) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition bow = mesh.getRoot();
        bow.addOrReplaceChild("centre", CubeListBuilder.create().addBox(-1.1f * s, -2.6f * s, -1.1f * s, 2.2f * s, 2.4f * s, 2.2f * s),
                PartPose.ZERO);
        for (int i = 0; i < 8; i++) {
            bow.addOrReplaceChild("petal" + i, CubeListBuilder.create().addBox(-0.8f * s, -2.4f * s, 0, 1.6f * s, 2.4f * s, 3.2f * s),
                    PartPose.offsetAndRotation(0, -0.3f * s, 0, -0.75f, (float) (i * Math.PI / 4), 0));
        }
        return mesh.getRoot().bake(64, 64);
    }

    /** Just a flat knot where the ribbons cross, with short tails. */
    private static ModelPart knot(float s) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition bow = mesh.getRoot();
        bow.addOrReplaceChild("knot", CubeListBuilder.create().addBox(-1.6f * s, -0.9f * s, -1.6f * s, 3.2f * s, 0.9f * s, 3.2f * s),
                PartPose.offsetAndRotation(0, 0, 0, 0, 0.785f, 0));
        tails(bow, s * 0.8f);
        return mesh.getRoot().bake(64, 64);
    }

    private static void tails(PartDefinition bow, float s) {
        bow.addOrReplaceChild("tail_l", CubeListBuilder.create().addBox(-0.7f * s, 0, 0, 1.4f * s, 0.15f, 3.6f * s),
                PartPose.offsetAndRotation(0, -0.15f, 0.6f * s, 0, -0.55f, 0));
        bow.addOrReplaceChild("tail_r", CubeListBuilder.create().addBox(-0.7f * s, 0, 0, 1.4f * s, 0.15f, 3.6f * s),
                PartPose.offsetAndRotation(0, -0.15f, 0.6f * s, 0, 0.55f, 0));
    }

    /**
     * Submits the whole present for one moment of the opening animation ({@code t} in ticks,
     * negative while it is closed). The pose stack is in model space: entity pixels, flipped.
     */
    void submit(SubmitNodeCollector collector, PoseStack pose, RenderType paperType, RenderType ribbonType,
            int paperColor, int ribbonColor, PresentStyle.Bow bowStyle, float t, int light) {
        boolean open = t >= 0;
        float fade = open ? Unwrap.paperFade(t) : 1;
        if (fade <= 0.01f) {
            return;
        }
        pose.pushPose();
        pose.scale(fade, fade, fade);

        draw(collector, pose, bottom, paperType, paperColor, light);

        float fold = open ? Unwrap.wallFold(t) : 0;
        float ribbonLength = open ? Unwrap.sideRibbon(t) : 1;
        for (Side side : sides) {
            pose.pushPose();
            pose.translate(side.x / 16f, 0, side.z / 16f);
            Quaternionf turn = side.aroundX ? new Quaternionf().rotationX(side.sign * fold)
                    : new Quaternionf().rotationZ(side.sign * fold);
            ClientCompat.rotate(pose, turn);
            draw(collector, pose, side.paper, paperType, paperColor, light);
            if (ribbonLength > 0.01f) {
                pose.scale(1, ribbonLength, 1);
                draw(collector, pose, side.ribbon, ribbonType, ribbonColor, light);
            }
            pose.popPose();
        }

        // The lid, with its ribbon cross and the bow on top.
        float bow = open ? Unwrap.bowScale(t) : 1;
        pose.pushPose();
        if (open) {
            pose.translate(Unwrap.lidAway(t, shape.width) / 16f,
                    (lidY - Unwrap.lidLift(t, shape.height, shape.lid)) / 16f, -Unwrap.lidAway(t, shape.depth) / 16f);
            ClientCompat.rotate(pose, new Quaternionf().rotationY(Unwrap.lidSpin(t)));
            ClientCompat.rotate(pose, new Quaternionf().rotationX(-Unwrap.lidFlip(t)));
        } else {
            pose.translate(0, lidY / 16f, 0);
        }
        draw(collector, pose, lidPaper, paperType, paperColor, light);
        if (bow > 0.02f) {
            draw(collector, pose, lidRibbon, ribbonType, ribbonColor, light);
            pose.translate(0, -0.18f / 16f, 0);
            if (open) {
                ClientCompat.rotate(pose, new Quaternionf().rotationY(Unwrap.bowSpin(t)));
                pose.scale(bow, bow, bow);
            }
            draw(collector, pose, bows.get(bowStyle), ribbonType, ribbonColor, light);
        }
        pose.popPose();
        pose.popPose();
    }

    private static void draw(SubmitNodeCollector collector, PoseStack pose, ModelPart part, RenderType type, int color,
            int light) {
        ClientCompat.part(collector, part, pose, type, light, OverlayTexture.NO_OVERLAY, color);
    }
}

package my.celium.org.christmaspresents.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import my.celium.org.christmaspresents.PresentBlockEntity;
import my.celium.org.christmaspresents.PresentStyle;
import my.celium.org.christmaspresents.Unwrap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;

/**
 * The opener's hands. While a present opens, its opener steps up to it and opens it with both
 * hands: in first person the arms reach into the view and do the unwrapping (pull the bow apart,
 * lift the lid, push the sides down, catch the gifts); everyone else sees the player hold both
 * arms out to the present.
 */
public final class PresentHands {
    /** Who is opening which present, and when they started, for the third-person arm pose. */
    private record Opening(BlockPos pos, long start, Vec3 center, float height) {
    }

    /** The local player's walk up to the present. */
    private record Approach(Vec3 from, float fromYaw, float fromPitch, Vec3 to, boolean move) {
    }

    private static final Map<UUID, Opening> OPENINGS = new HashMap<>();
    private static final Map<BlockPos, Approach> APPROACHES = new HashMap<>();

    private PresentHands() {
    }

    /** Called every client tick for every opening present. */
    public static void tick(PresentBlockEntity present, float t) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        UUID opener = present.opener();
        if (me == null || mc.level == null || opener == null) {
            return;
        }
        BlockPos pos = present.getBlockPos();
        PresentStyle.Shape shape = present.style().shape();
        Vec3 center = Vec3.atBottomCenterOf(pos);
        OPENINGS.put(opener, new Opening(pos.immutable(), mc.level.getGameTime() - (long) t, center,
                (shape.height + 0.5f) / 16f));
        OPENINGS.values().removeIf(o -> mc.level.getGameTime() - o.start > Unwrap.TICKS + 2);

        if (!opener.equals(me.getUUID()) || t > Unwrap.TICKS) {
            APPROACHES.remove(pos);
            return;
        }
        Approach approach = APPROACHES.computeIfAbsent(pos.immutable(), p -> startApproach(me, center));
        float k = Unwrap.easeInOut(Unwrap.clamp01((t + 1) / Unwrap.APPROACH));
        if (approach.move) {
            me.setPos(approach.from.lerp(approach.to, k));
        }
        me.setDeltaMovement(Vec3.ZERO);
        Vec3 look = center.add(0, (shape.height + 0.5f) / 16f, 0).subtract(me.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)));
        me.setYRot(approach.fromYaw + wrap(yaw - approach.fromYaw) * k);
        me.setXRot(approach.fromPitch + (pitch - approach.fromPitch) * k);
        me.setYHeadRot(me.getYRot());
    }

    /** One step in front of the present, on the side the player is on, if there is room. */
    private static Approach startApproach(LocalPlayer me, Vec3 center) {
        Vec3 away = new Vec3(me.getX() - center.x, 0, me.getZ() - center.z);
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(0, 0, 1);
        }
        Vec3 spot = center.add(away.normalize().scale(1.15)).add(0, me.getY() - center.y > 0.6 ? 0 : 0, 0);
        boolean free = me.level().noCollision(me, me.getBoundingBox().move(spot.subtract(me.position())));
        return new Approach(me.position(), me.getYRot(), me.getXRot(), spot, free && Math.abs(me.getY() - center.y) < 0.6);
    }

    private static float wrap(float degrees) {
        float d = degrees % 360;
        return d >= 180 ? d - 360 : d < -180 ? d + 360 : d;
    }

    /** Opening time for this player, or -1 when they are not opening a present. */
    static float openingTime(Entity entity, float partialTick) {
        Opening opening = entity == null ? null : OPENINGS.get(entity.getUUID());
        if (opening == null || entity.level() == null) {
            return -1;
        }
        float t = entity.level().getGameTime() - opening.start + partialTick;
        return t >= 0 && t <= Unwrap.TICKS ? t : -1;
    }

    /** Whether the local player's own first-person hands should hide for the opening animation. */
    public static boolean hideVanillaHands() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && openingTime(mc.player, 0) >= 0 && mc.options.getCameraType().isFirstPerson();
    }

    // ---- third person: both arms held out to the present

    /** Called after vanilla posed a player model. */
    public static void poseArms(int entityId, ModelPart rightArm, ModelPart leftArm) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || OPENINGS.isEmpty()) {
            return;
        }
        float t = openingTime(mc.level.getEntity(entityId), mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        if (t < 0) {
            return;
        }
        float u = t - Unwrap.APPROACH;
        float reach = Unwrap.easeOut(Unwrap.clamp01(t / Unwrap.APPROACH))
                * (1 - Unwrap.easeInOut(Unwrap.clamp01((u - 34) / 6f)));
        float lift = u < 9 ? 0 : u < 14 ? Unwrap.easeOut((u - 9) / 5f) : 1 - Unwrap.easeInOut(Unwrap.clamp01((u - 14) / 4f));
        float press = u < 16 ? 0 : 1 - Unwrap.clamp01((u - 24) / 4f);
        float pull = u < 0 ? 0 : u < 9 ? (float) Math.sin(u / 9f * Math.PI) : 0;
        float x = -0.95f - 0.6f * lift + 0.3f * press;
        rightArm.xRot = lerp(rightArm.xRot, x, reach);
        leftArm.xRot = lerp(leftArm.xRot, x, reach);
        rightArm.yRot = lerp(rightArm.yRot, -0.3f + 0.35f * pull, reach);
        leftArm.yRot = lerp(leftArm.yRot, 0.3f - 0.35f * pull, reach);
        rightArm.zRot = lerp(rightArm.zRot, 0.1f * pull, reach);
        leftArm.zRot = lerp(leftArm.zRot, -0.1f * pull, reach);
    }

    private static float lerp(float a, float b, float k) {
        return a + (b - a) * k;
    }

    // ---- first person: the arms that do the unwrapping

    /** Arm models, baked once per skin model type. */
    static final class Arms {
        final ModelPart right;
        final ModelPart left;

        Arms(ModelPart root) {
            this.right = root.getChild("right_arm");
            this.left = root.getChild("left_arm");
        }
    }

    /**
     * Draws the local player's arms reaching into the present. The pose stack sits at the
     * present's block corner, in world orientation.
     */
    static void submitArms(PoseStack pose, SubmitNodeCollector collector, BlockPos pos, PresentStyle.Shape shape,
            float t, int light, Arms wide, Arms slim) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        Camera camera = ClientCompat.camera(mc);
        Vec3 cam = camera.position();
        Vector3f forward = new Vector3f(camera.forwardVector());
        Vector3f up = new Vector3f(camera.upVector());
        Vector3f left = new Vector3f(camera.leftVector());
        PlayerSkin skin = me.getSkin();
        Arms arms = skin.model() == PlayerModelType.SLIM ? slim : wide;
        var type = RenderTypes.entityTranslucent(skin.body().texturePath());
        Vec3 base = Vec3.atBottomCenterOf(pos);
        for (int side = -1; side <= 1; side += 2) {
            // side 1 = right hand
            Vec3 hand = handTarget(base, shape, cam, forward, up, left, t, side);
            Vec3 shoulder = cam.add(vec(forward).scale(-0.05)).add(vec(up).scale(-0.42)).add(vec(left).scale(-side * 0.34));
            Vec3 toHand = hand.subtract(shoulder);
            double length = toHand.length();
            Vec3 dir = length < 1.0E-4 ? vec(forward) : toHand.scale(1 / length);
            double armLength = 0.66;
            if (length > armLength) {
                shoulder = hand.subtract(dir.scale(armLength));
            }
            ModelPart arm = side > 0 ? arms.right : arms.left;
            PartPose rest = arm.getInitialPose();
            pose.pushPose();
            pose.translate(shoulder.x - pos.getX(), shoulder.y - pos.getY(), shoulder.z - pos.getZ());
            ClientCompat.rotate(pose, new Quaternionf().rotationTo(0, -1, 0, (float) dir.x, (float) dir.y, (float) dir.z));
            pose.scale(-0.9375f, -0.9375f, 0.9375f);
            pose.translate(-rest.x() / 16f, -rest.y() / 16f, -rest.z() / 16f);
            ClientCompat.part(collector, arm, pose, type, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            pose.popPose();
        }
    }

    private static Vec3 vec(Vector3f v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    /**
     * Where each hand is, in world space: down by the body while walking up, on the bow loops
     * pulling them apart, at the sides of the lid lifting it, on the front pushing the sides
     * open, cupped under the rising gifts, and back down.
     */
    static Vec3 handTarget(Vec3 base, PresentStyle.Shape shape, Vec3 cam, Vector3f forward, Vector3f upV, Vector3f leftV,
            float t, int side) {
        float u = t - Unwrap.APPROACH;
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 toViewer = new Vec3(cam.x - base.x, 0, cam.z - base.z);
        Vec3 v = toViewer.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : toViewer.normalize();
        Vec3 right = new Vec3(v.z, 0, -v.x).scale(side);
        double top = (shape.height + 0.5) / 16;
        double half = Math.max(shape.width, shape.depth) / 32.0;
        double lid = shape.lid / 16.0;

        Vec3 rest = cam.add(vec(forward).scale(0.35)).add(vec(upV).scale(-0.75)).add(vec(leftV).scale(-side * 0.3));
        Vec3 bow0 = base.add(up.scale(top + 0.12)).add(right.scale(0.05)).add(v.scale(0.03));
        if (u < 0) {
            return lerp(rest, bow0, Unwrap.easeInOut(Unwrap.clamp01((u + Unwrap.APPROACH) / Unwrap.APPROACH)));
        }
        if (u < 9) {
            float p = Unwrap.easeOut(u / 9f);
            return bow0.add(right.scale(0.22 * p)).add(up.scale(0.05 * p));
        }
        Vec3 lidAt = base.add(up.scale(top - lid / 2 + Unwrap.lidLift(Math.min(u, 13), shape.height, shape.lid) / 16))
                .add(right.scale(half + 0.03));
        if (u < 11) {
            return lerp(base.add(up.scale(top + 0.17)).add(right.scale(0.27)), lidAt, (u - 9) / 2f);
        }
        if (u < 14) {
            return lidAt;
        }
        float fold17 = Unwrap.wallFold(17);
        if (u < 17) {
            return lerp(lidAt, push(base, v, up, right, shape, half, fold17), Unwrap.easeInOut((u - 14) / 3f));
        }
        if (u < 24) {
            return push(base, v, up, right, shape, half, Unwrap.wallFold(u));
        }
        Vec3 cup = base.add(up.scale((1.5 + Unwrap.giftRise(Math.max(u, 28))) / 16 - 0.04)).add(right.scale(0.09)).add(v.scale(0.06));
        if (u < 28) {
            return lerp(push(base, v, up, right, shape, half, Unwrap.wallFold(24)), cup, Unwrap.easeInOut((u - 24) / 4f));
        }
        if (u < 34) {
            return cup;
        }
        return lerp(cup, rest, Unwrap.easeInOut(Unwrap.clamp01((u - 34) / 6f)));
    }

    /** On top of this hand's side of the box, pressing it down as it folds open sideways. */
    private static Vec3 push(Vec3 base, Vec3 v, Vec3 up, Vec3 right, PresentStyle.Shape shape, double half, float fold) {
        double h = shape.height / 16.0;
        double reach = Math.min(Math.sin(fold) * h, h * 0.8);
        return base.add(right.scale(half + reach)).add(up.scale(Math.cos(fold) * h + 0.05)).add(v.scale(0.04));
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double k) {
        return a.add(b.subtract(a).scale(k));
    }
}

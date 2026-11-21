package my.celium.org.christmaspresents.client;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Client calls that differ between Minecraft 26.1, 26.2 and 26.3 (this copy: 26.1). */
final class ClientCompat {
    private ClientCompat() {
    }

    /** Draws a model part tinted with {@code color} (0xAARRGGBB). */
    static void part(SubmitNodeCollector collector, ModelPart part, PoseStack pose, RenderType renderType, int light,
            int overlay, int color) {
        collector.submitModelPart(part, pose, renderType, light, overlay, null, color, null);
    }

    static void rotate(PoseStack pose, Quaternionf rotation) {
        pose.mulPose(rotation);
    }

    static void setScreen(Minecraft minecraft, Screen screen) {
        minecraft.setScreen(screen);
    }

    static Camera camera(Minecraft minecraft) {
        return minecraft.gameRenderer.getMainCamera();
    }
}

package my.celium.org.christmaspresents.mixin;

import java.util.Collection;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a few ready-made presents to the Functional Blocks tab, so they can be
 * found with the creative search. Done here because each loader has its own tab event.
 */
@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin {
    @Shadow
    private Collection<ItemStack> displayItems;

    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @Inject(method = "buildContents", at = @At("TAIL"))
    private void christmaspresents$addItems(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey((CreativeModeTab) (Object) this);
        if (!my.celium.org.christmaspresents.PresentGate.on()) {
            return;
        }
        if (id == null || !id.getNamespace().equals("minecraft") || !id.getPath().equals("functional_blocks")) {
            return;
        }
        for (ItemStack stack : my.celium.org.christmaspresents.ChristmasPresentsMod.creativeItems()) {
            displayItems.add(stack);
            displayItemsSearchTab.add(stack.copy());
        }
    }
}

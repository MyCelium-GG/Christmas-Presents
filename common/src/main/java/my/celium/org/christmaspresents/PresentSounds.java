package my.celium.org.christmaspresents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * The mod's own sounds (assets/christmaspresents/sounds.json): paper being torn open, the lid
 * popping off with sleigh bells, a chime when the gifts come out, and wrapping. Sent as direct
 * holders, so nothing needs registering on any loader.
 */
final class PresentSounds {
    private PresentSounds() {
    }

    static void play(Level level, BlockPos pos, String name, float pitch) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ChristmasPresentsMod.ID, name));
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Holder.direct(event), SoundSource.BLOCKS,
                1.0f, pitch);
    }
}

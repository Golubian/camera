package de.maxhenkel.camera;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_REGISTER = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Main.MODID);

    //https://www.soundjay.com/mechanical/sounds/camera-shutter-click-01.mp3
    public static final DeferredHolder<SoundEvent, SoundEvent> TAKE_IMAGE = SOUND_REGISTER.register("take_image", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Main.MODID, "take_image")));

}

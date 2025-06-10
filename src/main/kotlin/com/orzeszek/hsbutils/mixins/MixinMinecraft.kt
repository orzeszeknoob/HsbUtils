package com.orzeszek.hsbutils.mixins

import net.minecraft.client.MinecraftClient
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(MinecraftClient::class)
class MixinMinecraft {
    @Inject(method = ["init"], at = [At("HEAD")])
    private fun onInit(callbackInfo: CallbackInfo) {
        // Your initialization code here
    }
} 
package com.orzeszek.mixins

import com.orzeszek.HsbUtils
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.hud.ChatHud
import net.minecraft.client.gui.hud.MessageIndicator
import net.minecraft.network.message.MessageType
import net.minecraft.text.Text
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents

@Mixin(ChatHud::class)
class MixinMinecraft {
    @Inject(
        method = ["addMessage(Lnet/minecraft/text/Text;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V"],
        at = [At("HEAD")]
    )
    private fun onChatMessage(message: Text, ci: CallbackInfo) {
        val messageString = message.string

        if (HsbUtils.MINESHAFT_PATTERN.matcher(messageString).find()) {
            HsbUtils.mineshaftDetected = true
            HsbUtils.lastMineshaftTime = System.currentTimeMillis()
            return
        }

        if (HsbUtils.MINESHAFT_LEAVE_PATTERN.matcher(messageString).find()) {
            HsbUtils.mineshaftLeaveDetected = true
            HsbUtils.lastMineshaftTime = System.currentTimeMillis()
            return
        }

        val matcher = HsbUtils.PRISMATIC_PATTERN.matcher(messageString)
        if (matcher.find()) {
            val gemstone = matcher.group(2).lowercase().trim()
            val file = java.io.File("config/hsbutils/$gemstone.txt")
            if (file.exists()) {
                HsbUtils.gemstoneType = gemstone
                HsbUtils.shouldExecuteCommand = true
            }
        }
    }
} 
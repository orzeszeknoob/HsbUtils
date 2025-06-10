package com.orzeszek.render

import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec3d
import net.minecraft.client.MinecraftClient
import org.lwjgl.opengl.GL11
import org.lwjgl.BufferUtils
import java.nio.FloatBuffer
import net.minecraft.client.render.Camera
import net.minecraft.client.gui.DrawContext

object WaypointRenderer {
    fun render(matrixStack: MatrixStack, camera: Camera, tickDelta: Float) {
        // TODO: Implement waypoint rendering logic
    }

    fun renderHud(drawContext: DrawContext, tickDelta: Float) {
        // TODO: Implement HUD rendering logic
    }

    private fun drawBoxOutline(matrixStack: MatrixStack, pos: Vec3d, r: Float, g: Float, b: Float) {
        val matrices = matrixStack.peek().positionMatrix

        val cam = MinecraftClient.getInstance().gameRenderer.camera
        val dx = cam.pos.x.toFloat()
        val dy = cam.pos.y.toFloat()
        val dz = cam.pos.z.toFloat()

        val x = pos.x.toFloat() - dx
        val y = pos.y.toFloat() - dy
        val z = pos.z.toFloat() - dz
        val x2 = x + 1f
        val y2 = y + 1f
        val z2 = z + 1f

        GL11.glPushMatrix()

        val fb: FloatBuffer = BufferUtils.createFloatBuffer(16)
        matrices.get(fb)
        fb.flip()
        GL11.glMultMatrixf(fb)

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glLineWidth(2.0f)

        GL11.glBegin(GL11.GL_LINES)
        GL11.glColor4f(r, g, b, 1.0f)

        // Bottom square
        GL11.glVertex3f(x, y, z)
        GL11.glVertex3f(x2, y, z)

        GL11.glVertex3f(x2, y, z)
        GL11.glVertex3f(x2, y, z2)

        GL11.glVertex3f(x2, y, z2)
        GL11.glVertex3f(x, y, z2)

        GL11.glVertex3f(x, y, z2)
        GL11.glVertex3f(x, y, z)

        // Top square
        GL11.glVertex3f(x, y2, z)
        GL11.glVertex3f(x2, y2, z)

        GL11.glVertex3f(x2, y2, z)
        GL11.glVertex3f(x2, y2, z2)

        GL11.glVertex3f(x2, y2, z2)
        GL11.glVertex3f(x, y2, z2)

        GL11.glVertex3f(x, y2, z2)
        GL11.glVertex3f(x, y2, z)

        // Vertical lines
        GL11.glVertex3f(x, y, z)
        GL11.glVertex3f(x, y2, z)

        GL11.glVertex3f(x2, y, z)
        GL11.glVertex3f(x2, y2, z)

        GL11.glVertex3f(x2, y, z2)
        GL11.glVertex3f(x2, y2, z2)

        GL11.glVertex3f(x, y, z2)
        GL11.glVertex3f(x, y2, z2)

        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)

        GL11.glPopMatrix()
    }
}
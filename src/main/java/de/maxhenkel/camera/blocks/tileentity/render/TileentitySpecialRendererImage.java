package de.maxhenkel.camera.blocks.tileentity.render;

import de.maxhenkel.camera.Main;
import de.maxhenkel.camera.blocks.BlockImageFrame;
import de.maxhenkel.camera.blocks.tileentity.TileEntityImage;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class TileentitySpecialRendererImage extends TileEntityRenderer<TileEntityImage> {

    public static final ResourceLocation DEFAULT_IMAGE = new ResourceLocation(Main.MODID, "textures/images/default_image.png");
    public static final ResourceLocation EMPTY_IMAGE = new ResourceLocation(Main.MODID, "textures/images/empty_image.png");
    public static final ResourceLocation FRAME_SIDE = new ResourceLocation(Main.MODID, "textures/images/frame_side.png");
    public static final ResourceLocation FRAME_BACK = new ResourceLocation(Main.MODID, "textures/images/frame_back.png");

    @Override
    public void render(TileEntityImage te, double x, double y, double z, float partialTicks, int destroyStage) {
        float ratio = 1.5F;
        ResourceLocation resourceLocation = EMPTY_IMAGE;
        if (te.hasImage()) {
            ResourceLocation rl = TextureCache.instance().getImage(te.getUuid());
            if (rl != null) {
                resourceLocation = rl;
                NativeImage image = TextureCache.instance().getNativeImage(te.getUuid());
                ratio = (float) image.getWidth() / (float) image.getHeight();
            } else {
                resourceLocation = DEFAULT_IMAGE;
            }
        }

        GlStateManager.pushMatrix();
        GlStateManager.translated(x, y + 1D, z);
        GlStateManager.rotatef(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.disableLighting();
        GlStateManager.disableBlend();

        EnumFacing facing = te.getBlockState().get(BlockImageFrame.FACING);

        rotate(facing);

        bindTexture(resourceLocation);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        float ratioX;
        float ratioY;

        if (ratio >= 1F) {
            ratioY = (1F - 1F / ratio) / 2F;
            ratioX = 0F;
        } else {
            ratioX = (1F - ratio) / 2F;
            ratioY = 0F;
        }

        buffer.pos(0D + ratioX, 0D + ratioY, -0.0625D).tex(0D, 0D).endVertex();
        buffer.pos(0D + ratioX, 1D - ratioY, -0.0625D).tex(0D, 1D).endVertex();
        buffer.pos(1D - ratioX, 1D - ratioY, -0.0625D).tex(1D, 1D).endVertex();
        buffer.pos(1D - ratioX, 0D + ratioY, -0.0625D).tex(1D, 0D).endVertex();

        tessellator.draw();

        bindTexture(FRAME_SIDE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        //Left
        buffer.pos(0D + ratioX, 0D + ratioY, 0D).tex(0D, 0D + ratioY).endVertex();
        buffer.pos(0D + ratioX, 1D - ratioY, 0D).tex(0D, 1D - ratioY).endVertex();
        buffer.pos(0D + ratioX, 1D - ratioY, -0.0625D).tex(0.0625D, 1D - ratioY).endVertex();
        buffer.pos(0D + ratioX, 0D + ratioY, -0.0625D).tex(0.0625D, 0D + ratioY).endVertex();

        //Right
        buffer.pos(1D - ratioX, 0D + ratioY, 0D).tex(0D, 0D + ratioY).endVertex();
        buffer.pos(1D - ratioX, 0D + ratioY, -0.0625D).tex(0.0625D, 0D + ratioY).endVertex();
        buffer.pos(1D - ratioX, 1D - ratioY, -0.0625D).tex(0.0625D, 1D - ratioY).endVertex();
        buffer.pos(1D - ratioX, 1D - ratioY, 0D).tex(0D, 1D - ratioY).endVertex();

        //Bottom
        buffer.pos(0D + ratioX, 1D - ratioY, 0D).tex(0D + ratioX, 0D).endVertex();
        buffer.pos(1D - ratioX, 1D - ratioY, 0D).tex(1D - ratioX, 0D).endVertex();
        buffer.pos(1D - ratioX, 1D - ratioY, -0.0625D).tex(1D - ratioX, 0.0625D).endVertex();
        buffer.pos(0D + ratioX, 1D - ratioY, -0.0625D).tex(0D + ratioX, 0.0625D).endVertex();

        //Top
        buffer.pos(0D + ratioX, 0D + ratioY, 0D).tex(0D + ratioX, 0D).endVertex();
        buffer.pos(0D + ratioX, 0D + ratioY, -0.0625D).tex(0D + ratioX, 0.0625D).endVertex();
        buffer.pos(1D - ratioX, 0D + ratioY, -0.0625D).tex(1D - ratioX, 0.0625D).endVertex();
        buffer.pos(1D - ratioX, 0D + ratioY, 0D).tex(1D - ratioX, 0D).endVertex();

        tessellator.draw();

        bindTexture(FRAME_BACK);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        buffer.pos(1D - ratioX, 0D + ratioY, 0D).tex(1D - ratioX, 0D + ratioY).endVertex();
        buffer.pos(1D - ratioX, 1D - ratioY, 0D).tex(1D - ratioX, 1D - ratioY).endVertex();
        buffer.pos(0D + ratioX, 1D - ratioY, 0D).tex(0D + ratioX, 1D - ratioY).endVertex();
        buffer.pos(0D + ratioX, 0D + ratioY, 0D).tex(0D + ratioX, 0D + ratioY).endVertex();

        tessellator.draw();

        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    public static void rotate(EnumFacing facing) {
        switch (facing) {
            case NORTH:
                GlStateManager.rotatef(180F, 0F, 1F, 0F);
                GlStateManager.translated(-1D, 0D, 1D);
                break;
            case SOUTH:
                break;
            case EAST:
                GlStateManager.rotatef(270F, 0F, 1F, 0F);
                GlStateManager.translated(-1D, 0D, 0D);
                break;
            case WEST:
                GlStateManager.rotatef(90F, 0F, 1F, 0F);
                GlStateManager.translated(0D, 0D, 1D);
                break;
        }
    }
}
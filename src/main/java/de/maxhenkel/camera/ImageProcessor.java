package de.maxhenkel.camera;

import de.maxhenkel.camera.net.MessagePartialImage;
import net.minecraft.client.renderer.texture.NativeImage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;

public class ImageProcessor {

    public static void sendScreenshot(UUID uuid, BufferedImage image) {
        if (image.getWidth() > 1080) {
            float ratio = ((float) image.getHeight()) / ((float) image.getWidth());
            int newHeight = ((int) (((float) 1080) * ratio));
            image = ImageTools.resize(image, 1080, newHeight);
        }

        byte[] data;
        try {
            data = ImageTools.toBytes(image);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        int size = data.length;
        if (size < 30_000) {
            Main.SIMPLE_CHANNEL.sendToServer(new MessagePartialImage(uuid, 0, size, data));
        } else {

            int bufferProgress = 0;
            byte[] currentBuffer = new byte[30_000];
            for (int i = 0; i < size; i++) {
                if (bufferProgress >= currentBuffer.length) {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessagePartialImage(uuid, i - currentBuffer.length, data.length, currentBuffer));
                    bufferProgress = 0;
                    currentBuffer = new byte[currentBuffer.length];
                }
                currentBuffer[bufferProgress] = data[i];
                bufferProgress++;
            }

            if (bufferProgress > 0) {
                byte[] rest = new byte[bufferProgress];
                System.arraycopy(currentBuffer, 0, rest, 0, bufferProgress);
                Main.SIMPLE_CHANNEL.sendToServer(new MessagePartialImage(uuid, size - rest.length, data.length, rest));
            }
        }
    }

    public static void sendScreenshodThreaded(UUID uuid, BufferedImage image) {
        new Thread(() -> sendScreenshot(uuid, image), "ProcessImageThread").start();
    }

    public static void sendScreenshodThreaded(UUID uuid, NativeImage image) {
        new Thread(() -> sendScreenshot(uuid, ImageTools.fromNativeImage(image)), "ProcessImageThread").start();
    }

}

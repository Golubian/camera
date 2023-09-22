package de.maxhenkel.camera.net;

import de.maxhenkel.camera.Main;
import de.maxhenkel.camera.items.CameraItem;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.UUID;

public class MessageRequestUploadCustomImage implements Message<MessageRequestUploadCustomImage> {

    private UUID uuid;

    public MessageRequestUploadCustomImage() {

    }

    public MessageRequestUploadCustomImage(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (Main.PACKET_MANAGER.canTakeImage(player.getUUID())) {
            if (CameraItem.consumePaper(player)) {
                Main.SIMPLE_CHANNEL.reply(new MessageUploadCustomImage(uuid), context);
            } else {
                player.displayClientMessage(Component.translatable("message.no_consumable"), true);
            }
        } else {
            player.displayClientMessage(Component.translatable("message.image_cooldown"), true);
        }
    }

    @Override
    public MessageRequestUploadCustomImage fromBytes(FriendlyByteBuf buf) {
        uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }

}

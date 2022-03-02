package de.maxhenkel.camera;

import de.maxhenkel.camera.entities.ImageEntity;
import de.maxhenkel.camera.entities.ImageRenderer;
import de.maxhenkel.camera.gui.AlbumContainer;
import de.maxhenkel.camera.gui.AlbumInventoryContainer;
import de.maxhenkel.camera.gui.AlbumInventoryScreen;
import de.maxhenkel.camera.gui.LecternAlbumScreen;
import de.maxhenkel.camera.items.AlbumItem;
import de.maxhenkel.camera.items.CameraItem;
import de.maxhenkel.camera.items.ImageFrameItem;
import de.maxhenkel.camera.items.ImageItem;
import de.maxhenkel.camera.net.*;
import de.maxhenkel.corelib.ClientRegistry;
import de.maxhenkel.corelib.CommonRegistry;
import de.maxhenkel.corelib.tag.ItemTag;
import de.maxhenkel.corelib.tag.SingleElementTag;
import de.maxhenkel.corelib.tag.Tag;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

@Mod(Main.MODID)
public class Main {

    public static final String MODID = "camera";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static SimpleChannel SIMPLE_CHANNEL;
    public static PacketManager PACKET_MANAGER;

    public static SimpleRecipeSerializer<RecipeImageCloning> CRAFTING_SPECIAL_IMAGE_CLONING;
    public static ImageFrameItem FRAME_ITEM;
    public static CameraItem CAMERA;
    public static ImageItem IMAGE;
    public static AlbumItem ALBUM;
    public static MenuType<AlbumInventoryContainer> ALBUM_INVENTORY_CONTAINER;
    public static MenuType<AlbumContainer> ALBUM_CONTAINER;
    public static EntityType<ImageEntity> IMAGE_ENTITY_TYPE;
    private static final ResourceLocation PAPER_LOCATION = new ResourceLocation(Main.MODID, "image_paper");
    public static Tag<Item> IMAGE_PAPER;

    public static ServerConfig SERVER_CONFIG;
    public static ClientConfig CLIENT_CONFIG;

    @OnlyIn(Dist.CLIENT)
    public static KeyMapping KEY_NEXT;
    @OnlyIn(Dist.CLIENT)
    public static KeyMapping KEY_PREVIOUS;

    public Main() {
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(Item.class, this::registerItems);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(SoundEvent.class, this::registerSounds);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(EntityType.class, this::registerEntities);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(MenuType.class, this::registerContainers);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(RecipeSerializer.class, this::registerRecipes);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);

        SERVER_CONFIG = CommonRegistry.registerConfig(ModConfig.Type.SERVER, ServerConfig.class, true);
        CLIENT_CONFIG = CommonRegistry.registerConfig(ModConfig.Type.CLIENT, ClientConfig.class, true);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(Main.this::clientSetup));
        Registry<Item> blocks = RegistryAccess.BUILTIN.get().registryOrThrow(Registry.ITEM_REGISTRY);
        TagKey<Item> itemTagKey = TagKey.create(Registry.ITEM_REGISTRY, PAPER_LOCATION);
        Optional<HolderSet.Named<Item>> tag = blocks.getTag(itemTagKey);
        if (tag.isPresent()) {
            IMAGE_PAPER = new ItemTag(tag.get());
        } else {
            IMAGE_PAPER = new SingleElementTag<>(PAPER_LOCATION, Items.PAPER);
            LOGGER.fatal("Failed to get image paper tag");
        }
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ServerEvents());

        SIMPLE_CHANNEL = CommonRegistry.registerChannel(Main.MODID, "default");
        PACKET_MANAGER = new PacketManager();
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 0, MessagePartialImage.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 1, MessageTakeImage.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 2, MessageRequestImage.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 3, MessageImage.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 4, MessageImageUnavailable.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 5, MessageSetShader.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 6, MessageDisableCameraMode.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 7, MessageResizeFrame.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 8, MessageRequestUploadCustomImage.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 9, MessageUploadCustomImage.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 10, MessageAlbumPage.class);
        CommonRegistry.registerMessage(SIMPLE_CHANNEL, 11, MessageTakeBook.class);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new ClientEvents());

        KEY_NEXT = ClientRegistry.registerKeyBinding("key.next_image", "key.categories.misc", GLFW.GLFW_KEY_DOWN);

        KEY_PREVIOUS = ClientRegistry.registerKeyBinding("key.previous_image", "key.categories.misc", GLFW.GLFW_KEY_UP);

        ClientRegistry.<AlbumInventoryContainer, AlbumInventoryScreen>registerScreen(Main.ALBUM_INVENTORY_CONTAINER, AlbumInventoryScreen::new);
        ClientRegistry.<AlbumContainer, LecternAlbumScreen>registerScreen(Main.ALBUM_CONTAINER, LecternAlbumScreen::new);

        EntityRenderers.register(IMAGE_ENTITY_TYPE, ImageRenderer::new);
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                FRAME_ITEM = new ImageFrameItem(),
                CAMERA = new CameraItem(),
                IMAGE = new ImageItem(),
                ALBUM = new AlbumItem()
        );
    }

    @SubscribeEvent
    public void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(
                ModSounds.TAKE_IMAGE
        );
    }

    @SubscribeEvent
    public void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
        IMAGE_ENTITY_TYPE = CommonRegistry.registerEntity(Main.MODID, "image_frame", MobCategory.MISC, ImageEntity.class, builder -> {
            builder.setTrackingRange(256)
                    .setUpdateInterval(20)
                    .setShouldReceiveVelocityUpdates(false)
                    .sized(1F, 1F)
                    .setCustomClientFactory((spawnEntity, world) -> new ImageEntity(world));
        });
        event.getRegistry().register(IMAGE_ENTITY_TYPE);
    }

    @SubscribeEvent
    public void registerContainers(RegistryEvent.Register<MenuType<?>> event) {
        ALBUM_INVENTORY_CONTAINER = new MenuType<>(AlbumInventoryContainer::new);
        ALBUM_INVENTORY_CONTAINER.setRegistryName(new ResourceLocation(Main.MODID, "album_inventory"));
        event.getRegistry().register(ALBUM_INVENTORY_CONTAINER);

        ALBUM_CONTAINER = new MenuType<>((i, playerInventory) -> new AlbumContainer(i));
        ALBUM_CONTAINER.setRegistryName(new ResourceLocation(Main.MODID, "album"));
        event.getRegistry().register(ALBUM_CONTAINER);
    }

    @SubscribeEvent
    public void registerRecipes(RegistryEvent.Register<RecipeSerializer<?>> event) {
        CRAFTING_SPECIAL_IMAGE_CLONING = new SimpleRecipeSerializer<>(RecipeImageCloning::new);
        CRAFTING_SPECIAL_IMAGE_CLONING.setRegistryName(MODID, "crafting_special_imagecloning");
        event.getRegistry().register(CRAFTING_SPECIAL_IMAGE_CLONING);
    }

}

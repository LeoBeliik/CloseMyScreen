package com.leobeliik.closemyscreen;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CloseMyScreen.MODID)
public class CloseMyScreen {
    // Define mod id in a common place for everything to reference
    static final String MODID = "closemyscreen";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String screenListFile = "config/closeMyScreen/close.dat";

    private static final KeyMapping screenKey = screenKey();
    private static List<String> noNoScrens = new ArrayList<>();

    public CloseMyScreen() {
        noNoScrens.clear();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> "", (a, b) -> true));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(this::keyRegistry));
        noNoScrens.add("CreativeModeInventoryScreen");
        loadScreens().stream().filter(loadScreen -> !noNoScrens.contains(loadScreen)).forEach(loadScreen -> noNoScrens.add(loadScreen));
    }


    @SuppressWarnings("UnstableApiUsage")
    private static List<String> loadScreens() {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(screenListFile), StandardCharsets.UTF_8)) {
            return gson.fromJson(new JsonReader(reader), new TypeToken<List<String>>() {}.getType());
        } catch (Exception e) {
            return noNoScrens;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void keyRegistry(final RegisterKeyMappingsEvent event) {
        event.register(screenKey);
    }

    @OnlyIn(Dist.CLIENT)
    private static KeyMapping screenKey() {
        return new KeyMapping(
                ("Don't close screen"),
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                "key.categories.misc");
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onKeyInput(ScreenEvent.KeyPressed.Pre event) {
        Screen screen = event.getScreen();
        Minecraft minecraft = screen.getMinecraft();
        String screenClass = screen.getClass().getName().split("[.]")[screen.getClass().getName().split("[.]").length - 1];

        if (screenKey.matches(event.getKeyCode(), event.getScanCode()) && minecraft.player != null) {
            noNoScrens.add(screenClass);
            minecraft.player.sendSystemMessage(Component.nullToEmpty("Screen will now close with the Inv button"));
        } else if (noNoScrens.contains(screenClass)) {
            if (minecraft.level != null && minecraft.options.keyInventory.matches(event.getKeyCode(), event.getScanCode())) {
                for (GuiEventListener renderable : screen.children()) {
                    if (renderable instanceof EditBox searchBar && searchBar.canConsumeInput()) {
                        return;
                    }
                }
                screen.onClose();
                event.setCanceled(true);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMouseClick(ScreenEvent.MouseButtonPressed event) {
        Screen screen = event.getScreen();
        Minecraft minecraft = screen.getMinecraft();
        if (minecraft.level != null && minecraft.player != null) {
            try {
                for (GuiEventListener renderable : screen.children()) {
                    if (renderable instanceof EditBox searchBar && searchBar.isFocused() && !searchBar.mouseClicked(event.getMouseX(), event.getMouseY(), 0)) {
                        searchBar.setCanLoseFocus(true);
                        searchBar.setFocus(false);
                        break;
                    }
                }
            } catch (ConcurrentModificationException e) {
                minecraft.player.sendSystemMessage(Component.nullToEmpty("Woops"));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent()
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        saveScreens();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void saveScreens() {
        new File("config/closeMyScreen/").mkdirs();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(screenListFile), StandardCharsets.UTF_8)) {
            writer.write(gson.toJson(noNoScrens));
        } catch (IOException ignored) {}
    }
}

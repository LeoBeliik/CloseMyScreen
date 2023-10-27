package com.leobeliik.closemyscreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CloseMyScreen.MODID)
public class CloseMyScreen {
    // Define mod id in a common place for everything to reference
    static final String MODID = "closemyscreen";

    public CloseMyScreen() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> "", (a, b) -> true));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onKeyInput(ScreenEvent.KeyPressed.Pre event) {
        Screen screen = event.getScreen();
        Minecraft minecraft = screen.getMinecraft();
        if (minecraft.level != null && minecraft.options.keyInventory.matches(event.getKeyCode(), event.getScanCode())) {
            for (GuiEventListener renderable : screen.children()) {
                if (renderable instanceof EditBox searchBar && searchBar.canConsumeInput() || renderable instanceof PageButton) {
                    return;
                }
            }
            screen.onClose();
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMouseClick(ScreenEvent.MouseButtonPressed event) {
        Screen screen = event.getScreen();
        Minecraft minecraft = screen.getMinecraft();
        if (minecraft.level != null) {
            for (GuiEventListener renderable : screen.children()) {
                if (renderable instanceof EditBox searchBar && searchBar.isFocused() && !searchBar.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
                    searchBar.setCanLoseFocus(true);
                    searchBar.setFocused(false);
                    break;
                }
            }
        }
    }
}

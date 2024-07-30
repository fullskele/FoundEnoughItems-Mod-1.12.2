package com.fullskele.foundenoughitems;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

@Mod(
        modid = FoundEnoughItems.MODID,
        name = FoundEnoughItems.NAME,
        version = FoundEnoughItems.VERSION,
        dependencies = "after:jei"
)
public class FoundEnoughItems
{
    public static final String MODID = "foundenoughitems";
    public static final String NAME = "Found Enough Items";
    public static final String VERSION = "1.0.1";

    public static File config;


    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        ConfigHandler.RegisterConfig(event);

    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new IngredientBlacklist());
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }
}

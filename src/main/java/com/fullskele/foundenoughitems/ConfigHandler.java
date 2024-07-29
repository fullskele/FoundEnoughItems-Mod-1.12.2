package com.fullskele.foundenoughitems;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

public class ConfigHandler {
    public static Configuration config;

    public static String[] JEI_BLACKLIST;
    public static boolean DOES_DISCOVER_MESSAGE;
    public static boolean DOES_DISCOVER_SOUND;
    public static String DISCOVER_SOUND;
    public static float DISCOVER_PITCH;
    public static float DISCOVER_VOLUME;

    public static void init(File file) {

        config = new Configuration(file);

        String category;

        category = "Item Blacklist Options";
        config.addCustomCategoryComment(category, "Item Blacklist Options");
        JEI_BLACKLIST = config.getStringList("Items to Discover", category, new String[]{"minecraft:ender_eye"}, "Items to hide in JEI, along with their crafting recipes.");

        category = "Item Reveal Options";
        config.addCustomCategoryComment(category, "Item Reveal Options");
        DOES_DISCOVER_MESSAGE = config.getBoolean("Discover Message Toggle", category, true, "Should the player receive an unlock message when they reveal an item?");
        DOES_DISCOVER_SOUND = config.getBoolean("Discover Sound Toggle", category, true, "Should a sound effect play when the player reveals an item?");

        category = "Reveal Sound Options";
        config.addCustomCategoryComment(category, "Reveal Sound Options");
        DISCOVER_SOUND = config.getString("Reveal Sound", category, "entity.player.levelup", "The sound asset to play upon item reveal.");
        DISCOVER_PITCH = config.getFloat("Reveal Pitch", category, 1.0f, -100.0f, 100.0f, "Unlock sound pitch.");
        DISCOVER_VOLUME = config.getFloat("Reveal Volume", category, 0.5f, 0.0f, 10.0f, "Unlock sound volume. WARNING: 1.0f is pretty loud.");


        config.save();
    }

    public static void RegisterConfig(FMLPreInitializationEvent event) {
        FoundEnoughItems.config = new File(event.getModConfigurationDirectory() + "/");
        init(new File(FoundEnoughItems.config.getPath(), FoundEnoughItems.MODID + ".cfg"));
    }
}

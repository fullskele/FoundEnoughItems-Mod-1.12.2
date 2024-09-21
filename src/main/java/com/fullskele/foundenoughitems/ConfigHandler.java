package com.fullskele.foundenoughitems;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

public class ConfigHandler {
    public static Configuration config;

    //TODO: Allow this to be configured, and make it generate the lang as well?
    public static final int NUM_LISTS = 10;

    public static String[][] JEI_BLACKLIST = new String[NUM_LISTS][];

    public static boolean[] DOES_DISCOVER_MESSAGE = new boolean[NUM_LISTS];
    public static boolean[] DOES_DISCOVER_SOUND = new boolean[NUM_LISTS];
    public static boolean[] DOES_HIDE_RECIPES = new boolean[NUM_LISTS];
    public static boolean[] DOES_HIDE_IN_JEI = new boolean[NUM_LISTS];

    public static String[] DISCOVER_SOUND = new String[NUM_LISTS];
    public static float[] DISCOVER_PITCH = new float[NUM_LISTS];
    public static float[] DISCOVER_VOLUME = new float[NUM_LISTS];


    public static void init(File file) {

        config = new Configuration(file);

        String category;



        for (int i=0;i<NUM_LISTS;i++) {
            category = "Item Blacklist Options #" + i;
            config.addCustomCategoryComment(category, "Item Blacklist #" + i);
            if (i==0) {
                JEI_BLACKLIST[i] = config.getStringList("Items to Discover", category, new String[]{"minecraft:ender_eye"}, "Items to hide in JEI, along with their crafting recipes.");
            } else {
                JEI_BLACKLIST[i] = config.getStringList("Items to Discover", category, new String[]{""}, "Items to hide in JEI, along with their crafting recipes.");
            }
            DOES_HIDE_IN_JEI[i] = config.getBoolean("JEI Hide Toggle", category, true, "Should these items be hidden in JEI?");
            DOES_HIDE_RECIPES[i] = config.getBoolean("Recipe Hide Toggle", category, true, "Should the recipe of these items be hidden?");
            DOES_DISCOVER_MESSAGE[i] = config.getBoolean("Discover Message Toggle", category, true, "Should the player receive an unlock message when they reveal these items?");
            DOES_DISCOVER_SOUND[i] = config.getBoolean("Discover Sound Toggle", category, true, "Should a sound effect play when the player reveals these items?");
            DISCOVER_SOUND[i] = config.getString("Reveal Sound", category, "entity.player.levelup", "The sound asset to play upon these item reveals.");
            DISCOVER_PITCH[i] = config.getFloat("Reveal Pitch", category, 1.0f, -100.0f, 100.0f, "Unlock sound pitch.");
            DISCOVER_VOLUME[i] = config.getFloat("Reveal Volume", category, 0.5f, 0.0f, 10.0f, "Unlock sound volume. WARNING: 1.0f is pretty loud.");
        }



        config.save();
    }

    public static void RegisterConfig(FMLPreInitializationEvent event) {
        FoundEnoughItems.config = new File(event.getModConfigurationDirectory() + "/");
        init(new File(FoundEnoughItems.config.getPath(), FoundEnoughItems.MODID + ".cfg"));
    }
}

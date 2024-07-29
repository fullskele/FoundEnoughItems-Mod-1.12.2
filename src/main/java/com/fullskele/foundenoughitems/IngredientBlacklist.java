package com.fullskele.foundenoughitems;

import mezz.jei.api.*;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.*;
import java.util.*;

@JEIPlugin
@Mod.EventBusSubscriber(Side.CLIENT)
public class IngredientBlacklist implements IModPlugin {

    private static IIngredientRegistry ingredientRegistry;
    private static IIngredientHelper<ItemStack> ingredientHelper;

    private static IRecipeRegistry recipeRegistry;

    private static final Set<ItemStack> unlockedItems = new HashSet<>();
    private static final Set<Item> blockedItems = new HashSet<>();
    private static boolean shouldSave = false;

    public void register(IModRegistry registry) {
        ingredientRegistry = registry.getIngredientRegistry();
        ingredientHelper = ingredientRegistry.getIngredientHelper(VanillaTypes.ITEM);

    }

    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        recipeRegistry = jeiRuntime.getRecipeRegistry();
        parseConfigForItems();
    }

    //TODO: CraftTweaker support to add categories and specific recipes that didn't get caught
    //TODO: Additional blacklist categories with individual parameters, need to make a datatype that encapsulates + handles them all for checks
    private static void parseConfigForItems() {
        List<ItemStack> validItemStacks = new ArrayList<>();

        for (String itemString : ConfigHandler.JEI_BLACKLIST) {
            //Convert the string to ResourceLocation
            ResourceLocation itemResourceLocation = new ResourceLocation(itemString);

            //Get item from Forge registry
            Item item = ForgeRegistries.ITEMS.getValue(itemResourceLocation);
            IRecipe recipe = (ForgeRegistries.RECIPES.getValue(itemResourceLocation));
            if (recipe != null) {
                try {
                    IRecipeWrapper recipeWrapper = recipeRegistry.getRecipeWrapper(recipe, "minecraft.crafting");
                    if (recipeWrapper != null) {
                        recipeRegistry.hideRecipe(recipeWrapper);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing item: " + itemString);
                    e.printStackTrace();
                }
            }

            // Check if item from config list is valid
            if (item != null) {
                validItemStacks.add(item.getDefaultInstance());
                blockedItems.add(item);
            } else {
                System.err.println("Invalid item: " + itemString);
            }
        }

        ingredientRegistry.removeIngredientsAtRuntime(VanillaTypes.ITEM, validItemStacks);
    }




    //Override tooltip if the item is hidden
    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemTooltip(ItemTooltipEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack itemStack = event.getItemStack();

        if (player != null && !itemStack.isEmpty()) {
            Item item = itemStack.getItem();
            if (blockedItems.contains(item) && unlockedItems.stream().noneMatch(stack -> stack.getItem().equals(itemStack.getItem()))) {
                event.getToolTip().clear();
                event.getToolTip().add(TextFormatting.WHITE + I18n.format("tooltip.foundenoughitems.name"));
                event.getToolTip().add(TextFormatting.WHITE + I18n.format("tooltip.foundenoughitems.separator"));
                event.getToolTip().add(TextFormatting.WHITE + I18n.format("tooltip.foundenoughitems.description"));
            }
        }
    }

    /* TODO: Dynamic item rendering for hidden items
    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModelBakeEvent(ModelBakeEvent event) {
    }
    */

    @SubscribeEvent
    public void newItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (Minecraft.getMinecraft().world.isRemote) {
            Minecraft.getMinecraft().addScheduledTask(() -> revealItem(event.getStack(), event.player));
        }
    }

    @SubscribeEvent
    public void newItemCraft(PlayerEvent.ItemCraftedEvent event) {
        if (Minecraft.getMinecraft().world.isRemote) {
            Minecraft.getMinecraft().addScheduledTask(() -> revealItem(event.crafting.copy(), event.player));
        }
    }

    @SubscribeEvent
    public void onGameLoad(net.minecraftforge.event.entity.player.PlayerEvent.LoadFromFile event) {
        // Clear unlockedItems before loading from file
        Minecraft.getMinecraft().addScheduledTask(IngredientBlacklist::removeCollectedItems);

        try {
            File file = event.getPlayerFile(FoundEnoughItems.MODID);
            if (!file.exists()) {
                file.createNewFile();
                return;
            }

            NBTTagCompound compound;
            try (FileInputStream fis = new FileInputStream(file);
                 DataInputStream dis = new DataInputStream(fis)) {
                compound = CompressedStreamTools.readCompressed(dis);
            }

            // Schedule the removal and clear task first
            Minecraft.getMinecraft().addScheduledTask(() -> {
                removeCollectedItems();

                // After removeCollectedItems, schedule addBackCollectedItems
                Minecraft.getMinecraft().addScheduledTask(() -> {

                    NBTTagList itemList = compound.getTagList("unlockedItems", 10); // 10 is the NBT tag type for compounds
                    for (int i = 0; i < itemList.tagCount(); i++) {
                        NBTTagCompound itemTag = itemList.getCompoundTagAt(i);
                        ItemStack itemStack = new ItemStack(itemTag);
                        unlockedItems.add(itemStack);
                    }

                    restoreCollectedItems();
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onGameSave(net.minecraftforge.event.entity.player.PlayerEvent.SaveToFile event) {
        if (shouldSave) {
            try {
                File file = event.getPlayerFile(FoundEnoughItems.MODID);
                if (!file.exists()) {
                    file.createNewFile();
                }

                NBTTagCompound compound = new NBTTagCompound();
                NBTTagList itemList = new NBTTagList();
                for (ItemStack itemStack : unlockedItems) {
                    NBTTagCompound itemTag = new NBTTagCompound();
                    itemStack.writeToNBT(itemTag);
                    itemList.appendTag(itemTag);
                }
                compound.setTag("unlockedItems", itemList);

                try (FileOutputStream fos = new FileOutputStream(file);
                     DataOutputStream dos = new DataOutputStream(fos)) {
                    CompressedStreamTools.writeCompressed(compound, dos);
                }
                shouldSave = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }




    //Can add mod compat as long as an event/action has a related ItemStack and EntityPlayer
    public static void revealItem(ItemStack itemStack, EntityPlayer player) {
        Item item = itemStack.getItem();

        if (blockedItems.contains(item) && unlockedItems.stream().noneMatch(stack -> stack.getItem().equals(itemStack.getItem()))) {

            // Send the player an unlock message
            if (ConfigHandler.DOES_DISCOVER_MESSAGE)
                player.sendMessage(new TextComponentString(I18n.format("message.foundenoughitems.pre") + " " + itemStack.getDisplayName() + I18n.format("message.foundenoughitems.post")));

            if (ConfigHandler.DOES_DISCOVER_SOUND) {
                ResourceLocation soundResourceLocation = new ResourceLocation(ConfigHandler.DISCOVER_SOUND);
                player.world.playSound(null, player.getPosition(), SoundEvent.REGISTRY.getObject(soundResourceLocation), SoundCategory.PLAYERS, ConfigHandler.DISCOVER_VOLUME, ConfigHandler.DISCOVER_PITCH);
            }
            // Add during runtime (real time)
            ingredientRegistry.addIngredientsAtRuntime(VanillaTypes.ITEM, Collections.singleton(itemStack));
            unlockedItems.add(itemStack);
            shouldSave = true;

            IRecipe recipe = (ForgeRegistries.RECIPES.getValue(itemStack.getItem().getRegistryName()));
            if (recipe != null) {
                try {
                    IRecipeWrapper recipeWrapper = recipeRegistry.getRecipeWrapper(recipe, "minecraft.crafting");
                    if (recipeWrapper != null) {
                        recipeRegistry.unhideRecipe(recipeWrapper);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing stack: " + itemStack);
                    e.printStackTrace();
                }
            }
        }
    }

    private static void removeCollectedItems() {
        if (!unlockedItems.isEmpty()) {
            ingredientRegistry.removeIngredientsAtRuntime(VanillaTypes.ITEM, unlockedItems);

            for (ItemStack i : unlockedItems) {
                ResourceLocation itemResourceLocation = new ResourceLocation(ingredientHelper.getResourceId(i));
                IRecipe recipe = (ForgeRegistries.RECIPES.getValue(itemResourceLocation));
                if (recipe != null) {
                    try {
                        IRecipeWrapper recipeWrapper = recipeRegistry.getRecipeWrapper(recipe, "minecraft.crafting");
                        if (recipeWrapper != null) {
                            recipeRegistry.hideRecipe(recipeWrapper);
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing item: " + i);
                        e.printStackTrace();
                    }
                }
            }
            unlockedItems.clear();
        }
    }

    private static void restoreCollectedItems() {
        if (!unlockedItems.isEmpty()) {
            ingredientRegistry.addIngredientsAtRuntime(VanillaTypes.ITEM, unlockedItems);

            for (ItemStack i : unlockedItems) {
                ResourceLocation itemResourceLocation = new ResourceLocation(ingredientHelper.getResourceId(i));
                IRecipe recipe = (ForgeRegistries.RECIPES.getValue(itemResourceLocation));
                if (recipe != null) {
                    try {
                        IRecipeWrapper recipeWrapper = recipeRegistry.getRecipeWrapper(recipe, "minecraft.crafting");
                        if (recipeWrapper != null) {
                            recipeRegistry.unhideRecipe(recipeWrapper);

                        }
                    } catch (Exception e) {
                        System.err.println("Error processing item: " + i);
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

package com.fullskele.foundenoughitems;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.fullskele.foundenoughitems.ConfigHandler.NUM_LISTS;

public class BlacklistManager {

    public List<Set<Item>[]> setsOfBlacklist;

    public BlacklistManager() {
        // Initialize sets with empty Set<Item>[] arrays
        setsOfBlacklist = new ArrayList<>();
        for (int i = 0; i <= NUM_LISTS; i++) {
            setsOfBlacklist.add(new Set[]{new HashSet<Item>()});
        }

    }

    // Return index of list a blacklisted item belongs to, -1 if no match
    public int getListNum(Item item) {
        for (int i = 0; i < setsOfBlacklist.size(); i++) {
            if (setsOfBlacklist.get(i)[0].contains(item)) {
                return i;
            }
        }
        return -1;
    }

    public String getNameLang(int index) {
        return I18n.format("tooltip.foundenoughitems.name" + index);
    }

    public String getSeparatorLang(int index) {
        return I18n.format("tooltip.foundenoughitems.separator" + index);
    }

    public String getTooltipLang(int index) {
        return I18n.format("tooltip.foundenoughitems.tooltip" + index);
    }

    public String getRevealMessage(int index, ItemStack itemStack) {
        return I18n.format("message.foundenoughitems.found"+index, itemStack.getDisplayName());
    }

    // Add an item to the blacklist for the specified set index
    public void addItemToBlacklist(int index, Item item) {
        setsOfBlacklist.get(index)[0].add(item);
    }
}

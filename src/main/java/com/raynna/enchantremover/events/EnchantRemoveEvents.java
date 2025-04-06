package com.raynna.enchantremover.events;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EnchantRemoveEvents {

    @SubscribeEvent
    public static void onAnvilTake(AnvilRepairEvent event) {
        System.out.println("RepairEvent");
        if (event.getEntity().getPersistentData().contains("enchant_removed")) {
            CompoundTag newItemTag = event.getEntity().getPersistentData().getCompound("enchant_removed");
            event.getEntity().getPersistentData().remove("enchant_removed");
            if (event.getEntity().getServer() == null) {
                return;
            }
            HolderLookup.Provider provider = event.getEntity().getServer().registryAccess();
            Optional<ItemStack> optionalItem = ItemStack.parse(provider, newItemTag);
            optionalItem.ifPresent(item -> {
                event.getEntity().getInventory().placeItemBackInInventory(item);
                System.out.println(item.getHoverName().getString());
            });
        }
    }

    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.isEmpty() || !right.is(Items.BOOK) || !left.isEnchanted()) {
            return;
        }

        ItemEnchantments originalEnchants = left.getTagEnchantments();
        if (originalEnchants.isEmpty()) {
            return;
        }

        Holder<Enchantment> firstEnchantHolder = null;
        int firstLevel = 0;

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : originalEnchants.entrySet()) {
            firstEnchantHolder = entry.getKey();
            firstLevel = entry.getIntValue();
            break;
        }

        if (firstEnchantHolder == null) {
            return;
        }

        EnchantmentInstance instance = new EnchantmentInstance(firstEnchantHolder, firstLevel);
        ItemStack enchantedBook = EnchantedBookItem.createForEnchantment(instance);

        Map<Holder<Enchantment>, Integer> mutableEnchants = new HashMap<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : originalEnchants.entrySet()) {
            if (!entry.getKey().equals(firstEnchantHolder)) {
                mutableEnchants.put(entry.getKey(), entry.getIntValue());
            }
        }

        ItemStack newItem = left.copy();
        EnchantmentHelper.setEnchantments(newItem, ItemEnchantments.EMPTY);
        EnchantmentHelper.updateEnchantments(newItem, mutable -> {
            for (Map.Entry<Holder<Enchantment>, Integer> entry : mutableEnchants.entrySet()) {
                mutable.set(entry.getKey(), entry.getValue());
            }
        });
        if (event.getPlayer().getServer() == null) {
            return;
        }
        HolderLookup.Provider provider = event.getPlayer().getServer().registryAccess();
        CompoundTag compoundTag = new CompoundTag();
        event.getPlayer().getPersistentData().put("enchant_removed", newItem.save(provider, compoundTag));

        event.setOutput(enchantedBook);
        event.setCost(1);
        event.setMaterialCost(1);
    }

    public static void register() {
        NeoForge.EVENT_BUS.register(EnchantRemoveEvents.class);
    }
}

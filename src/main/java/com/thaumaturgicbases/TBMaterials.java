package com.thaumaturgicbases;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.trim.TrimMaterial;

/** Tool and armour materials. TC4 values: thauminite tools (3, 974, 7F, 2.8F, 15), armour (27, {3, 8, 6, 3}, 17). */
public final class TBMaterials {
    public static final TagKey<Item> INGOTS_THAUMINITE = TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "ingots/thauminite"));

    public static final ToolMaterial TOOL_THAUMINITE = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 974, 7.0F, 2.8F, 15, INGOTS_THAUMINITE);

    public static final ResourceKey<EquipmentAsset> ASSET_THAUMINITE = ResourceKey.create(EquipmentAssets.ROOT_ID, ThaumaturgicBases.id("thauminite"));
    public static final ArmorMaterial ARMOR_THAUMINITE = new ArmorMaterial(27,
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 3),
            17, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F, INGOTS_THAUMINITE, ASSET_THAUMINITE);

    public static final ResourceKey<TrimMaterial> TRIM_THAUMINITE = ResourceKey.create(Registries.TRIM_MATERIAL, ThaumaturgicBases.id("thauminite"));

    private TBMaterials() {
    }
}

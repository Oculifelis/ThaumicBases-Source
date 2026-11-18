package com.thaumaturgicbases;

import com.thaumaturgicbases.block.SpikeBlock;
import com.thaumaturgicbases.item.VisGearArmorItem;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/** TB.BloodyMess: spikes, bloody cloth and the bloody robes. */
public final class TBBloodyMess {
    /** Wiping a bloody spike with one of these gives bloody cloth (TB: Thaumcraft enchanted fabric). */
    public static final TagKey<Item> CLEANS_SPIKES = TagKey.create(Registries.ITEM, ThaumaturgicBases.id("cleans_spikes"));
    public static final TagKey<Item> REPAIRS_BLOODY_ROBES = TagKey.create(Registries.ITEM, ThaumaturgicBases.id("repairs_bloody_robes"));

    public static final DeferredBlock<SpikeBlock> IRON_SPIKE = TBRegistry.block("iron_spike", SpikeBlock::new, TBBloodyMess::spike);
    public static final DeferredBlock<SpikeBlock> THAUMIC_SPIKE = TBRegistry.block("thaumic_spike", SpikeBlock::new, TBBloodyMess::spike);
    public static final DeferredBlock<SpikeBlock> VOID_SPIKE = TBRegistry.block("void_spike", SpikeBlock::new, TBBloodyMess::spike);

    public static final DeferredItem<Item> BLOODY_CLOTH = TBRegistry.item("bloody_cloth", Item::new);

    // TB bloodyA: ("TBBLOODY", 21, {2, 6, 5, 2}, 21)
    public static final ResourceKey<EquipmentAsset> ASSET_BLOODY = ResourceKey.create(EquipmentAssets.ROOT_ID, ThaumaturgicBases.id("bloody"));
    public static final ArmorMaterial ARMOR_BLOODY = new ArmorMaterial(21,
            Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 5, ArmorType.CHESTPLATE, 6, ArmorType.HELMET, 2),
            21, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, REPAIRS_BLOODY_ROBES, ASSET_BLOODY);

    public static final DeferredItem<VisGearArmorItem> BLOODY_CHEST = TBRegistry.item("bloody_chestplate", VisGearArmorItem::new,
            p -> robe(p, ArmorType.CHESTPLATE, "bloody_chestplate_health", Attributes.MAX_HEALTH, 0.2, EquipmentSlotGroup.CHEST));
    public static final DeferredItem<VisGearArmorItem> BLOODY_LEGS = TBRegistry.item("bloody_leggings", VisGearArmorItem::new,
            p -> robe(p, ArmorType.LEGGINGS, "bloody_leggings_knockback", Attributes.KNOCKBACK_RESISTANCE, 0.5, EquipmentSlotGroup.LEGS));
    public static final DeferredItem<VisGearArmorItem> BLOODY_BOOTS = TBRegistry.item("bloody_boots", VisGearArmorItem::new,
            p -> robe(p, ArmorType.BOOTS, "bloody_boots_speed", Attributes.MOVEMENT_SPEED, 0.1, EquipmentSlotGroup.FEET));

    private TBBloodyMess() {
    }

    private static BlockBehaviour.Properties spike(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();
    }

    /** TB ItemBloodyArmor: each piece multiplies one attribute (operation 2 there, ADD_MULTIPLIED_TOTAL here). */
    private static Item.Properties robe(Item.Properties p, ArmorType type, String id,
                                        net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                        double amount, EquipmentSlotGroup slot) {
        return p.humanoidArmor(ARMOR_BLOODY, type).rarity(Rarity.UNCOMMON).attributes(ARMOR_BLOODY.createAttributes(type)
                .withModifierAdded(attribute, new AttributeModifier(ThaumaturgicBases.id(id), amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), slot));
    }

    static void init() {
    }
}

package com.thaumaturgicbases;

import com.leclowndu93150.thaumaturge.content.equipment.GearWarp;
import com.thaumaturgicbases.item.ScytheItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/** TB's void flint and steel, void shears and Herobrine's Scythe, plus the self-repair they share. */
public final class TBTools {
    public static final DeferredItem<FlintAndSteelItem> VOID_FLINT_AND_STEEL = TBRegistry.item("void_flint_and_steel", FlintAndSteelItem::new,
            p -> GearWarp.with(p.durability(64).rarity(Rarity.UNCOMMON), 1));
    public static final DeferredItem<ShearsItem> VOID_SHEARS = TBRegistry.item("void_shears", ShearsItem::new,
            p -> GearWarp.with(p.durability(238).rarity(Rarity.UNCOMMON).component(DataComponents.TOOL, ShearsItem.createToolProperties()), 1));
    // TB: ToolMaterial.EMERALD with the modifiers replaced: +14.5 attack damage, +50% move speed in hand.
    public static final DeferredItem<ScytheItem> HEROBRINES_SCYTHE = TBRegistry.item("herobrines_scythe", ScytheItem::new,
            p -> GearWarp.with(p.sword(ToolMaterial.DIAMOND, 3.0F, -2.4F).rarity(Rarity.UNCOMMON).attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 14.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(ThaumaturgicBases.id("herobrines_scythe_speed"), 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND)
                    .build()), 3));

    private TBTools() {
    }

    /** Items in the {@code thaumaturgicbases:self_repair} data map mend one point every that many ticks anywhere in a player's inventory. */
    static void init() {
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, event -> {
            Player player = event.getEntity();
            if (player.level().isClientSide()) return;
            for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty() || !stack.isDamaged()) continue;
                Integer interval = stack.typeHolder().getData(TBDataMaps.SELF_REPAIR);
                if (interval != null && interval > 0 && player.tickCount % interval == 0) stack.setDamageValue(stack.getDamageValue() - 1);
            }
        });
    }
}

package com.thaumaturgicbases.item;

import com.leclowndu93150.thaumaturge.api.items.IVisDiscountGear;
import com.thaumaturgicbases.TBDataMaps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Armour piece whose vis discount comes from the thaumaturgicbases:vis_discount data map; Thaumaturge turns it into an attribute. */
public class VisGearArmorItem extends Item implements IVisDiscountGear {
    public VisGearArmorItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getVisDiscount(ItemStack stack) {
        return TBDataMaps.visDiscount(stack);
    }
}

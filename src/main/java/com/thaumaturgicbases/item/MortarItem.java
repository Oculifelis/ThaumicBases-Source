package com.thaumaturgicbases.item;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

/** TB ItemMortarAndPestle: stays in the crafting grid one point more worn, until it breaks. */
public class MortarItem extends Item {
    public MortarItem(Properties props) {
        super(props);
    }

    @Override
    public @Nullable ItemStackTemplate getCraftingRemainder(ItemInstance stack) {
        int damage = stack.getOrDefault(DataComponents.DAMAGE, 0) + 1;
        if (damage >= stack.getOrDefault(DataComponents.MAX_DAMAGE, 0)) return null;
        return new ItemStackTemplate(this, DataComponentPatch.builder().set(DataComponents.DAMAGE, damage).build());
    }
}

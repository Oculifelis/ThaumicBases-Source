package com.thaumaturgicbases.node;

import com.thaumaturgicbases.TBComponents;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** TB ItemNodeFoci: one item, its kind the {@code thaumaturgicbases:node_focus} component (an id in the node_focus registry). */
public class NodeFocusItem extends Item {
    public NodeFocusItem(Item.Properties props) {
        super(props);
    }

    public static ItemStack of(Item item, Identifier focus) {
        ItemStack stack = new ItemStack(item);
        stack.set(TBComponents.NODE_FOCUS.get(), focus);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier focus = stack.get(TBComponents.NODE_FOCUS.get());
        return Component.translatable(focus == null ? getDescriptionId() : "item." + focus.getNamespace() + ".node_focus." + focus.getPath());
    }

    /** Every focus the loaded datapacks define, for the creative tab. */
    public static List<ItemStack> all(Item item, HolderLookup.Provider registries) {
        return registries.lookup(NodeFocus.REGISTRY_KEY).map(r -> r.listElementIds()
                .map(k -> of(item, k.identifier())).sorted((a, b) -> a.get(TBComponents.NODE_FOCUS.get()).compareTo(b.get(TBComponents.NODE_FOCUS.get())))
                .toList()).orElse(List.of());
    }
}

package com.thaumaturgicbases;

import com.thaumaturgicbases.item.MortarItem;
import com.thaumaturgicbases.item.SmokingPipeItem;
import com.thaumaturgicbases.item.TobaccoEffect;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * TB.Tobacco. TB had one tobacco item with nine metadata kinds; here each kind is its own item, and what smoking it
 * does is the {@code thaumaturgicbases:tobacco} data map. The crop and seeds live in {@link TBFlora}.
 */
public final class TBTobacco {
    public static final List<String> KINDS = List.of("pile", "eldritch", "fighting", "hunger", "knowledge", "mining", "sanity", "tainted", "wispy");

    public static final DeferredItem<Item> TOBACCO_LEAVES = TBRegistry.item("tobacco_leaves", Item::new);
    public static final Map<String, DeferredItem<Item>> TOBACCO = new LinkedHashMap<>();

    static {
        for (String kind : KINDS) TOBACCO.put(kind, TBRegistry.item("tobacco_" + kind, Item::new));
    }

    public static final DeferredItem<MortarItem> MORTAR = TBRegistry.item("mortar_and_pestle", MortarItem::new, p -> p.durability(256));
    public static final DeferredItem<SmokingPipeItem> GREATWOOD_PIPE = TBRegistry.item("greatwood_pipe",
            p -> new SmokingPipeItem(p, TobaccoEffect.Pipe.GREATWOOD), p -> p.stacksTo(1));
    public static final DeferredItem<SmokingPipeItem> SILVERWOOD_PIPE = TBRegistry.item("silverwood_pipe",
            p -> new SmokingPipeItem(p, TobaccoEffect.Pipe.SILVERWOOD), p -> p.stacksTo(1));

    private TBTobacco() {
    }

    static void init() {
    }
}

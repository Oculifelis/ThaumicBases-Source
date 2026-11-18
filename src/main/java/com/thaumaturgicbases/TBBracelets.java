package com.thaumaturgicbases;

import com.leclowndu93150.thaumaturge.api.wands.IWandRodOnUpdate;
import com.leclowndu93150.thaumaturge.api.wands.WandCap;
import com.leclowndu93150.thaumaturge.api.wands.WandRod;
import com.leclowndu93150.thaumaturge.registry.TTWandParts;
import com.thaumaturgicbases.item.CastingBraceletItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * TB's Casting Bracelets (ItemCastingBracelet, 13 metadata values) as one item whose kind is its
 * {@code thaumaturge:wand_parts} component. Each kind registers its own cap and rod into Thaumaturge's part registries:
 * the rod holds the bracelet's vis (TB's capacities) and borrows the matching wand rod's regeneration; the cap is the
 * matching wand cap with TB's 1/1.5 vis discount on top. No cap or rod item exists, so Thaumaturge makes no wand
 * assembly recipe from them; the bracelets are crafted whole (tools/tbgen/phase6.py).
 */
public final class TBBracelets {
    /** kind -> (bracelet capacity in vis, wand cap it copies, wand rod whose regeneration it keeps). */
    public record Kind(String name, int capacity, Supplier<WandCap> cap, Supplier<WandRod> rod) {
    }

    public static final List<Kind> KINDS = List.of(
            new Kind("iron", 10, TTWandParts.CAP_IRON, TTWandParts.ROD_WOOD),
            new Kind("gold", 15, TTWandParts.CAP_GOLD, TTWandParts.ROD_WOOD),
            new Kind("greatwood", 17, TTWandParts.CAP_GOLD, TTWandParts.ROD_GREATWOOD),
            new Kind("thaumium", 20, TBWandParts.CAP_THAUMINITE, TBWandParts.ROD_THAUMIUM),
            new Kind("silverwood", 23, TTWandParts.CAP_THAUMIUM, TTWandParts.ROD_SILVERWOOD),
            new Kind("reed", 23, TTWandParts.CAP_THAUMIUM, TTWandParts.ROD_REED),
            new Kind("bone", 25, TTWandParts.CAP_THAUMIUM, TTWandParts.ROD_BONE),
            new Kind("obsidian", 25, TTWandParts.CAP_THAUMIUM, TTWandParts.ROD_OBSIDIAN),
            new Kind("ice", 25, TTWandParts.CAP_THAUMIUM, TTWandParts.ROD_ICE),
            new Kind("blaze", 25, TTWandParts.CAP_THAUMIUM, TTWandParts.ROD_BLAZE),
            new Kind("quartz", 25, TTWandParts.CAP_THAUMIUM, TTWandParts.ROD_QUARTZ),
            new Kind("void", 27, TTWandParts.CAP_THAUMIUM, TBWandParts.ROD_VOID),
            new Kind("primal", 30, TTWandParts.CAP_VOID, TTWandParts.STAFF_PRIMAL));

    /** TB divided every cap's vis cost by 1.5 on a bracelet. */
    private static final float DISCOUNT = 1.5F;

    public static final Map<String, DeferredHolder<WandCap, WandCap>> CAPS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<WandRod, WandRod>> RODS = new LinkedHashMap<>();

    static {
        for (Kind k : KINDS) {
            CAPS.put(k.name(), TBWandParts.CAPS.register("bracelet_" + k.name(), () -> {
                WandCap base = k.cap().get();
                return new WandCap(base.baseCostModifier() / DISCOUNT, base.specialCostAspects(), base.specialCostModifier() / DISCOUNT,
                        base.craftCost(), base.texture());
            }));
            IWandRodOnUpdate regen = (stack, player) -> {
                IWandRodOnUpdate inner = k.rod().get().onUpdate();
                if (inner != null) inner.onUpdate(stack, player);
            };
            // capacity is in vis; Thaumaturge stores rod.capacity * 100 centivis (no sceptre), as TB stored vis * 100.
            RODS.put(k.name(), TBWandParts.RODS.register("bracelet_" + k.name(),
                    () -> new WandRod(k.capacity(), 0, k.rod().get().texture(), regen, false, false, false)));
        }
    }

    public static final DeferredItem<CastingBraceletItem> CASTING_BRACELET = TBRegistry.ITEMS.registerItem("casting_bracelet",
            CastingBraceletItem::new, p -> p.stacksTo(1));

    static {
        TBRegistry.TAB_STACKS.add(r -> all());
    }

    private TBBracelets() {
    }

    /** The bracelet kind a stack holds, or null for a bracelet without TB parts. */
    public static String kindOf(WandRod rod) {
        for (Map.Entry<String, DeferredHolder<WandRod, WandRod>> e : RODS.entrySet()) {
            if (e.getValue().isBound() && e.getValue().get() == rod) return e.getKey();
        }
        return null;
    }

    /** One bracelet of each kind, for the creative tab. */
    public static List<ItemStack> all() {
        return KINDS.stream().map(k -> CastingBraceletItem.of(CASTING_BRACELET.get(), CAPS.get(k.name()).get(), RODS.get(k.name()).get())).toList();
    }

    static void init() {
    }
}

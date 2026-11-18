package com.thaumaturgicbases;

import com.leclowndu93150.thaumaturge.api.wands.WandCap;
import com.leclowndu93150.thaumaturge.api.wands.WandRod;
import com.leclowndu93150.thaumaturge.content.wands.WandRodPrimalOnUpdate;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Wand caps and rods, registered into Thaumaturge's own part registries. Thaumaturge derives every cap x rod
 * assembly recipe from these registries, so no wand recipes are written here.
 */
public final class TBWandParts {
    public static final DeferredRegister<WandCap> CAPS = DeferredRegister.create(WandCap.REGISTRY_KEY, ThaumaturgicBases.MODID);
    public static final DeferredRegister<WandRod> RODS = DeferredRegister.create(WandRod.REGISTRY_KEY, ThaumaturgicBases.MODID);

    // TB: WandCap("thauminite", 0.85F, ..., 6). Slightly cheaper than thaumium (0.9), same crafting cost.
    public static final DeferredHolder<WandCap, WandCap> CAP_THAUMINITE = CAPS.register("thauminite",
            () -> new WandCap(0.85F, List.of(), 0.0F, 6, ThaumaturgicBases.id("textures/models/wand_cap_thauminite.png")));

    // TB: WandRod("tbthaumium", 80, ..., 6, new WandRodPrimalOnUpdate()). Slowly regains every primal.
    public static final DeferredHolder<WandRod, WandRod> ROD_THAUMIUM = RODS.register("thaumium",
            () -> new WandRod(80, 6, ThaumaturgicBases.id("textures/models/wand_rod_thaumium.png"), new WandRodPrimalOnUpdate(),
                    false, false, false, ThaumaturgicBases.id("rod_thaumium")));

    // TB: WandRod("tbvoid", 160, ..., 16, new WandRodPrimalOnUpdate()).
    public static final DeferredHolder<WandRod, WandRod> ROD_VOID = RODS.register("void",
            () -> new WandRod(160, 16, ThaumaturgicBases.id("textures/models/wand_rod_void.png"), new WandRodPrimalOnUpdate(),
                    false, false, false, ThaumaturgicBases.id("rod_void")));

    private TBWandParts() {
    }

    static void register(IEventBus bus) {
        CAPS.register(bus);
        RODS.register(bus);
    }
}

package com.thaumaturgicbases.spell;

import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPartKind;
import com.mojang.serialization.MapCodec;
import com.thaumaturgicbases.ThaumaturgicBases;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TB's four wand foci (Drain, Experience, Flux Scrubber, Activation) as Thaumaturge spell effects. Each focus is one
 * behaviour type; TB's rank-5 upgrades (Aquatic, Netheric, Decomposing, Vaporizing, Warping, Calming,
 * Crystallisation) are further parts of the same type with other JSON fields, written by tools/tbgen/phase6.py into
 * {@code data/thaumaturgicbases/thaumaturge/spell_part/}. Potency and Frugal are Thaumaturge's own modifiers.
 */
public final class TBSpells {
    public static final DeferredRegister<SpellBehaviorType<?>> BEHAVIORS = DeferredRegister.create(SpellBehaviorType.REGISTRY_KEY, ThaumaturgicBases.MODID);

    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<DrainEffect>> DRAIN = effect("drain", DrainEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<ExperienceEffect>> EXPERIENCE = effect("experience", ExperienceEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<FluxScrubEffect>> FLUX_SCRUB = effect("flux_scrub", FluxScrubEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<ActivationEffect>> ACTIVATION = effect("activation", ActivationEffect.CODEC);

    private TBSpells() {
    }

    private static <B extends SpellBehavior> DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<B>> effect(String name, MapCodec<B> codec) {
        return BEHAVIORS.register(name, () -> new SpellBehaviorType<>(SpellPartKind.EFFECT, codec));
    }

    public static void register(IEventBus bus) {
        BEHAVIORS.register(bus);
    }
}

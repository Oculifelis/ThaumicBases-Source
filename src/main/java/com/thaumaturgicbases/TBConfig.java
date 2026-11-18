package com.thaumaturgicbases;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.util.StringRepresentable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * TB's enable* switches (TBConfig). Blocks and items always register (a world must not lose them when a switch
 * changes); a switched-off feature loses its recipes through the {@code thaumaturgicbases:feature} recipe condition.
 * TB's enchantment/foci IDs are gone (registries), and the furnace numbers live in data maps.
 */
public final class TBConfig {
    public enum Feature implements StringRepresentable {
        FLORA("Thaumaturgic Bases plants, saplings and trees"),
        TOBACCO("tobacco and pipes"),
        BLOODY_MESS("spikes and bloody robes"),
        BRACELETS("casting bracelets"),
        NODE_TOOLS("node manipulator, node foci and node linker"),
        REVOLVER("the revolver and its upgrades");

        public static final Codec<Feature> CODEC = StringRepresentable.fromEnum(Feature::values);
        private final String what;

        Feature(String what) {
            this.what = what;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final Map<Feature, BooleanSupplier> ENABLED = new java.util.EnumMap<>(Feature.class);
    static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Switch off a Thaumaturgic Bases feature: its recipes disappear, its blocks and items stay registered.").push("features");
        for (Feature f : Feature.values()) {
            ModConfigSpec.BooleanValue v = b.comment("Enable " + f.what).define("enable_" + f.getSerializedName(), true);
            ENABLED.put(f, v::getAsBoolean);
        }
        b.pop();
        SPEC = b.build();
    }

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, ThaumaturgicBases.MODID);

    /** {@code {"type": "thaumaturgicbases:feature", "feature": "flora"}} */
    public record FeatureCondition(Feature feature) implements ICondition {
        public static final MapCodec<FeatureCondition> CODEC = Feature.CODEC.fieldOf("feature").xmap(FeatureCondition::new, FeatureCondition::feature);

        @Override
        public boolean test(IContext context) {
            return enabled(feature);
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

    static {
        CONDITIONS.register("feature", () -> FeatureCondition.CODEC);
    }

    private TBConfig() {
    }

    public static boolean enabled(Feature feature) {
        return ENABLED.get(feature).getAsBoolean();
    }

    static void register(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.STARTUP, SPEC);
        CONDITIONS.register(bus);
    }
}

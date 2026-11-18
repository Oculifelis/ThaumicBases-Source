package com.thaumaturgicbases.node;

import com.leclowndu93150.thaumaturge.api.nodes.NodeModifier;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicbases.ThaumaturgicBases;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

/**
 * One Node Manipulator focus (TB TileNodeManipulator's ten effects), loaded from
 * {@code data/<ns>/thaumaturgicbases/node_focus/<id>.json}. The manipulator runs the first {@link Step} that matches the
 * node below it; when that step has run {@code ticks} ticks the node changes. {@code orb_interval} lets an aspect
 * orb escape every so many ticks while a step runs (Destruction); {@code refund_chance} gives back drained vis
 * (Efficiency); {@code regen_chance} refills every aspect below its base by one point each work tick (Speed).
 */
public record NodeFocus(int color, List<Step> steps, int orbInterval, float refundChance, float regenChance) {
    public static final ResourceKey<Registry<NodeFocus>> REGISTRY_KEY = ResourceKey.createRegistryKey(ThaumaturgicBases.id("node_focus"));

    /** A node modifier, with "none" for none and "destroy" (as a result) for removing the node. */
    public static final String NONE = "none";
    public static final String DESTROY = "destroy";

    /**
     * Runs while the node's type is {@code type} (any when absent) and its modifier is one of {@code modifiers} (any when
     * absent); after {@code ticks} it sets {@code set_type} and/or {@code set_modifier}.
     */
    public record Step(Optional<NodeType> type, Optional<List<String>> modifiers, Optional<NodeType> setType,
                       Optional<String> setModifier, int ticks) {
        public static final Codec<Step> CODEC = RecordCodecBuilder.create(i -> i.group(
                NodeType.CODEC.optionalFieldOf("type").forGetter(Step::type),
                Codec.STRING.listOf().optionalFieldOf("modifiers").forGetter(Step::modifiers),
                NodeType.CODEC.optionalFieldOf("set_type").forGetter(Step::setType),
                Codec.STRING.optionalFieldOf("set_modifier").forGetter(Step::setModifier),
                Codec.INT.fieldOf("ticks").forGetter(Step::ticks)).apply(i, Step::new));

        public boolean matches(NodeType nodeType, @Nullable NodeModifier modifier) {
            return type.map(t -> t == nodeType).orElse(true) && modifiers.map(m -> m.contains(name(modifier))).orElse(true);
        }
    }

    public static final Codec<NodeFocus> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(NodeFocus::color),
            Step.CODEC.listOf().optionalFieldOf("steps", List.of()).forGetter(NodeFocus::steps),
            Codec.INT.optionalFieldOf("orb_interval", 0).forGetter(NodeFocus::orbInterval),
            Codec.FLOAT.optionalFieldOf("refund_chance", 0.0F).forGetter(NodeFocus::refundChance),
            Codec.FLOAT.optionalFieldOf("regen_chance", 0.0F).forGetter(NodeFocus::regenChance)).apply(i, NodeFocus::new));

    public static String name(@Nullable NodeModifier modifier) {
        return modifier == null ? NONE : modifier.name().toLowerCase(Locale.ROOT);
    }

    public static @Nullable NodeModifier modifier(String name) {
        return NONE.equals(name) ? null : NodeModifier.valueOf(name.toUpperCase(Locale.ROOT));
    }

    /** The first step that applies to a node of this type and modifier, or -1. */
    public int stepFor(NodeType type, @Nullable NodeModifier modifier) {
        for (int s = 0; s < steps.size(); s++) {
            if (steps.get(s).matches(type, modifier)) return s;
        }
        return -1;
    }
}

package com.thaumaturgicbases.item;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeType;
import com.leclowndu93150.thaumaturge.api.research.scan.ScanningManager;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.content.entity.WispEntity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

/**
 * What smoking one tobacco does (TB TBTobacco.performTobaccoEffect), as the {@code thaumaturgicbases:tobacco} item data map.
 * Every outcome whose pipe matches rolls its chance and then applies all of its parts.
 */
public record TobaccoEffect(List<Outcome> outcomes) {
    // Declared before CODEC: building CODEC initialises Outcome, which reads this field.
    static final Codec<WarpType> WARP_TYPE = Codec.STRING.xmap(s -> WarpType.valueOf(s.toUpperCase(Locale.ROOT)), t -> t.name().toLowerCase(Locale.ROOT));

    public static final Codec<TobaccoEffect> CODEC = Outcome.CODEC.listOf().xmap(TobaccoEffect::new, TobaccoEffect::outcomes);

    public enum Pipe implements StringRepresentable {
        ANY, GREATWOOD, SILVERWOOD;

        public static final Codec<Pipe> CODEC = StringRepresentable.fromEnum(Pipe::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Effect(Holder<MobEffect> id, int duration, int amplifier) {
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("id").forGetter(Effect::id),
                Codec.INT.fieldOf("duration").forGetter(Effect::duration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(Effect::amplifier)
        ).apply(i, Effect::new));
    }

    /** {@code rolls} times: one random category gets {@code amount} observation; landing on {@code warp_category} also gives temporary warp. */
    public record Knowledge(int rolls, int amount, List<Identifier> categories, Optional<Identifier> warpCategory, int warp) {
        public static final Codec<Knowledge> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("rolls").forGetter(Knowledge::rolls),
                Codec.INT.optionalFieldOf("amount", 1).forGetter(Knowledge::amount),
                Identifier.CODEC.listOf().fieldOf("categories").forGetter(Knowledge::categories),
                Identifier.CODEC.optionalFieldOf("warp_category").forGetter(Knowledge::warpCategory),
                Codec.INT.optionalFieldOf("warp", 0).forGetter(Knowledge::warp)
        ).apply(i, Knowledge::new));
    }

    public record Outcome(Pipe pipe, float chance, Optional<Effect> effect, int warp, int warpMax, WarpType warpType,
                          int nutrition, float saturation, Optional<Knowledge> knowledge,
                          Optional<Holder<EntityType<?>>> spawn, Optional<ResourceKey<IAspect>> spawnAvoids, boolean breakPipe) {
        public static final Codec<Outcome> CODEC = RecordCodecBuilder.create(i -> i.group(
                Pipe.CODEC.optionalFieldOf("pipe", Pipe.ANY).forGetter(Outcome::pipe),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Outcome::chance),
                Effect.CODEC.optionalFieldOf("effect").forGetter(Outcome::effect),
                Codec.INT.optionalFieldOf("warp", 0).forGetter(Outcome::warp),
                Codec.INT.optionalFieldOf("warp_max", 0).forGetter(Outcome::warpMax),
                WARP_TYPE.optionalFieldOf("warp_type", WarpType.TEMPORARY).forGetter(Outcome::warpType),
                Codec.INT.optionalFieldOf("nutrition", 0).forGetter(Outcome::nutrition),
                Codec.FLOAT.optionalFieldOf("saturation", 0.0F).forGetter(Outcome::saturation),
                Knowledge.CODEC.optionalFieldOf("knowledge").forGetter(Outcome::knowledge),
                BuiltInRegistries.ENTITY_TYPE.holderByNameCodec().optionalFieldOf("spawn").forGetter(Outcome::spawn),
                ResourceKey.codec(IAspect.REGISTRY_KEY).optionalFieldOf("spawn_avoids_aspect").forGetter(Outcome::spawnAvoids),
                Codec.BOOL.optionalFieldOf("break_pipe", false).forGetter(Outcome::breakPipe)
        ).apply(i, Outcome::new));

        /** Returns true when the pipe breaks. */
        boolean apply(ServerPlayer smoker) {
            ServerLevel level = smoker.level();
            RandomSource rand = smoker.getRandom();
            effect.ifPresent(e -> smoker.addEffect(new MobEffectInstance(e.id(), e.duration(), e.amplifier(), true, true)));
            if (warp != 0) {
                int amount = warpMax > warp ? warp + rand.nextInt(warpMax - warp + 1) : warp;
                WarpHelper.addWarp(smoker, amount, warpType);
            }
            if (nutrition > 0) smoker.getFoodData().eat(nutrition, saturation);
            knowledge.ifPresent(k -> {
                if (k.categories().isEmpty()) return;
                for (int r = 0; r < k.rolls(); ++r) {
                    Identifier category = k.categories().get(rand.nextInt(k.categories().size()));
                    ScanningManager.addKnowledge(smoker, KnowledgeType.OBSERVATION, category, k.amount());
                    if (k.warp() > 0 && k.warpCategory().filter(category::equals).isPresent()) {
                        WarpHelper.addWarp(smoker, k.warp(), WarpType.TEMPORARY);
                    }
                }
            });
            spawn.ifPresent(type -> {
                Entity e = type.value().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (e == null) return;
                e.snapTo(smoker.getX(), smoker.getY(), smoker.getZ());
                if (e instanceof WispEntity wisp) {
                    List<Holder.Reference<IAspect>> aspects = level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).listElements()
                            .filter(a -> spawnAvoids.map(key -> !a.is(key)).orElse(true)).toList();
                    if (!aspects.isEmpty()) wisp.setAspect(aspects.get(rand.nextInt(aspects.size())).key().identifier());
                }
                level.addFreshEntity(e);
            });
            return breakPipe;
        }
    }

    /** Applies every matching outcome; returns true when the pipe breaks (TB: tainted tobacco in a silverwood pipe). */
    public boolean smoke(ServerPlayer smoker, Pipe wood) {
        boolean broke = false;
        for (Outcome o : outcomes) {
            if ((o.pipe() == Pipe.ANY || o.pipe() == wood) && smoker.getRandom().nextFloat() < o.chance()) {
                broke |= o.apply(smoker);
            }
        }
        return broke;
    }
}

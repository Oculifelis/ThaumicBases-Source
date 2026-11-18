package com.thaumaturgicbases.revolver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicbases.ThaumaturgicBases;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags;

/**
 * One revolver upgrade (TB RevolverUpgrade), loaded from {@code data/<ns>/thaumaturgicbases/revolver_upgrade/<id>.json}.
 * Upgrades that conflicted in TB share a {@code slot}: the revolver holds one upgrade per slot. A bullet's damage
 * starts at TB's 14; each upgrade multiplies it by {@code match_multiplier * match_per_level^level} when its
 * {@code match} fits the target, else by {@code else_multiplier * else_per_level^level}; {@code final_multiplier}
 * applies last (TB's modifyDamage_end).
 */
public record RevolverUpgrade(String slot, int maxLevel, Optional<Match> match, float matchMultiplier, float matchPerLevel,
                              float elseMultiplier, float elsePerLevel, float finalMultiplier, boolean pierce, boolean noclip,
                              float speedPerLevel, int extraShotsPerLevel, Optional<Reload> reload, Optional<OnHit> onHit,
                              Optional<Knowledge> knowledge, Gun gun) {
    public static final ResourceKey<Registry<RevolverUpgrade>> REGISTRY_KEY = ResourceKey.createRegistryKey(ThaumaturgicBases.id("revolver_upgrade"));

    /** Who an upgrade's damage bonus is for: an entity tag, players, bosses, or anything. */
    public record Match(Optional<TagKey<EntityType<?>>> tag, boolean players, boolean bosses, boolean any) {
        public static final Codec<Match> CODEC = RecordCodecBuilder.create(i -> i.group(
                TagKey.codec(Registries.ENTITY_TYPE).optionalFieldOf("tag").forGetter(Match::tag),
                Codec.BOOL.optionalFieldOf("players", false).forGetter(Match::players),
                Codec.BOOL.optionalFieldOf("bosses", false).forGetter(Match::bosses),
                Codec.BOOL.optionalFieldOf("any", false).forGetter(Match::any)).apply(i, Match::new));

        public boolean test(LivingEntity e) {
            return any || players && e instanceof Player || bosses && e.getType().builtInRegistryHolder().is(Tags.EntityTypes.BOSSES)
                    || tag.map(t -> e.getType().builtInRegistryHolder().is(t)).orElse(false);
        }
    }

    /** A reload that takes {@code aversio} points for {@code shots} shots, replacing the usual 1 for 2 (Primal). */
    public record Reload(int aversio, int shots) {
        public static final Codec<Reload> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("aversio").forGetter(Reload::aversio),
                Codec.INT.fieldOf("shots").forGetter(Reload::shots)).apply(i, Reload::new));
    }

    /** A mob effect the bullet gives {@code chance} of the time. */
    public record OnHit(Identifier effect, int duration, int amplifier, float chance) {
        public static final Codec<OnHit> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("effect").forGetter(OnHit::effect),
                Codec.INT.optionalFieldOf("duration", 200).forGetter(OnHit::duration),
                Codec.INT.optionalFieldOf("amplifier", 1).forGetter(OnHit::amplifier),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(OnHit::chance)).apply(i, OnHit::new));
    }

    /** On a kill, one chance in {@code max(1, odds - level)} to gain {@code amount} observation in {@code category}. */
    public record Knowledge(int odds, Identifier category, int amount) {
        public static final Codec<Knowledge> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("odds", 7).forGetter(Knowledge::odds),
                Identifier.CODEC.fieldOf("category").forGetter(Knowledge::category),
                Codec.INT.optionalFieldOf("amount", 1).forGetter(Knowledge::amount)).apply(i, Knowledge::new));
    }

    /** What the gun itself gains: melee damage per level (Heavy) and self-repair every interval/level ticks (Void). */
    public record Gun(float meleePerLevel, int repairInterval) {
        public static final com.mojang.serialization.MapCodec<Gun> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.FLOAT.optionalFieldOf("melee_per_level", 0.0F).forGetter(Gun::meleePerLevel),
                Codec.INT.optionalFieldOf("repair_interval", 0).forGetter(Gun::repairInterval)).apply(i, Gun::new));
    }

    public static final Codec<RevolverUpgrade> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("slot").forGetter(RevolverUpgrade::slot),
            Codec.INT.fieldOf("max_level").forGetter(RevolverUpgrade::maxLevel),
            Match.CODEC.optionalFieldOf("match").forGetter(RevolverUpgrade::match),
            Codec.FLOAT.optionalFieldOf("match_multiplier", 1.0F).forGetter(RevolverUpgrade::matchMultiplier),
            Codec.FLOAT.optionalFieldOf("match_per_level", 1.0F).forGetter(RevolverUpgrade::matchPerLevel),
            Codec.FLOAT.optionalFieldOf("else_multiplier", 1.0F).forGetter(RevolverUpgrade::elseMultiplier),
            Codec.FLOAT.optionalFieldOf("else_per_level", 1.0F).forGetter(RevolverUpgrade::elsePerLevel),
            Codec.FLOAT.optionalFieldOf("final_multiplier", 1.0F).forGetter(RevolverUpgrade::finalMultiplier),
            Codec.BOOL.optionalFieldOf("pierce", false).forGetter(RevolverUpgrade::pierce),
            Codec.BOOL.optionalFieldOf("noclip", false).forGetter(RevolverUpgrade::noclip),
            Codec.FLOAT.optionalFieldOf("speed_per_level", 1.0F).forGetter(RevolverUpgrade::speedPerLevel),
            Codec.INT.optionalFieldOf("extra_shots_per_level", 0).forGetter(RevolverUpgrade::extraShotsPerLevel),
            Reload.CODEC.optionalFieldOf("reload").forGetter(RevolverUpgrade::reload),
            OnHit.CODEC.optionalFieldOf("on_hit").forGetter(RevolverUpgrade::onHit),
            Knowledge.CODEC.optionalFieldOf("knowledge").forGetter(RevolverUpgrade::knowledge),
            Gun.MAP_CODEC.forGetter(RevolverUpgrade::gun)
    ).apply(i, RevolverUpgrade::new));

    public float damage(LivingEntity target, float current, int level) {
        boolean hit = match.map(m -> m.test(target)).orElse(false);
        return hit ? current * matchMultiplier * (float) Math.pow(matchPerLevel, level)
                : current * elseMultiplier * (float) Math.pow(elsePerLevel, level);
    }
}

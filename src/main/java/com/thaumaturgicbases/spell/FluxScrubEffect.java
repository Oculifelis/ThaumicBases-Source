package com.thaumaturgicbases.spell;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.content.taint.item.EssentiaCrystalFactory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicbases.ThaumaturgicBases;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * TB FociFlux (Flux Scrubber): while held, removes the flux blocks (block tag {@code thaumaturgicbases:scrubbable_flux})
 * within {@code radius} of the point looked at, more of them per pulse with Potency. Warping: each removed block gives
 * {@code warp} temporary warp ({@code warp_chance} of the time also normal warp). Calming ({@code warp} below 0) takes
 * that much away instead. Crystallisation ({@code crystals}): 1 drops a Potentia crystal, 2 a random aspect's crystal,
 * {@code crystal_chance} of the time.
 */
public final class FluxScrubEffect extends AbstractEffectBehavior {
    public static final TagKey<Block> SCRUBBABLE = TagKey.create(Registries.BLOCK, ThaumaturgicBases.id("scrubbable_flux"));
    public static final MapCodec<FluxScrubEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("reach", 16.0).forGetter(e -> e.reach),
            Codec.INT.optionalFieldOf("radius", 3).forGetter(e -> e.radius),
            Codec.INT.optionalFieldOf("per_pulse", 1).forGetter(e -> e.perPulse),
            Codec.INT.optionalFieldOf("warp", 0).forGetter(e -> e.warp),
            Codec.FLOAT.optionalFieldOf("warp_chance", 0.1F).forGetter(e -> e.warpChance),
            Codec.INT.optionalFieldOf("crystals", 0).forGetter(e -> e.crystals),
            Codec.FLOAT.optionalFieldOf("crystal_chance", 0.4F).forGetter(e -> e.crystalChance)).apply(i, FluxScrubEffect::new));

    private final double reach;
    private final int radius;
    private final int perPulse;
    private final int warp;
    private final float warpChance;
    private final int crystals;
    private final float crystalChance;

    public FluxScrubEffect(double reach, int radius, int perPulse, int warp, float warpChance, int crystals, float crystalChance) {
        this.reach = reach;
        this.radius = radius;
        this.perPulse = perPulse;
        this.warp = warp;
        this.warpChance = warpChance;
        this.crystals = crystals;
        this.crystalChance = crystalChance;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TBSpells.FLUX_SCRUB.get();
    }

    @Override
    protected boolean widens() {
        return true;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        ServerLevel level = ctx.level();
        Vec3 at = SpellUtil.block(ctx, target, reach, ClipContext.Fluid.ANY).map(h -> h.getLocation())
                .orElse(SpellUtil.onCaster(ctx, target) ? target.position().add(target.direction().normalize().scale(4)) : target.position());
        BlockPos centre = BlockPos.containing(at);
        int left = perPulse + SpellUtil.potency(power);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            if (left <= 0) break;
            if (!level.getBlockState(pos).is(SCRUBBABLE)) continue;
            BlockPos p = pos.immutable();
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            left--;
            level.sendParticles(ParticleTypes.WITCH, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
            level.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.2F, 1.4F);
            if (ctx.caster() instanceof ServerPlayer player && warp != 0) {
                WarpHelper.addWarp(player, warp, WarpType.TEMPORARY);
                if (ctx.random().nextFloat() < (warp < 0 ? 0.4F : warpChance)) WarpHelper.addWarp(player, warp, WarpType.NORMAL);
            }
            if (crystals > 0 && ctx.random().nextFloat() < crystalChance) {
                if (crystals == 1) {
                    SpellUtil.dropCrystal(level, p, TTAspects.POTENTIA, 1);
                } else {
                    List<Holder.Reference<IAspect>> all = level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).listElements().toList();
                    if (!all.isEmpty()) Block.popResource(level, p, EssentiaCrystalFactory.of(all.get(ctx.random().nextInt(all.size())), 1));
                }
            }
        }
    }
}

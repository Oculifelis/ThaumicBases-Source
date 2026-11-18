package com.thaumaturgicbases.spell;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;

/**
 * TB FociDrain (Liquid Drain): while held, removes the water source the caster looks at. Aquatic: a removed source
 * becomes an Aqua crystal {@code crystal_chance} of the time (Alkimia instead, {@code poison_chance} of those).
 * Netheric: lava sources go too, each paying {@code lava_vis} more vis, and the lava flowing next to it stops.
 */
public final class DrainEffect extends AbstractEffectBehavior {
    public static final MapCodec<DrainEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("reach", 32.0).forGetter(e -> e.reach),
            Codec.BOOL.optionalFieldOf("lava", false).forGetter(e -> e.lava),
            Codec.FLOAT.optionalFieldOf("lava_vis", 0.03F).forGetter(e -> e.lavaVis),
            Codec.FLOAT.optionalFieldOf("crystal_chance", 0.0F).forGetter(e -> e.crystalChance),
            Codec.FLOAT.optionalFieldOf("poison_chance", 0.1F).forGetter(e -> e.poisonChance)).apply(i, DrainEffect::new));

    private final double reach;
    private final boolean lava;
    private final float lavaVis;
    private final float crystalChance;
    private final float poisonChance;

    public DrainEffect(double reach, boolean lava, float lavaVis, float crystalChance, float poisonChance) {
        this.reach = reach;
        this.lava = lava;
        this.lavaVis = lavaVis;
        this.crystalChance = crystalChance;
        this.poisonChance = poisonChance;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TBSpells.DRAIN.get();
    }

    @Override
    protected boolean widens() {
        return true;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        SpellUtil.block(ctx, target, reach, ClipContext.Fluid.SOURCE_ONLY).ifPresent(hit -> {
            ServerLevel level = ctx.level();
            BlockPos pos = hit.getBlockPos();
            if (!drains(level.getFluidState(pos))) {
                pos = pos.relative(hit.getDirection());
                if (!drains(level.getFluidState(pos))) return;
            }
            boolean isLava = level.getFluidState(pos).is(FluidTags.LAVA);
            if (isLava && !SpellUtil.payExtra(ctx, lavaVis)) return;
            if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2)) return;
            level.sendParticles(isLava ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.0);
            level.playSound(null, pos, isLava ? SoundEvents.LAVA_EXTINGUISH : SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.3F, 1.2F);
            if (!isLava && crystalChance > 0 && ctx.random().nextFloat() < crystalChance) {
                SpellUtil.dropCrystal(level, pos, ctx.random().nextFloat() < poisonChance ? TTAspects.ALKIMIA : TTAspects.AQUA, 1);
            }
            if (isLava) {
                for (Direction d : Direction.values()) {
                    BlockPos n = pos.relative(d);
                    FluidState f = level.getFluidState(n);
                    if (f.is(FluidTags.LAVA) && !f.isSource()) level.setBlock(n, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        });
    }

    private boolean drains(FluidState fluid) {
        return fluid.isSource() && (fluid.is(FluidTags.WATER) || lava && fluid.is(FluidTags.LAVA));
    }
}

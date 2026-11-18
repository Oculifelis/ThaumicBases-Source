package com.thaumaturgicbases.spell;

import com.leclowndu93150.thaumaturge.api.aspect.AspectIndexAccess;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.content.taint.item.EssentiaCrystalFactory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

/**
 * TB FociExperience: consumes the block looked at and turns it into experience (the block's own experience drop, or
 * {@code potency + 1} points when it has none). Each block costs {@code vis_per_hardness} more vis per point of
 * hardness; unbreakable blocks are left alone. Vaporizing ({@code crystal_chance}): a consumed block may also drop one
 * crystal of each of its aspects, {@code max_aspect} at most. Decomposing ({@code mobs}): the creature looked at is
 * dissolved into experience worth its health, paying {@code vis_per_health} more vis per point; players and bosses
 * are spared.
 */
public final class ExperienceEffect extends AbstractEffectBehavior {
    public static final MapCodec<ExperienceEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("reach", 16.0).forGetter(e -> e.reach),
            Codec.FLOAT.optionalFieldOf("vis_per_hardness", 1.0F).forGetter(e -> e.visPerHardness),
            Codec.FLOAT.optionalFieldOf("crystal_chance", 0.0F).forGetter(e -> e.crystalChance),
            Codec.INT.optionalFieldOf("max_aspect", 4).forGetter(e -> e.maxAspect),
            Codec.BOOL.optionalFieldOf("mobs", false).forGetter(e -> e.mobs),
            Codec.FLOAT.optionalFieldOf("vis_per_health", 0.5F).forGetter(e -> e.visPerHealth)).apply(i, ExperienceEffect::new));

    private final double reach;
    private final float visPerHardness;
    private final float crystalChance;
    private final int maxAspect;
    private final boolean mobs;
    private final float visPerHealth;

    public ExperienceEffect(double reach, float visPerHardness, float crystalChance, int maxAspect, boolean mobs, float visPerHealth) {
        this.reach = reach;
        this.visPerHardness = visPerHardness;
        this.crystalChance = crystalChance;
        this.maxAspect = maxAspect;
        this.mobs = mobs;
        this.visPerHealth = visPerHealth;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TBSpells.EXPERIENCE.get();
    }

    @Override
    protected boolean widens() {
        return true;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        ServerLevel level = ctx.level();
        if (mobs) {
            Optional<LivingEntity> mob = SpellUtil.living(ctx, target, reach);
            if (mob.isPresent()) {
                LivingEntity eaten = mob.get();
                if (eaten instanceof Player || eaten.getType().builtInRegistryHolder().is(Tags.EntityTypes.BOSSES)) return;
                float hp = eaten.getHealth();
                if (!SpellUtil.payExtra(ctx, hp * visPerHealth)) return;
                ExperienceOrb.award(level, eaten.position(), Math.max(1, Math.round(hp)));
                level.sendParticles(ParticleTypes.WITCH, eaten.getX(), eaten.getY() + eaten.getBbHeight() / 2, eaten.getZ(), 16,
                        0.3, 0.3, 0.3, 0.0);
                eaten.discard();
                return;
            }
        }
        int potency = SpellUtil.potency(power);
        SpellUtil.block(ctx, target, reach, ClipContext.Fluid.NONE).ifPresent(hit -> {
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (state.isAir() || hardness <= 0.0F) return;
            if (ctx.caster() instanceof Player p && !level.mayInteract(p, pos)) return;
            if (!SpellUtil.payExtra(ctx, hardness * visPerHardness)) return;
            int xp = state.getExpDrop(level, pos, level.getBlockEntity(pos), ctx.caster(), ItemStack.EMPTY);
            if (xp <= 0) xp = potency + 1;
            ItemStack asItem = new ItemStack(state.getBlock().asItem());
            if (!level.removeBlock(pos, false)) return;
            level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.2);
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, 0.8F);
            ExperienceOrb.award(level, pos.getCenter(), xp);
            if (crystalChance > 0 && !asItem.isEmpty() && ctx.random().nextFloat() < crystalChance) {
                for (AspectInstance a : AspectIndexAccess.of(asItem).entries()) {
                    Block.popResource(level, pos, EssentiaCrystalFactory.of(a.aspect(), Math.min(maxAspect, a.amount())));
                }
            }
        });
    }
}

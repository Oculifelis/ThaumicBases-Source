package com.thaumaturgicbases.spell;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.content.casters.CasterManager;
import com.leclowndu93150.thaumaturge.content.taint.item.EssentiaCrystalFactory;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Helpers shared by the TB spell effects. */
final class SpellUtil {
    /** Thaumaturge's Amplify adds 0.25 power a level, so TB's Potency rank is read back as {@code (power - 1) / 0.25}. */
    private static final float POWER_PER_POTENCY = 0.25F;

    private SpellUtil() {
    }

    static int potency(float power) {
        return Math.max(0, Math.round((power - 1.0F) / POWER_PER_POTENCY));
    }

    /** Whether the effect sits right under the origin (the target is the caster): it then works on what the caster looks at. */
    static boolean onCaster(CastContext ctx, SpellTarget target) {
        return target.entity().filter(e -> e == ctx.caster()).isPresent();
    }

    /** The block the target names, or (cast on the caster) the block it looks at within {@code reach}. */
    static Optional<BlockHitResult> block(CastContext ctx, SpellTarget target, double reach, ClipContext.Fluid fluids) {
        if (target.block().isPresent()) {
            return target.block();
        }
        if (!onCaster(ctx, target)) {
            return Optional.empty();
        }
        Vec3 from = target.position();
        BlockHitResult hit = ctx.level().clip(new ClipContext(from, from.add(target.direction().normalize().scale(reach)),
                ClipContext.Block.OUTLINE, fluids, ctx.caster()));
        return hit.getType() == HitResult.Type.BLOCK ? Optional.of(hit) : Optional.empty();
    }

    /** The living creature the target names, or (cast on the caster) the one it looks at within {@code reach}. */
    static Optional<LivingEntity> living(CastContext ctx, SpellTarget target, double reach) {
        Predicate<Entity> alive = e -> e instanceof LivingEntity l && l.isAlive() && e != ctx.caster();
        if (!onCaster(ctx, target)) {
            return target.entity().filter(alive).map(LivingEntity.class::cast);
        }
        LivingEntity looker = ctx.caster();
        Vec3 eye = looker.getEyePosition();
        Vec3 look = looker.getLookAngle().scale(reach);
        Vec3 end = eye.add(look);
        BlockHitResult wall = looker.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, looker));
        if (wall.getType() == HitResult.Type.BLOCK) {
            end = wall.getLocation();
        }
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(looker.level(), looker, eye, end,
                looker.getBoundingBox().expandTowards(look).inflate(1), alive, 0.3F);
        return hit == null ? Optional.empty() : Optional.of((LivingEntity) hit.getEntity());
    }

    /** Extra vis on top of the part's own cost (TB's costs by hardness or health). Non-players pay nothing. */
    static boolean payExtra(CastContext ctx, float vis) {
        if (vis <= 0.0F || !(ctx.caster() instanceof Player player) || player.getAbilities().instabuild) {
            return true;
        }
        return CasterManager.drawVisFromPack(player, vis);
    }

    /** Drops one crystallised essentia of an aspect at a block. */
    static void dropCrystal(Level level, BlockPos pos, ResourceKey<IAspect> aspect, int count) {
        Block.popResource(level, pos, EssentiaCrystalFactory.of(Aspects.resolve(level, aspect), count));
    }
}

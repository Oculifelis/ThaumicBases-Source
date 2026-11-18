package com.thaumaturgicbases.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * TB EntityAIAvoidCampfire: a lit campfire or brazier keeps this mob scared for 100 ticks, and it runs up to 16 blocks
 * away. Mobs in the {@code thaumaturgicbases:fears_fire} entity tag get it when they join a level.
 */
public class AvoidFireBowlGoal extends Goal {
    private static final double NEAR_SPEED = 1.5;
    private static final double FAR_SPEED = 1.25;
    private final PathfinderMob mob;
    private BlockPos fire = BlockPos.ZERO;
    private int scareTimer;
    private Path path;

    public AvoidFireBowlGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    /** Called by a burning fire bowl for each mob in its 12x6x12 reach. */
    public static void scare(PathfinderMob mob, BlockPos fire) {
        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof AvoidFireBowlGoal goal) {
                goal.fire = fire;
                goal.scareTimer = 100;
                return;
            }
        }
    }

    @Override
    public boolean canUse() {
        if (scareTimer <= 0) return false;
        Vec3 away = DefaultRandomPos.getPosAway(mob, 16, 10, Vec3.atBottomCenterOf(fire));
        if (away == null) return false;
        path = mob.getNavigation().createPath(away.x, away.y, away.z, 0);
        return path != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !mob.getNavigation().isDone() && scareTimer > 0;
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(path, NEAR_SPEED);
    }

    @Override
    public void tick() {
        --scareTimer;
        mob.getNavigation().setSpeedModifier(mob.distanceToSqr(Vec3.atBottomCenterOf(fire)) < 49.0 ? NEAR_SPEED : FAR_SPEED);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}

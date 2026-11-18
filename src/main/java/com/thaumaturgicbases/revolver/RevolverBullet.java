package com.thaumaturgicbases.revolver;

import com.leclowndu93150.thaumaturge.api.capability.KnowledgeType;
import com.leclowndu93150.thaumaturge.api.research.scan.ScanningManager;
import com.thaumaturgicbases.revolver.RevolverItem.Applied;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * TB EntityRevolverBullet: a fast, weightless shot (three times an arrow's speed, Speed adds 9% a level) dealing 14
 * before upgrades. Primal bullets pass through blocks; Piercing ones go on through what they hit. Gone after 200 ticks.
 * The upgrades are read from a copy of the revolver taken when it fired.
 */
public class RevolverBullet extends ThrowableProjectile {
    private static final float BASE_DAMAGE = 14.0F;
    private ItemStack revolver = ItemStack.EMPTY;
    private boolean pierce;

    public RevolverBullet(EntityType<? extends RevolverBullet> type, Level level) {
        super(type, level);
    }

    public RevolverBullet(ServerLevel level, Player shooter, ItemStack revolver) {
        this(TBRevolver.BULLET.get(), level);
        this.revolver = revolver.copyWithCount(1);
        setOwner(shooter);
        setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        float speed = 3.0F;
        for (Applied a : RevolverItem.upgrades(revolver, level.registryAccess())) {
            speed *= (float) Math.pow(a.upgrade().speedPerLevel(), a.level());
            if (a.upgrade().noclip()) noPhysics = true;
            if (a.upgrade().pierce()) pierce = true;
        }
        shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, speed, 0.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            Vec3 m = getDeltaMovement();
            server.sendParticles(ParticleTypes.CRIT, getX() - m.x / 2, getY() - m.y / 2, getZ() - m.z / 2, 1, 0, 0, 0, 0);
            if (tickCount >= 200) discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (!(level() instanceof ServerLevel server) || !(hit.getEntity() instanceof LivingEntity target)) return;
        List<Applied> ups = RevolverItem.upgrades(revolver, server.registryAccess());
        float damage = BASE_DAMAGE;
        for (Applied a : ups) damage = a.upgrade().damage(target, damage, a.level());
        for (Applied a : ups) damage *= a.upgrade().finalMultiplier();
        Entity owner = getOwner();
        target.hurtServer(server, damageSources().thrown(this, owner), damage);
        for (Applied a : ups) {
            a.upgrade().onHit().filter(h -> random.nextFloat() < h.chance()).ifPresent(h ->
                    BuiltInRegistries.MOB_EFFECT.get(h.effect()).ifPresent(e -> target.addEffect(new MobEffectInstance(e, h.duration(), h.amplifier()))));
            if (!target.isAlive() && owner instanceof Player player) {
                a.upgrade().knowledge().filter(k -> random.nextInt(Math.max(1, k.odds() - a.level())) == 0)
                        .ifPresent(k -> ScanningManager.addKnowledge(player, KnowledgeType.OBSERVATION, k.category(), k.amount()));
            }
        }
        if (!pierce) discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!noPhysics && !level().isClientSide()) discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (!revolver.isEmpty()) out.store("revolver", ItemStack.CODEC, revolver);
        out.putBoolean("pierce", pierce);
        out.putBoolean("noclip", noPhysics);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        revolver = in.read("revolver", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        pierce = in.getBooleanOr("pierce", false);
        noPhysics = in.getBooleanOr("noclip", false);
    }
}

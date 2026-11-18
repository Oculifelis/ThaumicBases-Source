package com.thaumaturgicbases.item;

import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * TB ItemHerobrinesScythe: hitting a monster arcs lightning to another monster within 7 blocks of it, which is hit as
 * if by the wielder, and on down the chain until no unhit monster is left in reach.
 */
public class ScytheItem extends Item {
    private static final ThreadLocal<Boolean> CHAINING = ThreadLocal.withInitial(() -> false);

    public ScytheItem(Properties props) {
        super(props);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (CHAINING.get() || !(attacker instanceof Player player) || !(target instanceof Enemy) || !(target.level() instanceof ServerLevel level)) return;
        CHAINING.set(true);
        try {
            Set<LivingEntity> hit = new HashSet<>();
            hit.add(target);
            LivingEntity from = target;
            while (from != null) from = arc(level, player, hit, from);
        } finally {
            CHAINING.set(false);
        }
    }

    private static LivingEntity arc(ServerLevel level, Player player, Set<LivingEntity> hit, LivingEntity from) {
        List<LivingEntity> near = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(7),
                e -> e.isAlive() && e instanceof Enemy && !(e instanceof Player) && !hit.contains(e)));
        if (near.isEmpty()) return null;
        LivingEntity next = near.get(level.getRandom().nextInt(near.size()));
        hit.add(next);
        Vec3 a = from.position().add(0, level.getRandom().nextDouble() * from.getEyeHeight(), 0);
        Vec3 b = next.position().add(0, level.getRandom().nextDouble() * next.getEyeHeight(), 0);
        for (int i = 0; i <= 20; ++i) {
            Vec3 p = a.lerp(b, i / 20.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
        level.playSound(null, next.blockPosition(), TTSounds.ZAP.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        player.attack(next);
        return next;
    }
}

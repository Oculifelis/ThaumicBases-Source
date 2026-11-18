package com.thaumaturgicbases.spell;

import com.leclowndu93150.thaumaturge.api.casters.ICaster;
import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicbases.TBComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * TB FociActivation (remote access): sneak-cast at a block to remember it on the caster (the
 * {@code thaumaturgicbases:activation_link} component, replacing TB's blockX/Y/Z/dim NBT); cast again anywhere to
 * right-click that block from afar, as long as it is in the same dimension, loaded and within
 * {@code range * (potency + 1)} blocks.
 */
public final class ActivationEffect extends AbstractEffectBehavior {
    public static final MapCodec<ActivationEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("reach", 8.0).forGetter(e -> e.reach),
            Codec.INT.optionalFieldOf("range", 32).forGetter(e -> e.range)).apply(i, ActivationEffect::new));

    private final double reach;
    private final int range;

    public ActivationEffect(double reach, int range) {
        this.reach = reach;
        this.range = range;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TBSpells.ACTIVATION.get();
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        if (!(ctx.caster() instanceof Player player)) return;
        ItemStack caster = player.getMainHandItem().getItem() instanceof ICaster ? player.getMainHandItem() : player.getOffhandItem();
        if (!(caster.getItem() instanceof ICaster)) return;
        ServerLevel level = ctx.level();
        if (player.isShiftKeyDown()) {
            SpellUtil.block(ctx, target, reach, ClipContext.Fluid.NONE).ifPresent(hit -> {
                caster.set(TBComponents.ACTIVATION_LINK.get(), GlobalPos.of(level.dimension(), hit.getBlockPos()));
                player.sendOverlayMessage(Component.translatable("tb.txt.linkEstablished"));
            });
            return;
        }
        GlobalPos link = caster.get(TBComponents.ACTIVATION_LINK.get());
        if (link == null) return;
        BlockPos pos = link.pos();
        int max = range * (SpellUtil.potency(power) + 1);
        if (!link.dimension().equals(level.dimension())) {
            player.sendOverlayMessage(Component.translatable("tb.txt.wrongDimension"));
            return;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > (double) max * max) {
            player.sendOverlayMessage(Component.translatable("tb.txt.tooFar"));
            return;
        }
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        state.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        level.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.3);
    }
}

package com.thaumaturgicbases.item;

import com.thaumaturgicbases.TBDataMaps;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * TB ItemSmokingPipe: hold use for 64 ticks to smoke the first tobacco in the inventory. What it does comes from the
 * {@code thaumaturgicbases:tobacco} data map on that tobacco; the silverwood pipe picks the silverwood outcomes.
 */
public class SmokingPipeItem extends Item {
    private final TobaccoEffect.Pipe wood;

    public SmokingPipeItem(Properties props, TobaccoEffect.Pipe wood) {
        super(props);
        this.wood = wood;
    }

    private static int tobaccoSlot(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.typeHolder().getData(TBDataMaps.TOBACCO) != null) return i;
        }
        return -1;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (tobaccoSlot(player) < 0) return InteractionResult.FAIL;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 64;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        if (level.isClientSide() && ticksRemaining < 32) puff(level, user, 1);
    }

    private void puff(Level level, LivingEntity user, int count) {
        Vec3 look = user.getLookAngle();
        double x = user.getX() + look.x / 5, y = user.getEyeY() + look.y / 5, z = user.getZ() + look.z / 5;
        for (int i = 0; i < count; ++i) {
            level.addParticle(wood == TobaccoEffect.Pipe.SILVERWOOD ? ParticleTypes.POOF : ParticleTypes.SMOKE, x, y, z, look.x / 10, look.y / 10, look.z / 10);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(user instanceof Player player)) return stack;
        int slot = tobaccoSlot(player);
        if (slot < 0) return stack;
        if (level.isClientSide()) {
            puff(level, user, 100);
            return stack;
        }
        ItemStack tobacco = player.getInventory().getItem(slot);
        TobaccoEffect effect = tobacco.typeHolder().getData(TBDataMaps.TOBACCO);
        tobacco.shrink(1);
        if (effect != null && player instanceof ServerPlayer sp && effect.smoke(sp, wood)) {
            player.onEquippedItemBroken(stack.getItem(), user.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            return ItemStack.EMPTY;
        }
        return stack;
    }
}

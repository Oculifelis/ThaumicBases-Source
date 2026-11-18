package com.thaumaturgicbases.item;

import com.leclowndu93150.thaumaturge.api.wands.WandCap;
import com.leclowndu93150.thaumaturge.api.wands.WandRod;
import com.leclowndu93150.thaumaturge.content.casters.CasterManager;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.content.wands.WandParts;
import com.thaumaturgicbases.TBBracelets;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * TB ItemCastingBracelet: a coreless caster. It holds little vis (its rod), pays 1/1.5 of its cap's cost and cools
 * down in half the time a wand would. Its kind (iron ... primal) is its wand parts; the name and the model follow it.
 */
public class CastingBraceletItem extends ItemWand {
    public CastingBraceletItem(Item.Properties props) {
        super(props);
    }

    public static ItemStack of(Item item, WandCap cap, WandRod rod) {
        return ItemWand.create(item, cap, rod, false);
    }

    @Override
    public Component getName(ItemStack stack) {
        WandParts parts = assembly(stack);
        String kind = parts == null ? null : TBBracelets.kindOf(parts.rod());
        return Component.translatable(kind == null ? "item.thaumaturgicbases.casting_bracelet" : "item.thaumaturgicbases.casting_bracelet." + kind);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        float before = CasterManager.cooldownSeconds(player);
        InteractionResult result = super.use(level, player, hand);
        halveCooldown(player, player.getItemInHand(hand), before);
        return result;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        float before = CasterManager.cooldownSeconds(entity);
        boolean result = super.releaseUsing(stack, level, entity, timeLeft);
        halveCooldown(entity, stack, before);
        return result;
    }

    /** A cast that just started a cooldown gets half of it (TB's bracelets "recharge twice as fast"). */
    private static void halveCooldown(LivingEntity entity, ItemStack stack, float before) {
        if (entity.level().isClientSide()) return;
        float after = CasterManager.cooldownSeconds(entity);
        if (after <= before || after <= 0) return;
        int ticks = Math.max(1, Math.round(after * 20.0F / 2.0F));
        CasterManager.startCooldown(entity, ticks);
        if (entity instanceof Player player) player.getCooldowns().addCooldown(stack, ticks);
    }
}

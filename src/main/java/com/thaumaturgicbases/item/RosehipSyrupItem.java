package com.thaumaturgicbases.item;

import com.thaumaturgicbases.ThaumaturgicBases;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

/**
 * TB's Rosehip Syrup, a better milk: drinking it removes every effect in {@code thaumaturgicbases:rosehip_cures} and lowers
 * every effect in {@code thaumaturgicbases:rosehip_weakens} by one level (removing it at level I). The empty phial is
 * returned.
 */
public class RosehipSyrupItem extends Item {
    public static final TagKey<MobEffect> CURES = TagKey.create(Registries.MOB_EFFECT, ThaumaturgicBases.id("rosehip_cures"));
    public static final TagKey<MobEffect> WEAKENS = TagKey.create(Registries.MOB_EFFECT, ThaumaturgicBases.id("rosehip_weakens"));
    private static final Identifier PHIAL = Identifier.fromNamespaceAndPath("thaumaturge", "phial");

    public RosehipSyrupItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            for (MobEffectInstance effect : List.copyOf(entity.getActiveEffects())) {
                Holder<MobEffect> type = effect.getEffect();
                if (type.is(CURES)) {
                    entity.removeEffect(type);
                } else if (type.is(WEAKENS)) {
                    entity.removeEffect(type);
                    if (effect.getAmplifier() > 0) {
                        entity.addEffect(new MobEffectInstance(type, effect.getDuration(), effect.getAmplifier() - 1,
                                effect.isAmbient(), effect.isVisible(), effect.showIcon()));
                    }
                }
            }
        }
        ItemStack phial = new ItemStack(BuiltInRegistries.ITEM.getValue(PHIAL));
        if (entity instanceof Player player) {
            ItemStack copy = stack.copy();
            super.finishUsingItem(stack, level, entity);
            return ItemUtils.createFilledResult(copy, player, phial);
        }
        return super.finishUsingItem(stack, level, entity);
    }
}

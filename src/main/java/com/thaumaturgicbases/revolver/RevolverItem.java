package com.thaumaturgicbases.revolver;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.IAspectContainer;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.thaumaturgicbases.revolver.TBRevolver.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/**
 * TB ItemRevolver. Right-click fires a bullet while the cylinder holds shots. TB loaded the cylinder from a jar kept
 * in the revolver's own GUI; here sneak-right-clicking any essentia container (a jar) with Aversio in it reloads: one
 * point gives two shots (more with Efficiency; Primal takes two points for one shot). Melee hits do 5 (+2 a Heavy
 * level) and wear it twice as fast; Void slowly repairs it and warps its holder.
 */
public class RevolverItem extends Item {
    public RevolverItem(Item.Properties props) {
        super(props);
    }

    /** Every upgrade on a stack, resolved against the loaded upgrade registry. */
    public static List<Applied> upgrades(ItemStack stack, HolderLookup.Provider registries) {
        List<Applied> out = new ArrayList<>();
        Optional<? extends HolderLookup.RegistryLookup<RevolverUpgrade>> lookup = registries.lookup(RevolverUpgrade.REGISTRY_KEY);
        if (lookup.isEmpty()) return out;
        for (var c : TBRevolver.SLOT_COMPONENTS.values()) {
            Level l = stack.get(c.get());
            if (l == null || l.empty()) continue;
            lookup.get().get(ResourceKey.create(RevolverUpgrade.REGISTRY_KEY, l.upgrade()))
                    .ifPresent(h -> out.add(new Applied(l.upgrade(), h.value(), l.level())));
        }
        return out;
    }

    public record Applied(net.minecraft.resources.Identifier id, RevolverUpgrade upgrade, int level) {
    }

    @Override
    public InteractionResult use(net.minecraft.world.level.Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        int shots = stack.getOrDefault(TBRevolver.SHOTS.get(), 0);
        if (shots <= 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), TBRevolver.NO_AMMO.get(), SoundSource.PLAYERS, 1.0F, 2.0F);
            return InteractionResult.FAIL;
        }
        RevolverBullet bullet = new RevolverBullet(server, player, stack);
        server.addFreshEntity(bullet);
        stack.set(TBRevolver.SHOTS.get(), shots - 1);
        stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), TBRevolver.SHOT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !player.isShiftKeyDown()
                || !(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof IAspectContainer jar)) {
            return InteractionResult.PASS;
        }
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        ItemStack stack = ctx.getItemInHand();
        int aversio = 1;
        int shots = 2;
        Optional<RevolverUpgrade.Reload> override = Optional.empty();
        int extra = 0;
        for (Applied a : upgrades(stack, ctx.getLevel().registryAccess())) {
            if (a.upgrade().reload().isPresent()) override = a.upgrade().reload();
            extra += a.upgrade().extraShotsPerLevel() * a.level();
        }
        if (override.isPresent()) {
            aversio = override.get().aversio();
            shots = override.get().shots();
        } else {
            shots += extra;
        }
        Holder<IAspect> weapon = Aspects.resolve(ctx.getLevel(), TTAspects.AVERSIO);
        if (stack.getOrDefault(TBRevolver.SHOTS.get(), 0) >= shots || !jar.holds(weapon, aversio) || !jar.drain(weapon, aversio)) {
            ctx.getLevel().playSound(null, ctx.getClickedPos(), TBRevolver.NO_AMMO.get(), SoundSource.PLAYERS, 1.0F, 2.0F);
            return InteractionResult.FAIL;
        }
        stack.set(TBRevolver.SHOTS.get(), shots);
        ctx.getLevel().playSound(null, ctx.getClickedPos(), TBRevolver.RELOAD.get(), SoundSource.PLAYERS, 1.0F, 2.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        if (!(source.getWeaponItem() instanceof ItemStack stack) || target.level().isClientSide()) return 0.0F;
        float bonus = 0.0F;
        for (Applied a : upgrades(stack, target.level().registryAccess())) bonus += a.upgrade().gun().meleePerLevel() * a.level();
        return bonus;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(2, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!stack.isDamaged() || !(entity instanceof LivingEntity)) return;
        for (Applied a : upgrades(stack, level.registryAccess())) {
            int interval = a.upgrade().gun().repairInterval();
            if (interval > 0 && entity.tickCount % Math.max(1, interval / a.level()) == 0) stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("item.thaumaturgicbases.revolver.shots", stack.getOrDefault(TBRevolver.SHOTS.get(), 0))
                .withStyle(ChatFormatting.GRAY));
        for (var c : TBRevolver.SLOT_COMPONENTS.values()) {
            Level l = stack.get(c.get());
            if (l == null || l.empty()) continue;
            out.accept(Component.translatable("revolver_upgrade." + l.upgrade().getNamespace() + "." + l.upgrade().getPath())
                    .append(" ").append(Component.translatable("enchantment.level." + l.level())).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}

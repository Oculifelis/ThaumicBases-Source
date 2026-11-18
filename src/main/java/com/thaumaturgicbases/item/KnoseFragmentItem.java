package com.thaumaturgicbases.item;

import com.leclowndu93150.thaumaturge.api.research.scan.ScanningManager;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.thaumaturgicbases.TBComponents;
import com.thaumaturgicbases.TBComponents.KnoseInsight;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Rosa Mysteria fragment (TB's knoseFragment). Using one teaches the knowledge its {@link KnoseInsight} names; the
 * tainted fragment also brings warp. TC4 added aspect points to the player's pool; Thaumaturge has research
 * knowledge instead, so each fragment grants observation in the categories its aspects belong to.
 */
public class KnoseFragmentItem extends Item {
    public KnoseFragmentItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        KnoseInsight insight = stack.get(TBComponents.KNOSE_INSIGHT.get());
        return insight == null ? super.getName(stack) : Component.translatable(getDescriptionId() + "." + insight.variant());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        KnoseInsight insight = stack.get(TBComponents.KNOSE_INSIGHT.get());
        if (insight == null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
            for (TBComponents.Grant grant : insight.grants()) {
                ScanningManager.addKnowledge(server, grant.type(), grant.category(), grant.amount());
            }
            if (insight.warp() > 0) {
                WarpHelper.addWarp(server, insight.warp(), WarpType.TEMPORARY);
            }
            stack.consume(1, player);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }
}

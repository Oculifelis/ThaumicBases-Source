package com.thaumaturgicbases.item;

import com.thaumaturgicbases.TBRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Bucket of Incendium: pours one pyrofluid block on the clicked face and gives the empty bucket back. */
public class PyrofluidBucketItem extends Item {
    public PyrofluidBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        if (!level.getBlockState(pos).canBeReplaced() || player != null && !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, TBRegistry.PYROFLUID.get().defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (player != null) {
                ItemStack stack = context.getItemInHand();
                player.setItemInHand(context.getHand(), ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            }
        }
        return InteractionResult.SUCCESS;
    }
}

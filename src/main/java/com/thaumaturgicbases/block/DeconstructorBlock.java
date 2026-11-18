package com.thaumaturgicbases.block;

import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.mojang.serialization.MapCodec;
import com.thaumaturgicbases.TBDevices;
import com.thaumaturgicbases.ThaumaturgicBases;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * TB's Entity Deconstructor: a plate that hurts whatever stands on it and teaches the player who last clicked it
 * (TB.EntityDec). With {@code entity_deconstructor_adv} researched, primal crystal blocks can be fed to it; all six
 * make it work faster, reach farther, and also break down dropped items (TB.EntityDecAdv).
 */
public class DeconstructorBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1.6, 16);

    public DeconstructorBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeconstructorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TBDevices.DECONSTRUCTOR_BE.get(), DeconstructorBlockEntity::tick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DeconstructorBlockEntity d)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (KnowledgeAccess.of(player).isResearchComplete(ThaumaturgicBases.id("entity_deconstructor_adv")) && d.addCrystal(stack)) {
                stack.consume(1, player);
            }
            d.setOwner(player);
            player.sendSystemMessage(Component.translatable("thaumaturgicbases.deconstructor_linked").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        }
        return InteractionResult.SUCCESS;
    }
}

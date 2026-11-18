package com.thaumaturgicbases.block;

import com.leclowndu93150.thaumaturge.api.casters.IInteractWithCaster;
import com.mojang.serialization.MapCodec;
import com.thaumaturgicbases.TBDevices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * TB's Overchanter. TB had a one-slot GUI; here the item sits on the block like on a Thaumaturge pedestal: right-click
 * with an enchanted item to place it, with an empty hand to take it back, and with a caster to start.
 */
public class OverchanterBlock extends BaseEntityBlock implements IInteractWithCaster {
    public static final BooleanProperty HAS_ITEM = BooleanProperty.create("has_item");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public OverchanterBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(HAS_ITEM, false).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_ITEM, ACTIVE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OverchanterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TBDevices.OVERCHANTER_BE.get(), OverchanterBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof OverchanterBlockEntity be) || !be.isEmpty()
                || !OverchanterBlockEntity.canOverchant(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) be.setItem(0, stack.split(1));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof OverchanterBlockEntity be) || be.isEmpty() || be.isWorking()) return InteractionResult.PASS;
        if (!level.isClientSide()) player.getInventory().placeItemBackInInventory(be.takeItem());
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onCasterRightClick(Level level, ItemStack caster, Player player, BlockPos pos, Direction face, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof OverchanterBlockEntity be)) return false;
        if (level.isClientSide()) return be.canStart();
        if (!be.tryStart()) return false;
        player.swing(hand, true);
        return true;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean moved) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
}

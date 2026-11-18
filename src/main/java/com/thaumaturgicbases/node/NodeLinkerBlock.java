package com.thaumaturgicbases.node;

import com.leclowndu93150.thaumaturge.api.casters.IInteractWithCaster;
import com.mojang.serialization.MapCodec;
import com.thaumaturgicbases.TBComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * TB BlockNodeLinker. Right-click one linker with a caster, then another: the first becomes the receiver and draws
 * from the node under the second. The pending first linker is the {@code thaumaturgicbases:linker_origin} component on the
 * caster (TB's linkCoord NBT).
 */
public class NodeLinkerBlock extends BaseEntityBlock implements IInteractWithCaster {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    public NodeLinkerBlock(Properties props) {
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
        return new NodeLinkerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TBNodes.NODE_LINKER_BE.get(), NodeLinkerBlockEntity::serverTick);
    }

    @Override
    public boolean onCasterRightClick(Level level, ItemStack caster, Player player, BlockPos pos, Direction face, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof NodeLinkerBlockEntity)) return false;
        player.swing(hand, true);
        if (level.isClientSide()) return true;
        GlobalPos origin = caster.get(TBComponents.LINKER_ORIGIN.get());
        if (origin != null && origin.dimension().equals(level.dimension()) && !origin.pos().equals(pos)
                && level.getBlockEntity(origin.pos()) instanceof NodeLinkerBlockEntity first) {
            first.setLink(pos);
            caster.remove(TBComponents.LINKER_ORIGIN.get());
            player.sendOverlayMessage(Component.translatable("tb.txt.linkEstablished"));
            return true;
        }
        caster.set(TBComponents.LINKER_ORIGIN.get(), GlobalPos.of(level.dimension(), pos));
        player.sendOverlayMessage(Component.translatable("tb.txt.linkStarted"));
        return true;
    }
}

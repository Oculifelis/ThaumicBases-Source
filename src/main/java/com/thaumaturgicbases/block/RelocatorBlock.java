package com.thaumaturgicbases.block;

import com.leclowndu93150.thaumaturge.api.casters.IInteractWithCaster;
import com.mojang.serialization.MapCodec;
import com.thaumaturgicbases.TBDevices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * TB's Thaumic Relocator: pushes entities along its facing (the inverse one pulls them in); sneaking flips it.
 * Range is 10 blocks per relocator stacked behind it. A redstone signal switches it off; a caster click turns it to
 * the clicked face (sneaking: the opposite face).
 */
public class RelocatorBlock extends BaseEntityBlock implements IInteractWithCaster {
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    private static final DustParticleOptions PUSH = new DustParticleOptions(0x8F5CFF, 1.0F);
    private static final DustParticleOptions PULL = new DustParticleOptions(0xFF7A3D, 1.0F);
    public final boolean inverse;

    public RelocatorBlock(Properties props, boolean inverse) {
        super(props);
        this.inverse = inverse;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RelocatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TBDevices.RELOCATOR_BE.get(), RelocatorBlockEntity::tick);
    }

    @Override
    public boolean onCasterRightClick(Level level, ItemStack caster, Player player, BlockPos pos, Direction face, InteractionHand hand) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return false;
        player.swing(hand);
        level.playSound(player, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(FACING, player.isShiftKeyDown() ? face.getOpposite() : face), 3);
        }
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (level.hasNeighborSignal(pos)) return;
        Direction d = state.getValue(FACING);
        double x = pos.getX() + 0.5 + d.getStepX() * 0.5 + (d.getStepX() == 0 ? (r.nextDouble() - 0.5) : 0);
        double y = pos.getY() + 0.5 + d.getStepY() * 0.5 + (d.getStepY() == 0 ? (r.nextDouble() - 0.5) : 0);
        double z = pos.getZ() + 0.5 + d.getStepZ() * 0.5 + (d.getStepZ() == 0 ? (r.nextDouble() - 0.5) : 0);
        // Pushing: particles leave the face; pulling: they start 3 blocks out and stream in.
        int out = inverse ? 3 : 0;
        double v = inverse ? -0.25 : 0.25;
        level.addParticle(inverse ? PULL : PUSH, x + d.getStepX() * out, y + d.getStepY() * out, z + d.getStepZ() * out,
                d.getStepX() * v, d.getStepY() * v, d.getStepZ() * v);
    }
}

package com.thaumaturgicbases.block;

import com.mojang.serialization.MapCodec;
import com.thaumaturgicbases.TBDevices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * TB's Campfire and Brazier: while burning they scare nearby mobs away ({@link com.thaumaturgicbases.entity.AvoidFireBowlGoal}).
 * The campfire takes a log (the {@code thaumaturgicbases:campfire_logs} item data map gives its burn time) and then any
 * furnace fuel; the brazier burns 6 Ignis essentia from nearby jars for 1600 ticks.
 */
public class FireBowlBlock extends BaseEntityBlock {
    public static final BooleanProperty LOGS = BooleanProperty.create("logs");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape CAMPFIRE = Block.box(1, 0, 1, 15, 8, 15);
    private static final VoxelShape BRAZIER = Block.box(1, 0, 1, 15, 16, 15);
    public final boolean brazier;

    public FireBowlBlock(Properties props, boolean brazier) {
        super(props);
        this.brazier = brazier;
        registerDefaultState(defaultBlockState().setValue(LOGS, false).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOGS, LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return brazier ? BRAZIER : CAMPFIRE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FireBowlBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TBDevices.FIRE_BOWL_BE.get(), FireBowlBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (brazier || stack.isEmpty() || !(level.getBlockEntity(pos) instanceof FireBowlBlockEntity fire)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (fire.addLog(stack) || fire.addFuel(stack)) {
            stack.consume(1, player);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(LIT)) return;
        double y = pos.getY() + (brazier ? 0.875 : 0.0);
        level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5 + (r.nextDouble() * 2 - 1) / 4, y,
                pos.getZ() + 0.5 + (r.nextDouble() * 2 - 1) / 4, 0, 0.04, 0);
        for (int i = 0; i < 10; ++i) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + (r.nextDouble() * 2 - 1) / 4, y + 0.1,
                    pos.getZ() + 0.5 + (r.nextDouble() * 2 - 1) / 4, 0, r.nextDouble() / 20, 0);
        }
        if (r.nextInt(8) == 0) {
            level.playLocalSound(pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 0.8F + r.nextFloat() * 0.4F, false);
        }
    }
}

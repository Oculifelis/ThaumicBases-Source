package com.thaumaturgicbases.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * TB's Briar: a two-block bush with 8 stages that grows with a 1-in-4 chance per random tick. Once ripe its seed bags
 * can be picked by right-clicking (the loot table drops them from the ripe upper half), which resets both halves.
 */
public class BriarBlock extends DoublePlantBlock implements BonemealableBlock {
    public static final MapCodec<BriarBlock> CODEC = simpleCodec(BriarBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    private static final int DELAY = 4;

    public BriarBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AGE, 0));
    }

    @Override
    public MapCodec<? extends DoublePlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AGE);
    }

    private static BlockPos lower(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    private void setAge(Level level, BlockPos lower, int age) {
        BlockState bottom = level.getBlockState(lower);
        BlockState top = level.getBlockState(lower.above());
        if (bottom.is(this) && top.is(this)) {
            level.setBlock(lower, bottom.setValue(AGE, age), Block.UPDATE_ALL);
            level.setBlock(lower.above(), top.setValue(AGE, age), Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(AGE) < 7;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(DELAY) == 0) {
            setAge(level, pos, state.getValue(AGE) + 1);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < 7;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextInt(DELAY) == 0;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        setAge(level, lower(state, pos), state.getValue(AGE) + 1);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) < 7) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            BlockPos lower = lower(state, pos);
            BlockState top = level.getBlockState(lower.above());
            for (ItemStack drop : Block.getDrops(top, server, lower.above(), null, player, player.getMainHandItem())) {
                if (!drop.is(asItem())) {
                    Block.popResource(level, lower, drop);
                }
            }
            setAge(level, lower, 0);
        }
        return InteractionResult.SUCCESS;
    }
}

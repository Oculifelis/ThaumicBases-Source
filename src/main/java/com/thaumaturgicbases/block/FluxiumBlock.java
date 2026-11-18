package com.thaumaturgicbases.block;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * TB's Fluxium: a flower that gathers flux and vents it above itself. TC4 placed a Flux Gas block; Thaumaturge keeps
 * flux in the chunk aura, so each random tick with open air above adds one point of flux there.
 */
public class FluxiumBlock extends VegetationBlock {
    public static final MapCodec<FluxiumBlock> CODEC = simpleCodec(FluxiumBlock::new);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 13.6, 12);

    public FluxiumBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends VegetationBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isEmptyBlock(pos.above())) {
            AuraHelper.polluteAura(level, pos.above(), 1.0F, true);
        }
    }
}

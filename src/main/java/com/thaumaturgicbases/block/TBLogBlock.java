package com.thaumaturgicbases.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** TB's tree logs: peaceful and ender logs burn like wood (5/5); nether logs never catch fire. */
public class TBLogBlock extends RotatedPillarBlock {
    private final boolean flammable;

    public TBLogBlock(boolean flammable, Properties properties) {
        super(properties);
        this.flammable = flammable;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return flammable ? 5 : 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return flammable ? 5 : 0;
    }
}

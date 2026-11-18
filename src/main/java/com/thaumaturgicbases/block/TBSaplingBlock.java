package com.thaumaturgicbases.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** TB's saplings; the nether and ender ones also take root on their own dimension's blocks. */
public class TBSaplingBlock extends SaplingBlock {
    private final @Nullable TagKey<Block> extraSoil;

    public TBSaplingBlock(TreeGrower grower, @Nullable TagKey<Block> extraSoil, Properties properties) {
        super(grower, properties);
        this.extraSoil = extraSoil;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || extraSoil != null && state.is(extraSoil);
    }
}

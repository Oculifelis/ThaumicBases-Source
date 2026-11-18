package com.thaumaturgicbases.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A fallen Aurelia petal lying on the ground; its loot table drops the Aurelia Petal item. */
public class AureliaPetalBlock extends VegetationBlock {
    public static final MapCodec<AureliaPetalBlock> CODEC = simpleCodec(AureliaPetalBlock::new);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 4, 12);

    public AureliaPetalBlock(Properties properties) {
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
}

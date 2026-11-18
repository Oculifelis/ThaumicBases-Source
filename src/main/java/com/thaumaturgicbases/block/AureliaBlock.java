package com.thaumaturgicbases.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * TB's Aurelia: opens under a full moon (moon brightness 1) at night with open sky above, and while open sheds petals onto soil up to four
 * blocks away.
 */
public class AureliaBlock extends VegetationBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 13.6, 12);

    private final Supplier<? extends Block> petal;
    private final MapCodec<AureliaBlock> codec = MapCodec.unit(this);

    public AureliaBlock(Supplier<? extends Block> petal, Properties properties) {
        super(properties);
        this.petal = petal;
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends VegetationBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
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
        boolean open = level.canSeeSky(pos.above()) && level.isDarkOutside() && level.getMoonBrightness(pos) >= 1.0F;
        if (open != state.getValue(OPEN)) {
            level.setBlockAndUpdate(pos, state.setValue(OPEN, open));
        }
        if (!open) {
            return;
        }
        BlockPos side = pos.offset(random.nextInt(5) - random.nextInt(5), 0, random.nextInt(5) - random.nextInt(5));
        for (int dy = 1; dy >= -1; dy--) {
            BlockPos target = side.above(dy);
            if (level.isEmptyBlock(target) && mayPlaceOn(level.getBlockState(target.below()), level, target.below())) {
                level.setBlockAndUpdate(target, petal.get().defaultBlockState());
                return;
            }
        }
    }
}

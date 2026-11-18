package com.thaumaturgicbases.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * TB's Redlon: a stem that grows a Block of Redstone. A ripe stem only tries to set fruit on one random tick in four,
 * as in the original, and it sparkles like redstone.
 */
public class RedlonStemBlock extends StemBlock {
    public RedlonStemBlock(ResourceKey<Block> fruit, ResourceKey<Block> attachedStem, ResourceKey<Item> seed,
                           TagKey<Block> stemSupport, TagKey<Block> fruitSupport, Properties properties) {
        super(fruit, attachedStem, seed, stemSupport, fruitSupport, properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) == MAX_AGE && random.nextInt(4) != 0) {
            return;
        }
        super.randomTick(state, level, pos, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(DustParticleOptions.REDSTONE, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble() * 0.5,
                    pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }
}

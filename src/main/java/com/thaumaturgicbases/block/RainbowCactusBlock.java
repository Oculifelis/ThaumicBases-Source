package com.thaumaturgicbases.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;

/** TB's Rainbow Cactus: a cactus that does not hurt. Its loot table drops dyes from every part above the base. */
public class RainbowCactusBlock extends CactusBlock {
    public RainbowCactusBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
    }
}

package com.thaumaturgicbases.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Incendium (TB pyrofluid): lava that cannot spread. It crystallises over random ticks; once fully crystallised it is
 * solid, safe and breaks by hand, and its loot table drops blaze powder.
 */
public class PyrofluidBlock extends Block {
    public static final int SOLID = 8;
    public static final IntegerProperty CRYSTAL = IntegerProperty.create("crystal", 0, SOLID);

    public PyrofluidBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CRYSTAL, 0));
    }

    public static boolean isSolid(BlockState state) {
        return state.getValue(CRYSTAL) >= SOLID;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRYSTAL);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !isSolid(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int next = state.getValue(CRYSTAL) + 1;
        level.setBlock(pos, state.setValue(CRYSTAL, next), Block.UPDATE_ALL);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isSolid(state) ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return isSolid(state) ? super.getDestroyProgress(state, player, level, pos) : 0.0F;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!isSolid(state)) {
            effectApplier.apply(InsideBlockEffectType.LAVA_IGNITE);
            effectApplier.runAfter(InsideBlockEffectType.LAVA_IGNITE, Entity::lavaHurt);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (isSolid(state) || !level.getBlockState(pos.above()).isAir()) {
            return;
        }
        if (random.nextInt(100) == 0) {
            double x = pos.getX() + random.nextDouble(), y = pos.getY() + 1.0, z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0, 0.0, 0.0);
            level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.2F + random.nextFloat() * 0.2F,
                    0.9F + random.nextFloat() * 0.15F, false);
        }
        if (random.nextInt(200) == 0) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS,
                    0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
        }
    }
}

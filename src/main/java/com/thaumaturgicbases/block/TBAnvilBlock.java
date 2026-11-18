package com.thaumaturgicbases.block;

import com.leclowndu93150.thaumaturge.api.infusion.IInfusionStabiliser;
import com.thaumaturgicbases.menu.TBAnvilMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * TB's Thaumic Anvil and Void Anvil. Both keep their wear in one block ({@link #DAMAGE} 0-2, TB's metadata) instead of
 * three blocks, open {@link TBAnvilMenu}, and steady infusions. The void anvil mends one step of wear per random tick.
 */
public class TBAnvilBlock extends AnvilBlock implements IInfusionStabiliser {
    public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, 2);
    public final boolean isVoid;

    public TBAnvilBlock(Properties props, boolean isVoid) {
        super(props);
        this.isVoid = isVoid;
        registerDefaultState(defaultBlockState().setValue(DAMAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DAMAGE);
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, player) -> new TBAnvilMenu(id, inv, ContainerLevelAccess.create(level, pos), this),
                Component.translatable(getDescriptionId()));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return isVoid && state.getValue(DAMAGE) > 0;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isVoid && state.getValue(DAMAGE) > 0) level.setBlock(pos, state.setValue(DAMAGE, state.getValue(DAMAGE) - 1), 3);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        super.animateTick(state, level, pos, r);
        if (isVoid) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    pos.getX() + r.nextFloat(), pos.getY() + r.nextFloat(), pos.getZ() + r.nextFloat(), 0, -0.1, 0);
        }
    }

    @Override
    public boolean canStabiliseInfusion(Level level, BlockPos pos) {
        return true;
    }
}

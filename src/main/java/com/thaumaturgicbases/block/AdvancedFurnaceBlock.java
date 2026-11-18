package com.thaumaturgicbases.block;

import com.leclowndu93150.thaumaturge.content.essentia.smeltery.BlockEntitySmelter;
import com.leclowndu93150.thaumaturge.content.essentia.smeltery.BlockSmelter;
import com.thaumaturgicbases.TBDevices;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * TB's Advanced Alchemical Furnace: Thaumaturge's essentia smelter (same menu, alembics and bellows) whose base speed
 * comes from the {@code thaumaturge:smelter_stats} data map entry for this block, sped up while it burns a fuel listed
 * in the {@code thaumaturgicbases:furnace_fuel_boost} item data map (alumentum by default, as in TB).
 */
public class AdvancedFurnaceBlock extends BlockSmelter {
    public AdvancedFurnaceBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != TBDevices.ADVANCED_FURNACE_BE.get() ? null
                : (l, p, s, be) -> {
                    AdvancedFurnaceBlockEntity furnace = (AdvancedFurnaceBlockEntity) be;
                    furnace.checkFuel();
                    BlockEntitySmelter.staticTick(l, p, s, furnace);
                };
    }
}

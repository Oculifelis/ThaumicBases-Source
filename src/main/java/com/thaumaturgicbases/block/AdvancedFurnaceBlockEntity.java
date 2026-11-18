package com.thaumaturgicbases.block;

import com.leclowndu93150.thaumaturge.content.essentia.smeltery.BlockEntitySmelter;
import com.thaumaturgicbases.TBDataMaps;
import com.thaumaturgicbases.TBDevices;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Remembers the boost of the fuel item it last lit (TB's isFuelAlumentum) and divides the smelt interval by it. */
public class AdvancedFurnaceBlockEntity extends BlockEntitySmelter {
    private static final int FUEL_SLOT = 1;
    private int boost = 1;

    public AdvancedFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(TBDevices.ADVANCED_FURNACE_BE.get(), pos, state);
    }

    /** Called before each smelter tick: while nothing burns, the fuel waiting in the slot decides the next boost. */
    void checkFuel() {
        if (fuelRemaining == 0) {
            var fuel = itemSlots().getResource(FUEL_SLOT);
            Integer value = fuel.isEmpty() ? null : fuel.typeHolder().getData(TBDataMaps.FURNACE_FUEL_BOOST);
            boost = value == null ? 1 : Math.max(1, value);
        }
    }

    @Override
    public int ventInterval() {
        return Math.max(1, super.ventInterval() / boost);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thaumaturgicbases.advanced_alchemical_furnace");
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("FuelBoost", boost);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        boost = in.getIntOr("FuelBoost", 1);
    }
}

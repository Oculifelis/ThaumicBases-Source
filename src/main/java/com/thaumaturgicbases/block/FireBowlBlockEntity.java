package com.thaumaturgicbases.block;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.essentia.EssentiaTransportHelper;
import com.leclowndu93150.thaumaturge.content.infusion.EssentiaSources;
import com.thaumaturgicbases.TBDataMaps;
import com.thaumaturgicbases.TBDevices;
import com.thaumaturgicbases.entity.AvoidFireBowlGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/** TB TileCampfire / TileBrazier: burn timers, the lit state, and scaring mobs every 10 ticks while lit. */
public class FireBowlBlockEntity extends BlockEntity {
    private static final int BRAZIER_ESSENTIA = 6;
    private static final int BRAZIER_BURN = 1600;
    private int burnTime;
    private int logTime;
    private EssentiaSources sources;

    public FireBowlBlockEntity(BlockPos pos, BlockState state) {
        super(TBDevices.FIRE_BOWL_BE.get(), pos, state);
    }

    private boolean isBrazier() {
        return getBlockState().getBlock() instanceof FireBowlBlock b && b.brazier;
    }

    boolean addLog(ItemStack stack) {
        Integer time = stack.getItem().builtInRegistryHolder().getData(TBDataMaps.CAMPFIRE_LOGS);
        if (logTime > 0 || time == null) return false;
        logTime = time;
        update();
        return true;
    }

    boolean addFuel(ItemStack stack) {
        if (logTime <= 0 || burnTime > 0 || level == null) return false;
        int time = level.fuelValues().burnDuration(stack);
        if (time <= 0) return false;
        burnTime = time;
        update();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FireBowlBlockEntity be) {
        if (be.isBrazier()) {
            if (be.burnTime > 0) {
                --be.burnTime;
            } else {
                if (be.sources == null) be.sources = new EssentiaSources(pos);
                Holder<IAspect> ignis = EssentiaTransportHelper.resolve(level, TTAspects.IGNIS);
                if (ignis != null && level.getGameTime() % 20 == 0 && be.sources.drain((ServerLevel) level, ignis, BRAZIER_ESSENTIA)) {
                    be.burnTime = BRAZIER_BURN;
                }
            }
        } else if (be.burnTime > 0) {
            --be.burnTime;
            if (be.logTime > 0) --be.logTime;
        }
        be.update();
        if (be.burnTime > 0 && (be.isBrazier() || be.logTime > 0) && level.getGameTime() % 10 == 0) {
            for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class, new AABB(pos).inflate(12, 6, 12))) {
                AvoidFireBowlGoal.scare(mob, pos);
            }
        }
    }

    /** Keeps the blockstate in step with the timers (TB's metadata 0/1/2) and saves when it changes. */
    private void update() {
        if (level == null) return;
        BlockState state = getBlockState();
        boolean logs = isBrazier() || logTime > 0;
        boolean lit = burnTime > 0 && logs;
        if (state.getValue(FireBowlBlock.LOGS) != logs || state.getValue(FireBowlBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(FireBowlBlock.LOGS, logs).setValue(FireBowlBlock.LIT, lit), 3);
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("BurnTime", burnTime);
        out.putInt("LogTime", logTime);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        burnTime = in.getIntOr("BurnTime", 0);
        logTime = in.getIntOr("LogTime", 0);
    }
}

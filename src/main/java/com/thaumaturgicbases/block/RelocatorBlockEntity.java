package com.thaumaturgicbases.block;

import com.thaumaturgicbases.TBDevices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** TB TileRelocator: measures its free range every 100 ticks and nudges entities in it by 0.1 blocks/tick. */
public class RelocatorBlockEntity extends BlockEntity {
    private int checkCount;
    private int power = -1;

    public RelocatorBlockEntity(BlockPos pos, BlockState state) {
        super(TBDevices.RELOCATOR_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RelocatorBlockEntity be) {
        Direction facing = state.getValue(RelocatorBlock.FACING);
        if (be.power < 0 || be.checkCount++ % 100 == 0) be.power = measure(level, pos, facing);
        if (level.hasNeighborSignal(pos) || be.power <= 0) return;
        BlockPos front = pos.relative(facing);
        if (level.getBlockState(front).isFaceSturdy(level, front, facing.getOpposite())) return;

        boolean inverse = ((RelocatorBlock) state.getBlock()).inverse;
        AABB box = new AABB(pos).expandTowards(facing.getStepX() * be.power, facing.getStepY() * be.power, facing.getStepZ() * be.power);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box)) {
            // Players move on their own client; everything else moves on the server.
            if ((e instanceof Player) != level.isClientSide()) continue;
            boolean towards = inverse != e.isShiftKeyDown();
            double s = towards ? -0.1 : 0.1;
            e.setDeltaMovement(e.getDeltaMovement().add(facing.getStepX() * s, facing.getStepY() * s, facing.getStepZ() * s));
            if (facing.getStepY() * s < 0) e.resetFallDistance();
        }
    }

    /** Free blocks in front, up to 10 per relocator in the column behind (this one included). */
    private static int measure(Level level, BlockPos pos, Direction facing) {
        int chain = 0;
        while (level.getBlockState(pos.relative(facing.getOpposite(), chain)).getBlock() instanceof RelocatorBlock) ++chain;
        int power = 0;
        for (int i = 1; i <= chain * 10; ++i) {
            BlockPos p = pos.relative(facing, i);
            if (level.getBlockState(p).isCollisionShapeFullBlock(level, p)) break;
            ++power;
        }
        return power;
    }
}

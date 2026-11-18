package com.thaumaturgicbases.block;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * TB's Crying Obsidian: a column of exactly two blocks is an obelisk that sets the player's respawn point (TB
 * BlockCryingObelisk). The player wakes on the first free spot within 2 blocks, as from a bed.
 */
public class CryingObeliskBlock extends Block {
    /** 0 = plain block, 1 = obelisk bottom, 2 = obelisk top; only picks the textures (TB chose icons from neighbours). */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);

    public CryingObeliskBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    private int part(LevelReader level, BlockPos pos) {
        BlockPos base = obeliskBase(level, pos);
        return base == null ? 0 : base.equals(pos) ? 1 : 2;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(PART, part(ctx.getLevel(), ctx.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction dir, BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return dir.getAxis() == Direction.Axis.Y ? state.setValue(PART, part(level, pos)) : state;
    }

    /** The lower block of a two-high obelisk for {@code pos}, or null when {@code pos} is not part of one. */
    private BlockPos obeliskBase(BlockGetter level, BlockPos pos) {
        boolean up = level.getBlockState(pos.above()).is(this);
        boolean down = level.getBlockState(pos.below()).is(this);
        if (up && !down && !level.getBlockState(pos.above(2)).is(this)) return pos;
        if (down && !up && !level.getBlockState(pos.below(2)).is(this)) return pos.below();
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos base = obeliskBase(level, pos);
        if (base == null) return InteractionResult.PASS;
        level.playSound(player, pos, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.2F, 2.0F);
        if (player instanceof ServerPlayer sp) {
            sp.setRespawnPosition(new ServerPlayer.RespawnConfig(
                    LevelData.RespawnData.of(level.dimension(), base, sp.getYRot(), sp.getXRot()), false), false);
            sp.sendSystemMessage(Component.translatable("thaumaturgicbases.spawn_set").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.entity.LivingEntity sleeper) {
        return obeliskBase(level, pos) != null;
    }

    @Override
    public Optional<ServerPlayer.RespawnPosAngle> getRespawnPosition(BlockState state, EntityType<?> type, LevelReader level, BlockPos pos, float yaw) {
        if (obeliskBase(level, pos) == null) return Optional.empty();
        for (int dy = 0; dy > -3; --dy) {
            for (int dx = -2; dx <= 2; ++dx) {
                for (int dz = -2; dz <= 2; ++dz) {
                    BlockPos floor = pos.offset(dx, dy, dz);
                    if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                            && !level.getBlockState(floor.above()).blocksMotion()
                            && !level.getBlockState(floor.above(2)).blocksMotion()) {
                        return Optional.of(new ServerPlayer.RespawnPosAngle(Vec3.atBottomCenterOf(floor.above()), yaw, 0.0F));
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (obeliskBase(level, pos) == null) return;
        for (int i = 0; i < 10; ++i) {
            double rise = r.nextDouble() * 3;
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.5 + (r.nextDouble() * 2 - 1), pos.getY() - 3 + rise,
                    pos.getZ() + 0.5 + (r.nextDouble() * 2 - 1), 0, rise, 0);
        }
    }
}

package com.thaumaturgicbases.block;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicbases.TBBloodyMess;
import com.thaumaturgicbases.TBDataMaps;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * TB BlockSpike (iron, thaumic, void). How hard each one hits lives in the {@code thaumaturgicbases:spike} block data map.
 * A kill stains it bloody; using Thaumaturge fabric on a bloody spike wipes it and gives bloody cloth.
 */
public class SpikeBlock extends Block {
    public static final BooleanProperty BLOODY = BooleanProperty.create("bloody");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);
    private static final GameProfile SPIKE_PROFILE = new GameProfile(UUID.fromString("5e1e6a1c-7b1f-4a52-9b2f-0d1c7a6b5e00"), "[TB]Spikes");

    /** {@code damage}; {@code as_player}: hits as a fake player so mobs drop player loot; {@code redstone_off}; {@code hurts_items}. */
    public record Stats(float damage, boolean asPlayer, boolean redstoneOff, boolean hurtsItems, Optional<TagKey<Item>> safeBoots) {
        public static final Codec<Stats> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("damage").forGetter(Stats::damage),
                Codec.BOOL.optionalFieldOf("as_player", false).forGetter(Stats::asPlayer),
                Codec.BOOL.optionalFieldOf("redstone_off", false).forGetter(Stats::redstoneOff),
                Codec.BOOL.optionalFieldOf("hurts_items", false).forGetter(Stats::hurtsItems),
                TagKey.codec(Registries.ITEM).optionalFieldOf("safe_boots").forGetter(Stats::safeBoots)
        ).apply(i, Stats::new));
    }

    public SpikeBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(BLOODY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BLOODY);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.getBlockState(pos.below()).isAir();
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbourState, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
        if (!(level instanceof ServerLevel server)) return;
        Stats stats = state.getBlock().builtInRegistryHolder().getData(TBDataMaps.SPIKE);
        if (stats == null) return;
        if (stats.redstoneOff() && level.hasNeighborSignal(pos)) return;
        if (!stats.hurtsItems() && (entity instanceof ItemEntity || entity instanceof ExperienceOrb)) return;
        if (stats.safeBoots().isPresent() && entity instanceof LivingEntity living
                && living.getItemBySlot(EquipmentSlot.FEET).is(stats.safeBoots().get())) return;
        DamageSource source = stats.asPlayer()
                ? server.damageSources().playerAttack(FakePlayerFactory.get(server, SPIKE_PROFILE))
                : server.damageSources().cactus();
        entity.hurtServer(server, source, stats.damage());
        if (!state.getValue(BLOODY) && entity instanceof LivingEntity living && living.isDeadOrDying()) {
            level.setBlock(pos, state.setValue(BLOODY, true), Block.UPDATE_ALL);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(BLOODY) || !stack.is(TBBloodyMess.CLEANS_SPIKES)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            stack.consume(1, player);
            player.getInventory().placeItemBackInInventory(new ItemStack(TBBloodyMess.BLOODY_CLOTH.get()));
            level.setBlock(pos, state.setValue(BLOODY, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }
}

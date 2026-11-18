package com.thaumaturgicbases.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * TB's BlockTBPlant and its simple subclasses. Every plant uses age 0-7; a 4-stage plant moves two ages per stage, so
 * its models (and the original's growth timing) follow {@link #stage}. Drops come from the block's loot table.
 * Right-clicking a ripe plant drops its loot in place (minus one seed unless {@link Spec#keepSeed}) and replants it.
 */
public class TBPlantBlock extends CropBlock {
    private static final VoxelShape BUSH = Block.box(4, 0, 4, 12, 12, 12);

    /**
     * @param stages     4 or 8 growth stages, as in the original
     * @param delay      the original's growthDelay: chance per random tick is 1 / (delay / growthSpeed + 1)
     * @param soil       blocks the plant may stand on
     * @param crop       crop shape (true) or a small bush (false)
     * @param harvest    right-click harvesting when ripe
     * @param keepSeed   right-click harvest also yields the seed (TB's Knose and Sweed)
     * @param spreads    when ripe, sows itself onto a neighbouring soil block each random tick (TB's Sweed)
     * @param bedding    a block tag that must be two blocks below (TB's Lucrite needs Salis Mundus under the farmland)
     * @param sturdySoil any full top face counts as soil (TB's Primal Shroom)
     * @param bonemeal   bone meal stages: the original gave 1-3, Lucrite and Primal Shroom 1
     */
    public record Spec(int stages, int delay, TagKey<Block> soil, boolean crop, boolean harvest, boolean keepSeed,
                       boolean spreads, @Nullable TagKey<Block> bedding, boolean sturdySoil, int bonemeal) {
        public static Spec crop(int stages, int delay, TagKey<Block> soil) {
            return new Spec(stages, delay, soil, true, true, false, false, null, false, 3);
        }

        public static Spec bush(int stages, int delay, TagKey<Block> soil) {
            return new Spec(stages, delay, soil, false, true, false, false, null, false, 3);
        }

        public Spec withKeepSeed() {
            return new Spec(stages, delay, soil, crop, harvest, true, spreads, bedding, sturdySoil, bonemeal);
        }

        public Spec withSpreads() {
            return new Spec(stages, delay, soil, crop, harvest, keepSeed, true, bedding, sturdySoil, bonemeal);
        }

        public Spec withBedding(TagKey<Block> tag) {
            return new Spec(stages, delay, soil, crop, harvest, keepSeed, spreads, tag, sturdySoil, 1);
        }

        public Spec withSturdySoil() {
            return new Spec(stages, delay, soil, crop, false, keepSeed, spreads, bedding, true, 1);
        }
    }

    private final Spec spec;
    private final Supplier<? extends ItemLike> seed;
    private final MapCodec<TBPlantBlock> codec = MapCodec.unit(this);

    public TBPlantBlock(Spec spec, Supplier<? extends ItemLike> seed, Properties properties) {
        super(properties);
        this.spec = spec;
        this.seed = seed;
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return codec;
    }

    public Spec spec() {
        return spec;
    }

    /** Growth stage (0 .. stages-1) shown for an age. */
    public static int stage(int age, int stages) {
        return age >= 7 ? stages - 1 : Math.min(stages - 1, age * stages / 8);
    }

    private int ageOfStage(int stage) {
        return stage >= spec.stages - 1 ? 7 : stage * 8 / spec.stages;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return spec.crop ? super.getShape(state, level, pos, context) : BUSH;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return spec.sturdySoil ? state.isFaceSturdy(level, pos, Direction.UP) : state.is(spec.soil);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        if (!mayPlaceOn(level.getBlockState(below), level, below)) {
            return false;
        }
        return spec.bedding == null || level.getBlockState(below.below()).is(spec.bedding);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return spec.spreads || !isMaxAge(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        if (isMaxAge(state)) {
            if (spec.spreads) {
                spread(level, pos, random);
            }
            return;
        }
        if (level.getRawBrightness(pos.above(), 0) >= 9) {
            float f = getGrowthSpeed(state, level, pos);
            boolean grow = random.nextInt((int) (spec.delay / f) + 1) == 0;
            if (net.neoforged.neoforge.common.CommonHooks.canCropGrow(level, pos, state, grow)) {
                level.setBlock(pos, getStateForAge(ageOfStage(stage(getAge(state), spec.stages) + 1)), Block.UPDATE_CLIENTS);
                net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(level, pos, state);
            }
        }
    }

    private void spread(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos side = pos.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
        for (int dy = 1; dy >= -1; dy--) {
            BlockPos target = side.above(dy);
            if (level.isEmptyBlock(target) && mayPlaceOn(level.getBlockState(target.below()), level, target.below())) {
                level.setBlockAndUpdate(target, defaultBlockState());
                return;
            }
        }
    }

    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        int stage = stage(getAge(state), spec.stages) + 1 + level.getRandom().nextInt(spec.bonemeal);
        level.setBlock(pos, getStateForAge(ageOfStage(Math.min(stage, spec.stages - 1))), Block.UPDATE_CLIENTS);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!spec.harvest || !isMaxAge(state)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            List<ItemStack> drops = Block.getDrops(state, server, pos, null, player, player.getMainHandItem());
            boolean seedTaken = spec.keepSeed;
            for (ItemStack drop : drops) {
                if (!seedTaken && drop.is(seed.get().asItem())) {
                    drop.shrink(1);
                    seedTaken = true;
                }
                if (!drop.isEmpty()) {
                    Block.popResource(level, pos, drop);
                }
            }
            level.setBlock(pos, getStateForAge(0), Block.UPDATE_CLIENTS);
        }
        return InteractionResult.SUCCESS;
    }
}

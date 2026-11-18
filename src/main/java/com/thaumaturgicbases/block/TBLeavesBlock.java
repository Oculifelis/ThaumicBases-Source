package com.thaumaturgicbases.block;

import com.thaumaturgicbases.ThaumaturgicBases;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * TB's four tree leaves. Golden oak leaves only sparkle (their loot table carries the golden apples). Peaceful leaves
 * bring the local biome's animals; nether and ender leaves turn the ground beneath them (the
 * {@code thaumaturgicbases:nether_tree_converts} / {@code ender_tree_converts} tags) into nether or end blocks and let that
 * dimension's mobs through. To keep a farm from growing without bound, no mob appears while 12 or more living
 * entities are within 8 blocks (the original had no cap).
 */
public class TBLeavesBlock extends UntintedParticleLeavesBlock {
    public enum Effect { GOLDEN, PEACEFUL, NETHER, ENDER }

    private static final int CROWD = 12;
    public static final TagKey<Block> NETHER_CONVERTS = TagKey.create(Registries.BLOCK, ThaumaturgicBases.id("nether_tree_converts"));
    public static final TagKey<Block> ENDER_CONVERTS = TagKey.create(Registries.BLOCK, ThaumaturgicBases.id("ender_tree_converts"));
    public static final TagKey<Block> NETHER_SOIL = TagKey.create(Registries.BLOCK, ThaumaturgicBases.id("nether_sapling_soil"));
    public static final TagKey<Block> ENDER_SOIL = TagKey.create(Registries.BLOCK, ThaumaturgicBases.id("ender_sapling_soil"));

    private final Effect effect;

    public TBLeavesBlock(Effect effect, ParticleOptions particle, Properties properties) {
        super(0.01F, particle, properties);
        this.effect = effect;
    }

    // Vanilla leaves burn at 30/60; nether leaves never catch fire (fire on them burns forever: minecraft:infiniburn_*).
    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return effect == Effect.NETHER ? 0 : 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return effect == Effect.NETHER ? 0 : 30;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return effect != Effect.GOLDEN || super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (!level.getBlockState(pos).is(this)) {
            return;
        }
        switch (effect) {
            case PEACEFUL -> {
                if (random.nextDouble() <= 0.03D) {
                    BlockPos ground = groundBelow(level, pos, 6);
                    if (ground != null) {
                        spawn(level, ground, level.getBiome(ground).value(), MobCategory.CREATURE, random);
                    }
                }
            }
            case NETHER -> otherworld(level, pos, random, 8, NETHER_SOIL, NETHER_CONVERTS, Biomes.NETHER_WASTES, 0.05D);
            case ENDER -> otherworld(level, pos, random, 11, ENDER_SOIL, ENDER_CONVERTS, Biomes.THE_END, 0.02D);
            default -> {
            }
        }
    }

    private static @Nullable BlockPos groundBelow(Level level, BlockPos pos, int depth) {
        for (int dy = 1; dy <= depth; dy++) {
            BlockPos p = pos.below(dy);
            if (!level.getBlockState(p).isAir()) {
                return p;
            }
        }
        return null;
    }

    private void otherworld(ServerLevel level, BlockPos pos, RandomSource random, int depth, TagKey<Block> home,
                            TagKey<Block> converts, ResourceKey<Biome> biome, double spawnChance) {
        BlockPos ground = groundBelow(level, pos, depth);
        if (ground == null) {
            return;
        }
        BlockState below = level.getBlockState(ground);
        if (below.is(home)) {
            if (random.nextDouble() <= spawnChance) {
                Optional<Biome> source = level.registryAccess().lookupOrThrow(Registries.BIOME).getOptional(biome);
                source.ifPresent(b -> spawn(level, ground, b, random.nextBoolean() ? MobCategory.CREATURE : MobCategory.MONSTER, random));
            }
        } else if (below.is(converts)) {
            float r = random.nextFloat();
            Block to = effect == Effect.NETHER
                    ? (r <= 0.6F ? Blocks.NETHERRACK : r <= 0.9F ? Blocks.SOUL_SAND : Blocks.NETHER_QUARTZ_ORE)
                    : (r <= 0.9F ? Blocks.END_STONE : Blocks.OBSIDIAN);
            level.setBlockAndUpdate(ground, to.defaultBlockState());
        }
    }

    private static void spawn(ServerLevel level, BlockPos ground, Biome biome, MobCategory category, RandomSource random) {
        if (level.getEntitiesOfClass(LivingEntity.class, new AABB(ground).inflate(8)).size() >= CROWD) {
            return;
        }
        Optional<MobSpawnSettings.SpawnerData> data = biome.getMobSettings().getMobs(category).getRandom(random);
        if (data.isEmpty()) {
            return;
        }
        Entity entity = data.get().type().create(level, EntitySpawnReason.NATURAL);
        if (!(entity instanceof Mob mob)) {
            if (entity != null) {
                entity.discard();
            }
            return;
        }
        mob.snapTo(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        if (level.noCollision(mob) && !level.containsAnyLiquid(mob.getBoundingBox())) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(ground), EntitySpawnReason.NATURAL, null);
            level.addFreshEntity(mob);
        } else {
            mob.discard();
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        double x = pos.getX() + random.nextDouble(), y = pos.getY() + random.nextDouble(), z = pos.getZ() + random.nextDouble();
        switch (effect) {
            case GOLDEN -> {
                if (random.nextInt(4) == 0) {
                    level.addParticle(ParticleTypes.WAX_ON, x, y, z, 0.0, 0.0, 0.0);
                }
            }
            case PEACEFUL -> {
                if (random.nextInt(8) == 0) {
                    level.addParticle(ParticleTypes.HEART, x, y, z, 0.0, 0.0, 0.0);
                }
            }
            case NETHER -> {
                if (random.nextFloat() <= 0.01F && level.isEmptyBlock(pos.below())) {
                    level.addParticle(ParticleTypes.DRIPPING_LAVA, x, pos.getY(), z, 0.0, 0.0, 0.0);
                }
            }
            case ENDER -> level.addParticle(ParticleTypes.PORTAL, x, y, z, random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        }
    }
}

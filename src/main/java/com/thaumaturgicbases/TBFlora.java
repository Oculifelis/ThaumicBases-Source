package com.thaumaturgicbases;

import com.thaumaturgicbases.block.AshroomBlock;
import com.thaumaturgicbases.block.AureliaBlock;
import com.thaumaturgicbases.block.AureliaPetalBlock;
import com.thaumaturgicbases.block.BriarBlock;
import com.thaumaturgicbases.block.FluxiumBlock;
import com.thaumaturgicbases.block.RainbowCactusBlock;
import com.thaumaturgicbases.block.RedlonStemBlock;
import com.thaumaturgicbases.block.TBLeavesBlock;
import com.thaumaturgicbases.block.TBLogBlock;
import com.thaumaturgicbases.block.TBPlantBlock;
import com.thaumaturgicbases.block.TBPlantBlock.Spec;
import com.thaumaturgicbases.block.TBSaplingBlock;
import com.thaumaturgicbases.item.KnoseFragmentItem;
import com.thaumaturgicbases.item.RosehipSyrupItem;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Phase 3: TB's plants, the four special saplings and their trees. Soil for each plant is the block tag
 * {@code thaumaturgicbases:soil/<plant>}; drops, tree shapes and the trees' ground conversion are data.
 */
public final class TBFlora {
    private TBFlora() {
    }

    static TagKey<Block> soil(String plant) {
        return TagKey.create(Registries.BLOCK, ThaumaturgicBases.id("soil/" + plant));
    }

    private static ResourceKey<Block> blockKey(String name) {
        return ResourceKey.create(Registries.BLOCK, ThaumaturgicBases.id(name));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> tree(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ThaumaturgicBases.id(name));
    }

    private static BlockBehaviour.Properties plant(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties flower(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY);
    }

    // Seeds are block items named after themselves, like vanilla wheat seeds.
    private static DeferredItem<BlockItem> seeds(String name, DeferredBlock<? extends Block> crop) {
        return TBRegistry.item(name, p -> new BlockItem(crop.get(), p), Item.Properties::useItemDescriptionPrefix);
    }

    private static DeferredBlock<TBPlantBlock> crop(String name, Spec spec, String seedName) {
        Holder seed = new Holder();
        DeferredBlock<TBPlantBlock> block = TBRegistry.BLOCKS.registerBlock(name, p -> new TBPlantBlock(spec, () -> seed.item.get(), p), TBFlora::plant);
        seed.item = seeds(seedName, block);
        return block;
    }

    private static final class Holder {
        DeferredItem<BlockItem> item;
    }

    // Crops and bushes (TB.BasicPlants and its children). Delays and stages are the original's.
    public static final DeferredBlock<TBPlantBlock> PLAX = crop("plax", Spec.crop(8, 4, soil("plax")), "plax_seeds");
    public static final DeferredBlock<TBPlantBlock> METALLEAT = crop("metalleat", Spec.crop(8, 8, soil("metalleat")), "metalleat_seeds");
    public static final DeferredBlock<TBPlantBlock> LUCRITE = crop("lucrite",
            Spec.crop(8, 16, soil("lucrite")).withBedding(TagKey.create(Registries.BLOCK, ThaumaturgicBases.id("lucrite_bedding"))), "lucrite_seeds");
    public static final DeferredBlock<TBPlantBlock> LAZULLIA = crop("lazullia", Spec.crop(8, 16, soil("lazullia")), "lazullia_seeds");
    public static final DeferredBlock<TBPlantBlock> VOID_PLANT = crop("void_plant", Spec.crop(4, 32, soil("void_plant")), "void_plant_seed");
    public static final DeferredBlock<TBPlantBlock> KNOSE = crop("knose", Spec.bush(4, 16, soil("knose")).withKeepSeed(), "knose_seed");
    public static final DeferredBlock<TBPlantBlock> SWEED = crop("sweed", Spec.bush(4, 4, soil("sweed")).withKeepSeed().withSpreads(), "sweed_seeds");
    public static final DeferredBlock<TBPlantBlock> TOBACCO = crop("tobacco", Spec.crop(8, 16, soil("tobacco")), "tobacco_seeds");
    public static final DeferredBlock<TBPlantBlock> GLIEONIA = crop("glieonia", Spec.bush(4, 12, soil("glieonia")), "glieonia_seed");

    // The Primal Shroom is planted as itself (TB dropped the block as its own seed).
    public static final DeferredBlock<AshroomBlock> ASHROOM = TBRegistry.block("ashroom",
            p -> new AshroomBlock(Spec.bush(4, 8, soil("ashroom")).withSturdySoil(), TBFlora::ashroomItem, p),
            p -> plant(p).lightLevel(s -> 4));

    private static Item ashroomItem() {
        return ASHROOM.get().asItem();
    }

    public static final DeferredBlock<BriarBlock> BRIAR = TBRegistry.BLOCKS.registerBlock("briar", BriarBlock::new,
            p -> p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.SWEET_BERRY_BUSH)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<DoubleHighBlockItem> BRIAR_ITEM = TBRegistry.item("briar", p -> new DoubleHighBlockItem(BRIAR.get(), p),
            Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredItem<Item> BRIAR_SEEDBAG = TBRegistry.item("briar_seedbag", Item::new);
    public static final DeferredItem<RosehipSyrupItem> ROSEHIP_SYRUP = TBRegistry.item("rosehip_syrup", RosehipSyrupItem::new,
            p -> p.stacksTo(16).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK));

    public static final DeferredBlock<AureliaPetalBlock> AURELIA_PETAL_BLOCK = TBRegistry.BLOCKS.registerBlock("fallen_aurelia_petal",
            AureliaPetalBlock::new, p -> flower(p).lightLevel(s -> 4).replaceable());
    public static final DeferredBlock<AureliaBlock> AURELIA = TBRegistry.block("aurelia",
            p -> new AureliaBlock(AURELIA_PETAL_BLOCK, p), p -> flower(p).randomTicks().lightLevel(s -> 7));
    public static final DeferredItem<Item> AURELIA_PETAL = TBRegistry.item("aurelia_petal", Item::new);

    public static final DeferredBlock<FluxiumBlock> FLUXIUM = TBRegistry.block("fluxium", FluxiumBlock::new, p -> flower(p).randomTicks());

    public static final DeferredItem<KnoseFragmentItem> KNOSE_FRAGMENT = TBRegistry.item("knose_fragment", KnoseFragmentItem::new);

    public static final DeferredBlock<RainbowCactusBlock> RAINBOW_CACTUS = TBRegistry.block("rainbow_cactus", RainbowCactusBlock::new,
            p -> p.mapColor(MapColor.COLOR_MAGENTA).randomTicks().strength(0.5F).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY));

    // Redlon grows a vanilla Block of Redstone on any block that can carry a melon.
    private static final ResourceKey<Block> REDSTONE_BLOCK = ResourceKey.create(Registries.BLOCK,
            net.minecraft.resources.Identifier.withDefaultNamespace("redstone_block"));
    private static final ResourceKey<Item> REDLON_SEEDS_KEY = ResourceKey.create(Registries.ITEM, ThaumaturgicBases.id("redlon_seeds"));
    public static final DeferredBlock<AttachedStemBlock> ATTACHED_REDLON_STEM = TBRegistry.BLOCKS.registerBlock("attached_redlon_stem",
            p -> new AttachedStemBlock(blockKey("redlon_stem"), REDSTONE_BLOCK, REDLON_SEEDS_KEY, soil("redlon"), p),
            p -> p.mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<RedlonStemBlock> REDLON_STEM = TBRegistry.BLOCKS.registerBlock("redlon_stem",
            p -> new RedlonStemBlock(REDSTONE_BLOCK, blockKey("attached_redlon_stem"), REDLON_SEEDS_KEY, soil("redlon"),
                    TagKey.create(Registries.BLOCK, ThaumaturgicBases.id("redlon_fruit_support")), p),
            p -> p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.HARD_CROP).pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> REDLON_SEEDS = seeds("redlon_seeds", REDLON_STEM);

    // Trees (TB.GoldenOak and the three special saplings). The golden oak grows on vanilla oak logs.
    private static TreeGrower grower(String name) {
        return new TreeGrower(ThaumaturgicBases.MODID + ":" + name, Optional.empty(), Optional.of(tree(name)), Optional.empty());
    }

    private static UnaryOperator<BlockBehaviour.Properties> sapling() {
        return p -> p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY);
    }

    private static UnaryOperator<BlockBehaviour.Properties> leaves(MapColor color, int light) {
        return p -> p.mapColor(color).strength(0.2F).randomTicks().sound(SoundType.GRASS).noOcclusion().lightLevel(s -> light)
                .isValidSpawn((s, l, pos, e) -> false).isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false)
                .ignitedByLava().pushReaction(PushReaction.DESTROY).isRedstoneConductor((s, l, pos) -> false);
    }

    private static UnaryOperator<BlockBehaviour.Properties> log(MapColor color) {
        return p -> p.mapColor(color).strength(2.0F).sound(SoundType.WOOD);
    }

    private static ParticleOptions leafParticle(int rgb) {
        return ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, rgb);
    }

    public static final DeferredBlock<TBSaplingBlock> GOLDEN_OAK_SAPLING = TBRegistry.block("golden_oak_sapling",
            p -> new TBSaplingBlock(grower("golden_oak"), null, p), sapling());
    public static final DeferredBlock<TBSaplingBlock> PEACEFUL_SAPLING = TBRegistry.block("peaceful_sapling",
            p -> new TBSaplingBlock(grower("peaceful_tree"), null, p), sapling());
    public static final DeferredBlock<TBSaplingBlock> NETHER_SAPLING = TBRegistry.block("nether_sapling",
            p -> new TBSaplingBlock(grower("nether_tree"), TBLeavesBlock.NETHER_SOIL, p), sapling());
    public static final DeferredBlock<TBSaplingBlock> ENDER_SAPLING = TBRegistry.block("ender_sapling",
            p -> new TBSaplingBlock(grower("ender_tree"), TBLeavesBlock.ENDER_SOIL, p), sapling());

    public static final DeferredBlock<TBLogBlock> PEACEFUL_LOG = TBRegistry.block("peaceful_log", p -> new TBLogBlock(true, p), log(MapColor.COLOR_PINK));
    public static final DeferredBlock<TBLogBlock> NETHER_LOG = TBRegistry.block("nether_log", p -> new TBLogBlock(false, p), log(MapColor.NETHER));
    public static final DeferredBlock<TBLogBlock> ENDER_LOG = TBRegistry.block("ender_log", p -> new TBLogBlock(true, p), log(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<TBLogBlock> PEACEFUL_WOOD = TBRegistry.block("peaceful_wood", p -> new TBLogBlock(true, p), log(MapColor.COLOR_PINK));
    public static final DeferredBlock<TBLogBlock> STRIPPED_PEACEFUL_LOG = TBRegistry.block("stripped_peaceful_log", p -> new TBLogBlock(true, p), log(MapColor.COLOR_PINK));
    public static final DeferredBlock<TBLogBlock> STRIPPED_PEACEFUL_WOOD = TBRegistry.block("stripped_peaceful_wood", p -> new TBLogBlock(true, p), log(MapColor.COLOR_PINK));
    public static final DeferredBlock<TBLogBlock> NETHER_WOOD = TBRegistry.block("nether_wood", p -> new TBLogBlock(false, p), log(MapColor.NETHER));
    public static final DeferredBlock<TBLogBlock> STRIPPED_NETHER_LOG = TBRegistry.block("stripped_nether_log", p -> new TBLogBlock(false, p), log(MapColor.NETHER));
    public static final DeferredBlock<TBLogBlock> STRIPPED_NETHER_WOOD = TBRegistry.block("stripped_nether_wood", p -> new TBLogBlock(false, p), log(MapColor.NETHER));
    public static final DeferredBlock<TBLogBlock> ENDER_WOOD = TBRegistry.block("ender_wood", p -> new TBLogBlock(true, p), log(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<TBLogBlock> STRIPPED_ENDER_LOG = TBRegistry.block("stripped_ender_log", p -> new TBLogBlock(true, p), log(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<TBLogBlock> STRIPPED_ENDER_WOOD = TBRegistry.block("stripped_ender_wood", p -> new TBLogBlock(true, p), log(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<Block> ENDER_PLANKS = TBRegistry.block("ender_planks", Block::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 1200.0F).sound(SoundType.WOOD));

    public static final DeferredBlock<TBLeavesBlock> GOLDEN_OAK_LEAVES = TBRegistry.block("golden_oak_leaves",
            p -> new TBLeavesBlock(TBLeavesBlock.Effect.GOLDEN, leafParticle(0xE8C14A), p), leaves(MapColor.GOLD, 0));
    public static final DeferredBlock<TBLeavesBlock> PEACEFUL_LEAVES = TBRegistry.block("peaceful_leaves",
            p -> new TBLeavesBlock(TBLeavesBlock.Effect.PEACEFUL, leafParticle(0xF2A7C8), p), leaves(MapColor.COLOR_PINK, 0));
    public static final DeferredBlock<TBLeavesBlock> NETHER_LEAVES = TBRegistry.block("nether_leaves",
            p -> new TBLeavesBlock(TBLeavesBlock.Effect.NETHER, leafParticle(0xB0402A), p), leaves(MapColor.NETHER, 0));
    public static final DeferredBlock<TBLeavesBlock> ENDER_LEAVES = TBRegistry.block("ender_leaves",
            p -> new TBLeavesBlock(TBLeavesBlock.Effect.ENDER, leafParticle(0x5A2E7A), p), leaves(MapColor.COLOR_PURPLE, 11));

    /** Loads this class so its entries join the registers before they are attached to the bus. */
    static void init() {
    }
}

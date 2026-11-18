package com.thaumaturgicbases;

import com.leclowndu93150.thaumaturge.content.wands.ItemWandCap;
import com.leclowndu93150.thaumaturge.content.wands.ItemWandRod;
import com.thaumaturgicbases.block.PyrofluidBlock;
import com.thaumaturgicbases.item.PyrofluidBucketItem;
import com.thaumaturgicbases.item.VisGearArmorItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Replaces DummyCore's BlocksRegistry/ItemRegistry: plain DeferredRegisters plus a creative tab fed in order. */
public final class TBRegistry {
    private static final String MODID = ThaumaturgicBases.MODID;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    private static final List<Supplier<? extends Item>> TAB_ITEMS = new ArrayList<>();
    /** Items whose creative-tab stacks carry components (bracelet kinds, node foci, revolver). */
    public static final List<Function<net.minecraft.core.HolderLookup.Provider, List<ItemStack>>> TAB_STACKS = new ArrayList<>();

    /** TB crystal block colours, in the original metadata order. */
    public static final List<String> CRYSTALS = List.of("air", "fire", "water", "earth", "order", "entropy", "mixed", "tainted");

    // Decorative blocks (TB.DecoBlocks)
    public static final DeferredBlock<Block> QUICKSILVER_BLOCK = block("quicksilver_block", Block::new,
            p -> p.mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> QUICKSILVER_BRICK = block("quicksilver_brick", Block::new,
            p -> p.mapColor(MapColor.METAL).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> ELDRITCH_ARK = block("eldritch_ark", Block::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(3.0f, 12.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<SlabBlock> ELDRITCH_ARK_SLAB = block("eldritch_ark_slab", SlabBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(3.0f, 12.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> IRON_GREATWOOD = block("iron_greatwood", Block::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.0f, 6.0f).sound(SoundType.WOOD));

    // Thauminite (TB.Thauminite)
    public static final DeferredItem<Item> THAUMINITE_NUGGET = item("thauminite_nugget", Item::new);
    public static final DeferredItem<Item> THAUMINITE_INGOT = item("thauminite_ingot", Item::new, p -> p.trimMaterial(TBMaterials.TRIM_THAUMINITE));
    public static final DeferredBlock<Block> THAUMINITE_BLOCK = block("thauminite_block", Block::new,
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(5.0f, 8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredItem<Item> THAUMINITE_SWORD = item("thauminite_sword", Item::new,
            p -> p.sword(TBMaterials.TOOL_THAUMINITE, 3.0F, -2.4F));
    public static final DeferredItem<Item> THAUMINITE_PICKAXE = item("thauminite_pickaxe", Item::new,
            p -> p.pickaxe(TBMaterials.TOOL_THAUMINITE, 1.0F, -2.8F));
    public static final DeferredItem<AxeItem> THAUMINITE_AXE = item("thauminite_axe",
            p -> new AxeItem(TBMaterials.TOOL_THAUMINITE, 5.0F, -3.0F, p));
    public static final DeferredItem<ShovelItem> THAUMINITE_SHOVEL = item("thauminite_shovel",
            p -> new ShovelItem(TBMaterials.TOOL_THAUMINITE, 1.5F, -3.0F, p));
    public static final DeferredItem<HoeItem> THAUMINITE_HOE = item("thauminite_hoe",
            p -> new HoeItem(TBMaterials.TOOL_THAUMINITE, -3.0F, 0.0F, p));
    public static final DeferredItem<ShearsItem> THAUMINITE_SHEARS = item("thauminite_shears", ShearsItem::new,
            p -> p.durability(974).component(DataComponents.TOOL, ShearsItem.createToolProperties()));
    public static final DeferredItem<VisGearArmorItem> THAUMINITE_HELMET = item("thauminite_helmet", VisGearArmorItem::new,
            p -> p.humanoidArmor(TBMaterials.ARMOR_THAUMINITE, ArmorType.HELMET));
    public static final DeferredItem<VisGearArmorItem> THAUMINITE_CHESTPLATE = item("thauminite_chestplate", VisGearArmorItem::new,
            p -> p.humanoidArmor(TBMaterials.ARMOR_THAUMINITE, ArmorType.CHESTPLATE));
    public static final DeferredItem<VisGearArmorItem> THAUMINITE_LEGGINGS = item("thauminite_leggings", VisGearArmorItem::new,
            p -> p.humanoidArmor(TBMaterials.ARMOR_THAUMINITE, ArmorType.LEGGINGS));
    public static final DeferredItem<VisGearArmorItem> THAUMINITE_BOOTS = item("thauminite_boots", VisGearArmorItem::new,
            p -> p.humanoidArmor(TBMaterials.ARMOR_THAUMINITE, ArmorType.BOOTS));

    // Wand parts (CAP_thauminite, ROD_tbthaumium, ROD_tbvoid)
    public static final DeferredItem<ItemWandCap> WAND_CAP_THAUMINITE = item("wand_cap_thauminite",
            p -> new ItemWandCap(p, TBWandParts.CAP_THAUMINITE));
    public static final DeferredItem<ItemWandRod> WAND_ROD_THAUMIUM = item("wand_rod_thaumium",
            p -> new ItemWandRod(p, TBWandParts.ROD_THAUMIUM));
    public static final DeferredItem<ItemWandRod> WAND_ROD_VOID = item("wand_rod_void",
            p -> new ItemWandRod(p, TBWandParts.ROD_VOID));

    // Alchemy (TB.SMB, TB.CrystalBlocks, TB.Pyrofluid)
    public static final DeferredBlock<Block> SALIS_MUNDUS_BLOCK = block("salis_mundus_block", Block::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(1.0f).sound(SoundType.SAND));
    public static final Map<String, DeferredBlock<TransparentBlock>> CRYSTAL_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<SlabBlock>> CRYSTAL_SLABS = new LinkedHashMap<>();

    static {
        for (String c : CRYSTALS) {
            CRYSTAL_BLOCKS.put(c, block("crystal_block_" + c, TransparentBlock::new, TBRegistry::crystal));
        }
        for (String c : CRYSTALS) {
            CRYSTAL_SLABS.put(c, block("crystal_slab_" + c, SlabBlock::new, TBRegistry::crystal));
        }
    }

    public static final DeferredBlock<PyrofluidBlock> PYROFLUID = BLOCKS.registerBlock("pyrofluid", PyrofluidBlock::new,
            p -> p.mapColor(MapColor.FIRE).strength(5.0f).lightLevel(s -> 15).noOcclusion().randomTicks()
                    .pushReaction(PushReaction.BLOCK).sound(SoundType.AMETHYST));
    public static final DeferredItem<PyrofluidBucketItem> PYROFLUID_BUCKET = item("pyrofluid_bucket", PyrofluidBucketItem::new,
            p -> p.stacksTo(1).craftRemainder(Items.BUCKET));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thaumaturgicbases"))
            .icon(() -> new ItemStack(THAUMINITE_INGOT.get()))
            .displayItems((params, out) -> {
                TAB_ITEMS.forEach(i -> out.accept(i.get()));
                TAB_STACKS.forEach(s -> s.apply(params.holders()).forEach(out::accept));
            })
            .build());

    private TBRegistry() {
    }

    private static BlockBehaviour.Properties crystal(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.7f, 1.0f).sound(SoundType.AMETHYST).lightLevel(s -> 8)
                .noOcclusion().isValidSpawn((s, l, pos, e) -> false).isRedstoneConductor((s, l, pos) -> false)
                .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false);
    }

    static <I extends Item> DeferredItem<I> item(String name, Function<Item.Properties, I> factory) {
        return item(name, factory, UnaryOperator.identity());
    }

    static <I extends Item> DeferredItem<I> item(String name, Function<Item.Properties, I> factory, UnaryOperator<Item.Properties> props) {
        DeferredItem<I> item = ITEMS.registerItem(name, factory, props);
        TAB_ITEMS.add(item);
        return item;
    }

    static <B extends Block> DeferredBlock<B> block(String name, Function<BlockBehaviour.Properties, B> factory,
                                                    UnaryOperator<BlockBehaviour.Properties> props) {
        DeferredBlock<B> block = BLOCKS.registerBlock(name, factory, props);
        TAB_ITEMS.add(ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }
}

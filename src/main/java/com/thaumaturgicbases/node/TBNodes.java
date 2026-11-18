package com.thaumaturgicbases.node;

import com.thaumaturgicbases.TBRegistry;
import com.thaumaturgicbases.ThaumaturgicBases;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Phase 6 node devices: the Node Manipulator, its foci (a datapack registry) and the Node Linker. */
public final class TBNodes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumaturgicBases.MODID);

    public static final DeferredBlock<NodeManipulatorBlock> NODE_MANIPULATOR = TBRegistry.BLOCKS.registerBlock("node_manipulator",
            NodeManipulatorBlock::new, p -> p.mapColor(MapColor.STONE).strength(2.0F, 10.0F).sound(SoundType.STONE).noOcclusion()
                    .requiresCorrectToolForDrops());
    public static final DeferredBlock<NodeLinkerBlock> NODE_LINKER = TBRegistry.BLOCKS.registerBlock("node_linker",
            NodeLinkerBlock::new, p -> p.mapColor(MapColor.STONE).strength(2.0F, 10.0F).sound(SoundType.STONE).noOcclusion()
                    .requiresCorrectToolForDrops());
    public static final DeferredItem<?> NODE_MANIPULATOR_ITEM = TBRegistry.ITEMS.registerSimpleBlockItem(NODE_MANIPULATOR);
    public static final DeferredItem<?> NODE_LINKER_ITEM = TBRegistry.ITEMS.registerSimpleBlockItem(NODE_LINKER);
    public static final DeferredItem<NodeFocusItem> NODE_FOCUS = TBRegistry.ITEMS.registerItem("node_focus", NodeFocusItem::new, p -> p.stacksTo(1));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NodeManipulatorBlockEntity>> NODE_MANIPULATOR_BE =
            BLOCK_ENTITIES.register("node_manipulator", () -> new BlockEntityType<>(NodeManipulatorBlockEntity::new, NODE_MANIPULATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NodeLinkerBlockEntity>> NODE_LINKER_BE =
            BLOCK_ENTITIES.register("node_linker", () -> new BlockEntityType<>(NodeLinkerBlockEntity::new, NODE_LINKER.get()));

    static {
        TBRegistry.TAB_STACKS.add(r -> java.util.List.of(new net.minecraft.world.item.ItemStack(NODE_MANIPULATOR_ITEM.get()),
                new net.minecraft.world.item.ItemStack(NODE_LINKER_ITEM.get())));
        TBRegistry.TAB_STACKS.add(r -> NodeFocusItem.all(NODE_FOCUS.get(), r));
    }

    private TBNodes() {
    }

    /** Loads the class so its blocks and items join the registers before they are registered. */
    public static void init() {
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        bus.addListener(DataPackRegistryEvent.NewRegistry.class,
                e -> e.dataPackRegistry(NodeFocus.REGISTRY_KEY, NodeFocus.CODEC, NodeFocus.CODEC));
    }
}

package com.thaumaturgicbases;

import com.leclowndu93150.thaumaturge.content.essentia.smeltery.BlockSmelter;
import com.thaumaturgicbases.block.AdvancedFurnaceBlock;
import com.thaumaturgicbases.block.AdvancedFurnaceBlockEntity;
import com.thaumaturgicbases.block.CryingObeliskBlock;
import com.thaumaturgicbases.block.DeconstructorBlock;
import com.thaumaturgicbases.block.DeconstructorBlockEntity;
import com.thaumaturgicbases.block.FireBowlBlock;
import com.thaumaturgicbases.block.FireBowlBlockEntity;
import com.thaumaturgicbases.block.OverchanterBlock;
import com.thaumaturgicbases.block.OverchanterBlockEntity;
import com.thaumaturgicbases.block.RelocatorBlock;
import com.thaumaturgicbases.block.RelocatorBlockEntity;
import com.thaumaturgicbases.block.TBAnvilBlock;
import com.thaumaturgicbases.entity.AvoidFireBowlGoal;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Phase 4 devices (TB.AdvAlc, TB.CryingObs, TB.Relocator, the anvils, TB.Campfire, TB.Brazier, TB.EntityDec, TB.Overchanter). */
public final class TBDevices {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumaturgicBases.MODID);
    /** Mobs that get {@link AvoidFireBowlGoal}: TB scared every IMob creature. */
    public static final TagKey<EntityType<?>> FEARS_FIRE = TagKey.create(Registries.ENTITY_TYPE, ThaumaturgicBases.id("fears_fire"));

    public static final DeferredBlock<AdvancedFurnaceBlock> ADVANCED_FURNACE = TBRegistry.block("advanced_alchemical_furnace", AdvancedFurnaceBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(2.0F, 3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(BlockSmelter.LIT) ? 12 : 0));
    public static final DeferredBlock<CryingObeliskBlock> CRYING_OBSIDIAN = TBRegistry.block("crying_obsidian", CryingObeliskBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(10.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().lightLevel(s -> 6));
    public static final DeferredBlock<RelocatorBlock> RELOCATOR = TBRegistry.block("relocator", p -> new RelocatorBlock(p, false), TBDevices::relocator);
    public static final DeferredBlock<RelocatorBlock> INVERSE_RELOCATOR = TBRegistry.block("inverse_relocator", p -> new RelocatorBlock(p, true), TBDevices::relocator);
    public static final DeferredBlock<TBAnvilBlock> THAUMIC_ANVIL = TBRegistry.block("thaumic_anvil", p -> new TBAnvilBlock(p, false),
            p -> p.mapColor(MapColor.METAL).strength(6.0F, 16.0F).sound(SoundType.ANVIL).requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<TBAnvilBlock> VOID_ANVIL = TBRegistry.block("void_anvil", p -> new TBAnvilBlock(p, true),
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 45.0F).sound(SoundType.ANVIL).requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<FireBowlBlock> CAMPFIRE = TBRegistry.block("campfire", p -> new FireBowlBlock(p, false),
            p -> p.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion().lightLevel(s -> s.getValue(FireBowlBlock.LIT) ? 15 : 0));
    public static final DeferredBlock<FireBowlBlock> BRAZIER = TBRegistry.block("brazier", p -> new FireBowlBlock(p, true),
            p -> p.mapColor(MapColor.STONE).strength(1.0F).sound(SoundType.STONE).noOcclusion().lightLevel(s -> s.getValue(FireBowlBlock.LIT) ? 15 : 0));
    public static final DeferredBlock<DeconstructorBlock> ENTITY_DECONSTRUCTOR = TBRegistry.block("entity_deconstructor", DeconstructorBlock::new,
            p -> p.mapColor(MapColor.GOLD).strength(1.0F).sound(SoundType.STONE).noOcclusion());
    public static final DeferredBlock<OverchanterBlock> OVERCHANTER = TBRegistry.block("overchanter", OverchanterBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(5.0F, 5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().lightLevel(s -> 7));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedFurnaceBlockEntity>> ADVANCED_FURNACE_BE =
            BLOCK_ENTITIES.register("advanced_alchemical_furnace", () -> new BlockEntityType<>(AdvancedFurnaceBlockEntity::new, ADVANCED_FURNACE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RelocatorBlockEntity>> RELOCATOR_BE =
            BLOCK_ENTITIES.register("relocator", () -> new BlockEntityType<>(RelocatorBlockEntity::new, RELOCATOR.get(), INVERSE_RELOCATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FireBowlBlockEntity>> FIRE_BOWL_BE =
            BLOCK_ENTITIES.register("fire_bowl", () -> new BlockEntityType<>(FireBowlBlockEntity::new, CAMPFIRE.get(), BRAZIER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeconstructorBlockEntity>> DECONSTRUCTOR_BE =
            BLOCK_ENTITIES.register("entity_deconstructor", () -> new BlockEntityType<>(DeconstructorBlockEntity::new, ENTITY_DECONSTRUCTOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OverchanterBlockEntity>> OVERCHANTER_BE =
            BLOCK_ENTITIES.register("overchanter", () -> new BlockEntityType<>(OverchanterBlockEntity::new, OVERCHANTER.get()));

    private TBDevices() {
    }

    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties relocator(net.minecraft.world.level.block.state.BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.WOOD).strength(2.5F, 15.0F).sound(SoundType.WOOD);
    }

    static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, event -> {
            if (!event.getLevel().isClientSide() && event.getEntity() instanceof PathfinderMob mob && mob.getType().builtInRegistryHolder().is(FEARS_FIRE)) {
                mob.goalSelector.addGoal(1, new AvoidFireBowlGoal(mob));
            }
        });
    }
}

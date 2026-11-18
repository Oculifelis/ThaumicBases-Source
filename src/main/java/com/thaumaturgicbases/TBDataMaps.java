package com.thaumaturgicbases;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** Item tunables that live in datapacks (data/thaumaturgicbases/data_maps/item/*.json) instead of code. */
public final class TBDataMaps {
    /** Percent vis discount granted by worn gear (thauminite armour, bloody robes...). */
    public static final DataMapType<Item, Integer> VIS_DISCOUNT = DataMapType.builder(ThaumaturgicBases.id("vis_discount"), Registries.ITEM, Codec.INT)
            .synced(Codec.INT, false)
            .build();

    /** Advanced Alchemical Furnace: fuels that divide its smelt interval while they burn (TB: alumentum, x2). */
    public static final DataMapType<Item, Integer> FURNACE_FUEL_BOOST = DataMapType.builder(ThaumaturgicBases.id("furnace_fuel_boost"), Registries.ITEM, Codec.INT)
            .build();

    /** Campfire: items accepted as its log, with how many burning ticks one lasts (TB: logs 1600, magical logs 6400). */
    public static final DataMapType<Item, Integer> CAMPFIRE_LOGS = DataMapType.builder(ThaumaturgicBases.id("campfire_logs"), Registries.ITEM, Codec.INT)
            .build();

    /** Entity Deconstructor: the research category each primal aspect teaches (block data map on the deconstructor). */
    public static final DataMapType<Block, Map<ResourceKey<IAspect>, Identifier>> ASPECT_KNOWLEDGE = DataMapType.builder(
                    ThaumaturgicBases.id("aspect_knowledge"), Registries.BLOCK,
                    Codec.unboundedMap(ResourceKey.codec(IAspect.REGISTRY_KEY), Identifier.CODEC))
            .build();

    /** What smoking a tobacco does (TB TBTobacco); synced so the client knows which items a pipe can smoke. */
    public static final DataMapType<Item, com.thaumaturgicbases.item.TobaccoEffect> TOBACCO = DataMapType.builder(ThaumaturgicBases.id("tobacco"), Registries.ITEM,
                    com.thaumaturgicbases.item.TobaccoEffect.CODEC)
            .synced(com.thaumaturgicbases.item.TobaccoEffect.CODEC, false)
            .build();

    /** Spike damage and rules per spike block (TB BlockSpike: iron 8, thaumic 14, void 20 as a player). */
    public static final DataMapType<Block, com.thaumaturgicbases.block.SpikeBlock.Stats> SPIKE = DataMapType.builder(ThaumaturgicBases.id("spike"), Registries.BLOCK,
                    com.thaumaturgicbases.block.SpikeBlock.Stats.CODEC)
            .build();

    /** Ticks per point of self-repair for items carried by a player (TB void tools: 20). */
    public static final DataMapType<Item, Integer> SELF_REPAIR = DataMapType.builder(ThaumaturgicBases.id("self_repair"), Registries.ITEM, Codec.INT)
            .build();

    private TBDataMaps() {
    }

    public static int visDiscount(ItemStack stack) {
        Integer value = stack.getItem().builtInRegistryHolder().getData(VIS_DISCOUNT);
        return value == null ? 0 : value;
    }

    static void register(IEventBus bus) {
        bus.addListener(RegisterDataMapTypesEvent.class, event -> {
            event.register(VIS_DISCOUNT);
            event.register(FURNACE_FUEL_BOOST);
            event.register(CAMPFIRE_LOGS);
            event.register(ASPECT_KNOWLEDGE);
            event.register(TOBACCO);
            event.register(SPIKE);
            event.register(SELF_REPAIR);
        });
    }
}

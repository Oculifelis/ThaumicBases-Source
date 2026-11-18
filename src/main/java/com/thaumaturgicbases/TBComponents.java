package com.thaumaturgicbases;

import com.leclowndu93150.thaumaturge.api.capability.KnowledgeType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.component.DataComponentType;

/** Item data components. They replace TB's item metadata. */
public final class TBComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ThaumaturgicBases.MODID);

    /** One knowledge grant: {@code amount} points of {@code type} in research category {@code category}. */
    public record Grant(KnowledgeType type, Identifier category, int amount) {
        public static final Codec<Grant> CODEC = RecordCodecBuilder.create(i -> i.group(
                KnowledgeType.CODEC.fieldOf("type").forGetter(Grant::type),
                Identifier.CODEC.fieldOf("category").forGetter(Grant::category),
                Codec.INT.fieldOf("amount").forGetter(Grant::amount)
        ).apply(i, Grant::new));
    }

    /**
     * What a Rosa Mysteria fragment teaches (TB's knoseFragment metadata). {@code variant} picks the name and the item
     * model; the loot table of the Rosa Mysteria sets the whole component from the crystal block it grew on.
     */
    public record KnoseInsight(String variant, List<Grant> grants, int warp) {
        public static final Codec<KnoseInsight> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("variant").forGetter(KnoseInsight::variant),
                Grant.CODEC.listOf().fieldOf("grants").forGetter(KnoseInsight::grants),
                Codec.INT.optionalFieldOf("warp", 0).forGetter(KnoseInsight::warp)
        ).apply(i, KnoseInsight::new));
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<KnoseInsight>> KNOSE_INSIGHT =
            COMPONENTS.registerComponentType("knose_insight",
                    b -> b.persistent(KnoseInsight.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(KnoseInsight.CODEC)));

    /** Enchantments the Overchanter has already raised on this item (TB's "overchants" NBT int array). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Holder<Enchantment>>>> OVERCHANTS =
            COMPONENTS.registerComponentType("overchants",
                    b -> b.persistent(Enchantment.CODEC.listOf())
                            .networkSynchronized(ByteBufCodecs.holderRegistry(Registries.ENCHANTMENT).apply(ByteBufCodecs.list())));

    /** The block an Activation focus remembers (TB's blockX/blockY/blockZ/dim NBT on the wand). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.GlobalPos>> ACTIVATION_LINK =
            COMPONENTS.registerComponentType("activation_link",
                    b -> b.persistent(net.minecraft.core.GlobalPos.CODEC).networkSynchronized(net.minecraft.core.GlobalPos.STREAM_CODEC));

    /** The Node Linker a caster clicked first (TB's linkCoordX/Y/Z NBT on the wand). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.GlobalPos>> LINKER_ORIGIN =
            COMPONENTS.registerComponentType("linker_origin",
                    b -> b.persistent(net.minecraft.core.GlobalPos.CODEC).networkSynchronized(net.minecraft.core.GlobalPos.STREAM_CODEC));

    /** Which node focus this is: an entry of the {@code thaumaturgicbases:node_focus} datapack registry (TB nodeFoci metadata). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> NODE_FOCUS =
            COMPONENTS.registerComponentType("node_focus",
                    b -> b.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC));

    private TBComponents() {
    }

    static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}

package com.thaumaturgicbases.revolver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thaumaturgicbases.TBRegistry;
import com.thaumaturgicbases.ThaumaturgicBases;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TB's Thaumaturge's Arsenal: the revolver, its bullet, its sounds and its upgrades. Each upgrade slot is a data
 * component holding one {@link Level} (TB's "tb.upgrades" NBT list); the upgrade recipes are infusions whose
 * catalyst_patch raises one slot by one level, so conflicting upgrades (one slot) cannot be combined.
 */
public final class TBRevolver {
    /** An upgrade and its level in one slot; {@code thaumaturgicbases:none} at 0 when the slot is empty. */
    public record Level(Identifier upgrade, int level) {
        public static final Identifier NONE_ID = ThaumaturgicBases.id("none");
        public static final Level NONE = new Level(NONE_ID, 0);
        public static final Codec<Level> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("upgrade").forGetter(Level::upgrade),
                Codec.INT.fieldOf("level").forGetter(Level::level)).apply(i, Level::new));

        public boolean empty() {
            return level <= 0 || NONE_ID.equals(upgrade);
        }
    }

    /** The slots, named after TB's conflict groups (and one each for the upgrades that conflicted with nothing). */
    public static final List<String> SLOTS = List.of("damage", "barrel", "round", "piercing", "void", "heavy");

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ThaumaturgicBases.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ThaumaturgicBases.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ThaumaturgicBases.MODID);

    public static final Map<String, DeferredHolder<DataComponentType<?>, DataComponentType<Level>>> SLOT_COMPONENTS = new LinkedHashMap<>();

    static {
        for (String slot : SLOTS) {
            SLOT_COMPONENTS.put(slot, COMPONENTS.registerComponentType("revolver_" + slot,
                    b -> b.persistent(Level.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(Level.CODEC))));
        }
    }

    /** Shots left in the cylinder (TB's "shots" NBT). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SHOTS = COMPONENTS.registerComponentType("revolver_shots",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<SoundEvent, SoundEvent> SHOT = sound("revolver_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> NO_AMMO = sound("revolver_no_ammo");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD = sound("revolver_reload");

    public static final DeferredHolder<EntityType<?>, EntityType<RevolverBullet>> BULLET = ENTITIES.register("revolver_bullet",
            () -> EntityType.Builder.<RevolverBullet>of(RevolverBullet::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(8)
                    .updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, ThaumaturgicBases.id("revolver_bullet"))));

    /** TB: 5 attack damage, 1561 uses, rare. */
    public static final DeferredItem<RevolverItem> REVOLVER = TBRegistry.ITEMS.registerItem("revolver", RevolverItem::new, p -> {
        Item.Properties out = p.stacksTo(1).durability(1561).rarity(Rarity.RARE).component(SHOTS.get(), 0)
                .attributes(ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build());
        for (DeferredHolder<DataComponentType<?>, DataComponentType<Level>> c : SLOT_COMPONENTS.values()) out = out.component(c.get(), Level.NONE);
        return out;
    });

    static {
        TBRegistry.TAB_STACKS.add(r -> List.of(new ItemStack(REVOLVER.get())));
    }

    private TBRevolver() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ThaumaturgicBases.id(name)));
    }

    public static void init() {
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        bus.addListener(DataPackRegistryEvent.NewRegistry.class,
                e -> e.dataPackRegistry(RevolverUpgrade.REGISTRY_KEY, RevolverUpgrade.CODEC, RevolverUpgrade.CODEC));
    }
}

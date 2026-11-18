package com.thaumaturgicbases;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeType;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.api.research.scan.ScanningManager;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.wands.EntityAspectOrb;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TB's five enchantments are datapack enchantments (data/thaumaturgicbases/enchantment). Eldritch Bane and Tainted are pure
 * JSON; Elder Knowledge, Magic Touch and Vaporising carry the effect components below, which TB's kill handler
 * (EnchantmentHandler.onMobDeath) reads here. Tainted also comes from a sword despawning inside a tainted node.
 */
public final class TBEnchantments {
    public static final DeferredRegister<DataComponentType<?>> EFFECTS = DeferredRegister.create(Registries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, ThaumaturgicBases.MODID);
    public static final ResourceKey<Enchantment> TAINTED = ResourceKey.create(Registries.ENCHANTMENT, ThaumaturgicBases.id("tainted"));

    /** Elder Knowledge: with {@code chance}, the victim's aspects teach observations; {@code categories} maps each primal to a category. */
    public record KillKnowledge(LevelBasedValue chance, Map<ResourceKey<IAspect>, Identifier> categories) {
        public static final Codec<KillKnowledge> CODEC = RecordCodecBuilder.create(i -> i.group(
                LevelBasedValue.CODEC.fieldOf("chance").forGetter(KillKnowledge::chance),
                Codec.unboundedMap(ResourceKey.codec(IAspect.REGISTRY_KEY), Identifier.CODEC).fieldOf("categories").forGetter(KillKnowledge::categories)
        ).apply(i, KillKnowledge::new));
    }

    /** Magic Touch: up to {@code rolls} rounds; each primal of the victim drops an aspect orb half the time. */
    public record AspectOrbs(LevelBasedValue rolls) {
        public static final Codec<AspectOrbs> CODEC = LevelBasedValue.CODEC.fieldOf("rolls").codec().xmap(AspectOrbs::new, AspectOrbs::rolls);
    }

    /** Vaporising: with {@code chance}, the victim's aspects drop as essentia crystals, stopping early with {@code stop_chance}. */
    public record CrystalDrops(LevelBasedValue chance, LevelBasedValue stopChance) {
        public static final Codec<CrystalDrops> CODEC = RecordCodecBuilder.create(i -> i.group(
                LevelBasedValue.CODEC.fieldOf("chance").forGetter(CrystalDrops::chance),
                LevelBasedValue.CODEC.fieldOf("stop_chance").forGetter(CrystalDrops::stopChance)
        ).apply(i, CrystalDrops::new));
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<KillKnowledge>> KILL_KNOWLEDGE =
            EFFECTS.register("kill_knowledge", () -> DataComponentType.<KillKnowledge>builder().persistent(KillKnowledge.CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AspectOrbs>> ASPECT_ORBS =
            EFFECTS.register("aspect_orbs", () -> DataComponentType.<AspectOrbs>builder().persistent(AspectOrbs.CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CrystalDrops>> CRYSTAL_DROPS =
            EFFECTS.register("crystal_drops", () -> DataComponentType.<CrystalDrops>builder().persistent(CrystalDrops.CODEC).build());

    private TBEnchantments() {
    }

    static void register(IEventBus bus) {
        EFFECTS.register(bus);
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, TBEnchantments::onKill);
        NeoForge.EVENT_BUS.addListener(ItemExpireEvent.class, TBEnchantments::onExpire);
    }

    private static void onKill(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || !(event.getSource().getDirectEntity() instanceof ServerPlayer player)) return;
        ItemStack weapon = player.getMainHandItem();
        ItemEnchantments enchantments = weapon.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;
        AspectList aspects = ScanningManager.entityAspects(victim);
        if (aspects == null || aspects.isEmpty()) return;
        RandomSource rand = level.getRandom();
        for (var entry : enchantments.entrySet()) {
            Enchantment ench = entry.getKey().value();
            int lvl = entry.getIntValue();
            KillKnowledge knowledge = ench.effects().get(KILL_KNOWLEDGE.get());
            if (knowledge != null && rand.nextFloat() < knowledge.chance().calculate(lvl)) {
                for (AspectInstance a : aspects.entries()) {
                    Identifier category = categoryOf(a.aspect(), knowledge.categories());
                    if (category != null) ScanningManager.addKnowledge(player, KnowledgeType.OBSERVATION, category, 1);
                    if (rand.nextBoolean()) break;
                }
            }
            AspectOrbs orbs = ench.effects().get(ASPECT_ORBS.get());
            if (orbs != null) {
                Map<Holder<IAspect>, Integer> primals = primals(aspects);
                for (int r = 0; r < (int) orbs.rolls().calculate(lvl); ++r) {
                    for (var p : primals.entrySet()) {
                        if (!rand.nextBoolean()) continue;
                        p.getKey().unwrapKey().ifPresent(key -> level.addFreshEntity(new EntityAspectOrb(level, victim.getX(), victim.getY(), victim.getZ(),
                                key, 1 + rand.nextInt(p.getValue()))));
                    }
                    if (rand.nextBoolean()) break;
                }
            }
            CrystalDrops drops = ench.effects().get(CRYSTAL_DROPS.get());
            if (drops != null && rand.nextFloat() < drops.chance().calculate(lvl)) {
                for (AspectInstance a : aspects.entries()) {
                    if (rand.nextBoolean()) {
                        ItemStack crystal = new ItemStack(TTItems.ESSENTIA_CRYSTAL.get(), Math.max(1, (1 + rand.nextInt(Math.max(1, a.amount()))) / 2));
                        crystal.set(TTDataComponents.CRYSTAL_ASPECT.get(), new AspectInstance(a.aspect(), 1));
                        level.addFreshEntity(new ItemEntity(level, victim.getX(), victim.getEyeY(), victim.getZ(), crystal));
                    }
                    if (rand.nextFloat() < drops.stopChance().calculate(lvl)) break;
                }
            }
        }
    }

    /** First category found walking an aspect down to its primals. */
    private static Identifier categoryOf(Holder<IAspect> aspect, Map<ResourceKey<IAspect>, Identifier> categories) {
        Identifier direct = aspect.unwrapKey().map(categories::get).orElse(null);
        if (direct != null) return direct;
        for (Holder<IAspect> part : aspect.value().components()) {
            Identifier c = categoryOf(part, categories);
            if (c != null) return c;
        }
        return null;
    }

    /** TB ResearchManager.reduceToPrimals: each compound splits its amount into its components. */
    private static Map<Holder<IAspect>, Integer> primals(AspectList aspects) {
        Map<Holder<IAspect>, Integer> out = new LinkedHashMap<>();
        for (AspectInstance a : aspects.entries()) addPrimals(a.aspect(), a.amount(), out);
        return out;
    }

    private static void addPrimals(Holder<IAspect> aspect, int amount, Map<Holder<IAspect>, Integer> out) {
        if (aspect.value().isPrimal() || aspect.value().components().isEmpty()) {
            out.merge(aspect, amount, Integer::sum);
            return;
        }
        for (Holder<IAspect> part : aspect.value().components()) addPrimals(part, amount, out);
    }

    /** TB itemExpire: a sword despawning inside a tainted node gains Tainted I-III, if nothing on it clashes, and 1000 more ticks. */
    private static void onExpire(ItemExpireEvent event) {
        ItemEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(entity.blockPosition()) instanceof BlockEntityNode node) || node.kind() != NodeType.TAINTED) return;
        Holder<Enchantment> tainted = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(TAINTED).orElse(null);
        ItemStack stack = entity.getItem();
        if (tainted == null || !tainted.value().canEnchant(stack) || EnchantmentHelper.getItemEnchantmentLevel(tainted, stack) > 0) return;
        ItemEnchantments current = stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> other : current.keySet()) {
            if (!Enchantment.areCompatible(tainted, other)) return;
        }
        stack.enchant(tainted, 1 + level.getRandom().nextInt(tainted.value().getMaxLevel()));
        entity.setItem(stack.copy());
        event.setExtraLife(1000);
    }
}

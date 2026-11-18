package com.thaumaturgicbases;

import com.leclowndu93150.thaumaturge.api.capability.IPlayerKnowledge;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeType;
import com.leclowndu93150.thaumaturge.api.capability.ResearchFlag;
import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import java.util.ArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Builds before the rename used the mod id "thaumicbases". Two things keep those worlds loading:
 * <ul>
 * <li>Every entry we register (blocks, items, block entities, entities, data components, attachments, menus, sounds...)
 * gets a registry alias from its old id, so chunks, inventories and saved data resolve to the new one.</li>
 * <li>Research and research points saved under the old namespace move to the new ids when a player logs in.</li>
 * </ul>
 * Datapack registry entries (enchantments, spell parts, worldgen) were renamed outright; they have no aliases.
 */
public final class LegacyIds {
    private LegacyIds() {}

    static void register(IEventBus modBus) {
        modBus.addListener(EventPriority.LOWEST, LegacyIds::aliasRegistry);
        NeoForge.EVENT_BUS.addListener(LegacyIds::migrateKnowledge);
        if (Boolean.getBoolean(ThaumaturgicBases.MODID + ".selftest")) {
            NeoForge.EVENT_BUS.addListener(LegacyIds::checkAliases);
        }
    }

    private static void aliasRegistry(RegisterEvent event) {
        Registry<?> registry = event.getRegistry();
        int count = 0;
        for (Identifier id : new ArrayList<>(registry.keySet())) {
            if (id.getNamespace().equals(ThaumaturgicBases.MODID)) {
                registry.addAlias(ThaumaturgicBases.legacyId(id.getPath().replace(ThaumaturgicBases.MODID, ThaumaturgicBases.LEGACY_MODID)), id);
                count++;
            }
        }
        if (count > 0) {
            ThaumaturgicBases.LOG.debug("Aliased {} old ids in {}", count, event.getRegistryKey().identifier());
        }
    }

    /** Development check: every item and block must still resolve from its old id. */
    private static void checkAliases(ServerStartedEvent event) {
        int ok = 0;
        java.util.List<String> bad = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (!id.getNamespace().equals(ThaumaturgicBases.MODID)) {
                continue;
            }
            Identifier old = ThaumaturgicBases.legacyId(id.getPath().replace(ThaumaturgicBases.MODID, ThaumaturgicBases.LEGACY_MODID));
            if (BuiltInRegistries.ITEM.getValue(old) == BuiltInRegistries.ITEM.getValue(id)) {
                ok++;
            } else {
                bad.add(old.toString());
            }
        }
        for (Identifier id : BuiltInRegistries.BLOCK.keySet()) {
            if (id.getNamespace().equals(ThaumaturgicBases.MODID)) {
                Identifier old = ThaumaturgicBases.legacyId(id.getPath().replace(ThaumaturgicBases.MODID, ThaumaturgicBases.LEGACY_MODID));
                if (BuiltInRegistries.BLOCK.getValue(old) == BuiltInRegistries.BLOCK.getValue(id)) {
                    ok++;
                } else {
                    bad.add(old.toString());
                }
            }
        }
        if (bad.isEmpty() && ok > 0) {
            ThaumaturgicBases.LOG.info("LEGACY ALIAS PASS: {} old item and block ids resolve", ok);
        } else {
            ThaumaturgicBases.LOG.error("LEGACY ALIAS FAIL: {} ok, unresolved {}", ok, bad);
        }
    }

    private static Identifier renamed(Identifier old) {
        return old.getNamespace().equals(ThaumaturgicBases.LEGACY_MODID) ? ThaumaturgicBases.id(old.getPath().replace(ThaumaturgicBases.LEGACY_MODID, ThaumaturgicBases.MODID)) : null;
    }

    private static void migrateKnowledge(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        IPlayerKnowledge knowledge = KnowledgeAccess.of(player);
        if (knowledge == null) {
            return;
        }
        int moved = migrate(knowledge);
        if (moved > 0) {
            knowledge.sync(player);
            ThaumaturgicBases.LOG.info("Moved {} research records of {} from the old thaumicbases ids", moved,
                    player.getGameProfile().name());
        }
    }

    /** Moves research, stages, flags, completion and research points from the old namespace. Returns records moved. */
    static int migrate(IPlayerKnowledge knowledge) {
        int moved = 0;
        for (Identifier old : new ArrayList<>(knowledge.researchList())) {
            Identifier now = renamed(old);
            if (now == null) {
                continue;
            }
            knowledge.addResearch(now);
            int stage = knowledge.researchStage(old);
            if (stage > 0) {
                knowledge.setResearchStage(now, Math.max(stage, knowledge.researchStage(now)));
            }
            for (ResearchFlag flag : ResearchFlag.values()) {
                if (knowledge.hasResearchFlag(old, flag)) {
                    knowledge.setResearchFlag(now, flag);
                }
            }
            if (knowledge.isResearchComplete(old) && knowledge instanceof PlayerKnowledge impl) {
                impl.markComplete(now);
            }
            knowledge.removeResearch(old);
            moved++;
        }
        // Research points are kept per category; the category was thaumicbases:thaumicbases.
        ResourceKey<IResearchCategory> oldCategory = ResourceKey.create(IResearchCategory.REGISTRY_KEY,
                Identifier.fromNamespaceAndPath(ThaumaturgicBases.LEGACY_MODID, "thaumicbases"));
        ResourceKey<IResearchCategory> newCategory = ResourceKey.create(IResearchCategory.REGISTRY_KEY,
                ThaumaturgicBases.id("thaumaturgicbases"));
        for (KnowledgeType type : KnowledgeType.values()) {
            int raw = knowledge.rawKnowledge(type, oldCategory);
            if (raw > 0) {
                knowledge.addKnowledge(type, newCategory, raw);
                knowledge.addKnowledge(type, oldCategory, -raw);
                moved++;
            }
        }
        return moved;
    }
}

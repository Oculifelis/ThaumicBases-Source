package com.thaumaturgicbases;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Thaumic Bases (GTNH 1.9.22), ported from the Thaumcraft 4 addon to Thaumaturge (NeoForge 26.1.2) without DummyCore. */
@Mod(ThaumaturgicBases.MODID)
public final class ThaumaturgicBases {
    public static final String MODID = "thaumaturgicbases";
    /** The mod id before the 2026-11-14 rename; see {@link LegacyIds}. */
    public static final String LEGACY_MODID = "thaumicbases";
    public static final Logger LOG = LogUtils.getLogger();

    public ThaumaturgicBases(IEventBus modBus, ModContainer container, net.neoforged.api.distmarker.Dist dist) {
        LegacyIds.register(modBus);
        TBConfig.register(modBus, container);
        SelfTest.register();
        TBFlora.init();
        TBTobacco.init();
        TBBloodyMess.init();
        TBTools.init();
        TBBracelets.init();
        com.thaumaturgicbases.node.TBNodes.init();
        com.thaumaturgicbases.revolver.TBRevolver.init();
        TBRegistry.register(modBus);
        TBComponents.register(modBus);
        TBWandParts.register(modBus);
        TBDataMaps.register(modBus);
        TBDevices.register(modBus);
        TBEnchantments.register(modBus);
        com.thaumaturgicbases.spell.TBSpells.register(modBus);
        com.thaumaturgicbases.node.TBNodes.register(modBus);
        com.thaumaturgicbases.revolver.TBRevolver.register(modBus);
        if (dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            com.thaumaturgicbases.client.TBClient.init(modBus);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    /** The same path under the old mod id. */
    public static Identifier legacyId(String path) {
        return Identifier.fromNamespaceAndPath(LEGACY_MODID, path);
    }
}

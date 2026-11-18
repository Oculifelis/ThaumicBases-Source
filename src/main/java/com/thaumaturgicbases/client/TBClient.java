package com.thaumaturgicbases.client;

import com.thaumaturgicbases.revolver.TBRevolver;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client registrations: the revolver bullet draws nothing itself (its trail is server-sent particles, as TB's sparkle). */
public final class TBClient {
    private TBClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
                e -> e.registerEntityRenderer(TBRevolver.BULLET.get(), NoopRenderer::new));
    }
}

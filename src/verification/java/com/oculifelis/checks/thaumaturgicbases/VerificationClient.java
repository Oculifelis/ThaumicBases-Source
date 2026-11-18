package com.oculifelis.checks.thaumaturgicbases;

import com.leclowndu93150.thaumaturge.client.screen.research.ThaumonomiconBrowserScreen;
import com.leclowndu93150.thaumaturge.content.research.ResearchGrants;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/** A fresh-world and research-tab check in the separate development mod. */
@EventBusSubscriber(modid = VerificationChecks.ID, value = Dist.CLIENT)
public final class VerificationClient {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String TARGET = System.getProperty("oculifelis.checks.client");
    private static final String WORLD = "check-" + TARGET + "-" + Long.toUnsignedString(System.currentTimeMillis(), 36);
    private static final long START = System.nanoTime();
    private static final AtomicInteger GRAB_CANCELLATIONS = new AtomicInteger();
    private static final AtomicBoolean GRANTED = new AtomicBoolean();
    private static final AtomicBoolean CAPTURED = new AtomicBoolean();
    private static volatile Throwable serverFailure;
    private static boolean configured, creating, finished, grantQueued, opened, checked, shotQueued;
    private static int idleTicks, worldTicks, openedAt, shotAt, entries;
    private static String screenshot;

    private VerificationClient() {}

    public static void noteGrabCancellation() {
        GRAB_CANCELLATIONS.incrementAndGet();
    }

    @SubscribeEvent
    private static void tick(ClientTickEvent.Post event) {
        if (TARGET == null || finished) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        try {
            mc.mouseHandler.releaseMouse();
            if (!configured) {
                Path expected = Path.of(System.getProperty("oculifelis.checks.expectedDirectory")).toAbsolutePath().normalize();
                Path actual = mc.gameDirectory.toPath().toAbsolutePath().normalize();
                if (!expected.equals(actual)) throw new IllegalStateException("Wrong check directory: " + actual + " expected " + expected);
                mc.options.pauseOnLostFocus = false;
                mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
                mc.options.framerateLimit().set(30);
                mc.options.renderDistance().set(4);
                mc.options.simulationDistance().set(5);
                GLFW.glfwSetWindowPos(mc.getWindow().handle(), -4000, -4000);
                configured = true;
                LOG.info("CLIENTCHECK {} armed in {}", TARGET, actual);
            }
            if (System.nanoTime() - START > 8L * 60L * 1_000_000_000L) throw new IllegalStateException("Client check timeout");
            step(mc);
        } catch (Throwable failure) {
            finished = true;
            LOG.error("CLIENTCHECK FAIL " + TARGET, failure);
            mc.mouseHandler.releaseMouse();
            mc.stop();
        }
    }

    private static void step(Minecraft mc) throws Exception {
        if (mc.level == null || mc.player == null) {
            if (!creating && mc.getOverlay() == null && mc.screen != null && !(mc.screen instanceof TitleScreen) && ++idleTicks > 200) {
                mc.setScreen(new TitleScreen());
                idleTicks = 0;
            }
            if (!creating && mc.screen instanceof TitleScreen title && mc.getOverlay() == null) {
                creating = true;
                LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
                LOG.info("CLIENTCHECK {} creating fresh world {}", TARGET, WORLD);
                mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261113L, false, false), WorldPresets::createFlatWorldDimensions, title);
            }
            return;
        }
        worldTicks++;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        if (worldTicks == 1) LOG.info("CLIENTCHECK {} world joined", TARGET);
        if (worldTicks == 40) {
            int before = GRAB_CANCELLATIONS.get();
            mc.mouseHandler.grabMouse();
            if (GRAB_CANCELLATIONS.get() <= before || mc.mouseHandler.isMouseGrabbed()) throw new IllegalStateException("Mouse guard did not cancel the deliberate grab");
            LOG.info("CLIENTCHECK {} mouse grab cancelled", TARGET);
        }
        if (worldTicks >= 60 && !grantQueued) {
            grantQueued = true;
            var server = mc.getSingleplayerServer();
            if (server == null) throw new IllegalStateException("No integrated server");
            var uuid = mc.player.getUUID();
            server.execute(() -> {
                try {
                    ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                    if (player == null) throw new IllegalStateException("No server player");
                    int count = ResearchGrants.grantAll(player);
                    LOG.info("CLIENTCHECK {} research preparation: {} grants", TARGET, count);
                    GRANTED.set(true);
                } catch (Throwable failure) {
                    serverFailure = failure;
                }
            });
        }
        if (serverFailure != null) throw new IllegalStateException("Research preparation failed", serverFailure);
        if (worldTicks >= 100 && GRANTED.get() && !opened) {
            Identifier category = Identifier.fromNamespaceAndPath(TARGET, TARGET);
            ThaumonomiconBrowserScreen.rememberEntry(null);
            Field remembered = ThaumonomiconBrowserScreen.class.getDeclaredField("rememberedCategory");
            remembered.setAccessible(true);
            remembered.set(null, category);
            var item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "thaumonomicon"));
            if (item == Items.AIR) throw new IllegalStateException("Thaumonomicon item is missing");
            mc.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            opened = true;
            openedAt = worldTicks;
        }
        if (opened && !checked && worldTicks >= openedAt + 30) {
            if (!(mc.screen instanceof ThaumonomiconBrowserScreen browser)) throw new IllegalStateException("Book screen did not open: " + mc.screen);
            Field activeField = ThaumonomiconBrowserScreen.class.getDeclaredField("active");
            activeField.setAccessible(true);
            Object active = activeField.get(browser);
            if (active == null) throw new IllegalStateException("No active research category");
            Method id = active.getClass().getDeclaredMethod("id");
            id.setAccessible(true);
            Identifier selected = (Identifier) id.invoke(active);
            Identifier wanted = Identifier.fromNamespaceAndPath(TARGET, TARGET);
            if (!wanted.equals(selected)) throw new IllegalStateException("Wrong research tab " + selected + " expected " + wanted);
            Field modelField = ThaumonomiconBrowserScreen.class.getDeclaredField("model");
            modelField.setAccessible(true);
            Object model = modelField.get(browser);
            Method shown = model.getClass().getDeclaredMethod("shown", active.getClass());
            shown.setAccessible(true);
            if (!Boolean.TRUE.equals(shown.invoke(model, active))) throw new IllegalStateException("The selected tab is hidden");
            Method entriesOf = model.getClass().getDeclaredMethod("entriesOf", Identifier.class);
            entriesOf.setAccessible(true);
            entries = ((List<?>) entriesOf.invoke(model, wanted)).size();
            if (entries == 0) throw new IllegalStateException("The selected tab has no entries");
            checked = true;
            LOG.info("CLIENTCHECK {} selected tab {} with {} entries", TARGET, selected, entries);
        }
        if (checked && !shotQueued) {
            shotQueued = true;
            shotAt = worldTicks;
            screenshot = "verification-" + TARGET + "-" + System.currentTimeMillis() + ".png";
            Screenshot.grab(mc.gameDirectory, screenshot, mc.getMainRenderTarget(), 1, message -> {
                LOG.info("CLIENTCHECK SCREENSHOT {}", new File(new File(mc.gameDirectory, "screenshots"), screenshot).getAbsolutePath());
                CAPTURED.set(true);
            });
        }
        if (checked && CAPTURED.get() && worldTicks >= shotAt + 20) {
            finished = true;
            LOG.info("CLIENTCHECK PASS {} world, visible tab, {} entries, mouse guard", TARGET, entries);
            mc.stop();
        }
    }
}

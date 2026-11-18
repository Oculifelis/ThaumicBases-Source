package com.oculifelis.checks.thaumaturgicbases;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/** Development checks loaded from the verification source set, separate from the ordinary mod jar. */
@Mod(VerificationChecks.ID)
public final class VerificationChecks {
    public static final String ID = "thaumaturgicbases_checks";
    private static final String TARGET = "thaumaturgicbases";
    private static final Logger LOG = LogUtils.getLogger();
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, ID);
    private static final List<String> TESTS = List.of("registered_items_have_names_and_models", "recipe_resources_load");
    static {
        FUNCTIONS.register(TESTS.get(0), () -> VerificationChecks::items);
        FUNCTIONS.register(TESTS.get(1), () -> VerificationChecks::recipes);
    }

    public VerificationChecks(IEventBus bus) {
        FUNCTIONS.register(bus);
        bus.addListener(VerificationChecks::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, id(name));
            event.registerTest(id(name), new FunctionGameTestInstance(function, new TestData<>(environment, id("arena"), 40, 0, true)));
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    private static JsonObject asset(String path) {
        try (InputStream stream = com.thaumaturgicbases.ThaumaturgicBases.class.getResourceAsStream("/assets/" + TARGET + "/" + path)) {
            if (stream == null) throw new IllegalStateException("Missing asset " + TARGET + ":" + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(path, ex);
        }
    }

    private static void items(GameTestHelper h) {
        JsonObject lang = asset("lang/en_us.json");
        int count = 0;
        for (var item : BuiltInRegistries.ITEM) {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            if (!TARGET.equals(key.getNamespace())) continue;
            String description = item.getDescriptionId();
            h.assertTrue(lang.has(description) && !lang.get(description).getAsString().isBlank(), "English name for " + key + " (" + description + ")");
            JsonObject model = asset("items/" + key.getPath() + ".json");
            h.assertTrue(model.has("model"), "Item model definition for " + key);
            count++;
        }
        h.assertTrue(count > 0, "The mod registers items");
        LOG.info("CHECKS {} item registry, English names and model definitions: {} checked", TARGET, count);
        h.succeed();
    }

    private static void recipes(GameTestHelper h) {
        var manager = h.getLevel().getServer().getRecipeManager();
        int required = 0, conditionalLoaded = 0, conditionalAbsent = 0;
        var resources = h.getLevel().getServer().getResourceManager().listResources("recipe", key -> TARGET.equals(key.getNamespace()) && key.getPath().endsWith(".json"));
        for (var entry : resources.entrySet()) {
            Identifier file = entry.getKey();
            JsonObject json;
            try (var reader = entry.getValue().openAsReader()) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (java.io.IOException ex) {
                throw new IllegalStateException(file.toString(), ex);
            }
            String path = file.getPath().substring("recipe/".length(), file.getPath().length() - ".json".length());
            var key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(TARGET, path));
            boolean loaded = manager.byKey(key).isPresent();
            if (json.has("neoforge:conditions")) {
                if (loaded) conditionalLoaded++; else conditionalAbsent++;
            } else {
                h.assertTrue(loaded, "Recipe decoded and loaded: " + file);
                required++;
            }
        }
        h.assertTrue(required > 0, "The mod has unconditional recipes to verify");
        LOG.info("CHECKS {} recipes: {} required loaded, {} conditional loaded, {} conditional absent (conditions left to the loader)", TARGET, required, conditionalLoaded, conditionalAbsent);
        h.succeed();
    }
}

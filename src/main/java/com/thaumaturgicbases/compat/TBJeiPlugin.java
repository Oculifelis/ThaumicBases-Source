package com.thaumaturgicbases.compat;

import com.leclowndu93150.thaumaturge.compat.jei.category.AspectFromStacksCategory;
import com.thaumaturgicbases.TBDevices;
import com.thaumaturgicbases.ThaumaturgicBases;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * JEI: the Advanced Alchemical Furnace is a catalyst on Thaumaturge's essentia smelting page and the two anvils on the
 * vanilla anvil page; information pages cover the plant and tree drops, the bloody cloth, Herobrine's Scythe and the
 * devices whose use is not a recipe. Every recipe here is a vanilla or Thaumaturge type, which their plugins show.
 * Loaded only with JEI (Thaumaturge's own JEI classes load with it).
 */
@JeiPlugin
public final class TBJeiPlugin implements IModPlugin {
    private static final String[] INFO = {"knose_fragment", "aurelia_petal", "briar_seedbag", "tobacco_leaves",
            "bloody_cloth", "herobrines_scythe", "golden_oak_leaves", "ender_log", "ender_leaves", "nether_log",
            "nether_leaves", "peaceful_log", "peaceful_leaves", "advanced_alchemical_furnace", "entity_deconstructor",
            "overchanter", "relocator", "inverse_relocator", "campfire", "brazier"};

    @Override
    public Identifier getPluginUid() {
        return ThaumaturgicBases.id("jei");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AspectFromStacksCategory.RECIPE_TYPE, TBDevices.ADVANCED_FURNACE.get());
        registration.addCraftingStation(RecipeTypes.ANVIL, TBDevices.THAUMIC_ANVIL.get(), TBDevices.VOID_ANVIL.get());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (String name : INFO) {
            BuiltInRegistries.ITEM.getOptional(ThaumaturgicBases.id(name)).ifPresent(item ->
                    registration.addIngredientInfo(item, Component.translatable("jei.thaumaturgicbases.info." + name)));
        }
    }
}

package com.thaumaturgicbases.block;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.content.wands.EntityAspectOrb;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;

/** TB's Primal Shroom: once ripe, breaking it bursts into 8-20 single primal aspect orbs. */
public class AshroomBlock extends TBPlantBlock {
    private static final List<ResourceKey<IAspect>> PRIMALS = List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio").stream()
            .map(a -> ResourceKey.create(IAspect.REGISTRY_KEY, Identifier.fromNamespaceAndPath("thaumaturge", a))).toList();

    public AshroomBlock(Spec spec, Supplier<? extends ItemLike> seed, Properties properties) {
        super(spec, seed, properties);
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (!isMaxAge(state)) {
            return;
        }
        int count = 8 + (int) Math.round(level.getRandom().nextDouble() * 11.76742);
        for (int i = 0; i < count; i++) {
            ResourceKey<IAspect> aspect = PRIMALS.get(level.getRandom().nextInt(PRIMALS.size()));
            level.addFreshEntity(new EntityAspectOrb(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, aspect, 1));
        }
    }
}

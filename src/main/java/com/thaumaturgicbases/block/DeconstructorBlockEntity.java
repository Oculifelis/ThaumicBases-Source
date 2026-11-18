package com.thaumaturgicbases.block;

import com.leclowndu93150.thaumaturge.api.aspect.AspectDataMaps;
import com.leclowndu93150.thaumaturge.api.aspect.AspectIndexAccess;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.IEntityAspectSource;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeType;
import com.leclowndu93150.thaumaturge.api.research.scan.ScanningManager;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.thaumaturgicbases.TBDataMaps;
import com.thaumaturgicbases.TBDevices;
import com.thaumaturgicbases.TBRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * TB TileEntityDeconstructor. Every 40 ticks of work it takes the first entity on the plate apart: a living entity
 * takes 2 magic damage (a player also gains 1 warp), a dropped item (all six crystals needed) loses one from its
 * stack. One of its aspects, broken down to a primal, then becomes 1 observation for the linked player, in the
 * research category the {@code thaumaturgicbases:aspect_knowledge} block data map gives that primal. Each crystal adds 8
 * blocks of reach (base 32) and speeds entity work by half a tick.
 */
public class DeconstructorBlockEntity extends BlockEntity {
    private int tickTime;
    private UUID owner;
    private int crystals;  // bit i = crystal of PRIMALS[i]

    public DeconstructorBlockEntity(BlockPos pos, BlockState state) {
        super(TBDevices.DECONSTRUCTOR_BE.get(), pos, state);
    }

    void setOwner(Player player) {
        owner = player.getUUID();
        setChanged();
    }

    /** Takes a primal crystal block it does not hold yet (TB used crystal metadata 0-5: air, fire, water, earth, order, entropy). */
    boolean addCrystal(ItemStack stack) {
        for (int i = 0; i < 6; ++i) {
            if (stack.is(TBRegistry.CRYSTAL_BLOCKS.get(TBRegistry.CRYSTALS.get(i)).asItem()) && (crystals & 1 << i) == 0) {
                crystals |= 1 << i;
                tickTime = 0;
                setChanged();
                return true;
            }
        }
        return false;
    }

    private int stability() {
        return Integer.bitCount(crystals);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DeconstructorBlockEntity be) {
        if (level.isClientSide() || be.owner == null) return;
        List<Entity> on = level.getEntitiesOfClass(Entity.class, new AABB(pos), Entity::isAlive);
        if (on.isEmpty()) {
            be.tickTime = 0;
            return;
        }
        Entity e = on.getFirst();
        AspectList aspects;
        if (e instanceof ItemEntity item) {
            if (be.stability() < 6) {
                be.tickTime = 0;
                return;
            }
            aspects = AspectIndexAccess.of(item.getItem());
        } else {
            aspects = entityAspects(e);
        }
        if (aspects == null || aspects.isEmpty()) {
            be.tickTime = 0;
            return;
        }
        ServerLevel server = (ServerLevel) level;
        if (level.getGameTime() % 4 == 0) {
            server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 2, 0.3, 0.2, 0.3, 0.05);
        }
        be.tickTime += 1 + (e instanceof ItemEntity ? 0 : be.stability() / 2);
        if (be.tickTime < 40) return;
        be.tickTime = 0;
        boolean teach;
        if (e instanceof ItemEntity item) {
            item.getItem().shrink(1);
            if (item.getItem().isEmpty()) item.discard();
            // TB: the chance to learn rises with the item's primal content, 1 in 40 per point.
            teach = level.getRandom().nextInt(40) < aspects.totalAmount();
        } else {
            e.hurtServer(server, level.damageSources().magic(), 2.0F);
            if (e instanceof ServerPlayer sp) WarpHelper.addWarp(sp, 1, WarpType.TEMPORARY);
            teach = true;
        }
        if (teach) be.teach(server, aspects);
    }

    private static AspectList entityAspects(Entity e) {
        if (e instanceof IEntityAspectSource source) return source.entityAspects();
        if (!(e instanceof LivingEntity)) return null;
        return e.getType().builtInRegistryHolder().getData(AspectDataMaps.ENTITY_ASPECTS);
    }

    private void teach(ServerLevel level, AspectList aspects) {
        if (!(level.getPlayerByUUID(owner) instanceof ServerPlayer player)) return;
        double reach = 32 + stability() * 8;
        if (player.distanceToSqr(worldPosition.getCenter()) >= reach * reach) return;
        List<Holder<IAspect>> list = new ArrayList<>(aspects.aspects());
        Holder<IAspect> aspect = toPrimal(list.get(level.getRandom().nextInt(list.size())), level.getRandom());
        Map<ResourceKey<IAspect>, Identifier> table = getBlockState().typeHolder().getData(TBDataMaps.ASPECT_KNOWLEDGE);
        Optional<ResourceKey<IAspect>> key = aspect.unwrapKey();
        if (table == null || key.isEmpty() || !table.containsKey(key.get())) return;
        ScanningManager.addKnowledge(player, KnowledgeType.OBSERVATION, table.get(key.get()), 1);
    }

    /** Follows a random component down to a primal (TC4 ResearchManager.reduceToPrimals). */
    private static Holder<IAspect> toPrimal(Holder<IAspect> aspect, RandomSource r) {
        for (int depth = 0; depth < 16 && !aspect.value().isPrimal() && !aspect.value().components().isEmpty(); ++depth) {
            List<Holder<IAspect>> parts = aspect.value().components();
            aspect = parts.get(r.nextInt(parts.size()));
        }
        return aspect;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("Crystals", crystals);
        if (owner != null) out.store("Owner", net.minecraft.core.UUIDUtil.CODEC, owner);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        crystals = in.getIntOr("Crystals", 0);
        owner = in.read("Owner", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
    }
}

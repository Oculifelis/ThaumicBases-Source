package com.thaumaturgicbases.node;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityJarNode;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeData;
import com.leclowndu93150.thaumaturge.content.entity.WispEntity;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.thaumaturgicbases.ThaumaturgicBases;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * TB TileNodeLinker. The linker that holds a link is the receiver; every 10 ticks it moves one point of a random aspect
 * from the node under the linked (transmitter) linker into the node under itself: 30% of the time (or when the
 * transmitter holds nothing) from the transmitter's base aspects (a lasting move), else from what it holds now. A
 * transmitter emptied for good vanishes. Each move risks the link's instability: TB's events (flux gas, flux goo,
 * an arcing zap, a wisp, harm to the transmitter's base aspects, an exploding transmitter) are all kept with TB's odds.
 * A block in {@code thaumaturgicbases:node_linker_stabilizers} on top of the receiver stops the two worst ones.
 */
public class NodeLinkerBlockEntity extends BlockEntity {
    public static final TagKey<Block> STABILIZERS = TagKey.create(Registries.BLOCK, ThaumaturgicBases.id("node_linker_stabilizers"));
    private @Nullable BlockPos link;
    private int instability;

    public NodeLinkerBlockEntity(BlockPos pos, BlockState state) {
        super(TBNodes.NODE_LINKER_BE.get(), pos, state);
    }

    public @Nullable BlockPos link() {
        return link;
    }

    void setLink(@Nullable BlockPos pos) {
        link = pos;
        instability = 0;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private static @Nullable BlockEntityNode node(Level level, BlockPos linker) {
        BlockEntity be = level.getBlockEntity(linker.below());
        return be instanceof BlockEntityNode n && !(be instanceof BlockEntityJarNode) ? n : null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NodeLinkerBlockEntity be) {
        if (be.link == null || !(level instanceof ServerLevel server)) {
            return;
        }
        if (!(level.getBlockEntity(be.link) instanceof NodeLinkerBlockEntity other) || other.link != null) {
            be.setLink(null);
            return;
        }
        BlockEntityNode receiver = node(level, pos);
        BlockEntityNode transmitter = node(level, be.link);
        if (receiver == null || transmitter == null) {
            be.instability = 0;
            return;
        }
        if (level.getGameTime() % 10 != 0) return;
        be.beam(server, pos, be.link);
        RandomSource rand = level.getRandom();
        boolean perm = rand.nextDouble() < 0.3 || transmitter.getAspects().isEmpty();
        AspectList pullFrom = perm ? transmitter.capacity() : transmitter.getAspects();
        if (pullFrom.totalAmount() <= 0) {
            level.removeBlock(be.link.below(), false);
            return;
        }
        List<AspectInstance> entries = pullFrom.entries();
        Holder<IAspect> a = entries.get(rand.nextInt(entries.size())).aspect();
        if (perm) {
            if (rand.nextDouble() <= 0.7) {
                if (be.instabilityCheck(server)) {
                    setBase(receiver, receiver.capacity().add(a, 1));
                    receiver.fill(a, 1);
                } else {
                    be.instability++;
                }
                setBase(transmitter, transmitter.capacity().reduce(a, 1));
            }
        } else {
            int have = receiver.amountOf(a);
            if (have > 0 && have <= receiver.capacityOf(a)) {
                if (be.instabilityCheck(server)) receiver.fill(a, 1);
            } else if (rand.nextDouble() <= 0.33) {
                be.instability++;
            }
            transmitter.drain(a, 1);
        }
        transmitter.setChanged();
        receiver.setChanged();
        be.setChanged();
    }

    private static void setBase(BlockEntityNode node, AspectList base) {
        node.applyNodeData(new NodeData(node.kind(), Optional.ofNullable(node.trait()), node.getAspects(), base));
    }

    private boolean stabilized() {
        return level != null && level.getBlockState(worldPosition.above()).is(STABILIZERS);
    }

    /** TB instabilityCheck: true when the move goes through; false after an instability event. */
    private boolean instabilityCheck(ServerLevel level) {
        RandomSource rand = level.getRandom();
        if (instability <= 0 || rand.nextInt(50) > instability) return true;
        int rnd = rand.nextInt(instability);
        if (rnd == 49) {
            if (!stabilized()) instability -= explodeTransmitter(level);
        } else if (rnd >= 45) {
            if (!stabilized()) instability -= harmTransmitter(level);
        } else if (rnd >= 31) {
            instability -= wisp(level);
        } else if (rnd >= 20) {
            instability -= zap(level);
        } else if (rnd >= 10) {
            instability -= fluxNode(level);
        } else {
            instability -= fluxTransmission(level);
        }
        instability = Math.max(0, instability);
        return false;
    }

    private BlockPos side(RandomSource rand) {
        return rand.nextBoolean() && link != null ? link : worldPosition;
    }

    private int harmTransmitter(ServerLevel level) {
        BlockEntityNode transmitter = link == null ? null : node(level, link);
        if (transmitter == null) return 0;
        boolean perm = level.getRandom().nextBoolean();
        AspectList list = perm ? transmitter.capacity() : transmitter.getAspects();
        if (list.isEmpty()) return 0;
        AspectInstance a = list.entries().get(level.getRandom().nextInt(list.size()));
        int by = Math.min(4 + level.getRandom().nextInt(16), a.amount());
        if (perm) {
            setBase(transmitter, list.reduce(a.aspect(), by));
        } else {
            transmitter.drain(a.aspect(), by);
        }
        return perm ? 25 : 6;
    }

    private int zap(ServerLevel level) {
        BlockPos at = side(level.getRandom());
        List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(at.getX() - 5, at.getY() - 6, at.getZ() - 5,
                at.getX() + 5, at.getY() + 4, at.getZ() + 5));
        if (players.isEmpty()) return 0;
        Player target = players.get(level.getRandom().nextInt(players.size()));
        Vec3 from = Vec3.atCenterOf(at.below());
        Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0);
        for (int i = 0; i <= 8; i++) {
            Vec3 p = from.lerp(to, i / 8.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
        target.hurtServer(level, level.damageSources().magic(), 4 + level.getRandom().nextInt(4));
        return 5;
    }

    private int wisp(ServerLevel level) {
        BlockPos at = side(level.getRandom());
        WispEntity wisp = TTEntities.WISP.get().create(level, EntitySpawnReason.EVENT);
        if (wisp == null) return 0;
        List<Holder.Reference<IAspect>> all = level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).listElements().toList();
        if (!all.isEmpty()) wisp.setAspect(all.get(level.getRandom().nextInt(all.size())).key().identifier());
        wisp.snapTo(at.getX() + 0.5, at.getY() - 0.5, at.getZ() + 0.5, 0, 0);
        level.addFreshEntity(wisp);
        return 9;
    }

    private int fluxNode(ServerLevel level) {
        BlockPos at = side(level.getRandom());
        BlockPos p = at.below().relative(Direction.Plane.HORIZONTAL.getRandomDirection(level.getRandom()));
        if (level.getBlockState(p).canBeReplaced()) level.setBlock(p, TTBlocks.FLUX_GOO.get().defaultBlockState(), 3);
        return 5;
    }

    private int fluxTransmission(ServerLevel level) {
        BlockPos p = side(level.getRandom()).above(2);
        if (level.getBlockState(p).canBeReplaced()) level.setBlock(p, TTBlocks.FLUX_GAS.get().defaultBlockState(), 3);
        return 3;
    }

    private int explodeTransmitter(ServerLevel level) {
        if (link == null) return instability;
        BlockPos node = link.below();
        level.removeBlock(node, false);
        level.explode(null, node.getX() + 0.5, node.getY() + 0.5, node.getZ() + 0.5, 2.0F, true, Level.ExplosionInteraction.BLOCK);
        return instability;
    }

    private void beam(ServerLevel level, BlockPos to, BlockPos from) {
        Vec3 a = Vec3.atCenterOf(from).add(0, 0.325, 0);
        Vec3 b = Vec3.atCenterOf(to).add(0, 0.325, 0);
        int n = Math.max(2, (int) (a.distanceTo(b) * 2));
        for (int i = 0; i <= n; i++) {
            Vec3 p = a.lerp(b, i / (double) n);
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (link != null) out.store("link", BlockPos.CODEC, link);
        out.putInt("instability", instability);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        link = in.read("link", BlockPos.CODEC).orElse(null);
        instability = in.getIntOr("instability", 0);
    }
}

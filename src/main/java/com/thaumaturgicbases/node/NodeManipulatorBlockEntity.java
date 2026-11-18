package com.thaumaturgicbases.node;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.nodes.NodeModifier;
import com.leclowndu93150.thaumaturge.api.nodes.NodeType;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityJarNode;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.wands.EntityAspectOrb;
import com.thaumaturgicbases.TBComponents;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * TB TileNodeManipulator. Works on the aura node right below it (not a jarred or energised one) with the node focus
 * placed in it; what the focus does is its {@link NodeFocus} entry. A step's timer only counts while the same step
 * keeps matching the node, as in TB.
 */
public class NodeManipulatorBlockEntity extends BlockEntity {
    /**
     * Idle, or counting the time of one focus step on the node below. The step index and its work time belong to
     * {@link State#STEPPING} only: they change together in {@link #enter}, and are saved only while stepping.
     */
    enum State {
        IDLE, STEPPING;

        String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        static State of(String id) {
            for (State s : values()) if (s.id().equals(id)) return s;
            return IDLE;
        }
    }

    private ItemStack focus = ItemStack.EMPTY;
    private State state = State.IDLE;
    private int step = -1;
    private int workTime;
    private final Map<Holder<IAspect>, Integer> lastAmounts = new HashMap<>();

    public NodeManipulatorBlockEntity(BlockPos pos, BlockState state) {
        super(TBNodes.NODE_MANIPULATOR_BE.get(), pos, state);
    }

    public ItemStack focus() {
        return focus;
    }

    void setFocus(ItemStack stack) {
        focus = stack;
        stop();
        setChanged();
        if (level != null) {
            level.setBlock(worldPosition, getBlockState().setValue(NodeManipulatorBlock.HAS_FOCUS, !stack.isEmpty()), 3);
        }
    }

    ItemStack takeFocus() {
        ItemStack out = focus;
        setFocus(ItemStack.EMPTY);
        return out;
    }

    private void stop() {
        enter(-1);
        lastAmounts.clear();
    }

    /** The one place the state changes: a step index of 0 or more starts that step's timer, -1 goes idle. */
    private void enter(int nextStep) {
        state = nextStep >= 0 ? State.STEPPING : State.IDLE;
        step = state == State.STEPPING ? nextStep : -1;
        workTime = 0;
    }

    private Optional<NodeFocus> focusEntry(Level level) {
        Identifier id = focus.get(TBComponents.NODE_FOCUS.get());
        if (id == null) return Optional.empty();
        return level.registryAccess().lookup(NodeFocus.REGISTRY_KEY)
                .flatMap(r -> r.get(ResourceKey.create(NodeFocus.REGISTRY_KEY, id))).map(Holder::value);
    }

    private static @Nullable BlockEntityNode nodeBelow(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos.below());
        if (be instanceof BlockEntityNode node && !(be instanceof BlockEntityJarNode) && !node.isEnergized()) return node;
        return null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NodeManipulatorBlockEntity be) {
        if (be.focus.isEmpty() || level.getGameTime() % 2 != 0) return;
        BlockEntityNode node = nodeBelow(level, pos);
        Optional<NodeFocus> entry = be.focusEntry(level);
        if (node == null || entry.isEmpty()) {
            if (be.state != State.IDLE) be.stop();
            return;
        }
        NodeFocus f = entry.get();
        if (f.refundChance() > 0) be.refund(level, node, f.refundChance());
        if (f.regenChance() > 0) be.regen(level, node, f.regenChance());
        if (level.getGameTime() % 20 != 0) return;
        int s = f.stepFor(node.kind(), node.trait());
        // the beam shows while a step runs or while the focus's passive effects (refund/regen) act; the latter is
        // focus config, not manipulator state
        boolean passive = f.refundChance() > 0 || f.regenChance() > 0;
        if ((s >= 0 || passive) && level instanceof ServerLevel server) beam(server, pos, f.color());
        if (s != be.step) be.enter(s);
        if (be.state != State.STEPPING) return;
        NodeFocus.Step st = f.steps().get(s);
        if (be.workTime >= st.ticks()) {
            be.finish(level, pos, node, st);
            return;
        }
        be.workTime += 20;
        if (f.orbInterval() > 0 && be.workTime % f.orbInterval() == 0) {
            List<AspectInstance> aspects = node.getAspects().entries();
            if (!aspects.isEmpty()) {
                AspectInstance a = aspects.get(level.getRandom().nextInt(aspects.size()));
                a.aspect().unwrapKey().ifPresent(key ->
                        level.addFreshEntity(new EntityAspectOrb(level, pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5, key, 1)));
            }
        }
        be.setChanged();
    }

    private void finish(Level level, BlockPos pos, BlockEntityNode node, NodeFocus.Step st) {
        if (st.setModifier().filter(NodeFocus.DESTROY::equals).isPresent()) {
            level.removeBlock(pos.below(), false);
            stop();
            return;
        }
        st.setType().ifPresent(node::reclassify);
        st.setModifier().ifPresent(m -> {
            NodeModifier mod = NodeFocus.modifier(m);
            node.assignTrait(mod);
        });
        node.setChanged();
        stop();
        setChanged();
    }

    /** Efficiency: drained aspects come back {@code chance} of the time. */
    private void refund(Level level, BlockEntityNode node, float chance) {
        for (AspectInstance a : node.getAspects().entries()) {
            Integer before = lastAmounts.get(a.aspect());
            if (before != null && a.amount() < before && level.getRandom().nextFloat() < chance) {
                node.fill(a.aspect(), before - a.amount());
            }
        }
        lastAmounts.clear();
        for (AspectInstance a : node.getAspects().entries()) lastAmounts.put(a.aspect(), a.amount());
    }

    /** Speed: every aspect below its base gains a point {@code chance} of the time. */
    private static void regen(Level level, BlockEntityNode node, float chance) {
        if (level.getRandom().nextFloat() >= chance) return;
        for (AspectInstance a : node.capacity().entries()) {
            if (node.amountOf(a.aspect()) < node.capacityOf(a.aspect())) node.fill(a.aspect(), 1);
        }
    }

    private static void beam(ServerLevel level, BlockPos pos, int color) {
        DustParticleOptions dust = new DustParticleOptions(color, 0.8F);
        for (int i = 0; i < 4; i++) {
            level.sendParticles(dust, pos.getX() + 0.5, pos.getY() + 0.3 - i * 0.25, pos.getZ() + 0.5, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !focus.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, focus);
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!focus.isEmpty()) out.store("focus", ItemStack.CODEC, focus);
        out.putString("state", state.id());
        if (state == State.STEPPING) {
            out.putInt("step", step);
            out.putInt("work_time", workTime);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        focus = in.read("focus", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        boolean stepping = State.of(in.getStringOr("state", "")) == State.STEPPING;
        enter(stepping ? in.getIntOr("step", -1) : -1);
        if (state == State.STEPPING) workTime = in.getIntOr("work_time", 0);
    }
}

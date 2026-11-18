package com.thaumaturgicbases.block;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.essentia.EssentiaTransportHelper;
import com.leclowndu93150.thaumaturge.content.infusion.EssentiaSources;
import com.thaumaturgicbases.TBComponents;
import com.thaumaturgicbases.TBDevices;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * TB TileOverchanter. Once started it burns 8 Praecantatio a second from nearby jars (a missed second does not count).
 * From second 16 it absorbs 825 experience points (30 levels): first from any tank within 8 blocks holding a
 * {@code c:experience} fluid (Automagy jars, EnderIO obelisks...; 20 mB a point), else 30 levels from a player within
 * 6 blocks, who takes 8 magic damage. From second 32, with the experience paid, the first enchantment it has not
 * raised before goes up one level and is remembered in the {@code thaumaturgicbases:overchants} component.
 * A redstone signal starts it too; hoppers insert items it can work on and extract only finished ones.
 */
public class OverchanterBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final int XP_30_LEVELS = 825;
    private static final int MB_PER_POINT = 20;
    private static final TagKey<Fluid> EXPERIENCE = TagKey.create(Registries.FLUID, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "experience"));
    private ItemStack item = ItemStack.EMPTY;
    private boolean working;
    private int ticks;
    private int xpToAbsorb = XP_30_LEVELS;
    private EssentiaSources sources;

    public OverchanterBlockEntity(BlockPos pos, BlockState state) {
        super(TBDevices.OVERCHANTER_BE.get(), pos, state);
    }

    @Override
    public boolean isEmpty() {
        return item.isEmpty();
    }

    boolean isWorking() {
        return working;
    }

    private void setItem(ItemStack stack) {
        item = stack;
        changed();
    }

    ItemStack takeItem() {
        ItemStack out = item;
        item = ItemStack.EMPTY;
        reset();
        changed();
        return out;
    }

    static boolean canOverchant(ItemStack stack) {
        return next(stack) != null;
    }

    /** The first enchantment on the stack that is not yet in its overchant list (TB findEnchantment). */
    private static Holder<Enchantment> next(ItemStack stack) {
        ItemEnchantments ench = stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        List<Holder<Enchantment>> done = stack.getOrDefault(TBComponents.OVERCHANTS.get(), List.of());
        for (Holder<Enchantment> e : ench.keySet()) {
            if (!done.contains(e)) return e;
        }
        return null;
    }

    boolean canStart() {
        return !working && !item.isEmpty() && next(item) != null;
    }

    boolean tryStart() {
        if (!canStart() || level == null) return false;
        working = true;
        level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.5F, 1.0F);
        changed();
        return true;
    }

    private boolean tryStartFromRedstone() {
        return level != null && level.hasNeighborSignal(worldPosition) && tryStart();
    }

    private void reset() {
        working = false;
        ticks = 0;
        xpToAbsorb = XP_30_LEVELS;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OverchanterBlockEntity be) {
        if (be.item.isEmpty()) {
            if (be.working) be.reset();
            return;
        }
        if (!be.working && !be.tryStartFromRedstone()) return;
        if (be.ticks % 20 == 0) {
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.6F, 1.4F);
            if (be.sources == null) be.sources = new EssentiaSources(pos);
            Holder<IAspect> magic = EssentiaTransportHelper.resolve(level, TTAspects.PRAECANTATIO);
            if (magic != null && be.sources.drain((ServerLevel) level, magic, 8)) {
                int seconds = be.ticks / 20 + 1;
                if (seconds >= 16 && be.xpToAbsorb > 0) be.absorbXp((ServerLevel) level);
                if (seconds >= 32 && be.xpToAbsorb == 0) {
                    be.overchant();
                    level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return;
                }
            } else {
                be.ticks -= 20;
            }
        }
        ++be.ticks;
        be.setChanged();
    }

    private void overchant() {
        Holder<Enchantment> e = next(item);
        if (e != null) {
            EnchantmentHelper.updateEnchantments(item, m -> m.set(e, m.getLevel(e) + 1));
            List<Holder<Enchantment>> done = new ArrayList<>(item.getOrDefault(TBComponents.OVERCHANTS.get(), List.of()));
            done.add(e);
            item.set(TBComponents.OVERCHANTS.get(), List.copyOf(done));
        }
        reset();
        changed();
    }

    private void absorbXp(ServerLevel level) {
        xpToAbsorb = drainTanks(level, xpToAbsorb);
        if (xpToAbsorb == 0) return;
        int levels = (int) Math.round(xpToAbsorb > 255 ? (59 + Math.sqrt(24.0 * xpToAbsorb - 5159)) / 6 : xpToAbsorb / 17.0);
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(6, 3, 6))) {
            if (p.experienceLevel >= levels) {
                p.hurtServer(level, level.damageSources().magic(), 8.0F);
                level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.BLOCKS, 0.3F, 1.6F);
                p.giveExperienceLevels(-levels);
                xpToAbsorb = 0;
                return;
            }
        }
    }

    /** Drains experience fluid from tanks within 8 blocks; returns the points still owed. */
    private int drainTanks(ServerLevel level, int owed) {
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-8, -8, -8), worldPosition.offset(8, 8, 8))) {
            if (owed <= 0) break;
            if (p.equals(worldPosition) || level.getBlockEntity(p) == null) continue;
            ResourceHandler<FluidResource> tank = level.getCapability(Capabilities.Fluid.BLOCK, p, null);
            if (tank == null) continue;
            for (int slot = 0; slot < tank.size() && owed > 0; ++slot) {
                FluidResource fluid = tank.getResource(slot);
                if (fluid.isEmpty() || !fluid.typeHolder().is(EXPERIENCE)) continue;
                try (Transaction tx = Transaction.openRoot()) {
                    int drained = tank.extract(slot, fluid, owed * MB_PER_POINT, tx);
                    int points = drained / MB_PER_POINT;
                    if (points > 0) {
                        tx.commit();
                        owed -= points;
                    }
                }
            }
        }
        return owed;
    }

    private void changed() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            BlockState next = state.setValue(OverchanterBlock.HAS_ITEM, !item.isEmpty()).setValue(OverchanterBlock.ACTIVE, working);
            if (next != state) level.setBlock(worldPosition, next, 3);
        }
    }

    // Hopper access (TB: ISidedInventory). One slot, stack size 1.

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return item.isEmpty() && canOverchant(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !working && !canOverchant(stack);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public ItemStack getItem(int slot) {
        return item;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return count > 0 && !item.isEmpty() ? takeItem() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = item;
        item = ItemStack.EMPTY;
        reset();
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            takeItem();
        } else {
            setItem(stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        removeItemNoUpdate(0);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !item.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), item);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!item.isEmpty()) out.store("Item", ItemStack.CODEC, item);
        out.putBoolean("Working", working);
        out.putInt("Ticks", ticks);
        out.putInt("XpToAbsorb", xpToAbsorb);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        item = in.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        working = in.getBooleanOr("Working", false);
        ticks = in.getIntOr("Ticks", 0);
        xpToAbsorb = in.getIntOr("XpToAbsorb", XP_30_LEVELS);
    }
}

package com.thaumaturgicbases.menu;

import com.thaumaturgicbases.block.TBAnvilBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The vanilla anvil with TB's rules (ContainerThaumicAnvil / ContainerVoidAnvil). Thaumic: "too expensive" only from
 * 60 levels instead of 40, and a third of the wear chance. Void: every operation costs 3 levels and never wears it.
 * The vanilla logic is reused by lowering the inputs' prior-work penalty while it runs, then adding it back.
 */
public class TBAnvilMenu extends AnvilMenu {
    private static final int VANILLA_CAP = 40;
    private static final int THAUMIC_CAP = 60;
    private static final int VOID_COST = 3;
    private static final float WEAR_CHANCE = 0.12F / 3;
    private final TBAnvilBlock anvil;

    public TBAnvilMenu(int id, Inventory inv, ContainerLevelAccess access, TBAnvilBlock anvil) {
        super(id, inv, access);
        this.anvil = anvil;
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(anvil);
    }

    @Override
    protected void createResultInternal() {
        ItemStack left = inputSlots.getItem(0);
        ItemStack right = inputSlots.getItem(1);
        int leftCost = left.getOrDefault(DataComponents.REPAIR_COST, 0);
        int rightCost = right.getOrDefault(DataComponents.REPAIR_COST, 0);
        // Void: no prior-work penalty at all. Thaumic: up to 20 levels of it hidden, so 40-59 passes the vanilla cap.
        int hidden = anvil.isVoid ? leftCost + rightCost : Math.min(THAUMIC_CAP - VANILLA_CAP, leftCost + rightCost);
        int fromLeft = Math.min(hidden, leftCost);
        setRepairCost(left, leftCost - fromLeft);
        setRepairCost(right, rightCost - (hidden - fromLeft));
        try {
            super.createResultInternal();
        } finally {
            setRepairCost(left, leftCost);
            setRepairCost(right, rightCost);
        }
        ItemStack result = resultSlots.getItem(0);
        if (result.isEmpty()) return;
        int prior = Math.max(leftCost, rightCost);
        result.set(DataComponents.REPAIR_COST, right.isEmpty() ? prior : calculateIncreasedRepairCost(prior));
        if (anvil.isVoid) {
            setCost(VOID_COST);
        } else {
            setCost(getCost() + hidden);
            if (getCost() >= THAUMIC_CAP && !player.hasInfiniteMaterials()) resultSlots.setItem(0, ItemStack.EMPTY);
        }
    }

    private static void setRepairCost(ItemStack stack, int cost) {
        if (stack.isEmpty()) return;
        if (cost > 0) stack.set(DataComponents.REPAIR_COST, cost);
        else stack.remove(DataComponents.REPAIR_COST);
    }

    @Override
    protected void onTake(Player player, ItemStack carried) {
        super.onTake(player, carried);
        if (anvil.isVoid || player.hasInfiniteMaterials()) return;
        access.execute((level, pos) -> {
            BlockState state = level.getBlockState(pos);
            if (!state.is(anvil) || player.getRandom().nextFloat() >= WEAR_CHANCE) return;
            int damage = state.getValue(TBAnvilBlock.DAMAGE) + 1;
            if (damage > 2) {
                level.removeBlock(pos, false);
                level.levelEvent(1029, pos, 0);
            } else {
                level.setBlock(pos, state.setValue(TBAnvilBlock.DAMAGE, damage), 2);
            }
        });
    }
}

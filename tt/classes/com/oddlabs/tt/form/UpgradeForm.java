package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.SupplyContainer;

public final strictfp class UpgradeForm extends Form {
    private static final int BUTTON_WIDTH = 130;

    // Upgrade costs per level (index = current level, upgrading TO level+1)
    // [tree, rock, iron]
    private static final int[][] UPGRADE_COSTS = {
        {3, 2, 0},  // Level 0 -> 1
        {5, 3, 2},  // Level 1 -> 2
        {8, 5, 4},  // Level 2 -> 3
    };

    private final Building building;

    public UpgradeForm(Building building) {
        super("Upgrade Building");
        this.building = building;

        int level = building.getUpgradeLevel();
        int maxLevel = Building.MAX_UPGRADE_LEVEL;

        Label level_label = new Label(
                "Current Level: " + level + " / " + maxLevel,
                Skin.getSkin().getEditFont());
        addChild(level_label);
        level_label.place();

        if (level < maxLevel) {
            int[] cost = UPGRADE_COSTS[level];

            String costText = "Cost: " + cost[0] + " Wood";
            if (cost[1] > 0) costText += ", " + cost[1] + " Rock";
            if (cost[2] > 0) costText += ", " + cost[2] + " Iron";

            Label cost_label = new Label(costText, Skin.getSkin().getEditFont());
            addChild(cost_label);
            cost_label.place(level_label, BOTTOM_LEFT);

            boolean has_supplies = building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER);
            String availText;
            boolean can_afford;

            if (has_supplies) {
                SupplyContainer tree_c = building.getSupplyContainer(TreeSupply.class);
                SupplyContainer rock_c = building.getSupplyContainer(RockSupply.class);
                SupplyContainer iron_c = building.getSupplyContainer(IronSupply.class);
                int tree_n = tree_c != null ? tree_c.getNumSupplies() : 0;
                int rock_n = rock_c != null ? rock_c.getNumSupplies() : 0;
                int iron_n = iron_c != null ? iron_c.getNumSupplies() : 0;

                availText = "Available: " + tree_n + " Wood, " + rock_n + " Rock, " + iron_n + " Iron";
                can_afford = tree_n >= cost[0] && rock_n >= cost[1] && iron_n >= cost[2];
            } else {
                availText = "No supply storage - free upgrade";
                can_afford = true;
            }

            Label avail_label = new Label(availText, Skin.getSkin().getEditFont());
            addChild(avail_label);
            avail_label.place(cost_label, BOTTOM_LEFT);

            if (can_afford && building.canUpgrade()) {
                HorizButton upgrade_button = new HorizButton("Upgrade to Level " + (level + 1), BUTTON_WIDTH);
                addChild(upgrade_button);
                upgrade_button.place(avail_label, BOTTOM_LEFT);
                upgrade_button.addMouseClickListener(new UpgradeListener());
            } else {
                Label no_label = new Label(
                        can_afford ? "Cannot upgrade" : "Not enough resources",
                        Skin.getSkin().getEditFont());
                no_label.setColor(new float[]{1f, 0.3f, 0.3f, 1f});
                addChild(no_label);
                no_label.place(avail_label, BOTTOM_LEFT);
            }
        } else {
            Label max_label = new Label("Maximum level reached!", Skin.getSkin().getEditFont());
            addChild(max_label);
            max_label.place(level_label, BOTTOM_LEFT);
        }

        compileCanvas();
        centerPos();
    }

    private final strictfp class UpgradeListener implements MouseClickListener {
        public final void mouseClicked(int button, int x, int y, int clicks) {
            performUpgrade();
        }
    }

    private void performUpgrade() {
        if (!building.canUpgrade()) return;

        int level = building.getUpgradeLevel();
        if (level >= UPGRADE_COSTS.length) return;
        int[] cost = UPGRADE_COSTS[level];

        boolean has_supplies = building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER);
        if (has_supplies) {
            SupplyContainer tree_c = building.getSupplyContainer(TreeSupply.class);
            SupplyContainer rock_c = building.getSupplyContainer(RockSupply.class);
            SupplyContainer iron_c = building.getSupplyContainer(IronSupply.class);

            int tree_n = tree_c != null ? tree_c.getNumSupplies() : 0;
            int rock_n = rock_c != null ? rock_c.getNumSupplies() : 0;
            int iron_n = iron_c != null ? iron_c.getNumSupplies() : 0;

            if (tree_n < cost[0] || rock_n < cost[1] || iron_n < cost[2]) return;

            if (tree_c != null) tree_c.increaseSupply(-cost[0]);
            if (rock_c != null) rock_c.increaseSupply(-cost[1]);
            if (iron_c != null && cost[2] > 0) iron_c.increaseSupply(-cost[2]);
        }

        building.upgrade();
        cancel();
    }
}

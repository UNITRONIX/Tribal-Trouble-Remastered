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
    private static final int BUTTON_WIDTH = 180;

    // Resource costs for non-military buildings (index = current level)
    private static final int[][] UPGRADE_COSTS = {
        {3, 2, 0},  // Level 0 -> 1
        {5, 3, 2},  // Level 1 -> 2
        {8, 5, 4},  // Level 2 -> 3
    };

    // Base upgrade time in ticks for barracks (reduced by units inside)
    private static final int[] BARRACKS_BASE_TICKS = {600, 900, 1500};

    // Descriptions per level for each building type
    private static final String[][] BARRACKS_DESC = {
        {"Lv.1: +25% szybkosc treningu", "Lv.2: +50% szybkosc, +20% obrona", "Lv.3: 2x szybkosc, 2x obrona"},
        {"Lv.1: Szybszy trening jednostek", "Lv.2: Lepsza obrona budynku", "Lv.3: Maksymalne ulepszenie"},
    };
    private static final String[] ECONOMY_DESC = {
        "Lv.1: +25% szybkosc produkcji",
        "Lv.2: +50% szybkosc, +20% obrona",
        "Lv.3: 2x szybkosc, 2x obrona",
    };

    private final Building building;
    private final boolean isBarracks;

    public UpgradeForm(Building building) {
        super("Ulepszenie budynku");
        this.building = building;
        this.isBarracks = building.getAbilities().hasAbilities(Abilities.BUILD_ARMIES);

        int level = building.getUpgradeLevel();
        int maxLevel = Building.MAX_UPGRADE_LEVEL;

        Label level_label = new Label(
                "Poziom: " + level + " / " + maxLevel,
                Skin.getSkin().getEditFont());
        addChild(level_label);
        level_label.place();

        if (level < maxLevel) {
            // Description of what the upgrade does
            String desc = ECONOMY_DESC[level];
            Label desc_label = new Label(desc, Skin.getSkin().getEditFont());
            addChild(desc_label);
            desc_label.place(level_label, BOTTOM_LEFT);

            Label last_placed = desc_label;

            if (isBarracks) {
                // Barracks: time-based upgrade, more units = faster
                int unitCount = 0;
                if (building.getUnitContainer() != null) {
                    unitCount = building.getUnitContainer().getNumSupplies();
                }
                int baseTicks = BARRACKS_BASE_TICKS[level];
                float speedMult = Math.max(1, unitCount);
                int actualTicks = Math.max(60, (int)(baseTicks / speedMult));
                int seconds = actualTicks / 20;

                Label time_label = new Label(
                        "Czas ulepszenia: ~" + seconds + "s (jednostek w budynku: " + unitCount + ")",
                        Skin.getSkin().getEditFont());
                addChild(time_label);
                time_label.place(last_placed, BOTTOM_LEFT);
                last_placed = time_label;

                Label hint_label = new Label(
                        "Im wiecej jednostek, tym szybciej!",
                        Skin.getSkin().getEditFont());
                addChild(hint_label);
                hint_label.place(last_placed, BOTTOM_LEFT);
                last_placed = hint_label;

                if (building.canUpgrade()) {
                    HorizButton upgrade_button = new HorizButton("Ulepsz do poziomu " + (level + 1), BUTTON_WIDTH);
                    addChild(upgrade_button);
                    upgrade_button.place(last_placed, BOTTOM_LEFT);
                    upgrade_button.addMouseClickListener(new UpgradeListener());
                }
            } else {
                // Economy buildings: resource-based upgrade
                int[] cost = UPGRADE_COSTS[level];

                String costText = "Koszt: " + cost[0] + " Drewno";
                if (cost[1] > 0) costText += ", " + cost[1] + " Kamien";
                if (cost[2] > 0) costText += ", " + cost[2] + " Zelao";

                Label cost_label = new Label(costText, Skin.getSkin().getEditFont());
                addChild(cost_label);
                cost_label.place(last_placed, BOTTOM_LEFT);
                last_placed = cost_label;

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

                    availText = "Dostepne: " + tree_n + " Drewno, " + rock_n + " Kamien, " + iron_n + " Zelao";
                    can_afford = tree_n >= cost[0] && rock_n >= cost[1] && iron_n >= cost[2];
                } else {
                    availText = "Brak magazynu - darmowe ulepszenie";
                    can_afford = true;
                }

                Label avail_label = new Label(availText, Skin.getSkin().getEditFont());
                addChild(avail_label);
                avail_label.place(last_placed, BOTTOM_LEFT);
                last_placed = avail_label;

                if (can_afford && building.canUpgrade()) {
                    HorizButton upgrade_button = new HorizButton("Ulepsz do poziomu " + (level + 1), BUTTON_WIDTH);
                    addChild(upgrade_button);
                    upgrade_button.place(last_placed, BOTTOM_LEFT);
                    upgrade_button.addMouseClickListener(new UpgradeListener());
                } else {
                    Label no_label = new Label(
                            can_afford ? "Nie mozna ulepszyc" : "Brak surowcow",
                            Skin.getSkin().getEditFont());
                    no_label.setColor(new float[]{1f, 0.3f, 0.3f, 1f});
                    addChild(no_label);
                    no_label.place(last_placed, BOTTOM_LEFT);
                }
            }
        } else {
            Label max_label = new Label("Maksymalny poziom osiagniety!", Skin.getSkin().getEditFont());
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
        if (level >= Building.MAX_UPGRADE_LEVEL) return;

        if (isBarracks) {
            // Barracks: instant upgrade (time calculation is for display only in this version)
            building.upgrade();
        } else {
            // Economy buildings: deduct resources
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
        }
        cancel();
    }
}

package com.oddlabs.tt.model;

import com.oddlabs.tt.model.behaviour.AttackController;
import com.oddlabs.tt.model.behaviour.HuntController;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.ScanFilter;
import com.oddlabs.tt.player.Player;

public final strictfp class AllyCombatScanFilter implements ScanFilter {
    public static final int ALLY_RANGE = 12;

    private final Player owner;
    private Selectable ally_target = null;

    public AllyCombatScanFilter(Player owner) {
        this.owner = owner;
    }

    public final Selectable removeAllyTarget() {
        Selectable result = ally_target;
        ally_target = null;
        return result;
    }

    public final int getMinRadius() {
        return 1;
    }

    public final int getMaxRadius() {
        return ALLY_RANGE;
    }

    public final boolean filter(int grid_x, int grid_y, Occupant occ) {
        if (occ instanceof Unit) {
            Unit u = (Unit) occ;
            if (u.getOwner() == owner && !u.isDead()) {
                if (u.getCurrentController() instanceof AttackController
                        || u.getCurrentController() instanceof HuntController) {
                    ally_target = u;
                    return true;
                }
            }
        }
        return false;
    }
}

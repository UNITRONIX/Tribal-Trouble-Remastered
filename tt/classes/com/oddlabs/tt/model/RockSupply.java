package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;

public final strictfp class RockSupply extends SupplyModel {
    private static final int INITIAL_SUPPLIES = 15;

    private final int initial_supplies;

    public RockSupply(
            World world,
            SpriteKey sprite_renderer,
            float size,
            int grid_x,
            int grid_y,
            float x,
            float y,
            float rotation,
            boolean increase) {
        this(world, sprite_renderer, size, grid_x, grid_y, x, y, rotation, INITIAL_SUPPLIES, increase);
    }

    public RockSupply(
            World world,
            SpriteKey sprite_renderer,
            float size,
            int grid_x,
            int grid_y,
            float x,
            float y,
            float rotation,
            int supplies,
            boolean increase) {
        super(
                world,
                sprite_renderer,
                size,
                grid_x,
                grid_y,
                x,
                y,
                rotation,
                supplies,
                increase);
        this.initial_supplies = supplies;
    }

    public final Supply respawn() {
        return new RockSupply(
                getWorld(),
                getSpriteRenderer(),
                getSize(),
                getGridX(),
                getGridY(),
                getPositionX(),
                getPositionY(),
                0,
                initial_supplies,
                false);
    }
}

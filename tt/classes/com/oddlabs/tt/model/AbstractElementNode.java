package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.util.BoundingBox;
import com.oddlabs.util.LinkedList;

import java.util.List;

public abstract strictfp class AbstractElementNode extends BoundingBox {
    private final LinkedList models = new LinkedList();

    private int child_count = 0;

    private final AbstractElementNode owner;

    protected final int getChildCount() {
        return child_count - models.size();
    }

    protected final AbstractElementNode insertElement(Element model) {
        checkBoundsZ(model.bmin_z);
        checkBoundsZ(model.bmax_z);
        return doInsertElement(model);
    }

    protected abstract AbstractElementNode doInsertElement(Element model);

    public final void removeElement(Element model) {
        models.remove(model);
    }

    protected final void incElementCount() {
        child_count++;
    }

    protected final AbstractElementNode reinsertElement(Element model) {
        child_count--;
        assert child_count >= 0;
        if (contains(model)) {
            return insertElement(model);
        } else {
            return owner.reinsertElement(model);
        }
    }

    private int getDepth() {
        if (owner != null) return 1 + owner.getDepth();
        else return 0;
    }

    protected AbstractElementNode(AbstractElementNode owner) {
        this.owner = owner;
    }

    protected final AbstractElementNode addElement(Element model) {
        models.addLast(model);
        return this;
    }

    public static final AbstractElementNode newRoot(HeightMap heightmap) {
        AbstractElementNode root = new ElementNode(null, heightmap.getGridUnitsPerWorld(), 0, 0);
        root.setInfiniteBounds();
        return root;
    }

    public static void buildSupplies(
            World world,
            List iron_positions,
            List rock_positions,
            float[][] plants,
            int terrain_type) {
        buildRockSupplies(world, rock_positions);
        buildIronSupplies(world, iron_positions);
        addPlants(world, plants, terrain_type);
    }

    private static final void buildRockSupplies(World world, List positions) {
        SpriteKey[] sprite_renderers = world.getLandscapeResources().getRockFragments();
        int num_supplies = positions.size();
        System.out.println("num_rocks = " + num_supplies);
        for (int i = 0; i < num_supplies; i++) {
            int[] coords = (int[]) positions.get(i);
            int grid_x = coords[0];
            int grid_y = coords[1];
            float x = UnitGrid.coordinateFromGrid(grid_x) + (world.getRandom().nextFloat() - .5f);
            float y = UnitGrid.coordinateFromGrid(grid_y) + (world.getRandom().nextFloat() - .5f);
            float rotation = world.getRandom().nextFloat() * 360f;
            boolean mega = (i % 5 == 0);
            float size = mega ? 5f : 2f;
            int supplies = mega ? 45 : 15;
            new RockSupply(
                    world,
                    sprite_renderers[i % sprite_renderers.length],
                    size,
                    grid_x,
                    grid_y,
                    x,
                    y,
                    rotation,
                    supplies,
                    true);
        }
    }

    private static final void buildIronSupplies(World world, List positions) {
        SpriteKey[] sprite_renderers = world.getLandscapeResources().getIronFragments();
        int num_supplies = positions.size();
        System.out.println("num_iron = " + num_supplies);
        for (int i = 0; i < num_supplies; i++) {
            int[] coords = (int[]) positions.get(i);
            int grid_x = coords[0];
            int grid_y = coords[1];
            float x = UnitGrid.coordinateFromGrid(grid_x) + (world.getRandom().nextFloat() - .5f);
            float y = UnitGrid.coordinateFromGrid(grid_y) + (world.getRandom().nextFloat() - .5f);
            float rotation = world.getRandom().nextFloat() * 360f;
            boolean mega = (i % 5 == 0);
            float size = mega ? 5f : 2f;
            int supplies = mega ? 45 : 15;
            new IronSupply(
                    world,
                    sprite_renderers[i % sprite_renderers.length],
                    size,
                    grid_x,
                    grid_y,
                    x,
                    y,
                    rotation,
                    supplies,
                    true);
        }
    }

    private static final void addPlants(World world, float[][] plants, int terrain_type) {
        int num_plants = 0;
        for (int t = 0; t < plants.length; t++) {
            num_plants += plants[t].length / 2;
            for (int p = 0; p < plants[t].length >> 1; p++) {
                float dir_x = world.getRandom().nextFloat();
                float dir_y = world.getRandom().nextFloat();
                float len_sqr = dir_x * dir_x + dir_y * dir_y;
                if (len_sqr < .001) {
                    dir_x = 1f;
                    dir_y = 0f;
                } else {
                    float inv_len = 1f / (float) StrictMath.sqrt(len_sqr);
                    dir_x *= inv_len;
                    dir_y *= inv_len;
                }
                new Plants(
                        world,
                        plants[t][2 * p],
                        plants[t][2 * p + 1],
                        dir_x,
                        dir_y,
                        world.getLandscapeResources().getPlants()[terrain_type][t]);
            }
        }
        System.out.println("num_plants = " + num_plants);
    }

    public abstract void visit(ElementNodeVisitor visitor);

    public final void visitElements(ElementNodeVisitor visitor) {
        Element model = (Element) models.getFirst();
        while (model != null) {
            visitor.visit(model);
            model = (Element) model.getNext();
        }
    }
}

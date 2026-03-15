package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public final strictfp class HealthBarRenderer {
    private static final float BAR_WIDTH = 1.6f;
    private static final float BAR_HEIGHT = 0.12f;
    private static final float BAR_OFFSET_Z = 2.8f;
    private static final float BUILDING_BAR_WIDTH = 3.0f;
    private static final float BUILDING_BAR_HEIGHT = 0.2f;
    private static final float BUILDING_BAR_OFFSET_Z = 6.0f;
    private static final float MAX_RENDER_DIST_SQ = 200f * 200f;

    private final List entries = new ArrayList();
    private final Player local_player;
    private Object hovered_target;

    public HealthBarRenderer(Player local_player) {
        this.local_player = local_player;
    }

    public void setHoveredTarget(Object target) {
        this.hovered_target = target;
    }

    public void addUnit(Unit unit, float z_offset) {
        if (unit.isDead() || unit.isMounted()) return;
        int hp = unit.getHitPoints();
        int max_hp = unit.getUnitTemplate().getMaxHitPoints();
        int level = unit.getLevel();
        if (hp >= max_hp && level == 0 && unit != hovered_target) return;
        entries.add(
                new HealthBarEntry(
                        unit.getPositionX(),
                        unit.getPositionY(),
                        z_offset + BAR_OFFSET_Z,
                        (float) hp / max_hp,
                        BAR_WIDTH,
                        BAR_HEIGHT,
                        unit.getOwner(),
                        level));
    }

    public void addBuilding(Building building, float z_offset) {
        if (building.isDead()) return;
        int hp = building.getHitPoints();
        int max_hp = building.getBuildingTemplate().getMaxHitPoints();
        int upgrade = building.getUpgradeLevel();
        if (hp >= max_hp && upgrade == 0 && building != hovered_target) return;
        entries.add(
                new HealthBarEntry(
                        building.getPositionX(),
                        building.getPositionY(),
                        z_offset + BUILDING_BAR_OFFSET_Z,
                        (float) hp / max_hp,
                        BUILDING_BAR_WIDTH,
                        BUILDING_BAR_HEIGHT,
                        building.getOwner(),
                        upgrade));
    }

    public void render(CameraState camera) {
        if (entries.isEmpty()) return;

        float cam_x = camera.getCurrentX();
        float cam_y = camera.getCurrentY();
        float cam_z = camera.getCurrentZ();

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);

        for (int i = 0; i < entries.size(); i++) {
            HealthBarEntry entry = (HealthBarEntry) entries.get(i);

            float dx = entry.x - cam_x;
            float dy = entry.y - cam_y;
            float dz = entry.z - cam_z;
            float dist_sq = dx * dx + dy * dy + dz * dz;
            if (dist_sq > MAX_RENDER_DIST_SQ) continue;

            float half_w = entry.width / 2f;
            float half_h = entry.height / 2f;

            GL11.glPushMatrix();
            GL11.glTranslatef(entry.x, entry.y, entry.z);

            // Billboard: extract camera right and up vectors from modelview
            float[] mv = new float[16];
            GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, mv);
            float rx = mv[0], ry = mv[4], rz = mv[8];
            float ux = mv[1], uy = mv[5], uz = mv[9];

            // Background (dark)
            GL11.glColor4f(0f, 0f, 0f, 0.6f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3f(
                    -half_w * rx - half_h * ux,
                    -half_w * ry - half_h * uy,
                    -half_w * rz - half_h * uz);
            GL11.glVertex3f(
                    half_w * rx - half_h * ux,
                    half_w * ry - half_h * uy,
                    half_w * rz - half_h * uz);
            GL11.glVertex3f(
                    half_w * rx + half_h * ux,
                    half_w * ry + half_h * uy,
                    half_w * rz + half_h * uz);
            GL11.glVertex3f(
                    -half_w * rx + half_h * ux,
                    -half_w * ry + half_h * uy,
                    -half_w * rz + half_h * uz);
            GL11.glEnd();

            // Health fill
            float fill_w = half_w * entry.ratio;
            float start_x_offset = -half_w;
            float end_x_offset = start_x_offset + 2f * fill_w;

            // Color based on health ratio
            float r, g;
            if (entry.ratio > 0.5f) {
                g = 1f;
                r = 2f * (1f - entry.ratio);
            } else {
                r = 1f;
                g = 2f * entry.ratio;
            }
            GL11.glColor4f(r, g, 0f, 0.85f);
            GL11.glBegin(GL11.GL_QUADS);
            float sx = start_x_offset;
            float ex = end_x_offset;
            GL11.glVertex3f(
                    sx * rx - half_h * ux,
                    sx * ry - half_h * uy,
                    sx * rz - half_h * uz);
            GL11.glVertex3f(
                    ex * rx - half_h * ux,
                    ex * ry - half_h * uy,
                    ex * rz - half_h * uz);
            GL11.glVertex3f(
                    ex * rx + half_h * ux,
                    ex * ry + half_h * uy,
                    ex * rz + half_h * uz);
            GL11.glVertex3f(
                    sx * rx + half_h * ux,
                    sx * ry + half_h * uy,
                    sx * rz + half_h * uz);
            GL11.glEnd();

            // Level indicators (small diamonds above health bar)
            if (entry.level > 0) {
                float dot_size = entry.height * 0.6f;
                float dot_spacing = dot_size * 2.2f;
                float total_dots_w = entry.level * dot_spacing;
                float dot_start = -total_dots_w / 2f + dot_spacing / 2f;
                float dot_z_off = half_h + dot_size * 1.5f;

                GL11.glColor4f(1f, 0.85f, 0.2f, 0.9f);
                for (int lv = 0; lv < entry.level; lv++) {
                    float cx = dot_start + lv * dot_spacing;
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex3f(
                            cx * rx + (dot_z_off - dot_size) * ux,
                            cx * ry + (dot_z_off - dot_size) * uy,
                            cx * rz + (dot_z_off - dot_size) * uz);
                    GL11.glVertex3f(
                            (cx + dot_size) * rx + dot_z_off * ux,
                            (cx + dot_size) * ry + dot_z_off * uy,
                            (cx + dot_size) * rz + dot_z_off * uz);
                    GL11.glVertex3f(
                            cx * rx + (dot_z_off + dot_size) * ux,
                            cx * ry + (dot_z_off + dot_size) * uy,
                            cx * rz + (dot_z_off + dot_size) * uz);
                    GL11.glVertex3f(
                            (cx - dot_size) * rx + dot_z_off * ux,
                            (cx - dot_size) * ry + dot_z_off * uy,
                            (cx - dot_size) * rz + dot_z_off * uz);
                    GL11.glEnd();
                }
            }

            GL11.glPopMatrix();
        }

        GL11.glPopAttrib();
        entries.clear();
    }

    private static final class HealthBarEntry {
        final float x, y, z;
        final float ratio;
        final float width, height;
        final Player owner;
        final int level;

        HealthBarEntry(
                float x, float y, float z, float ratio, float width, float height, Player owner, int level) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.ratio = ratio;
            this.width = width;
            this.height = height;
            this.owner = owner;
            this.level = level;
        }
    }
}

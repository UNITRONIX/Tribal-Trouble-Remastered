package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.procedural.GeneratorHalos;
import com.oddlabs.tt.resource.Resources;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

final strictfp class TreeShadowRenderer extends ShadowRenderer {
    private static final float TREE_SHADOW_DIAMETER = 3.5f;

    private final Texture[] halos;
    private final List shadow_x = new ArrayList();
    private final List shadow_y = new ArrayList();
    private final List shadow_size = new ArrayList();

    TreeShadowRenderer() {
        halos = (Texture[]) Resources.findResource(RacesResources.DEFAULT_SHADOW_DESC);
    }

    void addTreeShadow(float x, float y, float scale) {
        if (Globals.process_shadows) {
            shadow_x.add(new Float(x));
            shadow_y.add(new Float(y));
            shadow_size.add(new Float(TREE_SHADOW_DIAMETER * scale));
        }
    }

    void renderShadows(LandscapeRenderer renderer) {
        if (shadow_x.isEmpty()) return;
        setupShadows();
        GL11.glColor3f(1f, 1f, 1f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, halos[GeneratorHalos.SHADOWED].getHandle());
        for (int i = 0; i < shadow_x.size(); i++) {
            float x = ((Float) shadow_x.get(i)).floatValue();
            float y = ((Float) shadow_y.get(i)).floatValue();
            float size = ((Float) shadow_size.get(i)).floatValue();
            renderShadow(renderer, size, x, y);
        }
        resetShadows();
        shadow_x.clear();
        shadow_y.clear();
        shadow_size.clear();
    }
}

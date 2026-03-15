package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.procedural.GeneratorHalos;
import com.oddlabs.tt.resource.ResourceDescriptor;
import com.oddlabs.tt.resource.Resources;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

final strictfp class SelectableShadowRenderer extends ShadowListRenderer {
    private final Texture[] halos;

    private final List selection_list = new ArrayList();
    private final List shadowed_list = new ArrayList();

    public SelectableShadowRenderer(ResourceDescriptor halos_desc) {
        halos = (Texture[]) Resources.findResource(halos_desc);
    }

    public final void addToSelectionList(ModelState model) {
        if (Globals.process_shadows) selection_list.add(model);
    }

    public final void addToShadowList(ModelState model) {
        if (Globals.process_shadows) shadowed_list.add(model);
    }

    protected final void renderShadows(LandscapeRenderer renderer) {
        setupShadows();
        // Circular halo shadows removed - using dynamic lighting only
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, halos[GeneratorHalos.SELECTED].getHandle());
        for (int i = 0; i < selection_list.size(); i++) {
            ModelState model = (ModelState) selection_list.get(i);
            selection_list.set(i, null);
            float[] color = model.getSelectionColor();
            GL11.glColor3f(color[0], color[1], color[2]);
            renderShadow(
                    renderer,
                    model.getModel().getShadowDiameter(),
                    model.getModel().getPositionX(),
                    model.getModel().getPositionY());
        }
        resetShadows();
        selection_list.clear();
        shadowed_list.clear(); // still cleared even though not rendered
    }
}

package com.oddlabs.tt.vbo;

import com.oddlabs.tt.global.*;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.*;

import org.lwjgl.opengl.*;

import java.nio.*;

public final strictfp class IntVBO extends VBO {
    private IntBuffer saved_buffer = null;

    public IntVBO(int usage, int size) {
        super(GL15.GL_ELEMENT_ARRAY_BUFFER, usage, size << 2);
        ByteBuffer buffer = getSavedBuffer();
        if (buffer != null) saved_buffer = buffer.asIntBuffer();
    }

    private static void registerTrianglesRendered(int mode, int count) {
        int num_triangles;
        switch (mode) {
            case GL11.GL_TRIANGLES:
                num_triangles = count / 3;
                break;
            case GL11.GL_TRIANGLE_FAN:
            case GL11.GL_TRIANGLE_STRIP:
                num_triangles = count - 2;
                break;
            default:
                num_triangles = count;
                break;
        }
        Renderer.registerTrianglesRendered(num_triangles);
    }

    public final void put(int[] buffer) {
        IntBuffer buf = Utils.toBuffer(buffer);
        if (!use_vbo) {
            saved_buffer.put(buf);
        } else {
            makeCurrent();
            GL15.glBufferSubData(getTarget(), 0, buf);
        }
    }

    public final void drawElements(int mode, int count, int index) {
        registerTrianglesRendered(mode, count);
        if (!use_vbo) {
            saved_buffer.position(index);
            saved_buffer.limit(index + count);
            GL11.glDrawElements(mode, saved_buffer);
            saved_buffer.clear();
        } else {
            makeCurrent();
            GL11.glDrawElements(mode, count, GL11.GL_UNSIGNED_INT, (long) index << 2);
        }
    }

    public final int capacity() {
        return getSize() >> 2;
    }
}

package com.oddlabs.tt.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * VAO-based fullscreen quad for post-processing.
 * First step of the gradual OpenGL 3.3 migration.
 * Replaces immediate-mode glBegin/glVertex calls with modern vertex attributes.
 */
public final strictfp class FullscreenQuad {
    private static final float[] VERTICES = {
        // position (x,y), texcoord (u,v)
        -1f, -1f,  0f, 0f,
         1f, -1f,  1f, 0f,
         1f,  1f,  1f, 1f,
        -1f,  1f,  0f, 1f
    };
    private static final int STRIDE = 4 * 4; // 4 floats * 4 bytes

    private int vao;
    private int vbo;
    private boolean initialized = false;

    public void init() {
        if (initialized) return;

        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buf = ByteBuffer.allocateDirect(VERTICES.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buf.put(VERTICES).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);

        // attribute 0: position (vec2)
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, STRIDE, 0);
        // attribute 1: texcoord (vec2)
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, STRIDE, 2 * 4);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        initialized = true;
    }

    public void draw() {
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        GL30.glBindVertexArray(0);
    }

    public void delete() {
        if (!initialized) return;
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        initialized = false;
    }
}

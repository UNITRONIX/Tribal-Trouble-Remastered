package com.oddlabs.tt.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public final strictfp class ShaderProgram {
    private final int program;

    public ShaderProgram(String vertexSource, String fragmentSource) {
        int vert = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
        int frag = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vert);
        GL20.glAttachShader(program, frag);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 1024);
            throw new RuntimeException("Shader link failed: " + log);
        }

        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);
    }

    private static int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 1024);
            throw new RuntimeException("Shader compile failed: " + log);
        }
        return shader;
    }

    public void bind() {
        GL20.glUseProgram(program);
    }

    public static void unbind() {
        GL20.glUseProgram(0);
    }

    public int getUniformLocation(String name) {
        return GL20.glGetUniformLocation(program, name);
    }

    public void setUniform1i(String name, int value) {
        GL20.glUniform1i(getUniformLocation(name), value);
    }

    public void setUniform1f(String name, float value) {
        GL20.glUniform1f(getUniformLocation(name), value);
    }

    public void setUniform2f(String name, float x, float y) {
        GL20.glUniform2f(getUniformLocation(name), x, y);
    }

    public void delete() {
        GL20.glDeleteProgram(program);
    }
}

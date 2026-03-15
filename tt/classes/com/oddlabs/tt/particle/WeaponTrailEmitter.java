package com.oddlabs.tt.particle;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.TextureKey;
import com.oddlabs.util.Vector3f;
import com.oddlabs.util.Vector4f;

import org.lwjgl.opengl.GL11;

public final strictfp class WeaponTrailEmitter extends LinearEmitter {
    private static final float TRAIL_PARTICLES_PER_SECOND = 30f;
    private static final float TRAIL_ENERGY = 0.3f;
    private static final float TRAIL_PARTICLE_SIZE = 0.15f;

    public WeaponTrailEmitter(
            World world,
            float x,
            float y,
            float z,
            TextureKey[] textures,
            AnimationManager manager) {
        super(
                world,
                new Vector3f(x, y, z),
                0f,
                0f,
                0f,
                -1,
                TRAIL_PARTICLES_PER_SECOND,
                new Vector3f(0f, 0f, 0f),
                new Vector3f(0f, 0f, 0f),
                new Vector4f(1f, 1f, 1f, 0.6f),
                new Vector4f(0f, 0f, 0f, -2f),
                new Vector3f(TRAIL_PARTICLE_SIZE, TRAIL_PARTICLE_SIZE, TRAIL_PARTICLE_SIZE),
                new Vector3f(-0.1f, -0.1f, -0.1f),
                TRAIL_ENERGY,
                0f,
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE,
                textures,
                null,
                textures.length,
                manager);
    }

    public final void updatePosition(float x, float y, float z) {
        setPosition(new Vector3f(x, y, z));
    }

    protected int initParticle(
            Vector3f position,
            Vector3f velocity,
            Vector3f acceleration,
            Vector4f color,
            Vector4f delta_color,
            Vector3f particle_radius,
            Vector3f growth_rate,
            float energy) {
        LinearParticle particle = new LinearParticle();
        particle.setPos(position.getX(), position.getY(), position.getZ());
        particle.setVelocity(0f, 0f, 0f);
        particle.setAcceleration(0f, 0f, 0f);
        particle.setColor(color.getX(), color.getY(), color.getZ(), color.getW());
        particle.setDeltaColor(
                delta_color.getX(), delta_color.getY(), delta_color.getZ(), delta_color.getW());
        particle.setRadius(
                particle_radius.getX(), particle_radius.getY(), particle_radius.getZ());
        particle.setGrowthRate(growth_rate.getX(), growth_rate.getY(), growth_rate.getZ());
        particle.setEnergy(energy);
        particle.setType(0);
        add(particle);
        return 1;
    }
}

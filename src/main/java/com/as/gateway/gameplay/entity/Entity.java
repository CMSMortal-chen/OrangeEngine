package com.as.gateway.gameplay.entity;

import org.joml.Vector2f;

public abstract class Entity {
    public int spriteSize = 32;
    public Vector2f pos = new Vector2f(0,0);

    public abstract void render(long vg);
}

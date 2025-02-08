package com.as.gateway.gameplay.entity;

import org.joml.Vector2f;

public class PlayerEntity extends Entity {
    public PlayerEntity(){

    }
    public PlayerEntity(Vector2f pos){
        this.pos.set(pos);
    }

    @Override
    public void render(long vg) {

    }
}

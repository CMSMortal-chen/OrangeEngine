package com.as.gateway.gameplay.map.trigger;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.joml.Vector2f;

@RequiredArgsConstructor
public abstract class Trigger {
    public Vector2f pos = new Vector2f(0f,0f);

    @NonNull
    public String name;

    public Trigger(String name, Vector2f pos){
        this.pos = pos;
        this.name = name;
    }

    public void update(Vector2f playerPos){
        if (playerPos.equals(pos,0.1f)){
            trigger();
        }
    }

    public abstract void trigger();
}

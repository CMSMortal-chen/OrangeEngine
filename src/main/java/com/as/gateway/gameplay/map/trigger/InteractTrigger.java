package com.as.gateway.gameplay.map.trigger;

import lombok.NonNull;
import org.joml.Vector2f;

public abstract class InteractTrigger extends Trigger{

    public InteractTrigger(@NonNull String name) {
        super(name);
    }

    public InteractTrigger(String name, Vector2f pos) {
        super(name, pos);
    }

    @Override
    public abstract void trigger();

    @Override
    public void update(Vector2f playerPos){
    }
}

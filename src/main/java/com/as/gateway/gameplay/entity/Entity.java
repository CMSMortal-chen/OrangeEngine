package com.as.gateway.gameplay.entity;

import com.as.gateway.gameplay.entity.movement.controller.MovementController;
import com.as.gateway.gameplay.entity.movement.controller.NPCController;
import com.as.gateway.gameplay.map.Direction;
import lombok.Getter;
import org.joml.Vector2f;

public abstract class Entity {
    public MovementController controller = new NPCController(this);

    @Getter
    protected Direction currentDirection = Direction.S;
    public int spriteSize = 32;
    public Vector2f pos = new Vector2f(0,0);

    public void setCurrentDirection(Direction direction){
        if (direction != null){
            currentDirection = direction;
        }else {
            direction = Direction.S;
        }
    }

    public void onUpdate(){

    }

    public void teleport(Vector2f pos){
        this.pos = pos;
        if (!controller.actions.isEmpty()) {
            controller.actions.clear();
        }
    }

    public abstract void render(long vg, float x, float y);
}

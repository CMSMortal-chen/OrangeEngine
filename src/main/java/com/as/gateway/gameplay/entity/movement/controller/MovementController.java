package com.as.gateway.gameplay.entity.movement.controller;

import com.as.gateway.gameplay.entity.movement.pathfinding.Action;
import com.as.gateway.gameplay.entity.movement.pathfinding.Paths;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2f;

import java.util.concurrent.CopyOnWriteArrayList;

public abstract class MovementController {
    public CopyOnWriteArrayList<Action> actions = new CopyOnWriteArrayList<>();
    public boolean isWalking = false;

    @Getter
    @Setter
    public float speed = 10f;

    public abstract void handleMovement();

    protected void handleMovement(Vector2f pos) {
        if (!actions.isEmpty()) {
            Action action = actions.get(actions.size() - 1);
            if (action.origin == null) {
                action.origin = new Vector2f(pos);
            }
            if (action.move(pos, speed)) {
                actions.remove(action);
            }
        }
    }
    public void handleControl() {

    }

    public void pathTo(Paths paths){
        actions = paths.getActions();
    }
}

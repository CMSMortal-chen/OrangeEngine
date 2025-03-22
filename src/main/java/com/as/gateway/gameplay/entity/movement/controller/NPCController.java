package com.as.gateway.gameplay.entity.movement.controller;

import com.as.gateway.gameplay.entity.Entity;


public class NPCController extends MovementController{
    public Entity entity;

    public NPCController(Entity entity){
        this.entity = entity;
    }


    @Override
    public void handleMovement() {
        handleMovement(entity.pos);
    }
}

package com.as.gateway.gameplay.render;

import cms.mortalchen.orange.utils.render.anim.AnimationUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.joml.Vector2f;

@RequiredArgsConstructor
public class Camera {
    @Getter
    @Setter
    @NonNull
    public Vector2f pos;
    public Vector2f targetPos;

    public void updatePos(Vector2f pos){
        if (targetPos == null){
            targetPos = new Vector2f(pos);
        }
        if (!targetPos.equals(pos)) {
            targetPos = pos;
        }
        if (!pos.equals(targetPos)) {
            pos.x = AnimationUtil.moveUD(pos.x,targetPos.x,20f,14f);
            pos.y = AnimationUtil.moveUD(pos.y,targetPos.y,20f,14f);
        }
    }
}

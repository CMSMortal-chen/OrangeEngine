package com.as.gateway.gameplay.entity.movement.pathfinding;

import cms.mortalchen.orange.utils.render.anim.AnimationUtil;
import com.as.gateway.gameplay.map.Direction;
import org.joml.Vector2f;

public class Action {
    public Direction direction;
    public int steps;
    public Vector2f origin = null;

    public Action(Direction direction, int steps) {
        this.direction = direction;
        this.steps = steps;
    }

    public boolean isYFinished(Vector2f pos,float speed) {
        // 提取方向分量避免重复调用getter
        final float dy = direction.getDy();

        // 计算目标坐标
        final float targetY = Math.round(origin.y + steps * dy);

        // 使用三元运算符简化条件判断
        final boolean yFinish = (dy >= 0)  // 使用原始方向判断更直观
                ? (pos.y >= targetY)
                : (pos.y <= targetY);

        // 坐标修正逻辑合并（保持原子性）
        pos.y = AnimationUtil.getAnimationState(pos.y, targetY, speed);

        return yFinish;
    }

    public boolean isXFinished(Vector2f pos, float speed) {
        // 提取方向分量避免重复调用getter
        final float dx = direction.getDx();

        // 计算目标坐标
        final float targetX = Math.round(origin.x + steps * dx);

        // 使用三元运算符简化条件判断
        final boolean xFinish = (dx >= 0)
                ? (pos.x >= targetX)
                : (pos.x <= targetX);
        // 坐标修正逻辑合并（保持原子性）
        pos.x = AnimationUtil.getAnimationState(pos.x, targetX, speed);

        return xFinish;
    }

    public boolean move(Vector2f pos, float speed) {
        boolean isXFinished = isXFinished(pos, speed);
        boolean isYFinished = isYFinished(pos, speed);
        return isXFinished && isYFinished;
    }


    @Override
    public String toString() {
        return direction + " 走 " + steps + " 格";
    }
}
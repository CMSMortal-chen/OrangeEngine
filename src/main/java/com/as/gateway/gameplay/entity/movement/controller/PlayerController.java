package com.as.gateway.gameplay.entity.movement.controller;

import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.input.InputManager;
import cms.mortalchen.orange.input.KeyBinding;
import com.as.gateway.gameplay.entity.PlayerEntity;
import com.as.gateway.gameplay.map.Direction;
import com.as.gateway.gameplay.map.TileObj;
import com.as.gateway.gameplay.entity.movement.pathfinding.Action;
import com.as.gateway.states.rpg.RPGGame;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.joml.Vector2f;

@RequiredArgsConstructor
public class PlayerController extends MovementController{
    public boolean isCollidingWithWall = false;

    @NonNull
    public PlayerEntity player;

    public void handleMovement() {
        handleMovement(player.pos);
    }

    @Override
    public void handleMovement(Vector2f pos) {
        if (!actions.isEmpty()) {
            Action action = actions.get(actions.size() - 1);
            if (action.origin == null) {
                action.origin = new Vector2f(pos);
            }
            if (action.move(pos,10f)) {
                actions.remove(action);
            }
        }
    }

    public void handleControl() {
        InputManager inputManager = OrangeEngine.instance.inputManager;

        int x = 0;
        int y = 0;

        if (inputManager.isKeyPressed(KeyBinding.UP.get())) {
            y = -1;
        } else if (inputManager.isKeyPressed(KeyBinding.DOWN.get())) {
            y = 1;
        }

        if (inputManager.isKeyPressed(KeyBinding.LEFT.get())) {
            x = -1;
        } else if (inputManager.isKeyPressed(KeyBinding.RIGHT.get())) {
            x = 1;
        }

        Direction direction = Direction.getDirection(x, y);

        isWalking = !actions.isEmpty() || direction != null;

        player.setCurrentDirection(direction);

        TileObj tileObj = new TileObj(new Vector2f(player.pos).add(player.getCurrentDirection().getDx() * 0.5f, player.getCurrentDirection().getDy() * 0.5f), false);

        if (RPGGame.instance.currentMap.barrierTiles.stream().anyMatch(tileObj1 -> tileObj1.equal(tileObj))) {
            actions.clear();
            handleCollisionPush();
        } else if (direction != null) {
            if (actions.isEmpty()) {
                actions.add(new Action(player.getCurrentDirection(), 1));
            } else {
                actions.set(0, new Action(player.getCurrentDirection(), 1));
            }
        }
    }
    private void handleCollisionPush() {
        // 获取玩家所在网格
        float currentTileX = player.pos.x;
        float currentTileY = player.pos.y;

        // 创建当前Tile检测对象
        TileObj currentTile = new TileObj(
                new Vector2f(currentTileX, currentTileY),
                false
        );

        // 检查当前是否在障碍物中
        boolean isInBarrier = RPGGame.instance.currentMap.barrierTiles.stream()
                .anyMatch(barrierTile -> barrierTile.equal(currentTile));

        if (isInBarrier && player.getCurrentDirection() != null) {
            // 获取移动方向的反方向
            Direction reverseDir = player.getCurrentDirection().getOpposite();

            // 根据方向类型计算推离量
            float pushForce = 0.2f;
            if (reverseDir.isDiagonal()) { // 处理对角线推离
                pushForce *= 0.707f; // 1/√2 保持对角线方向速度一致
            }

            Vector2f pushOffset = new Vector2f(
                    reverseDir.getDx() * pushForce,
                    reverseDir.getDy() * pushForce
            );

            // 应用推离
            player.pos.add(pushOffset);
            clampPositionToLegalArea();

            // 二次验证防止连续卡墙
            if (isStillColliding()) {
                player.pos.set(currentTileX + 0.5f, currentTileY + 0.5f);
            }
        }
    }

    private void clampPositionToLegalArea() {
        // 假设地图尺寸为mapWidth和mapHeight
        float halfWidth = RPGGame.instance.currentMap.mapWidth;
        float halfHeight = RPGGame.instance.currentMap.mapHeight;

        // 使用更平滑的钳制方式
        player.pos.x = Math.max(0f, Math.min(player.pos.x, halfWidth));
        player.pos.y = Math.max(0f, Math.min(player.pos.y, halfHeight));

        // 如果卡在角落则对齐中心
        if (isStillColliding()) {
            player.pos.x = player.pos.x + 0f;
            player.pos.y = player.pos.y + 0f;
        }
    }

    // 新增辅助方法
    private boolean isStillColliding() {
        float currentTileX = player.pos.x;
        float currentTileY = player.pos.y;
        TileObj checkTile = new TileObj(
                new Vector2f(currentTileX, currentTileY),
                false
        );
        return RPGGame.instance.currentMap.barrierTiles.stream()
                .anyMatch(t -> t.equal(checkTile));
    }
}

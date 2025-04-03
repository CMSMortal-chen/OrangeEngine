package com.as.gateway.gameplay.entity;

import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.Image;
import cms.mortalchen.orange.resource.ResourceLocation;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.time.TimerUtil;
import com.as.gateway.gameplay.entity.movement.controller.PlayerController;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;

public class PlayerEntity extends Entity {
    public Image playerImage = new Image(OrangeEngine.resourceManager, new ResourceLocation("/textures/image/player.png"), NanoVG.NVG_IMAGE_NEAREST);
    public int walkingAnim = 0;
    private TimerUtil walkingTimer = new TimerUtil();

    public PlayerEntity() {
        controller = new PlayerController(this);
    }

    public PlayerEntity(Vector2f pos) {
        controller = new PlayerController(this);
        this.pos.set(pos);
    }

    @Override
    public void render(long vg, float x, float y) {
        Vector2i uv = getImageUV();
        //RenderUtils.drawRect(vg, x, y, x + (32), y + (32), new Color(255,0,0,100));

        //shadow
        RenderUtils.drawRect(vg, x + 7, y + (32) - 5, x + (32) - 7, y + (32) - 2, new Color(0,0,0,80));
        RenderUtils.drawRect(vg, x + 4, y + (32) - 2, x + (32) - 4, y + (32) + 1, new Color(0,0,0,80));
        RenderUtils.drawRect(vg, x + 7, y + (32) + 1, x + (32) - 7, y + (32) + 4, new Color(0,0,0,80));

        RenderUtils.drawTileImage(vg, playerImage.getLocation(), x - 4, y - 20, 120, 112, (uv.x + walkingAnim) * 20, (uv.y * 28) + 1, 2f, 20, 28, 255, Color.WHITE);
    }

    @Override
    public void onUpdate() {
        if (controller.isWalking) {
            if (walkingTimer.hasReached(1200L)) {
                walkingTimer.reset();
            } else if (walkingTimer.hasReached(900L)) {
                walkingAnim = 0;
            } else if (walkingTimer.hasReached(600L)) {
                walkingAnim = 1;
            } else if (walkingTimer.hasReached(300L)) {
                walkingAnim = 0;
            } else if (walkingTimer.hasReached(0L)) {
                walkingAnim = -1;
            }
        } else {
            walkingTimer.reset();
            walkingAnim = 0;
        }
    }


    private Vector2i getImageUV() {
        int x = 1;
        int y = -1;
        switch (currentDirection) {
            case N -> {
                y = 3;
            }
            case NE -> {
                x = 4;
                y = 3;
            }
            case E -> {
                y = 2;
            }
            case SE -> {
                x = 4;
                y = 1;
            }
            case S -> {
                y = 0;
            }
            case SW -> {
                x = 4;
                y = 0;
            }
            case W -> {
                y = 1;
            }
            case NW -> {
                x = 4;
                y = 2;
            }
        }
        return new Vector2i(x, y);
    }
}

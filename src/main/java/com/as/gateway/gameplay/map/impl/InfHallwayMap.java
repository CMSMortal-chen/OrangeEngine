package com.as.gateway.gameplay.map.impl;

import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.Image;
import cms.mortalchen.orange.resource.ResourceLocation;
import com.as.gateway.gameplay.map.Map;
import com.as.gateway.gameplay.map.TileObj;
import com.as.gateway.states.rpg.RPGGame;
import org.joml.Vector2f;
import org.lwjgl.nanovg.NanoVG;

public class InfHallwayMap extends Map {
    public InfHallwayMap() {
        super(60,40, new Image(OrangeEngine.resourceManager, new ResourceLocation("/textures/image/map/map_1739112436942.jpg"), NanoVG.NVG_IMAGE_NEAREST));
        for (int i = -1; i < 41; i++) {
            barrierTiles.add(new TileObj(new Vector2f(27, i), true));
            barrierTiles.add(new TileObj(new Vector2f(32, i), true));
        }
        init();
    }

    @Override
    public void update() {
        if (RPGGame.instance.player.pos.y < 15) {
            RPGGame.instance.player.teleport(new Vector2f(RPGGame.instance.player.pos.x, 22f));
        } else if (RPGGame.instance.player.pos.y > 22) {
            RPGGame.instance.player.teleport(new Vector2f(RPGGame.instance.player.pos.x, 15f));
        }
    }
}

package com.as.gateway.gameplay.map.impl;

import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.Image;
import cms.mortalchen.orange.resource.ResourceLocation;
import com.as.gateway.gameplay.map.Map;
import com.as.gateway.gameplay.map.TileObj;
import com.as.gateway.states.rpg.RPGGame;
import org.joml.Vector2f;
import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;
import java.util.HashMap;

public class MCHouseMap extends Map {
    public MCHouseMap() {
        super(60,34, new Image(OrangeEngine.resourceManager, new ResourceLocation("/textures/image/map/主角家_主角房间.png"), NanoVG.NVG_IMAGE_NEAREST));
        ArrayList<Vector2f> barriers = new ArrayList<>();

        barriers.add(new Vector2f(18,6));
        barriers.add(new Vector2f(19,6));

        barriers.add(new Vector2f(27,6));
        barriers.add(new Vector2f(28,6));

        barriers.add(new Vector2f(32,6));
        barriers.add(new Vector2f(33,6));
        barriers.add(new Vector2f(34,6));

        barriers.add(new Vector2f(37,6));
        barriers.add(new Vector2f(38,6));

        barriers.add(new Vector2f(39,6));
        barriers.add(new Vector2f(40,6));
        barriers.add(new Vector2f(39,7));
        barriers.add(new Vector2f(40,7));

        barriers.add(new Vector2f(33,7));

        for (int i = 2; i < 17; i++) {
            barriers.add(new Vector2f(17,i));
        }
        for (int i = 23; i < 42; i++) {
            barriers.add(new Vector2f(i,12));
        }
        for (int i2 = 2; i2 < 6; i2++) {
            for (int i = 17; i < 42; i++) {
                barriers.add(new Vector2f(i, i2));
            }
        }

        for (int i = 2; i < 13; i++) {
            barriers.add(new Vector2f(41,i));
        }

        for (int i = 12; i < 17; i++) {
            barriers.add(new Vector2f(23,i));
        }
        barriers.add(new Vector2f(18,16));
        barriers.add(new Vector2f(19,16));
        barriers.add(new Vector2f(19,17));
        barriers.add(new Vector2f(20,17));
        barriers.add(new Vector2f(21,17));
        barriers.add(new Vector2f(21,16));
        barriers.add(new Vector2f(22,16));
        barriers.add(new Vector2f(23,16));

        barriers.forEach(vector2f -> {
            barrierTiles.add(new TileObj(vector2f, true));
        });

        init();
    }

    @Override
    public void update() {

    }
}

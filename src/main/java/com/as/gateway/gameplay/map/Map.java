package com.as.gateway.gameplay.map;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.render.Image;
import cms.mortalchen.orange.utils.render.RenderUtils;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.as.gateway.gameplay.entity.movement.pathfinding.AStarPathfinder;
import com.as.gateway.gameplay.entity.movement.pathfinding.Node;
import com.as.gateway.gameplay.map.trigger.Trigger;
import com.as.gateway.states.rpg.RPGGame;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.joml.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Map {
    Node[][] grid;
    public AStarPathfinder pathfinder;
    public ArrayList<TileObj> barrierTiles = new ArrayList<>();

    public CopyOnWriteArrayList<Trigger> triggers = new CopyOnWriteArrayList<>();
    public Color backgroundColor = new Color(5, 5, 5);

    @NonNull
    public Integer mapWidth, mapHeight;

    @NonNull
    public Image mapImage;

    public transient float animPosX = Float.NEGATIVE_INFINITY;
    public transient float animPosY = Float.NEGATIVE_INFINITY;

    public void render(long nvg, Vector2f offsetPos,Vector2f originPoint) {
        RenderUtils.drawRect(nvg, -10, -10, MainGame.WINDOW_WIDTH + 10, MainGame.WINDOW_HEIGHT + 10, backgroundColor);
//        if (Float.isInfinite(animPosX)){
//            animPosX = cameraPos.x * (RPGGame.tileSize * RPGGame.scale);
//        }
//        if (Float.isInfinite(animPosY)){
//            animPosY = cameraPos.y * (RPGGame.tileSize * RPGGame.scale);
//        }
//        if (animPosX != cameraPos.x * (RPGGame.tileSize * RPGGame.scale)){
//            animPosX = AnimationUtil.moveUD(animPosX,cameraPos.x * (RPGGame.tileSize * RPGGame.scale),20f,16f);
//        }
//        if (animPosY != cameraPos.y * (RPGGame.tileSize * RPGGame.scale)){
//            animPosY = AnimationUtil.moveUD(animPosY,cameraPos.y * (RPGGame.tileSize * RPGGame.scale),20f,16f);
//        }
        mapImage.render(-((offsetPos.x * (RPGGame.tileSize * RPGGame.scale)) - originPoint.x),
                -((offsetPos.y * (RPGGame.tileSize * RPGGame.scale)) - originPoint.y),
                mapWidth * (RPGGame.tileSize * RPGGame.scale),
                mapHeight * (RPGGame.tileSize * RPGGame.scale), Color.WHITE);

        if (RPGGame.instance.isDebug) {
            barrierTiles.forEach(tileObj -> {
                RenderUtils.drawRect(nvg,
                        -(((offsetPos.x - tileObj.pos.x) * (RPGGame.tileSize * RPGGame.scale)) - originPoint.x)
                        , -(((offsetPos.y - tileObj.pos.y) * (RPGGame.tileSize * RPGGame.scale)) - originPoint.y),
                        -(((offsetPos.x - tileObj.pos.x) * (RPGGame.tileSize * RPGGame.scale)) - originPoint.x) + 32
                        , -(((offsetPos.y - tileObj.pos.y) * (RPGGame.tileSize * RPGGame.scale)) - originPoint.y) + 32, new Color(255, 0, 0, 100));
            });
        }
    }

    public void update(){

    }

    public String toJSON(){
        return JSONObject.toJSONString(this, JSONWriter.Feature.PrettyFormat);
    }

    public void init(){
        initGrid();
        pathfinder = new AStarPathfinder(grid);
        barrierTiles = barrierTiles.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
    }

    public void initGrid(){
        grid = new Node[mapWidth][mapHeight];
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                grid[x][y] = new Node(x, y);
                int finalX = x;
                int finalY = y;
                if (barrierTiles.stream().anyMatch(tileObj -> tileObj.pos.equals(new Vector2f(finalX, finalY)))) {
                    grid[x][y].isObstacle = true;
                }
            }
        }
    }
}

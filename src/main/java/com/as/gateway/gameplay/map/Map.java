package com.as.gateway.gameplay.map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;

@RequiredArgsConstructor
public class Map {
    public HashMap<Integer, TileObj> tileObjIDMap = new HashMap<>();
    public ArrayList<Integer> tiles = new ArrayList<>();
    public ArrayList<TileObj> barrierTiles = new ArrayList<>();

    @NonNull
    public Integer mapWidth, mapHeight;

    public void render(long nvg, Vector2f cameraPos){

    }
}

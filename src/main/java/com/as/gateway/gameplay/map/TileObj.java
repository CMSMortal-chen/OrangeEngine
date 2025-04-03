package com.as.gateway.gameplay.map;

import lombok.AllArgsConstructor;
import org.joml.Vector2f;

@AllArgsConstructor
public class TileObj {
    public Vector2f pos;
    public boolean isBarrier;

    public boolean equal(TileObj pos) {
        return this.pos.equals(pos.pos,0.6f);
    }
}

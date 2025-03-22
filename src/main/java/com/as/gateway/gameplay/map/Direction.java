package com.as.gateway.gameplay.map;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Direction {
    N(0, -1),
    NE(1, -1),
    E(1, 0),
    SE(1, 1),
    S(0, 1),
    SW(-1, 1),
    W(-1, 0),
    NW(-1, -1);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public static Direction getDirection(int x, int y) {
        return Arrays.stream(Direction.values()).filter(direction -> (direction.dx == x && direction.dy == y)).findFirst().orElse(null);
    }

    // 对角线判断
    public boolean isDiagonal() {
        return dx != 0 && dy != 0;
    }

    public Direction getOpposite() {
        return switch (this) {
            case N -> S;
            case NE -> SW;
            case E -> W;
            case SE -> NW;
            case S -> N;
            case SW -> NE;
            case W -> E;
            case NW -> SE;
        };
    }
}
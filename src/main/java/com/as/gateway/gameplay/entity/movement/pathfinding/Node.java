package com.as.gateway.gameplay.entity.movement.pathfinding;

public class Node {
    public int x, y;
    // 从起点到当前节点的实际代价
    public double costFromStart;
    // 从当前节点到终点的预估代价（启发式函数）
    public double estimatedCostToGoal;
    // 总代价：实际代价与预估代价之和
    public double totalCost;

    // 用于回溯路径的父节点
    public Node parent;
    // 标记是否为障碍
    public boolean isObstacle;

    public Node(int x, int y) {
        this.x = x;
        this.y = y;
        this.isObstacle = false;
    }
    public Node(float x, float y) {
        this.x = Math.round(x);
        this.y = Math.round(y);
        this.isObstacle = false;
    }
}
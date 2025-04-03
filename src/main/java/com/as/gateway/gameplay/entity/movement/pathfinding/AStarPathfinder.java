package com.as.gateway.gameplay.entity.movement.pathfinding;

import com.as.gateway.gameplay.map.Direction;

import java.util.*;

public class AStarPathfinder {
    private Node[][] grid;
    private int rows;
    private int cols;

    public AStarPathfinder(Node[][] grid) {
        this.grid = grid;
        this.rows = grid[0].length;
        this.cols = grid.length;
    }

    // 判断 (x, y) 是否在合法范围内
    private boolean isValid(int x, int y) {
        return x >= 0 && x < cols && y >= 0 && y < rows;
    }

    // 判断该坐标是否可行走（不超出范围且非障碍）
    private boolean isWalkable(int x, int y) {
        return isValid(x, y) && !grid[x][y].isObstacle;
    }

    // 使用 Octile Distance 作为启发式函数
    private double heuristic(Node a, Node b) {
        int dx = Math.abs(a.x - b.x);
        int dy = Math.abs(a.y - b.y);
        double D = 1;
        double D2 = Math.sqrt(2);
        return D * (dx + dy) + (D2 - 2 * D) * Math.min(dx, dy);
    }

    public List<Node> findPath(Node start, Node end) {
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingDouble(n -> n.totalCost));
        Set<Node> closedSet = new HashSet<>();

        start.costFromStart = 0;
        start.estimatedCostToGoal = heuristic(start, end);
        start.totalCost = start.costFromStart + start.estimatedCostToGoal;
        openList.add(start);
        Node current = null;
        while (!openList.isEmpty()) {
            current = openList.poll();
            if (current == end) {
                return reconstructPath(end);
            }
            closedSet.add(current);

            // 遍历所有八个方向
            for (Direction d : Direction.values()) {
                int nx = current.x + d.getDx();
                int ny = current.y + d.getDy();
                if (!isWalkable(nx, ny)) continue;
                Node neighbor = grid[nx][ny];
                if (closedSet.contains(neighbor)) continue;

                // 根据方向计算步长：水平或垂直为 1，对角为 √2
                double stepCost = (Math.abs(d.getDx()) + Math.abs(d.getDy()) == 1) ? 1 : Math.sqrt(2);
                double tentativeCostFromStart = current.costFromStart + stepCost;

                if (!openList.contains(neighbor) || tentativeCostFromStart < neighbor.costFromStart) {
                    neighbor.parent = current;
                    neighbor.costFromStart = tentativeCostFromStart;
                    neighbor.estimatedCostToGoal = heuristic(neighbor, end);
                    neighbor.totalCost = neighbor.costFromStart + neighbor.estimatedCostToGoal;
                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
        }
        // 如果无法找到路径，则返回目前已完成的部分
        if (current != null) {
            return reconstructPath(current);
        }else {
            return new ArrayList<>();
        }
    }

    // 回溯构造路径（从终点到起点，然后反转）
    private List<Node> reconstructPath(Node end) {
        List<Node> path = new ArrayList<>();
        Node current = end;
        while (current != null) {
            path.add(current);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    public void updateGrid(Node[][] grid){
        this.grid = grid;
        this.rows = grid[0].length;
        this.cols = grid.length;
    }
}
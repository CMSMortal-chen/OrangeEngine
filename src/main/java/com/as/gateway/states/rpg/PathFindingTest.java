package com.as.gateway.states.rpg;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.states.GameState;
import cms.mortalchen.orange.utils.render.RenderUtils;
import com.as.gateway.gameplay.entity.movement.pathfinding.AStarPathfinder;
import com.as.gateway.gameplay.entity.movement.pathfinding.Action;
import com.as.gateway.gameplay.entity.movement.pathfinding.Node;
import com.as.gateway.gameplay.entity.movement.pathfinding.Paths;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PathFindingTest extends GameState {

    Node[][] grid;
    Paths actionList;

    public PathFindingTest(MainGame game) {
        super(game);
        int rows = 10, cols = 10;
        grid = new Node[rows][cols];
        // 初始化网格
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                grid[x][y] = new Node(x, y);
            }
        }

        // 设置部分障碍，例如在 (3,3) 到 (3,6) 之间设置障碍
        for (int y = 3; y <= 6; y++) {
            grid[3][y].isObstacle = true;
        }

        for (int y = 3; y <= 8; y++) {
            grid[7][y].isObstacle = true;
        }

        for (int y = 2; y <= 7; y++) {
            grid[y][4].isObstacle = true;
        }

        // 定义起点和终点
        Node start = grid[0][0];
        Node end = grid[9][9];

        // 执行 A* 寻路
        AStarPathfinder pathfinder = new AStarPathfinder(grid);
        List<Node> path = pathfinder.findPath(start, end);

        if (path.isEmpty()) {
            System.out.println("无法找到路径！");
        } else {
            // 使用 ActionList 压缩路径并输出动作列表
            actionList = Paths.compressToPaths(path);
            System.out.println("路径动作列表：");
            System.out.println(actionList);
        }
    }

    /**
     * 内容渲染
     *
     * @param vg NanoVg实例
     */
    @Override
    public void render(long vg) {
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                Node currentNode = grid[x][y];
                Color color = new Color(0,128,255);
                if (currentNode.isObstacle){
                    color = new Color(255,0,0);
                }
                RenderUtils.drawRect(vg,x * 31,y * 31,(x * 31) + 30,(y * 31) + 30, color);
            }
        }
        ArrayList<Node> nodes = new ArrayList<>();
        int x = 0;
        int y = 0;
        nodes.add(new Node(0,0));
        for (int i = 0; i < actionList.getActions().size(); i++) {
            Action action = actionList.getActions().get(i);
            for (int j = 0; j < action.steps; j++) {
                x += action.direction.getDx();
                y += action.direction.getDy();
                nodes.add(new Node(x,y));
            }
        }
        nodes.forEach(node -> {
            RenderUtils.drawRect(vg,node.x * 31,node.y * 31,(node.x * 31) + 30,(node.y * 31) + 30, Color.YELLOW);
        });
        RenderUtils.drawRect(vg,9 * 31,9 * 31,(9 * 31) + 30,(9 * 31) + 30, Color.GREEN);
    }

    /**
     * 游戏更新
     *
     * @param deltaTime deltatime
     */
    @Override
    public void updateTick(float deltaTime) {

    }
}

package com.as.gateway.gameplay.entity.movement.pathfinding;

import com.as.gateway.gameplay.map.Direction;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class Paths {
    // 获取内部存储的动作列表
    private final CopyOnWriteArrayList<Action> actions;

    public Paths() {
        actions = new CopyOnWriteArrayList<>();
    }

    // 添加一个动作
    public void addAction(Action action) {
        actions.add(action);
    }

    // 返回动作数量
    public int size() {
        return actions.size();
    }

    // 根据路径压缩生成 ActionList：连续同一方向的移动合并记录步数
    public static Paths compressToPaths(List<Node> path) {
        Paths actionList = new Paths();
        if (path == null || path.size() < 2) {
            return actionList;
        }
        Node prev = path.get(0);
        Direction currentDirection = null;
        int count = 0;
        for (int i = 1; i < path.size(); i++) {
            Node curr = path.get(i);
            Direction d = getDirection(prev, curr);
            if (d == null) {
                prev = curr;
                continue;
            }
            if (currentDirection == null) {
                currentDirection = d;
                count = 1;
            } else if (d == currentDirection) {
                count++;
            } else {
                actionList.addAction(new Action(currentDirection, count));
                currentDirection = d;
                count = 1;
            }
            prev = curr;
        }
        if (currentDirection != null) {
            actionList.addAction(new Action(currentDirection, count));
        }
        return actionList;
    }

    // 辅助方法：根据相邻两个节点的位移，从 Direction 枚举中查找对应方向
    private static Direction getDirection(Node from, Node to) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;
        return Direction.getDirection(dx, dy);
    }

    /**
     * 将当前 ActionList 还原为完整的节点列表，
     * 需要传入路径的起点 start。还原过程中，
     * 将依次根据每个动作的方向和步数计算后续节点的位置。
     */
    public List<Node> toNodeList(Node start) {
        List<Node> nodeList = new ArrayList<>();
        // 添加起点
        nodeList.add(new Node(start.x, start.y));
        int currentX = start.x;
        int currentY = start.y;
        for (Action action : actions) {
            for (int i = 0; i < action.steps; i++) {
                currentX += action.direction.getDx();
                currentY += action.direction.getDy();
                nodeList.add(new Node(currentX, currentY));
            }
        }
        return nodeList;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Action action : actions) {
            sb.append(action.toString()).append("\n");
        }
        return sb.toString();
    }
}
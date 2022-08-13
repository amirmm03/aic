package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.protobuf.AIProto;

import java.util.ArrayList;

public class GraphController {
    private ArrayList<AIProto.Path>[] adjacent;
    private AIProto.Graph graph;
    private int[][] distances;
    private int[][] nextNodeToMove;

    public GraphController(AIProto.Graph graph) {
        this.graph = graph;
        fillAdjacent();
        floydWarshall();
    }

    private void fillAdjacent() {
        int nodesCount = graph.getNodesCount();
        adjacent = new ArrayList[nodesCount + 1];
        for (int i = 1; i <= nodesCount; i++)
            adjacent[i] = new ArrayList<>();
        for (AIProto.Path path : graph.getPathsList()) {
            adjacent[path.getFirstNodeId()].add(path);
            adjacent[path.getSecondNodeId()].add(path);
        }
    }

    public void floydWarshall() {
        int nodesCount = graph.getNodesCount();
        distances = new int[nodesCount + 1][nodesCount + 1];
        nextNodeToMove = new int[nodesCount + 1][nodesCount + 1];
        for (int i = 1; i <= nodesCount; i++) {
            for (int j = 1; j <= nodesCount; j++) {
                distances[i][j] = 100000;
            }
        }

        for (int i = 1; i <= nodesCount; i++) {
            distances[i][i] = 0;
            nextNodeToMove[i][i] = i;

        }
        for (AIProto.Path path : graph.getPathsList()) {
            int v = path.getFirstNodeId();
            int u = path.getSecondNodeId();
            //TODO change constant
            distances[v][u] = distances[u][v] = 1 + (int) (path.getPrice()/25);
            nextNodeToMove[v][u] = u;
            nextNodeToMove[u][v] = v;
        }
        for (int k = 1; k <= nodesCount; k++)
            for (int i = 1; i <= nodesCount; i++)
                for (int j = 1; j <= nodesCount; j++)
                    if (distances[i][j] > distances[i][k] + distances[k][j]) {
                        distances[i][j] = distances[i][k] + distances[k][j];
                        nextNodeToMove[i][j] = nextNodeToMove[i][k];
                    }

    }

    public int getNextOnPath(int from, int to) {
        return nextNodeToMove[from][to];
    }

    public int getDistance(int from, int to) {
        return distances[from][to];
    }
}

package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.protobuf.AIProto.Graph;
import ir.sharif.aic.hideandseek.protobuf.AIProto.Path;

import java.util.ArrayList;

public class PoliceGraphController {

    private ArrayList<Path>[] adjacent;
    private Graph graph;
    private int[][] distances;


    public PoliceGraphController(Graph graph) {
        this.graph = graph;
        fillAdjacent();
        floydWarshall();
    }

    private void fillAdjacent() {
        int nodesCount = graph.getNodesCount();
        adjacent = new ArrayList[nodesCount + 1];
        for (int i = 1; i <= nodesCount; i++)
            adjacent[i] = new ArrayList<>();
        for (Path path : graph.getPathsList()) {
            adjacent[path.getFirstNodeId()].add(path);
            adjacent[path.getSecondNodeId()].add(path);
        }
    }

    public void floydWarshall() {
        int nodesCount = graph.getNodesCount();
        distances = new int[nodesCount + 1][nodesCount + 1];
        for (int i = 1; i <= nodesCount; i++)
            for (int j = 1; j <= nodesCount; j++)
                distances[i][j] = nodesCount + 1;
        for (int i = 1; i <= nodesCount; i++)
            distances[i][i] = 0;
        for (Path path : graph.getPathsList()) {
            int v = path.getFirstNodeId();
            int u = path.getSecondNodeId();
            distances[v][u] = distances[u][v] = 1;
        }
        for (int k = 1; k <= nodesCount; k++)
            for (int i = 1; i <= nodesCount; i++)
                for (int j = 1; j <= nodesCount; j++)
                    distances[i][j] = Math.min(distances[i][j], distances[i][k] + distances[k][j]);

    }


}

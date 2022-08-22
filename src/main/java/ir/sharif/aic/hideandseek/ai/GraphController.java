package ir.sharif.aic.hideandseek.ai;


import ir.sharif.aic.hideandseek.protobuf.AIProto;

import java.util.ArrayList;
import java.util.Collections;

public class GraphController {
    protected ArrayList<AIProto.Path>[] adjacent;
    private final AIProto.Graph graph;
    ArrayList<Integer> costOfPaths = new ArrayList<>();
    private final Pair[][][] distances;
    private final int[][][] nextNodeToMove;
    private final int[][][] priceOfMovement;

    //                                               3  5  1  4  2  4 3 5 1 5 4 2 3 1 2
    //                                          {40,30,25,20,15,12,10,8,7,6,5,4,3,2,1,0};
    private final int[] weightedEdgePrices = {20, 6, 1, 12, 3, 0, 30, 8, 2, 15, 10, 4, 25, 7, 5, 40};

    private final int numberOfTotalCalculations;
    private int numberOfTotalCalculationsDone = 0;

    public GraphController(AIProto.Graph graph) {
        numberOfTotalCalculations = weightedEdgePrices.length;
        this.graph = graph;
        fillAdjacent();
        distances = new Pair[numberOfTotalCalculations][][];
        nextNodeToMove = new int[numberOfTotalCalculations][][];
        priceOfMovement = new int[numberOfTotalCalculations][][];
        floydWarshall(numberOfTotalCalculationsDone);

    }

    private void fillAdjacent() {
        int nodesCount = graph.getNodesCount();
        adjacent = new ArrayList[nodesCount + 1];
        for (int i = 1; i <= nodesCount; i++)
            adjacent[i] = new ArrayList<>();
        for (AIProto.Path path : graph.getPathsList()) {
            adjacent[path.getFirstNodeId()].add(path);
            adjacent[path.getSecondNodeId()].add(path);
            if (!costOfPaths.contains((int) path.getPrice()))
                costOfPaths.add((int) path.getPrice());
        }
        Collections.sort(costOfPaths);
    }

    public void floydWarshall(int dimension) {
        int nodesCount = graph.getNodesCount();
        Pair[][] dimensionDistances = new Pair[nodesCount + 1][nodesCount + 1];
        int[][] dimensionNextNodeToMove = new int[nodesCount + 1][nodesCount + 1];
        int[][] dimensionPriceOfMovement = new int[nodesCount + 1][nodesCount + 1];
        for (int i = 1; i <= nodesCount; i++) {
            for (int j = 1; j <= nodesCount; j++) {
                dimensionDistances[i][j] = new Pair();
                dimensionDistances[i][j].WeightedDistance = Integer.MAX_VALUE / 2;
            }
        }
        for (int i = 1; i <= nodesCount; i++) {
            dimensionDistances[i][i].WeightedDistance = 0;
            dimensionDistances[i][i].edges = 0;
            dimensionNextNodeToMove[i][i] = i;
            dimensionPriceOfMovement[i][i] = 0;
        }
        for (AIProto.Path path : graph.getPathsList()) {
            int v = path.getFirstNodeId();
            int u = path.getSecondNodeId();

            dimensionDistances[v][u].WeightedDistance = 1 + costOfPaths.indexOf((int) path.getPrice()) * weightedEdgePrices[dimension];
            dimensionDistances[u][v].WeightedDistance = dimensionDistances[v][u].WeightedDistance;

            dimensionDistances[u][v].edges = 1;
            dimensionDistances[v][u].edges = 1;

            dimensionNextNodeToMove[v][u] = u;
            dimensionNextNodeToMove[u][v] = v;

            dimensionPriceOfMovement[v][u] = (int) path.getPrice();
            dimensionPriceOfMovement[u][v] = (int) path.getPrice();

        }
        for (int k = 1; k <= nodesCount; k++)
            for (int i = 1; i <= nodesCount; i++)
                for (int j = 1; j <= nodesCount; j++)
                    if (dimensionDistances[i][j].WeightedDistance > dimensionDistances[i][k].WeightedDistance + dimensionDistances[k][j].WeightedDistance) {
                        dimensionDistances[i][j].WeightedDistance = dimensionDistances[i][k].WeightedDistance + dimensionDistances[k][j].WeightedDistance;
                        dimensionDistances[i][j].edges = dimensionDistances[i][k].edges + dimensionDistances[k][j].edges;
                        dimensionPriceOfMovement[i][j] = dimensionPriceOfMovement[i][k] + dimensionPriceOfMovement[k][j];
                        dimensionNextNodeToMove[i][j] = dimensionNextNodeToMove[i][k];
                    }


        distances[dimension] = dimensionDistances;
        priceOfMovement[dimension] = dimensionPriceOfMovement;
        nextNodeToMove[dimension] = dimensionNextNodeToMove;
        numberOfTotalCalculationsDone++;

    }

    public void updateInfo() {
        if (numberOfTotalCalculationsDone < numberOfTotalCalculations)
            floydWarshall(numberOfTotalCalculationsDone);
    }

    public int getNextOnPath(int from, int to, Double myMoney) {
        int bestDimension = -1;
        int bestDimensionDistance = Integer.MAX_VALUE;
        for (int dimension = 0; dimension < numberOfTotalCalculationsDone; dimension++) {
            if (bestDimensionDistance > distances[dimension][from][to].edges && myMoney > priceOfMovement[dimension][from][to]) {
                bestDimension = dimension;
                bestDimensionDistance = distances[dimension][from][to].edges;
            }
        }
        if (bestDimension == -1)
            return from;
        return nextNodeToMove[bestDimension][from][to];
    }

    public int getDistance(int from, int to, Double myMoney) {
        int bestDimensionDistance = Integer.MAX_VALUE;
        for (int dimension = 0; dimension < numberOfTotalCalculationsDone; dimension++) {
            if (bestDimensionDistance > distances[dimension][from][to].edges && myMoney >= priceOfMovement[dimension][from][to]) {
                bestDimensionDistance = distances[dimension][from][to].edges;
            }
        }
        return bestDimensionDistance;
    }

}

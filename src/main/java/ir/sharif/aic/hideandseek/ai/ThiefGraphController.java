package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.protobuf.AIProto;

import java.util.ArrayList;
import java.util.List;

public class ThiefGraphController extends GraphController{

    public ThiefGraphController(AIProto.Graph graph) {
        super(graph);
    }

    public double getScore(int nodeId, List<Integer> policeList) {
        int closest = 10000000;
        for (int police : policeList)
            closest = Math.min(closest, getDistance(nodeId, police,Double.MAX_VALUE));
        return closest;
    }

    public ArrayList<AIProto.Path> getAdjacent(int v) {
        return adjacent[v];
    }
}

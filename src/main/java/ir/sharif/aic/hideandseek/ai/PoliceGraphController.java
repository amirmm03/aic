package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.protobuf.AIProto;
import ir.sharif.aic.hideandseek.protobuf.AIProto.Graph;

import java.util.HashMap;
import java.util.Random;

public class PoliceGraphController extends GraphController {

    Random random;

    public PoliceGraphController(Graph graph, int id) {
        super(graph);
        random = new Random(id * 1000L + System.currentTimeMillis());
    }

    public AIProto.Agent findClosestThief(AIProto.GameView gameView, HashMap<AIProto.Agent, Boolean> thievesCaptured) {
        AIProto.Agent me = gameView.getViewer();
        AIProto.Agent closestThief = null;
        for (AIProto.Agent thief : thievesCaptured.keySet()) {
            if (closestThief == null)
                closestThief = thief;
            if (getDistance(me.getNodeId(), thief.getNodeId()) < getDistance(me.getNodeId(), closestThief.getNodeId()))
                closestThief = thief;
        }
        return closestThief;
    }


    public int randomMove(int myLocation) {
        int randomInt = random.nextInt(adjacent[myLocation].size());
        AIProto.Path path = adjacent[myLocation].get(randomInt);
        if (path.getFirstNodeId() == myLocation)
            return path.getSecondNodeId();
        else
            return path.getFirstNodeId();
    }
}

package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.protobuf.AIProto;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

import static java.lang.Math.min;

public class PoliceGraphController extends GraphController {

    private final Random random;

    private final ArrayList<Integer> distributedNodes = new ArrayList<>();
    private final ArrayList<Integer> policeIds = new ArrayList<>();
    FileWriter fileWriter;
    public PoliceGraphController(AIProto.GameView gameView) {
        super(gameView.getConfig().getGraph());
//        File file = new File(gameView.getViewer().getId() + ".txt");
//        try {
//            fileWriter = new FileWriter(file);
//        } catch (IOException e) {
//            System.exit(0);
//        }
        random = new Random(gameView.getViewer().getId() * 1793L + System.currentTimeMillis() * 7);

        fillDistributedNodes(gameView);

    }

    private void fillDistributedNodes(AIProto.GameView gameView) {
        fillPoliceIds(gameView);

        distributedNodes.add(1);
        for (int i = 0; i < policeIds.size(); i++) {
            addNextNodeToDistributedList();
        }
        distributedNodes.remove(0);
    }

    private void fillPoliceIds(AIProto.GameView gameView) {
        policeIds.add(gameView.getViewer().getId());
        for (AIProto.Agent agent : gameView.getVisibleAgentsList()) {
            if (agent.getTeamValue() == gameView.getViewer().getTeamValue() && agent.getType() == AIProto.AgentType.POLICE) {
                policeIds.add(agent.getId());
            }
        }
        Collections.sort(policeIds);
    }

    private void addNextNodeToDistributedList() {
        int bestNode = 1;
        int bestNodeMinimumDistance = 0;
        for (int i = 1; i < distances.length; i++) {
            if (distributedNodes.contains(i))
                continue;
            int minimumDistance = 100000;
            for (int j = 0; j < distributedNodes.size(); j++) {
                minimumDistance = min(minimumDistance, distances[i][distributedNodes.get(j)]);
            }
            if (minimumDistance > bestNodeMinimumDistance) {
                bestNodeMinimumDistance = minimumDistance;
                bestNode = i;
            }
        }
        distributedNodes.add(bestNode);
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

    public int distributedMove(AIProto.GameView gameView) {

        AIProto.Agent me = gameView.getViewer();

        ArrayList<Integer> copy = (ArrayList<Integer>) distributedNodes.clone();

        ArrayList<AIProto.Agent> agents = new ArrayList<>(gameView.getVisibleAgentsList());
        agents.add(me);

        for (Integer policeId : policeIds) {
            for (AIProto.Agent agent : agents) {
                if (policeId == agent.getId()) {
                    int closestNode = findClosestNode(agent.getNodeId(), copy);
                    copy.remove((Object) closestNode);
                    if (agent.getId() == me.getId()) {
                        return getNextOnPath(me.getNodeId(), closestNode);
                    }
                }
            }
        }
        return randomMove(me.getNodeId());
    }

    private int findClosestNode(int nodeId, ArrayList<Integer> nodes) {
        int bestNode = nodes.get(0);
        for (Integer distributedNode : nodes) {
            if (distances[distributedNode][nodeId] < distances[bestNode][nodeId])
                bestNode = distributedNode;
        }
        return bestNode;
    }
}

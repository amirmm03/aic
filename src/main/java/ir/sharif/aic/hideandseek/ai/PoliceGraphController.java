package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.client.Phone;
import ir.sharif.aic.hideandseek.protobuf.AIProto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static java.lang.Math.min;

public class PoliceGraphController extends GraphController {

    private final Random random;

    private final ArrayList<Integer> distributedNodes = new ArrayList<>();
    private final ArrayList<Integer> policeIds = new ArrayList<>();

    public PoliceGraphController(AIProto.GameView gameView) {
        super(gameView.getConfig().getGraph());
        random = new Random(gameView.getViewer().getId() * 1793L + System.currentTimeMillis() * 7);
        fillDistributedNodes(gameView.getVisibleAgentsList());
    }

    private void fillDistributedNodes(List<AIProto.Agent> visibleAgentsList) {
        for (AIProto.Agent agent : visibleAgentsList) {
            if (agent.getTeamValue() == AIProto.Team.FIRST_VALUE && agent.getType() == AIProto.AgentType.POLICE) {
                policeIds.add(agent.getId());
            }
        }
        distributedNodes.add(1);

        for (int i = 0; i < policeIds.size(); i++) {
            addNextNodeToDistributedList();
        }

        distributedNodes.remove(0);
    }

    private void addNextNodeToDistributedList() {
        int bestNode = 1;
        int bestNodeMinimumDistance = 0;
        for (int i = 1; i < distances.length; i++) {
            if (distributedNodes.contains(i))
                continue;
            int minimumDistance = 100000;
            for (int j = 0; j < distributedNodes.size(); j++) {
                minimumDistance = min(minimumDistance, distances[i][j]);
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

    public int distributedMove(AIProto.GameView gameView, Phone phone) {
        AIProto.Agent me = gameView.getViewer();
        ArrayList<Integer> copy = (ArrayList<Integer>) distributedNodes.clone();

        for (AIProto.Agent agent : gameView.getVisibleAgentsList()) {
            if (agent.getTeamValue() == me.getTeamValue() && agent.getType() == AIProto.AgentType.POLICE) {

                int closestNode = findClosestNode(agent.getNodeId(), copy);
                copy.remove((Object) closestNode);
                if (agent.getId() == me.getId()) {
                    return getNextOnPath(me.getNodeId(), closestNode);
                }
            }
        }
        // TODO: 8/15/2022 bug here 
        for (AIProto.Agent agent : gameView.getVisibleAgentsList()) {
            if (agent.getId() == me.getId())
                phone.sendMessage("11111");
        }
        phone.sendMessage("00000");
//        while (true) {
//            System.out.println("\n");
//            if (false)
//                break;
//        }
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

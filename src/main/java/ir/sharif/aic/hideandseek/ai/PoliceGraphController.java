package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.protobuf.AIProto;

import java.io.FileWriter;
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
            addNextNodeToDistributedList(gameView.getConfig().getGraph().getNodesCount());
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

    private void addNextNodeToDistributedList(int nodesCount) {
        int bestNode = 1;
        int bestNodeMinimumDistance = 0;
        for (int i = 1; i <= nodesCount; i++) {
            if (distributedNodes.contains(i))
                continue;
            int minimumDistance = 100000;
            for (int j = 0; j < distributedNodes.size(); j++) {
                minimumDistance = min(minimumDistance, getDistance(i, distributedNodes.get(j), Double.MAX_VALUE));
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
            if (getDistance(me.getNodeId(), thief.getNodeId(), gameView.getBalance()) < getDistance(me.getNodeId(), closestThief.getNodeId(), gameView.getBalance()))
                closestThief = thief;
        }
        return closestThief;
    }


    public int bestNearNode(int myLocation, double balance, int depth) {
        if (depth == 0)
            return myLocation;

        int bestNode = myLocation;
        for (AIProto.Path path : adjacent[myLocation]) {
            int otherNode = path.getFirstNodeId() ^ path.getSecondNodeId() ^ myLocation;
            if (path.getPrice() < balance) {
                otherNode = bestNearNode(otherNode, balance - path.getPrice(), depth - 1);
                if (adjacent[otherNode].size() > adjacent[bestNode].size())
                    bestNode = otherNode;
            }
        }
        return bestNode;
    }

    public int distributedMove(AIProto.GameView gameView) {

        AIProto.Agent me = gameView.getViewer();

        ArrayList<Integer> copy = (ArrayList<Integer>) distributedNodes.clone();

        ArrayList<AIProto.Agent> agents = new ArrayList<>(gameView.getVisibleAgentsList());
        agents.add(me);

        for (Integer policeId : policeIds) {
            for (AIProto.Agent agent : agents) {
                if (policeId == agent.getId()) {
                    int closestNode = findClosestNode(agent.getNodeId(), copy, gameView.getBalance());
                    copy.remove((Object) closestNode);
                    if (agent.getId() == me.getId()) {
                        return getNextOnPath(me.getNodeId(), closestNode, gameView.getBalance());
                    }
                }
            }
        }
        return bestNearNode(me.getNodeId(), gameView.getBalance(), 1);
    }

    private int findClosestNode(int nodeId, ArrayList<Integer> nodes, double balance) {
        int bestNode = nodes.get(0);
        for (Integer distributedNode : nodes) {
            if (getDistance(distributedNode, nodeId, balance) < getDistance(bestNode, nodeId, balance))
                bestNode = distributedNode;
        }
        return bestNode;
    }

    public int randomMoveNearThief(int myNode, int thiefNode, double balance, int depth) {
        if (getDistance(myNode, thiefNode, balance) >= 1) {
            return getNextOnPath(myNode, thiefNode, balance);
        }
        return getNextOnPath(myNode, bestNearNode(myNode, balance, depth), balance);
    }

    public int randomMove(int myLocation, double balance) {
        if (balance <= 1)
            return myLocation;
        int randomInt = random.nextInt(adjacent[myLocation].size());
        AIProto.Path path = adjacent[myLocation].get(randomInt);
        if (path.getPrice() > balance) {
            return randomMove(myLocation, balance / 2);
        }
        if (path.getFirstNodeId() == myLocation)
            return path.getSecondNodeId();
        else
            return path.getFirstNodeId();
    }

    public int evaluate(ArrayList<Integer> policeNodes, ArrayList<Integer> thiefNodes) {
        return 0;
    }


    private int temp_score = 0;

    public int getNextNodeWithMinimax(int policeID, int depth,
                                      LinkedHashMap<Integer, Integer> allies_ID_NODE,
                                      ArrayList<Integer> thieves_NODE) {

        int policeNodeId = allies_ID_NODE.get(policeID);

        HashMap<Integer, Integer> nextScores = new HashMap<>();

        for (AIProto.Path path : adjacent[policeNodeId]) {
            int nextNode = policeNodeId ^ path.getFirstNodeId() ^ path.getSecondNodeId();


            this.temp_score = 0;

            LinkedHashMap<Integer, Integer> seenAllies_ID_NODE_toSend = new LinkedHashMap<>();
            seenAllies_ID_NODE_toSend.put(policeNodeId, nextNode);

            placeOthers(policeID, depth, seenAllies_ID_NODE_toSend, allies_ID_NODE, thieves_NODE);

            nextScores.put(nextNode, temp_score);
        }

        int max = 0;
        int max_nodeId = 0;

        for (Integer nodeId : nextScores.keySet()) {
            if (nextScores.get(nodeId) > max) {
                max = nextScores.get(nodeId);
                max_nodeId = nodeId;
            }
        }

        return max_nodeId;
    }

    private void placeOthers(int policeId, int depth,
                             LinkedHashMap<Integer, Integer> seenAllies_ID_NODE,
                             LinkedHashMap<Integer, Integer> allies_ID_NODE,
                             ArrayList<Integer> thieves_NODE) {

        if (seenAllies_ID_NODE.keySet().size() == allies_ID_NODE.keySet().size()) {
            if (depth < 3) {
                placeOthers(policeId, depth + 1, new LinkedHashMap<>(), seenAllies_ID_NODE, thieves_NODE);
            } else {
                ArrayList<Integer> policePlaces = new ArrayList<>();
                for (Integer allyId : seenAllies_ID_NODE.keySet()) {
                    policePlaces.add(seenAllies_ID_NODE.get(allyId));
                }

                int score = evaluate(policePlaces, thieves_NODE);

                if (score > temp_score) {
                    temp_score = score;
                }
            }
        }

        for (Integer allyId : allies_ID_NODE.keySet()) {
            if (!seenAllies_ID_NODE.keySet().contains(allyId)) {
                int nodeId = allies_ID_NODE.get(allyId);
                for (AIProto.Path path : adjacent[nodeId]) {
                    int nextNode = nodeId ^ path.getSecondNodeId() ^ path.getFirstNodeId();
                    seenAllies_ID_NODE.put(allyId, nextNode);

                    placeOthers(policeId, depth, seenAllies_ID_NODE, allies_ID_NODE, thieves_NODE);

                    seenAllies_ID_NODE.remove(allyId, nextNode);
                }
            }
        }

    }
}

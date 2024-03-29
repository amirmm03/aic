package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.protobuf.AIProto;

import java.io.FileWriter;
import java.util.*;

import static java.lang.Math.min;

public class PoliceGraphController extends GraphController {

    private final Random random;
    public int numberOfOps;
    public int bestEvalScore;

    private final LinkedHashSet<Integer> distributedNodes = new LinkedHashSet<>();
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

    }

    private void fillDistributedNodes(AIProto.GameView gameView) {
        fillPoliceIds(gameView);
        int delete = distributedNodes.size();
        for (int i = 0; i < policeIds.size(); i++) {
            addNextNodeToDistributedList(gameView.getConfig().getGraph().getNodesCount());
        }
        for (int i = 0; i < delete; i++) {
            distributedNodes.remove(0);
        }

    }

    private void fillPoliceIds(AIProto.GameView gameView) {
        policeIds.clear();
        distributedNodes.clear();
        policeIds.add(gameView.getViewer().getId());
        for (AIProto.Agent agent : gameView.getVisibleAgentsList()) {
            if (agent.getTeamValue() == gameView.getViewer().getTeamValue() && (agent.getType() == AIProto.AgentType.POLICE || agent.getType() == AIProto.AgentType.BATMAN)) {
                policeIds.add(agent.getId());
                distributedNodes.add(agent.getNodeId());
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
            Integer[] distributedNodesArray = new Integer[distributedNodes.size()];
            distributedNodesArray = distributedNodes.toArray(distributedNodesArray);

            for (int j = 0; j < distributedNodes.size(); j++) {
                minimumDistance = min(minimumDistance, getDistance(i, distributedNodesArray[j], Double.MAX_VALUE));
            }
            if (minimumDistance > bestNodeMinimumDistance) {
                bestNodeMinimumDistance = minimumDistance;
                bestNode = i;
            }
        }
        distributedNodes.add(bestNode);
    }

    public MyThief findClosestThief(AIProto.GameView gameView, HashMap<MyThief, Boolean> thievesCaptured) {
        AIProto.Agent me = gameView.getViewer();
        MyThief closestThief = null;
        for (MyThief thief : thievesCaptured.keySet()) {
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

    private boolean distributeNodesUpdate = true;

    public int distributedMove(AIProto.GameView gameView) {

        if (gameView.getTurn().getTurnNumber() % 7 == 5)
            distributeNodesUpdate = true;

        if (distributeNodesUpdate) {
            fillDistributedNodes(gameView);
            distributeNodesUpdate = false;
        }

        AIProto.Agent me = gameView.getViewer();

        ArrayList<Integer> copy = new ArrayList<>(distributedNodes);

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

        int score = 0;
//        ArrayList<Integer> destinations = new ArrayList<>();
//        for (Integer policeNode : policeNodes) {
//            destinations.add(findClosestNode(policeNode, thiefNodes, Double.MAX_VALUE));
//        }
//
//        for (Integer policeNode : policeNodes) {
//            for (Integer thiefNode : thiefNodes) {
//                if (policeNode == thiefNode)
//                    score += 10000;
//            }
//        }
//
//

        numberOfOps++;
        return score;
    }


//    public int distanceFromFirstIntersection(int a, int b, int dest) {
//        if (a == b)
//            return getDistance(a, dest, Double.MAX_VALUE);
//        int aDist = getDistance(a, dest, Double.MAX_VALUE);
//        int bDist = getDistance(b, dest, Double.MAX_VALUE);
//        if (aDist == bDist)
//            return distanceFromFirstIntersection(getNextOnPath(a, dest, Double.MAX_VALUE), getNextOnPath(b, dest, Double.MAX_VALUE), dest);
//        if (aDist > bDist)
//            return distanceFromFirstIntersection(getNextOnPath(a, dest, Double.MAX_VALUE), b, dest);
//        return distanceFromFirstIntersection(a, getNextOnPath(b, dest, Double.MAX_VALUE), dest);
//    }


    public int getNextNodeWithMinimax(int policeID, int depth,
                                      LinkedHashMap<Integer, Integer> allies_ID_NODE,
                                      ArrayList<Integer> thieves_NODE) {
        ArrayList<Integer> policeNodes = new ArrayList<>(allies_ID_NODE.values());

        bestEvalScore = -1;
        for (Integer id : allies_ID_NODE.keySet()) {
            for (AIProto.Path path : adjacent[allies_ID_NODE.get(id)]) {
                int oldNode = allies_ID_NODE.get(id);
                int newNode = path.getFirstNodeId() ^ path.getSecondNodeId() ^ allies_ID_NODE.get(id);
                allies_ID_NODE.put(id, newNode);
                int tmp = minimax(policeID, depth - 1, allies_ID_NODE, thieves_NODE);
                if (tmp > bestEvalScore) {
                    bestEvalScore = tmp;

                }
                allies_ID_NODE.put(id, oldNode);

            }
        }
        return 0;

    }

    public int minimax(int policeID, int depth,
                       LinkedHashMap<Integer, Integer> allies_ID_NODE,
                       ArrayList<Integer> thieves_NODE) {
        if (depth == 0) {
            ArrayList<Integer> policeNodes = new ArrayList<>(allies_ID_NODE.values());
            return evaluate(policeNodes, thieves_NODE);
        }


        int best = -1;
        for (Integer id : allies_ID_NODE.keySet()) {
            for (AIProto.Path path : adjacent[allies_ID_NODE.get(id)]) {
                int oldNode = allies_ID_NODE.get(id);
                int newNode = path.getFirstNodeId() ^ path.getSecondNodeId() ^ allies_ID_NODE.get(id);
                allies_ID_NODE.put(id, newNode);
                best = Math.max(best, minimax(policeID, depth - 1, allies_ID_NODE, thieves_NODE));
                allies_ID_NODE.put(id, oldNode);
            }
        }
        return best;

    }

    public int getNextOnPathWithoutIntersection(int myNode, int thiefNode, double balance, ArrayList<AIProto.Agent> otherPolices, ArrayList<MyThief> thieves) {
        ArrayList<Integer> destinations = new ArrayList<>();
        ArrayList<Integer> thiefNodes = new ArrayList<>();

        for (MyThief thief : thieves) {
            thiefNodes.add(thief.getNodeId());
        }

        for (AIProto.Agent policeNode : otherPolices) {
            if ((findClosestNode(policeNode.getNodeId(), thiefNodes, balance)) == thiefNode) {
                if (getDistance(myNode, thiefNode, balance) > getDistance(policeNode.getNodeId(), thiefNode, balance)) {
                    return newPath(myNode, thiefNode, balance);
                }
            }
        }
        return getNextOnPath(myNode, thiefNode, balance);

    }

    private int newPath(int myNode, int thiefNode, double balance) {

        for (AIProto.Path path : adjacent[thiefNode]) {
            int adjNode = path.getFirstNodeId() ^ path.getSecondNodeId() ^ thiefNode;
            if (adjNode != getNodeBeforeDestination(myNode, thiefNode, balance) && getNodeBeforeDestination(myNode, adjNode, balance) != thiefNode) {
                return getNextOnPath(myNode, adjNode, balance);
            }
        }


        for (AIProto.Path path1 : adjacent[thiefNode]) {
            int adjNode1 = path1.getFirstNodeId() ^ path1.getSecondNodeId() ^ thiefNode;
            if (adjNode1 != getNodeBeforeDestination(myNode, thiefNode, balance)) {
                for (AIProto.Path path2 : adjacent[adjNode1]) {
                    int adjNode2 = path2.getFirstNodeId() ^ path2.getSecondNodeId() ^ adjNode1;
                    if (adjNode2 != getNodeBeforeDestination(myNode, adjNode1, balance) && getNodeBeforeDestination(myNode, adjNode2, balance) != adjNode1) {
                        return getNextOnPath(myNode, adjNode2, balance);
                    }

                }
            }
        }

        return getNextOnPath(myNode, thiefNode, balance);
    }

    private int getNodeBeforeDestination(int from, int dest, double balance) {
        return getNextOnPath(dest, from, balance);
    }
}

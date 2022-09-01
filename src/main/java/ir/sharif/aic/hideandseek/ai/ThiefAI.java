package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.client.Phone;
import ir.sharif.aic.hideandseek.protobuf.AIProto;
import ir.sharif.aic.hideandseek.protobuf.AIProto.*;
import ir.sharif.aic.hideandseek.protobuf.AIProto.GameView;

import java.lang.reflect.Array;
import java.util.*;

public class ThiefAI extends AI {
    private ThiefGraphController graphController;
    private ArrayList<Integer> thievesVisibleLocations = new ArrayList<>();

    public ThiefAI(Phone phone) {
        this.phone = phone;
    }

    /**
     * Used to select the starting node.
     */
    @Override
    public int getStartingNode(GameView gameView) {
        graphController = new ThiefGraphController(gameView.getConfig().getGraph());

        int target = 2;
        double bestScore = 0;


        for (Node node : gameView.getConfig().getGraph().getNodesList()) {
            double tempScore = (double) graphController.getDistance(node.getId(), 1, Double.MAX_VALUE);
            if (tempScore > bestScore) {
                bestScore = tempScore;
                target = node.getId();
            }
        }

        if (gameView.getViewer().getType() == AgentType.JOKER) return target;

        ArrayList<Integer> goodNodes = new ArrayList<>();
        for (Node node : gameView.getConfig().getGraph().getNodesList()) {
            if (graphController.getDistance(node.getId(), 1, Double.MAX_VALUE) >= (bestScore * 2) / 3) {
                goodNodes.add(node.getId());
            }
        }

//        Random random = new Random(System.currentTimeMillis());
//
//        target = goodNodes.get((random.nextInt(goodNodes.size()) + gameView.getViewer().getId())%goodNodes.size());

        ArrayList<Integer> myThieves = new ArrayList<>();
        ArrayList<Integer> alreadyChosenNodes = new ArrayList<>();
        // alreadyChosenNodes.add(1);
        Agent me = gameView.getViewer();
        for (Agent agent : gameView.getVisibleAgentsList()) {

            if (agent.getTeamValue() == me.getTeamValue() &&
                    (agent.getType() == AgentType.THIEF || agent.getType() == AgentType.JOKER)) {
                myThieves.add(agent.getId());
            }
        }


        // System.out.println("my id is  "+me.getId() + " agent is "+agent.getType().name());


        myThieves.add(me.getId());

        Collections.sort(myThieves);
        ArrayList<Integer> chosenNodes = graphController.getBestCombinationOfNodes(goodNodes, myThieves.size(), alreadyChosenNodes);


        target = chosenNodes.get(myThieves.indexOf(me.getId()));
        return target;
    }

    /**
     * Implement this function to move your thief agent based on current game view.
     */
    @Override
    public int move(GameView gameView) {
        graphController.updateInfo();
        updateVisibleThief(gameView);
        Agent me = gameView.getViewer();
        List<Integer> policeList = new ArrayList<>();
        List<Agent> thieveList = new ArrayList<>();
        for (Agent agent : gameView.getVisibleAgentsList()) {
            if (agent.getTeamValue() != me.getTeamValue() && agent.getType() == AgentType.POLICE)
                policeList.add(agent.getNodeId());
            if (agent.getTeamValue() == me.getTeamValue() && (agent.getType() == AIProto.AgentType.THIEF || agent.getType() == AIProto.AgentType.JOKER) && !agent.getIsDead())
                thieveList.add(agent);
        }

        //just updating till now


        if (haveThiefHereWithHigherId(me, thieveList)) {
            return me.getNodeId();
        }
        int bestNode = findBestNodeNear(me, gameView.getBalance(), policeList, thieveList);
        return graphController.getNextOnPath(me.getNodeId(), bestNode, gameView.getBalance());

    }

    private int findBestNodeNear(Agent me, double myMoney, List<Integer> policeList, List<Agent> thieveList) {
        int bestNode = me.getNodeId();

        for (Path path : graphController.getAdjacent(me.getNodeId())) {
            int adjacent = me.getNodeId() ^ path.getFirstNodeId() ^ path.getSecondNodeId();
            if (graphController.getScore(bestNode, policeList, thieveList, thievesVisibleLocations) <= graphController.getScore(adjacent, policeList, thieveList, thievesVisibleLocations)
                    && myMoney > path.getPrice())
                bestNode = adjacent;
            if (graphController.getScore(bestNode, policeList, thieveList, thievesVisibleLocations) > 2) {
                for (Path path1 : graphController.getAdjacent(adjacent)) {
                    int adjacentAdjacent = adjacent ^ path1.getSecondNodeId() ^ path1.getFirstNodeId();
                    if (graphController.getScore(bestNode, policeList, thieveList, thievesVisibleLocations) < graphController.getScore(adjacentAdjacent, policeList, thieveList, thievesVisibleLocations)
                            && (myMoney > path.getPrice() + path1.getPrice()))
                        bestNode = adjacentAdjacent;
                }
            }
        }

        return bestNode;
    }

    private void updateVisibleThief(GameView gameView) {
        Agent me = gameView.getViewer();
        if (gameView.getConfig().getTurnSettings().getVisibleTurnsList().contains(gameView.getTurn().getTurnNumber() - 1)) {
            thievesVisibleLocations = new ArrayList<>();
            for (Agent agent : gameView.getVisibleAgentsList()) {
                if (agent.getTeamValue() == me.getTeamValue() && (agent.getType() == AIProto.AgentType.THIEF || agent.getType() == AIProto.AgentType.JOKER) && !agent.getIsDead()) {
                    thievesVisibleLocations.add(agent.getNodeId());
                }
            }
            thievesVisibleLocations.add(me.getNodeId());
        }
    }

    private boolean haveThiefHereWithHigherId(Agent me, List<Agent> otherThieves) {
        for (Agent thief : otherThieves) {
            if (thief.getNodeId() == me.getNodeId() && thief.getId() > me.getId())
                return true;
        }
        return false;
    }

}

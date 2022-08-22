package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.client.Phone;
import ir.sharif.aic.hideandseek.protobuf.AIProto;
import ir.sharif.aic.hideandseek.protobuf.AIProto.*;
import ir.sharif.aic.hideandseek.protobuf.AIProto.GameView;

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

        HashMap<Node, Integer> scores = new HashMap<>();

        for (Node node : gameView.getConfig().getGraph().getNodesList()) {
            double tempScore = (double) graphController.getDistance(node.getId(), 1, Double.MAX_VALUE);
            if (tempScore > bestScore) {
                bestScore = tempScore;
                target = node.getId();
            }
        }

        ArrayList<Integer> goodNodes = new ArrayList<>();
        for (Node node: gameView.getConfig().getGraph().getNodesList()) {
            if (graphController.getDistance(node.getId(), 1, Double.MAX_VALUE) >= (bestScore * 2)/3) {
                goodNodes.add(node.getId());
            }
        }

//        Random random = new Random(System.currentTimeMillis());
//
//        target = goodNodes.get((random.nextInt(goodNodes.size()) + gameView.getViewer().getId())%goodNodes.size()).getId();

        ArrayList<Integer> myThieves = new ArrayList<>();
        ArrayList<Integer> alreadyChosenNodes = new ArrayList<>();
       // alreadyChosenNodes.add(1);
        Agent me = gameView.getViewer();
        for (Agent agent : gameView.getVisibleAgentsList()) {
            if (agent.getTeamValue() == me.getTeamValue() && agent.getType() == AgentType.THIEF) {
                myThieves.add(agent.getId());
            }
        }
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
        ArrayList<Path> adjacentPath = graphController.getAdjacent(me.getNodeId());
        List<Integer> policeList = new ArrayList<>();
        List<Agent> thieveList = new ArrayList<>();
        for (Agent agent : gameView.getVisibleAgentsList()) {
            if (agent.getTeamValue() != me.getTeamValue() && agent.getType() == AgentType.POLICE)
                policeList.add(agent.getNodeId());
            if (agent.getTeamValue() == me.getTeamValue() && agent.getType() == AgentType.THIEF)
                thieveList.add(agent);
        }
        int next = me.getNodeId();
        if (haveThiefHereWithHigherId(me,thieveList)){
            return next;
        }
        for (Path path : adjacentPath) {
            int adjacent = me.getNodeId() ^ path.getFirstNodeId() ^ path.getSecondNodeId();
            if (graphController.getScore(next, policeList, thieveList, thievesVisibleLocations) <= graphController.getScore(adjacent, policeList, thieveList, thievesVisibleLocations))
                next = adjacent;
        }
        return next;
    }

    private void updateVisibleThief(GameView gameView) {
        Agent me = gameView.getViewer();
        if (gameView.getConfig().getTurnSettings().getVisibleTurnsList().contains(gameView.getTurn().getTurnNumber())) {
            thievesVisibleLocations = new ArrayList<>();
            for (Agent agent : gameView.getVisibleAgentsList()) {
                if (agent.getTeamValue() == me.getTeamValue() && agent.getType() == AIProto.AgentType.THIEF && !agent.getIsDead()) {
                    thievesVisibleLocations.add(agent.getNodeId());
                }
            }
            thievesVisibleLocations.add(me.getNodeId());
        }
    }

    private boolean haveThiefHereWithHigherId(Agent me , List<Agent> otherThieves) {
        for (Agent thief : otherThieves) {
            if (thief.getNodeId() == me.getNodeId() && thief.getId() > me.getId())
                return true;
        }
        return false;
    }

}

package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.client.Phone;
import ir.sharif.aic.hideandseek.protobuf.AIProto;
import ir.sharif.aic.hideandseek.protobuf.AIProto.*;
import ir.sharif.aic.hideandseek.protobuf.AIProto.GameView;

import java.util.*;

public class ThiefAI extends AI {
    private ThiefGraphController graphController;
    private ArrayList<Integer> thievesVisibleLocations = new ArrayList<>();
    private int myLastKnownLoc = 1;

    private int lastChatIndex = 0;

    public ThiefAI(Phone phone) {
        this.phone = phone;
    }

    /**
     * Used to select the starting node.
     */
    @Override
    public int getStartingNode(GameView gameView) {
        graphController = new ThiefGraphController(gameView.getConfig().getGraph());

        graphController.graphCenter = graphController.findGraphCenter(gameView);

        System.out.println("==================" + graphController.graphCenter + "==============");

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
        Agent me = gameView.getViewer();

        List<myPolice> policeList = new ArrayList<>();
        List<Agent> thieveList = new ArrayList<>();
        for (Agent agent : gameView.getVisibleAgentsList()) {
            if (agent.getTeamValue() != me.getTeamValue() && (agent.getType() == AgentType.POLICE || agent.getType() == AgentType.BATMAN))
                policeList.add(new myPolice(agent.getId(),agent.getNodeId()));
            if (agent.getTeamValue() == me.getTeamValue() && (agent.getType() == AIProto.AgentType.THIEF || agent.getType() == AIProto.AgentType.JOKER) && !agent.getIsDead())
                thieveList.add(agent);
            if (agent.getTeamValue() != me.getTeamValue() && agent.getType() == AgentType.BATMAN) {
                phone.sendMessage(getBatmanChatMessage(agent.getNodeId()));
            }
        }

        for (int i = lastChatIndex; i < gameView.getChatBoxCount(); i++) {
            myPolice batman = getMyPoliceBatmanFromChat(gameView.getChatBox(i).getText());
            boolean flag = false;
            for (myPolice police : policeList) {
                if (police.node == batman.node) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                policeList.add(batman);
            }
        }

        lastChatIndex = gameView.getChatBoxCount();


        updateVisibleThief(gameView, policeList);

        //just updating till now


        if (haveThiefHereWithHigherId(me, thieveList)) {
            return me.getNodeId();
        }
        return graphController.bestNodeWithMinimax(me.getNodeId(),policeList,thieveList,thievesVisibleLocations,3,myLastKnownLoc,gameView.getTurn().getTurnNumber(),gameView.getConfig().getTurnSettings().getVisibleTurnsList(),me.getId(),gameView.getBalance(),gameView.getConfig().getIncomeSettings().getThievesIncomeEachTurn());
//        int bestNode = findBestNodeNear(me, gameView.getBalance(), policeList, thieveList);
//        return graphController.getNextOnPath(me.getNodeId(), bestNode, gameView.getBalance());

    }

//    private int findBestNodeNear(Agent me, double myMoney, List<myPolice> policeList, List<Agent> thieveList) {
//        int bestNode = me.getNodeId();
//
//        for (Path path : graphController.getAdjacent(me.getNodeId())) {
//            int adjacent = me.getNodeId() ^ path.getFirstNodeId() ^ path.getSecondNodeId();
//            if (graphController.getScore(bestNode, policeList, thieveList, thievesVisibleLocations) <= graphController.getScore(adjacent, policeList, thieveList, thievesVisibleLocations)
//                    && myMoney > path.getPrice())
//                bestNode = adjacent;
//            if (graphController.getScore(bestNode, policeList, thieveList, thievesVisibleLocations) > 2) {
//                for (Path path1 : graphController.getAdjacent(adjacent)) {
//                    int adjacentAdjacent = adjacent ^ path1.getSecondNodeId() ^ path1.getFirstNodeId();
//                    if (graphController.getScore(bestNode, policeList, thieveList, thievesVisibleLocations) < graphController.getScore(adjacentAdjacent, policeList, thieveList, thievesVisibleLocations)
//                            && (myMoney > path.getPrice() + path1.getPrice()))
//                        bestNode = adjacentAdjacent;
//                }
//            }
//        }
//
//        return bestNode;
//    }

    private void updateVisibleThief(GameView gameView, List<myPolice> policeList) {
        Agent me = gameView.getViewer();
        if (gameView.getConfig().getTurnSettings().getVisibleTurnsList().contains(gameView.getTurn().getTurnNumber() - 1)) {
            thievesVisibleLocations = new ArrayList<>();
            myLastKnownLoc = gameView.getViewer().getNodeId();
            for (Agent agent : gameView.getVisibleAgentsList()) {
                if (agent.getTeamValue() == me.getTeamValue() && agent.getType() == AIProto.AgentType.JOKER && !agent.getIsDead()
                        && thereIsPoliceInRadius(agent.getNodeId(), gameView.getConfig().getGraph().getVisibleRadiusYPoliceJoker(), policeList)) {
                    thievesVisibleLocations.add(agent.getNodeId());
                }
                if (agent.getTeamValue() == me.getTeamValue() && agent.getType() == AgentType.THIEF && !agent.getIsDead()
                        && thereIsPoliceInRadius(agent.getNodeId(), gameView.getConfig().getGraph().getVisibleRadiusXPoliceThief(), policeList)) {
                    thievesVisibleLocations.add(agent.getNodeId());
                }
            }
            thievesVisibleLocations.add(me.getNodeId());
        }
    }

    private boolean thereIsPoliceInRadius(int myNode, int radius, List<myPolice> policeNodes) {
        for (myPolice policeNode : policeNodes) {
            if (graphController.getDistance(myNode, policeNode.node, 0.1) <= radius) {
                return true;
            }
        }
        return false;
    }

    private boolean haveThiefHereWithHigherId(Agent me, List<Agent> otherThieves) {
        for (Agent thief : otherThieves) {
            if (thief.getNodeId() == me.getNodeId() && thief.getId() < me.getId())
                return true;
        }
        return false;
    }

    private String getBatmanChatMessage(int node) {
        String message = String.format("%" + 8 + "s",
                Integer.toBinaryString(node)).replaceAll(" ", "0");
        return message;
    }

    private myPolice getMyPoliceBatmanFromChat(String chat) {
        int node = Integer.parseInt(chat, 2);
        return new myPolice(999, node);
    }


}

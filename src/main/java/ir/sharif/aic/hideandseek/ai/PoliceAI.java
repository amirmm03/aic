package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.client.Phone;
import ir.sharif.aic.hideandseek.protobuf.AIProto;
import ir.sharif.aic.hideandseek.protobuf.AIProto.GameView;
import ir.sharif.aic.hideandseek.protobuf.AIProto.Agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PoliceAI extends AI {
    private PoliceGraphController policeGraphController;
    private HashMap<MyThief, Boolean> thievesCaptured = new HashMap<>();
    ArrayList<Agent> OtherPolices = new ArrayList<>();
    private int numberOfMovesAfterGettingClose = 4;


    private int lastChatBoxSize = 0;


    public PoliceAI(Phone phone) {
        this.phone = phone;
    }

    /**
     * This function always returns zero (Polices can not set their starting node).
     */
    @Override
    public int getStartingNode(GameView gameView) {
        policeGraphController = new PoliceGraphController(gameView);
        return 1;
    }

    /**
     * Implement this function to move your police agent based on current game view.
     */
    @Override
    public int move(GameView gameView) {
        int currentTurn = gameView.getTurn().getTurnNumber();

        updateThief(gameView);
        policeGraphController.updateInfo();

        for (int i = lastChatBoxSize; i < gameView.getChatBoxCount(); i++) {
            AIProto.Chat chat = gameView.getChatBox(i);
            MessageData messageData = extractData(chat.getText());
            MyThief myThief = new MyThief(currentTurn, messageData.nodeId, messageData.type == AIProto.AgentType.JOKER);

            thievesCaptured.put(myThief, false);
        }
        if (thievesCaptured.isEmpty()) {
            return policeGraphController.distributedMove(gameView);
        }
        //System.out.println("size of " + thievesCaptured.keySet().size());


        lastChatBoxSize = gameView.getChatBoxCount();


        if (false) {

            return gameView.getViewer().getNodeId();
        } else {


            Agent me = gameView.getViewer();
            if (havePoliceHereWithHigherId(me)) {
                return policeGraphController.randomMove(me.getNodeId(), gameView.getBalance());
            }


            MyThief closestThief = policeGraphController.findClosestThief(gameView, thievesCaptured);
            if (thievesCaptured.get(closestThief)) {
                numberOfMovesAfterGettingClose--;
                return policeGraphController.randomMoveNearThief(me.getNodeId(), closestThief.getNodeId(), gameView.getBalance(), numberOfMovesAfterGettingClose);
            }
//        LinkedHashMap<Integer, Integer> polices = new LinkedHashMap<>();
//        for (Agent otherPolice : OtherPolices) {
//            polices.put(otherPolice.getId(), otherPolice.getNodeId());
//        }
//        polices.put(me.getId(), me.getNodeId());
//        ArrayList<Integer> thiefNodes = new ArrayList<>();
//        for (Agent agent : thievesCaptured.keySet()) {
//            if (!thievesCaptured.get(agent))
//                thiefNodes.add(agent.getNodeId());
//        }
//
//        int targetNode = policeGraphController.getNextNodeWithMinimax(me.getId(), 2, polices, thiefNodes);
//        if (policeGraphController.bestEvalScore>10000) {
//            return targetNode;
//        }

            numberOfMovesAfterGettingClose = 4;
            int nextNode = policeGraphController.getNextOnPath(me.getNodeId(), closestThief.getNodeId(), gameView.getBalance());

            if (nextNode == closestThief.getNodeId()) {
                thievesCaptured.put(closestThief, true);
            }
            return nextNode;
        }
    }

    private boolean havePoliceHereWithHigherId(Agent me) {
        for (Agent otherPolice : OtherPolices) {
            if (otherPolice.getNodeId() == me.getNodeId() && otherPolice.getId() > me.getId())
                return true;
        }
        return false;
    }

//    private boolean allThievesCaptured() {
//        for (Agent agent : thievesCaptured.keySet()) {
//            if (!thievesCaptured.get(agent))
//                return false;
//        }
//        return true;
//    }

    private void updateThief(GameView gameView) {
        Agent me = gameView.getViewer();
        if (gameView.getConfig().getTurnSettings().getVisibleTurnsList().contains(gameView.getTurn().getTurnNumber())) {
            thievesCaptured = new HashMap<>();
            for (Agent agent : gameView.getVisibleAgentsList()) {
                if (agent.getTeamValue() != me.getTeamValue() && (agent.getType() == AIProto.AgentType.THIEF || agent.getType() == AIProto.AgentType.JOKER) && !agent.getIsDead()) {
                    if (agent.getType() != AIProto.AgentType.JOKER) {
                        phone.sendMessage(getChatMessage(agent.getType(), agent.getNodeId()));
                    }

                    thievesCaptured.put(new MyThief( gameView.getTurn().getTurnNumber(),agent.getNodeId(),agent.getType() == AIProto.AgentType.JOKER), false);
                }
            }
        }
        OtherPolices.clear();
        for (Agent myPolice : gameView.getVisibleAgentsList()) {
            if (myPolice.getTeam() == me.getTeam() && (myPolice.getType() == AIProto.AgentType.POLICE || myPolice.getType() == AIProto.AgentType.BATMAN)) {
                OtherPolices.add(myPolice);
                for (MyThief thief : thievesCaptured.keySet()) {
                    if (thief.getNodeId() == myPolice.getNodeId())
                        thievesCaptured.put(thief, true);
                }
            }
        }
    }


    private String getChatMessage(AIProto.AgentType type, int nodeId) {
        String nodeIdString = String.format("%" + 8 + "s",
                Integer.toBinaryString(nodeId)).replaceAll(" ", "0");

        if (type == AIProto.AgentType.JOKER) {
            return "1" + nodeIdString;
        } else {
            return "0" + nodeIdString;
        }
    }

    private MessageData extractData(String message) {
        int nodeId = Integer.parseInt(message.substring(1, 9), 2);

        if (message.charAt(0) == '1') {
            return new MessageData(AIProto.AgentType.JOKER, nodeId);
        } else {
            return new MessageData(AIProto.AgentType.THIEF, nodeId);
        }
    }
}


class MessageData {
    public AIProto.AgentType type;
    public int nodeId;

    public MessageData(AIProto.AgentType type, int nodeId) {
        this.type = type;
        this.nodeId = nodeId;
    }
}


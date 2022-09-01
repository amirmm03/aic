package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.client.Phone;
import ir.sharif.aic.hideandseek.protobuf.AIProto;
import ir.sharif.aic.hideandseek.protobuf.AIProto.GameView;
import ir.sharif.aic.hideandseek.protobuf.AIProto.Agent;

import javax.swing.plaf.TableHeaderUI;
import java.lang.invoke.MutableCallSite;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PoliceAI extends AI {
    private PoliceGraphController policeGraphController;
    private HashMap<MyThief, Boolean> thievesCaptured = new HashMap<>();
    ArrayList<Agent> OtherPolices = new ArrayList<>();
    private int numberOfMovesAfterGettingClose = 4;

    private int lastSentChat = 0;

    private Integer joker_node = 0;
    private Integer joker_last_update = 0;
    private Integer joker_last_notify = 0;

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
            ArrayList<MyThief> badAgents = getThievesFromMessage(currentTurn, chat.getText());

            for (MyThief badAgent : badAgents) {
                thievesCaptured.put(badAgent, false);
            }

        }
        lastChatBoxSize = gameView.getChatBoxCount();

        if (thievesCaptured.isEmpty()) {
            return policeGraphController.distributedMove(gameView);
        }


        if (gameView.getConfig().getTurnSettings().getVisibleTurnsList().contains(currentTurn)) {
            for (Agent agent : gameView.getVisibleAgentsList()) {
                if (agent.getType() == AIProto.AgentType.JOKER &&
                        agent.getTeamValue() != gameView.getViewer().getTeamValue()) {
                    joker_last_update = gameView.getTurn().getTurnNumber();
                    joker_node = agent.getNodeId();
                }
            }
        }


        if (currentTurn - joker_last_notify >= 3 && currentTurn - joker_last_update <= 3) {
            joker_last_notify.byteValue();
        }


        Agent me = gameView.getViewer();
        MyThief joker = getJokerNode();
        if (joker != null && thievesCaptured.keySet().size() > 1) {
            thievesCaptured.clear();
            thievesCaptured.put(joker, false);
        }
//        if (getJokerNode()!=null && me.getType() == AIProto.AgentType.BATMAN) {
//            return policeGraphController.getNextOnPath(me.getNodeId(),getJokerNode().node, gameView.getBalance());
//        } else {


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
        int nextNode = policeGraphController.getNextOnPathWithoutIntersection(me.getNodeId(), closestThief.getNodeId(), gameView.getBalance(),OtherPolices,new ArrayList<>(thievesCaptured.keySet()));

        if (nextNode == closestThief.getNodeId()) {
            thievesCaptured.put(closestThief, true);
        }
        return nextNode;
        // }
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

            ArrayList<Agent> seenAgents = new ArrayList<>();

            for (Agent agent : gameView.getVisibleAgentsList()) {
                if (agent.getTeamValue() != me.getTeamValue() &&
                        (agent.getType() == AIProto.AgentType.THIEF || agent.getType() == AIProto.AgentType.JOKER)
                        && !agent.getIsDead()) {
                    seenAgents.add(agent);

                    thievesCaptured.put(new MyThief(gameView.getTurn().getTurnNumber(), agent.getNodeId(), agent.getType() == AIProto.AgentType.JOKER), false);
                }
            }

            int currentTurn = gameView.getTurn().getTurnNumber();

            String toSend = getChatMessageThiefArray(seenAgents).toString();

            if (currentTurn - lastSentChat > 4 && gameView.getBalance() > toSend.length() && !toSend.isEmpty()) {
                lastSentChat = currentTurn;
                phone.sendMessage(toSend);
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

    private StringBuilder getChatMessageThiefArray(ArrayList<Agent> agents) {
        StringBuilder message = new StringBuilder();
        for (Agent agent : agents) {
            if (agent.getType() == AIProto.AgentType.JOKER) {
                message.append("1");
            } else message.append("0");
            message.append(String.format("%" + 8 + "s",
                    Integer.toBinaryString(agent.getNodeId())).replaceAll(" ", "0"));
        }
        return message;
    }

    private ArrayList<MyThief> getThievesFromMessage(int turnNumber, String message) {
        ArrayList<MyThief> result = new ArrayList<>();

        int count = message.length() / 9;
        for (int i = 0; i < count; i++) {
            String agentMessage = message.substring(i * 9, (i + 1) * 9);
            int node = Integer.parseInt(agentMessage.substring(1, 9), 2);
            result.add(new MyThief(turnNumber, node, agentMessage.charAt(0) == '1'));
        }

        return result;
    }


    private MyThief getJokerNode() {

        for (MyThief myThief : thievesCaptured.keySet()) {
            if (myThief.isJoker && !thievesCaptured.get(myThief)) {
                return myThief;
            }
        }
        return null;
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


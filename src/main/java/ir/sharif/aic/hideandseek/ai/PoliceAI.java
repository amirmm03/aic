package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.client.Phone;
import ir.sharif.aic.hideandseek.protobuf.AIProto;
import ir.sharif.aic.hideandseek.protobuf.AIProto.GameView;
import ir.sharif.aic.hideandseek.protobuf.AIProto.Agent;

import java.util.HashMap;

public class PoliceAI extends AI {
    private PoliceGraphController policeGraphController;
    private HashMap<Agent, Boolean> thievesCaptured = new HashMap<>();

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
        if (gameView.getConfig().getTurnSettings().getVisibleTurnsList().contains(gameView.getTurn().getTurnNumber())) {
            updateThief(gameView);
        }


        if (thievesCaptured.isEmpty() || allThievesCaptured()) {
            return policeGraphController.distributedMove(gameView,phone);
        }

        Agent me = gameView.getViewer();
        Agent closestThief = policeGraphController.findClosestThief(gameView,thievesCaptured);

        if (thievesCaptured.get(closestThief)) {
            return policeGraphController.randomMove(me.getNodeId());
        }
        int nextNode = policeGraphController.getNextOnPath(me.getNodeId(), closestThief.getNodeId());

        if (nextNode == closestThief.getNodeId()) {
            thievesCaptured.put(closestThief,true);
        }
        return nextNode;

    }

    private boolean allThievesCaptured() {
        for (Agent agent : thievesCaptured.keySet()) {
            if (!thievesCaptured.get(agent))
                return false;
        }
        return true;
    }

    private void updateThief(GameView gameView) {
        Agent me = gameView.getViewer();
        thievesCaptured = new HashMap<>();
        for (Agent agent : gameView.getVisibleAgentsList()) {
            if (agent.getTeamValue() != me.getTeamValue() && agent.getType() == AIProto.AgentType.THIEF && !agent.getIsDead()) {
                thievesCaptured.put(agent,false);
            }
        }
    }

}

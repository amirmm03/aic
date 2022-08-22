package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.client.Phone;
import ir.sharif.aic.hideandseek.protobuf.AIProto;
import ir.sharif.aic.hideandseek.protobuf.AIProto.GameView;
import ir.sharif.aic.hideandseek.protobuf.AIProto.Agent;

import java.util.ArrayList;
import java.util.HashMap;

public class PoliceAI extends AI {
    private PoliceGraphController policeGraphController;
    private HashMap<Agent, Boolean> thievesCaptured = new HashMap<>();
    ArrayList<Agent> OtherPolices = new ArrayList<>();

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
        policeGraphController.updateInfo();
        updateThief(gameView);

        if (thievesCaptured.isEmpty()) {
            return policeGraphController.distributedMove(gameView);
        }


        Agent me = gameView.getViewer();
        if (havePoliceHereWithHigherId(me)) {
            return policeGraphController.randomMove(me.getNodeId(), gameView.getBalance());
        }


        Agent closestThief = policeGraphController.findClosestThief(gameView, thievesCaptured);

        if (thievesCaptured.get(closestThief)) {
            return policeGraphController.randomMove(me.getNodeId(), gameView.getBalance());
        }
        int nextNode = policeGraphController.getNextOnPath(me.getNodeId(), closestThief.getNodeId(), gameView.getBalance());

        if (nextNode == closestThief.getNodeId()) {
            thievesCaptured.put(closestThief, true);
        }
        return nextNode;

    }

    private boolean havePoliceHereWithHigherId(Agent me) {
        for (Agent otherPolice : OtherPolices) {
            if (otherPolice.getNodeId() == me.getNodeId() && otherPolice.getId() > me.getId())
                return true;
        }
        return false;
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
        if (gameView.getConfig().getTurnSettings().getVisibleTurnsList().contains(gameView.getTurn().getTurnNumber())) {
            thievesCaptured = new HashMap<>();
            for (Agent agent : gameView.getVisibleAgentsList()) {
                if (agent.getTeamValue() != me.getTeamValue() && agent.getType() == AIProto.AgentType.THIEF && !agent.getIsDead()) {
                    thievesCaptured.put(agent, false);
                }
            }
        }
        OtherPolices.clear();
        for (Agent myPolice : gameView.getVisibleAgentsList()) {
            if (myPolice.getTeam() == me.getTeam() && myPolice.getType() == AIProto.AgentType.POLICE) {
                OtherPolices.add(myPolice);
                for (Agent thief : thievesCaptured.keySet()) {
                    if (thief.getNodeId() == myPolice.getNodeId())
                        thievesCaptured.put(thief, true);
                }
            }
        }
    }

}

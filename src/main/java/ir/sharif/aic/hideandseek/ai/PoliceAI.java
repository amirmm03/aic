package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.client.Phone;
import ir.sharif.aic.hideandseek.protobuf.AIProto;
import ir.sharif.aic.hideandseek.protobuf.AIProto.GameView;
import ir.sharif.aic.hideandseek.protobuf.AIProto.Agent;

import javax.swing.text.StyledEditorKit;
import java.util.ArrayList;
import java.util.Random;

public class PoliceAI extends AI {
    private PoliceGraphController policeGraphController;
    private ArrayList<AIProto.Agent> thiefs = new ArrayList<>();
    private boolean hasReachedATheif = false;
    private Random random = new Random();

    public PoliceAI(Phone phone) {
        this.phone = phone;
    }

    /**
     * This function always returns zero (Polices can not set their starting node).
     */
    @Override
    public int getStartingNode(GameView gameView) {
        policeGraphController = new PoliceGraphController(gameView.getConfig().getGraph());
        return 1;
    }

    /**
     * Implement this function to move your police agent based on current game view.
     */
    @Override
    public int move(GameView gameView) {
        if (gameView.getConfig().getTurnSettings().getVisibleTurnsList().contains(gameView.getTurn().getTurnNumber())) {
            updateThief(gameView);
            hasReachedATheif = false;
        }
        if (hasReachedATheif) {
            //random move
        }
        if (thiefs.isEmpty()) {
            //distribute
        }
        Agent me = gameView.getViewer();
        Agent closestThief = null;
        for (Agent thief : thiefs) {
            if (closestThief == null || policeGraphController.getDistance(me.getNodeId(), thief.getNodeId()) <
                    policeGraphController.getDistance(me.getNodeId(), closestThief.getNodeId()))
                closestThief = thief;
        }

        int nextNode = policeGraphController.getNextOnPath(me.getNodeId(), closestThief.getNodeId());
        if (nextNode == closestThief.getNodeId()){
            thiefs.remove(closestThief);
            hasReachedATheif = true;
        }
        return nextNode;

    }

    private void updateThief(GameView gameView) {
        Agent me = gameView.getViewer();
        thiefs = new ArrayList<>();
        for (Agent agent : gameView.getVisibleAgentsList()) {
            if (agent.getTeamValue() != me.getTeamValue() && agent.getType() == AIProto.AgentType.THIEF && !agent.getIsDead()) {
                thiefs.add(agent);
            }
        }
    }

}

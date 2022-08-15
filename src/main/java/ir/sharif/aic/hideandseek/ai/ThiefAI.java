package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.client.Phone;
import ir.sharif.aic.hideandseek.protobuf.AIProto.*;
import ir.sharif.aic.hideandseek.protobuf.AIProto.GameView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ThiefAI extends AI {
    private static ArrayList<Integer>[] initialNodes = new ArrayList[2];
    static {
        initialNodes[0] = new ArrayList<Integer>();
        initialNodes[1] = new ArrayList<Integer>();
    }


    private GraphController graphController;
    public ThiefAI(Phone phone) {
        this.phone = phone;
    }

    /**
     * Used to select the starting node.
     */
    @Override
    public int getStartingNode(GameView gameView) {
        graphController = new GraphController(gameView.getConfig().getGraph());

        int target = 2;
        double score = 0;

        for (Node node : gameView.getConfig().getGraph().getNodesList()) {
            double tempScore = (double) graphController.getDistance(node.getId(), 1);
            for (Integer allayNode : initialNodes[gameView.getViewer().getTeam().getNumber()]) {
                tempScore += ((double) graphController.getDistance(node.getId(), allayNode)) * 0.3;
            }

            if (tempScore > score) {
                score = tempScore;
                target = node.getId();
            }
        }

        initialNodes[gameView.getViewer().getTeam().getNumber()].add(target);
        return target;
    }

    /**
     * Implement this function to move your thief agent based on current game view.
     */
    @Override
    public int move(GameView gameView) {
        Agent me = gameView.getViewer();
        ArrayList<Path> adjacentPath = graphController.getAdjacent(me.getNodeId());
        List<Integer> policeList = new ArrayList<>();
        for (Agent agent : gameView.getVisibleAgentsList())
            if (agent.getTeamValue() != me.getTeamValue() && agent.getType() == AgentType.POLICE)
                policeList.add(agent.getNodeId());
        int next = me.getNodeId();
        for (Path path : adjacentPath) {
            int adjacent = me.getNodeId() ^ path.getFirstNodeId() ^ path.getSecondNodeId();
            if (graphController.getScore(next, policeList) <= graphController.getScore(adjacent, policeList))
                next = adjacent;
        }
        return next;
    }

}

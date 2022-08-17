package ir.sharif.aic.hideandseek.ai;

import com.google.type.DateTime;
import ir.sharif.aic.hideandseek.client.Phone;
import ir.sharif.aic.hideandseek.protobuf.AIProto.*;
import ir.sharif.aic.hideandseek.protobuf.AIProto.GameView;

import java.util.*;

public class ThiefAI extends AI {
    private ThiefGraphController graphController;

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

        ArrayList<Node> goodNodes = new ArrayList<>();
        for (Node node: gameView.getConfig().getGraph().getNodesList()) {
            if (graphController.getDistance(node.getId(), 1, Double.MAX_VALUE) >= (bestScore * 2)/3) {
                goodNodes.add(node);
            }
        }

        Random random = new Random(System.currentTimeMillis());

        target = goodNodes.get((random.nextInt(goodNodes.size()) + gameView.getViewer().getId())%goodNodes.size()).getId();

        return target;
    }

    /**
     * Implement this function to move your thief agent based on current game view.
     */
    @Override
    public int move(GameView gameView) {
        graphController.updateInfo();
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

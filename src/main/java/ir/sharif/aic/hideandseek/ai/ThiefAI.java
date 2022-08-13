package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.client.Phone;
import ir.sharif.aic.hideandseek.protobuf.AIProto.GameView;

import java.util.Random;

public class ThiefAI extends AI {

    public ThiefAI(Phone phone) {
        this.phone = phone;
    }

    /**
     * Used to select the starting node.
     */
    @Override
    public int getStartingNode(GameView gameView) {
        Random random = new Random();

        return  random.nextInt(gameView.getConfig().getGraph().getNodesCount()) + 1;
    }

    /**
     * Implement this function to move your thief agent based on current game view.
     */
    @Override
    public int move(GameView gameView) {
        return 2;
    }

}

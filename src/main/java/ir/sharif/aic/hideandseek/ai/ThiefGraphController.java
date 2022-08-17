package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.protobuf.AIProto;

import java.util.ArrayList;
import java.util.List;

public class ThiefGraphController extends GraphController{

    public ThiefGraphController(AIProto.Graph graph) {
        super(graph);
    }

    public double getScore(int nodeId, List<Integer> policeList, List<Integer> thieveList) {
        double output = 10000000;
        for (int police : policeList)
            output = Math.min(output, getDistance(nodeId, police,Double.MAX_VALUE));
        for (Integer thief : thieveList) {
            output += (double) getDistance(nodeId, thief, Double.MAX_VALUE) / 10;
        }
        return output;
    }

    public ArrayList<AIProto.Path> getAdjacent(int v) {
        return adjacent[v];
    }

    public ArrayList<Integer> getBestCombinationOfNodes(ArrayList<Integer> goodNodes, int size, ArrayList<Integer> alreadyChosenNodes) {
        if (size == 0) {
            return alreadyChosenNodes;
        }
        ArrayList<Integer> chosenNodes = new ArrayList<>();
        int price = 0;
        for (int i = 0; i < goodNodes.size(); i++) {
            int node = goodNodes.get(i);
            alreadyChosenNodes.add(node);
            goodNodes.remove((Object)node);
            ArrayList<Integer> tempChosenNodes = getBestCombinationOfNodes(goodNodes, size - 1, alreadyChosenNodes);
            int tempPrice = calculatePrice(tempChosenNodes);
            if (price < tempPrice) {
                price = tempPrice;
                chosenNodes.clear();
                chosenNodes.addAll(tempChosenNodes);
            }
            goodNodes.add(i, node);
            alreadyChosenNodes.remove((Object)node);
        }
        return chosenNodes;
    }
    private int calculatePrice(ArrayList<Integer> nodes) {
        if (nodes.size() == 1) return 0;
        int lastNode = nodes.get(nodes.size() - 1);
        nodes.remove(nodes.size() - 1);
        int price = calculatePrice(nodes);
        for (Integer node : nodes) {
            price += getDistance(node, lastNode, Double.MAX_VALUE);
        }
        nodes.add(lastNode);
        return price;
    }
}

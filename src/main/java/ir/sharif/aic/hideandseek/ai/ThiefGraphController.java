package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.protobuf.AIProto;

import java.util.ArrayList;
import java.util.List;

public class ThiefGraphController extends GraphController {

    public ThiefGraphController(AIProto.Graph graph) {
        super(graph);
    }

    public double getScore(int nodeId, List<Integer> policeList, List<AIProto.Agent> thieveList, ArrayList<Integer> thievesVisibleLocations) {

        double output = 10000000;



        for (int police : policeList)
            output = Math.min(output, getDistance(nodeId, police, Double.MAX_VALUE));

        if (output <= 2) {
            return output;
        }

        if (thievesVisibleLocations.contains(nodeId)) return 1;

        for (int police : policeList)
            output += (getDistance(nodeId, police, Double.MAX_VALUE));
        output /= policeList.size();

        int thiefEffect = 0;
        for (AIProto.Agent thief : thieveList) {
            thiefEffect += (double) getDistance(nodeId, thief.getNodeId(), Double.MAX_VALUE) / 3;
        }
        if (thiefEffect != 0)
            thiefEffect /= thieveList.size();
        output += thiefEffect;

        int closest = Integer.MAX_VALUE;
        for (Integer node : thievesVisibleLocations) {
            closest = Math.min(closest, getDistance(nodeId, node, Double.MAX_VALUE));
        }
        output += (double) closest / 2;


        output += (double) getAdjacent(nodeId).size() / 4;

//        for (AIProto.Path path : adjacentPath) {
//            int adjacent = nodeId ^ path.getFirstNodeId() ^ path.getSecondNodeId();
//            if (thievesVisibleLocations.contain(adjacent) )
//                output /= 4;`
//        }


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
            goodNodes.remove((Object) node);
            ArrayList<Integer> tempChosenNodes = getBestCombinationOfNodes(goodNodes, size - 1, alreadyChosenNodes);
            int tempPrice = calculatePrice(tempChosenNodes);

            if (price < tempPrice || (price == tempPrice & minDistance(tempChosenNodes) > minDistance(chosenNodes))) {
                price = tempPrice;
                chosenNodes.clear();
                chosenNodes.addAll(tempChosenNodes);
            }
            goodNodes.add(i, node);
            alreadyChosenNodes.remove((Object) node);
        }
        return chosenNodes;
    }

    private int minDistance(ArrayList<Integer> chosenNodes) {
        int ans = 10000000;
        for (Integer node1 : chosenNodes) {
            for (Integer node2 : chosenNodes) {
                if (node1 != node2)
                    ans = Math.min(ans, getDistance(node1, node2, Double.MAX_VALUE));
            }
        }
        return ans;
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

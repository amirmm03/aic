package ir.sharif.aic.hideandseek.ai;

import ir.sharif.aic.hideandseek.protobuf.AIProto;

import java.util.ArrayList;
import java.util.List;

public class ThiefGraphController extends GraphController {

    public ThiefGraphController(AIProto.Graph graph) {
        super(graph);
    }

    public double getScore(int nodeId, List<myPolice> policeList, List<AIProto.Agent> thieveList, ArrayList<Integer> thievesVisibleLocations) {

        double output = 10000000;


        for (myPolice police : policeList)
            output = Math.min(output, getDistance(nodeId, police.node, Double.MAX_VALUE));

        if (output <= 2) {
            return output;
        }

        if (thievesVisibleLocations.contains(nodeId)) return 1;

        for (myPolice police : policeList)
            output += (getDistance(nodeId, police.node, Double.MAX_VALUE));
        output /= policeList.size();

        int thiefEffect = 0;
        for (AIProto.Agent thief : thieveList) {
            thiefEffect += (double) getDistance(nodeId, thief.getNodeId(), Double.MAX_VALUE) / 5;
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

    public int bestNodeWithMinimax(int myCurrentLoc, List<myPolice> policeList, List<AIProto.Agent> thieveList, ArrayList<Integer> thievesVisibleLocations, int depth, int myLastKnownLoc, int thisturn, List<Integer> visibleTurnsList) {
        double bestScore = getScore(myCurrentLoc,policeList,thieveList,thievesVisibleLocations);

        int bestMove = myCurrentLoc;
        for (AIProto.Path path : adjacent[myCurrentLoc]) {
            int nextNode = path.getFirstNodeId() ^ path.getSecondNodeId() ^ myCurrentLoc;
            List<myPolice> clonedPolice = new ArrayList<>(policeList);
            policeMove(clonedPolice, myLastKnownLoc);
            double tmpScore = minimax(nextNode,clonedPolice,thieveList,thievesVisibleLocations,depth-1,myLastKnownLoc,thisturn+2,visibleTurnsList );
            if (tmpScore > bestScore) {
                bestScore = tmpScore;
                bestMove = nextNode;
            }
        }
        return bestMove;
    }

    public double minimax(int myCurrentLoc, List<myPolice> policeList, List<AIProto.Agent> thieveList, ArrayList<Integer> thievesVisibleLocations, int depth, int myLastKnownLoc, int thisturn, List<Integer> visibleTurnsList) {

        if (depth == 0)
            return getScore(myCurrentLoc,policeList,thieveList,thievesVisibleLocations);
        double bestScore = getScore(myCurrentLoc,policeList,thieveList,thievesVisibleLocations);
        if (bestScore<1){
            return 0;
        }
        if (visibleTurnsList.contains(thisturn-1)){
            myLastKnownLoc = myCurrentLoc;
        }
        for (AIProto.Path path : adjacent[myCurrentLoc]) {
            int nextNode = path.getFirstNodeId() ^ path.getSecondNodeId() ^ myCurrentLoc;

            List<myPolice> clonedPolice = new ArrayList<>(policeList);
            policeMove(clonedPolice, myLastKnownLoc);
            double tmpScore = minimax(nextNode,clonedPolice,thieveList,thievesVisibleLocations,depth-1,myLastKnownLoc,thisturn+2,visibleTurnsList );
            if (tmpScore > bestScore) {
                bestScore = tmpScore;
            }
        }
        return bestScore;
    }

    private void policeMove(List<myPolice> policeList, int myLastKnownPlace) {
        for (int i = 0; i < policeList.size(); i++) {

            if (havePoliceHereWithHigherId(policeList, i)) {
                randomMove(policeList.get(i));
                break;
            }
            policeList.get(i).node = getNextOnPathWithoutIntersection(policeList.get(i), myLastKnownPlace, policeList);
            break;
        }
    }

    private int getNextOnPathWithoutIntersection(myPolice me, int thiefNode, List<myPolice> policeList) {
        ArrayList<Integer> destinations = new ArrayList<>();


        for (myPolice myPolice : policeList) {

            if (getDistance(me.node, thiefNode, Double.MAX_VALUE) > getDistance(myPolice.node, thiefNode, Double.MAX_VALUE)) {
                return newPath(me.node, thiefNode, Double.MAX_VALUE);
            }
        }

        return getNextOnPath(me.node, thiefNode, Double.MAX_VALUE);
    }

    private int newPath(int myNode, int thiefNode, double balance) {

        for (AIProto.Path path : adjacent[thiefNode]) {
            int adjNode = path.getFirstNodeId() ^ path.getSecondNodeId() ^ thiefNode;
            if (adjNode != getNodeBeforeDestination(myNode, thiefNode, balance) && getNodeBeforeDestination(myNode, adjNode, balance) != thiefNode) {
                return getNextOnPath(myNode, adjNode, balance);
            }
        }


        for (AIProto.Path path1 : adjacent[thiefNode]) {
            int adjNode1 = path1.getFirstNodeId() ^ path1.getSecondNodeId() ^ thiefNode;
            if (adjNode1 != getNodeBeforeDestination(myNode, thiefNode, balance)) {
                for (AIProto.Path path2 : adjacent[adjNode1]) {
                    int adjNode2 = path2.getFirstNodeId() ^ path2.getSecondNodeId() ^ adjNode1;
                    if (adjNode2 != getNodeBeforeDestination(myNode, adjNode1, balance) && getNodeBeforeDestination(myNode, adjNode2, balance) != adjNode1) {
                        return getNextOnPath(myNode, adjNode2, balance);
                    }

                }
            }
        }

        return getNextOnPath(myNode, thiefNode, balance);
    }

    private int getNodeBeforeDestination(int from, int dest, double balance) {
        return getNextOnPath(dest, from, balance);
    }

    private void randomMove(myPolice myPolice) {
        int node = myPolice.node;
        AIProto.Path path = adjacent[node].get(0);
        myPolice.node = path.getSecondNodeId() ^ path.getFirstNodeId() ^ node;
    }

    private boolean havePoliceHereWithHigherId(List<myPolice> policeList, int myIndex) {
        for (int i = 0; i < policeList.size(); i++) {
            if (policeList.get(i).node == policeList.get(myIndex).node && policeList.get(i).id < policeList.get(myIndex).id)
                return true;
        }
        return false;
    }




}

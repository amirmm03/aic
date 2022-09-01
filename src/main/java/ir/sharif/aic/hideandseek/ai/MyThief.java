package ir.sharif.aic.hideandseek.ai;

public class MyThief {
    public boolean isJoker;
    private int turn;
    public int node;

    public MyThief(int turn , int node , boolean isJoker){
        this.turn = turn;
        this.node = node;
        this.isJoker = isJoker;
    }
    public int getNodeId(){
        return node;
    }


}

package cs271.HW1;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 10/21/13
 * Time: 10:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class Event {
    public int time;
    public int nodeId;
    public String key;
    public String op;

    public Event(String Key, String Op, int Time, int Node){
        time = Time;
        nodeId = Node;
        key = Key;
        op = Op;
    }
}

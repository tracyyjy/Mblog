package cs271.HW1;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 10/21/13
 * Time: 10:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class Message {
    int sourceReplicaId;
    int destReplicaId;
    ArrayList<Event> NP = new ArrayList<Event>();
    int[][] TimeTable = new int[3][3];
    public Message(int s, int d){
        sourceReplicaId = s;
        destReplicaId = d;
    }
}

package cs271.HW1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 10/21/13
 * Time: 10:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class Replication {

    private int Clock;
    public ArrayList<Event> Log;

    public int ID;
    public int[][] TimeTable;
    public Map<String, Integer> dict = new HashMap<String, Integer>();

    public Replication(int id){
        ID = id;
        Clock = 0;
        Log = new ArrayList<Event>();
        TimeTable = new int[3][3];
    }

    public int clock(){
        Clock++;
        return Clock;
    }

    public void operate(String key, String op){
        TimeTable[ID][ID] = clock();
        Log.add(new Event(key, op, TimeTable[ID][ID], ID));
        if (op.equals("increment"))
            increment(key);
        else if (op.equals("decrement"))
            decrement(key);
    }

    public void update(String key, String op){
        if (op.equals("increment"))
            increment(key);
        else if (op.equals("decrement"))
            decrement(key);
    }

    private void increment(String key){
        if (dict.containsKey(key))
            dict.put(key, dict.get(key)+1);
        else
            dict.put(key, 1);
    }

    private void decrement(String key){
        if (dict.containsKey(key))
            dict.put(key, dict.get(key)-1);
        else
            dict.put(key, -1);
    }

}

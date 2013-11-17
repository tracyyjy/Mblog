package cs271.HW1;
import java.util.*;
import java.util.regex.*;

import static java.lang.System.*;

public class Main {

    public static void main(String[] args) {
        out.println("This is a simulated distributed logging program.");
        out.println("\nCommand: function para1 para2");
        out.println("Example: decrement(2, name)");
        out.println("Options: replicaId = [0-2]; Key = [a-zA-Z_0-9]+; DON'T wrap keys with \"\"; non-existent keys will be created.");
        out.println("      1: increment replicaId Key");
        out.println("      2: decrement replicaId Key");
        out.println("      3: getValue replicaId Key");
        out.println("      4: printState replicaId");
        out.println("      5: sendLog sourceReplicaId destReplicaId");
        out.println("      6: receiveLog transmissionNumber ");

        ArrayList<Message> messages = new ArrayList<Message>();

        Replication[] cluster = new Replication[3];
        for (int i = 0; i < cluster.length; i++) cluster[i] = new Replication(i);

        Scanner scanner = new Scanner(in);
        out.println("\nPlease command or exit");
        String command = scanner.nextLine();
        Pattern pattern = Pattern.compile("[a-zA-Z](\\s+)(\\d+)(\\s*)(\\w*)");

        while (!command.equals("exit")){
            Matcher m = pattern.matcher(command);

            try {
                if (m.find()) {
                    String[] commArray = command.split("\\s+");
                    if (commArray[0].equals("increment"))           increment(Integer.parseInt(commArray[1]), commArray[2], cluster);
                    else if (commArray[0].equals("decrement"))      decrement(Integer.parseInt(commArray[1]), commArray[2], cluster);
                    else if (commArray[0].equals("getValue"))       getValue (Integer.parseInt(commArray[1]), commArray[2], cluster);
                    else if (commArray[0].equals("printState"))     printState(Integer.parseInt(commArray[1]), cluster);
                    else if (commArray[0].equals("sendLog"))        send(Integer.parseInt(commArray[1]), Integer.parseInt(commArray[2]), cluster, messages);
                    else if (commArray[0].equals("receiveLog"))     receive(Integer.parseInt(commArray[1]), cluster, messages);
                    else out.println("Invalid function or parameters!");
                }
                else out.println("Invalid function or parameters!");
            } catch (ArrayIndexOutOfBoundsException ex){
                out.println("Invalid function or parameters!");
            }

            out.println("\nPlease command:");
            command = scanner.nextLine();
        }
    }

    public static void increment (int replicaId, String key, Replication[] cluster ){
        cluster[replicaId].operate(key, "increment");
    }

    public static void decrement(int replicaId, String key, Replication[] cluster){
        cluster[replicaId].operate(key, "decrement");
    }

    public static void getValue(int replicaId, String key, Replication[] cluster){
        if (cluster[replicaId].dict.containsKey(key))
            out.println(key + " => " + cluster[replicaId].dict.get(key));
        else out.println("NULL. Key does not exist!");
    }

    public static void printState(int replicaId, Replication[] cluster){

        String kvStrings = "";
        for (String k: cluster[replicaId].dict.keySet()){
            kvStrings+= k + " => " + cluster[replicaId].dict.get(k) + "\n";
        }

        String logStrings = "";
        for (Event e: cluster[replicaId].Log)
            logStrings += "\"" + e.op + " " + e.key + "\" ";

        out.println(kvStrings);
        out.println("Log: { " + logStrings + "}\n");
        out.println("TimeTable:");
        out.println("|" + cluster[replicaId].TimeTable[0][0] + " " + cluster[replicaId].TimeTable[0][1] + " " + cluster[replicaId].TimeTable[0][2] + "|");
        out.println("|" + cluster[replicaId].TimeTable[1][0] + " " + cluster[replicaId].TimeTable[1][1] + " " + cluster[replicaId].TimeTable[1][2] + "|");
        out.println("|" + cluster[replicaId].TimeTable[2][0] + " " + cluster[replicaId].TimeTable[2][1] + " " + cluster[replicaId].TimeTable[2][2] + "|");
    }

    public static void send(int sourceReplicaId, int destReplicaId, Replication[] cluster, ArrayList<Message> messages) {
        Message m = new Message(sourceReplicaId, destReplicaId);
        for (Event event: cluster[sourceReplicaId].Log){
            if (!hasrec(sourceReplicaId, event, destReplicaId, cluster)) m.NP.add(event);
        }
        for (int i = 0; i < m.TimeTable.length; i++) {
            m.TimeTable[i] = cluster[sourceReplicaId].TimeTable[i].clone();
        }
        messages.add(m);
        out.println("transmission number: " + (messages.size()-1));
    }

    public static void receive(int transmissionNumber, Replication[] cluster, ArrayList<Message> messages) {
        try {
            Message m = messages.get(transmissionNumber);
            Replication dst = cluster[m.destReplicaId];
            ArrayList<Event> NE = new ArrayList<Event>();

            for (Event event: m.NP){
                if (!hasrec(m.destReplicaId,event,m.destReplicaId, cluster)) NE.add(event);
            }
            // Update Dict
            for (Event event: NE) dst.update(event.key, event.op);
            // Update TimeTable
            for (int i = 0; i < dst.TimeTable.length; i++){
                dst.TimeTable[m.destReplicaId][i] = Math.max(dst.TimeTable[m.destReplicaId][i], m.TimeTable[m.sourceReplicaId][i]);
            }
            for (int i = 0; i < dst.TimeTable.length; i++){
                for (int j = 0; j < dst.TimeTable[0].length; j++){
                    dst.TimeTable[i][j] = Math.max(dst.TimeTable[i][j], m.TimeTable[i][j]);
                }
            }
            // Update Log
            for (Event event: NE) if (!dst.Log.contains(event)) dst.Log.add(event);
            ArrayList<Event> toRemoveList = new ArrayList<Event>();
            for (Event event: dst.Log) {
                boolean knownToAll = true;
                for ( int j = 0; j < dst.TimeTable[0].length; j++){
                    if (!hasrec(m.destReplicaId, event, j, cluster))
                    {
                        knownToAll = false;
                        break;
                    }
                }
                if (knownToAll) toRemoveList.add(event);
            }
            dst.Log.removeAll(toRemoveList);

        } catch (IndexOutOfBoundsException ex){
            out.println("Incorrect transmission number!");
        }
    }

    public static boolean hasrec (int nodeId_i, Event eR, int nodeId_k, Replication[] cluster) {
        return (cluster[nodeId_i].TimeTable[nodeId_k][eR.nodeId] >= eR.time);
    }
}
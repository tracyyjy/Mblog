package cs271;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/15/13
 * Time: 11:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class Node {

    public ArrayList<String> Tweets = new ArrayList<String>();
    private Server server;
    ServerSocket listener = new ServerSocket(9898);

    public Node() throws IOException{
        server = new Server(this, listener);
    }

    private static void log(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) throws IOException {
        Node node = new Node();
        Thread serverThread  = new Thread(node.server);
        serverThread.start();
    }

}

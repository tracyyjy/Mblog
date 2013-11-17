package cs271;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/16/13
 * Time: 4:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class Server implements Runnable{
    private Node node;
    private Socket socket;
    private ServerSocket listener;

    public Server(Node node, ServerSocket listener) {
        this.node = node;
        this.listener = listener;
    }

    public void run() {
        try {
            while (true){
                this.socket = listener.accept();
                log("new connection established!");

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Send a welcome message to the client.
                out.println("Server connected, Node at your service.");

                // Get commands from the client, line by line. Then execute.
                while (true) {
                    String func = in.readLine();
                    String para = in.readLine();
                    if (func == null) {
                        break;
                    }
                    if (func.equals("post")) {
                        node.Tweets.add(para);
                    }
                    else if (func.equals("read")){
                        System.out.println(node.Tweets.get(node.Tweets.size()-1));
                    }
                }
            }
        } catch (IOException e) {
            log("Error handling client ");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log("Couldn't close a socket, what's going on?");
            }
            log("Connection with client closed");
        }
    }

    private void log(String message) {
        System.out.println(message);
    }
}

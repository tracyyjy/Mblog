package cs271;

import cs271.Messages.ClientMessage;
import cs271.Messages.ServerMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/16/13
 * Time: 4:21 PM
 * To change this template use File | Settings | File Templates.
 */

public class Server extends Thread{
    private Node node;
    private Socket socket;
    private ServerSocket listener;
    ObjectInputStream in;
    ObjectOutputStream out;

    public Server(Node node, ServerSocket listener) {
        this.node = node;
        this.listener = listener;
    }

    public void run() {
        while (true){
            try {
                while (true){
                    this.socket = listener.accept();
                    log("Server: new connection established!");

                    try{
                        out = new ObjectOutputStream(socket.getOutputStream());
                    }   catch( IOException e){
                        log("IOException while trying to get ObjectOutputStream!");
                    }

                    // send a welcome message to the client.
                    sendToClient("Server connected, node at your service.");

                    try{
                        in =  new ObjectInputStream(socket.getInputStream());
                    }  catch (IOException e){
                        log("IOException while trying to get ObjectInputStream!");
                    }

                    // execute client commands
                    while (true) {
                        try {
                            checkout((ClientMessage) in.readObject());
                        } catch (ClassNotFoundException e) {
                            log("ClassNotFoundException while trying to read client message object!");
                        }
                    }
                }
            }  catch ( IOException e) {
                log("IOException while checking out message!");
            }  finally {
                try {
                    socket.close();
                }
                catch (IOException e) {
                    log("Couldn't close a socket, what's going on?");
                }
                log("Connection with client closed");
            }
        }
    }

    private void checkout(ClientMessage message){
        if (message != null){
            String function = message.getFunction();
            if (function.equals("post")) {
                node.propose(message.getArgument());
                log("post received, voting...");
            }
            else if (function.equals("read")){
                sendToClient(node.getTweets());
            }
            else if (function.equals("fail")){
                node.fail();
                sendToClient("node failed");
            }
            else if (function.equals("unfail")){
                node.recover();
                sendToClient("node recovered");
            }
        else
            log("Unknown client message received!");
        }
    }

    public void sendToClient(String message){
        try {
            out.writeObject(new ServerMessage(message));
            out.flush();
        } catch (IOException e) {
            log("Exception while sending message to client!");
        }
    }

    private void log(String message) {
        System.out.println(message);
    }
}

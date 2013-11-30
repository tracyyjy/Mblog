package cs271;

import cs271.Messages.ClientMessage;
import cs271.Messages.ServerMessage;

import java.io.*;
import java.net.InetAddress;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/15/13
 * Time: 11:34 AM
 * To change this template use File | Settings | File Templates.
 */

public class Client {

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public Client(int port){

        // Make connection and initialize streams
        try {
            socket = new Socket(InetAddress.getLocalHost(), port);
        } catch (IOException e) {
            log("Exception connecting to server!");
        }

        try {
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            log("Exception when creating client-side input object stream!");
        }

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            checkout((ServerMessage) in.readObject());
        } catch (IOException e) {
            log("Exception when creating client-side output object stream!");
        } catch (ClassNotFoundException e){
            log("Exception when reading object from checkout!");
        }
    }

    public void run(){
        Scanner scanner = new Scanner(System.in);
        String command = scanner.nextLine();
        Pattern pattern = Pattern.compile("[a-z]+\\s*(\".*\")*\\s*");

        while (!command.equals("exit")){

            // parse the command
            Matcher m = pattern.matcher(command);

            // send client message
            try {
                if (m.find()) {
                    String[] commArray = command.split("\\s+");
                    if (commArray[0].equals("post"))           {
                        String[] commArray2 = command.split("\"");
                        String tweet = commArray2[1];
                        out.writeObject(new ClientMessage("post", tweet));
                        log("post message sent");
                    }
                    else if (commArray[0].equals("read"))
                        out.writeObject(new ClientMessage("read", ""));
                    else if (commArray[0].equals("fail"))
                        out.writeObject(new ClientMessage("fail", ""));
                    else if (commArray[0].equals("unfail"))
                        out.writeObject(new ClientMessage("unfail", ""));
                    else log("Invalid function or parameters!");
                    out.flush();
                }
                else log("Command pattern not found!");
            } catch (ArrayIndexOutOfBoundsException ex){
                log("Exception when reading commands!");
            } catch (IOException ex){
                log(" Exception when writing command objects");
            }

            // checkout server message
            try {
                checkout((ServerMessage) in.readObject());
            } catch (IOException e) {
                log("IOException while trying to read server message Object! ");
            } catch (ClassNotFoundException e) {
                log("ClassNotFoundException while trying to read server message Object! ");
            }

            log("\nPlease command or exit:");
            command = scanner.nextLine();
        }
    }

    public void log(String m){
        System.out.println(m);
    }

    public void checkout(ServerMessage message){
        if (message != null) log(message.getMessage());
        else log("Unknown message received!");
    }

    public void instruct() throws IOException{
        log("This is a distributed micro blog client.");
        log("\n" +
                "Command: function [para]");
        log("Example: post \"testString\"");
        log("      1: post tweet (wrap tweet with \"\")");
        log("      2: read");
        log("      3: fail");
        log("      4: unfail");
        log("\nPlease command or exit");

    }

    public static void main(String[] args) throws IOException {
        Client client = new Client(Integer.parseInt(args[0]));
        client.instruct();
        client.run();
    }

}

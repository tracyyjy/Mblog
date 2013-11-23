package cs271;

import java.net.InetAddress;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import static java.lang.System.in;
import static java.lang.System.out;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/15/13
 * Time: 11:34 AM
 * To change this template use File | Settings | File Templates.
 */

public class Client {

    private Socket socket;
    private BufferedReader tcp_in;
    private PrintWriter tcp_out;

    public Client() throws IOException{

        // Make connection and initialize streams
        socket = new Socket(InetAddress.getLocalHost(), 9898);
        tcp_in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        tcp_out = new PrintWriter(socket.getOutputStream(), true);
        // Consume the initial welcoming messages from the server
//        for (int i = 0; i < 1; i++) {
//            out.println(tcp_in.readLine());
//        }
        out.println(tcp_in.readLine());

    }


    public void run(){
        Scanner scanner = new Scanner(in);
        String command = scanner.nextLine();
        Pattern pattern = Pattern.compile("[a-z]+\\s*(\".*\")*\\s*");

        while (!command.equals("exit")){
            Matcher m = pattern.matcher(command);

            try {
                if (m.find()) {
                    String[] commArray = command.split("\\s+");
                    if (commArray[0].equals("post"))           {
                        String[] commArray2 = command.split("\"");
                        String tweet = commArray2[1];
                        sendCommand("post", tweet);
                    }
                    else if (commArray[0].equals("read"))      sendCommand("read", "");
                    else if (commArray[0].equals("fail"))       sendCommand("fail", "");
                    else if (commArray[0].equals("unfail"))     sendCommand("unfail", "");
                    else out.println("Invalid function or parameters!");
                }
                else out.println("not found! Invalid function or parameters!");
            } catch (ArrayIndexOutOfBoundsException ex){
                out.println("exception! Invalid function or parameters!");
            }

            out.println("\nPlease command or exit:");
            command = scanner.nextLine();
            try{
                out.println(tcp_in.readLine());
            }
            catch(IOException i)
            {
                i.printStackTrace();
            }


        }
    }

    public void instruct() throws IOException{
        out.println("This is a distributed micro blog client.");
        out.println("\n" +
                "Command: function [para]");
        out.println("Example: post \"testString\"");
        out.println("      1: post tweet (wrap tweet with \"\")");
        out.println("      2: read");
        out.println("      3: fail");
        out.println("      4: unfail");
        out.println("\nPlease command or exit");

    }

    public void sendCommand(String func, String para){
        tcp_out.println(func);
        tcp_out.println(para);
    }



    public static void main(String[] args) throws IOException {

        Client client = new Client();
        client.instruct();
        client.run();
    }

}

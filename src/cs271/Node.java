package cs271;

import cs271.Messages.Message;

import java.io.*;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/15/13
 * Time: 11:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class Node {

    // Node Data
    private Set<PeerInformation> cluster;
    private PeerInformation peerInformation;
    private NodeListener nodeListener;
    private boolean isRunning;

    public ArrayList<String> Tweets = new ArrayList<String>();
    private Server server;
    ServerSocket clientListener = new ServerSocket(9898);

    public Node() throws IOException{
        server = new Server(this, clientListener);
    }

    private static void log(String message) {
        System.out.println(message);
    }

    private synchronized void writeDebug(String s, boolean isError)
    {

        PrintStream out = isError ? System.err : System.out;
        out.print(toString());
        out.print(": ");
        out.println(s);
    }

    public static void main(String[] args) throws IOException {
        Node node = new Node();
        Thread serverThread  = new Thread(node.server);
        serverThread.start();
    }

    private class NodeListener implements Runnable
    {
        private boolean isRunning;
        private ServerSocket serverSocket;

        public NodeListener()
        {
            isRunning = true;
            try
            {
                serverSocket = new ServerSocket(peerInformation.getPort());
            }
            catch(IOException e)
            {
                writeDebug("IOException while trying to listen!", true);
            }
        }

        public void run()
        {
            Socket socket = null;
            ObjectInputStream in;
            while(isRunning)
            {
                try
                {
                    socket = serverSocket.accept();
                    in = new ObjectInputStream(socket.getInputStream());
                    deliver((Message)in.readObject());
                }
                catch(IOException e)
                {
                    writeDebug("IOException while trying to accept connection!", true);
                    e.printStackTrace();
                }
                catch(ClassNotFoundException e)
                {
                    writeDebug("ClassNotFoundException while trying to read Object!", true);
                }
                finally
                {
                    try
                    {
                        if(socket != null)
                            socket.close();
                    }
                    catch(Exception e){}
                }
            }
            try
            {
                if(serverSocket != null)
                    serverSocket.close();
            }
            catch(Exception e){}
        }

        public void kill()
        {
            isRunning = false;
        }
    }

}

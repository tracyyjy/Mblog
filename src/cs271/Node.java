package cs271;

import cs271.Messages.*;
import cs271.Messages.Message;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/15/13
 * Time: 11:34 AM
 * To change this template use File | Settings | File Templates.
 */

public class Node {

    // timeout per connection
    private static final int socketTimeout = 1000;

    // if a proposer doesn't hear back from a majority of acceptors, try again
    private static final int proposeTimeout = 10000;

    // Node Data
    private Map<Integer, String> Tweets;

    private Server server;
    private ServerSocket clientListener;
    private Agent peerListener;

    private Set<NodeInformation> cluster;
    private NodeInformation nodeInformation;
    private boolean isRunning;

    /* Proposer Variables */
    private int currentProposedPosition;
    // K-V for positions and their current proposed ballot number in this node
    private Map<Integer, Integer> currentProposedBallotNumbers;
    // K-V for positions and their current
    private Map<Integer, Integer> numPromises;
    private Map<Integer, Proposal> proposals;

    /* Acceptor Variables */
    // K-V for positions and their maximum ballot number this acceptor ever received
    private Map<Integer, Integer> receivedMaxBallotNumber;
    // K-V for positions and their proposals this acceptor ever accepted
    private Map<Integer, Proposal> acceptedProposals;

    // Learner Variables
    private Map<Integer, Integer> numAcceptances;
    private Map<Integer, String> chosenValues;

    public Node(int port, int siteNum) throws IOException{

        // initialize local and cluster information
        this.Tweets = new HashMap<Integer, String>();
        this.nodeInformation = new NodeInformation("localhost", port, siteNum);
        this.cluster = new HashSet<NodeInformation>();
        this.knowCluster();

        this.currentProposedPosition = 0;
        this.currentProposedBallotNumbers = new HashMap<Integer, Integer>();
        this.numPromises = new HashMap<Integer, Integer>();
        this.numAcceptances = new HashMap<Integer, Integer>();
        this.proposals = new HashMap<Integer, Proposal>();
        this.receivedMaxBallotNumber = new HashMap<Integer, Integer>();
        this.acceptedProposals = new HashMap<Integer, Proposal>();
        this.chosenValues = new HashMap<Integer, String>();
        this.isRunning = false;

        // register C/S server socket
        clientListener = new ServerSocket(9898);
        server = new Server(this, clientListener);
        server.start();

        // register inter-node communication socket
        peerListener = new Agent();
        peerListener.start();

        for (NodeInformation node: cluster){
            if (node == nodeInformation) continue;
        }

        isRunning = true;

        log("Node " + nodeInformation.getNum() + " started");
    }

    public void knowCluster() {
        for (int i = 0; i < 5; i++) {
            this.cluster.add(new NodeInformation("localhost", 9903 + i, i));
        }
    }

    public synchronized void fail() {
        /* failure simulation code ?
        *
        *
        *
        * */
    }

    public synchronized void recover() {
        /* recovery simulation code ?
        *
        *
        *
        * */
    }

    public void propose(String value) {
        currentProposedPosition = Tweets.size();
        propose(value, currentProposedPosition);
    }

    public void propose(String value, int position) {
        if(!isRunning)
            return;

        numPromises.put(position, 0);

        // increment the ballot number for a position
        int ballotNumber;
        if (!currentProposedBallotNumbers.containsKey(position)) ballotNumber = 0;
        else ballotNumber = currentProposedBallotNumbers.get(position) + 1;
        currentProposedBallotNumbers.put(currentProposedPosition, ballotNumber);

        Proposal proposal = new Proposal(position, ballotNumber, value);
        proposals.put(position, proposal);

        // send Phase1 prepare request to all with position & ballot number
        broadcast(new PrepareRequestMessage(position, ballotNumber));
    }

    private void broadcast(Message m) {
        if(!isRunning)
            return;

        m.setSender(nodeInformation);
        for(NodeInformation node : cluster)
        {
            // send to itself
            if(this.nodeInformation == node)
                checkout(m);

            // send to peer
            else
                unicast(node, m);
        }
    }

    private void unicast(NodeInformation node, Message m) {
        if(!isRunning)
            return;

        Socket socket = null;
        ObjectOutputStream out = null;
        m.setReceiver(node);

        try
        {
            socket = new Socket(node.getHost(), node.getPort());
            socket.setSoTimeout(socketTimeout);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(m);
            out.flush();
        }
        catch(ConnectException e)
        {
            writeDebug("Exception when unicasting to node" + node.getNum() + " (refused)", true);
        }
        catch(SocketTimeoutException e)
        {
            writeDebug("Exception when unicasting to node" + node.getNum() + " (timeout)", true);
        }
        catch(IOException e)
        {
            e.printStackTrace();
            writeDebug("IOException while trying to send message!", true);
        }
        finally
        {
            try
            {
                if(out != null)
                    out.close();
                if(socket != null)
                    socket.close();
            }
            catch(IOException e){}
        }
    }

    private synchronized void checkout(Message m) {
        if(!isRunning)
            return;

        // Acceptors process Phase1b
        if(m instanceof PrepareRequestMessage)
        {
            PrepareRequestMessage prepareRequest = (PrepareRequestMessage)m;
            int position = prepareRequest.getPosition();
            int ballotNumber = prepareRequest.getBallotNumber();
            boolean promised = false;
            boolean accepted = acceptedProposals.containsKey(position);

            writeDebug("Got Prepare Request from " + prepareRequest.getSender() + ": (" + position + ", "+ ballotNumber + ")");

            // if ballot number is higher than any received proposal for a given position, promise it
            if(!receivedMaxBallotNumber.containsKey(position) || receivedMaxBallotNumber.get(position) < ballotNumber){
                promised = true;
                receivedMaxBallotNumber.put(position , ballotNumber);
            }

            // if has accepted some proposal, respond with this proposal; otherwise respond with null
            PrepareResponseMessage prepareResponse = new PrepareResponseMessage(position, receivedMaxBallotNumber.get(position), promised, accepted, acceptedProposals.get(position));
            prepareResponse.setSender(nodeInformation);
            unicast(prepareRequest.getSender(), prepareResponse);
        }

        // Proposer processes Phase2a
        else if(m instanceof PrepareResponseMessage)
        {
            PrepareResponseMessage prepareResponse = (PrepareResponseMessage)m;
            Proposal acceptedProposal = prepareResponse.getProposal();
            int position = prepareResponse.getPosition();
            int ballotNumber = prepareResponse.getBallotNumber();
            Proposal proposal = proposals.get(position);
            boolean promised = prepareResponse.getPromised();
            boolean accepted = prepareResponse.getAccepted();
            int n = numPromises.get(position);

            writeDebug("Got Prepare Response from " + prepareResponse.getSender() + ": " + position + ", " + ballotNumber + ", " + (acceptedProposal == null ? "None" : acceptedProposal.toString()));

            // ignore if already heard from a majority
            if (n > (cluster.size() / 2))
                return;

            // if acceptor already promised something higher, use higher ballot number
            if(!promised) {
                int tmpBallotNumber = currentProposedBallotNumbers.get(position);
                while(tmpBallotNumber < ballotNumber)
                    tmpBallotNumber += cluster.size();
                currentProposedBallotNumbers.put(position, tmpBallotNumber);
                propose(proposal.getValue(), proposal.getPosition());
                return;
            }

            // if promised by a new acceptor, increment the counter
            n++;

            // if acceptors already accepted something (maybe different), always keep the one with highest ballot number
            if(accepted && acceptedProposal!=null) {
                if (acceptedProposal.getBallotNumber() > proposal.getBallotNumber())
                    proposal = acceptedProposal;
            }

            // if recently promised by a quorum
            if(n > (cluster.size() / 2)) {
                AcceptRequestMessage acceptRequest = new AcceptRequestMessage(position, proposal);
                broadcast(acceptRequest);
            }

            // record the new counter
            numPromises.put(position, n);
        }

        // Acceptors process Phase2b
        else if(m instanceof AcceptRequestMessage) {
            AcceptRequestMessage acceptRequest = (AcceptRequestMessage)m;
            Proposal requestedProposal = acceptRequest.getProposal();
            int position = requestedProposal.getPosition();
            int ballotNumber = requestedProposal.getBallotNumber();

            writeDebug("Got Accept Request from " + acceptRequest.getSender() + ": " + requestedProposal.toString());

            // if promised to higher a ballot number, ignore this proposal
            if(ballotNumber < receivedMaxBallotNumber.get(position))
                return;

            // otherwise "accept" the proposal, here one acceptor might update its max ballot number with some number raised other acceptors
            if(ballotNumber > receivedMaxBallotNumber.get(position))
                receivedMaxBallotNumber.put(position, ballotNumber);
            acceptedProposals.put(position, requestedProposal);

            writeDebug("Accepted: " + requestedProposal.toString());

            // Broadcast decision to all
            AcceptConfirmMessage acceptConfirmMessage = new AcceptConfirmMessage(position, requestedProposal);
            broadcast(acceptConfirmMessage);
        }

        // Proposers & Learners learn the decision
        else if(m instanceof AcceptConfirmMessage) {
            AcceptConfirmMessage acceptConfirmMessage = (AcceptConfirmMessage)m;
            Proposal acceptedProposal = acceptConfirmMessage.getProposal();
            int position = acceptedProposal.getPosition();

            // if first time a acceptance acquired
            if (numAcceptances.get(position) == null){
                numAcceptances.put(position,0);
            }
            int n = numAcceptances.get(position);

            writeDebug("Got Accept Notification from " + acceptConfirmMessage.getSender() + ": " + (acceptedProposal == null ? "None" : acceptedProposal.toString()));

            // ignore if already learned from a majority
            if (n > (cluster.size() / 2))
                return;

            n++;

            // if recently learned from a quorum
            if(n > (cluster.size() / 2))
            {
                chosenValues.put(position, acceptedProposal.getValue());
                writeDebug("Learned: " + acceptedProposal.getPosition() + ", " + acceptedProposal.getValue());
            }
            else
                numAcceptances.put(position, n);
        }
        else
            writeDebug("Unknown Message received", true);
    }

    private static void log(String message) {
        System.out.println(message);
    }

    private void writeDebug(String s) {
        writeDebug(s, false);
    }

    private synchronized void writeDebug(String s, boolean isError) {

        PrintStream out = isError ? System.err : System.out;
        out.print(toString());
        out.print(": ");
        out.println(s);
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);
        int siteNum = Integer.parseInt(args[1]);
        Node node = new Node(port, siteNum);

    }

    /* Agent class assumes the role of Proposer/Acceptor*/
    private class Agent extends Thread {
        private boolean isRunning;
        private ServerSocket serverSocket;

        public Agent()
        {
            isRunning = true;
            try
            {
                serverSocket = new ServerSocket(nodeInformation.getPort());
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
                    checkout((Message) in.readObject());
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

    private class Proposer extends Thread {
        private boolean isRunning;
        private long expireTime;
        private Proposal proposal;

        public Proposer(Proposal proposal)
        {
            this.isRunning = true;
            this.proposal = proposal;
        }

        public void run()
        {
            expireTime = System.currentTimeMillis() + proposeTimeout;
            while(isRunning)
            {
                if(expireTime < System.currentTimeMillis())
                {
                    propose(proposal.getValue(), proposal.getPosition());
                    kill();
                }
                yield(); // so the while loop doesn't spin too much
            }
        }

        public void kill()
        {
            isRunning = false;
        }

    }

}

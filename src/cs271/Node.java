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

    // constants for paxos modes
    private static final boolean multi = true;
    private static final boolean basic = false;

    // EC2 network set-up
    private static final Map<String, Integer> Hosts= new HashMap<String, Integer>();

    static{
//        Hosts.put("23.20.72.231", 0);
//        Hosts.put("50.112.9.1", 1);
//        Hosts.put("54.215.35.190", 2);
//        Hosts.put("54.247.12.88", 3);
//        Hosts.put("54.254.32.56", 4);
        Hosts.put("192.168.0.100", 0);
        Hosts.put("192.168.0.103", 1);
        Hosts.put("192.168.0.104", 2);
    }

    // timeout per connection
    private static final int socketTimeout = 1000;

    // if a proposer doesn't hear back from a majority of acceptors, try again
    private static final int proposeTimeout = 10000;

    // Node Data
    private TreeMap<Integer, String> Tweets;
    private ArrayList<String> GlobalTweets;

    private Server server;
    private ServerSocket clientListener;
    private Agent peerListener;

    private Set<NodeInformation> cluster;
    private NodeInformation nodeInformation;
    private boolean mode;
    private boolean alive;
    private boolean healthy;
    private int health;

    /* Proposer Variables */
    private int currentProposedPosition;
    // K-V for positions and their current proposed ballot number in this node
    private Map<Integer, Integer> currentProposedBallotNumbers;
    // K-V for positions and their current statistics
    private Map<Integer, Integer> numPromises;
    private Map<Integer, Proposal> proposals;
    private Map<Integer, ProposalHandler> handlers;
    private Map<Integer, Integer> maxAcceptedProposalBallotNumber;

    /* Acceptor Variables */
    // K-V for positions and their maximum ballot number this acceptor ever received
    private Map<Integer, Integer> receivedMaxBallotNumber;
    // K-V for positions and their proposals this acceptor ever accepted
    private Map<Integer, Proposal> acceptedProposals;

    /* Learner Variables */
    private Map<Integer, Integer> numAcceptances;
    private ArrayList<Map<Integer, Integer>> numIsolatedAcceptances;

    public Node(String host, boolean mode) throws IOException{

        // initialize local and cluster information
        this.mode = mode;

        this.Tweets = new TreeMap<Integer, String>();
        this.nodeInformation = new NodeInformation(host, 9905, Hosts.get(host));
        this.cluster = new HashSet<NodeInformation>();
        this.knowCluster();

        this.numIsolatedAcceptances = new ArrayList<Map<Integer, Integer>>();

        if (mode==multi){
            this.GlobalTweets = new ArrayList<String>();
            for (int i = 0; i < cluster.size(); i++){
                Map<Integer, Integer> tempMap = new HashMap<Integer, Integer>();
                numIsolatedAcceptances.add(tempMap);
            }
        }

        this.currentProposedPosition = 0;
        this.currentProposedBallotNumbers = new HashMap<Integer, Integer>();
        this.maxAcceptedProposalBallotNumber = new HashMap<Integer, Integer>();
        this.numPromises = new HashMap<Integer, Integer>();
        this.numAcceptances = new HashMap<Integer, Integer>();
        this.proposals = new HashMap<Integer, Proposal>();
        this.handlers = new HashMap<Integer, ProposalHandler>();
        this.receivedMaxBallotNumber = new HashMap<Integer, Integer>();
        this.acceptedProposals = new HashMap<Integer, Proposal>();
        this.healthy = false;
        this.alive = false;

        // register C/S server socket
        clientListener = new ServerSocket(9900 /*+ nodeInformation.getNodeId()*/);
        server = new Server(this, clientListener);
        server.start();

        // register inter-node communication socket
        peerListener = new Agent();
        Thread test = new Thread(peerListener);
        test.start();

        alive = true;
        if (mode==multi){
            this.health = cluster.size();
        }
        healthy = true;


        log("Node " + nodeInformation.getNodeId() + " started, multi-paxos: " + mode);
    }

    public synchronized boolean getStatus(){
        return healthy;
    }

    public synchronized void fail() {
        /* failure simulation */
        healthy = false;
        if (mode==multi){
            this.health = 0;
        }
        alive = false;
        log("**********FAILING**********");
    }

    public synchronized void recover() {
        /* recovery simulation */
        alive = true;
        if (!healthy){
            if (mode == multi){
                health = 0;
                GlobalTweets.clear();
                for (Map.Entry entry: Tweets.entrySet()){
                    GlobalTweets.add("From node " + nodeInformation.getNodeId() + " with " + (String) entry.getValue());
                }
                health++;
            }
            broadcast(new SosMessage());
        }
    }

    public String getTweets(){
        String string;
        string = new String();
        for (Map.Entry entry: Tweets.entrySet()){
            String tmp = entry.getKey() + ": " + entry.getValue() + "\n";
            string += tmp;
        }
        if (mode == multi){
            string += "Global Stream:\n" + getGlobalTweets();
            string = "Local Stream:\n" + string;
        }
        return string;
    }

    public String getGlobalTweets(){
        String string = new String();
        for (String tweet: GlobalTweets){
            String tmp = tweet + "\n";
            string += tmp;
        }
        return string;
    }

    public void propose(String value) {
        currentProposedPosition = Tweets.size();
        propose(value, currentProposedPosition);
    }

    public void propose(String value, int position) {

        if(!alive)
            return;

        numPromises.put(position, 0);

        // increment the ballot number for a position
        int ballotNumber;
        if (!currentProposedBallotNumbers.containsKey(position))
            ballotNumber = 0;
        else
            ballotNumber = currentProposedBallotNumbers.get(position) + 1;
        currentProposedBallotNumbers.put(currentProposedPosition, ballotNumber);

        // remove possible legacy handler
        if(handlers.containsKey(position)) {
            handlers.remove(position).suicide();
        }
        // create new proposal and its handler
        Proposal proposal = new Proposal(nodeInformation.getNodeId(), position, ballotNumber, value);
        proposals.put(position, proposal);
        ProposalHandler handler = new ProposalHandler(proposal);
        handler.start();
        handlers.put(position, handler);

        // in Basic Paxos, send Phase1 prepare request to all with position & ballot number
        if (mode==basic)
            broadcast(new PrepareRequestMessage(position, ballotNumber));
        // in Multi-Paxos, send Phase2 accept request to all with position and proposal
        else
            broadcast(new AcceptRequestMessage(position, proposal));

        writeDebug("Sent Prepare Request to Acceptors: " + ", position: " + position + ", ballot number: " + ballotNumber );
    }

    private void knowCluster() {
        for (Map.Entry entry: Hosts.entrySet()) {
            this.cluster.add(new NodeInformation((String) entry.getKey(), 9905, (Integer) entry.getValue()));
        }
        log(cluster.size()+ " nodes in cluster. Node " + nodeInformation.getNodeId() + " at " + nodeInformation.getHost() + ": " + nodeInformation.getPort());
    }

    private void broadcast(Message m) {
        if(!alive)
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
        if(!alive)
            return;

        Socket socket = null;
        ObjectOutputStream out = null;
        m.setReceiver(node);

        try
        {
            socket = new Socket(node.getHost(), node.getPort());
            log("socket connected!");
            socket.setSoTimeout(socketTimeout);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(m);
            out.flush();
        }
        catch(ConnectException e)
        {
            writeDebug("Exception when unicasting to node" + node.getNodeId() + " (refused)", true);
        }
        catch(SocketTimeoutException e)
        {
            writeDebug("Exception when unicasting to node" + node.getNodeId() + " (timeout)", true);
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
            catch(IOException e){

            }
        }
    }

    private synchronized void checkout(Message m) {
        if(!alive)
            return;

        // receive sos message from a failed peer
        if (m instanceof SosMessage){
            SosMessage sosMessage = (SosMessage)m;
            if (healthy && !sosMessage.getHealthy()){
                SupportMessage supportMessage = new SupportMessage(healthy, Tweets);
                supportMessage.setSender(nodeInformation);
                unicast(sosMessage.getSender(), supportMessage);
            }
        }

        // receive support message from a healthy peer
        else if (m instanceof SupportMessage){
            SupportMessage supportMessage = (SupportMessage)m;
            if (mode==basic){
                if (!healthy && supportMessage.getHealthy()){
                    Tweets = new TreeMap<Integer, String>(supportMessage.getTweets());
                    healthy = true;
                    log("**********Recovering********** Healthy: " + String.valueOf(healthy));
                }
            }
            else {
                if (!healthy && supportMessage.getHealthy()){
                    for (Map.Entry entry: supportMessage.getTweets().entrySet()){
                        GlobalTweets.add("From node " + supportMessage.getSender().getNodeId() + " with " + (String) entry.getValue());
                    }
                    health++;
                    healthy = (health==cluster.size());
                    log("**********Recovering********** Health: " + String.valueOf(health) + " Healthy: " + String.valueOf(healthy));
                }
            }
        }

        // Acceptors process Phase1b
        else if(m instanceof PrepareRequestMessage)
        {
            PrepareRequestMessage prepareRequest = (PrepareRequestMessage)m;
            int position = prepareRequest.getPosition();
            int ballotNumber = prepareRequest.getBallotNumber();
            boolean promised = false;
            boolean accepted = acceptedProposals.containsKey(position);

            writeDebug("Got Prepare Request from Proposer: " + prepareRequest.getSender().getNodeId() + ", position: " + position + ", ballot number: "+ ballotNumber + "");

            // if ballot number is higher than any received proposal for a given position, promise it
            if(!receivedMaxBallotNumber.containsKey(position) || receivedMaxBallotNumber.get(position) < ballotNumber){
                promised = true;
                receivedMaxBallotNumber.put(position , ballotNumber);
            }

            // if has accepted some proposal, respond with this proposal; otherwise respond with null
            PrepareResponseMessage prepareResponse = new PrepareResponseMessage(position, receivedMaxBallotNumber.get(position), promised, accepted, acceptedProposals.get(position));
            prepareResponse.setSender(nodeInformation);
            prepareResponse.setReceiver(prepareRequest.getSender());
            unicast(prepareRequest.getSender(), prepareResponse);

            writeDebug("Sent Prepare Response to Acceptor: " + prepareResponse.getReceiver().getNodeId() + ", promised?:" + promised + ", position: " + position + ", required ballot number: " + ballotNumber + ", accepted value: " + (acceptedProposals.get(position) == null ? "None" : acceptedProposals.get(position).toString()));
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

            writeDebug("Got Prepare Response from Acceptor: " + prepareResponse.getSender().getNodeId() + ", promised?:" + promised + ", position: " + position + ", required ballot number: " + ballotNumber + ", accepted value: " + (acceptedProposal == null ? "None" : acceptedProposal.toString()));

            // ignore if already heard from a quorum
            if (n > (cluster.size() / 2))
                return;

            // if acceptor already promised something equal or higher, use higher ballot number
            if(!promised) {
                int tmpBallotNumber = currentProposedBallotNumbers.get(position);
                while(tmpBallotNumber < ballotNumber)
                    tmpBallotNumber += cluster.size();
                currentProposedBallotNumbers.put(position, tmpBallotNumber);
                propose(proposal.getValue(), proposal.getPosition());
                log( "REPROPOSED!");

                return;
            }

            // if promised by a new acceptor, increment the counter
            n++;

            // if acceptors already accepted something (maybe different), always keep the one with highest ballot number
            if(accepted && acceptedProposal!=null) {
                if (!maxAcceptedProposalBallotNumber.containsKey(position)){
                    maxAcceptedProposalBallotNumber.put(position,-1);
                }
                if (acceptedProposal.getBallotNumber() > maxAcceptedProposalBallotNumber.get(position)){
                    proposal.setValue(acceptedProposal.getValue());
                    maxAcceptedProposalBallotNumber.put(position, proposal.getBallotNumber());
                }
            }

            // if recently promised by a quorum
            if(n > (cluster.size() / 2)) {
                AcceptRequestMessage acceptRequest = new AcceptRequestMessage(position, proposal);
                acceptRequest.setSender(nodeInformation);
                broadcast(acceptRequest);
                writeDebug("Sent Accept Request to Proposers: " + ", proposal: " + proposal.toString());
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

            writeDebug("Got Accept Request from Proposer: " + acceptRequest.getSender().getNodeId() + ", proposal: " + requestedProposal.toString());

            if (mode == basic){
                // if promised to higher ballot number, ignore this proposal
                if(ballotNumber < receivedMaxBallotNumber.get(position))
                    return;

                // otherwise "accept" the proposal, here one acceptor might update its max ballot number with some number raised by other acceptors
                //                    receivedMaxBallotNumber.put(position, ballotNumber);
                acceptedProposals.put(position, requestedProposal);
                Tweets.put(requestedProposal.getPosition(), requestedProposal.getValue());
            }
            else {
                // verification for leadership
                if (requestedProposal.getProposerNumber()!=m.getSender().getNodeId())
                    return;
            };

            writeDebug("Accepted proposal:" + requestedProposal.toString());

            // Broadcast decision to all
            AcceptConfirmMessage acceptConfirmMessage = new AcceptConfirmMessage(position, requestedProposal);
            acceptConfirmMessage.setSender(nodeInformation);
            //broadcast(acceptConfirmMessage);
            unicast(m.getSender(),acceptConfirmMessage);
            writeDebug("Sent Accept Confirm to " + m.getSender() + ": " + (requestedProposal == null ? "None" : requestedProposal.toString()));
        }

        // Proposers learn the decision
        else if(m instanceof AcceptConfirmMessage) {
            AcceptConfirmMessage acceptConfirmMessage = (AcceptConfirmMessage)m;
            Proposal acceptedProposal = acceptConfirmMessage.getProposal();
            int position = acceptedProposal.getPosition();
            int proposerId = acceptedProposal.getProposerNumber();
            Map<Integer, Integer> acceptanceList;

            if (mode==basic){
                acceptanceList = numAcceptances;
            }
            else {
                acceptanceList = (HashMap<Integer, Integer>) (numIsolatedAcceptances.get(proposerId));
            }

            // if first time an acceptance acquired
            if (acceptanceList.get(position) == null){
                acceptanceList.put(position, 0);
            }

            writeDebug("Got Accept Confirm from " + acceptConfirmMessage.getSender() + ": " + (acceptedProposal == null ? "None" : acceptedProposal.toString()));

            int n = acceptanceList.get(position);

            // ignore if already learned from a quorum
            if (n > (cluster.size() / 2)){
                return;
            }

            log("before " + Integer.toString(n));
            n++;
            log("after " + Integer.toString(n));

            // if recently learned from a quorum
            if(n > (cluster.size() / 2)) {
                writeDebug("Learned: " + n + " " + acceptedProposal.getPosition() + ", " + acceptedProposal.getValue());
                if(nodeInformation.getNodeId()==acceptedProposal.getProposerNumber()){
                    server.sendToClient("value accepted by " + n);
                    Tweets.put(acceptedProposal.getPosition(), acceptedProposal.getValue());
                }
                else{
                    if(mode==basic){
                        Tweets.put(acceptedProposal.getPosition(), acceptedProposal.getValue());
                    }
                }
                if (mode==multi){
                    GlobalTweets.add("From node " + proposerId + " with " + acceptedProposal.getValue());
                }
            }
            acceptanceList.put(position, n);

            log("finally " + Integer.toString(n));
            log("finally " + acceptanceList.get(position));
            log("finally " + numAcceptances.get(position));
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
        boolean mode = basic;
        String host = args[0];
        if (args.length > 1){
            if (args[1].equals("multi")) mode = multi;
        }
        Node node = new Node(host, mode);
    }

    /* Agent class assumes the role of Proposer/Acceptor*/
    private class Agent implements Runnable {
        private boolean isRunning;
        private ServerSocket serverSocket;

        public Agent() {
            isRunning = true;
            try {
                serverSocket = new ServerSocket(nodeInformation.getPort());
            } catch(IOException e) {
                writeDebug("IOException while trying to listen!", true);
            }
        }

        public void run() {
            Socket socket = null;
            ObjectInputStream in;
            while(isRunning) {
                try {
                    socket = serverSocket.accept();
                    in = new ObjectInputStream(socket.getInputStream());
                    checkout((Message) in.readObject());
                } catch(IOException e)
                {
                    writeDebug("IOException while trying to accept connection!", true);
                    e.printStackTrace();
                } catch(ClassNotFoundException e) {
                    writeDebug("ClassNotFoundException while trying to read Object!", true);
                } finally {
                    try {
                        if(socket != null)
                            socket.close();
                    } catch(Exception e){}
                }
            } try {
                if(serverSocket != null)
                    serverSocket.close();
            } catch(Exception e){}
        }

        public void kill() {
            isRunning = false;
        }
    }

    /* Handler handles when a proposal reaches timeout*/
    private class ProposalHandler extends Thread {

        private boolean alive;
        private long expireTime;
        private Proposal proposal;

        public ProposalHandler(Proposal proposal) {
            this.alive = true;
            this.proposal = proposal;
        }

        public void run() {
            expireTime = System.currentTimeMillis() + proposeTimeout;
            while(alive) {
                if(expireTime < System.currentTimeMillis()) {
                    if (!Tweets.containsKey(proposal.getPosition())){
                        server.sendToClient("Timeout, not accepted!");
                    }
                    suicide();
                }
                yield(); // so the while loop doesn't spin too much
            }
        }

        public void suicide() {
            alive = false;
        }
    }
}

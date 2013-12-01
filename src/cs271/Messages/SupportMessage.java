package cs271.Messages;

import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/30/13
 * Time: 10:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class SupportMessage extends Message {

    private boolean healthy;
    private TreeMap<Integer, String> tweets;

    public SupportMessage(boolean healthy, TreeMap<Integer, String> tweets)
    {
        this.healthy = healthy;
        this.tweets = tweets;
    }

    public boolean getHealthy(){
        return healthy;
    }

    public TreeMap<Integer, String> getTweets(){
        return tweets;
    }
}

package cs271;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/24/13
 * Time: 11:35 PM
 * To change this template use File | Settings | File Templates.
 */
import java.io.Serializable;

public class PeerInformation implements Serializable
{
    private String host;
    private int port;
    private int num;
    private boolean isLeader;

    public PeerInformation(String host, int port, int num)
    {
        this.host = host;
        this.port = port;
        this.num = num;
        this.isLeader = false;
    }

    public void becomeLeader()
    {
        isLeader = true;
    }

    public void becomeNonLeader()
    {
        isLeader = false;
    }

    public boolean isLeader()
    {
        return isLeader;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public int getNum()
    {
        return num;
    }

    public String toString()
    {
        return ((Integer)num).toString();
    }
}
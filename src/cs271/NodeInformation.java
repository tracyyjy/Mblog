package cs271;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/24/13
 * Time: 11:35 PM
 * To change this template use File | Settings | File Templates.
 */

public class NodeInformation implements Serializable
{
    private String host;
    private int port;
    private int num;

    public NodeInformation(String host, int port, int num)
    {
        this.host = host;
        this.port = port;
        this.num = num;
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
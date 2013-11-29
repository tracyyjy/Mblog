package cs271.Messages;

import cs271.NodeInformation;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/25/13
 * Time: 12:27 AM
 * To change this template use File | Settings | File Templates.
 */

public abstract class Message implements Serializable
{
    protected int position;
    protected NodeInformation sender;
    protected NodeInformation receiver;

    public int getPosition()
    {
        return position;
    }

    public NodeInformation getSender()
    {
        return sender;
    }

    public void setSender(NodeInformation sender)
    {
        this.sender = sender;
    }

    public NodeInformation getReceiver()
    {
        return receiver;
    }

    public void setReceiver(NodeInformation receiver)
    {
        this.receiver = receiver;
    }
}

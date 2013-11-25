package cs271.Messages;

import cs271.PeerInformation;

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
    protected PeerInformation sender;
    protected PeerInformation receiver;

    public PeerInformation getSender()
    {
        return sender;
    }

    public void setSender(PeerInformation sender)
    {
        this.sender = sender;
    }

    public PeerInformation getReceiver()
    {
        return receiver;
    }

    public void setReceiver(PeerInformation receiver)
    {
        this.receiver = receiver;
    }
}

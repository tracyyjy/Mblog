package cs271.Messages;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/25/13
 * Time: 12:32 AM
 * To change this template use File | Settings | File Templates.
 */

public class Proposal implements Serializable
{
    private int BallotNumber;
    private int key;
    private String value;

    public Proposal(int BallotNumber, int key, String value)
    {
        this.BallotNumber = BallotNumber;
        this.key = key;
        this.value = value;
    }

    public int getBallotNumber()
    {
        return BallotNumber;
    }

    public int getKey()
    {
        return key;
    }

    public String getValue()
    {
        return value;
    }

    public String toString()
    {
        return "{" + BallotNumber + ", " + key + ", " + value + "}";
    }
}


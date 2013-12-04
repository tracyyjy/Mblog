package cs271;

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
    private int proposerNumber;
    private int position;
    private int BallotNumber;
    private String value;

    public Proposal(int proposerNumber, int position, int BallotNumber, String value)
    {
        this.proposerNumber = proposerNumber;
        this.position = position;
        this.BallotNumber = BallotNumber;
        this.value = value;
    }

    public int getProposerNumber(){
        return proposerNumber;
    }

    public int getPosition()
    {
        return position;
    }

    public int getBallotNumber()
    {
        return BallotNumber;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value){
        this.value =value;
    }

    public String toString()
    {
        return "{" + position + ", " + BallotNumber + ", " + value + "}";
    }
}


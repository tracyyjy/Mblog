package cs271.Messages;

import cs271.Proposal;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/25/13
 * Time: 12:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class PrepareResponseMessage extends Message
{
    private int BallotNumber;
    private Proposal proposal;
    private boolean promised;
    private boolean accepted;

    public PrepareResponseMessage(int position, int BallotNumber, boolean promised, boolean accepted, Proposal proposal)
    {
        this.position = position;
        this.proposal = proposal;
        this.BallotNumber = BallotNumber;
        this.promised = promised;
        this.accepted = accepted;
    }

    public Proposal getProposal()
    {
        return proposal;
    }

    public int getBallotNumber()
    {
        return BallotNumber;
    }

    public boolean getPromised()
    {
        return promised;
    }

    public boolean getAccepted()
    {
        return accepted;
    }
}

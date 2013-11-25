package cs271.Messages;

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

    public PrepareResponseMessage(int BallotNumber, Proposal proposal)
    {
        this.proposal = proposal;
        this.BallotNumber = BallotNumber;
    }

    public Proposal getProposal()
    {
        return proposal;
    }

    public int getBallotNumber()
    {
        return BallotNumber;
    }
}

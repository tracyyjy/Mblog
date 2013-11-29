package cs271.Messages;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/25/13
 * Time: 12:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class AcceptRequestMessage extends Message
{
    private Proposal proposal;

    public AcceptRequestMessage(int position, Proposal proposal)
    {
        this.position = position;
        this.proposal = proposal;
    }

    public Proposal getProposal()
    {
        return proposal;
    }
}


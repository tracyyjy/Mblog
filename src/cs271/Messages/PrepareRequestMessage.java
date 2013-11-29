package cs271.Messages;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/25/13
 * Time: 12:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class PrepareRequestMessage extends Message
{
    private int BallotNumber;

    public PrepareRequestMessage(int round, int BallotNumber)
    {
        this.position = round;
        this.BallotNumber = BallotNumber;
    }

    public int getBallotNumber()
    {
        return BallotNumber;
    }
}

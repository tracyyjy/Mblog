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
    private int csn;
    private int psn;

    public PrepareRequestMessage(int csn, int psn)
    {
        this.csn = csn;
        this.psn = psn;
    }

    public int getPsn()
    {
        return psn;
    }

    public int getCsn()
    {
        return csn;
    }
}

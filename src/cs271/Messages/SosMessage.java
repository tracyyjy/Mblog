package cs271.Messages;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/30/13
 * Time: 10:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class SosMessage extends Message{

    private boolean healthy;

    public SosMessage()
    {
        this.healthy = false;
    }

    public boolean getHealthy(){
        return healthy;
    }
}

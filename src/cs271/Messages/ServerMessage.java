package cs271.Messages;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/29/13
 * Time: 10:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerMessage implements Serializable {

    private String message;

    public ServerMessage (String message){
        this.message = message;
    }

    public String getMessage (){
        return message;
    }
}

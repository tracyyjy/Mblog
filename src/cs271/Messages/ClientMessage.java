package cs271.Messages;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Photeinis
 * Date: 11/29/13
 * Time: 10:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClientMessage implements Serializable {
    private String function;
    private String argument;

    public ClientMessage(String function, String argument){
        this.function = function;
        this.argument = argument;
    }

    public String getFunction(){
        return function;
    }

    public String getArgument(){
        return argument;
    }
}

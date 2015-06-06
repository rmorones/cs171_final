package cs171_final;

import java.io.Serializable;
import java.util.Map;

/**
 *
 * @author Ricardo Morones <rmorones@umail.ucsb.edu>
 * @author Chris Kim <chriskim06@gmail.com>
 */
public class PaxosObj implements Serializable {
    private String command;
    private Pair ballot_num;
    private Pair accept_num;
    private String accept_val;
    private Integer senderId;
    private String commandLine;
    private Integer round;
    private Map<Integer, PaxosObj> log;
    
    
    public PaxosObj(String command, Pair ballot_num, Pair accept_num, String msg, Integer senderId, Integer round) {
        this.command = command;
        this.accept_num = accept_num;
        this.ballot_num = ballot_num;
        this.accept_val = msg;
        this.senderId = senderId;
        this.commandLine = null;
        this.round = round;
    }

    //Client thread constructor
    public PaxosObj(String command, String commandLine) {
        this.command = command;
        this.commandLine = commandLine;
        this.ballot_num = null;
        this.accept_num = null;
        this.accept_val = null;
        this.senderId = null;
        this.round = null;
    }

    public PaxosObj(PaxosObj copy) {
        this.command = copy.command;
        this.ballot_num = copy.ballot_num;
        this.accept_num = copy.accept_num;
        this.accept_val = copy.accept_val;
        this.senderId = copy.senderId;
        this.commandLine = copy.commandLine;
        this.round = copy.round;
    }
    
    public void setLog(Map<Integer, PaxosObj> log) {
        this.log = log;
    }
    
    public void setCommandLine(String cmd) {
        this.commandLine = cmd;
    }
    
    public String getCmdLine() {
        return commandLine;
    }
    
    public String getAcceptedValue() {
        return accept_val;
    }
    
    public Integer getRound() {
        return round;
    }

    public String getCommand() {
        return command;
    }

    public Pair getBallot_num() {
        return ballot_num;
    }

    public Pair getAccept_num() {
        return accept_num;
    }

    public Integer getSenderId() {
        return senderId;
    }
    
    public void setSenderId(int id) {
        this.senderId = id;
    }
    
    public String[] getCommandLine() {
        String[] parsed = new String[3];
        String dummy = commandLine;
        int secondlast;
        int lastspace = dummy.lastIndexOf(" ");
        
        parsed[2] = dummy.substring(lastspace + 1);
        dummy = dummy.substring(0, lastspace);
        secondlast = dummy.lastIndexOf(" ");
        parsed[1] = dummy.substring(secondlast + 1);
        dummy = dummy.substring(0, secondlast);
        parsed[0] = dummy;
        
        return parsed;
    }
    
}

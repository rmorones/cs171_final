package cs171_final;

/**
 *
 * @author Ricardo Morones <rmorones@umail.ucsb.edu>
 * @author Chris Kim <chriskim06@gmail.com>
 */
public class PaxosObj {
    private String command;
    private Pair ballot_num;
    private Pair accept_num;
    private String accept_val;
    private Integer senderId;
    private String commandLine;
    private Integer round;
    
    
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
    
    public void setCommandLine(String cmd) {
        this.commandLine = cmd;
    }
    
    public String getAcceptedValue() {
        return commandLine;
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

    public String getAccept_val() {
        return accept_val;
    }

    public Integer getSenderId() {
        return senderId;
    }
    
    public String[] getCommandLine() {
        String[] parsed = new String[3];
        int second;
        int space = commandLine.indexOf(" ");
        if (commandLine.startsWith("post")) {
            int first = commandLine.indexOf(" ", space + 1);
            second = commandLine.indexOf(" ", first + 1);
            parsed[0] = commandLine.substring(0, first);
            parsed[1] = commandLine.substring(first + 1, second);
        } else {
            second = commandLine.indexOf(" ", space + 1);
            parsed[0] = commandLine.substring(0, space);
            parsed[1] = commandLine.substring(space + 1, second);
        }
        parsed[2] = commandLine.substring(second + 1);
        return parsed;
    }
    
}

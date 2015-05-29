package cs171_final;

/**
 *
 * @author Ricardo Morones <rmorones@umail.ucsb.edu>
 * @author Chris Kim <chriskim06@gmail.com>
 */
public class PaxosObj {
    private final String command;
    private final Pair ballot_num;
    private final Pair accept_num;
    private final String accept_val;
    private final Integer senderId;
    private final boolean leader;
    private final String commandLine;
    
    
    public PaxosObj(String command, Pair ballot_num, Pair accept_num, String msg, Integer senderId, boolean leader) {
        this.command = command;
        this.accept_num = accept_num;
        this.ballot_num = ballot_num;
        this.accept_val = msg;
        this.senderId = senderId;
        this.leader = leader;
        this.commandLine = null;
    }

    //Client thread constructor
    public PaxosObj(String command, String commandLine) {
        this.command = command;
        this.commandLine = commandLine;
        this.ballot_num = null;
        this.accept_num = null;
        this.accept_val = null;
        this.senderId = null;
        this.leader = false;
    }
    
    public PaxosObj() {
        this.command = null;
        this.ballot_num = null;
        this.accept_num = null;
        this.accept_val = null;
        this.senderId = null;
        this.leader = false;
        this.commandLine = null;
    }
    
    public boolean isLeader() {
        return leader;
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
}

package cs171_final;

import java.io.Serializable;

/**
 *
 * @author Ricardo Morones <rmorones@umail.ucsb.edu>
 * @author Chris Kim <chriskim06@gmail.com>
 */

/*
 * To be instantiated when a request must be made 
 */

public class Paxos {
    @SuppressWarnings("FieldMayBeFinal")
    private class Pair<X, Y> implements Serializable {
        private X first;
        private Y second;
        
        public Pair(X first, Y second) {
            this.first = first;
            this.second = second;
        }
    }
    
    private final Pair ballot_num;
    private final Pair accept_num;
    private String accept_val;
    private final Integer id;
    private final String message;
    
    
    public Paxos(String message, Integer id) {
        this.id = id;
        this.ballot_num = new Pair(0, id);
        System.out.println(ballot_num.first.toString());
        ballot_num.first = 0;
        this.accept_num = new Pair(0, 0);
        this.message = message;
        this.accept_val = null;
    }
    
    public void request(Integer leaderId) {
        //tell leader to send request, if can't then request will random leader elected
        //prepare + ack
        
        
    }
    
    public void accept() {
        //accept, once majority is received: 2 + myself then okay to send decide message to everyone
        //accept + ack
    }
    
    public void decide() {
        //send decide v message to everyone
    }
}

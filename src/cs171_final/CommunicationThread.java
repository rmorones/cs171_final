package cs171_final;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author Ricardo Morones <rmorones@umail.ucsb.edu>
 * @author Chris Kim <chriskim06@gmail.com>
 */
public class CommunicationThread extends Thread {
    private final int port;
    private final Site site;
    private boolean failed;
    private final Queue<PaxosObj> missed;
    private boolean leader; //need to figure out where leader whould be changed
    private int round;
    private String myAcceptVal;
    private Pair myBallotNum;
    private Pair myAcceptNum;
    private final ArrayList<PaxosObj> pMajority;
    private final ArrayList<Pair> aMajority;
    private final ArrayList<String> log;
    
    public CommunicationThread(int port, Site site) {
        this.site = site;
        this.port = port;
        this.failed = false;
        this.missed = new LinkedList<>();
        this.leader = false;
        this.round = 0;
        this.pMajority = new ArrayList<>();
        this.aMajority = new ArrayList<>();
        this.myAcceptNum = new Pair(0, 0);
        this.myBallotNum = new Pair(0, site.siteId);
        this.myAcceptVal = null;
        this.log = new ArrayList<>();
//        if(site.siteId == 0)
//            this.leader = true;
    }
    
    public void toggleMode() {
        failed = !failed;
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void run() {
        ServerSocket serverSocket;
        Socket mysocket;
        ObjectInputStream in;
        
        try {
          serverSocket = new ServerSocket(port);
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }
        
         // Infinite loop, processes a single client connection at a time
        while (true) {
            try {
                // Wait for a client to connect (blocking)
                mysocket = serverSocket.accept();
                in = new ObjectInputStream(mysocket.getInputStream());
                PaxosObj input = (PaxosObj)in.readObject();
                
                if (input == null) {
                    System.out.println("object is null");
                    continue;
                }

                if(failed) {
                    missed.add(input);
                } else {
                    //catch up then process the new command
                    while(!missed.isEmpty()) {
                        PaxosObj i = missed.poll();
                        execute(i);
                    }
                    execute(input);
                }
            }
            catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void execute(PaxosObj input) throws IOException {
        
        switch (input.getCommand()) {
            case "fail": {
                failed = true;
                break;
            }
            case "restore": {
                failed = false;
                break;
            }
            case "ack prepare": {
                if(round == input.getRound()) {
                    pMajority.add(input);
                    if(pMajority.size() == 2) {
                        //send accept messages
                        boolean set = false;
                        for(PaxosObj p : pMajority) {
                            if(p.getAccept_val() != null) {
                                set = true;
                            }
                        }
                        if(myAcceptVal != null) {
                            set = true;
                        }
                        
                        if(set) {
                            if(compareBallot(pMajority.get(0).getBallot_num(), myBallotNum) && compareBallot(pMajority.get(0).getBallot_num(), pMajority.get(1).getBallot_num())) {
                                myAcceptVal = pMajority.get(0).getAccept_val();
                            } else if(compareBallot(pMajority.get(1).getBallot_num(), myBallotNum) && compareBallot(pMajority.get(1).getBallot_num(), pMajority.get(0).getBallot_num())) {
                                myAcceptVal = pMajority.get(1).getAccept_val();
                            }
                         }
                        //make list empty again
                        for(int i = 0; i < pMajority.size(); ++i)
                            pMajority.remove(i);
                        myAcceptNum = myBallotNum;
                        accept(myAcceptVal);
                    }
                }
                break;
            }
            case "request": {
                //if leader then just send accepts, else, send prepare messages
                //POST msg/READ ipAddr port
                String msg = input.getCommandLine()[0];
                if(leader) {
                    if(msg.startsWith("post")) {
                        msg = msg.substring(msg.indexOf(" ") + 1);
                        ++myBallotNum.first;
                        myAcceptNum = myBallotNum;
                        accept(msg);
                    } else {
                        //handle read
                    }
                } else {
                    if(msg.startsWith("post")) {
                        msg = msg.substring(msg.indexOf(" ") + 1);
                        prepare(msg);
                    } else { 
                        //handle read
                    }
                }
                break;
            }
            case "prepare": {
                //reply with ack
                if(compareBallot(input.getBallot_num(), myBallotNum)) {
                    send_ack_prepare(input);
                } else {
                    //do nothing?
                }
                break;
            }
            case "accept": {
                //reply with ack
                //have to do this only the first time
                //after receiving two accepts(majority) then send decide
                if(compareBallot(input.getBallot_num(), myBallotNum)) {
                    myAcceptNum = input.getBallot_num();
                    accept(input.getAccept_val());
                }
                break;
            }
            case "decide": {
                //accept value & increase round
                myAcceptNum = input.getAccept_num();
                myAcceptVal = input.getAccept_val();
                //insert value into log for round i where i is the index
                log.add(round, myAcceptVal); //might have to preallocate some null slots for this to work all the time
                ++round;
                break;
            }
            default:
                System.out.println("Error, input.command not defined");
                break;
        }
    }
    
    private void accept(String msg) throws IOException {
        myAcceptVal = msg;
        if(!aMajority.contains(myAcceptNum)) {
            aMajority.add(myAcceptNum);
            for(int i = 0; i < site.siteIPList.length; ++i) {
                if(site.siteId != i) { //don't send to yourself
                    Socket mysocket;
                    mysocket= new Socket(site.siteIPList[i], 5232);
                    ObjectOutputStream out;
                    out = new ObjectOutputStream(mysocket.getOutputStream());
                    PaxosObj outObj = new PaxosObj("accept", myAcceptNum, null, myAcceptVal, site.siteId, round);
                    out.writeObject(outObj);
                    out.flush();
                    out.close();
                    mysocket.close();
                }
            }
        } else { //there is already one in list and we just received the second one(majority reached)
            aMajority.remove(myAcceptNum);
            decide(myAcceptVal);
        }
    }
    
    private void prepare(String msg) throws IOException {
        ++myBallotNum.first;
        for(int i = 0; i < site.siteIPList.length; ++i) {
            if(site.siteId != i) { //don't send to yourself
                Socket mysocket;
                mysocket= new Socket(site.siteIPList[i], 5232);
                ObjectOutputStream out;
                out = new ObjectOutputStream(mysocket.getOutputStream());
                PaxosObj outObj = new PaxosObj("prepare", myBallotNum, null, msg, site.siteId, round);
                out.writeObject(outObj);
                out.flush();
                out.close();
                mysocket.close();
            }
        }
    }

    private void decide(String accept_val) throws IOException {
        for(int i = 0; i < site.siteIPList.length; ++i) {
            if(site.siteId != i) { //don't send to yourself
                Socket mysocket;
                mysocket= new Socket(site.siteIPList[i], 5232);
                ObjectOutputStream out;
                out = new ObjectOutputStream(mysocket.getOutputStream());
                PaxosObj outObj = new PaxosObj("decide", null, myAcceptNum, myAcceptVal, site.siteId, round);
                out.writeObject(outObj);
                out.flush();
                out.close();
                mysocket.close();
            }
        } 
    }

    private void send_ack_prepare(PaxosObj input) throws IOException {
        myBallotNum = input.getBallot_num(); //not sure if this works, might have to overwrite the '=' operator
        Socket mysocket;
        mysocket= new Socket(site.siteIPList[input.getSenderId()], 5232);
        ObjectOutputStream out;
        out = new ObjectOutputStream(mysocket.getOutputStream());
        PaxosObj outObj = new PaxosObj("ack prepare", myBallotNum, myAcceptNum, myAcceptVal, site.siteId, round);
        out.writeObject(outObj);
        out.flush();
        out.close();
        mysocket.close();
    }
 
    /*
     * Returns true if a >= b, where both are ballot numbers 
     */
    private boolean compareBallot(Pair a, Pair b) {
        if(a == null || b == null) {
            return false;
        }
        if(a.first == null || b.first == null || a.second == null || b.second == null) {
            return false;
        }
        
        if(a.first > b.first) {
            return true;
        } else if(a.first.compareTo(b.first) == 0 && a.second >= b.second) {
            return true;
        }
        
        return false;
    }
    
}

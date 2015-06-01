package cs171_final;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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
    private final ArrayList<PaxosObj> instances; //might be useful to have list of all instances
    private final ArrayList<PaxosObj> pMajority;
    private final ArrayList<Pair> aMajority;
    private final Map<Integer, String> log;
    private String[] proposedMessage = new String[3];
    
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
        this.log = new HashMap<>();
        this.instances = new ArrayList<>();
    }
    
    public void toggleMode() {
        failed = !failed;
        if (failed) {
            leader = false;
        }
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
                        String msg = i.getAccept_val();
                        int tempround = i.getRound();
                        if (!log.containsKey(tempround)) {
                            log.put(tempround, msg);
                        }
//                        execute(i);
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
            case "request": {
                //if leader then just send accepts, else, send prepare messages
                //POST msg/READ ipAddr port
                proposedMessage = input.getCommandLine();
                String msg = proposedMessage[0];
                if(leader) {
                    if(msg.startsWith("Post")) {
                        msg = msg.substring(msg.indexOf(" ") + 1);
                        ++myBallotNum.first;
                        myAcceptNum = myBallotNum;
                        accept(msg);
                    } else {
                        read(proposedMessage[1], Integer.parseInt(proposedMessage[2]));
                    }
                } else {
                    if(msg.startsWith("Post")) {
                        msg = msg.substring(msg.indexOf(" ") + 1);
                        prepare(msg);
                    } else {
                        read(proposedMessage[1], Integer.parseInt(proposedMessage[2]));
                    }
                }
                break;
            }
            case "prepare": {
                if (input.getRound() < round) {
                    break;
                }
                //reply with ack
                if(compareBallot(input.getBallot_num(), myBallotNum)) {
                    promise(input);
                } else {
                    //do nothing?
                }
                break;
            }
            case "promise": {
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
                            if (compareBallot(pMajority.get(0).getAccept_num(), myAcceptNum) 
                                    && compareBallot(pMajority.get(0).getAccept_num(), pMajority.get(1).getAccept_num())) {
                                myAcceptVal = pMajority.get(0).getAcceptedValue();
                            } else if (compareBallot(pMajority.get(1).getAccept_num(), myAcceptNum) 
                                    && compareBallot(pMajority.get(1).getAccept_num(), pMajority.get(0).getAccept_num())) {
                                myAcceptVal = pMajority.get(1).getAcceptedValue();
                            }
                        } else {
                            myAcceptVal = proposedMessage[0] + " " + proposedMessage[1] + " " + proposedMessage[2];
                        }
                        //make list empty again
                        //a site can only prepare one value at a time
                        pMajority.clear();
                        myAcceptNum = myBallotNum;
                        //myAcceptNum = pMajority.get(0).getBallot_num();
                        accept(myAcceptVal);
                    }
                }
                break;
            }
            case "accept": {
                if (input.getRound() < round) {
                    break;
                }
                //reply with ack
                //have to do this only the first time
                //after receiving two accepst(majority) then send decide
                if(myAcceptNum.equals(input.getBallot_num())) {
                    aMajority.add(input.getBallot_num());
                    if(compareBallot(input.getBallot_num(), myBallotNum)) {
                        myAcceptVal = input.getAcceptedValue();
                    }
                } else if(compareBallot(input.getBallot_num(), myBallotNum)) {
                    myAcceptNum = input.getBallot_num();
                    aMajority.add(input.getBallot_num());
                    accept(input.getAcceptedValue());
                }
                
                if(Collections.frequency(aMajority, input.getBallot_num()) == 2) {
                    for(Pair ballotnum : aMajority) {
                        if(ballotnum.equals(input.getBallot_num()))
                            aMajority.remove(ballotnum);
                    }
                    decide();
                }
                
                break;
            }
            case "decide": {
                //not done yet
                //accept value & increase round
                if (input.getRound() < round) {
                    break;
                }
                myAcceptNum = input.getAccept_num();
                myAcceptVal = input.getAcceptedValue();
                if (myAcceptNum.second == site.siteId) {
                    leader = true;
                } else {
                    leader = false;
                }
                //insert value into log for round i where i is the index
                if(log.size() > 0) {
                    if(!log.get(input.getRound()).equals(myAcceptVal)) {
                        System.out.println("Error, different values choosen for index=" + input.getRound());
                    }
                }
                log.put(input.getRound(), myAcceptVal);
                if(input.getRound() == round) {
                    ++round;
                }
                String temp = input.getAcceptedValue();
                temp = temp.substring(0, temp.lastIndexOf(" "));
                temp = temp.substring(temp.lastIndexOf(" ") + 1);
                if (proposedMessage[0] != null) {
                    Socket socket = new Socket(proposedMessage[1], Integer.parseInt(proposedMessage[2]));
                    ObjectOutputStream out;
                    out = new ObjectOutputStream(socket.getOutputStream());
                    Map<Integer, String> success = new HashMap<>();
                    if (temp.equals(proposedMessage[1])) {
                        success.put(round, "Success");
                    } else {
                        success.put(-1, "Failed to do something");
                    }
                    out.writeObject(success);
                    out.flush();
                    out.close();
                    socket.close();
                }
                break;
            }
            default: {
                System.out.println("Error, input.command not defined");
                break;
            }
        }
    }
    
    private void read(String ip, int portNum) throws IOException {
        Socket mysocket = new Socket(ip, portNum);
        ObjectOutputStream out;
        out = new ObjectOutputStream(mysocket.getOutputStream());
        out.writeObject(log);
        out.flush();
        out.close();
        mysocket.close();
    }
    
    private void accept(String msg) throws IOException {
        myAcceptVal = msg;
        for(int i = 0; i < site.siteIPList.length; ++i) {
            if(site.siteId != i) { //don't send to yourself
                Socket mysocket;
                mysocket = new Socket(site.siteIPList[i], port);
                ObjectOutputStream out;
                out = new ObjectOutputStream(mysocket.getOutputStream());
                PaxosObj outObj = new PaxosObj("accept", myAcceptNum, null, myAcceptVal, site.siteId, round);
                out.writeObject(outObj);
                out.flush();
                out.close();
                mysocket.close();
            }
        }
    }
    
    private void prepare(String msg) throws IOException {
        ++myBallotNum.first;
        for(int i = 0; i < site.siteIPList.length; ++i) {
            if(site.siteId != i) { //don't send to yourself
                Socket mysocket;
                mysocket = new Socket(site.siteIPList[i], port);
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

    private void decide() throws IOException {
        for(int i = 0; i < site.siteIPList.length; ++i) {
            if(site.siteId != i) { //don't send to yourself
                Socket mysocket;
                mysocket = new Socket(site.siteIPList[i], port);
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

    private void promise(PaxosObj input) throws IOException {
        myBallotNum = input.getBallot_num(); //not sure if this works, might have to overwrite the '=' operator
        Socket mysocket;
        mysocket = new Socket(site.siteIPList[input.getSenderId()], port);
        ObjectOutputStream out;
        out = new ObjectOutputStream(mysocket.getOutputStream());
        PaxosObj outObj = new PaxosObj("promise", myBallotNum, myAcceptNum, myAcceptVal, site.siteId, round);
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

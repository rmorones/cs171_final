package cs171_final;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

/**
 *
 * @author Ricardo Morones <rmorones@umail.ucsb.edu>
 * @author Chris Kim <chriskim06@gmail.com>
 */
public class CommunicationThread extends Thread {
    private final int port;
    private final Site site;
    private boolean failed;
    private boolean leader; //need to figure out where leader whould be changed
    private int round;
    private String myAcceptVal;
    private Pair myBallotNum;
    private Pair myAcceptNum;
    private final ArrayList<PaxosObj> pMajority;
    private final ArrayList<Pair> aMajority;
    private final ArrayList<String[]> requests = new ArrayList<>();
    public final Map<Integer, PaxosObj> log;
    private String[] proposedMessage = new String[3];
    private boolean inElection = false;
    
    public CommunicationThread(int port, Site site) {
        this.site = site;
        this.port = port;
        this.failed = false;
        this.leader = false;
        this.round = 0;
        this.pMajority = new ArrayList<>();
        this.aMajority = new ArrayList<>();
        this.myAcceptNum = new Pair(0, 0);
        this.myBallotNum = new Pair(0, site.siteId);
        this.myAcceptVal = null;
        this.log = new HashMap<>();
        this.proposedMessage = null;
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
                if (inElection) {
                    serverSocket.setSoTimeout(5000);
                } else {
                    serverSocket.setSoTimeout(0);
                }
                mysocket = serverSocket.accept();
                in = new ObjectInputStream(mysocket.getInputStream());
                PaxosObj input = (PaxosObj)in.readObject();
                System.out.println("connection established with " + mysocket.getInetAddress().toString());
                if (input == null) {
                    System.out.println("object is null");
                    continue;
                }

                if(failed) {
                    continue;
                }
                execute(input);
            } catch (java.net.SocketTimeoutException e) {
                System.out.println("Timeout");
                myBallotNum.first = 0;
                myBallotNum.second = site.siteId;
                myAcceptNum.first = myAcceptNum.second = 0;
                myAcceptVal = null;
                inElection = false;
                if (proposedMessage == null) {
                    continue;
                }
                try {
                    Socket sos;
                    sos = new Socket(proposedMessage[1], Integer.parseInt(proposedMessage[2]));
                    ObjectOutputStream out;
                    out = new ObjectOutputStream(sos.getOutputStream());
                    Map<Integer, PaxosObj> failure = new HashMap<>();
                    failure.put(-10, null);
                    out.writeObject(failure);
                    out.flush();
                    out.close();
                    sos.close();
                    proposedMessage = null;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void execute(PaxosObj input) throws IOException {
        System.out.println("command "+ input.getCommand());
        switch (input.getCommand()) {
            case "request": {
                //if leader then just send accepts, else, send prepare messages
                //POST msg/READ ipAddr port
                if (proposedMessage != null) {
                    requests.add(proposedMessage);
                    System.out.println("proposed: " + proposedMessage[0]);
                }
                proposedMessage = input.getCommandLine();
                 if(requests.size() > 0) {
 			System.out.println("propsed = " + requests.get(0)[0]);
 		}
                String msg = proposedMessage[0];
                if(leader) {
                    if(msg.startsWith("Post")) {
                        inElection = true;
                        ++myBallotNum.first;
                        myAcceptNum = myBallotNum;
                        accept(proposedMessage[0] + " " + proposedMessage[1] + " " + proposedMessage[2]);
                    } else {
                        read(proposedMessage[1], Integer.parseInt(proposedMessage[2]));
                        proposedMessage = null;
                    }
                } else {
                    if(msg.startsWith("Post")) {
                        inElection = true;
                        prepare(input.getAcceptedValue());
                    } else {
                        read(proposedMessage[1], Integer.parseInt(proposedMessage[2]));
                        proposedMessage = null;
                    }
                }
                break;
            }
            case "prepare": {
                inElection = true;
                if (input.getRound() < round || failed) {
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
                if(round == input.getRound() && !failed) {
                    pMajority.add(input);
                    if(pMajority.size() == 2) {
                        //send accept messages
                        boolean set = false;
                        for(PaxosObj p : pMajority) {
                            if(p.getAcceptedValue() != null) {
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
                if (input.getRound() < round || failed) {
                    break;
                }
                //reply with ack
                //have to do this only the first time
                //after receiving two accepst(majority) then send decide
                if(myAcceptNum.equals(input.getBallot_num())) {
                    aMajority.add(input.getBallot_num());
                    if(compareBallot(input.getBallot_num(), myBallotNum) && input.getAcceptedValue() != null) {
                        myAcceptVal = input.getAcceptedValue();
                    }
                } else if(compareBallot(input.getBallot_num(), myBallotNum)) {
                    myAcceptNum = input.getBallot_num();
                    aMajority.add(input.getBallot_num());
                    myBallotNum = myAcceptNum;
                    accept(input.getAcceptedValue());
                }
                
                if(Collections.frequency(aMajority, input.getBallot_num()) == 2) {
                    ListIterator<Pair> li = aMajority.listIterator();
                    while (li.hasNext()) {
                        Pair p = li.next();
                        if (p.equals(input.getBallot_num())) {
                            li.remove();
                        }
                    }
                    System.out.println("Sending decides");
                    decide();
                }
                
                break;
            }
            case "decide": {
                //not done yet
                //accept value & increase round
                if (input.getRound() != round || failed) {
                    break;
                } else if (log.containsKey(input.getRound())) {
                    break;
                }
                inElection = false;
                myAcceptNum = input.getAccept_num();
                myAcceptVal = input.getAcceptedValue();
                myAcceptVal = myAcceptVal.substring(0, myAcceptVal.lastIndexOf(" ")); //get rid of PORT
                myAcceptVal = myAcceptVal.substring(0, myAcceptVal.lastIndexOf(" ")); //get rid of IP
                myAcceptVal = myAcceptVal.substring(5);
                leader = myAcceptNum.second == site.siteId;
                if(leader) {
                    System.out.println("Im the leader");
                } else {
                    System.out.println("Im the not leader");
                }
                //insert value into log for round i where i is the index
                if(log.size() > 0 && log.get(input.getRound()) != null) {
                    if(!log.get(input.getRound()).getAcceptedValue().equals(myAcceptVal)) {
                        System.out.println("Error, different values choosen for index=" + input.getRound());
                    }
                }
                System.out.println("adding " + input.getAcceptedValue() + " to index " + input.getRound().toString());
                log.put(input.getRound(), new PaxosObj(input));
                String tempIP = input.getAcceptedValue();
                String tempPort = tempIP.substring(tempIP.lastIndexOf(" ") + 1);
                tempIP = tempIP.substring(0, tempIP.lastIndexOf(" "));
                tempIP = tempIP.substring(tempIP.lastIndexOf(" ") + 1);
                if (proposedMessage != null) {
                    Socket socket;
                    System.out.println("Sending message to client with port");
                    socket = new Socket(proposedMessage[1], Integer.parseInt(proposedMessage[2]));
                    ObjectOutputStream out;
                    out = new ObjectOutputStream(socket.getOutputStream());
                    Map<Integer, PaxosObj> success = new HashMap<>();
                    if (tempIP.equals(proposedMessage[1]) && tempPort.equals(proposedMessage[2])) {
                        success.put(round, new PaxosObj(null, null, null, "Success!", null, null));
                    } else {
                        success.put(-1, null);
                    }
                    out.writeObject(success);
                    out.flush();
                    out.close();
                    socket.close();
                }
                for (String[] p : requests) {
                    if (p[1] == null) {
                        break;
                    }

                    Socket socket;
                    socket = new Socket(p[1], Integer.parseInt(p[2]));
                    ObjectOutputStream out;
                    out = new ObjectOutputStream(socket.getOutputStream());
                    Map<Integer, PaxosObj> success = new HashMap<>();
                    success.put(-1, null);
                    out.writeObject(success);
                    out.flush();
                    out.close();
                    socket.close();
                }
                requests.clear();
 		proposedMessage = null;
                ++round;
                myBallotNum.first = 0;
                myBallotNum.second = site.siteId;
                myAcceptNum.first = myAcceptNum.second = 0;
                myAcceptVal = null;
                break;
            }
            case "logUpdate": {
                Map<Integer, PaxosObj> received = input.getLog();
                if(received.size() > log.size()) {
                    log.putAll(received);
                    round = log.size();
                }
                break;
            }
            case "update": {
                Socket mysocket;
                mysocket = new Socket(site.siteIPList[input.getSenderId()], port);
                ObjectOutputStream out;
                out = new ObjectOutputStream(mysocket.getOutputStream());
                PaxosObj outObj = new PaxosObj("logUpdate", null);
                outObj.setLog(log);
                out.writeObject(outObj);
                out.flush();
                out.close();
                mysocket.close();
                break;
            }
            case "Restore": {
                myBallotNum.first = 0;
                myBallotNum.second = site.siteId;
                myAcceptNum.first = myAcceptNum.second = 0;
                myAcceptVal = null;
                updateLog();
                break;
            }
            default: {
                System.out.println("Error, input.command not defined");
                break;
            }
        }
    }
    
    private void updateLog() throws IOException {
        for(int i = 0; i < site.siteIPList.length; ++i) {
            if(site.siteId != i) { //don't send to yourself
                Socket mysocket;
                mysocket = new Socket(site.siteIPList[i], port);
                ObjectOutputStream out;
                out = new ObjectOutputStream(mysocket.getOutputStream());
                PaxosObj outObj = new PaxosObj("update", null);
                outObj.setSenderId(site.siteId);
                out.writeObject(outObj);
                out.flush();
                out.close();
                mysocket.close();
            }
        }
    }
    
    private void read(String ip, int portNum) throws IOException {
        Socket mysocket;
        mysocket = new Socket(ip, portNum);
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
            if(site.siteId != i && !failed) { //don't send to yourself
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
            if(site.siteId != i && !failed) { //don't send to yourself
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
            if(site.siteId != i && !failed) { //don't send to yourself
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

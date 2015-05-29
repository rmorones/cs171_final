package cs171_final;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
    
    public CommunicationThread(int port, Site site) {
        this.site = site;
        this.port = port;
        this.failed = false;
        this.missed = new LinkedList<>();
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
    
    void execute(PaxosObj input) throws IOException {
        
        switch (input.getCommand()) {
            case "fail": {
                failed = true;
                break;
            }
            case "restore": {
                failed = false;
                break;
            }
            case "request": {
                //if leader then just send accepts, else, send prepare messages
                if(input.isLeader()) {
                    for(int i = 0; i < site.siteIPList.length; ++i) {
                        Socket mysocket;
                        mysocket= new Socket(site.siteIPList[i], 5232);
                        ObjectOutputStream out;
                        out = new ObjectOutputStream(mysocket.getOutputStream());
                        PaxosObj outObj = new PaxosObj("accept", new Pair(), new Pair(), null, site.siteId, false);
                        out.writeObject(outObj);
                        out.flush();
                        out.close();
                        mysocket.close();
                    }
                } else {
                    
                }
                break;
            }
            case "prepare": {
                
                break;
            }
            case "accept": {
                break;
            }
            case "decide": {
                break;
            }
        }
    }
    
}

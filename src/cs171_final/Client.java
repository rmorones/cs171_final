package cs171_final;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author Ricardo Morones <rmorones@umail.ucsb.edu>
 * @author Chris Kim <chriskim06@gmail.com>
 */
@SuppressWarnings("CallToPrintStackTrace")
public class Client extends Thread {
    
    private int leader = 0;
    private static final int SITE_PORT = 5352;
    private static int port;
    private final String[] ipList = new String[5];
    private String publicIP;
    private ServerSocket serverSocket = null;
    
    public Client() {
        readConfig();
    }
    
    @Override
    public void run() {
        try {
            BufferedReader input;
            input = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("Post")) {
                    if (line.length() > 145) {
                        line = line.substring(0, 145);
                    }
                }
                sendAndListen(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void sendAndListen(String content) {
        String command = content;
        if(command.length() > 4) {
            command = command.substring(0, 4);
        }
        if (!command.equals("Read") && !command.equals("Post")) {
            System.out.println("Input error. Accepted commands: Read/Post");
            return;
        }
        Socket site;
        try {
//            serverSocket = new ServerSocket(0);
            serverSocket = new ServerSocket();
//            port = serverSocket.getLocalPort();
            site = new Socket(ipList[leader], SITE_PORT);
            ObjectOutputStream outputStream = new ObjectOutputStream(site.getOutputStream());
            // send client's public ip and port
            outputStream.writeObject(new PaxosObj("request", content + " " + publicIP + " " + port));
            outputStream.flush();
            site.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        listen();
    }
    
    private void listen() {
        Socket site = null;
        try {
            ObjectInputStream inputStream;
            serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), port));
            serverSocket.setSoTimeout(10000);
            site = serverSocket.accept();
            inputStream = new ObjectInputStream(site.getInputStream());
            Map<Integer, PaxosObj> response = (HashMap<Integer, PaxosObj>) inputStream.readObject();
            if (response.size() == 1) {
                for (Map.Entry<Integer, PaxosObj> e : response.entrySet()) {
                    if (e.getKey() == -1) {
                        System.out.println("Failed to handle request. Site was not able to become the leader.");
                        Random random = new Random();
                        int temp = leader;
                        while (leader == temp) {
                            leader = random.nextInt(5);
                        }
                    } else if(e.getKey() == -10) {
                        System.out.println("Too many failures. Try again when a majority of sites can be reached.");
                    } else {
                        System.out.println(e.getKey() + ": " + e.getValue().getAcceptedValue());
                    }
                }
            } else {
                for (int i = 0; i < response.size(); i++) {
                    System.out.println(i + ": " + response.get(i).getAcceptedValue());
                }
            }
        } catch (java.net.SocketTimeoutException e) {
            System.out.println("Failure: site took too long to respond. Retry posting message.");
            // pick random site id as the leader for next request
            Random random = new Random();
            int temp = leader;
            while (leader == temp) {
                leader = random.nextInt(5);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                site.close();
                serverSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void readConfig() {
        BufferedReader file;
        try {
            file = new BufferedReader(new InputStreamReader(new FileInputStream("config.txt")));
            String line;
            int i = 0;
            while ((line = file.readLine()) != null) {
                ipList[i] = line;
                i++;
            }
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            publicIP = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Client [PORT]");
            return;
        } else {
            try {
                this.port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return;
            }
        }
        Client client = new Client();
        client.start();
        try {
            client.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}

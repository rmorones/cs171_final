package cs171_final;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
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
    private static final int PORT = 5352;
    // need to figure out format of config file that contains ip's
    private String[] ipList = new String[6];
    
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
        String command = content.substring(0, 4);
        if (!command.equals("Read") && !command.equals("Post")) {
            System.out.println("Input error. Accepted commands: Read/Post");
            return;
        }
        Socket site;
        try {
            site = new Socket(ipList[leader], PORT);
            ObjectOutputStream outputStream = new ObjectOutputStream(site.getOutputStream());
            // send client's public ip and port
            outputStream.writeObject(content + " " + ipList[5] + " " + PORT);
            outputStream.flush();
            site.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        listen();
    }
    
    private void listen() {
        ServerSocket serverSocket;
        Socket site;
        ObjectInputStream inputStream;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(ipList[5], PORT));
            site = serverSocket.accept();
            inputStream = new ObjectInputStream(site.getInputStream());
            Map<Integer, String> response = (LinkedHashMap<Integer, String>) inputStream.readObject();
            for (Map.Entry<Integer, String> e : response.entrySet()) {
                if (!e.getValue().equals("failure")) {// successful post or read
                    System.out.println(e.getKey() + ": " + e.getValue());
                } else {// failed post
                    System.out.println("Failure; retry posting message.");
                    // pick random site id as the leader for next request
                    Random random = new Random();
                    int temp = leader;
                    while (leader == temp) {
                        leader = random.nextInt(5);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private void readConfig() {
        BufferedReader file;
        try {
            file = new BufferedReader(new InputStreamReader(new FileInputStream("config.txt")));
            String line;
            int i = 0;
            while ((line = file.readLine()) != null) {
                ipList[i] = line.substring(0, line.indexOf(" "));
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void main(String[] args) {
        Client client = new Client();
        client.start();
        try {
            client.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}

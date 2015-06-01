package cs171_final;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Ricardo Morones <rmorones@umail.ucsb.edu>
 * @author Chris Kim <chriskim06@gmail.com>
 */
@SuppressWarnings("CallToPrintStackTrace")
public class Site extends Thread {
    
    public static final int PORT = 5352;
    public String[] siteIPList = new String[5];
    public int siteId = 100;
    private final CommunicationThread communicationThread;
    // true: normal mode, false: fail mode
    private boolean mode = false;
    
    public Site() {
        readConfig();
        communicationThread = new CommunicationThread(PORT, this);
        communicationThread.start();
    }
    
    @Override
    public void run() {
        try {
            BufferedReader input;
            input = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = input.readLine()) != null) {
                if (line.equals("Fail") && mode) {
                    mode = !mode;
                    communicationThread.toggleMode();
                } else if (line.equals("Restore") && !mode) {
                    mode = !mode;
                    communicationThread.toggleMode();
                }
            }
        } catch (IOException e) {
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
                if (siteId == 100) {
                    siteId = Integer.parseInt(line);
                } else {
                    siteIPList[i] = line;
                    i++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        Site site = new Site();
        site.start();
        try {
            site.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}

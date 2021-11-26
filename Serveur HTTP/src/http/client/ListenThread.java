package http.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ListenThread extends Thread {
    
    private BufferedReader socIn;

    public ListenThread(Socket sock) {
        try {
            this.socIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        } catch (Exception e) {
            System.err.println("Error creating ListenThread: " + e);
        }
    }

    @Override
    public void run() {
        try {
            String line;
            while (true) {
                line = socIn.readLine();
                System.out.println(line);
            }
        } catch (Exception e) {
            System.err.println("Error in ListenThread: "+e);
        }
    }
}

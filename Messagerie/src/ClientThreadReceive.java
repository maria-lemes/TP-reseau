package src; /***
 * ClientThread
 * Example of a TCP server
 * Date: 14/12/08
 * Authors:
 */

import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

public class ClientThreadReceive
        extends Thread {

    private Socket clientSocket;

    public ClientThreadReceive(Socket s) {
        this.clientSocket = s;
    }

    /**
     * receives a request from client then sends an echo to the client
     *
     **/
    public void run() {
        try {
       
            BufferedReader socIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            while (true) {
                String line = socIn.readLine();
                if(line!=null) {
                    System.out.println(line);
                }else{
                    System.out.println("Server stopped responding");
                    clientSocket.close();
                    System.exit(0);
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Error in ClientThreadReceive:" + e);
        }
    }
}

  

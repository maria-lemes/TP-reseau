package src; /***
 * ClientThread
 * Example of a TCP server
 * Date: 14/12/08
 * Authors:
 */

import java.io.*;
import java.net.*;

public class ClientThreadSend
        extends Thread {

    private Socket clientSocket;

    public ClientThreadSend(Socket s) {
        this.clientSocket = s;
    }

    /**
     * reads the inputs from client and sends it to the server
     *
     **/
    public void run() {
        BufferedReader stdIn = null;
        PrintStream socOut = null;

        try {
            stdIn = new BufferedReader(new InputStreamReader(System.in));
            socOut = new PrintStream(clientSocket.getOutputStream());

            while (true) {
                System.out.print("User: ");
                String user = stdIn.readLine();
                if (!"".equals(user)) {
                    validateUser(user, socOut);
                    break;
                }
            }

            while (true) {
                System.out.print("Menu: \n" +
                        "\t1- Send private message\n" +
                        "\t2- Create group chat\n"+
                        "\t3- Send message to a group chat\n"+
                        "\t4- Check message history\n"+
                        "\t0- Exit\n");

                String option = stdIn.readLine();
                switch (option){
                    case "1":
                        sendMessage(stdIn,socOut);
                        break;
                    case "2":
                        createGroup(stdIn,socOut);
                        break;
                    case "3":
                        groupMessage(stdIn,socOut);
                        break;
                    case "4":
                        checkHistory(stdIn,socOut);
                        break;
                    case "0":
                        closeConnection(socOut);
                        return;
                    default: break;
                }
            }

            
        } catch (Exception e) {
            System.err.println("Error in ClientThreadSend:" + e);
        }
    }

    private static void validateUser(String user, PrintStream socOut) {
        try {
            socOut.println("<validate-user:" + user + ">");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendMessage(BufferedReader stdIn, PrintStream socOut) throws IOException {
            System.out.print("Enter recipient's username: ");
            String name = stdIn.readLine();
            socOut.println("<recipient-name:"+name+">");
            System.out.print("Write message or tap '.' to quit:\n");
            while(true) {
                String line = stdIn.readLine();
                if (".".equals(line)) break;
                socOut.println("<message:"+line+">");
            }
    }

    private static void createGroup(BufferedReader stdIn, PrintStream socOut) throws IOException {
        System.out.print("Enter the group name: ");
        String name = stdIn.readLine();
        socOut.println("<group-name:"+name+">");

        while (true) {
            System.out.print("Enter the name of a participant or 'done': ");
            String line = stdIn.readLine();
            if ("done".equals(line)){
                socOut.println("<done>");
                break;
            }else {
                socOut.println("<participant:" + line + ">");
            }
        }
    }

    private static void groupMessage(BufferedReader stdIn, PrintStream socOut) throws IOException {
        System.out.print("Enter the group name: ");
        String name = stdIn.readLine();
        socOut.println("<send-group-message-to:"+name+">");
        System.out.print("Write message or tap '.' to quit:\n");

        while (true) {

            String line = stdIn.readLine();
            if (".".equals(line)) break;
            socOut.println("<group-message:"+line+">");
        }
    }

    private static void checkHistory(BufferedReader stdIn,PrintStream socOut) throws IOException {
        System.out.print("Enter user/group name: ");
        String name = stdIn.readLine();
        socOut.println("<check-history-of:"+name+">");
    }

    private void closeConnection( PrintStream socOut) throws IOException {
        socOut.println("<disconnect>");
        clientSocket.close();
        System.exit(0);
    }
}

  

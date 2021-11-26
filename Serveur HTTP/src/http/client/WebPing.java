package http.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;

public class WebPing {

  /**
   * Creates a client and connects to the server whose address and port are
   * given in the command line, allowing the user to send requests to the server
   * @param args server address and port
   */
  public static void main(String[] args) {

    if (args.length != 2) {
      System.err.println("Usage java WebPing <server host name> <server port number>");
      return;
    }

    String httpServerHost = args[0];
    int httpServerPort = Integer.parseInt(args[1]);

    try {
      Socket sock = new Socket(httpServerHost, httpServerPort);
      InetAddress addr = sock.getInetAddress();
      BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
      PrintStream socOut = new PrintStream(sock.getOutputStream());

      ListenThread lt = new ListenThread(sock);
      lt.start();

      System.out.println("Connected to " + addr);

      String line;
      while (true) {
        line = stdIn.readLine();
        if (line.equals("/exit"))
          break;
        socOut.println(line);
      }

      sock.close();
    } catch (java.io.IOException e) {
      System.out.println("Can't connect to " + httpServerHost + ":" + httpServerPort);
      System.out.println(e);
    }
  }
}
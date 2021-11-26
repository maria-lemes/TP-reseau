///A Simple Web Server (WebServer.java)

package http.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Example program from Chapter 1 Programming Spiders, Bots and Aggregators in
 * Java Copyright 2001 by Jeff Heaton
 * 
 * WebServer is a very simple web-server. Any request is responded with a very
 * simple web-page.
 * 
 * HTTP request handlers added by Gustavo Bertoldi and Maria Lemes
 * 
 * @author Jeff Heaton
 * @version 1.0
 */
public class WebServer {

  private static final String PAGES_PATH = "pages";
  private static final File ERROR_404_PAGE = new File("pages/error404.html");
  private static final File ERROR_403_PAGE = new File("pages/error403.html");
  private static final File ERROR_500_PAGE = new File("pages/error500.html");
  private static final ArrayList<String> appFiles = new ArrayList<>();
  static {
    appFiles.add("/error403.html");
    appFiles.add("/error404.html");
    appFiles.add("/error500.html");
    appFiles.add("/index.html");
  }

  /**
   * WebServer constructor.
   */
  protected void start() {
    final File file = new File(".");
    System.out.println("file = " + file.getAbsoluteFile().getParent());
    ServerSocket s;

    System.out.println("Webserver starting up on port 3000");
    System.out.println("(press ctrl-c to exit)");
    try {
      // create the main server socket
      s = new ServerSocket(3000);
    } catch (Exception e) {
      System.out.println("Error: " + e);
      return;
    }

    System.out.println("Waiting for connection");
    while (true) {
      try {
        // wait for a connection
        Socket remote = s.accept();
        System.out.println("Remote connection from " + remote.getInetAddress() + " accepted.");

        // remote is now the connected socket
        BufferedReader in = new BufferedReader(new InputStreamReader(remote.getInputStream()));
        OutputStream out = remote.getOutputStream();

        String request = "";
        String line = ".";
        while (line != null && !line.equals("")) {
          line = in.readLine();
          request += line;
        }


        processRequest(request, out, in);
        out.close();
      } catch (Exception e) {
        System.out.println("Error: " + e);
      }
    }
  }

  /**
   * Process the request received by the server, getting its type to call the
   * appropriate function to handle it
   * @param request the request text
   * @param clientOut client output stream
   * @param clientIn client input stream
   */
  private static void processRequest(String request, OutputStream clientOut, BufferedReader clientIn) {
    System.out.println(request);
    String[] headers = request.split(" ");
    if (headers.length < 2) {
      System.out.println("Invalid request");
    } else {
      String type = headers[0].toUpperCase();
      String ressource = headers[1];
      System.out.println(type + " " + ressource + " request received. ");
      switch (type) {
      case "GET":
        doGet(ressource, clientOut);
        break;
      case "POST":
        doPost(ressource, clientOut, clientIn);
        break;
      case "PUT":
        doPut(ressource, clientOut, clientIn);
        break;
      case "DELETE":
        doDelete(ressource, clientOut);
        break;
      case "HEAD":
        doHead(ressource, clientOut);
        break;
      default:
        System.err.println("Unknown request: " + type);
        break;
      }
    }
  }

  /**
   * Handles a GET request, receiving the demanded ressource and and sending it
   * to the client, if it exists
   * @param ressourceName the request text
   * @param clientOutput client output stream
   */
  private static void doGet(String ressourceName, OutputStream clientOutput) {
    try {
      if (ressourceName.equals("/")) {
        ressourceName = "/index.html";
      }
      File ressource = new File(PAGES_PATH + ressourceName);
      String status;
      String contentType;
      if (ressource.exists() && ressource.isFile()) {
        contentType = WebServer.getContentType(ressourceName);
        if (contentType != null) {
          status = "200 OK";
        } else {
          status = "403 Forbidden";
          contentType = "text/html";
          ressource = ERROR_403_PAGE;
        }
      } else {
        // File doesn't exist
        status = "404 Not found";
        contentType = "text/html";
        ressource = ERROR_404_PAGE;
      }
      String header = WebServer.makeHeader(status, contentType, ressource.length());
      BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(ressource));
      clientOutput.write(header.getBytes());
      byte[] buffer = new byte[256];
      int nbReadLines;
      while ((nbReadLines = fileIn.read(buffer)) != -1) {
        clientOutput.write(buffer, 0, nbReadLines);
      }
      clientOutput.flush();
      fileIn.close();
    } catch (Exception e) {
      System.out.println("Error while processing GET request: " + e);
    }
  }

  /**
   * Handles a POST request, writting the data sent by the client in the target
   * ressource, id the target ressource exists already, data is added, ressource
   * is created otherwise.
   * @param ressourceName the request text
   * @param clientOutput client output stream
   * @param clientIn client input stream
   */
  private static void doPost(String ressourceName, OutputStream clientOutput, BufferedReader clientIn) {
    File ressource = null;
    String header = "";
    try {
      if (appFiles.contains(ressourceName)) {
        ressource = ERROR_403_PAGE;
        header = WebServer.makeHeader("403 Forbidden", "text/html", ressource.length());
      } else {
        ressource = new File(PAGES_PATH + ressourceName);
        boolean exists = ressource.exists();

        // If the file already exists, the file buffer is opened in insertion at
        // the end mode, the file is created otherwise.
        BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(ressource, exists));
        
        // Receive data
        int c;
        while ((c = clientIn.read()) > 0) {
          fileOut.write(c);
        }
        fileOut.close();

        // Create header
        String status = exists ? "200 OK" : "201 Created";
        header = WebServer.makeHeader(status, "", 0);
        ressource = null;
      }
    } catch (Exception e) {
      System.err.println("Error while processing POST request: " + e);
      String status = "500 Internal server error";
      String contentType = "text/html";
      ressource = ERROR_500_PAGE;
      header = WebServer.makeHeader(status, contentType, ressource.length());
    } finally {
      try {
        clientOutput.write(header.getBytes());
        if (ressource != null) {
          BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(ressource));
          byte[] buffer = new byte[256];
          int nbReadLines;
          while ((nbReadLines = fileIn.read(buffer)) != -1) {
            clientOutput.write(buffer, 0, nbReadLines);
          }
          clientOutput.flush();
          fileIn.close();
        }
      } catch (Exception e) {
        System.err.println("Error while processing POST request: " + e);
      }
    }
  }

  /**
   * Handles a PUT request, updating the target ressource, if it exists, with
   * the data sent by the client.
   * @param ressourceName the request text
   * @param clientOutput client output stream
   * @param clientIn client input stream
   */
  private static void doPut(String ressourceName, OutputStream clientOutput, BufferedReader clientIn) {
    File ressource = null;
    String header = "";
    try {
      if (appFiles.contains(ressourceName)) {
        ressource = ERROR_403_PAGE;
        header = WebServer.makeHeader("403 Forbidden", "text/html", ressource.length());
      } else {
        ressource = new File(PAGES_PATH + ressourceName);
        final boolean exists = ressource.exists();

        if (ressource.exists()) {
          BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(ressource));

          // Receive data
          int c;
          while ((c = clientIn.read()) > 0) {
            fileOut.write(c);
          }
          fileOut.close();
          ressource = null;
        } else {
          ressource = ERROR_404_PAGE;
        }

        // Create header
        String status = exists ? "200 OK" : "404 Not found";
        final long contentLength = ressource != null ? ressource.length() : 0;
        header = WebServer.makeHeader(status, "text/html", contentLength);
      }

    } catch (Exception e) {
      System.err.println("Error while processing PUT request: " + e);
      String status = "500 Internal server error";
      String contentType = "text/html";
      ressource = ERROR_500_PAGE;
      header = WebServer.makeHeader(status, contentType, ressource.length());
    } finally {
      try {
        clientOutput.write(header.getBytes());
        BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(ressource));
        byte[] buffer = new byte[256];
        int nbReadLines;
        while ((nbReadLines = fileIn.read(buffer)) != -1) {
          clientOutput.write(buffer, 0, nbReadLines);
        }
        clientOutput.flush();
        fileIn.close();

      } catch (Exception e) {
        System.err.println("Error while processing PUT request: " + e);
      }
    }
  }

  /**
   * Handles a DELETE request, deleting the ressource if it exists
   * @param ressourceName the request text
   * @param clientOutput client output stream
   */
  private static void doDelete(String ressourceName, OutputStream clientOutput) {
    File ressource = null;
    String header = "";
    try {
      if (appFiles.contains(ressourceName)) {
        ressource = ERROR_403_PAGE;
        header = WebServer.makeHeader("403 Forbidden", "text/html", ressource.length());
      } else {
        ressource = new File(PAGES_PATH + ressourceName);

        if (ressource.exists()) {
          if (ressource.delete()) {
            header = WebServer.makeHeader("200 OK", "", 0);
            ressource = null;
          } else {
            ressource = ERROR_500_PAGE;
            header = WebServer.makeHeader("500 Internal server error", "text/html", ressource.length());
          }
        } else {
          ressource = ERROR_404_PAGE;
          header = WebServer.makeHeader("404 Not found", "text/html", ressource.length());
        }
      }
    } catch (Exception e) {
      System.err.println("Error while processing DELETE request: " + e);
      String status = "500 Internal server error";
      String contentType = "text/html";
      ressource = ERROR_500_PAGE;
      header = WebServer.makeHeader(status, contentType, ressource.length());
    } finally {
      try {
        clientOutput.write(header.getBytes());
        if (ressource != null) {
          BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(ressource));
          byte[] buffer = new byte[256];
          int nbReadLines;
          while ((nbReadLines = fileIn.read(buffer)) != -1) {
            clientOutput.write(buffer, 0, nbReadLines);
          }
          clientOutput.flush();
          fileIn.close();
        }
      } catch (Exception e) {
        System.err.println("Error while processing DELETE request: " + e);
      }
    }
  }

  /**
   * Handles a HEAD request, sending information to the client about the
   * demanded ressource
   * @param ressourceName the request text
   * @param clientOutput client output stream
   */
  private static void doHead(String ressourceName, OutputStream clientOutput) {
    File ressource = null;
    String header = "";
    try {
      if (ressourceName == "") ressourceName = "index.html";
      ressource = new File(PAGES_PATH + ressourceName);
      if (ressource.exists()) {
        header = WebServer.makeHeader("200 OK", WebServer.getContentType(ressourceName), ressource.length());
        ressource = null;
      } else {
        ressource = ERROR_404_PAGE;
        header = WebServer.makeHeader("404 Not found", "text/html", ressource.length());
      }  

    } catch (Exception e) {
      System.err.println("Error while processing HEAD request: " + e);
      String status = "500 Internal server error";
      String contentType = "text/html";
      ressource = ERROR_500_PAGE;
      header = WebServer.makeHeader(status, contentType, ressource.length());
    } finally {
      try {
        clientOutput.write(header.getBytes());
        if (ressource != null) {
          BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(ressource));
          byte[] buffer = new byte[256];
          int nbReadLines;
          while ((nbReadLines = fileIn.read(buffer)) != -1) {
            clientOutput.write(buffer, 0, nbReadLines);
          }
          clientOutput.flush();
          fileIn.close();
        }
      } catch (Exception e) {
        System.err.println("Error while processing HEAD request: " + e);
      }
    }
  }

  /**
   * Returns a string with the header to be sent to the client
   * @param status request status after processing
   * @param contentType 
   * @param contentLength
   * @return header string
   */
  private static String makeHeader(String status, String contentType, long contentLength) {
    return "HTTP/1.0 " + status + "\n" + "Content-Type: " + contentType + "\n" + "Content-Length: " + contentLength
        + "\n" + "Server: B++\n" + "\r\n";
  }

  /**
   * returns the content type of a ressource, or null if the ressource is not
   * supported
   * @param ressourceName
   * @return content type string
   */
  private static String getContentType(String ressourceName) {
    if (ressourceName.endsWith(".html") || ressourceName.endsWith(".htm")) {
      return "text/html";
    } else if (ressourceName.endsWith(".pdf")) {
      return "application/pdf";
    } else if (ressourceName.endsWith(".jpeg") || ressourceName.endsWith(".jpg")) {
      return "image/jpg";
    } else if (ressourceName.endsWith(".txt")) {
      return "text/plain";
    } else {
      return null;
    }
  }

  /**
   * Start the application.
   * 
   * @param args Command line parameters are not used.
   */
  public static void main(String args[]) {
    WebServer ws = new WebServer();
    ws.start();
  }
}

package src;
/***
 * EchoServer
 * Example of a TCP server
 * Date: 10/01/04
 * Authors:
 */

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class EchoServerMultiThreaded {

    private static Map<String, Socket> users = new HashMap<>();
    public static Map<String, Socket> getUsers() { return users; }

    public static HashMap<String,ArrayList<String>> groups = new HashMap<>();
    //history: <user,<sender,messageRecord>>
    public static Map<String,Map<String,ArrayList<String>>> offlineHistory = new HashMap<>();
    public static Map<String, Set<String>> privateChats = new HashMap<>();

    /**
     * main method
     **/
    public static void main(String args[]) {

        ServerSocket listenSocket;

        if (args.length != 1) {
            System.out.println("Usage: java EchoServer <EchoServer port>");
            System.exit(1);
        }
        try {
            listenSocket = new ServerSocket(Integer.parseInt(args[0])); // port
            System.out.println("Server ready...");
            getUsersList();
            getPrivateChats();
            getGroupsList();

            while (true) {
                Socket clientSocket = listenSocket.accept();
                ClientThread ct = new ClientThread(clientSocket);
                System.out.println(users);
                ct.start();

            }
        } catch (Exception e) {
            System.err.println("Error in EchoServerMultiThreaded:" + e);
        }
    }

/**
 * Creates a group identified by a name
 * @param name group's name
 * @param usersList list of all the users to be added at the group (if they exist)
**/
    public synchronized static void createGroup(String name, ArrayList<String> usersList) throws IOException {
        ArrayList participants = new ArrayList();
        String groupCreator = usersList.get(usersList.size() - 1);
        PrintStream socOut = new PrintStream(users.get(groupCreator).getOutputStream());
        for(String u : usersList){
            if(users.containsKey(u))
                participants.add(u);
            else{
                socOut.println("User "+u+" doesn't exist.");
            }
        }
        groups.put(name,participants);
        System.out.println("Group created : " + usersList);
        System.out.println("Groups list : " + groups);
        saveGroupsList(name,participants);
    }

    /**
     * Sends a message to a group chat
     * @param message message to be sent
     * @param sender user who sends the message
     * @param group group where the message will be sent
     **/
    public synchronized static void sendGroupMessage(String message, String sender, String group) throws IOException {
        System.out.println(groups);
        if(groups.get(group) != null) {
            for (String user : groups.get(group)) {
                if (!user.equals(sender)) {
                    try {
                        //si l'utilisateur n'est pas enligne
                         if(users.get(user)  == null ){
                            addToOfflineHistory(user, message, group);
                        }else{
                            PrintStream socOut = new PrintStream(users.get(user).getOutputStream());
                            socOut.println(message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            saveFullHistory(group,message);
        } else{
            try {
                PrintStream socOut = new PrintStream(users.get(sender).getOutputStream());
                socOut.println("This group doesn't exist");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Send a message to one specific user
     * @param message message to be sent
     * @param receiver user who receives the message
     * @param sender user who sends the message
     **/
    public synchronized static void sendPrivateMessage(String message, String receiver, String sender){
        String chatName;
       try{
           if (!users.containsKey(receiver)) {
               PrintStream socOut = new PrintStream(users.get(sender).getOutputStream());
               socOut.println("This user doesn't exist");
               return;
           } else {
               if (users.get(receiver) == null) {
                   //si l'utilisateur n'est pas enligne
                   PrintStream socOut = new PrintStream(users.get(sender).getOutputStream());
                   socOut.println("This user is offline");
                   addToOfflineHistory(receiver, message, sender);
               } else {
                   PrintStream socOut = new PrintStream(users.get(receiver).getOutputStream());
                   socOut.println(message);
               }
               //création une conversation privée avec un nom pour sauvegarder l'historique
               if(privateChats.containsKey(receiver+"-"+sender)){
                   chatName = receiver+"-"+sender;
               }else if(privateChats.containsKey(sender+"-"+receiver)){
                   chatName = sender+"-"+receiver;
               }else{
                   Set<String> privateChat = new HashSet<String>();
                   privateChat.add(receiver);
                   privateChat.add(sender);
                   privateChats.put(receiver+"-"+sender,privateChat);
                   chatName = receiver+"-"+sender;
                   savePrivateChats(chatName);
               }
               saveFullHistory(chatName,message);
           }

       } catch (Exception e) {
           e.printStackTrace();
       }
    }

    /**
     * Saves messages sent to users who are offline in the server (non-persistent version)
     * @param message message to be sent
     * @param receiver user who receives the message
     * @param conversation group or user to whom the message will be sent
     **/
    public synchronized static void addToOfflineHistory(String receiver, String message, String conversation) throws IOException {

        if(offlineHistory.get(receiver) == null){
            Map<String, ArrayList<String>> convRecord = new HashMap<>();
            ArrayList<String> messageRecord = new ArrayList<>();
            messageRecord.add(message);
            convRecord.put(conversation,messageRecord);
            offlineHistory.put(receiver,convRecord);

        }else if(offlineHistory.get(receiver).get(conversation) == null){
            ArrayList<String> messageRecord = new ArrayList<>();
            messageRecord.add(message);
            offlineHistory.get(receiver).put(conversation,messageRecord);

        }else{
            offlineHistory.get(receiver).get(conversation).add(message);

        }

        saveOfflineHistory(receiver);

    }


    /**
     * Shows all messages a user received while offline
     * @param user user who received the offline messages
     **/
    public synchronized static boolean showOfflineHistory(String user){

                System.out.println("History map: " + offlineHistory.get(user));
                PrintStream socOut = null;
                try {
                    if (offlineHistory.get(user) != null) {
                        socOut = new PrintStream(users.get(user).getOutputStream());
                        socOut.println("Messages you received while offline:\n");
                        for (String conversation : offlineHistory.get(user).keySet()) {
                            socOut.println("------" + conversation + "-----");
                            for (String message : offlineHistory.get(user).get(conversation)) {
                                socOut.println(message);
                            }
                            socOut.println("-----------------\n");
                        }
                    } else{checkOfflineHistory(user);}
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
    }

    /**
     * Disconnects a user from the system
     * @param username user who will be disconnected
     **/

    public synchronized static void disconectUser(String username) {
        try {
            users.get(username).close();
            users.put(username,null);
            System.out.println(users);
            System.out.println("User " + username + " disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Saves messages sent to users who are offline in a file (persistent version)
     * @param user  user who received the offline messages
     **/

    public synchronized static void saveOfflineHistory(String user) throws IOException {
        File file = new File("hist",user+".txt");
        String content = "Messages received while offline:\n";
        file.createNewFile();

        if (offlineHistory.get(user) != null) {
                for (String conversation : offlineHistory.get(user).keySet()) {
                    content = content + "------" + conversation + "-----\n";
                    for (String message : offlineHistory.get(user).get(conversation)) {
                       content = content + message + "\n";
                    }
                   content = content + "-----------------\n";
                }
        }else{
            content = "You don't have new messages";
        }
        FileWriter fw = new FileWriter("hist/"+user+".txt", false); //le fichier sera toujours réécrit
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(content);
        bw.close();

    }

    /**
     * Saves all messages sent in a specific chat in a file (persistent version)
     * @param conversation chat where the message was sent
     * @param message message sent
     **/
    public synchronized static void saveFullHistory(String conversation,String message) throws IOException {
        File file = new File("hist","hist/"+conversation+".txt");

        FileWriter fw = new FileWriter("hist/"+conversation+".txt", true); //le fichier sera completé
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(message+"\n");
        bw.close();

    }

    /**
     * Gets all messages sent in a specific chat from a file
     * @param conversation chat where the message was sent
     * @param user user who is checking the history
     **/
    public synchronized static void checkHistory(String user, String conversation) throws IOException {
        String chatName = null;
        PrintStream socOut = new PrintStream(users.get(user).getOutputStream());


        //verifie si on prend un groupChat ou un privateChat
        if(users.containsKey(conversation)){
            if(privateChats.containsKey(user+"-"+conversation)) chatName = user+"-"+conversation;
            else if(privateChats.containsKey(conversation+"-"+user))chatName = conversation+"-"+user;
        }else if(groups.containsKey(conversation)){
            chatName = conversation;
        }else{
            socOut.println("This conversation doesn't exist. Please try another name");
        }

        File file = new File("hist",chatName+".txt");

        if(file.exists()) {
            String content = Files.readString(Path.of("hist/"+chatName + ".txt"));
            if (content != null) {
                socOut.println("--------"+conversation+"--------");
                socOut.println(content);
                socOut.println("Press '.' to go back to the menu");
            }
        }
    }


    /**
     * Gets all messages sent while the user was offline
     * @param receiver user who received the messages
     **/
    public synchronized static void checkOfflineHistory(String receiver) throws IOException {
        File file = new File("hist",receiver+".txt");
        PrintStream socOut = new PrintStream(users.get(receiver).getOutputStream());
        if(file.exists()) {
            String content = Files.readString(Path.of("hist/"+receiver + ".txt"));
            if (content != null) {
                socOut.println(content);
            } else {
                socOut.println("You don't have new messages");
            }
        }
        socOut.println("Enter your choice from the menu: ");
    }

    /**
     * Deletes the offline history when the user logs-in
     * @param user user who logs-in
     **/
    public synchronized static void cleanOfflineHistory(String user){
         offlineHistory.put(user,null);
        try {
            saveOfflineHistory(user);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves all user having a login into a file (user's persistance)
     * @param user user who logs-in
     **/
    public synchronized static void saveUsersList(String user) throws IOException {
        if(!users.containsKey(user)) {
            FileWriter fw = new FileWriter("hist/usersList.txt", true); //le fichier sera completé
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(user + "\n");
            bw.close();
        }
    }

    /**
     * Gets all users having already a login everytime the server starts (user's persistance)
     **/
    public synchronized static void getUsersList() throws IOException {
        File file=new File("hist/usersList.txt");
        FileReader fr=new FileReader(file);
        BufferedReader br=new BufferedReader(fr);
        String line;

        while((line=br.readLine())!=null)
        {
            users.put(line,null);
        }
        fr.close();

    }

    /**
     * Saves all groups created into a file (group's persistance)
     * @param group group's name
     * @param participants group's participants
     **/
    public synchronized static void saveGroupsList(String group, ArrayList<String> participants) throws IOException {
            FileWriter fw = new FileWriter("hist/groupsList.txt", true); //le fichier sera completé
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write("{group:"+group+"\n");
            for(String p: participants){
                bw.write(p+"\n");
            }
            bw.write("}\n");
            bw.close();
    }

    /**
     * Gets all groups created everytime the server starts (group's persistance)
     **/
    public synchronized static void getGroupsList() throws IOException {
        File file=new File("hist/groupsList.txt");
        FileReader fr=new FileReader(file);
        BufferedReader br=new BufferedReader(fr);
        String line;
        String name=null;

        while((line=br.readLine())!=null)
        {
            if(line.startsWith("{group:")){
                name =  line.substring(7, line.length()-0);
                ArrayList<String> participants = new ArrayList<>();
                groups.put(name,participants);
            }else if(!line.equals("}")){
                groups.get(name).add(line);
            }
        }

        fr.close();

    }

    /**
     * Saves all private chats creates into a file (user's persistance)
     * @param chatName name of the two users in the chat concatenated
     **/
    public synchronized static void savePrivateChats(String chatName) throws IOException {
            FileWriter fw = new FileWriter("hist/privateChats.txt", true); //le fichier sera completé
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(chatName + "\n");
            bw.close();
    }

    /**
     * Gets all chats already created everytime the server starts (chats's persistance)
     **/
    public synchronized static void getPrivateChats() throws IOException {
        File file=new File("hist/privateChats.txt");
        FileReader fr=new FileReader(file);
        BufferedReader br=new BufferedReader(fr);
        String line;

        while((line=br.readLine())!=null)
        {
            privateChats.put(line,null);
        }

        fr.close();

    }


}

import java.io.*;

/*
 * Driver class for the App
 */

public class HwhatsApp {
    static App wa;
    public static volatile boolean stopPinging = false;
    public static volatile String response = "";

    public static void main(String[] args) throws IOException, InterruptedException{
        wa = new App();

        long id = -1;
        long lastUpdate = 0;
        long currChatID = -1;
        String username = "";
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);
        wa.updateClient();

        System.out.print("Welcome to WhatsApp! Login with your userID number: ");
        id = Long.parseLong(br.readLine());
        if(wa.knownUsers.contains(id)){
            System.out.print("\033[H\033[2J");
            System.out.println("Welcome back " + wa.userNames.get(id));
        }
        else{
            System.out.println("New user ID dectected, create a username: ");
            username = br.readLine();
            wa.addUser(id, username);
            System.out.print("\033[H\033[2J");
            System.out.println("Welcome " + username + "! Here are your commands: ");
            System.out.println("\'message xxxxxx\' send a message to an open chat");
            System.out.println("\'create xxxxxx\' to create a new chat with id xxxxxx");
            System.out.println("\'open xxxxxx\' to open the messages in a chat");
            System.out.println("\'add xxxxxx yyyyyy\' to add user xxxxxx to chat yyyyyy");
            System.out.println("\'switch\' to switch view to a specific user");
            System.out.println("\'exit\' to exit a chat");
            System.out.println("\'logout\' to quit session");
        }
        printChannels(id);
        currChatID = -1;
        wa.updateClientMaps();
        wa.updateServer();

        /*
         * Creates two threads, one to monitor the System.in input, and one to
         * continually ping the server every 100 ms. This is necessary using blocking
         * I/O, but can be done without multiple threads if using java NIO.
         */
        while(true){
            stopPinging = false;
            response = "";

            Thread inputThread = new Thread(() -> {
                try {
                    response = br.readLine(); // Waits for input
                    stopPinging = true; // Sets the flag to stop pinging
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            inputThread.start();

            while(!stopPinging){
                wa.updateClient();
                long update = App.updateFromServer;
                if(currChatID != -1 && update != lastUpdate){
                    lastUpdate = update;
                    System.out.print("\033[H\033[2J");
                    wa.printChannel(currChatID, id);
                }
                else if(update != lastUpdate){
                    lastUpdate = update;
                    System.out.print("\033[H\033[2J");
                    printChannels(id);
                }
                Thread.sleep(100);
            }

            String[] responseTokens = response.split("\\s+");
            try{
                if(response.equals("logout")){
                    break;
                }
                else if(responseTokens[0].equals("create")){
                    if(currChatID != -1){
                        System.out.println("Please exit chats before creating a new one");
                        continue;
                    }
                    Long channelID = Long.parseLong(responseTokens[1]);
                    System.out.print("\033[H\033[2J");
                    wa.createChannel(id, channelID);
                    printChannels(id);
                }
                else if(responseTokens[0].equals("message")){
                    wa.updateClient();
                    if(currChatID == -1){
                        System.out.println("No chat open, please open a chat before sending a message");
                        continue;
                    }
                    String message = "";
                    for(int i = 1; i < responseTokens.length; i++){
                        message += responseTokens[i] + " ";
                    }

                    System.out.print("\033[H\033[2J");
                    wa.addMessage(id, currChatID, message);

                    wa.printChannel(currChatID, id);
                }
                else if(responseTokens[0].equals("open")){
                    wa.updateClient();
                    if(currChatID != -1){
                        System.out.println("please exit all chats before opening another one");
                        continue;
                    }
                    Long chatID = Long.parseLong(responseTokens[1]);
                    if(!App.userChannelAccess.get(id).contains(chatID)){
                        System.out.println("You do not have access to that chat");
                        continue;
                    }
                    currChatID = chatID;
                    System.out.print("\033[H\033[2J");
                    wa.printChannel(currChatID, id);
                }
                else if(responseTokens[0].equals("add")){
                    if(currChatID != -1){
                        System.out.println("please exit all chats before adding to any chat");
                        continue;
                    }
                    wa.updateClient();
                    long user2 = Long.parseLong(responseTokens[1]);
                    long channel = Long.parseLong(responseTokens[2]);
                    if(!wa.knownUsers.contains(user2)){
                        System.out.println(user2 + " not found, please create an account and try again");
                        continue;
                    }
                    System.out.print("\033[H\033[2J");
                    wa.addUserToChannel(channel, user2);
                    printChannels(id);
                }
                else if(responseTokens[0].equals("switch")){
                    System.out.print("\033[H\033[2J");
                    System.out.println("Login with your userID number: ");
                    id = Long.parseLong(br.readLine());
                    if(wa.knownUsers.contains(id)){
                        System.out.println("Welcome back " + wa.userNames.get(id));
                    }
                    else{
                        System.out.println("New user ID dectected, create a username: ");
                        username = br.readLine();
                        wa.addUser(id, username);
                    }
                    System.out.print("\033[H\033[2J");
                    printChannels(id);
                    currChatID = -1;
                }
                else if(responseTokens[0].equals("exit")){
                    wa.updateClient();
                    currChatID = -1;
                    System.out.print("\033[H\033[2J");
                    printChannels(id);
                }
                else if(responseTokens[0].equals("refresh")){
                    wa.updateClient();
                    System.out.print("\033[H\033[2J");
                    if(currChatID == -1){
                        printChannels(id);
                    }
                    else{
                        wa.printChannel(currChatID, id);
                    }
                    continue;
                }
                else if(currChatID != -1){
                    wa.updateClient();
                    String message = "";
                    for(int i = 0; i < responseTokens.length; i++){
                        message += responseTokens[i] + " ";
                    }

                    System.out.print("\033[H\033[2J");
                    wa.addMessage(id, currChatID, message);

                    wa.printChannel(currChatID, id);
                }
                else{
                    System.out.println("unrecognized character sequence");
                    continue;
                }
            }
            catch(Exception e){
                System.out.println(e.getLocalizedMessage());
                System.out.println("incorrect action found, try again");
                continue;
            }
            wa.updateClientMaps();
            wa.updateServer();
        }
    }
    /*
     * Helper function to print all the users that have access to a certain chat
     */
    public static void printUsersInChannel(long id) throws IOException{
        for(long l : wa.getUsersInChannel(id)){
            System.out.print(l + ", ");
        }
        System.out.println();
    }

    /*
     * Helper function to print all the channels that are avilable to a certain user
     */

    public static void printChannels(long id) throws IOException{
        System.out.println("Available Chats: ");
        for(long l : App.userChannelAccess.get(id)){
            Channel c = App.channelMap.get(l);
            System.out.print(c.id + ": ");
            printUsersInChannel(c.id);
        }
    }
}
import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

/*
 *
 * This contains the code for the App class, which handles logic like adding users,
 * sending messages and more. Each internal data structure is shared between each instance of the app,
 * and we use concurrent hash maps since each client is on a new thread
 *
 * Every method is designed to run in O(1) time to ensure quick loading
 *
 */
public class App {

    HashSet<Long> knownUsers = new HashSet<>();
    ConcurrentHashMap<Long, String> userNames = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Long, String> idMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Long, Channel> channelMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Channel, HashSet<Long>> userLists = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Long, HashSet<Long>> userChannelAccess = new ConcurrentHashMap<>();
    static long updateFromServer = -1;

    //To send to the server, basic routing table
    ConcurrentHashMap<String, Object> clientMaps = new ConcurrentHashMap<>();

    String hostname = "localhost";
    int port = 1234;


    public App() throws IOException{}

    /*
     * Add a new user with their associated username
     */

    public void addUser(Long userID, String userName) throws IOException{
        idMap.put(userID, userName);
        if(!userChannelAccess.containsKey(userID)){
            userChannelAccess.put(userID, new HashSet<Long>());
        }
        knownUsers.add(userID);
        userNames.put(userID, userName);
    }

    /*
     * Add a message to a specific chat channel
     */
    public void addMessage(long user, Long channelID, String channelMessage) throws IOException{
        if(!channelMap.containsKey(channelID)){
            channelMap.put(channelID, new Channel(channelID, new ArrayList<Message>()));
        }
        updateAddChannel(user, channelMap.get(channelID), channelMessage);
    }

    private void updateAddChannel(long user, Channel c, String s) throws IOException{
        c.textMessages.add(new Message(user, s));
    }

    /*
     * Print out all the text in a channel with the correct formatting for who is viewing
     */

    public void printChannel(long channelID, long viewerID) throws IOException{
        Channel c = channelMap.get(channelID);
        long lastID = -1;
        for(Message s : c.textMessages){
            if(lastID != s.sender){
                System.out.println();
                if(s.sender == viewerID){
                    System.out.print("You: ");
                }
                else{
                    System.out.print(idMap.get(s.sender) + ": ");
                }
            }
            System.out.println(s.message);
            lastID = s.sender;
        }
    }

    /*
     * Allow a user to be able to view and send messages to a chat
     */

    public void addUserToChannel(long channelID, long userID) throws IOException{
        if(!userLists.containsKey(channelMap.get(channelID))){
            System.out.println("Channel not found");
            return;
        }
        userLists.get(channelMap.get(channelID)).add(userID);
        if(!userChannelAccess.containsKey(userID)){
            userChannelAccess.put(userID, new HashSet<Long>());
        }
        userChannelAccess.get(userID).add(channelID);
    }

    /*
     * List all users that are in a chat
     */

    public HashSet<Long> getUsersInChannel(long channelID) throws IOException{
        return userLists.get(channelMap.get(channelID));
    }

    /*
     * Create a new channel and add the creator to channel
     */

    public void createChannel(long creator, long id) throws IOException{
        Channel c = new Channel(id, new ArrayList<Message>());
        channelMap.put(id, c);
        userLists.put(c, new HashSet<>());
        addUserToChannel(id, creator);
    }

    /*
     * Send our map of data to the server
     */

    public void updateServer() throws IOException{
        try (Socket socket = new Socket(hostname, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        ) {
            // Send the ConcurrentHashMaps to the server
            oos.writeObject(clientMaps);
            oos.flush();

        } catch (Exception e) {
            System.out.println("Unknown host: " + e.getMessage());
        }
    }

    /*
     * Ping the server to request data, which leads to a data send along the stream
     * that this function takes and casts to the correct maps
     */

    public void updateClient() throws IOException{
        try (Socket socket = new Socket(hostname, port)){
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(new Integer(1));
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            ConcurrentHashMap<String, Object> updatedMaps = (ConcurrentHashMap<String, Object>) ois.readObject();
            for(Map.Entry<String, Object> entry : updatedMaps.entrySet()){
                String key = entry.getKey();
                Object value = entry.getValue();
                if(key.equals("knownUsers")){
                    knownUsers = (HashSet<Long>) value;
                }
                else if(key.equals("userNames")){
                    userNames = (ConcurrentHashMap<Long, String>) value;
                }
                else if(key.equals("idMap")){
                    idMap = (ConcurrentHashMap<Long, String>) value;
                }
                else if(key.equals("channelMap")){
                    channelMap = (ConcurrentHashMap<Long, Channel>) value;
                }
                else if(key.equals("userLists")){
                    userLists = (ConcurrentHashMap<Channel, HashSet<Long>>) value;
                }
                else if(key.equals("userChannelAccess")){
                    userChannelAccess = (ConcurrentHashMap<Long, HashSet<Long>>) value;
                }
                else if(key.equals("update")){
                    updateFromServer = (long) value;
                }
            }
        }catch(SocketTimeoutException e){
            System.out.println("couldn't receive info");
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    /*
     * Helper function to update all of our individual hashmaps
     */

    public void updateClientMaps(){
        clientMaps.put("knownUsers", knownUsers);
        clientMaps.put("userNames", userNames);
        clientMaps.put("idMap", idMap);
        clientMaps.put("userLists", userLists);
        clientMaps.put("userChannelAccess", userChannelAccess);
        clientMaps.put("channelMap", channelMap);
    }

}

/*
 * Channel object. Each channel has a channel ID and a list of all the messages sent
 * in the channel, which is decrypted on the client side by the print function
 */

class Channel implements Serializable{
    private static final long serialVersionUID = 1L;

    long id;
    ArrayList<Message> textMessages;

    public Channel(long id, ArrayList<Message> m){
        this.id = id;
        this.textMessages = m;
    }
}

/*
 * Message object. Stores the sender and message text, allowing it to be displayed
 * differently depending on who is viewing it.
 */

class Message implements Serializable{
    private static final long serialVersionUID = 1L;

    long sender;
    String message;

    public Message(long s, String m){
        this.sender = s;
        this.message = m;
    }
}
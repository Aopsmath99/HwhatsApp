import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

/*
 * This class contains server logic. The data is stored in a concurrent hashmap since each
 * client is on a different thread, and concurrent hashmap is resource safe.
 *
 * Everytime a client sends an update, the centralMaps hashmap stores the result and sends back a
 * result to any waiting clients.
 *
 * It also checks if a chat history or setting has been updated and pings all clients to
 * allow for dynamic refreshing.
 *
 * Default to localhost but can easily be changed to any existing server to allow multiple machines
 * to communicate
 */

public class Server {
    private static final ConcurrentHashMap<String, Object> centralMaps = new ConcurrentHashMap<>();
    private static volatile long numUpdates = 0;
    public static void main(String[] args) throws IOException{
        try (ServerSocket serverSocket = new ServerSocket(1234)) {
            System.out.println("Server is listening on port 1234");
            centralMaps.put("update", numUpdates);
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run(){
            try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
                Object o = ois.readObject();
                if(numUpdates == Long.MAX_VALUE){
                    numUpdates = 0;
                }
                try{
                    HashSet<Channel> channelSet = new HashSet<Channel>();
                    ConcurrentHashMap<String, Object> clientMaps = (ConcurrentHashMap<String, Object>) o;
                    System.out.println("Received ConcurrentHashMaps from client: " + clientMaps);
                    System.out.println("Current Central Map: " + centralMaps);
                    // Update the central ConcurrentHashMaps
                    for (String key : clientMaps.keySet()) {
                        if(key.equals("knownUsers") && centralMaps.containsKey("knownUsers")){
                            HashSet<Long> users = (HashSet<Long>)centralMaps.get("knownUsers");
                            for(long l : (HashSet<Long>)clientMaps.get(key)){
                                users.add(l);
                            }
                            centralMaps.put(key, users);
                        }
                        else if(key.equals("userNames") && centralMaps.containsKey("userNames")){
                            ConcurrentHashMap<Long, String> users = (ConcurrentHashMap<Long, String>)clientMaps.get("userNames");
                            ConcurrentHashMap<Long, String> current = (ConcurrentHashMap<Long, String>)centralMaps.get("userNames");
                            for(Map.Entry<Long, String> entry : users.entrySet()){
                                current.put(entry.getKey(), entry.getValue());
                            }
                        }
                        else if(key.equals("idMap") && centralMaps.containsKey("idMap")){
                            ConcurrentHashMap<Long, String> users = (ConcurrentHashMap<Long, String>)clientMaps.get("idMap");
                            ConcurrentHashMap<Long, String> current = (ConcurrentHashMap<Long, String>)centralMaps.get("idMap");
                            for(Map.Entry<Long, String> entry : users.entrySet()){
                                current.put(entry.getKey(), entry.getValue());
                            }
                        }
                        else if(key.equals("channelMap") && centralMaps.containsKey("channelMap")){
                            ConcurrentHashMap<Long, Channel> users = (ConcurrentHashMap<Long, Channel>)clientMaps.get("channelMap");
                            ConcurrentHashMap<Long, Channel> current = (ConcurrentHashMap<Long, Channel>)centralMaps.get("channelMap");
                            for(Map.Entry<Long, Channel> entry : users.entrySet()){
                                if(!current.containsKey(entry.getKey()) || (entry.getValue().textMessages.size() != current.get(entry.getKey()).textMessages.size())){
                                    numUpdates++;
                                    System.out.println("found update on channel " + entry.getKey());
                                    centralMaps.put("update", numUpdates);
                                }
                                current.put(entry.getKey(), entry.getValue());

                            }
                            for(Map.Entry<Long, Channel> entry : current.entrySet()){
                                channelSet.add(entry.getValue());
                            }
                        }
                        else if(key.equals("userLists") && centralMaps.containsKey("userLists")){
                            ConcurrentHashMap<Channel, HashSet<Long>> users = (ConcurrentHashMap<Channel, HashSet<Long>>)clientMaps.get("userLists");
                            ConcurrentHashMap<Channel, HashSet<Long>> current = (ConcurrentHashMap<Channel, HashSet<Long>>)centralMaps.get("userLists");
                            for(Map.Entry<Channel, HashSet<Long>> entry : users.entrySet()){
                                if(!current.containsKey(entry.getKey()) || (entry.getValue().size() != current.get(entry.getKey()).size())){
                                    numUpdates++;
                                    System.out.println("found update on channel " + entry.getKey());
                                    centralMaps.put("update", numUpdates);
                                }
                                current.put(entry.getKey(), entry.getValue());
                            }
                            for(Map.Entry<Channel, HashSet<Long>> entry : current.entrySet()){
                                if(!channelSet.contains(entry.getKey())){
                                    current.remove(entry.getKey());
                                }
                            }
                        }
                        else if(key.equals("userChannelAccess") && centralMaps.containsKey("userChannelAccess")){
                            ConcurrentHashMap<Long, HashSet<Long>> users = (ConcurrentHashMap<Long, HashSet<Long>>)clientMaps.get("userChannelAccess");
                            ConcurrentHashMap<Long, HashSet<Long>> current = (ConcurrentHashMap<Long, HashSet<Long>>)centralMaps.get("userChannelAccess");
                            for(Map.Entry<Long, HashSet<Long>> entry : users.entrySet()){
                                current.put(entry.getKey(), entry.getValue());
                            }
                        }
                        else{
                            centralMaps.put(key, clientMaps.get(key));
                        }
                    }
                    System.out.println("Central Map " + centralMaps);

                    oos.writeObject(centralMaps);
                    oos.flush();

                }catch(ClassCastException e){
                    Integer i = (Integer)o;
                    if(i == 1){
                        oos.writeObject(centralMaps);
                        oos.flush();
                    }
                }
            }catch(ClassNotFoundException e){
                System.out.println(e.getMessage());


            }
            catch(IOException e){
                System.out.println(e.getLocalizedMessage());
            }
        }
    }
}
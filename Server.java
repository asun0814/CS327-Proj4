/**
 * CS 327 Proj 2
 * Server class manages the socket, accepts clients, and keeps track of all the
 * ClientHandler threads
 * @author: Alix Sun, Elaine Zhou
 * @start date: 10/15/2025
 */

// import statments : dataoutputstream and datainputstream
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
// attribute fields
// method calls

public class Server{
    /** Server socket listens for incoming connections */
    private ServerSocket serverSocket;

    /** Port number that the server listens with */
    private int port;

    /** Arraylist of all clients connected to the server, each represented by a ClientHandler */
    private ArrayList<ClientHandler> clients;

    /** Counter to assign a unique ID to each client */
    public int clientCounter;

    /** Flag to keep the server running; setting to false stops the server */
    private boolean running;

    /* The SRTServer object responsible for the SRT layer */
    private SRTServer srtS;

    /* the maximum clients this server can handle */
    int maxClients;

    /* TEMP IN OUT STREAMS */
    ObjectInputStream input;
    ObjectOutputStream output;

    /**
     * Constructor: Initializes the server on a specific port.
     *
     * @param newPort Port number for the server to listen on.
     * @throws IOException if the ServerSocket cannot be created.
     */
    public Server(int newPort, int max) throws IOException {
        port = newPort;
        serverSocket = new ServerSocket(newPort);       // create a the server's listening socket using the port number
        clients = new ArrayList<>();                    // initialize client list
        clientCounter = 0;
        maxClients = max;                     // start client IDs at 0

        running = true;                                 // set server running flag
        System.out.println("Server started on port " + port);

        srtS = new SRTServer(port, maxClients, serverSocket, this); // create the SRT layer
    }

    /**
     * Starts the server loop.
     * Waits for new client connections. Creates ClientHandler threads them.
     */
    public void start() {
        System.out.println("Waiting for clients...");
        while (running) {
            try {
                // Accept a new client connection (blocks until client connects)
                Socket connectionSocket = serverSocket.accept();

                // Assign a unique ID to this client
                clientCounter++;

                // Create a handler for this client
                ClientHandler handler = new ClientHandler(connectionSocket, clientCounter);
                //srtS.servSocks[clientCounter] = connectionSocket;

                // Add the handler to the list of clients
                clients.add(handler);


                // Start the handler thread, the server can accept new clients while
                // existing clients are still connected
                System.out.println("Client " + clientCounter + " connected.");
                //handler.start();

            } catch (IOException e) {
                System.err.println("Error accepting client: " + e.getMessage());
            }
        }
    }

    public int startOverlay() {
        try{
            Socket connectionSocket = serverSocket.accept();



            // Create a handler for this client
            //ClientHandler handler = new ClientHandler(connectionSocket, clientCounter);

            try {
                output = new ObjectOutputStream(connectionSocket.getOutputStream());
                output.flush();
                input = new ObjectInputStream(connectionSocket.getInputStream());

            } catch (IOException e) {
                System.err.println("Error creating streams for client " + clientCounter);
            }

            srtS.servSocks[clientCounter] = connectionSocket;
            srtS.inputStreams.put(clientCounter, input);
            srtS.outputStreams.put(clientCounter, output);


            // Add the handler to the list of clients
            //clients.add(handler);


            // Start the handler thread, the server can accept new clients while
            // existing clients are still connected
            System.out.println("Client " + clientCounter + " connected.");
            //handler.start();

            // Assign a unique ID to this client
            clientCounter++;

        }catch(Exception e){
            return -1;
        }
        return 1;
    }


    /**
     * stopOverlay() - Stop the overlay before terminating their processes
     *
     */
    public int stopOverlay(){
        System.out.println("stopping overlay");
        running = false;
        try{
            for(int i=0; i< srtS.servSocks.length; i++){
                if(srtS.servSocks[i] != null && !srtS.servSocks[i].isClosed()){
                    srtS.servSocks[i].close();
                }
                srtS.TCBtable[i] = null;
            }
        }catch(IOException e){
            System.out.println("error in startOverlay");
            return -1;
        }
        return 1;
    }

    /**
     * Sends a message to a specific client by ID.
     *
     * @param clientID The ID of the recipient client.
     * @param msg      The message to send.
     */
    public void sendToClient(int clientID, Message msg) {
        for (ClientHandler handler : clients) {
            // If we find the client handler with the right ID,
            // we send message to that specific client
            if (handler.getClientID() == clientID) {
                handler.sendMessage(msg);
                // stop searching once we find the client
                break;
            }
        }
        System.out.println("Client " + clientID + " not found.");
    }

    /**
     * Removes a client from the server's list when disconnected.
     *
     * @param clientID The ID of the client to remove.
     */
    public void removeClient(int clientID) {
        // Remove the client handler from the list when it has the right ID
        for (ClientHandler handler : clients) {
            if (handler.getClientID() == clientID) {
                clients.remove(handler);
                // stop once we remove the match
                break;
            }
        }
        System.out.println("Client " + clientID + " removed.");
    }

    /**
     * Stops the server:
     * - Sets running = false so the loop ends.
     * - Closes the server socket to stop listening for new clients.
     */
    public void stopServer() {
        running = false;
        try {
            // Make sure the server socket exists and it hasn't been closed
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }

    }

    /**
     * generates a list of ClientIDs from all ClientHandlers in the clients Arraylist
     *
     * @return A String list of all clientIDs.
     */
    public String listClients(){
        String list = "";
        for (ClientHandler handler : clients){
            list = list + handler.getClientID() + ", ";
        }
        list = list.substring(0, list.length()-1);
        return list;
    }


    private class AcceptClients extends Thread{

        /**
         * Starts the server loop. (start from above server class)
         * Waits for new client connections. Creates ClientHandler threads them.
         */
        @Override
        public void run() {
            System.out.println("Waiting for clients...");
            while (running) {
                try {
                    // Accept a new client connection (blocks until client connects)
                    Socket connectionSocket = serverSocket.accept();

                    // Assign a unique ID to this client
                    clientCounter++;

                    // Create a handler for this client
                    ClientHandler handler = new ClientHandler(connectionSocket, clientCounter);
                    srtS.servSocks[clientCounter] = connectionSocket;

                    // Add the handler to the list of clients
                    clients.add(handler);


                    // Start the handler thread, the server can accept new clients while
                    // existing clients are still connected
                    System.out.println("Client " + clientCounter + " connected.");
                    //handler.start();

                } catch (IOException e) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        }
    }


    // Inner class for handling one client
    private class ClientHandler extends Thread {
        private Socket connectionSocket;
        private ObjectInputStream input;
        private ObjectOutputStream output;
        private int clientID;
        /**
         * Constructor: Sets up streams for the connected client.
         *
         * @param newSocket   The socket for this client.
         * @param newClientID The unique ID assigned to this client.
         */
        public ClientHandler(Socket newSocket, int newClientID) {
            connectionSocket = newSocket;
            clientID = newClientID;
            try {
                input = new ObjectInputStream(connectionSocket.getInputStream());
                output = new ObjectOutputStream(connectionSocket.getOutputStream());
            } catch (IOException e) {
                System.err.println("Error creating streams for client " + clientID);
            }
        }
        /**
         * Main loop for this client:
         * - Continuously reads messages from the client.
         * - Ends when the client disconnects.
         */
        @Override
        public void run() {
            try {
                Boolean clientConnectRunning = true;
                while (clientConnectRunning) {
                    // Receive message or ACK/NAK string from the input
                    Object received = input.readObject();

                    if (received instanceof Message) {
                        Message rcvdMsg = (Message) received;
                        // If the client sent a message object, then print it
                        System.out.println("\nClient " + clientID + ": " + rcvdMsg.toString());

                        //if recipient is another client, send to proper destination
                        if(!rcvdMsg.getTo().equalsIgnoreCase("server")){
                            sendToClient(Integer.parseInt(rcvdMsg.getTo()), rcvdMsg);

                        }else{
                            //If the message is meant for the server
                            String recMsgText = rcvdMsg.getText();
                            String responseMsgText = "";
                            switch(recMsgText){
                                case "Hello server, how are you?":
                                    responseMsgText = "Doing well, thank you for asking.";
                                    break;

                                case "What's the server status?":
                                    responseMsgText = "Currently hosting " + clients.size() + " clients.";
                                    break;

                                case "Requesting client list":
                                    responseMsgText = "Current clients:\n" + listClients();
                                    break;

                                case "Quitting":
                                    responseMsgText = "Closing connection";
                                    clientConnectRunning = false;
                                    break;

                                default:
                                    break;
                            }
                            Message msg = new Message(responseMsgText,"Server", rcvdMsg.getFrom());
                            sendMessage(msg);
                        }

                    } else if (received instanceof String) {
                        // If the client sent ACK/NAK
                        System.out.println("\nClient " + clientID + ": " + received);
                    }

                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client " + clientID + " disconnected.");
            } finally {
                // Clean up resources and remove this client
                // TO DO get client to close first
                closeConnection();
                removeClient(clientID);
            }
        }
        /**
         * Sends a message to this client.
         *
         * @param msg The message to send.
         */
        public void sendMessage(Message msg) {
            try {
                output.writeObject(msg);
                output.flush();                             //send
            } catch (IOException e) {
                System.err.println("Error sending to client " + clientID);
            }
        }

        /**
         * Sends either an ACK (success) or NAK (failure) string
         * to the client to acknowledge a message or event.
         * NOTE: currently not used
         *
         * @param ack true = send "ACK", false = send "NAK".
         */
        public void sendAckNak(boolean ack) {
            try {
                // Create a new string("ACK" or "NAK") depending on the boolean parameter input
                output.writeObject(ack ? "ACK" : "NAK");
                output.flush();                             //send
            } catch (IOException e) {
                System.err.println("ACK/NAK send failed: " + e.getMessage());
            }
        }

        /**
         * Closes this client’s input/output streams and socket.
         */
        private void closeConnection() {
            try {
                if (input != null) input.close();
                if (output != null) output.close();
                if (connectionSocket != null) connectionSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing connection for client " + clientID);
            }
        }
        /**
         * Getter for the client’s ID.
         *
         * @return client ID as an integer.
         */
        public int getClientID() {
            return clientID;
        }
    }

    /**
     * Entry point to run the server.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {

            // start server on port 59090
            Server server = new Server(59090, 5);
            //server.start();

            System.out.println("overlay returns: " + server.startOverlay());

            try{
                if(server.srtS.initSRTServer()==-1){
                    throw new Exception("innitSRTServer failed");
                }

                // System.out.println();
                // for(int i=0; i< server.srtS.tableSize; i++){
                //     TCBServer current = server.srtS.TCBtable[i];
                //     System.out.print(current.nodeIDServer +  ", " + current.portNumServer + ", " + current.nodeIDClient + ", "  + current.portNumClient + ", " + current.stateServer);
                // }


                int socksr = server.srtS.createSockSRTServer(88);
                if(socksr == -1){
                    throw new Exception("createSockSRTServer failed");
                }

                // System.out.println();
                // for(int i=0; i< server.srtS.tableSize; i++){
                //     TCBServer current = server.srtS.TCBtable[i];
                //     if(current != null){
                //          System.out.print(current.nodeIDServer +  ", " + current.portNumServer + ", " + current.nodeIDClient + ", "  + current.portNumClient + ", " + current.stateServer + "\n");
                //     }

                // }

                int sockfd = 0; //_______________________________________________FIX
                if (server.srtS.acceptSRTServer(socksr) == -1){
                    throw new Exception("createSockSRTServer failed");
                }

                System.out.println("Sleeping for 10000");
                Thread.sleep(10000);
                System.out.println("sleep over");

                if (server.srtS.closeSRTServer(sockfd) == -1){
                    throw new Exception("closeSRTServer failed");
                }


                if (server.stopOverlay() == -1){
                    throw new Exception("stopOverlay failed");
                }

            }catch(Exception e){
                System.out.println("Error caught");
            }


            server.running = false;
            System.out.println("server.running set to false");

            // System.out.println("stop overlay returns: " + server.stopOverlay());



            //Close all connected client sockets first
            System.out.println("Closing client connections...");
            for (ClientHandler handler : server.clients) {
                handler.closeConnection();
            }
            System.out.println("All client connections closed.");
            // If the clienthandler list is empty, close all the server socket and stop the server
            if (server.clients.isEmpty()){
                server.serverSocket.close();
                server.stopServer();
            }



        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
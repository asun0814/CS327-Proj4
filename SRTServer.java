import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/*
 * CS 327 Proj 2
 *
 *
 * @author: Alix Sun, Elaine Zhou
 * @start date: 10/13/2025
 */



public class SRTServer {


    /** The set delay between receiving FIN and closing the connection*/
    private static final int CLOSE_WAIT_TIMEOUT = 1000;


    public int IDCounter;                     // ID should match for both client and server
    public int serverPort;                    // Server's port number
    public int tableSize;                     // max number of client connections that can be storied in TCBtable

    public TCBServer[] TCBtable;              // Array of SRTServer's Transport Control Blocks (TCBServer objects)
    private Socket[] servSocks;               // Array of SRTServer's connection sockets for each client
    private ArrayList<ListenThread> lisThreads; // mostly here in case of loose running threads at teardown
    private ServerSocket serverSock;          // The server's dedicated socket

    public boolean running;


    /**
     * Constructor: Sets the SRTServer port number and maximum client capacity
     *
     * @param portNumServer      Port number for the server to listen on.
     * @param size               Maximum number of client entries or sockegts in TCBtable and servSocks arrays respectively
     */
    public SRTServer(int portNumServer, int size, ServerSocket sock) {
        serverPort = portNumServer;
        IDCounter = 0;
        tableSize = size;
        serverSock = sock;
        running = true;

    }

    public int startOverlay() {
        System.out.println("Waiting for clients...");

        while (running) {
            try {
                // Accept a new client connection (blocks until client connects)
                Socket connectionSocket = serverSock.accept();

                // Assign a unique ID to this client
                // Add the handler to the list of clients
                servSocks[IDCounter] = connectionSocket;

                IDCounter++;

                // Start the handler thread, the server can accept new clients while
                // existing clients are still connected
                System.out.println("Client " + IDCounter + " connected.");

            } catch (IOException e) {
                System.err.println("Error accepting client: " + e.getMessage());
                return -1;
            }
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
            for(int i=0; i< servSocks.length; i++){
                if(servSocks[i] != null && !servSocks[i].isClosed()){
                    servSocks[i].close();
                }
                TCBtable[i] = null;
            }
        }catch(IOException e){
            System.out.println("error in startOverlay");
            return -1;
        }
        return 1;
    }


    /**
     *  initSRTServer() - initializes a TCB table for containing TCBServer objects, as well as the server sockets' table
     * and an arraylist to keep track of threads.
     *
     * @return            1 if sucessful, -1 if an error occurs
     */
    public int initSRTServer(){
        System.out.println("innitiating SRT server");
        try{
            TCBtable = new TCBServer[tableSize];
            servSocks = new Socket[tableSize];
            lisThreads = new ArrayList<>();
        }catch(Exception e){
            return -1;
        }

        // if (error) {return -1};
        return 1;
    }

    /**
     * createSockSRTServer() - Creates a unique server socket for each client and creates a TCB table entry
     * to store ID and Port numbers for the server and client. Client port number (portNumClient) is initialized
     * to -1 until the connection is accepted later in acceptSRTServer(). Increments IDcounter.
     *
     * @param serverPort     The socket ID used to find the TCB entry in the TCB table (client)
     * @return            IDCounter, which is nodeIDServer of the created TCBServer entry. Returns -1 if an error occurs
     */
    public int createSockSRTServer(int serverPort){
        System.out.println("creating SRT Socket");
        try{
            TCBServer newEntry = new TCBServer(IDCounter, serverPort, IDCounter, -1, TCBServer.CLOSED);   // create TCB for new client, no port number yet (-1)

            for(int i=0; i<TCBtable.length; i++){                          // find the first empty table entry

                if(TCBtable[i] == null){
                    TCBtable[i] = newEntry;
                    //IDCounter++;

                    return IDCounter; //socket descriptor id
                }
            }
            return -1;

        }catch(Exception e){
            return -1;
        }
    }

    /**
     * acceptSRTServer() - Accept a connection request from the client, gets the TCBServer
     * entry using the sockfd and changes the state of the connection to LISTENING.
     * It starts the ListenThread to wait for SYNs, and changes the state to CONNECTED
     * on receiving the SYN segment
     *
     * @param sockfd        The socket ID used to find the TCB entry in the TCB table (client)
     * @return              1 if sucessful, -1 if an error occurs 
     */
    public int acceptSRTServer(int sockfd){
        System.out.println("Accepting SRT connection");
        try{
            for(int i=0; i<TCBtable.length; i++){
                if(TCBtable[i].nodeIDClient == sockfd){
                    //Socket connSock = serverSock.accept(); // accept connection to pass the new connection socket to a ListeningThread
                    //servSocks[i] = connSock;
                    TCBtable[i].portNumClient = servSocks[i].getPort();     // set client port
                    TCBtable[i].stateServer = TCBServer.LISTENING;             // set state to LISTENING

                    //start listening thread for SYN
                    ListenThread newThread = new ListenThread(servSocks[i], sockfd, TCBtable[i]);
                    lisThreads.add(newThread);                       //to keep track of any loose threads
                    newThread.start();

//                    try{
//                        newThread.join();                         //wait for thread to finish and remove from thread list
//                        lisThreads.remove(newThread);
//                    }catch(InterruptedException e){
//                        return -1;
//                    }

                    return 1;
                }
            }
            return -1;
        }catch (Exception e){
            return -1;
        }
    }

    /**
     * closeSRTServer() - SRTServer closes and removes the socket, removes the TCB entry obtained using socksr
     *
     * @param socksr        The server socket ID used to find the TCB entry in the TCB table
     * @return              1 if succeeded ( was in the right state to complete a close) and -1 if fails (i.e., in the wrong state).
     */
    public int closeSRTServer(int socksr){
        System.out.println("Closing SRT connection");
        try{
            for(int i=0; i<TCBtable.length; i++){
                if(TCBtable[i].nodeIDClient == socksr){
                    if(TCBtable[i].stateServer== TCBServer.CLOSED){
                        servSocks[i].close();
                        servSocks[i] = null;
                        TCBtable[i] = null;
                        return 1;
                    }
                }
            }
        }catch(IOException e){
            return -1;
        }
        return -1;
    }

    /**
     * The run() method handles incoming segments and
     * cancels the thread on receiving SYNs and FINs. The thread must also start a CLOSE WAIT
     * TIMEOUT timer on receiving FIN before changing state to CLOSED.
     */
    public class ListenThread extends Thread {
        private Socket connectionSocket;
        private ObjectInputStream input;
        private ObjectOutputStream output;

        private TCBServer TCBsr;
        private int clientID;
        private Boolean listening;

        private Timer timer;
        private TimerTask close = new TimerTask() {
            @Override
            public void run(){
                listening = false;
                TCBsr.stateServer = TCBServer.CLOSED;
            }
        };


        /**
         * Constructor for ListenThread: 
         *
         * @param newSocket        The socket for this client.
         * @param sockfd           The unique ID assigned to this client.
         * @param blockSr          The TCB assigned to this client.
         */
        public ListenThread(Socket newSocket, int sockfd, TCBServer blockSr) {
            connectionSocket = newSocket;
            clientID = sockfd;
            TCBsr = blockSr;

            try {
                output = new ObjectOutputStream(connectionSocket.getOutputStream());
                output.flush();
                input = new ObjectInputStream(connectionSocket.getInputStream());

            } catch (IOException e) {
                System.err.println("Error creating streams for client " + clientID);
            }
        }
        /**
         * Main loop for this ListeningThread:
         * - Reads messages from the client.
         * - Ends after SYNACK or FINACK sent.
         */
        @Override
        public void run() {
            System.out.println("running ListenThread " + clientID);
            timer = new Timer();
            try {
                listening = true;
                while (listening) {
                    // Receive segment from the input
                    Object received = input.readObject();

                    if (received instanceof Segment) {
                        Segment rcvd = (Segment) received;
                        System.out.println("Recived segment from client " + clientID);
                        Segment sendSeg;

                        if(rcvd.type == Segment.SYN){ //Syn
                            if(TCBsr.stateServer == TCBServer.LISTENING || TCBsr.stateServer == TCBServer.CONNECTED){
                                System.out.println("Recived type SYN, sending SYNACK");
                                listening = false;
                                //TCBsr.stateServer = CONNECTED;   handled in acceptSRTServer()
                                TCBsr.stateServer = TCBServer.CONNECTED;             // set state to CONNECTED

                                sendSeg = new Segment(Segment.SYNACK);
                                sendSegment(sendSeg);
                            }


                        }else if(rcvd.type == Segment.FIN){ //Fin
                            if(TCBsr.stateServer == TCBServer.CLOSEWAIT || TCBsr.stateServer == TCBServer.CONNECTED){
                                System.out.println("Recived Type FIN, sending FINACK");
                                TCBsr.stateServer = TCBServer.CLOSEWAIT;

                                sendSeg = new Segment(Segment.FINACK);
                                sendSegment(sendSeg);

                                timer.schedule(close , CLOSE_WAIT_TIMEOUT); // should reset timer when a new FIN is recived
                            }

                        }

                    }

                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client " + clientID + " disconnected.");
            } //closing and removing client is seperated into closeSRTServer
        }


        /**
         * Sends a segment to this client.
         *
         * @param seg The segment to send.
         */
        public void sendSegment(Segment seg) {
            try {
                output.writeObject(seg);
                output.flush();                             //send
            } catch (IOException e) {
                System.err.println("Error sending to client " + clientID);
            }
        }
    }


}
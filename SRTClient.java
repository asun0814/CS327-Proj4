/*
 * CS 327 Proj 2
 * The SRTClient class setup overlays, initializes SRTClients, and manages connection setup and teardown and overlay
 * teardown
 *
 * @author: Alix Sun, Elaine Zhou
 * @start date: 10/13/2025
 */

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class SRTClient {
    /** Server port for connection */
    private int ServerPort;

    /** Hashmap used to store the existing TCP connections and access these sockets via integer
     * TCP descriptors */
    private HashMap<Integer, Socket> TCPConnections = new HashMap<>();

    /** A TCB table that stores the TCB objects, each TCB object holds connection-specific info like ports, node IDs,
     * and state */
    private TCBClient[] TCBTable;

    /** Max amount of TCB Objects we could store in the TCB table, determine the length for the TCB array */
    private int maxTCBEntries;

    /** Descriptor to access the matching socket */
    private int socketDescriptor;

    /** Output streams to send segments to the server */
    //private HashMap<Integer, ObjectInputStream> inputStreams = new HashMap<>();
    private ObjectInputStream[] inStreams;
    /** Input streams to receive segments from the server */
    //private HashMap<Integer, ObjectOutputStream> outputStreams = new HashMap<>();
    private ObjectOutputStream[] outStreams;

    /** The number of milliseconds to wait for SYNACK before retransmitting SYN, should be 100 milliseconds */
    private final int SYN_TIMEOUT;

    /** The max number of SYN retransmissions in srtClientConnect() */
    private final int SYN_MAX_RETRY;

    /** The number of milliseconds to wait for FINACK before retransmitting FIN, should be 100 milliseconds */
    private final int FIN_TIMEOUT;

    /** The max number of FIN retransmissions in srtClientDisconnect()  */
    private final int FIN_MAX_RETRY;

    /** Constructor for SRTClient class */
    public SRTClient(){
        maxTCBEntries = 5;
        SYN_TIMEOUT = 10000;
        FIN_TIMEOUT = 10000;
        SYN_MAX_RETRY = 5;
        FIN_MAX_RETRY = 5;
        socketDescriptor = 0;

    }


//     /**
//     * startOverlay() - Create a direct TCP link between the client and the server
//      * @return socketDescriptor    The TCP socket descriptor
//     */
//    public int startOverlay() throws IOException {
//        // The server port to establish a TCP link
//        ServerPort = 59090;
//        // Descriptor to access the matching socket
//        socketDescriptor = 0;
//
//
//        // Get the IP address of the client machine to connect to the server at given host:port
//        InetAddress ip = InetAddress.getByName("localhost");
//                //.getHostAddress();
//
//        try {
//            System.out.println("1Connecting to " + ip + "...");
//            // Connect to the server at given host:port
//            Socket clientSocket = new Socket(ip, ServerPort);
//
//            int socketID = socketDescriptor;
//            // Assign a socket ID to the clientSocket
//            System.out.println("Socket ID: " + socketDescriptor);
//            socketDescriptor++;
//            System.out.println("Socket ID: " + socketDescriptor);
//            System.out.println("Socket ID 2: " + socketID);


//
//
//            // Store the socketDescriptor and the matching TCP connection in a hashmap
//            // In a similar way, store the input and output in two hashmaps along with the socket descripton
//            TCPConnections.put(socketDescriptor, clientSocket);
//
//            System.out.println("After2");
//            System.out.println("TCP link created successfully, connected to server at " + ip + ":" + ServerPort);
//
//            // Starts the ListenThread thread to handle incoming segments
//
//            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
//            System.out.println("overlay 1 after output stream open");
//            output.flush(); // Ensure header is sent immediately
//            ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
//            System.out.println("overlay 1 after streams open");
//
//            outStreams[socketID] = output;
//            inStreams[socketID] = input;
//
//
//            ListenThread listener = new ListenThread(socketID, clientSocket);
//            System.out.println("Listen thread created");
//            listener.start();
//            System.out.println("Listen thread started");
//
//            return socketDescriptor;
//
//        } catch (UnknownHostException e) {
//            System.err.println("Error connecting to server: " + e.getMessage());
//            return -1;
//        }
//
//    }

    /**
     * initSRTClient() - initializes the TCB table marking all entries NULL, starts the ListenThread thread
     * to handle incoming segments
     *
     */
    public void initSRTClient(){
        System.out.println("Initializing SRT Client...");
        // Initializes the TCB table marking all entries NULL
        TCBTable = new TCBClient[maxTCBEntries];
        outStreams = new ObjectOutputStream[maxTCBEntries];
        inStreams = new ObjectInputStream[maxTCBEntries];

        for (int i = 0; i < TCBTable.length; i++) {
            TCBTable[i] = null;
            outStreams[i] = null;
            inStreams[i] = null;
        }

    }


    /**
     * createSockSRTClient() - Create a client side socket
     * @param client_port      The client port being set to
     * @return socket ID   The TCP socket descriptor
     *
     */
    public int createSockSRTClient(int client_port) throws IOException {
        System.out.println("Create Client socket ....");
        // Sco
        int socketID = 0;
        // Looks up the client TCB table to find the first NULL entry, and creates a new TCB entry
        for (int i = 0; i < TCBTable.length; i++) {
            if (TCBTable[i] == null) {
                // The state of the TCB instance has already been set to CLOSED in the TCB class
                TCBTable[i] = new TCBClient();
                // Set the client port to the call parameter client port
                TCBTable[i].portNumClient = client_port;
                // The TCB table entry index is the new socket ID to the client and be used to identify the
                // connection on the client side
                socketID = i;
                TCBTable[i].nodeIDClient = socketID;
                TCBTable[i].nodeIDServer = socketID;

                System.out.println("[createSockSRTClient] New SRTclient socket created:");
                System.out.println("  - Socket ID: " + socketID);
                System.out.println("  - Client Port: " + client_port);
                System.out.println("  - Initial State: CLOSED");

                return socketID;
            }
        }
        // Return -1 if there's no free slot
        return -1;
    }

    /**
     * connectSRTClient() - Connect to the server socket, sets up the TCB’s server port number
     * and a SYN segment to sent to the server
     * @param socksr      The socket ID used to find the TCB entry in the TCB table
     * @param server_port    The server’s port number
     * @return               1 if the connection is successfully setup, -1 if the connection failed
     */
    public int connectSRTClient(int socksr, int server_port){
        System.out.println("Connecting to the server ....");
        // Count of retransmission time
        int retryCount = 0;
        try {
            // Find the matching TCB entry using the socket ID
            TCBClient tcb = TCBTable[socksr];
            // Handle boundary situation when TCB is null
            if (tcb == null) {
                return -1;
            }
            // Sets the TCB’s portNumServer to the parameter server_port
            tcb.portNumServer = server_port;
            // Create and send a SYN segment
            Segment seg1 = new Segment(Segment.SYN);
            sendSegment(socksr, seg1);
            System.out.println("[connectSRTClient] SYN segment sent to the server port :" + server_port);

            // Change the status of the TCB object to SYNSENT to wait for SYNACK
            tcb.stateClient = TCBClient.SYNSENT;


            // Retransmit SYN segment if SYNACK is not received
            while (retryCount < SYN_MAX_RETRY) {
                try {
                    // Set the timer to 100 milliseconds by causing the current executing thread to sleep for the
                    // SYN_TIMEOUT amount of time, wait for the SYNACK response from the server
                    Thread.sleep(SYN_TIMEOUT);
                    if (tcb.stateClient == TCBClient.CONNECTED) {
                        System.out.println("[connectSRTClient] Socket " + socksr + " connected to server port " + server_port);
                        // Return 1 when the SYNACK is received by the listenthread
                        return 1;
                    }
                } catch (InterruptedException e) {
                    System.err.println("Thread interrupted" + e.getMessage());
                    return -1;
                }
                // SYNACK not received, retransmit SYN
                sendSegment(socksr, seg1);
                retryCount++;
                System.out.println("[connectSRTClient] SYNACK is not received, resend segment to the server port :"
                        + server_port + " for the " + retryCount + " times ");
            }
            // Exceeding the maximum retransmission number, close the TCB objects
            tcb.stateClient = TCBClient.CLOSED;
            System.out.println("[connectSRTClient] Connection failed after retries.");
            return -1;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            return -1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * disconnSRTClient() - Disconnects from the server after a wait period
     * @param socksr      The socket ID used to find the TCB entry in the TCB table
     * @return            1 if the disconnection was successful, -1 if the disconnection failed
     */
    public int disconnSRTClient(int socksr){
        int retryCount = 0;
        try {
            // Find the matching TCB entry using the socket ID
            TCBClient tcb = TCBTable[socksr];
            // Handle boundary situation when TCB is null
            if (tcb == null) {
                return -1;
            }
            // Create and send a FIN segment
            Segment seg1 = new Segment(Segment.FIN);
            sendSegment(socksr, seg1);
            System.out.println("[connectSRTClient] FIN segment sent to the server ");

            // Change the status of the TCB object to FINWAIT to wait for FINACK
            tcb.stateClient = TCBClient.FINWAIT;

            // Retransmit FIN segment if FINACK is not received
            while (retryCount < FIN_MAX_RETRY) {
                try {
                    // Set the timer to 100 milliseconds by causing the current executing thread to sleep for the
                    // SYN_TIMEOUT amount of time, wait for the SYNACK response from the server
                    Thread.sleep(FIN_TIMEOUT);
                    if (tcb.stateClient == TCBClient.CLOSED) {
                        System.out.println("[disconnSRTClient] Socket " + socksr + " has been disconnected from " +
                                "the server ");
                        // Return 1 when the FINACK is received by the listenthread
                        return 1;
                    }
                } catch (InterruptedException e) {
                    System.err.println("Thread interrupted" + e.getMessage());
                    return -1;
                }
                // FINACK not received, retransmit FIN
                sendSegment(socksr, seg1);
                retryCount++;
                System.out.println("[disconnSRTClient] FINACK is not received, resend segment to the server port for the "
                        + retryCount + " times ");
            }
            // Exceeding the maximum retransmission number, close the TCB objects
            tcb.stateClient = TCBClient.CLOSED;
            System.out.println("[disconnSRTClient] Disconnection failed after retries, close the TCB object ");
            return -1;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Error disconnecting from the server: " + e.getMessage());
            return -1;
        }
    }

    /**
     * closeSRTClient() - The client closes the socket, removes item from the TCB table and
     * returns            1 if removing TCB items succeeds and -1 if fails
     * @param socksr      The socket ID used to find the TCB entry in the TCB table
     */
    public int closeSRTClient(int socksr){
        try {
            // Find the matching TCB entry using the socket ID
            TCBClient tcb = TCBTable[socksr];
            // Handle boundary situation when selected TCB object is already null
            if (tcb == null) {
                return -1;
            }
            // Remove the matching TCB objects from the TCBtable by setting it back to null
            TCBTable[socksr] = null;

            System.out.println("[closeSRTClient] Socket " + socksr + " is removed from the TCB table ");
            // Return 1 when the removal is successful
            return 1;
        } catch (IllegalStateException e) {
            // Return -1 when the TCB object is in the wrong state
            System.err.println("Socket is not in the correct state to be closed: " + e.getMessage());
            return -1;
        }

    }
//    /**
//     * stopOverlay() - Stop the overlay before terminating their processes
//     *
//     */
//    public void stopOverlay() throws IOException {
//        try {
//            //Close all the sockets and clear the TCP connections map
//            for (Socket socket : TCPConnections.values()) {
//                socket.close();
//            }
//            TCPConnections.clear();
//            System.out.println("Overlay stopped successfully, all connections closed.");
//        } catch (IOException e) {
//            System.err.println("Close error: " + e.getMessage());
//        }
//    }
//

    /**
     * sendSegment() - Send a segment of particular type to the server
     * @param socksr      The socket ID used to find the matching output stream
     * @param segment - send a segment object
     */
    public void sendSegment(int socksr, Segment segment){
        try {
            //ObjectOutputStream output = outputStreams.get(socksr);
            ObjectOutputStream output = outStreams[socksr];
            // Similar to other send methods, send the segment via the TCP sockets
            output.writeObject(segment);
            // Ouputstream sent the segment to the server
            output.flush();
        } catch (IOException e) {
            System.err.println("Segment send failed: " + e.getMessage());
        }
    }


    /**
     * The run() method handles incoming segments and cancels the thread on receiving
     * SYNACKs and FINACKs
     * Technically works as a receive() method in the Client class
     */
    public static class ListenThread extends Thread {
        // Boolean used to cancel the running thread safely when necessary
        private boolean running = true;
        // Used to identify the specific TCB object in the table
        private int socketID;
        // Input and output streams for the ListenThread
        private ObjectOutputStream output;
        private ObjectInputStream input;
        // Specific TCB object processed in the thread
        private TCBClient tcb;
        private Socket connectSock;

        /**
         * Constructor for ListenThread
         *
         */
        public ListenThread(int socksr, Socket connectionSocket) throws IOException {
            System.out.println("Listen thread 1");
            socketID = socksr;
            connectSock = connectionSocket;


            System.out.println("This is the tcb object " + tcb);
            System.out.println("Input output start");
//            output = new ObjectOutputStream(TCPConnections.get(socksr).getOutputStream());
//            input = new ObjectInputStream(TCPConnections.get(socksr).getInputStream());
            System.out.println("Input output finish");
        }

        @Override
        public void run() {
            try {
                while (running) {
                    // Receive the incoming segments from the input stream
                    Segment seg = (Segment) input.readObject();
                    // Handle different types of segments
                    // Cancels the thread on receiving SYNACKs and FINACKs
                    switch (seg.type) {
                        case Segment.SYN:
                            Segment seg1 = new Segment(Segment.SYNACK);
                            sendSegment(socketID, seg1);
                            break;

                        case Segment.SYNACK:
                            // Update the SYNSENT tcb object status to connected once the thread receives SYNACK
                            // Revise it to be more specific
                            if (tcb.stateClient == TCBClient.SYNSENT) {
                                tcb.stateClient = TCBClient.CONNECTED;
                            }
                            // Then cancel the thread
                            System.out.println("[ListenThread] Client received SYNACK, canceling the thread ");
                            running = false;
                            break;

                        case Segment.FIN:
                            Segment seg2 = new Segment(Segment.FINACK);
                            sendSegment(socketID, seg2);
                            break;

                        case Segment.FINACK:
                            // Update the SYNSENT tcb object status to connected once the thread receives SYNACK
                            if (tcb.stateClient == TCBClient.FINWAIT) {
                                tcb.stateClient = TCBClient.CLOSED;
                            }
                            System.out.println("[ListenThread] Client received FINACK, canceling the thread ");
                            connectSock.close();
                            running = false;
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("[ListenThread] Listening Error: " + e.getMessage());
            }
        }

        public void sendSegment(int socksr, Segment segment){
            try {
                //output = outputStreams.get(socksr);
                // Similar to other send methods, send the segment via the TCP sockets
                output.writeObject(segment);
                // Ouputstream sent the segment to the server
                output.flush();
            } catch (IOException e) {
                System.err.println("Segment send failed: " + e.getMessage());
            }
        }

    }
    }



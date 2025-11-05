/**
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
import java.util.HashMap;
import java.util.Map;

public class SRTClient {
    /** Server port for connection */
    private int ServerPort;

    /** Hashmap used to store the existing TCP connections and access these sockets via integer
     * TCP descriptors */
    private static HashMap<Integer, Socket> TCPConnections = new HashMap<>();

    /** A TCB table that stores the TCB objects, each TCB object holds connection-specific info like ports, node IDs,
     * and state */
    private TCBClient[] TCBTable;

    /** Max amount of TCB Objects we could store in the TCB table, determine the length for the TCB array */
    private int maxTCBEntries;

    /** Output streams to send segments to the server */
    public HashMap<Integer, ObjectInputStream> inputStreams = new HashMap<>();

    /** Input streams to receive segments from the server */
    public HashMap<Integer, ObjectOutputStream> outputStreams = new HashMap<>();

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

    }


    /**
     * initSRTClient() - initializes the TCB table marking all entries NULL, starts the ListenThread thread
     * to handle incoming segments
     *
     */
    public void initSRTClient(HashMap<Integer, Socket> clientSockets, HashMap<Integer, ObjectInputStream> ins
            ,HashMap<Integer, ObjectOutputStream> outs ){
        System.out.println("Initializing SRT Client...");
        // Initializes the TCB table marking all entries NULL
        TCPConnections = clientSockets;
        TCBTable = new TCBClient[maxTCBEntries];
        outputStreams = outs;
        inputStreams = ins;

        for (int i = 0; i < TCBTable.length; i++) {
            TCBTable[i] = null;

        }

        // Create a thread for each TCP connection by looping through the TCP hashmap
        for (Map.Entry<Integer, Socket> entry : TCPConnections.entrySet()) {
            int socketID = entry.getKey();
            Socket newSocket = entry.getValue();

            try {
                // Create a TCB for this connection
                TCBClient tcb = new TCBClient();
                TCBTable[socketID] = tcb;

                // Start listener thread
                ListenThread listenThread = new ListenThread(socketID, newSocket, tcb, this);
                listenThread.start();
                System.out.println("[SRTClient] Listening thread started for socket " + socketID);

            } catch (IOException e) {
                System.err.println("[SRTClient] Failed to start ListenThread for socket " + socketID + ": " + e.getMessage());
            }

        }
    }


    /**
     * createSockSRTClient() - Create a client side socket
     * @param client_port      The client port being set to
     * @return socket ID   The TCP socket descriptor
     *
     */
    public int createSockSRTClient(int client_port) throws IOException {
        System.out.println("[createSockSRTClient] Create Client socket ....");
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
//                System.out.println("  - Socket ID: " + socketID);
//                System.out.println("  - Client Port: " + client_port);
//                System.out.println("  - Initial State: CLOSED");

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
        System.out.println("[connectSRTClient] Connecting to the server ....");
//        System.out.println("[connectSRTClient] Looking for socket: " + socksr);
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
                    System.err.println("[connectSRTClient] Thread interrupted" + e.getMessage());
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
            System.err.println("[connectSRTClient] Error connecting to server: " + e.getMessage());
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
        System.out.println("[disconnSRTClient] Starting disconnectSRTClient");
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
            System.out.println("[disconnSRTClient] FIN segment sent to the server ");

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
                    System.err.println("[disconnSRTClient] Thread interrupted" + e.getMessage());
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
            System.err.println("[disconnSRTClient]  Error disconnecting from the server: " + e.getMessage());
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
            System.err.println(" [closeSRTClient] Socket is not in the correct state to be closed: " + e.getMessage());
            return -1;
        }

    }

    /**
     * sendSegment() - Send a segment of particular type to the server
     * @param socksr      The socket ID used to find the matching output stream
     * @param segment - send a segment object
     */
    public void sendSegment(int socksr, Segment segment){
        try {
            Socket newSocket = TCPConnections.get(socksr);
            ObjectOutputStream out = outputStreams.get(socksr);
            // Similar to other send methods, send the segment via the TCP sockets
            out.writeObject(segment);
            // Ouputstream sent the segment to the server
            out.flush();
        } catch (IOException e) {
            System.err.println("[SRTClient sendSegment] Segment send failed: " + e.getMessage());
        }
    }

}
/**
 * CS 327 Proj 2
 * The SRTServer class setup overlays, initializes SRTServers, and manages connection setup and teardown and overlay
 * teardown
 *
 * @author: Alix Sun, Elaine Zhou
 * @start date: 10/13/2025
 */

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class SRTServer {
    /** The set delay between receiving FIN and closing the connection */
    private static final int CLOSE_WAIT_TIMEOUT = 1000;

    /** Server's port number */
    public int serverPort;

    /** Max number of client connections that can be storied in TCBtable */
    public int tableSize;

    /** Array of SRTServer's Transport Control Blocks (TCBServer objects) */
    public TCBServer[] TCBtable;

    /** Array of SRTServer's connection sockets for each client */
    public Socket[] servSocks;

    /** Mostly here in case of loose running threads at teardown */
    public ArrayList<ListenThreadS> lisThreads;

    /** The server's dedicated socket */
    public ServerSocket serverSock;

    /** Output streams to send segments to the server */
    public HashMap<Integer, ObjectInputStream> inputStreams = new HashMap<>();

    /** Input streams to send segments to the server */
    public HashMap<Integer, ObjectOutputStream> outputStreams = new HashMap<>();

    /** Boolean used to cancel the running thread safely when necessary */
    public boolean running;

    /** Server object to access its public attributes */
    private Server server;


    /**
     * Constructor: Sets the SRTServer port number and maximum client capacity
     *
     * @param portNumServer      Port number for the server to listen on.
     * @param size               Maximum number of client entries or sockegts in TCBtable and servSocks arrays respectively
     */
    public SRTServer(int portNumServer, int size, ServerSocket sock, Server ser) {
        serverPort = portNumServer;
        tableSize = size;
        serverSock = sock;
        running = true;
        server = ser;

        servSocks = new Socket[tableSize];

    }


    /**
     *  initSRTServer() - initializes a TCB table for containing TCBServer objects, as well as the server sockets' table
     * and an arraylist to keep track of threads.
     *
     * @return            1 if sucessful, -1 if an error occurs
     */
    public int initSRTServer(){
        System.out.println("[initSRTServer] innitiating SRT server");
        try{
            // Initialize the TCBtable
            TCBtable = new TCBServer[tableSize];
            for(int i=0; i< tableSize; i++){
                TCBtable[i] = null;
            }
            lisThreads = new ArrayList<>();
//            System.out.println("[initSRTServer] SRT server init done");
        }catch(Exception e){
            return -1;
        }

        // if (error) {return -1};
        return 1;
    }

    /**
      * createSockSRTServer() - Creates a unique server socket for each client and creates a TCB table entry
      * to store ID and Port numbers for the server and client. Client port number (portNumClient) is initialized
      * to -1 until the connection is accepted later in acceptSRTServer(). Increments server.clientCounter.
      *
      * @param serverPort     The socket ID used to find the TCB entry in the TCB table (client)
      * @return            server.clientCounter, which is nodeIDServer of the created TCBServer entry. Returns -1 if an error occurs
      */
    public int createSockSRTServer(int serverPort){
        System.out.println("[initSRTServer] Creating SRT Socket");
        try{
            for(int i=0; i<TCBtable.length; i++){                          // find the first empty table entry
//                System.out.println(" create loop " + i);
                if(TCBtable[i] == null){
                    TCBServer newEntry = new TCBServer(i, serverPort, i, -1, TCBServer.CLOSED);   // create TCB for new client, no port number yet (-1)
                    TCBtable[i] = newEntry;

                    return i; //socket descriptor id
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
      * It starts the ListenThreadS to wait for SYNs, and changes the state to CONNECTED
      * on receiving the SYN segment
      *
      * @param sockfd        The socket ID used to find the TCB entry in the TCB table (client)
      * @return              1 if sucessful, -1 if an error occurs
      */
    public int acceptSRTServer(int sockfd){
        System.out.println("[acceptSRTServer] Accepting SRT connection using " + sockfd);
        boolean found = false;
        try{
            for(int i=0; i<TCBtable.length; i++){
//                System.out.println("[acceptSRTServer] accept loop " + i);
                if(TCBtable[i] != null){
                    if( !found && TCBtable[i].nodeIDClient == sockfd){

                        found = true;
//                        System.out.println("[ acceptSRTServer] found sockfd");

                        //Socket connSock = serverSock.accept(); // accept connection to pass the new connection socket to a ListeningThread
                        //servSocks[i] = connSock;
//                        System.out.println(servSocks[i].getPort());
                        TCBtable[i].portNumClient = servSocks[i].getPort();     // set client port
//                        System.out.println(servSocks[i].getPort());
                        TCBtable[i].stateServer = TCBServer.LISTENING;             // set state to LISTENING

                        System.out.println("[acceptSRTServer] Set to listening state");
                        //start listening thread for SYN
//                        System.out.println("\n id: " + i + ", socket " + servSocks[i].toString());
                        boolean finRcved = false;


                        ListenThreadS newThread = new ListenThreadS(servSocks[i], sockfd, TCBtable[i]);
//                        System.out.println("[acceptSRTServer] new thread created for SYN");
                        lisThreads.add(newThread);                       //to keep track of any loose threads
//                        System.out.println("[acceptSRTServer] thread added to lisThreads list");

                        newThread.start();
                        System.out.println("[acceptSRTServer] New thread start: " + i);

                    }
                }
            }

            return 1;
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
        System.out.println("[closeSRTServer] Closing SRT connection");
        try{
            for(int i=0; i<TCBtable.length; i++){
                if(TCBtable[i] != null){
                    if(TCBtable[i].nodeIDClient == socksr){
                        if(TCBtable[i].stateServer== TCBServer.CLOSED){
                            servSocks[i].close();
                            TCBtable[i] = null;
//                            System.out.println("[closeSRTServer]  socket closed, table entry set to null");

                        }
                    }
                }

            }

            inputStreams.get(socksr).close();
            outputStreams.get(socksr).close();
//            System.out.println("[closeSRTServer]  input and output streams closed");
//            System.out.println("[closeSRTServer]  finished closing");
            return 1;
        }catch(IOException e){
            return -1;
        }
    }

    /**
     * The run() method handles incoming segments and
     * cancels the thread on receiving SYNs and FINs. The thread must also start a CLOSE WAIT
     * TIMEOUT timer on receiving FIN before changing state to CLOSED.
     */
    public class ListenThreadS extends Thread {
        private Socket connectionSocket;
        private ObjectInputStream input;
        private ObjectOutputStream output;
        private TCBServer TCBsr;
        private int clientID;
        private Boolean listening;
        private Timer timer;


        /**
         * Constructor for ListenThreadS:
         *
         * @param newSocket        The socket for this client.
         * @param sockfd           The unique ID assigned to this client.
         * @param blockSr          The TCB assigned to this client.
         */
        public ListenThreadS(Socket newSocket, int sockfd, TCBServer blockSr) {
            connectionSocket = newSocket;
            clientID = sockfd;
            TCBsr = blockSr;

            input = inputStreams.get(sockfd);
            output = outputStreams.get(sockfd);

        }

        /**
          * Main loop for this ListeningThread:
          * - Reads messages from the client.
          * - Ends after SYNACK or FINACK sent.
          */
        @Override
        public void run() {
            System.out.println("[ListeningThreadS] run: running ListenThreadS " + clientID);
            timer = new Timer();
            try {
                listening = true;
                while (listening) {
                    // Receive segment from the input
                    System.out.println("[ListeningThreadS] run: while look begin");

                    if(input != null){
                        System.out.println("[ListeningThreadS] input stream found");
                    }
                    Object received = input.readObject();
                    System.out.println("[ListeningThreadS] running: readobject() recived");

                    if (received instanceof Segment) {
                        Segment rcvd = (Segment) received;
                        System.out.println("[ListeningThreadS] run: Recived segment from client " + clientID);
                        Segment sendSeg;

                        if(rcvd.type == Segment.SYN){ //Syn
                            if(TCBsr.stateServer == TCBServer.LISTENING || TCBsr.stateServer == TCBServer.CONNECTED){
                                System.out.println("[ListeningThreadS] run: Recived type SYN, sending SYNACK");
                                listening = false;
                                //TCBsr.stateServer = CONNECTED;   handled in acceptSRTServer()
                                TCBsr.stateServer = TCBServer.CONNECTED;             // set state to CONNECTED

                                sendSeg = new Segment(Segment.SYNACK);
                                sendSegment(output, sendSeg);
                            }


                        }else if(rcvd.type == Segment.FIN){ //Fin
                            if(TCBsr.stateServer == TCBServer.CLOSEWAIT || TCBsr.stateServer == TCBServer.CONNECTED){
                                System.out.println("[ListeningThreadS] run: Recived Type FIN, sending FINACK");
                                TCBsr.stateServer = TCBServer.CLOSEWAIT;

                                sendSeg = new Segment(Segment.FINACK);
                                sendSegment(output, sendSeg);

                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        listening = false;
                                        TCBsr.stateServer = TCBServer.CLOSED;
                                    }
                                } , CLOSE_WAIT_TIMEOUT); // should reset timer when a new FIN is recived
                            }

                        }

                    }

                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println(e.getMessage());

                System.out.println("[ListeningThreadS] Client " + clientID + " disconnected.");
            } //closing and removing client is seperated into closeSRTServer
        }


        /*
         * sendSegment() - Send a segment of particular type to the server
         * @param socksr      The socket ID used to find the matching output stream
         * @param segment - send a segment object
         */
        public void sendSegment(ObjectOutputStream out, Segment segment){
            try {
                // Similar to other send methods, send the segment via the TCP sockets
                out.writeObject(segment);
                // Ouputstream sent the segment to the server
                out.flush();
            } catch (IOException e) {
                System.err.println("[ListeningThreadS sendSegment] Segment send failed: " + e.getMessage());
            }
        }
    }


}
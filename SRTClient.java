/*
 * CS 327 Proj 2
 *
 *
 * @author: Alix Sun, Elaine Zhou
 * @start date: 10/13/2025
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class SRTClient {
    /*TODO
 * - rename SRTClient
 * - fields for TCBClient //like a server handeler class
 *
 *      int nodeIDServer ; // node ID of server , similar as IP address
        int portNumServer ; // port number of server
        int nodeIDClient ; // node ID of client , similar as IP address
        int portNumClient ; // port number of client
        int stateClient ; // state of client
 *      /**
     * startOverlay() - Create a direct TCP link between the client and the server
     */
    public int startOverlay() throws IOException {
        // The server port to establish a TCP link
        int ServerPort = 59090;
        int socketDescriptor = 0;

        // Get the IP address of the client machine to connect to the server at given host:port
        String ip = InetAddress.getByName("localhost").getHostAddress();

        try {
            // Connect to the server at given host:port
            Socket clientSocket = new Socket(ip, ServerPort);
            // Assign a socket ID to the clientSocket
            socketDescriptor++;

            return socketDescriptor;


        } catch (UnknownHostException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            return -1;
        }

    }

    /**
     * createSockSRTClient() - Create a client side socket
     * @param client_port      The client port being set to
     *
     */
    public void createSockSRTClient(int client_port){



    }

    /**
     * connectSRTClient() - Connect to the server socket, sets up the TCB’s server port number
     * and a SYN segment to sent to the server
     * @param socksr      The socket ID used to find the TCB entry in the TCB table
     * @param server_port    The server’s port number
     *
     */
    public void connectSRTClient(int socksr, int server_port){

    }

    /**
     * disconnSRTClient() - Disconnects from the server after a wait period
     * @param socksr      The socket ID used to find the TCB entry in the TCB table
     *
     */
    public void disconnSRTClient(int socksr){

    }

    /**
     * closeSRTClient() - The client closes the socket, removes item from the TCB table and
     * returns 1 if succeeded and -1 if fails
     * @param socksr      The socket ID used to find the TCB entry in the TCB table
     */
    public void closeSRTClient(int socksr){

    }

    /**
     * stopOverlay() - Stop the overlay before terminating their processes
     *
     */
    public void stopOverlay(){

    }

    /**
     * The run() method handles incoming segments and cancels the thread on receiving
     * SYNACKs and FINACKs
     */
    public class ListenThread extends Thread {

    }
// * - methods
// *      - startOverlay{} // called in client?
// *      - initSRTClient{}
// *      - createSockSRTClient (int client port){}
// *      - connectSRTClient(int socksr, int server port){}
// *      - disconnSRTClient(int socksr){}
// *      - closeSRTClient(int socksr){}
// *      - class ListenThread extends Thread{}
// *
// * static variables
// *      SYN TIMEOUT: the number of milliseconds to wait for SYNACK before retransmitting SYN = 100 milliseconds
//        SYN MAX RETRY: the max number of SYN retransmissions in srt client connect() = 5
//        FIN TIMEOUT: the number of milliseconds to wait for FINACK before retransmitting FIN = 100 milliseconds
//        FIN MAX RETRY: the max number of FIN retransmissions in srt client disconnect() = 5
//// *
//// * for TCBClient
//// *      public final static int CLOSED = 1;
//// *      public final static int SYNSENT = 2;
//// *      public final static int CONNECTED = 3;
//// *      public final static int CLOSEWAIT = 4;
//// *
//// *
//// *
//// * main () {
////        // call the startOverlay method , throw error if it returns -1
////        // call the initSRTClient method , throw error if it returns -1
////        // create a srt client sock on port 87 using the createSockSRTClient (87)
////        and assign to socksr , throw error if it returns -1
////        // connect to srt server at port 88 using connectSRTClient ( socksr ,88) ,
////        throw error if it returns -1
////        // for now , just use a Thread . sleep (10000) here
////        // disconnect using disconnSRTClient ( socksr ), throw error if it returns -1
////        // close using closeSRTClient ( socksr ), throw error if it returns -1
////        Computer Networks 4 Prof. Prasad
////        Programming project 2 Due Date: Mar 21, 2023 11:59pm
////        // finally , call stopOverlay () , throw error if it returns -1
////    }
//// */
}

/*
 * CS 327 Proj 2
 *
 *
 * @author: Alix Sun, Elaine Zhou
 * @start date: 10/13/2025
 */



public class SRTServer {

    /**
     * startOverlay() - Create a direct TCP link between the client and the server
     *
     */
//    public void startOverlay(){
//        start();
//    }

    /**
     * createSockSRTServer() - Create a server side socket
     *
     */
    public void createSockSRTServer(){

    }

    /**
     * acceptSRTServer() - Accept a connection request from the client, gets the TCBServer
     * entry using the socksr and changes the state of the connection to LISTENING.
     * It starts the ListenThread to wait for SYNs, and changes the state to CONNECTED
     * on receiving the SYN segment
     * @param sockfd     The socket ID used to find the TCB entry in the TCB table
     */
    public void acceptSRTServer(int sockfd){

    }

    /**
     * closeSRTServer() - The server closes the socket, removes the TCB entry,
     * obtained using socksr. It returns 1 if succeeded (i.e., was in the right state
     * to complete a close) and -1 if fails (i.e., in the wrong state).
     * @param socksr    The socket ID used to find the TCB entry in the TCB table
     */
    public void closeSRTServer(int socksr){


    }

    /**
     * The run() method handles incoming segments and
     * cancels the thread on receiving SYNs and FINs. The thread must also start a CLOSE WAIT
     * TIMEOUT timer on receiving FIN before changing state to CLOSED.
     */
    public class ListenThread extends Thread {

    }






    /**
     * stopOverlay() - Stop the overlay before terminating their processes
     *
     */
    public void stopOverlay(){

    }






    /*TODO
 *
 * - rename SRTServer
 * - fields for TCBServer // like a client handeler class
 *
 *      int nodeIDServer ; // node ID of server , similar as IP address
        int portNumServer ; // port number of server
        int nodeIDClient ; // node ID of client , similar as IP address
        int portNumClient ; // port number of client
        int stateServer ; // state of server

 *
 * - methods
        startOverlay{} // called in server?
        initSRTServer{}
        acceptSRTServer(int sockfd){}
        closeSRTServer(int socksr){}
        public class ListenThread extends Thread{}

 *
 *
 * - statics
 *       CLOSE WAIT TIMEOUT = 1 second
 *
 * for TCBServer:
 *      public final static int CLOSED = 1;
 *      public final static int LISTENING = 2;
 *      public final static int CONNECTED = 3;
 *      public final static int CLOSEWAIT = 4;
 *
 * main () {
        // call the startOverlay method , throw error if it returns -1
        // call the initSRTServer () method , throw error if it returns -1
        // create a srt server sock at port 88 using the createSockSRTServer (88)
        and assign to socksr , throw error if it returns -1
        // connect to srt client using acceptSRTServer ( socksr ), throw error if it
        returns -1
        // for now , just use a Thread . sleep (10000) here
        // disconnect using closeSRTServer ( sockfd ), throw error if it returns -1
        // finally , call stopOverlay () , throw error if it returns -1
    }
 *
 */
}

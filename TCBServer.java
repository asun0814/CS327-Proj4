/**
 * CS 327 Proj 2
 * The SRT maintains all the state associated with a connection using a
 * Transport Control Block (TCB). For each connection the client and server side
 * initialize and maintain a TCB.
 * Therefore, the TCB class stores all connection-specific information and
 * current state for a single SRT connection.
 * @author: Alix Sun, Elaine Zhou
 * @start date: 10/15/2025
 */

public class TCBServer {
    //The TCBServer object should contain the following fields. The state can only take
    // the following static final values
    public static final int CLOSED = 1;
    public static final int LISTENING = 2;
    public static final int CONNECTED = 3;
    public static final int CLOSEWAIT = 4;


    public int nodeIDServer; //node ID of server, similar as IP address
    public int portNumServer; //port number of server
    public int nodeIDClient; //node ID of client, similar as IP address
    public int portNumClient; //port number of client
    public int stateServer; //state of server

    /**
     * TCB constructor
     * Initialize a TCB for each connection
     */
    public TCBServer(int serverNode, int serverPort, int clientNode, int clientPort, int state) {
        nodeIDServer = serverNode;
        portNumServer = serverPort;
        nodeIDClient = clientNode;
        portNumClient = clientPort;
        stateServer = CLOSED;

    }

    public TCBServer(){
        stateServer = CLOSED;
    }

}

/**
 * CS 327 Proj 2
 * The TCBClient class is a version of the TCB customized for the client side, tracking
 * its own states
 * @author: Alix Sun, Elaine Zhou
 * @start date: 10/15/2025
 */

public class TCBClient {
    //The TCBClient object should contain the following fields. The state can only take the following
    // static final values:
    public static final int CLOSED = 1;
    public static final int SYNSENT = 2;
    public static final int CONNECTED = 3;
    public static final int FINWAIT = 4;


    public int nodeIDServer; //node ID of server, similar as IP address
    public int portNumServer; //port number of server
    public int nodeIDClient; //node ID of client, similar as IP address
    public int portNumClient; //port number of client
    public int stateClient; //state of client

    /**
     * TCB constructor
     * Initialize a TCB for each connection, the initialized state is CLOSED
     */
    public TCBClient() {
        stateClient = CLOSED;

    }



}

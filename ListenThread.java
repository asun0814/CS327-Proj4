/**
 * CS 327 Proj 2
 * ListenThread class handles the segments received by the client
 * @author: Alix Sun, Elaine Zhou
 * @start date: 10/15/2025
 */


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * The run() method handles incoming segments and cancels the thread on receiving
 * SYNACKs and FINACKs
 * Technically works as a receive() method in the Client class
 */
public class ListenThread extends Thread {
    // Boolean used to cancel the running thread safely when necessary
    private boolean running = true;
    // Used to identify the specific TCB object in the table
    private int socketID;
    private SRTClient currentClient;
    // Input and output streams for the ListenThread, taken from the input, output streams hashmaps
    private ObjectOutputStream output;
    private ObjectInputStream input;
    // Specific TCB object processed in the thread
    private TCBClient tcb;
    private Socket connectSock;

    /**
     * Constructor for ListenThread
     */
    public ListenThread(int socksr, Socket connectionSocket, TCBClient newTCB, SRTClient newClient) throws IOException {
        System.out.println("[ListenThread] Listen thread " + socksr);
        socketID = socksr;
        connectSock = connectionSocket;
        tcb = newTCB;
        currentClient = newClient;
        // Get input and output stream from the specific TCP connection by getting it from the
        // input streams and output streams hashmap
        output = currentClient.outputStreams.get(socketID);
        input = currentClient.inputStreams.get(socketID);

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
                        currentClient.sendSegment(socketID, seg1);
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
                        currentClient.sendSegment(socketID, seg2);
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

}
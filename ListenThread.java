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
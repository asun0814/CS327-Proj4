/**
 * CS 327 Proj 2
 * Client class connects to the server, sends and receives messages, and manages the chat session
 *
 * @author: Alix Sun, Elaine Zhou
 * @start date: 10/15/2025
 */

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;

public class Client {
    /** TCP socket that represents the connection to the server */
    private Socket clientSocket;

    /** Output stream to send objects (Message or ACK/NAK) to the server */
    private ObjectOutputStream output;

    /** Input stream to receive objects (Message or ACK/NAK) from the server */
    private ObjectInputStream input;

    /** The name or ID of this client (used in the "from" field of Message) */
    private String clientName;

    /** Scanner to read user input from the terminal */
    private Scanner sc;

    /** Flag for if the client is in a chat with another client */
    Boolean ClientChatOpen;

    //private HashMap<Integer, ObjectInputStream> inputStreams = new HashMap<>();
    private ObjectInputStream[] inStreams;
    /** Input streams to receive segments from the server */
    //private HashMap<Integer, ObjectOutputStream> outputStreams = new HashMap<>();
    private ObjectOutputStream[] outStreams;

    /** Hashmap used to store the existing TCP connections and access these sockets via integer
     * TCP descriptors */
    private HashMap<Integer, Socket> TCPConnections = new HashMap<>();
    /**
     * Constructor: sets up a connection to the server and the input & output streams.
     *
     * @param host       The hostname or IP address of the server
     * @param port       The port number where the server is listening
     * @param newclientName The name or ID for this client
     */
    public Client(String host, int port, String newclientName) {
        // a main thread that sends the messages/ACK,NAK
        clientName = newclientName;
        try {
            // Connect to server at given host:port
            clientSocket = new Socket(host, port);

            // Create output and input streams
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());

            // Create a scanner for console input
            sc = new Scanner(System.in);

            System.out.println("Connected to server at " + host + ":" + port);

            // Second (background) thread to continuously receive messages
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    receive();
                }
            };

            new Thread(r).start();

            // Enter a loop to send messages typed by the user
            while(true){
                ClientChatOpen = false;
                String msgText = "";
                System.out.println("Please select a number option:");
                System.out.println("1. Say Hello To the Server \n 2. Ask for Server Status \n 3. Open chat with another Client \n 4. Quit");
              //  System.out.println("\n * Please enter the Chat state (by entering 3) before sending and receiving other" +
                //        " clients");
                int choice = sc.nextInt();

                sc.nextLine(); // consume newline

                switch (choice){
                    case 1:
                        msgText = "Hello server, how are you?";
                        System.out.println(msgText);

                        break;

                    case 2:
                        msgText = "What's the server status?";
                        System.out.println(msgText);
//                        // Send a SYN segment
//                        Segment SynSeg = new Segment(Segment.SYN);
//
//                        // Send a segment to server
//                        sendSegment(SynSeg);

                        break;

                    case 3:
                        ClientChatOpen = true;
                        // request the clients from server client handeler
                        Message listRequestMsg = new Message("Requesting client list", clientName, "Server");
                        System.out.println("Requesting client list from server...");
                        sendMessage(listRequestMsg);

                        // reciving thread should print the answering list from server in receiving thread
                        System.out.println("Select a client to contact:");
                        int chatPartnerID = sc.nextInt();
                        sc.nextLine();


                        System.out.println("Beginning chat with ClientID " + chatPartnerID + ":");
                        System.out.println("Enter 'quit' at anytime to exit \n");


                        Message joinMsg = new Message("A partner has entered the chat. To join them, please enter 3. Then enter their client ID", clientName, Integer.toString(chatPartnerID));
                        sendMessage(joinMsg);

                        // The chat protacol
                        while (ClientChatOpen) {
                            System.out.print("You: ");
                            msgText = sc.nextLine();

                            // User can type "quit" to exit the chat
                            if (msgText.equalsIgnoreCase("quit")) {
                                System.out.println("Would you like to exit this chat? Yes/No");
                                String quitInput = sc.nextLine();

                                if(quitInput.equalsIgnoreCase("yes")){
                                    msgText = "Partner has left the chat";
                                    ClientChatOpen = false;
                                    System.out.println("Exiting chat with " + chatPartnerID);
                                    break;
                                }

                            }

                            // Wrap user input into a Message object
                            Message toPartnerMsg = new Message(msgText, clientName, Integer.toString(chatPartnerID));

                            // Send message to server
                            sendMessage(toPartnerMsg);

                        }

                        break;

                    case 4:
                        msgText = "Quitting";
                        System.out.println(msgText);
                        close();
                        break;

                    default:
                        System.out.println("Invalid choice, try again.");
                        break;

                }

                if(choice != 3){
                    // Wrap user input into a Message object
                    Message msg = new Message(msgText, clientName, "Server");

                    // Send message to server
                    sendMessage(msg);
                }
            }
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }

    /**
     * startOverlay() - Create a direct TCP link between the client and the server
     * @return socketDescriptor    The TCP socket descriptor
     */
    public int startOverlay() throws IOException {
        // The server port to establish a TCP link
        int ServerPort = 59090;
        // Descriptor to access the matching socket
        int socketDescriptor = 0;


        // Get the IP address of the client machine to connect to the server at given host:port
        InetAddress ip = InetAddress.getByName("localhost");
        //.getHostAddress();

        try {

            // Connect to server at given host:port
            Socket newSocket = new Socket(ip, ServerPort);

            // Create output and input streams
            ObjectOutputStream out = new ObjectOutputStream(newSocket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(newSocket.getInputStream());


            System.out.println("1Connecting to " + ip + "...");
            // Assign a socket ID to the clientSocket
            System.out.println("Socket ID: " + socketDescriptor);
            socketDescriptor++;
            System.out.println("Socket ID: " + socketDescriptor);


            // Store the socketDescriptor and the matching TCP connection in a hashmap
            // In a similar way, store the input and output in two hashmaps along with the socket descripton
            TCPConnections.put(socketDescriptor, newSocket);
            System.out.println("TCP link created successfully, connected to server at " + ip + ":" + ServerPort);

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

            // Start listen thread
            new ListenThread(socketDescriptor, newSocket).start();
            System.out.println("Listen thread created");
            System.out.println("Listen thread started");

            return socketDescriptor;

        } catch (UnknownHostException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            return -1;
        }

    }




    /**
     * sendMessage() - Sends a Message object to the server.
     * Also automatically sends an ACK if successful or NAK if an error occurs.
     *
     * @param msg The Message object to be sent.
     */
    public void sendMessage(Message msg) {
        try {
            // write message to output stream
            output.writeObject(msg);
            // force send immediately
            output.flush();
            // send ACK back to server (self-confirmation)
            sendAckNak(true);
        } catch (IOException e) {
            // if failed, send NAK
            System.err.println("Send error: " + e.getMessage());
            sendAckNak(false);
        }
    }

    /**
     * sendAckNak() - Sends either an ACK (success) or NAK (failure) string
     * to the server to acknowledge a message or event.
     *
     * @param ack true = send "ACK", false = send "NAK".
     */
    public void sendAckNak(boolean ack) {
        try {
            // Create a new string("ACK" or "NAK") depending on the boolean parameter we got
            output.writeObject(ack ? "ACK" : "NAK");
            // Ouputstream sent the acknowledgement string to the server
            output.flush();
        } catch (IOException e) {
            System.err.println("ACK/NAK send failed: " + e.getMessage());
        }
    }

    /**
     * sendSegment() - Send a segment of particular type to the server
     * @param segment - send a segment object
     */
    public void sendSegment(Segment segment){
        try {
            // Similar to other send methods, send the segment via the TCP sockets
            output.writeObject(segment);
            // Ouputstream sent the segment to the server
            output.flush();
        } catch (IOException e) {
            System.err.println("Segment send failed: " + e.getMessage());
        }
    }



    /**
     * receive() - Continuously runs in a separate thread to listen for data
     * from the server. Handles both Message objects and ACK/NAK strings.
     */
    public void receive() {
        try {
            while (true) {
                // block until server sends something
                Object obj = input.readObject();

                if (obj instanceof Message) {
                    // If the server sent a Message, print it nicely
                    Message rcvdMsg = (Message) obj;

                    System.out.println("\n" + obj.toString());
                    System.out.print("You: "); // reprint input prompt
                } else if (obj instanceof String) {
                    // If the server sent ACK/NAK, print it
                    System.out.println("\n[Server reply] " + obj);
                    System.out.print("You: ");
                } else if (obj instanceof Segment) {
                    System.out.println("\n[Server reply] " + Segment.SYNACK);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Disconnected from server.");
        }
    }

    /**
     * stopOverlay() - Stop the overlay before terminating their processes
     *
     */
    public void stopOverlay() throws IOException {
        try {
            //Close all the sockets and clear the TCP connections map
            for (Socket socket : TCPConnections.values()) {
                socket.close();
            }
            TCPConnections.clear();
            System.out.println("Overlay stopped successfully, all connections closed.");
        } catch (IOException e) {
            System.err.println("Close error: " + e.getMessage());
        }
    }

    /**
     * close() - Closes the client by shutting down input/output
     * streams and closing the socket.
     */
    public void close() {
        try {
            sendMessage(new Message("Close", clientName, "Server"));
            if (input != null) input.close();
            if (output != null) output.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("Connection closed.");
        } catch (IOException e) {
            System.err.println("Close error: " + e.getMessage());
        }
    }

    /**
     * The run() method handles incoming segments and cancels the thread on receiving
     * SYNACKs and FINACKs
     * Technically works as a receive() method in the Client class
     */
    public static class ListenThread extends Thread {
        private int socketDescriptor;
        private Socket connectSock;

        /**
         * Constructor for ListenThread
         *
         */
        public ListenThread(int socksr, Socket connectionSocket) throws IOException {
            System.out.println("Listen thread 1");
            socketDescriptor = socksr;
            connectSock = connectionSocket;
        }

        @Override
        public void run() {
            try {
                ObjectInputStream input = new ObjectInputStream(connectSock.getInputStream());
                    while (!connectSock.isClosed()) {
                        Object obj = input.readObject();
                        System.out.println("[Overlay] Received on socket " + socketDescriptor + ": " + obj);
                    }
        } catch (IOException | ClassNotFoundException e) {
                System.err.println("[ListenThread] Listening Error: " + e.getMessage());
            }
        }

    /**
     * main() - Entry point to run the Client.
     * Example usage: java Client localhost 59090 Alice
     *
     * @param args Command-line arguments: host, port, clientName
     */
    public static void main(String[] args) throws IOException {
        int socksr = 0;
        String host = "localhost";
        int port = 59090;
        String clientName1 = "client1";
        Client client1 = new Client(host, port, clientName1);
        SRTClient srtClient = new SRTClient();


        client1.startOverlay();
        srtClient.initSRTClient();
//        System.out.println("Overlay result: "+ client1.startOverlay());

        srtClient.createSockSRTClient(87);
        srtClient.connectSRTClient(socksr, 88);
        System.out.println("srtClient.connectSRTClient result: "+ srtClient.connectSRTClient(socksr, 88));
        srtClient.disconnSRTClient(socksr);
        srtClient.closeSRTClient(socksr);
        client1.stopOverlay();







//        String host = "localhost";
//        int port = 59090;
//        String clientName1 = "client1";
//        String clientName2 = "client2";
//
//        Client client1 = new Client(host, port, clientName1);
//        Client client2 = new Client(host, port, clientName2);


    }

}}
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

    /** Output streams to send segments to the server */
    public HashMap<Integer, ObjectInputStream> inputStreams = new HashMap<>();
    //private ObjectInputStream[] inStreams;
    /** Input streams to receive segments from the server */
    public HashMap<Integer, ObjectOutputStream> outputStreams = new HashMap<>();
    //private ObjectOutputStream[] outStreams;

    /** Hashmap used to store the existing TCP connections and access these sockets via integer
     * TCP descriptors */
    private HashMap<Integer, Socket> TCPConnections = new HashMap<>();

    /**
     * Constructor: sets up a connection to the server and the input & output streams.
     */
    public Client() {

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

        try {

            // Connect to server at given host:port
            Socket newSocket = new Socket("localhost", ServerPort);

//            System.out.println("Output start");
            // Create output and input streams
            output = new ObjectOutputStream(newSocket.getOutputStream());
//            System.out.println("Output done");
//            System.out.println("input start");
            input = new ObjectInputStream(newSocket.getInputStream());
//            System.out.println("Input done");

            inputStreams.put(socketDescriptor, input);
            outputStreams.put(socketDescriptor, output);


            System.out.println("1Connecting to " + "localhost" + "...");
//            System.out.println("Socket ID: " + socketDescriptor);

            // Store the socketDescriptor and the matching TCP connection in a hashmap
            // In a similar way, store the input and output in two hashmaps along with the socket description
            TCPConnections.put(socketDescriptor, newSocket);
            System.out.println("TCP link created successfully, connected to server at " + "localhost" + ":" + ServerPort);

            socketDescriptor++;
            return socketDescriptor;

        } catch (UnknownHostException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            return -1;
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
     * main() - Entry point to run the Client.
     * Example usage: java Client localhost 59090 Alice
     *
     * @param args Command-line arguments: host, port, clientName
     */
    public static void main(String[] args) throws IOException {
        int socksr = 0;

        Client client1 = new Client();
        System.out.println("2");
        SRTClient srtClient = new SRTClient();
        System.out.println("3");

        System.out.println("overlay started ");
        client1.startOverlay();
        System.out.println("init started ");
        srtClient.initSRTClient(client1.TCPConnections, client1.inputStreams , client1.outputStreams);

        srtClient.createSockSRTClient(87);
        srtClient.connectSRTClient(socksr, 88);
        srtClient.disconnSRTClient(socksr);
        srtClient.closeSRTClient(socksr);
        client1.stopOverlay();

    }

}
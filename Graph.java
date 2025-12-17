import java.io.File;
import java.util.Scanner;

/**
 * Graph class that reads network.dat and computes shortest paths
 * using Dijkstra's algorithm.
 */


public class Graph {

    /**
     * Graph() - Load graph from network.dat, format per line: node1 node2 cost
     * @param filename to be loaded
     */
    public Graph(String filename) {
        Scanner keyboard = new Scanner(filename);
        while (keyboard.hasNext()) {
            int u = keyboard.nextInt();
            int v = keyboard.nextInt();
            int cost = keyboard.nextInt();


        }
        keyboard.close();
    }

    Graph g = new Graph("Network.dat");






}

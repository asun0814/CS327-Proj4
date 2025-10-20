/**
 * CS 327 Proj 2
 * The Segment class represents a message exchanged between the client and the server,
 * a Segment object should contain the header and the data portion
 * @author: Alix Sun, Elaine Zhou
 * @start date: 10/15/2025
 */

public class Segment {
    // The type is defined using the following static final values
    public static final int SYN = 0;
    public static final int SYNACK = 1;
    public static final int FIN = 2;
    public static final int FINACK = 3;
    public static final int DATA = 4;
    public static final int DATAACK = 5;

//    public int src_port; //currently not used
//    public int dest_port; //currently not used
//    public int seq_num; //currently not used
//    public int length; //currently not used
    public int type; //segment type

//    public int rcv_win; //currently not used
//    public int checksum; //currently not used
    public char data[MAX_SEG_LEN]; //currently not used

    /**
     * Segment constructor
     * @param newType     The segment's type to be initialized
     */
    public Segment(int newType) {
        type = newType;
        // what if input message is too long?
        if(message.length <= MAX_SEG_LEN){
            data = message;
        }else{
            //split array
            //make front into the data  (data = Arrays.copyOfRange(message,0,MAX_SEG_LEN);)
            //call constructor to make second segment data 2 = Arrays.copyOfRange(message,MAX_SEG_LEN, message.length)
            // update sequence # on data
            // calculate sequence on data2

            data = Arrays.copyOfRange(message,0, MAX_SEG_LEN); //split off front of message to fill Max Segment Length
            char[] data2 = Arrays.copyOfRange(message,MAX_SEG_LEN, message.length); // remaining data to send
        }
    }





}

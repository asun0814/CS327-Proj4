import java.io.Serializable;
import java.util.Date;

/*
 * CS 327 Proj 2
 * Message class to store text, sender, and recipient data
 * @author: Alix Sun, Elaine Zhou
 * @start date: 9/22/2025
 */
// This could be segment
public class Message implements Serializable {
    // Required for Serializable classes (good practice)
    private static final long serialVersionUID = 1L;

    // the message
    private String text;
    // Sender ID or name
    private String from;
    // Receiver ID or name
    private String to;
    // Timestamp of when the message was created
    private Date created;

    // Constructor
    public Message(String newText, String newFrom, String newTo) {
        text = newText;
        from = newFrom;
        to = newTo;
        created = new Date();
    }


    // Getters
    public String getText() {
        return text;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public Date getCreated() {
        return created;
    }

    // Setters
    public void setText(String text) {
        this.text = text;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    // toString method for printing the message being sent
    @Override
    public String toString() {
        return "[" + created + "] " + from + " → " + to + ": " + text;
    }
}




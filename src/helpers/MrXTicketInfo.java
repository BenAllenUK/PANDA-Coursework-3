package helpers;

import scotlandyard.Ticket;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by benallen on 28/04/15.
 */
public class MrXTicketInfo {
    /**
     * Get MrX's tickets
     * @return return a list of tickets used
     */
    public static List<Ticket> getTicketsUsed() {
        if(ticketsUsed == null){
            ticketsUsed = new LinkedList<>();
        }
        return ticketsUsed;
    }

    /**
     * Add a ticket to MrX's used tickets
     * @param ticket the ticket to add
     */
    public static void addTicketUsed(Ticket ticket){
        if(ticketsUsed == null){
            ticketsUsed = new LinkedList<>();
        }
        ticketsUsed.add(ticket);
    }

    private static List<Ticket> ticketsUsed;
}

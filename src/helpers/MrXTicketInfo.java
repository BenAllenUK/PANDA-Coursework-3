package helpers;

import scotlandyard.Ticket;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by benallen on 28/04/15.
 */
public class MrXTicketInfo {
    public static List<Ticket> getTicketsUsed() {
        if(ticketsUsed == null){
            ticketsUsed = new LinkedList<>();
        }
        return ticketsUsed;
    }

    public static void setTicketsUsed(List<Ticket> setUsed) {
        ticketsUsed = setUsed;
    }
    public static void addTicketUsed(Ticket ticket){
        if(ticketsUsed == null){
            ticketsUsed = new LinkedList<>();
        }
        ticketsUsed.add(ticket);
    }


    private static List<Ticket> ticketsUsed;
}

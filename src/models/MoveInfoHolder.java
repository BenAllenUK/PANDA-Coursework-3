package models;

import scotlandyard.Move;
import scotlandyard.MoveTicket;
import scotlandyard.Ticket;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by benallen on 06/04/15.
 */
public class MoveInfoHolder {
    public Move move;
    public HashMap<ScoreElements, Float> scores;
    public Set<Move> movesFromHere;
    public HashMap<Ticket, Integer> ticketNumbers;
    public MoveInfoHolder(Move possibleMove, HashMap<ScoreElements, Float> currentScores, Set<Move> movesFromHere, HashMap<Ticket, Integer> ticketNumbers){
        this.move = possibleMove;
        this.scores = currentScores;
        this.movesFromHere = movesFromHere;
        this.ticketNumbers = ticketNumbers;
    }
}

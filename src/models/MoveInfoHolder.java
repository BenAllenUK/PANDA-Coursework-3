package models;

import scotlandyard.Colour;
import scotlandyard.Move;
import scotlandyard.Ticket;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by benallen on 06/04/15.
 */
public class MoveInfoHolder {
    public Move move;
    public HashMap<ScoreElement, Float> scores;
    public Set<Move> movesFromHere;
    public HashMap<Colour, HashMap<Ticket, Integer>> ticketNumbers;
    public HashMap<Colour, Integer> playerPositions;
	public MoveInfoHolder nextMoveHolder;
    public MoveInfoHolder(Move possibleMove, HashMap<ScoreElement, Float> currentScores, Set<Move> movesFromHere, HashMap<Colour, HashMap<Ticket, Integer>> ticketNumbers, HashMap<Colour, Integer> playerPositions){
        this.move = possibleMove;
        this.scores = currentScores;
        this.movesFromHere = movesFromHere;
        this.ticketNumbers = ticketNumbers;
		this.playerPositions = playerPositions;
    }

	@Override
	public String toString() {
		return "MoveInfoHolder{" +
				"move=" + move +
				", scores=" + scores +
				'}';
	}
}

package models;

import scotlandyard.Move;
import scotlandyard.MoveDouble;
import scotlandyard.MoveTicket;
import scotlandyard.Ticket;

/**
 * Created by benallen on 23/04/15.
 */
public class MoveDetails {
    private Move move;
    private Ticket ticket1;
    private Ticket ticket2;
    private int endTarget;

    public MoveDetails(Move move) {
        this.move = move;
        invoke();
    }

    public Ticket getTicket1() {
        return ticket1;
    }

    public Ticket getTicket2() {
        return ticket2;
    }

    public int getEndTarget() {
        return endTarget;
    }

	public Move getMove() {
		return move;
	}

	private MoveDetails invoke() {
        final boolean isDouble = move instanceof MoveDouble;
        if(isDouble){
            ticket1 = ((MoveDouble)move).move1.ticket;
            ticket2 = ((MoveDouble)move).move2.ticket;
            endTarget = ((MoveDouble)move).move2.target;
        } else {
            ticket1 = ((MoveTicket)move).ticket;
            ticket2 = null;
            endTarget = ((MoveTicket)move).target;
        }
        return this;
    }

	public MoveDetails clone(){
		MoveDetails clone = new MoveDetails(move);
		return clone;
	}

	@Override
	public String toString() {
		return "MoveDetails{" +
				"move=" + move +
				'}';
	}
}
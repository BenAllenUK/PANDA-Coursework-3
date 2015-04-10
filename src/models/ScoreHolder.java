package models;

import scotlandyard.Move;
import scotlandyard.MoveTicket;

import java.util.HashMap;

/**
 * Created by benallen on 06/04/15.
 */
public class ScoreHolder {
    public Move move;
    public HashMap<ScoreElements, Float> scores;
    public ScoreHolder(Move possibleMove, HashMap<ScoreElements, Float> currentScore){
        this.move = possibleMove;
        this.scores = currentScore;
    }
}

package MachineLearning;

import player.MyAIPlayer;
import scotlandyard.Colour;
import scotlandyard.Move;
import scotlandyard.Spectator;
import scotlandyard.Ticket;
import solution.ScotlandYardModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rory on 28/04/15.
 */
public class GameInstance {
	private final ScotlandYardModel game;

	public GameInstance(Gene gene){
		gene.apply();

		List<Boolean> rounds = Arrays.asList(
				false,
				false, false,
				true,
				false, false, false, false,
				true,
				false, false, false, false,
				true,
				false, false, false, false,
				true,
				false, false, false, false, false,
				true);

		List<Integer> xStartPosArray = Arrays.asList(172, 170, 71, 104, 166, 51, 78, 35, 146, 127, 106, 45, 132);
		List<Integer> dStartPosArray = Arrays.asList(112, 141, 94, 34, 29, 123, 117, 138, 174, 26, 103, 53, 50, 91, 13, 155);

		Collections.shuffle(xStartPosArray);
		Collections.shuffle(dStartPosArray);

		Map<Colour, Integer> locations = new HashMap<Colour, Integer>();

		locations.put(Colour.Red, dStartPosArray.get(0));
		locations.put(Colour.Blue, dStartPosArray.get(1));
		locations.put(Colour.Green, dStartPosArray.get(2));
		locations.put(Colour.White, dStartPosArray.get(3));
		locations.put(Colour.Yellow, dStartPosArray.get(4));
		locations.put(Colour.Black, xStartPosArray.get(0));

		Map<Colour, Map<Ticket, Integer>> tickets = new HashMap<Colour, Map<Ticket, Integer>>();
		tickets.put(Colour.Red, getTickets(false));
		tickets.put(Colour.Blue, getTickets(false));
		tickets.put(Colour.Green, getTickets(false));
		tickets.put(Colour.Yellow, getTickets(false));
		tickets.put(Colour.White, getTickets(false));
		tickets.put(Colour.Black, getTickets(true));

		final String graphFile = "resources/graph.txt";
		game = new ScotlandYardModel(5, rounds, graphFile);

		for (HashMap.Entry<Colour, Integer> entry : locations.entrySet()) {

			MyAIPlayer player = new MyAIPlayer(game, graphFile);

			game.join(player, entry.getKey(), entry.getValue(), tickets.get(entry.getKey()));
		}

		game.spectate(new Spectator() {
			@Override
			public void notify(final Move move) {
				System.out.println("Round: "+game.getRound());
				synchronized (game){
					game.notifyAll();
				}
			}
		});

	}

	public GameResult start(){

		if(game.isReady()){
			game.start();
		}else{
			System.err.println("Could not start game");
			return new GameResult(true, 0);
		}

		while(!game.isGameOver()){
			synchronized (game){
				try {
					game.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		if(game.getWinningPlayers().size() > 1){
			System.out.println("Detectives won");
		}else{
			System.out.println("Mr X won");
		}

		return new GameResult(false, game.getRound());

	}

	public final static int[] mrXTicketNumbers = {4, 3, 3, 2, 5};
	public final static int[] detectiveTicketNumbers = {11, 8, 4, 0, 0};

	private static Map<Ticket, Integer> getTickets(boolean mrX) {
		Map<Ticket, Integer> tickets = new HashMap<Ticket, Integer>();
		int count = 0;
		for (Ticket ticket : Ticket.values()) {
			if (mrX)
				tickets.put(ticket, mrXTicketNumbers[count]);
			else
				tickets.put(ticket, detectiveTicketNumbers[count]);

			count++;
		}
		return tickets;
	}
}

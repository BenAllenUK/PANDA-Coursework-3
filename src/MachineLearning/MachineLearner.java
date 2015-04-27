package MachineLearning;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by rory on 27/04/15.
 */
public class MachineLearner {
	private static final int GENE_TESTS = 6;
	private GenePool mGenePool;
	private String newLine = System.getProperty("line.separator");

	public MachineLearner(final int poolNumber) {


		//load in current gene source
		loadGene(poolNumber);

		startTesting();
	}

	private void loadGene(final int poolNumber) {

		File geneFile = new File("genePools/genePool"+poolNumber);

		if (geneFile.exists()) {
			mGenePool = GenePool.load(geneFile);
		} else {
			mGenePool = GenePool.newRandomPool();
			mGenePool.save();
		}
	}

	private void startTesting() {

		while(true) {
			ArrayList<Gene> orderedGeneList = new ArrayList<Gene>();

			for (Gene gene : mGenePool.getGenes()) {
				while (gene.getTestCount() < GENE_TESTS) {

					System.out.println(newLine + newLine + newLine + newLine + newLine + newLine);
					System.out.println("testing round " + gene.getTestCount() + " " + gene);
					System.out.println(newLine + newLine + newLine + newLine + newLine + newLine);

					final GameResult result = playGame(gene);
					if (result == null || result.isError()) {
						System.err.println("null result returned, will retry");
					} else {
						System.out.println("Round ended with score " + result.getRound());
						gene.incrementScore(result.getRound());
						gene.incrementTestCount();
					}
				}

				if (orderedGeneList.size() == 0) {
					orderedGeneList.add(gene);
				} else {
					for (int i = 0; i < orderedGeneList.size(); i++) {
						if (gene.getScore() < orderedGeneList.get(i).getScore()) {
							orderedGeneList.add(i, gene);
							break;
						} else if (i == orderedGeneList.size() - 1) {
							orderedGeneList.add(gene);
						} else {
							continue;
						}
					}
				}

				mGenePool.save();
			}

			System.out.println(newLine + newLine + newLine + newLine + newLine + newLine);
			System.out.println("finished testing generation " + mGenePool.generation);
			System.out.println("bestGene = " + orderedGeneList.get(orderedGeneList.size() - 1));
			System.out.println(newLine + newLine + newLine + newLine + newLine + newLine);

			mGenePool.regenerateFrom(orderedGeneList);

			mGenePool.save();
		}
	}

	private GameResult playGame(final Gene gene) {

		gene.apply();

		try {
			return new GameInstance().startGame();
		} catch (IOException e) {
			return null;
		}
	}
}

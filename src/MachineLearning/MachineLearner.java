package MachineLearning;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by rory on 27/04/15.
 */
public class MachineLearner {
	private static final int GENE_TESTS = 3;
	private GenePool mGenePool;
	private String newLine = System.getProperty("line.separator");
	private GameInstance currentGameInstance;

	public MachineLearner(final int poolNumber) {


		//load in current gene source
		loadGene(poolNumber);

		startTesting();
	}

	private void loadGene(final int poolNumber) {

		File folder = new File("genePools");

		File geneFile = new File("genePools/genePool"+poolNumber);

		if(poolNumber == -1){
			int curGeneration = -1;
			for(File file : folder.listFiles()){
				final String fileName = file.getName();
				if(fileName.contains("genePool")){
					final int generation = Integer.parseInt(fileName.substring("genePool".length(), fileName.length()));
					if(!geneFile.exists() || curGeneration == -1 || curGeneration < generation){
						curGeneration = generation;
						geneFile = file;
					}
				}
			}
		}


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
						mGenePool.save();
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
							break;
						} else {
							continue;
						}
					}
				}


			}

			System.out.println(newLine + newLine + newLine + newLine + newLine + newLine);
			System.out.println("finished testing generation " + mGenePool.getGeneration());
			System.out.println("bestGene = " + orderedGeneList.get(orderedGeneList.size() - 1));
			System.out.println(newLine + newLine + newLine + newLine + newLine + newLine);

			mGenePool.regenerateFrom(orderedGeneList);

			mGenePool.save();
		}
	}

	private GameResult playGame(final Gene gene) {

		gene.apply();

		try {

			currentGameInstance = new GameInstance();
			final GameResult gameResult = currentGameInstance.startGame();
			return gameResult;
		} catch (IOException e) {
			return null;
		}
	}
}

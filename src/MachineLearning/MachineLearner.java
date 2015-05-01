package MachineLearning;

import helpers.Constants;

import java.io.File;
import java.util.ArrayList;

/**
 * Runs multiple instances of games synchronously through multiple generations to determine the best score weighting
 */
public class MachineLearner {
	private final String NEW_LINE = System.getProperty("line.separator");
	private GenePool mGenePool;

	public MachineLearner(final int poolNumber) {

		ensureGenePoolDir();

		//load in current gene source
		loadGene(poolNumber);

		startTesting();
	}

	private void ensureGenePoolDir() {
		File dir = new File("genePools");

		if(!dir.exists()){
			if(!dir.mkdir()){
				System.err.println("Could not make genepool dir");
			}
		}
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

	/**
	 * Tests the loaded {@link GenePool}'s genes, forever
	 */
	private void startTesting() {


		while(true) {
			ArrayList<Gene> orderedGeneList = new ArrayList<Gene>();

			final ArrayList<Gene> genes = mGenePool.getGenes();
			for (int i1 = 0; i1 < genes.size(); i1++) {
				final Gene gene = genes.get(i1);
				while (gene.getTestCount() < Constants.GENE_TESTS) {

					System.out.println(NEW_LINE + NEW_LINE + NEW_LINE + NEW_LINE + NEW_LINE + NEW_LINE);
					System.out.println("testing generation: " + mGenePool.getGeneration() + " gene: " + i1 + " round: " + gene.getTestCount() + " gene: " + gene);
					System.out.println(NEW_LINE + NEW_LINE + NEW_LINE + NEW_LINE + NEW_LINE + NEW_LINE);

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

			System.out.println(NEW_LINE + NEW_LINE + NEW_LINE + NEW_LINE + NEW_LINE + NEW_LINE);
			System.out.println("finished testing generation " + mGenePool.getGeneration());
			System.out.println("bestGene = " + orderedGeneList.get(orderedGeneList.size() - 1));
			System.out.println(NEW_LINE + NEW_LINE + NEW_LINE + NEW_LINE + NEW_LINE + NEW_LINE);

			mGenePool.regenerateFrom(orderedGeneList);

			mGenePool.save();
		}
	}

	private GameResult playGame(final Gene gene) {

		final GameInstance currentGameInstance = new GameInstance(gene);
		return currentGameInstance.start();
	}
}
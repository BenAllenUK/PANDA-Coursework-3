package MachineLearning;

import helpers.Constants;
import helpers.ScorerHelper;
import helpers.StaticConstants;

import java.util.Arrays;
import java.util.Random;

/**
 * A Gene represents a weight configuration, used in {@link ScorerHelper#score}
 */
public class Gene {

	private double[] chromosome = new double[7];

	private static int MEAN_DIST_WEIGHT_INDEX = 0;
	private static int MOVE_WEIGHT_INDEX = 1;
	private static int SECRET_MOVE_WEIGHT_INDEX = 2;
	private static int VISIBLE_ROUND_WEIGHT_INDEX = 3;
	private static int INVISIBLE_ROUND_WEIGHT_INDEX = 4;
	private static int SD_DIST_WEIGHT_INDEX = 5;
	private static int BOAT_WEIGHT_INDEX = 6;

	private int testCount;
	private int score;

	public double[] getChromosome() {
		return chromosome;
	}

	public void setChromosome(final double[] chromosome) {
		this.chromosome = chromosome;
	}

	public int getTestCount() {
		return testCount;
	}

	public void incrementTestCount() {
		this.testCount++;
	}

	public int getScore() {
		return score;
	}

	public void incrementScore(final int score) {
		this.score += score;
	}

	public static Gene newRandom() {
		Gene gene = new Gene();

		Random random = new Random();

		double[] chromosome = gene.getChromosome();
		chromosome[MEAN_DIST_WEIGHT_INDEX] = random.nextInt(Constants.GENE_INITIAL_BOUND);
		chromosome[MOVE_WEIGHT_INDEX] = random.nextInt(Constants.GENE_INITIAL_BOUND);
		chromosome[SECRET_MOVE_WEIGHT_INDEX] = random.nextInt(Constants.GENE_INITIAL_BOUND);
		chromosome[VISIBLE_ROUND_WEIGHT_INDEX] = random.nextInt(Constants.GENE_INITIAL_BOUND);
		chromosome[INVISIBLE_ROUND_WEIGHT_INDEX] = random.nextInt(Constants.GENE_INITIAL_BOUND);
		chromosome[SD_DIST_WEIGHT_INDEX] = random.nextInt(Constants.GENE_INITIAL_BOUND);
		chromosome[BOAT_WEIGHT_INDEX] = random.nextInt(Constants.GENE_INITIAL_BOUND);

		gene.setChromosome(chromosome);
		return gene;
	}

	public void apply(){
		StaticConstants.MEAN_DIST_WEIGHT = chromosome[MEAN_DIST_WEIGHT_INDEX];
		StaticConstants.MOVE_WEIGHT = chromosome[MOVE_WEIGHT_INDEX];
		StaticConstants.SECRET_MOVE_WEIGHT = chromosome[SECRET_MOVE_WEIGHT_INDEX];
		StaticConstants.VISIBLE_ROUND_WEIGHT = chromosome[VISIBLE_ROUND_WEIGHT_INDEX];
		StaticConstants.INVISIBLE_ROUND_WEIGHT = chromosome[INVISIBLE_ROUND_WEIGHT_INDEX];
		StaticConstants.SD_DIST_WEIGHT = chromosome[SD_DIST_WEIGHT_INDEX];
		StaticConstants.BOAT_WEIGHT = chromosome[BOAT_WEIGHT_INDEX];
	}

	@Override
	public String toString() {
		return "Gene{" +
				"chromosome=" + Arrays.toString(chromosome) +
				", score=" + score +
				'}';
	}

	public void setScore(final int score) {
		this.score = score;
	}

	public void setTestCount(final int testCount) {
		this.testCount = testCount;
	}
}
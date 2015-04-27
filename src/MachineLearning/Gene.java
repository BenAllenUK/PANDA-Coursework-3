package MachineLearning;

import helpers.ScorerHelper;

import java.util.Random;

/**
 * Created by rory on 27/04/15.
 */
public class Gene {
	private static final int RAND_BOUND = 40;
	private double MEAN_DIST_WEIGHT;
	private double MOVE_WEIGHT;
	private double SECRET_MOVE_WEIGHT;
	private double VISIBLE_ROUND_WEIGHT;
	private double INVISIBLE_ROUND_WEIGHT;
	private double SD_DIST_WEIGHT;
	private double BOAT_WEIGHT;

	private int testCount;
	private int score;

	public double getMEAN_DIST_WEIGHT() {
		return MEAN_DIST_WEIGHT;
	}

	public void setMEAN_DIST_WEIGHT(final double MEAN_DIST_WEIGHT) {
		this.MEAN_DIST_WEIGHT = MEAN_DIST_WEIGHT;
	}

	public double getMOVE_WEIGHT() {
		return MOVE_WEIGHT;
	}

	public void setMOVE_WEIGHT(final double MOVE_WEIGHT) {
		this.MOVE_WEIGHT = MOVE_WEIGHT;
	}

	public double getSECRET_MOVE_WEIGHT() {
		return SECRET_MOVE_WEIGHT;
	}

	public void setSECRET_MOVE_WEIGHT(final double SECRET_MOVE_WEIGHT) {
		this.SECRET_MOVE_WEIGHT = SECRET_MOVE_WEIGHT;
	}

	public double getVISIBLE_ROUND_WEIGHT() {
		return VISIBLE_ROUND_WEIGHT;
	}

	public void setVISIBLE_ROUND_WEIGHT(final double VISIBLE_ROUND_WEIGHT) {
		this.VISIBLE_ROUND_WEIGHT = VISIBLE_ROUND_WEIGHT;
	}

	public double getINVISIBLE_ROUND_WEIGHT() {
		return INVISIBLE_ROUND_WEIGHT;
	}

	public void setINVISIBLE_ROUND_WEIGHT(final double INVISIBLE_ROUND_WEIGHT) {
		this.INVISIBLE_ROUND_WEIGHT = INVISIBLE_ROUND_WEIGHT;
	}

	public double getSD_DIST_WEIGHT() {
		return SD_DIST_WEIGHT;
	}

	public void setSD_DIST_WEIGHT(final double SD_DIST_WEIGHT) {
		this.SD_DIST_WEIGHT = SD_DIST_WEIGHT;
	}

	public double getBOAT_WEIGHT() {
		return BOAT_WEIGHT;
	}

	public void setBOAT_WEIGHT(final double BOAT_WEIGHT) {
		this.BOAT_WEIGHT = BOAT_WEIGHT;
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

		gene.setMEAN_DIST_WEIGHT(random.nextInt(RAND_BOUND));
		gene.setMOVE_WEIGHT(random.nextInt(RAND_BOUND));
		gene.setSECRET_MOVE_WEIGHT(random.nextInt(RAND_BOUND));
		gene.setVISIBLE_ROUND_WEIGHT(random.nextInt(RAND_BOUND));
		gene.setINVISIBLE_ROUND_WEIGHT(random.nextInt(RAND_BOUND));
		gene.setSD_DIST_WEIGHT(random.nextInt(RAND_BOUND));
		gene.setBOAT_WEIGHT(random.nextInt(RAND_BOUND));

		return gene;
	}

	public void apply(){
		ScorerHelper.MEAN_DIST_WEIGHT = MEAN_DIST_WEIGHT;
		ScorerHelper.MOVE_WEIGHT = MOVE_WEIGHT;
		ScorerHelper.SECRET_MOVE_WEIGHT = SECRET_MOVE_WEIGHT;
		ScorerHelper.VISIBLE_ROUND_WEIGHT = VISIBLE_ROUND_WEIGHT;
		ScorerHelper.INVISIBLE_ROUND_WEIGHT = INVISIBLE_ROUND_WEIGHT;
		ScorerHelper.SD_DIST_WEIGHT = SD_DIST_WEIGHT;
		ScorerHelper.BOAT_WEIGHT = BOAT_WEIGHT;
	}

	@Override
	public String toString() {
		return "Gene{" +
				"MEAN_DIST_WEIGHT=" + MEAN_DIST_WEIGHT +
				", MOVE_WEIGHT=" + MOVE_WEIGHT +
				", SECRET_MOVE_WEIGHT=" + SECRET_MOVE_WEIGHT +
				", VISIBLE_ROUND_WEIGHT=" + VISIBLE_ROUND_WEIGHT +
				", INVISIBLE_ROUND_WEIGHT=" + INVISIBLE_ROUND_WEIGHT +
				", SD_DIST_WEIGHT=" + SD_DIST_WEIGHT +
				", BOAT_WEIGHT=" + BOAT_WEIGHT +
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

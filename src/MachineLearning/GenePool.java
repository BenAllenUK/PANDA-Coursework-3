package MachineLearning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by rory on 27/04/15.
 */
public class GenePool {
	private static final int POOL_SIZE = 30;
	private static final int PARENT_POOL_SIZE = 3;
	private ArrayList<Gene> geneList;
	private int generation;

	public GenePool() {
		geneList = new ArrayList<Gene>();
		generation = 1;
	}


	public static GenePool newRandomPool() {
		GenePool genePool = new GenePool();

		for (int i = 0; i < POOL_SIZE; i++) {
			genePool.geneList.add(Gene.newRandom());
		}

		return genePool;
	}

	public static GenePool load(final File file) {

		GenePool genePool = new GenePool();

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			int geneLineIndex = 0;
			Gene gene = null;
			int index = 0;
			for (String line; (line = br.readLine()) != null; ) {
				if (index == 0) {
					genePool.generation = Integer.parseInt(line);
					index++;
					continue;
				}

				if (line.equals("*")) {
					geneLineIndex = 0;
					gene = new Gene();
					genePool.geneList.add(gene);
					continue;
				}
				if (gene != null) {
					switch (geneLineIndex) {
						case 0:
							gene.setScore(Integer.parseInt(line));
							break;
						case 1:
							gene.setTestCount(Integer.parseInt(line));
							break;
						case 2:
							gene.setMEAN_DIST_WEIGHT(Double.parseDouble(line));
							break;
						case 3:
							gene.setMOVE_WEIGHT(Double.parseDouble(line));
							break;
						case 4:
							gene.setSECRET_MOVE_WEIGHT(Double.parseDouble(line));
							break;
						case 5:
							gene.setVISIBLE_ROUND_WEIGHT(Double.parseDouble(line));
							break;
						case 6:
							gene.setINVISIBLE_ROUND_WEIGHT(Double.parseDouble(line));
							break;
						case 7:
							gene.setSD_DIST_WEIGHT(Double.parseDouble(line));
							break;
						case 8:
							gene.setBOAT_WEIGHT(Double.parseDouble(line));
							break;
					}
					geneLineIndex++;
				}
				index++;
			}

			// line is not visible here.
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return genePool;
	}

	public void save() {

		File file = new File("genePools/genePool"+generation);

		try {
			final PrintWriter writer = new PrintWriter(file, "UTF-8");

			writer.println(generation);

			for (Gene gene : geneList) {
				writer.println("*");
				writer.println(gene.getScore());
				writer.println(gene.getTestCount());
				writer.println(gene.getMEAN_DIST_WEIGHT());
				writer.println(gene.getMOVE_WEIGHT());
				writer.println(gene.getSECRET_MOVE_WEIGHT());
				writer.println(gene.getVISIBLE_ROUND_WEIGHT());
				writer.println(gene.getINVISIBLE_ROUND_WEIGHT());
				writer.println(gene.getSD_DIST_WEIGHT());
				writer.println(gene.getBOAT_WEIGHT());
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<Gene> getGenes() {
		return geneList;
	}

	public void regenerateFrom(final ArrayList<Gene> orderedGenes) {
		geneList.clear();

		generation++;

		double mdwMean = 0;
		double mwMean = 0;
		double smwMean = 0;
		double vrwMean = 0;
		double irwMean = 0;
		double sdwMean = 0;
		double bwMean = 0;

		double mdwSd = 0;
		double mwSd = 0;
		double smwSd = 0;
		double vrwSd = 0;
		double irwSd = 0;
		double sdwSd = 0;
		double bwSd = 0;

		for (int i = orderedGenes.size() - PARENT_POOL_SIZE; i < orderedGenes.size(); i++) {
			mdwMean += orderedGenes.get(i).getMEAN_DIST_WEIGHT() / (float) PARENT_POOL_SIZE;
			mwMean += orderedGenes.get(i).getMOVE_WEIGHT() / (float) PARENT_POOL_SIZE;
			smwMean += orderedGenes.get(i).getSECRET_MOVE_WEIGHT() / (float) PARENT_POOL_SIZE;
			vrwMean += orderedGenes.get(i).getVISIBLE_ROUND_WEIGHT() / (float) PARENT_POOL_SIZE;
			irwMean += orderedGenes.get(i).getINVISIBLE_ROUND_WEIGHT() / (float) PARENT_POOL_SIZE;
			sdwMean += orderedGenes.get(i).getSD_DIST_WEIGHT() / (float) PARENT_POOL_SIZE;
			bwMean += orderedGenes.get(i).getBOAT_WEIGHT() / (float) PARENT_POOL_SIZE;
		}

		for (int i = orderedGenes.size() - PARENT_POOL_SIZE; i < orderedGenes.size(); i++) {
			mdwSd += ((mdwMean - orderedGenes.get(i).getMEAN_DIST_WEIGHT()) * (mdwMean - orderedGenes.get(i).getMEAN_DIST_WEIGHT())) / (float) PARENT_POOL_SIZE;
			mwSd += ((mwMean - orderedGenes.get(i).getMOVE_WEIGHT()) * (mwMean - orderedGenes.get(i).getMOVE_WEIGHT())) / (float) PARENT_POOL_SIZE;
			smwSd += ((smwMean - orderedGenes.get(i).getSECRET_MOVE_WEIGHT()) * (smwMean - orderedGenes.get(i).getSECRET_MOVE_WEIGHT())) / (float) PARENT_POOL_SIZE;
			vrwSd += ((vrwMean - orderedGenes.get(i).getVISIBLE_ROUND_WEIGHT()) * (vrwMean - orderedGenes.get(i).getVISIBLE_ROUND_WEIGHT())) / (float) PARENT_POOL_SIZE;
			irwSd += ((irwMean - orderedGenes.get(i).getINVISIBLE_ROUND_WEIGHT()) * (irwMean - orderedGenes.get(i).getINVISIBLE_ROUND_WEIGHT())) / (float) PARENT_POOL_SIZE;
			sdwSd += ((sdwMean - orderedGenes.get(i).getSD_DIST_WEIGHT()) * (sdwMean - orderedGenes.get(i).getSD_DIST_WEIGHT())) / (float) PARENT_POOL_SIZE;
			bwSd += ((bwMean - orderedGenes.get(i).getBOAT_WEIGHT()) * (bwMean - orderedGenes.get(i).getBOAT_WEIGHT())) / (float) PARENT_POOL_SIZE;
		}

		mdwSd = Math.sqrt(mdwSd);
		mdwSd = Math.sqrt(mdwSd);
		mdwSd = Math.sqrt(mdwSd);
		mdwSd = Math.sqrt(mdwSd);
		mdwSd = Math.sqrt(mdwSd);
		mdwSd = Math.sqrt(mdwSd);

		final float randFactor = 2f;
		final float sdFactor = 2f;

		for (int i = 0; i < POOL_SIZE; i++) {

			Gene newGene = new Gene();

			Random random = new Random();

			newGene.setMEAN_DIST_WEIGHT(mdwMean + (mdwSd * 2 * random.nextDouble() - mdwSd)*sdFactor + (random.nextInt(2)-1)*mdwMean/ randFactor);
			newGene.setMOVE_WEIGHT(mwMean + (mwSd * 2 * random.nextDouble() - mwSd)*sdFactor + (random.nextInt(2)-1)*mwMean/ randFactor);
			newGene.setSECRET_MOVE_WEIGHT(smwMean + (smwSd * 2 * random.nextDouble() - smwSd)*sdFactor + (random.nextInt(2)-1)*smwMean/ randFactor);
			newGene.setVISIBLE_ROUND_WEIGHT(vrwMean + (vrwSd * 2 * random.nextDouble() - vrwSd)*sdFactor + (random.nextInt(2)-1)*vrwMean/ randFactor);
			newGene.setINVISIBLE_ROUND_WEIGHT(irwMean + (irwSd * 2 * random.nextDouble() - irwSd)*sdFactor + (random.nextInt(2)-1)*irwMean/ randFactor);
			newGene.setSD_DIST_WEIGHT(sdwMean + (sdwSd * 2 * random.nextDouble() - sdwSd)*sdFactor + (random.nextInt(2)-1)*sdwMean/ randFactor);
			newGene.setBOAT_WEIGHT(bwMean + (bwSd * 2 * random.nextDouble() - bwSd)*sdFactor + (random.nextInt(2)-1)*bwMean/ randFactor);

			geneList.add(newGene);
		}
	}

	public int getGeneration() {
		return generation;
	}
}

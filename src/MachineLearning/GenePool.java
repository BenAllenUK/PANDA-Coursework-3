package MachineLearning;

import helpers.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

/**
 * Created by rory on 27/04/15.
 */
public class GenePool {
	private ArrayList<Gene> geneList;
	private int generation;

	public GenePool() {
		geneList = new ArrayList<Gene>();
		generation = 1;
	}


	public static GenePool newRandomPool() {
		GenePool genePool = new GenePool();

		for (int i = 0; i < Constants.GENE_POOL_SIZE; i++) {
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
				if(gene != null) {
					switch (geneLineIndex) {
						case 0:
							gene.setScore(Integer.parseInt(line));
							break;
						case 1:
							gene.setTestCount(Integer.parseInt(line));
							break;
						case 2:

							final String[] strings = line.split(" ");
							double[] chromosome = new double[strings.length];

							for (int i = 0; i < strings.length; i++) {
								chromosome[i] = Double.parseDouble(strings[i]);
							}

							gene.setChromosome(chromosome);

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

		//saves to genePools
		File file = new File("genePools/genePool"+generation);

		try {
			final PrintWriter writer = new PrintWriter(file, "UTF-8");

			writer.println(generation);



			for (Gene gene : geneList) {

				String chromosome = String.valueOf(gene.getChromosome()[0]);


				for (int i = 1; i < gene.getChromosome().length; i++) {
					chromosome += " "+String.valueOf(gene.getChromosome()[i]);
				}

				writer.println("*");
				writer.println(gene.getScore());
				writer.println(gene.getTestCount());
				writer.println(chromosome);
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

	/**
	 * Generates a new {@link GenePool} from a list of genes ordered from lowest score to highest.
	 * It uses 2 parents to generate the new pool
	 *
	 * @param orderedGenes a list of ordered genes from low score to high
	 */
	public void regenerateFrom(final ArrayList<Gene> orderedGenes) {
		geneList.clear();

		generation++;

		Collections.reverse(orderedGenes);

		Iterator<Gene> iterator = orderedGenes.iterator();

		Gene firstParent = iterator.next();
		Gene secondParent = iterator.next();

		Random random = new Random();

		for (int i = 0; i < Constants.GENE_POOL_SIZE; i++) {
			Gene gene = new Gene();


			final int chromosomeLength = firstParent.getChromosome().length;

			int crossoverPoint1 = random.nextInt(chromosomeLength);
			int crossoverPoint2 = random.nextInt(chromosomeLength);

			double[] chromosome = new double[chromosomeLength];

			for (int j = 0; j < chromosomeLength; j++) {


				//mutation decreases as we progress through generations
				boolean mutate = random.nextInt(Math.round((generation*generation)/(float)(Constants.GENE_POOL_SIZE * 6) + 2)) == 0;

				//Using two-point crossover
				if(j < Math.min(crossoverPoint1, crossoverPoint2)){
					chromosome[j] = firstParent.getChromosome()[j];
				}else if(j < Math.max(crossoverPoint1, crossoverPoint2)){
					chromosome[j] = secondParent.getChromosome()[j];
				}else{
					chromosome[j] = firstParent.getChromosome()[j];
				}

				if(mutate){
					chromosome[j] += ((random.nextBoolean() ? 1 : -1) * chromosome[j] / 5f);
				}

			}

			gene.setChromosome(chromosome);
			geneList.add(gene);

		}
	}

	public int getGeneration() {
		return generation;
	}
}
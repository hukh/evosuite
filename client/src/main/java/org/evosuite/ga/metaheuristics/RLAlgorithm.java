package org.evosuite.ga.metaheuristics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.evosuite.utils.LoggingUtils;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

public class RLAlgorithm {

	private static int numberOfOption;

	public RLAlgorithm(int num) {
		numberOfOption = num;

		LCF_SM = new LinearCombination_SoftMaxPolicy(0.5, 0.1);
		LC_EpP = new LinearCombination_EpslonGreedy(0.4, 0.1);
		
		
		selected = new int[numberOfOption];
		rwd = new double[numberOfOption];
		mu = new double[numberOfOption];
		s = new double[numberOfOption];
		sigma = new double[numberOfOption];
		mean = new double[numberOfOption];
		numbers_of_selections = new int[numberOfOption];
		sums_of_rewards = new double[numberOfOption];
		init();

		/*
		 * try { wrt = new PrintWriter(new FileOutputStream(new File("D:\\trace.txt"),
		 * true)); } catch (FileNotFoundException e1) { e1.printStackTrace(); }
		 */
	}

	final List<Integer> shuffled_options = new ArrayList<Integer>();

	private void init() {

		for (int j = 0; j < numberOfOption; j++) {
			shuffled_options.add(j);
		}
		Collections.shuffle(shuffled_options);
		//
		// for (int j = 0; j < 65; j++ ) {
		// LoggingUtils.getEvoLogger().info("shuffled_optionse " + j + " " +
		// shuffled_options.get(j));
		// }

	}

	/////////// TS ///////////////////

	/*
	 * Decision Making Using Thompson Sampling, Mellor, Joseph, 2004, page 69
	 */
	private NormalDistribution nd = new NormalDistribution();
	private WeibullDistribution td = new WeibullDistribution(2, 50);
	private Variance var = new Variance();

	private double mu[];
	private double s[];
	private double sigma[];
	private double mean[];

	private double varTotal = 0;
	private double meanTotal = 0;
	/*
	 * public NormalDistribution getNd() { return nd; }
	 * 
	 * public void setNd(NormalDistribution nd) { this.nd = nd; }
	 */

	public double[] getMu() {
		return mu;
	}

	public void setMu(double[] mu) {
		this.mu = mu;
	}
	
	/*
	 * public Variance getVar() { return var; }
	 * 
	 * public void setVar(Variance var) { this.var = var; }
	 */
	public double[] getS() {
		return s;
	}

	public void setS(double[] s) {
		this.s = s;
	}

	public double[] getSigma() {
		return sigma;
	}

	public void setSigma(double[] sigma) {
		this.sigma = sigma;
	}

	public double[] getMean() {
		return mean;
	}

	public void setMean(double[] mean) {
		this.mean = mean;
	}

	public double getVarTotal() {
		return varTotal;
	}

	public void setVarTotal(double varTotal) {
		this.varTotal = varTotal;
	}

	public double getMeanTotal() {
		return meanTotal;
	}

	public void setMeanTotal(double meanTotal) {
		this.meanTotal = meanTotal;
	}

	public int TS_part1() {
		int ad = 0;
		double max_random = 0; // for TS
		for (int j = 0; j < numberOfOption; j++) {
			int ind = shuffled_options.get(j);
			double random_gauss = 0;
			if (numbers_of_selections[ind] > 0) {
				// nd = new NormalDistribution(mu[j], s[j]);
				td = new WeibullDistribution(1, 5);
				random_gauss = td.sample();
			} else
				random_gauss = Double.MAX_VALUE;
			if (random_gauss > max_random) {
				max_random = random_gauss;
				ad = ind;
			}
		}
		return ad;
	}
	//the variance calculated using the link:
		// https://math.stackexchange.com/questions/775391/can-i-calculate-the-new-standard-deviation-when-adding-a-value-without-knowing-t
		//
	public void TS_part2(int counter, int ad, double reward_score) {
		ad_selected.add(ad);
		numbers_of_selections[ad] = numbers_of_selections[ad] + 1;
		sums_of_rewards[ad] = sums_of_rewards[ad] + reward_score;

		// calculate the variance for all reward
		double newMeanTotal = (meanTotal * (counter - 1) + reward_score) / counter;
		varTotal = ((counter - 1) * varTotal + (reward_score - newMeanTotal) * (reward_score - meanTotal)) / counter;
		meanTotal = newMeanTotal;
		if (counter < numberOfOption) { // first round the mu=reward and the s=1
			mu[ad] = reward_score;
			s[ad] = 1;
			sigma[ad] = 1;
		} else {
			int ns = numbers_of_selections[ad];
			sigma[ad] = ((ns - 1) * sigma[ad] + (reward_score - newMeanTotal) * (reward_score - mu[ad])) / ns;

			mu[ad] = ((mu[ad] * varTotal) + (reward_score * sigma[ad])) / (varTotal + sigma[ad]);
			s[ad] = (s[ad] * varTotal) / (s[ad] * varTotal);
		}
	}

	///////////////////////////////// UBC

	private static int numbers_of_selections[];
	private static double sums_of_rewards[];
	private static ArrayList<Integer> ad_selected = new ArrayList<Integer>();
	private static double total_reward = 0;
	private static double previou_reward = 0;

	public static double[] getSums_of_rewards() {
		return sums_of_rewards;
	}

	private double someIdea(int counter, int ad) {

		// calculate the variance for all reward
		double newMeanTotal = (meanTotal * (counter - 1) + previou_reward) / counter;
		varTotal = ((counter - 1) * varTotal + (previou_reward - newMeanTotal) * (previou_reward - meanTotal))
				/ counter;
		meanTotal = newMeanTotal;
		if (counter < numberOfOption) { // first round the mu=reward and the s=1
			mu[ad] = previou_reward;
			s[ad] = 1;
			// sigma[ad] = 1;
		} else {
			int ns = numbers_of_selections[ad];
			sigma[ad] = ((ns - 1) * sigma[ad] + (previou_reward - newMeanTotal) * (previou_reward - mu[ad])) / ns;

			mu[ad] = ((mu[ad] * varTotal) + (previou_reward * s[ad])) / (varTotal + s[ad]);
			s[ad] = (s[ad] * varTotal) / (s[ad] * varTotal);
		}
		return mu[ad];
	}

	public int UCB_part1(int counter) {

		int ad = 0;
		double max_lower_bound = 0;

		if (counter < numberOfOption) {
			ad = shuffled_options.get(counter);
		}
		for (int j = 0; j < numberOfOption; j++) {
			double upper_bound = 0;
			int ind = shuffled_options.get(j);
			if (numbers_of_selections[ind] > 0) {
				double average_reward = sums_of_rewards[ind] / (double) numbers_of_selections[ind];
				// 10 is the best for the c
				double delta_j = ((double) 1 / 100)
						* (Math.sqrt(Math.log((double) (counter)) / (double) numbers_of_selections[ind]));
				// double delta_j = (((double) 1/5)
				// * Math.sqrt(Math.log((double) (counter)) / (double)
				// numbers_of_selections[ind]));
				// double delta_j = ((double) 1/10) * (Math.log((double) (counter)) / (double)
				// numbers_of_selections[j]);
//				 upper_bound = average_reward + delta_j;
				upper_bound = someIdea(counter, j) + delta_j;
			} else
				upper_bound = Double.MAX_VALUE;
			if (upper_bound > max_lower_bound) {
				max_lower_bound = upper_bound;
				ad = ind;
			}
		}

		ad_selected.add(ad);
		numbers_of_selections[ad] = numbers_of_selections[ad] + 1;
		return ad;
	}

	public void UCB_part2(double reward_score, int ad) {
		sums_of_rewards[ad] = sums_of_rewards[ad] + reward_score;
		total_reward = total_reward + reward_score;
		previou_reward = reward_score;
	}

	//////////////////// DSGSarsa ////////////////////////////

	LinearCombination_SoftMaxPolicy LCF_SM;
//	RBF_SoftMaxPolicy rbf = new RBF_SoftMaxPolicy();
	LinearCombination_EpslonGreedy	LC_EpP;
	
	
	private int current_action = 0;
	private double current_coverage;
	private int current_size;
	private int current_numOfCoveredGoal;
	private double current_fitness;
	private int current_foundGoal;

	public int getCurrent_foundGoal() {
		return current_foundGoal;
	}

	public void setCurrent_foundGoal(int current_foundGoal) {
		this.current_foundGoal = current_foundGoal;
	}

	Random rand = new Random();
	private int[] selected;
	private int step = 30;

	private int getAction(int counter, int coverage, int size, int	numOfCoveredGoal, int fitness, int newFoundGoals) {

		int action = -1;
		if (counter < numberOfOption) {
			action = shuffled_options.get(counter);
			selected[action] += 1;
			LC_EpP.stateValue((int) coverage * 1000, size, numOfCoveredGoal, (int) fitness, newFoundGoals);
		} else {
			if (counter % step == 0) {
				step = (step > 4) ? (step - 1) : 4;
				// double[] values = value_function.stateValue(coverage, size,numOfCoveredGoal,	 // fitness, newFoundGoals);
				double[] values = LC_EpP.stateValue((int) coverage , size, numOfCoveredGoal, fitness, newFoundGoals);
				action = (int) values[1];
				selected[action] += 1;
			} else {
				action = Math.abs(rand.nextInt(numberOfOption));
				selected[action] += 1;
			}
		}
		return action;
	}
	//
	//

	private int getAction_softmax(int counter, double coverage, double numOfCoveredGoal, double fitness,
			double newFoundGoals) {

		int action = -1;
		if (counter < numberOfOption) {
			action = shuffled_options.get(counter);
			selected[action] += 1;
			
		} else {
			 if (counter % step == 0) {
			 step = (step > 2) ? (step - 1) : 2;
			 action = LCF_SM.stateValue(coverage , numOfCoveredGoal, fitness, newFoundGoals);
			 selected[action] += 1;
			 } else {
			action = LCF_SM.SoftMaxPolicy(coverage, numOfCoveredGoal, fitness, newFoundGoals);
//			action = rbf.SoftMaxPolicy(counter, coverage, numOfCoveredGoal, fitness, newFoundGoals);
			selected[action] += 1;
			 }
		}
		return action;
	}

	public int getCurrent_action() {
		return current_action;
	}

	public void setCurrent_action(int current_action) {
		this.current_action = current_action;
	}

	double[] rwd;

	
	/*
	 * Linear combination function with softmax 
	 */
	public void DSGSarsa_part2_LCF_SM(int counter, double newCoverage, int newSize, int newNumOfCoveredGoal, double newFitness,
			int newFoundGoals, double reward_score) {
		
		// This used with linear combination of feature
		double cc = (double) Math.round(current_coverage * 1000) / 10000;
		double nc = (double) Math.round(newCoverage * 1000) / 10000;

		double cf =  (double) Math.round(current_fitness) / 1000;
		double nf =  (double) Math.round(newFitness) / 1000;

		double ccg = (double) Math.round(current_numOfCoveredGoal) / 10000;
		double ncg = (double) Math.round(newNumOfCoveredGoal) / 10000;

		double cfg = (double) Math.round(current_foundGoal) / 10000;
		double nfg = (double) Math.round(newFoundGoals) / 10000;

		int newAction = getAction_softmax(counter, nc, ncg, nf, nfg);
		numbers_of_selections[newAction] += 1;

		LCF_SM.learn(cc, ccg, cf, cfg, current_action, nc, ncg, nf, nfg, newAction, reward_score);
//		rbf.learn(counter, cc, ccg, cf, cfg, current_action, nc, ncg, nf, nfg, newAction, reward_score);

		rwd[getCurrent_action()] += reward_score;
		current_coverage = newCoverage;
		current_size = newSize;
		current_numOfCoveredGoal = newNumOfCoveredGoal;
		current_fitness = newFitness;
		setCurrent_action(newAction);
	}
	
	public double DSGSarsa_part2_LC_EpsG(int counter, double newCoverage, int newSize, int newNumOfCoveredGoal, double newFitness,
			int newFoundGoals, double reward_score) {
		
		int newAction = getAction(counter, (int)newCoverage*100, newSize, newNumOfCoveredGoal, (int)newFitness, newFoundGoals);
		numbers_of_selections[newAction] += 1;

		double qValue = LC_EpP.learn((int)current_coverage*100, current_size, current_numOfCoveredGoal, (int)current_fitness, current_foundGoal, current_action, 
				(int)newCoverage*100, newSize, newNumOfCoveredGoal, (int)newFitness, newFoundGoals, newAction, (int)reward_score);

		rwd[getCurrent_action()] += reward_score;
		current_coverage = newCoverage;
		current_size = newSize;
		current_numOfCoveredGoal = newNumOfCoveredGoal;
		current_fitness = newFitness;
		setCurrent_action(newAction);
		
		return qValue;
	}

	// return the final estimation for the best option that the system recognize
	public int DSGSarsa_FinalEstimation(double newCoverage, int newSize, int newNumOfCoveredGoal, double newFitness,
			int newFoundGoals) {

		// double[] val = value_function.stateValue(newCoverage, newSize,
		// newNumOfCoveredGoal, newFitness, newFoundGoals);

		// double[] val = value_function.stateValue((int) newCoverage * 1000,
		// newNumOfCoveredGoal, (int) newFitness,
		// newFoundGoals);

		double nc = (double) Math.round(newCoverage * 1000) / 1000;
		double nf = (double) Math.round(newFitness * 100) / 100;
		double ncg = (double) Math.round(newNumOfCoveredGoal * 1000) / 1000;
		double nfg = (double) Math.round(newFoundGoals * 1000) / 1000;

		int val = LCF_SM.SoftMaxPolicy(nc, ncg, nf, nfg);
		

		// double[] val =value_function.printStatesValues(newCoverage, newSize,
		// newNumOfCoveredGoal, newFitness, newFoundGoals);
		LoggingUtils.getEvoLogger().info("  max " + val);
		return val;
	}

	public void prt() {

		// int ms = 0;
		// int mr = 0;
		// for (int i = 0; i < numberOfOption; i++) {
		// LoggingUtils.getEvoLogger().info("selected[" + i + "] " + selected[i] + " -
		// reward " + rwd[i]);
		// if (selected[i] > selected[ms])
		// ms = i;
		// if (rwd[i] > rwd[mr])
		// mr = i;
		// }
		// LoggingUtils.getEvoLogger().info(" Max selected " + ms + " - max reward " +
		// mr);

		for (int i = 0; i < numberOfOption; i++) {
			LoggingUtils.getEvoLogger().info("selected[" + i + "]," + numbers_of_selections[i]);
		}

	}

	public void saveWeights() {
		try {

			FileWriter fileWriter = new FileWriter("weight.txt");
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			for (int i = 0; i < numberOfOption; i++) {
				double[] w = LCF_SM.Weights_Action.get(i);
				String row = String.valueOf(w[0]);

				for (int j = 1; j < w.length; j++) {
					row += "," + String.valueOf(w[j]);
				}
				bufferedWriter.write(row);
				bufferedWriter.newLine();

			}
			bufferedWriter.close();
		} catch (IOException ex) {

		}
	}

	public void setWeights() {
		try {
			FileReader fileReader = new FileReader("weight.txt");
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line = null;
			int index = 0;
			while ((line = bufferedReader.readLine()) != null) {
				String[] valStr = line.split(",");
				double[] w = new double[valStr.length];
				for (int j = 0; j < w.length; j++) {
					w[j] = Double.valueOf(valStr[j])/100;
				}
				LCF_SM.Weights_Action.put(index, w);
				index++;
			}
			bufferedReader.close();
		} catch (IOException ex) {

		}
	}

}

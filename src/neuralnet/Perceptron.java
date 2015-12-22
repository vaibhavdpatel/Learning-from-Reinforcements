package neuralnet;

import java.io.PrintWriter;
import java.util.Random;

public class Perceptron {

	double weights[];
	private boolean debugging = false;
	int dir, numOfInputUnits, numOfOutputUnits;
	double learningRate, lambda = 0;


	/**
	 * Constructor
	 * 
	 * @param numOfInputUnits
	 * @param dir
	 * @param randomizeWeights
	 * @param learningRate
	 */
	public Perceptron(int numOfInputUnits, int dir, boolean randomizeWeights,
			double learningRate) {
		this(numOfInputUnits, dir, randomizeWeights, 0.1, learningRate);
	}

	public double[] getWeights() {
		return weights;
	}

	public void setWeights(double[] weights) {
		this.weights = weights;
	}

	/**
	 * dir = 0-left, 1-up, 2-right, 3-down
	 * 
	 * @param numOfInputUnits
	 * @param dir
	 * @param randomizeWeights
	 * @param defaultWeight
	 * @param learningRate
	 */
	public Perceptron(int numOfInputUnits, int dir, boolean randomizeWeights,
			double defaultWeight, double learningRate) {
		this.numOfInputUnits = numOfInputUnits;
		this.dir = dir;
		this.numOfOutputUnits = 1;
		this.learningRate = learningRate;

		weights = new double[numOfInputUnits+1];

		if(randomizeWeights == true) {
			Random r = new Random();
			int sign = 0;

			for(int i=0; i < numOfInputUnits +1; i++) {

				sign = r.nextInt()%2;
				weights[i] = r.nextDouble();

				weights[i] = (sign == 1) ? weights[i]*1 : weights[i]*-1;
				//System.out.print(weights[i] + " ");
			} 

		} else {
			for(int i=0; i < numOfInputUnits+1; i++) {
				weights[i] = defaultWeight;
			}
		}
	}

	public double predict (double input[]) {

		double net = weights[0];
		int size = input.length;

		if(debugging) {
			System.out.print("Weights  ");
			for(int i = 0; i < size+1; i++)
				System.out.print(weights[i] + " ");
			System.out.println();
		}

		for(int i = 0; i < size; i++) {
			net += input[i] * weights[i+1];
		}

		//net = 1/(1 + Math.exp(-net));
		if(debugging) {
			System.out.println("Net: " + net + ", should have returned: " +  Double.min(Double.max(net,-2), 5));
		}
		//return Double.max(net,-1);
		return net;
	}

	public void backPropagate(double input[], double y, double out) {

		if(debugging) {
			System.out.println("While training, Out value: " + out + ", y value: " + y);
		}

		double temp = (out - y) * learningRate  ;
		//temp *= out * (1 - out);
		weights[0] -= temp;

		for(int i = 0; i < numOfInputUnits; i++) {
			weights[i+1] -= (temp * input[i] + lambda * weights[i+1] );
		}
	}
	
	public void writeWeightsToFile(PrintWriter writer) {
		
		if(writer == null) 
			return;
		
		writer.print(dir + " ");
		
		for(int i = 0; i < weights.length; i++) {
			writer.print(weights[i] + " ");
		}
		
		writer.print("\n");
		
	}

}

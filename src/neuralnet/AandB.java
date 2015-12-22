package neuralnet;

import java.util.Random;

public class AandB {

	private Perceptron perceptron;
	double[][] inputs = new double[16][4];
	double[] outputs = new double[16];

	public AandB () {
		perceptron = new Perceptron(4, 0, false, 0, 0.2);

		for(int i = 0; i < 16; i ++) {
			inputs[i][3] = i%2;
			inputs[i][2] = i%4 / 2;
			inputs[i][1] = i%8 / 4;
			inputs[i][0] = i/ 8;

			if(inputs[i][0] == 0 || inputs[i][1] == 0) 
				outputs[i] = 0;
			else {
				outputs[i] = 1;
			}
		}
		/*
		for(int i = 0; i < 16; i ++) {
			System.out.println(inputs[i][0] + " " + inputs[i][1] + " " + inputs[i][2] + " " + inputs[i][3] + " " + outputs[i]);
		}*/
	}

	public void train() {

		Random r = new Random();

		int i ;
		for(int j = 0; j < 30000; j++) {

			i = r.nextInt(15);
			System.out.println(i);
			double predictedVal = perceptron.predict(inputs[i]);
			perceptron.backPropagate(inputs[i], outputs[i], predictedVal);
			System.out.println("######");
		}
		
		for(i=0; i < 16; i++) {
			double predictedVal = perceptron.predict(inputs[i]);
			System.out.println("Pred " + predictedVal + ", exp. " + outputs[i]);
			
		}

	}

	public static void main(String[] args) {
		AandB aAndB = new AandB();

		aAndB.train();
	}

}

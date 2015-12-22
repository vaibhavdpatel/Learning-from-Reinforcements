import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Random;

import neuralnet.Perceptron;

//
// VaibhavPlayer - A smart player trained using ANNs.
//

public final class VaibhavPlayer extends Player
{ 
	private Sensors           sensors;
	private double            reward;
	private Perceptron[]      perceptrons;
	private boolean debugging   = false;
	public static Color myColor = Color.yellow;
	int numOfPerceptrons        = Sensors.NUMBER_OF_SENSORS;
	int numOfInputs             = Sensors.NUMBER_OF_SENSORS * 4;
	double learningRate         = 0.2;
	double gamma                = 0.5;
	long numberOfStepsTaken     = 0;
	boolean playingFirstTime    = false;
	String WEIGHTS_FILE_NAME    = "weights.txt";
	boolean onlyWallSeenInRange = true;

	int[] stepsTaken            = new int[10];
	int stepsTakenInd           = -1;

	VaibhavPlayer(AgentWindow agentWindow)
	{
		this(agentWindow, false);
	}

	VaibhavPlayer(AgentWindow agentWindow, boolean showSensors)
	{
		super(agentWindow);
		setShowSensors(showSensors); // Display this player's sensors?

		perceptrons = new Perceptron [numOfPerceptrons];

		for(int i=0; i < numOfPerceptrons; i++) {
			perceptrons[i] = new Perceptron(numOfInputs, i, false, 0.3, learningRate);
		}
	}

	/**
	 * This method reads the stored weights of ANNs
	 * from a file given as an input.
	 * 
	 * @param fileName
	 */
	void readWeightsFromFile(String fileName) {
		FileReader file = null;
		try {
			file = new FileReader(fileName);
		} catch (FileNotFoundException e) {
			System.out.println("Exception occurred while opening the file - " + fileName);
			e.printStackTrace();
			return;
		}

		BufferedReader buffReader = new BufferedReader(file);
		String line;

		try {
			int dir=-1, i;
			double[] weights = null;
			while((line = buffReader.readLine()) != null) {

				i=0;
				for(String part : line.split("\\s+")) {
					if(dir == -1) {
						dir = Integer.valueOf(part);
						weights = perceptrons[dir].getWeights();

					} else{
						weights[i++] = Double.valueOf(part);
					}
				}
				dir = -1;
			}
		} catch (IOException e) {
			System.out.println("Exception occurred while reading file");
			e.printStackTrace();
		} finally {
			try {
				if(buffReader != null)
					buffReader.close();

				if(file != null) {
					file.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method writes the weights of all the ANNs
	 * to a file given as parameter.
	 * 
	 * @param fileName
	 */
	void writeWeightsToFile(String fileName) {

		PrintWriter writer = null;

		try {
			writer = new PrintWriter(fileName, "UTF-8");

			for(int i = 0; i < numOfPerceptrons; i++) {
				perceptrons[i].writeWeightsToFile(writer);
			}

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return;
		} finally {
			if(writer != null) {
				writer.close();
			}
		}
	}

	/* (non-Javadoc)
	 * @see Player#run()
	 */
	public void run()
	{ 
		double radians = 0.0;

		try
		{
			int maxPredictedValueIndex = -1, bestPredictedIndex;
			double[] predictedValues, savedOldValues = null;
			double[] oldSensorValues = new double[numOfInputs];
			double requiredQValue = 0.0;
			double nextQvalue = 0.0;
			double bestPredictedOrigValue = 0.0;
			int writtenWeightsToFile = 0;

			// get env. details
			double[] sensorValues = this.getSensorValues();

			for(int i = 0; i < stepsTaken.length; i++) {
				stepsTaken[i] = -1;
			}

			if(playingFirstTime == false) {
				readWeightsFromFile(WEIGHTS_FILE_NAME);
			}

			// Predict Q value
			predictedValues = predictQValuesInAllDirections(sensorValues);

			savedOldValues = new double[predictedValues.length];

			normalizePredictedValues(predictedValues, savedOldValues);

			// Find direction
			maxPredictedValueIndex = getMaximumOfAllPredictedValues(predictedValues);

			while(threadAlive()) // Basically, loop forever.
			{
				bestPredictedIndex = maxPredictedValueIndex;
				bestPredictedOrigValue = savedOldValues[bestPredictedIndex];

				double degree = 360 - ((double)360/numOfPerceptrons) *maxPredictedValueIndex;

				radians = Utils.convertToRadians(degree);

				if(debugging) {
					System.out.println("Prev degree: " + ((double)360/numOfPerceptrons) *maxPredictedValueIndex +
							"  Degree: " + degree + "\n Radians: " + radians);
				}

				setMoveVector(Utils.convertToPositiveRadians(radians));
				numberOfStepsTaken++;
				writtenWeightsToFile++;

				if(writtenWeightsToFile > 1000) {

					// Commented as the weights are already adjusted
					//writeWeightsToFile(WEIGHTS_FILE_NAME);
					writtenWeightsToFile = 0;
				}

				reward = getReward();

				if(debugging) {
					System.out.println("Reward: " + reward + 
							"\n=======================================================\n");
				}

				System.arraycopy(sensorValues, 0, oldSensorValues, 0, sensorValues.length);
				sensorValues = this.getSensorValues();

				// Predict Q value
				predictedValues = predictQValuesInAllDirections(sensorValues);
				normalizePredictedValues(predictedValues, savedOldValues);
				
				// find direction
				maxPredictedValueIndex = getMaximumOfAllPredictedValues(predictedValues);
				nextQvalue = predictedValues[maxPredictedValueIndex];
				requiredQValue = reward + gamma * nextQvalue;

				if(debugging) {
					System.out.println("Required (y): " + requiredQValue);
				}

				perceptrons[bestPredictedIndex].backPropagate(oldSensorValues, 
						requiredQValue, bestPredictedOrigValue);

			}	

		}
		catch(Exception e)
		{
			Utils.errorMsg(e + ", " + toString() + " has stopped running");
		}

		Utils.errorMsg(getName() + " is no longer alive for some reason");
		// This should never happen, so exit the simulator.
		Utils.exit(-1);
	}

	/**
	 * This method returns the what the sensors see 
	 * in each direction.
	 * 
	 * @return
	 */
	double[] getSensorValues() {

		double[] sensorValues = new double[numOfInputs];
		double distance;

		for(int i = 0; i < numOfInputs; i++) {
			sensorValues[i] = 0;
		}

		sensors     = getSensors();     // See what the world looks like.

		// Look around.  However, if received a punishment on last 
		// step toward a vegetable, make a random move.
		for(int dir = 0; dir < Sensors.NUMBER_OF_SENSORS; dir++)
		{ 
			switch (sensors.getObjectType(dir)) {

			case Sensors.WALL:
				distance = sensors.getDistance(dir);
				sensorValues[Sensors.NUMBER_OF_SENSORS*0 + dir] = 109/(13*distance);
				break;

			case Sensors.VEGETABLE:
				distance = sensors.getDistance(dir);
				onlyWallSeenInRange = false;
				sensorValues[Sensors.NUMBER_OF_SENSORS*1 + dir] = 109/(13*distance);
				break;

			case Sensors.ANIMAL:
				distance = sensors.getDistance(dir);
				onlyWallSeenInRange = false;
				sensorValues[Sensors.NUMBER_OF_SENSORS*2 + dir] = 109/(13*distance);
				break;

			case Sensors.MINERAL:
				distance = sensors.getDistance(dir);
				onlyWallSeenInRange = false;
				sensorValues[Sensors.NUMBER_OF_SENSORS*3 + dir] = 109/(13*distance);
				break;

			}
		}
		return sensorValues;
	}

	/**
	 * This method calls the predict function of each perceptron
	 * by passing the input features to them.
	 * 
	 * @param input
	 * @return
	 */
	double[] predictQValuesInAllDirections (double input[]) {

		double[] predictedValues = new double[numOfPerceptrons];

		if(debugging) {
			System.out.print("Inputs: ");

			for(int i=0; i < input.length; i++) {
				System.out.print(input[i] + " ");
			}

			System.out.println("\n");
		}

		for(int i = 0; i < numOfPerceptrons ; i++) {

			if(debugging)
				System.out.println("Predicting from ANN " + i);

			predictedValues[i] = perceptrons[i].predict(input);

			if(debugging)
				System.out.println("");
		}

		return predictedValues;
	}


	/**
	 * This method implements the Rectified Linear Unit on predicted outputs
	 * of the perceptron.
	 * 
	 * @param predictedValues
	 * @param saveOldValues
	 */
	void normalizePredictedValues(double[] predictedValues, double[] saveOldValues) {

		System.arraycopy(predictedValues, 0, saveOldValues, 0, predictedValues.length );

		for(int i = 0; i < predictedValues.length; i++) {
			predictedValues[i] = Double.min(Double.max(predictedValues[i],-3), 5);
		}

	}

	/**
	 * This method returns the direction in which the agent has to take
	 * the next step.
	 * 
	 * @param predictedValues
	 * @return
	 */
	int getMaximumOfAllPredictedValues(double[] predictedValues) {

		int returnIndex;
		double alpha = (1) / (Math.log10((double)numberOfStepsTaken) + 1);

		if(debugging) {
			System.out.println("Alpha: " + alpha + ", numberOfStepsTaken " + numberOfStepsTaken);
		}
		Random r = new Random();

		if(numberOfStepsTaken < 20) 
			return 0;

		if(alpha > 0.25 && playingFirstTime == true) {
			if(debugging) {
				System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			}
			returnIndex = r.nextInt(predictedValues.length);

		} else if(alpha > 0.35 && playingFirstTime == false) {
			if(debugging) {
				System.out.println("#################");
			}
			returnIndex = r.nextInt(predictedValues.length);

		} else {

			if(debugging) {
				System.out.print("Predictions: " + predictedValues[0] + " ");
			}

			ArrayList<Integer> repeatedVals = new ArrayList<Integer>();

			double max = predictedValues[0];
			repeatedVals.add(0);

			for(int i = 1; i < numOfPerceptrons; i++) {

				if(debugging) {
					System.out.print(predictedValues[i] + " ");
				}

				if(max < predictedValues[i]) {
					max = predictedValues[i];
					repeatedVals.clear();
					repeatedVals.add(i);
				} else if(max == predictedValues[i]){
					repeatedVals.add(i);
				}
			}

			if(debugging) {
				System.out.println();
			}

			if(repeatedVals.size() > 1) {
				returnIndex = repeatedVals.get(r.nextInt(repeatedVals.size() - 1));
			} else {
				returnIndex = repeatedVals.get(0);
			}
		}

		if(returnIndex == stepsTaken[(stepsTakenInd-1+stepsTaken.length) % (stepsTaken.length)] && 
				returnIndex == stepsTaken[(stepsTakenInd-3+stepsTaken.length) % (stepsTaken.length)]) {
			returnIndex = r.nextInt(predictedValues.length);
		} 

		stepsTaken[(++stepsTakenInd) % (stepsTaken.length)] = returnIndex;

		return returnIndex;
	}

}

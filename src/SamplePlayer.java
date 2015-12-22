//
//
// SamplePlayer - walk towards Vegetables; if none, away from Players.
//                If no other players are in sight, then
//                choose a random direction to move.
//
//                This leads to a very timid player, as there are
//                usually some players in sight, while seeing a
//                vegetable is less likely.  Thus, this player will
//                often run into a corner and hide.  However, this code
//                does illustrate how to access the sensors and choose
//                a move.  (The SmartPlayer class makes more intelligent
//                use of the sensors - other than remembering the last
//                move made, it is a "memory-less" solution.  In AI
//                jargon, it is a "reactive" planner that only
//                reacts to the current situation.)
//
// IMPORTANT:
//    So all players have unique names, call you class: <YourLoginName>Player

public final class SamplePlayer extends Player
{ private Sensors sensors, prevSensors;
private double  reward;
private boolean debugging = false;

SamplePlayer(AgentWindow agentWindow)
{
	this(agentWindow, false);
}
SamplePlayer(AgentWindow agentWindow, boolean showSensors)
{
	super(agentWindow);
	setShowSensors(showSensors); // Display this player's sensors?
	prevSensors = new Sensors();
}

public void run()
{ int currentTime = Integer.MAX_VALUE; // Can use this to notice that a game 
// is over (ie, it'll be lower than
// on the previous cycle).
boolean badLastChoice      = false, // Got penalized when going for food?
firstStepInWorld   = true;  // First step in a fresh environment?
double  previousAngleMoved = 0.0;   // Direction chosen on previous step.

try
{
	while(threadAlive()) // Basically, loop forever.
	{ int    directionOfClosestVegetable = -1,
	directionOfClosestPlayer    = -1;
	double distanceToClosestVegetable  = Double.MAX_VALUE,
			distanceToClosestPlayer     = Double.MAX_VALUE,
			distance, radians;

	int temp = getCurrentTime(); // Counts moves made in the game.

	if (temp > currentTime)
	{
		currentTime      = temp;
		firstStepInWorld = false;
	}
	else // Starting anew.
	{    // Reset the memory of what happened on the last step.
		badLastChoice      = false;
		firstStepInWorld   = true;
		previousAngleMoved = 0.0;
	}

	sensors     = getSensors();     // See what the world looks like.

	// Look around.  However, if received a punishment on last 
	// step toward a vegetable, make a random move.
	if (!badLastChoice) for(int dir = 0; dir < Sensors.NUMBER_OF_SENSORS; dir++)
	{ // Determine the closest food and the closest animal.
		switch (sensors.getObjectType(dir)) {
		case Sensors.VEGETABLE:
			distance = sensors.getDistance(dir);
			if (distance < distanceToClosestVegetable)
			{
				distanceToClosestVegetable = distance;
				directionOfClosestVegetable      = dir;
			}
			break;
		case Sensors.ANIMAL:
			distance = sensors.getDistance(dir);
			if (distance < distanceToClosestPlayer)
			{
				distanceToClosestPlayer = distance;
				directionOfClosestPlayer      = dir;
			}
			break;
		}
	}

	if (directionOfClosestVegetable >= 0)
	{ // Move toward closest vegetable, if one in sight.

		if (debugging)
		{
			Utils.println
			(getName() + " player is running toward a vegetable at "
					+ Utils.convertSensorIDtoDegrees(directionOfClosestVegetable)
					+ " degrees at t=" + currentTime);
		}
		radians 
		= Utils.convertSensorIDtoRadians(directionOfClosestVegetable);
	}
	else if (directionOfClosestPlayer >= 0)
	{ int oppositeDir = Utils.oppositeDirection(directionOfClosestPlayer);
	// Otherwise move away from the closest player.

	if (debugging)
	{
		Utils.println
		(getName() + " player is running away from a player at "
				+ Utils.convertSensorIDtoDegrees(directionOfClosestPlayer)
				+ " degrees (toward "
				+ Utils.convertSensorIDtoDegrees(oppositeDir)
				+ " degrees) at t=" + currentTime);
	}
	radians = Utils.convertSensorIDtoRadians(oppositeDir);
	}
	else // Otherwise do a random walk.
	{ int randomDir = Utils.getRandomDirection();

	// Note: these moves will be along the sensor rays.
	//       Could instead use:  2 * Math.PI * Math.random()

	if (debugging)
	{
		Utils.println(getName()
				+ " player is choosing a random direction: "
				+ Utils.convertSensorIDtoDegrees(randomDir)
				+ " degrees at t=" + currentTime); 
	}
	radians = Utils.convertSensorIDtoRadians(randomDir);
	}

	// The following provides a possibly useful debugging aid.
	if (singleStepping() && debugging)
	{ String str
		= "Chose to move "
				+ Utils.truncate(Utils.convertToDegrees(radians),
						1)
						+ " degrees.  Previously chose "
						+ Utils.truncate(Utils.convertToDegrees(previousAngleMoved),
								1)
								+ " degrees and received a reward of "
								+ Utils.truncate(reward, 2) + ".";

	// Print on the Agent World screen.
	playingField.agentWindow.reportInInfoBar(str);
	}

	previousAngleMoved = radians; // Remember previous move chosen.

	// Setting the direction to move causes this thread to sleep
	// until the 'manager' updates the world, resets the sensors,
	// and wakes all the players.

	// A NEGATIVE direction means, STAND STILL, so convert
	// the radians to a value in [0, 2 x PI).
	setMoveVector(Utils.convertToPositiveRadians(radians));

	// Sleeping for awhile ...

	// After being awakened, get the reward (reinforcement) for last action.
	reward = getReward();
	prevSensors.copy(sensors); // Hold these for training purposes.

	// For some minimal smarts, if can't reach a vegetable, don't
	// keep trying constantly.
	badLastChoice = (directionOfClosestVegetable >= 0 && reward < 0.0);
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


}


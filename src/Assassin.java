import java.awt.Color;

//
//
// Assassin - if this player sees another player, runs right toward it.
//            Otherwise does a random walk.
//
//

public final class Assassin extends Player
{ public static Color AssassinColor = Color.red;

  private static int     numberCreated = 0;
  private        int     IDnumber;
  private        Sensors sensors;

  Assassin(AgentWindow agentWindow)
  {
    this(agentWindow, false);
  }
  Assassin(AgentWindow agentWindow, boolean showSensors)
  {
    super(agentWindow);
    setShowSensors(showSensors);
    IDnumber = ++numberCreated;
  }

  void setPriority()
  { // Built-in players have lower priority than the default. 
    playerThread.setPriority(builtinPriority);
  }
  private boolean isaPlayer(int dir)
  {
    return sensors.getObjectType(dir) == Sensors.ANIMAL;
  }

  private boolean noPlayersAllDirectionsToTheLeft(int dir, int K)
  { 
    for(int i = 1; i <= K; i++) if (isaPlayer(Utils.leftK_sensor(dir,  i))) return false;

    return true;
  }
  private boolean noPlayersAllDirectionsToTheRight(int dir, int K)
  { 
    for(int i = 1; i <= K; i++) if (isaPlayer(Utils.rightK_sensor(dir, i))) return false;

    return true;
  }

  public void run()
  { double  reward;
    int     randomWalkCounter = 0; // After getting a neg reward, do a random walk for awhile.

    try
    {
      while(threadAlive())
      { int    dirOfClosestVegetable = -1,
               dirOfClosestPlayer    = -1,
               dirOfClosestMineral   = -1,
               playersInSight        =  0;
        double distanceOfClosestVegetable = Double.MAX_VALUE,
               distanceOfClosestPlayer    = Double.MAX_VALUE,
               distanceOfClosestMineral   = Double.MAX_VALUE,
               distance, radians;

        sensors = getSensors();
        if (randomWalkCounter <= 0) for(int dir = 0; dir < Sensors.NUMBER_OF_SENSORS; dir++)
        { 
          switch (sensors.getObjectType(dir)) {
          case Sensors.VEGETABLE:
            distance = sensors.getDistance(dir);
            if (distance < distanceOfClosestVegetable)
            {
              distanceOfClosestVegetable = distance;
              dirOfClosestVegetable      = dir;
            }
            break;
          case Sensors.MINERAL: // Find closest mineral, with some noise (for randomness).
            distance = sensors.getDistance(dir);
            if (distance < distanceOfClosestMineral && Math.random() < 0.95)
            {
              distanceOfClosestMineral = distance;
              dirOfClosestMineral      = dir;
            }
            break;
          case Sensors.ANIMAL:
            playersInSight++;
            distance = sensors.getDistance(dir);
            if (distance < distanceOfClosestPlayer  && // Look for ISOLATED prey.
                noPlayersAllDirectionsToTheLeft( dir, 6) && // Hardwire-in for 36 sensors ...
                noPlayersAllDirectionsToTheRight(dir, 6))
            {
              distanceOfClosestPlayer = distance;
              dirOfClosestPlayer      = dir;
            }
            break;
          }
        }
        
        if (playersInSight <= 4 * IDnumber && dirOfClosestPlayer >= 0 && Math.random() < 0.9)
        { // If there are few players in sight,
          // run toward the closest 'isolated' player (almost always).
          // Also, as a cheap trick, the assassins vary in their aggressiveness.
          radians = Utils.convertSensorIDtoRadians(dirOfClosestPlayer);
        }
        else if (dirOfClosestVegetable >= 0 && Math.random() < 0.9)
        { // Otherwise move toward closest vegetable, if one in sight.
          radians = Utils.convertSensorIDtoRadians(dirOfClosestVegetable);
        }
        else if (dirOfClosestMineral   >= 0 && Math.random() < 0.9)
        { // Otherwise, most of the time, run toward the closest mineral, possibly pushing it.
          // Recall that penalties lead to random walks anyway.
          radians = Utils.convertSensorIDtoRadians(dirOfClosestMineral);
        }
        else // Otherwise do a random walk.
        { 
          radians = 2 * Math.random() * Math.PI;
        }

        setMoveVector(Utils.convertToPositiveRadians(radians));
        reward = getReward();

	// After hitting another player (or the wall, etc), do some random steps for awhile.
        if (reward < 0)
        {
	  if (randomWalkCounter <= 0) randomWalkCounter = 3 + (int)(Math.random() * 7);
	}
	else randomWalkCounter--;
      }
    }
    catch(Exception e)
    {
      Utils.errorMsg(e + ", " + toString() + " has stopped running");
    }

    Utils.errorMsg(getName() + " is no longer alive for some reason");
    Utils.exit(-1);
  }

}

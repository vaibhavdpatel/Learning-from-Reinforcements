//
//
// Reward
//
//

public class Reward 
{ public static final int
    HARVESTING_VEGETABLE_REWARD                    =   5,
    PUSHING_MINERAL_PENALTY                        =  -2, 
    HIT_BY_MOVING_MINERAL_PENALTY                  = -25, // Also get a PUSHING_MINERAL_PENALTY.
    REWARD_FOR_PUSHING_MINERAL_INTO_ANOTHER_PLAYER =  25,
    BUMPING_PLAYER_PENALTY                         =  -3,
    BUMPING_WALL_PENALTY                           =  -1;
  
  private int    timeStamp;
  private double reward;

  Reward(int currentTime, double reward)
  {
    this.timeStamp = currentTime;
    this.reward    = reward;
  }

  public int getTimeStamp()
  {
    return timeStamp;
  }

  public double getReward()
  {
    return reward;
  }

  // Users can't set these.
  void copy(Reward sender)
  {
    timeStamp = sender.getTimeStamp();
    reward    = sender.getReward();
  }

  void setTimeStamp(int timeStamp)
  {
    this.timeStamp = timeStamp;
  }

  void setReward(double reward)
  {
    this.reward = reward;
  }

}


import java.awt.Color;

//
//
// RandomWalker
//
//

public final class RandomWalker extends Player
{ static Color RandomWalkerColor = Color.gray;

  RandomWalker(AgentWindow agentWindow)
  {
    super(agentWindow);
    setComputeSensors(false); // No need to waste computer time since these will be ignored.
  }

  void setPriority()
  { // Built-in players have lower priority than the default. 
    playerThread.setPriority(builtinPriority);
  }

  public void run()
  {
    try
    {
      while(threadAlive())
      { 
        setMoveVector(2 * Math.PI * Math.random()); // Will sleep until 'manager' resets the sensors.
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

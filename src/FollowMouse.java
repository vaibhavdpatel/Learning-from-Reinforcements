import java.awt.Color;

//
//
// FollowMouse - walk toward mouse IF MOUSE IS BEING PRESSED.
//
//

public final class FollowMouse extends Player
{ private static final boolean debuggingThisClass = false;

          static final Color FollowMouseColor = Color.orange;
  private boolean debugging;

  FollowMouse(AgentWindow agentWindow)
  { // Compute sensors if debugging in case developer wishes to guide this player.
    super(agentWindow);

    debugging = (debuggingThisClass && AgentWindow.masterDebugging);
    setComputeSensors(debugging);
  }

  void setPriority()
  { // Give this player a high priority since it interacts with users.
    playerThread.setPriority(Thread.NORM_PRIORITY + 1);
  }

  public void run()
  {
    try
    {
      while(threadAlive())
      { double radians = -1.0; // Stand still by default (indicated by negative number).

        // Allow testing out the treatment of a slow player.
        if (Math.random() > 1.99)
        {
          try { Thread.currentThread().sleep(1000); }
          catch(Exception e) {}
        }
        
        if (isMouseDown()) // Stand still if mouse NOT down.
        { Position me     = getPosition(),
                   delta  = getDirection(), // Have some momentum.
                   mouse  = getLastMouseDownPosition();
          int      deltaX = mouse.x - me.x + delta.x,
                   deltaY = mouse.y - me.y + delta.y;
        
          // Dont move unless at least a step-size away.
          if (Utils.square(deltaX) + Utils.square(deltaY)
              >= Utils.getPlayerStepSize() * Utils.getPlayerStepSize())
	  {
            radians = Utils.convertToPositiveRadians(Math.atan2((double)deltaY,
                                                                (double)deltaX));
            if (debugging)
	    {
	      playingField.agentWindow.reportInInfoBar
		(getName()
		 + " is trying to move toward the mouse, theta = "
		 // Rotate to the normal direction ... (buggy at 0, but that's ok).
		 + (int)(360 - Utils.convertToDegrees(radians)) + " degrees.");
	    }
	  }
        }
        setMoveVector(radians);
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

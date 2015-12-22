import java.util.Date;

//
//
// ManagerThread
//
// - could make PlayingField runnable, but this breaks up the classes better.

class ManagerThread extends Thread
{ private static final boolean debuggingThisClass = false;
                       boolean debugging,
                               reportSlowPlayers  = true; // Allow user to toggle this.

  private int  clockPeriod = 100;
          int  currentTime =   0; // The playing field accesses this.
  private long timeLastGameStarted;
          long totalTurnsTaken = 0; // This is watched to see if this thread has died.
  private PlayingField playingField;
    
  // Constructor
  ManagerThread(PlayingField playingField)
  {
    super("ManagerThread");

    this.playingField = playingField;
    setClockPeriod(clockPeriod); // Make sure this is in a valid range.
    timeLastGameStarted = System.currentTimeMillis();
    
    debugging = (debuggingThisClass && AgentWindow.masterDebugging);
  }

  void setClockPeriod(int msecs)
  { // Keep the clock period 'reasonable.'  If too low (under 50), the Java system can get overwhelmed and hang.
    clockPeriod = Math.max(50, Math.min(msecs, 600000)); // No more than a 10 minutes.
  }

  private boolean singleStepping()
  {
    return playingField.singleStepping;
  }

  void setReportSlowPlayers(boolean value)
  {
    reportSlowPlayers = value;
  }

  private void handleCollisionsAndLookForChainReactions()
  { boolean foundOne = false;
    int     cycles   = 0;

    do
    { 
      if (debugging && !debugging) Utils.println("Looking for chain reactions (cycle #" + cycles + ")");

      foundOne = false;
      if (cycles++ > playingField.playersSoFar + playingField.vegetablesSoFar + playingField.mineralsSoFar + 3)
      { // At worst, everyone should return to their original spot one-by-one.
        Utils.errorMsg("Too many cycles in lookForChainReactions()");
        break;
      }

      for(int i = 0; i < playingField.playersSoFar; i++)  if (playingField.player[i].exists(currentTime) && playingField.player[i].hasMoved())
      { Entity hit = playingField.intersectingPlayer(i);

        if (hit != null)
        { Player p = playingField.player[i];

          if (debugging) Utils.println(p.toString() + " is hitting "
                                       + hit.toString() + " on cycle " + cycles + " at " + currentTime);
          foundOne = true;
          if      (hit instanceof Player)    p.hitPlayer((Player)hit);
          else if (hit instanceof Mineral)   p.movedIntoMineral((Mineral)hit);
          else if (hit instanceof Vegetable) p.ateVegetable((Vegetable)hit);
          else Utils.errorMsg("How can one hit " + hit + " in a chain reaction?");
          p.returnToPrevLocationIfHit();
        }
      }

      for(int i = 0; i < playingField.mineralsSoFar; i++) if (playingField.mineral[i].exists(currentTime) && playingField.mineral[i].hasMoved())
      { Entity  hit = playingField.intersectingMineral(i);
        Mineral m   = playingField.mineral[i];

        if (hit != null)
        {
          if (debugging) Utils.println(m.toString() + " is hitting "
                                       + hit.toString() + " on cycle " + cycles + " at " + currentTime);
          foundOne = true;

          if      (hit instanceof Vegetable) m.hitVegetable((Vegetable)hit);
          else if (hit instanceof Mineral)   m.movedIntoMineral((Mineral)hit);
          else if (hit instanceof Player)    m.hitPlayer((Player)hit);
          m.returnToPrevLocationIfHit(); // Move back to original position
        }
        else if (cycles == 1 && m.isMoving()) m.slowDown(); // Apply some friction.
      }

      collectEatenVegetables(); // Wait until here so simulataneous hits are all counted.

    } while (foundOne);

    // Do final clean-up checks.
    for(int i = 0; i < playingField.mineralsSoFar; i++) if (playingField.mineral[i].exists(currentTime))
    {
      playingField.mineral[i].updateMineralStatus();
    }
  }

  void collectEatenVegetables()
  {
    for(int i = 0; i < playingField.vegetablesSoFar; i++) if (playingField.vegetable[i].exists(currentTime))
    {
      playingField.vegetable[i].checkIfEaten(currentTime, playingField.outerBoxInner);
    }
  }

  void gameOver()
  { 
    playingField.gamesPlayed++;

    String s = "Game #" + playingField.gamesPlayed + " (of " + playingField.gamesToPlay + ") Over";
    playingField.agentWindow.clock.setText(s);
    System.out.println("");
    Utils.println(s);
    long currentRealTime = System.currentTimeMillis();
    Utils.println("This game finished at " + (new Date(currentRealTime)).toString());
    Utils.println("It took" + Utils.truncate((currentRealTime - timeLastGameStarted) / (60 * 1000.0), 2)
                  + " minutes to play");
    System.out.println("");
    // Reset all the players statistics.
    for(int i = 0; i < playingField.playersSoFar; i++)
    {
      playingField.player[i].gameOver();
    }
    System.out.println("");
    if (playingField.isDisplayOn())
    {
      try { sleep(3000); } // Sleep a bit between games.
      catch(Exception e) {}
    }
  }

  void reinitializeGame()
  {
    // Reset all the vegetables; the players were reset by gameOver().
    for(int i = 0; i < playingField.vegetablesSoFar; i++) playingField.vegetable[i].gameOver();
    playingField.configure(false);
    currentTime = 0;
    timeLastGameStarted = System.currentTimeMillis();
  }

  void reportFinalPlayerStats()
  {
    playingField.agentWindow.clock.setText("The requested " + playingField.gamesPlayed + " games have been played.");
    System.out.println("");
    System.out.println("The Results After Playing " + playingField.gamesPlayed + " Games:");
    System.out.println("");
    for(int i = 0; i < playingField.playersSoFar; i++)
    {
      playingField.player[i].reportAggregateStats();
    }
    System.out.println("");
  }

  public void run()
  { 
    try
    { 
      while (isAlive() && (playingField.gamesToPlay < 0 || playingField.gamesPlayed < playingField.gamesToPlay))
      { 
        if (playingField.gamesToPlay > 0)
        {
	  playingField.agentWindow.setTitle("The Agent World - Game #"
	    				    + (playingField.gamesPlayed + 1)
					    + " of " + playingField.gamesToPlay);
        }
      
        // First, startup all the player threads.  Then sleep (longer than usual) while players make their first moves.
        playingField.prepareForNextCycle(currentTime);
        waitForPlayersToChooseMoves(5 * clockPeriod);
    
        while (isAlive() && (playingField.gameDuration < 0 || currentTime < playingField.gameDuration))
        {
          currentTime++;
          totalTurnsTaken++;
        
          if (playingField.isDisplayOn() || (currentTime % 10) == 0)
          { // If trying to run fast, only update clock every N ticks.
            if (playingField.gameDuration > 0)
            {
              playingField.agentWindow.clock.setText("  Time = "  + currentTime 
                                                     + " of "     + playingField.gameDuration);
            }
            else
            {
              playingField.agentWindow.clock.setText("  Time = " + currentTime);
            }
          }

          // First, make sure no vegetables grow "underfoot."
          for(int i = 0; i < playingField.vegetablesSoFar; i++) if (playingField.vegetable[i].exists(currentTime))
          { Vegetable v = playingField.vegetable[i];

            if (currentTime == v.getBirthday())
            { Entity hit = playingField.intersectingVegetable(i);

              if (hit != null)
              { // Can't be reborn at this time.  Delay a bit longer and move a little bit.

                // Recall that internal time is in number of steps taken.
                v.setBirthday(currentTime + 10 + (int)(30 * Math.random()));
                if (debugging) Utils.println(v.toString() + " couldn't grow at " + currentTime);
                v.perturbLocation(PlayingField.objectDiameter, playingField.outerBoxInner);
                continue;
              }
              if (debugging) Utils.println(v.toString() + " can grow at " + currentTime);
            }

          }
    
          // Move all the players.
          for(int i = 0; i < playingField.playersSoFar; i++) if (playingField.player[i].exists(currentTime))
          { Player   p   = playingField.player[i];
            Position pos = p.getPosition();
                
            p.setPrevPosition(pos);

            // Clear the player's accumulator.  Need to total for each step.
            p.setCurrentReward(0);

            // Then move the player.
            double moveVector = p.getMoveVector();
       
            if (moveVector >= 0.0)// See if this player is moving.
            { int      deltaX = (int)Math.round(Utils.getPlayerStepSize() * Math.cos(moveVector)),
                       deltaY = (int)Math.round(Utils.getPlayerStepSize() * Math.sin(moveVector));

              pos.x += deltaX;
              pos.y += deltaY;

              if (!playingField.outerBoxInner.inside(pos)) p.hitBorder();
              else p.setDirection(deltaX, deltaY);
            }
            else p.setDirection(0, 0);
          }

          for(int i = 0; i < playingField.mineralsSoFar; i++) if (playingField.mineral[i].exists(currentTime))
          { // Move all the minerals that are sliding.
            Mineral  m = playingField.mineral[i];
            Position p = m.getPosition();

            m.setPrevPosition(p);

            if (m.isMoving()) 
            { Position dir    = m.getDirection();
              double   theta  = Math.atan2((double)dir.y, (double)dir.x);
              int      deltaX = (int)Math.round(m.getStepSize() * Math.cos(theta)),
                       deltaY = (int)Math.round(m.getStepSize() * Math.sin(theta));

              p.x += deltaX;
              p.y += deltaY;

              if (!playingField.outerBoxInner.inside(p)) m.hitBorder();
              if (debugging && singleStepping())
	      {
		Utils.println(m.toString() + " moved from " + m.getPrevPosition() + " to " + p);
	      }
            }
          }

          // Now check for collisions, etc.
          // Handle cases where an object moved into a place where the previous object returned to.
          handleCollisionsAndLookForChainReactions(); // Also do some other final bookkeeping.

          playingField.updateSensorsAndRewards();

          // Be as fair as possible about resuming all the players by waiting until the end.
          playingField.prepareForNextCycle(currentTime);
          playingField.redisplay();
          waitForPlayersToChooseMoves(clockPeriod);
        } //end of INNER WHILE
      if (playingField.gamesToPlay >= 0)
      {
        gameOver();
        if (playingField.gamesPlayed < playingField.gamesToPlay) reinitializeGame();
      }
      else Utils.errorMsg("The manager thread has stopped running.");
} // end of OUTER WHILE

    if (playingField.gamesToPlay >= 0) reportFinalPlayerStats();
} // end of TRY
  catch(Exception e) 
  {
    Utils.errorMsg("Caught an exception running the manager: " + e);
    e.printStackTrace(System.err);
  }
}

  void waitForPlayersToChooseMoves(int maxWaitDuration)
  {
    if (singleStepping())
    {
      //xxx playingField.agentWindow.reportInInfoBar(" Press " + playingField.agentWindow.goLabel.trim() + " to continue.");
      playingField.agentWindow.pause.setEnabled(true);
      playingField.pause();
    }
    else
    { int sleepDuration = 0, miniSleepPeriod = 50;
      boolean interrupted = false;

      // Interrupted doesn't work in Java 1.0.2, so sleep a little and then check again.
      while (playingField.shouldManagerSleep() && sleepDuration < maxWaitDuration && !interrupted)
      {
        try
        {
          sleep(miniSleepPeriod);
          sleepDuration += miniSleepPeriod;
        }
        catch(InterruptedException e) { interrupted = true; }
      }

      if (debugging && !debugging && sleepDuration < maxWaitDuration)
      {
        Utils.println("The manager thread awoke from sleeping @ time = " + currentTime 
                      + " [sleepDuration=" + sleepDuration
                      + " of " + maxWaitDuration + "msec]");

      }

      // There is a slight chance that the delayed player has now chosen its move.
      // Don't worry if one awakens between the IF and the THEN parts of this statement.
      if (reportSlowPlayers && sleepDuration >= clockPeriod &&
          !interrupted && playingField.shouldManagerSleep())
      {
        Utils.println("Timed out before finding all players were ready to move @ time = " + currentTime);
        playingField.reportPlayersNotReadyToGo(currentTime);
        System.out.println("");
      }
    }
  }

  public String toString()
  {
    return "Manager";
  }
       
}

import java.awt.Color;

//
//
// Player
//
//

class Player extends Entity implements Runnable
{ private static final boolean debuggingThisClass = false;
  
  static  final Color PlayerColor = Color.red, latePlayerColor = Color.black;
 
  private Sensors  sensors, readSensors;
  private Reward   reward,  readReward;
  private static Boolean  synchForManager = new Boolean(true); // Only want ONE ("global") monitor.
  // The vector (in radians) specifying the direction moving.
  // (The manager controls the step size.)
  // Movement is continuous, though if set to a negative number
  // the player will stand still.
  private double moveVector, readMoveVector;
  private int    gameScores[];

  protected PlayerThread playerThread;

  private int     consecutiveMissedMoves = 0, resumeAfterThisManyMissedMoves = 25,
                  totalMissedMoves       = 0, totalMissedMovesThisGame       =  0,
                  restartsNeeded         = 0,
                  currentReward          = 0, totalReward     = 0,
                  gamesPlayed            = 0, aggregateReward = 0;
  private static int totalRestartsNeeded = 0;
  private boolean debugging, computeSensors = true, showSensors = false, readyToMove = false;
  private String  name;
  private Score   scoreLabel;
  private Color   color,
                  origColor; // Used when there is a need to reset the color.

          boolean showScores  = true; // Show it's score history when paused?

  protected int defaultPriority = Thread.NORM_PRIORITY - 2,
                builtinPriority = Thread.NORM_PRIORITY - 3, // Give new players priority.
                ejectedScore    = -1000;

  private static int counter = 0; // Count the number of players created.

  Player(AgentWindow agentWindow)
  {
    this(0, agentWindow.playingField);
  }
  Player(PlayingField playingField)
  {
    this(0, playingField);
  }
  Player(int currentTime, PlayingField playingField)
  {
    super(counter++, currentTime, playingField);

    sensors    = new Sensors();
    reward     = new Reward(-1, 0.0);

    // Create the "buffers" into which data is "latched."
    readSensors    = new Sensors();
    readReward     = new Reward(-1, 0.0);

    gameScores     = new int[1000]; // Just brute-force this as an array.

    moveVector     = -1.0;  // Start not moving.
    readMoveVector = moveVector;

    debugging = (debuggingThisClass && AgentWindow.masterDebugging);
  }

  final void gameOver()
  {
    Utils.println(getName() + " scored " + totalReward + " points");
    if (totalMissedMovesThisGame > 0 || totalMissedMoves > 0)
    {
      Utils.println("  It wasn't ready to move " + totalMissedMovesThisGame
                    + " times this game ["       + totalMissedMoves + " missed moves total]");
    }
    if (gamesPlayed >= 1000)
    {
      Utils.errorMsg("For coding simplicity, scores can only be kept for 1000 games.");
    }
    else gameScores[gamesPlayed++] = totalReward;
    aggregateReward         += totalReward;
    consecutiveMissedMoves   = 0; // Count these on a per-game basis.
    totalMissedMovesThisGame = 0;
    restartsNeeded           = 0;
    totalReward              = 0; // Leave the old score up there for awhile (ie, don't post this).
    reset();
  }

  final void reportAggregateStats()
  {
    System.out.println(name + " scored an average of " + getMeanScore() + reportRecentStats(false));
  }

  final String reportRecentStats(boolean showName)
  { int    total   = 0;
    double counter = 0.0;

    for(int i = gamesPlayed; i > gamesPlayed - 5 && i > 0; i--)
    {
      total += gameScores[i - 1];
      counter++;
    }

    return ((showName ? name : " and who") 
            + " scored an average of " + (total / counter) + " over the past "
            + Math.round(counter) + " games");
  }

  int getGameScore(int i)
  {
    if (i < gamesPlayed) return gameScores[i]; else return 0;
  }

  int getGamesPlayed()
  {
    return gamesPlayed;
  }

  double getMeanScore()
  {
    return aggregateReward / Math.max(1.0, (double)gamesPlayed);
  }

  final void returnToPrevLocationIfHit()
  {
    if (getCollided())
    { 
      setPosition(getPrevPosition());
      setCollided(false);
    }
  }

  int getScore()
  {
    return totalReward;
  }

  Score getScoreLabel()
  {
    return scoreLabel;
  }

  final void setName(String name)
  {
    this.name = name;
  }

  final public String getName()
  {
    return name;
  }

  final void setColor(Color color)
  {
    this.color = color;
  }

  final void setOrigColor(Color color)
  {
    origColor = color;
  }

  final Color getColor()
  {
    return color;
  }

  final Color getOrigColor()
  {
    return origColor;
  }

  final void setCurrentReward(int reward)
  {
    currentReward  = reward;
    if (reward != 0)
    {
      totalReward += reward;
      if (scoreLabel != null) scoreLabel.setScore(totalReward);
    }
  }

  final void addToCurrentReward(int reward)
  {
    if (reward != 0)
    {
      currentReward += reward;
      totalReward   += reward;
      if (scoreLabel != null) scoreLabel.setScore(totalReward);
    }
  }

  final void setScoreContainer(Score scoreLabel)
  {
    this.scoreLabel = scoreLabel;
  }

  final public void setClockPeriod(int msec)
  {
    playingField.setClockPeriod(msec);
  }

  final void resume()
  {
    playerThread.resume();
  }

  final boolean threadAlive()
  {
    return playerThread.isAlive();
  }

  final void eject()
  {
    setExists(false);
    // The playing field will automatically NOT wait for this player.
    if (scoreLabel != null) scoreLabel.ejected(); // Mark score as ejected.
  }

  // Restart this player if it was ejected from the previous game.
  final boolean reset()
  { 
    setExists(true);
    if (scoreLabel != null) scoreLabel.reset();
    return true;
  }

  final double getMoveVector()
  {
    synchronized(synchForManager)
    { // Need to copy into separate memory since main version can change.
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText(toString() + "in getMoveVector()");
      readMoveVector = moveVector;
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText("");
    }
    return readMoveVector;
  }

  final public void setMoveVector(double radians)
  { 
    synchronized(synchForManager)
    {
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText(toString() + "in setMoveVector()");
      moveVector = radians;
      consecutiveMissedMoves = 0;
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText("");
    }

    if (playerThread == null) Utils.errorMsg("no thread in setMoveVector() " + toString());
    if (debugging) Utils.println(toString() + "is ready to move and is being suspended");
    if (playingField == null) Utils.errorMsg("no playing field in setMoveVector() " + toString());

    playingField.readyToGo(getID());
    // Can't put this in a monitor, or all will lock up?  The readyToGo does do a synch that should help
    // prevent simultaneous resume and suspends (not even sure if that is a problem).
    playerThread.suspend(); // Be sure to do this last, since the playerThread calls this function.
  }

  void recordMissedMove()
  {
    consecutiveMissedMoves++;
    totalMissedMovesThisGame++;
    totalMissedMoves++;

    if (consecutiveMissedMoves == resumeAfterThisManyMissedMoves / 2)
    { // Try a resume along the way (shouldn't matter, though, since these should be being
      // sent elsewhere.  But can't hurt ...).
      if (playerThread != null) playerThread.resume();
    }
    if (consecutiveMissedMoves >= resumeAfterThisManyMissedMoves)
    {
      totalRestartsNeeded++;
      restartsNeeded++;
      Utils.errorMsg(getName() + " has not moved for " + consecutiveMissedMoves
                     + " steps.  It's thread will be restarted (restart #" + restartsNeeded + ")");
      if (debugging) Utils.println("Overall, this is restart #" + totalRestartsNeeded);
      if (playerThread != null)
      {
        if (debugging && threadAlive()) Utils.errorMsg(playerThread.toString() + " still is alive ...");
        playerThread.stop();
      }
      start();
    }
  }

  int getConsecutiveMissedMoves()
  {
     return consecutiveMissedMoves;
  }

  void setReadyToMove(boolean value)
  {
    readyToMove = value;
  }

  boolean getReadyToMove()
  {
    return readyToMove;
  }

  final void setReward(int timeStamp)
  {
    synchronized(synchForManager)
    {
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText(toString() + " in setReward()");
      reward.setTimeStamp(timeStamp);
      reward.setReward(currentReward);
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText("");
    }
  }

  final public Reward getRewardFull()
  {
    synchronized(synchForManager)
    {
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText(toString() + " in getRewardFull()");
      readReward.copy(reward);
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText("");
    }
    return readReward;
  }

  final public double getReward()
  {
    synchronized(synchForManager)
    {
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText(toString() + " in getReward()");
      readReward.copy(reward);
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText("");
    }
    return readReward.getReward();
  }

  // Read these sensor values into the structure for this player.
  final void setSensors(Sensors sensors)
  {
    synchronized(synchForManager)
    {
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText(toString() + " in setSensors()");
      this.sensors.copy(sensors);
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText("");
    }
  }

  final public Sensors getSensors()
  {
    synchronized(synchForManager)
    {
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText(toString() + " in getReward()");
      readSensors.copy(sensors);
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText("");
    }
    return readSensors;
  }

  // Start up the thread that runs this player.
  public void start()
  {
    if (debugging) Utils.println("Starting the thread that runs " + name);
    synchronized(synchForManager)
    {
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText(toString() + " in start()");
      playerThread = new PlayerThread(this, name + "'s thread");
      setPriority();
      playerThread.start();
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText("");
    }
  }

  void setPriority()
  { // Built-in players (except FollowMouse) have lower priority than this.
    playerThread.setPriority(defaultPriority);
  }

  // See if this player has been started.
  boolean started()
  { boolean result = false;

    synchronized(synchForManager)
    {
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText(toString() + " in started()");
      result = (playerThread != null);
      if (AgentWindow.reportSynchs) AgentWindow.developerLabel1.setText("");
    }
    return result;
  }

  public void run()
  { 
    Utils.errorMsg("Should have defined a run() method for " + toString());
  }

  public boolean getShowSensors()
  {
    return showSensors;
  }

  public void setShowSensors(boolean value)
  {
    if (value && !computeSensors)
    { String s = toString() + " doesn't use the sensors; hence, they are not computed.";

      playingField.agentWindow.reportInInfoBar(s);
    }
    else showSensors = value;
  }

  public void toggleShowingSensors()
  {
    setShowSensors(!showSensors);
  }

  public boolean getComputeSensors()
  {
    return computeSensors;
  }

  public void setComputeSensors(boolean value)
  {
    computeSensors = value;
    if (computeSensors == false) showSensors = false;
  }

  void describeSelectedPlayer()
  { String postfix = (getGamesPlayed() > 0 ? reportRecentStats(false) : "");

    playingField.agentWindow.reportInInfoBar("You selected "          + getName()
                                             + ", whose score (at t=" + getCurrentTime()
                                             + ") is " + getScore()   + postfix + ".");
  }

  void hitBorder()
  { Position pos  = getPosition(),
             prev = getPrevPosition();
    int      dirX = pos.x - prev.x,
             dirY = pos.y - prev.y;

    addToCurrentReward(Reward.BUMPING_WALL_PENALTY);
    if (debugging)
    {
      Utils.println(toString() + " charged BUMPING_WALL_PENALTY = "
                    + Reward.BUMPING_WALL_PENALTY + getCurrentTime());
    }
    
    // Stepped into a wall.
    FastRectangle rect = playingField.outerBoxInner;
    if      (pos.x < rect.x)
    {
      pos.x = rect.x;
      dirX  = -dirX;
    }
    else if (pos.x > rect.lastX)
    {
      pos.x = rect.lastX;
      dirX  = -dirX;
    }
    if      (pos.y < rect.y)
    {
      pos.y = rect.y;
      dirY  = -dirY;
    }
    else if (pos.y > rect.lastY)
    {
      pos.y = rect.lastY;
      dirY  = -dirY;
    }
    setDirection(dirX, dirY);
  }

  void hitPlayer(Player p)
  { 
    if (debugging && playingField.singleStepping)
    {
      Utils.println(toString() + " collided with " + p.toString());
    }
    setCollided(true);
    addToCurrentReward(Reward.BUMPING_PLAYER_PENALTY);
    if (debugging)
    {
      Utils.println(toString() + " charged BUMPING_PLAYER_PENALTY = "
                    + Reward.BUMPING_PLAYER_PENALTY + " at t=" + getCurrentTime());
    }
   }

  void ateVegetable(Vegetable v)
  {
    addToCurrentReward(Reward.HARVESTING_VEGETABLE_REWARD); 
    v.setEaten(true);
  }

  void movedIntoMineral(Mineral m)
  {
    m.resetStepSize(); // Return to full speed whenever hit by a player.
    setCollided(true);
    addToCurrentReward(Reward.PUSHING_MINERAL_PENALTY);
    if (debugging)
    {
      Utils.println(toString() + " charged PUSHING_MINERAL_PENALTY = "
                    + Reward.PUSHING_MINERAL_PENALTY + " at t=" + getCurrentTime());
    }
    Position dir = getDirection();
    if (m.isMoving())
    {
      if (debugging && playingField.singleStepping)
      {
        Utils.println(toString() + " (dir=" + dir.x + "," + dir.y
  		      + ") was hit by " + m.toString());
      }

      Player pusher = m.getPusher();
      if (pusher != null)
      {
        pusher.addToCurrentReward(Reward.REWARD_FOR_PUSHING_MINERAL_INTO_ANOTHER_PLAYER);
        if (debugging)
        {
          Utils.println(pusher.toString() + " charged REWARD_FOR_PUSHING_MINERAL_INTO_ANOTHER_PLAYER = "
                        + Reward.REWARD_FOR_PUSHING_MINERAL_INTO_ANOTHER_PLAYER + " at t=" + getCurrentTime());
        }
      }
      else Utils.errorMsg(m.toString() + " is moving but has no pusher");

      addToCurrentReward(Reward.HIT_BY_MOVING_MINERAL_PENALTY);
      if (debugging)
      {
        Utils.println(toString() + " charged HIT_BY_MOVING_MINERAL_PENALTY = "
                      + Reward.HIT_BY_MOVING_MINERAL_PENALTY + " at t=" + getCurrentTime());
      }
    
      if (isMoving())
      {
        m.setNextDirection(dir.x, dir.y);
        m.setNextPusher(this); // Record who last pushed mineral.
      }
      else 
      { // Mineral ran into a still player.
        m.stopMoving();
      }
    }
    else 
    { // Bug - on simultaneous hits, always goes to player with highest ID.
      if (debugging && playingField.singleStepping)
      {
        Utils.println(toString()
	              + " (dir =" + dir.x + "," +  dir.y
		      + ") hit " + m.toString());
      }

      m.setNextDirection(dir.x, dir.y); // A stationary mineral was pushed by this player.
      m.setNextPusher(this);
    }
  }

  public String toString()
  {
    return name + "(@" + getPosition().x + "," + getPosition().y + ")";
  }

}

// Catch thread-creation calls so can extend them as needed.
final class PlayerThread extends Thread
{ 
  PlayerThread(Runnable target, String name)
  {
    super(target, name);
  }

  public String toString()
  {
    return getName();
  }
}

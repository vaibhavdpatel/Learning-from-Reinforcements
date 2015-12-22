import java.awt.Color;

//
//
// Mineral - once pushed keep moving until hitting something;
//           also may be slowed by friction.
//
//

final class Mineral extends Entity
{ private static final boolean debuggingThisClass = false;

  static  Color    MineralColor = Color.darkGray;
  private Player   pusher       = null, nextPusher = null;
  static  int      Xwidth       = PlayingField.objectSize - 4,
                   Ywidth       = PlayingField.objectSize;
  int              stepSize; // Step size taken per time step (different for each mineral).
  private int      nextStepSize = 0;
  private Position nextDirection;
  private boolean  debugging, needsUpdating = false, nextDirectionSet = false;

  Mineral(int ID, int currentTime, PlayingField playingField)
  {
    super(ID, currentTime, playingField);
    nextDirection = new Position(0, 0);
    resetStepSize();
    updateMineralStatus();

    debugging = (debuggingThisClass && AgentWindow.masterDebugging);
  }

  void setNextDirection(int deltaX, int deltaY)
  {
    nextDirection.x  = deltaX;
    nextDirection.y  = deltaY;
    nextDirectionSet = true;
    needsUpdating    = true;
  }

  void setStepSize(int value)
  {
    stepSize = value;
  }

  int getStepSize()
  {
    return stepSize;
  }

  void resetStepSize()
  { // Start with big steps (ie, assume have one-third the mass and conserve momentum).
    nextStepSize  = 3 * Utils.getPlayerStepSize();
    needsUpdating = true;
  }

  void setNextStepSize(int value)
  {
    nextStepSize  = value;
    needsUpdating = true;
  }

  void setPusher(Player p)
  {
    pusher = p;
  }

  void setNextPusher(Player p)
  {
    nextPusher    = p;
    needsUpdating = true;
  }

  Player getPusher()
  {
    return pusher;
  }

  boolean isMoving()
  {
    return (getStepSize() > 0 && (getDirection().x != 0 || getDirection().y != 0));
  }

  void stopMoving()
  {
    if (debugging) Utils.println(toString() + " has stopped moving");
    setNextStepSize(0);
    nextPusher = null;
  }

  void slowDown()
  { double decayFactor = 0.95;

    if (decayFactor >= 0.0 && decayFactor < 1.0)
    { int currentStepSize = getStepSize();

      // Slow down until at speed of players.
      if (currentStepSize > PlayingField.objectSize)
      {
        setNextStepSize(Math.max(PlayingField.objectSize,
                                 (int)Math.floor(decayFactor * currentStepSize)));
        if (debugging) Utils.println(toString() + " has slowed from " + currentStepSize
                                     + " to " + nextStepSize);    
      }
    }
  }

  void returnToPrevLocationIfHit()
  {
    if (getCollided())
    { // Return to previous position and reduce step size (to nudge as close as possible).
      
      setPosition(getPrevPosition());
      setNextStepSize(getStepSize() / 4); // Will reach zero and then stop.  (Go slower than mineral hit, if any.)
      setCollided(false);
    }
  }

  void updateMineralStatus()
  {
    if (needsUpdating)
    {
      stepSize = nextStepSize;
      if (stepSize <= 0)
      {
        setDirection(0, 0);
        pusher = null;
      }
      else 
      {
        if (nextDirectionSet)
        {
          setDirection(nextDirection.x, nextDirection.y);
          nextDirectionSet = false; 
        }
        pusher = nextPusher;
      }
      needsUpdating = false;
    }
  }

  void hitPlayer(Player p)
  {
    setCollided(true); // The Player will take care of the bookkeeping.
  }

  void hitVegetable(Vegetable v)
  {
    v.setEaten(true);
    setNextStepSize(getStepSize() / 2); // Slow down going through vegetables.
  }

  void movedIntoMineral(Mineral m)
  {
    if (debugging)
    {
      Utils.println(toString() + " hit " + m.toString() + (m.isMoving() ? " [moving]" : "")
                    + " at t = " + getCurrentTime());
    }
    setCollided(true);

    Position dir = getDirection(); // Transfer momentum.
    m.setNextDirection(dir.x, dir.y);
    // Need to delay these changes until all minerals checked for collisions.
    m.setNextStepSize(getStepSize() / 2); // Only transfer half the speed.
    m.nextPusher = getPusher(); // Don't null the pusher of THIS mineral.
  }
   
  void hitBorder()
  { Position      p    = getPosition(), dir  = getDirection();
    FastRectangle rect = playingField.outerBoxInner;

    if      (p.x < rect.x)
    { // Hit on left.
      if (debugging && singleStepping()) Utils.println(toString() + "bounced off left wall");
      p.x   = rect.x;
      dir.x = -dir.x;
    }
    else if (p.x > rect.lastX)
    { // Hit on right.
      if (debugging && singleStepping()) Utils.println(toString() + "bounced off right wall");
      p.x   = rect.lastX;
      dir.x = -dir.x;
    }
    if      (p.y < rect.y)
    { // Hit on top.
      if (debugging && singleStepping()) Utils.println(toString() + "bounced off top wall");
      p.y   = rect.y;
      dir.y = -dir.y;
    }
    else if (p.y > rect.lastY)
    { // Hit on bottom.
      if (debugging && singleStepping()) Utils.println(toString() + "bounced off bottom wall");
      p.y   = rect.lastY;
      dir.y = -dir.y;
    }
    setDirection(dir.x, dir.y); // Note: may change both (since in a corner), so be careful.
  }

  public String toString()
  {
    if (debugging && isMoving())
    {
      return "Mineral" + getID()
              + "(@ " + getPosition().x + "," + getPosition().y
              + " " + getStepSize()
              + "step-size in dir = "  + getDirection().x + "," + getDirection().y + ")";
    }
    else return "Mineral" + getID() + "(@ " + getPosition().x + "," + getPosition().y + ")";
  }
}


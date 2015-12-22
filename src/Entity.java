
//
//
// Entity - an animal (player), mineral, or vegetable.
//
//

class Entity 
{ private static final boolean debuggingThisClass = false;

  private         Position p, prev, delta; // Its current and previous locations, and direction moving.
  private boolean collided, exists,        // Has it hit something? Is it alive?
                  grabbed, debugging,      // Is the user moving it?
                  canBeSeenBySelectedPlayer;
  private int     birthday, // When was it created (or reborn)?
                  ID;       // The unique ID for entities of a given type.
          static int       Xwidth = PlayingField.objectSize, // Symmetric objects by default.
                           Ywidth = PlayingField.objectSize; // - these should only be used for drawing.
          static final int offset = PlayingField.objectSize; // Treat everything as a circle (since symmetric).

  protected PlayingField playingField;
  
  Entity(int ID, int currentTime, PlayingField playingField)
  {
    p        = new Position(0, 0);
    prev     = new Position(0, 0);
    delta    = new Position(0, 0);
    this.ID  = ID;
    birthday = currentTime;
    exists   = false;
    collided = false;
    grabbed  = false;
    canBeSeenBySelectedPlayer = false;
    this.playingField = playingField;

    debugging = (debuggingThisClass && AgentWindow.masterDebugging);
  }

  void setID(int ID)
  { // Allow overriding so that various numbering schemes can be synch'ed.
    this.ID = ID;
  }

  int getID()
  {
    return ID;
  }

  void setCanBeSeenBySelectedPlayer(boolean value)
  {
    canBeSeenBySelectedPlayer = value;
  }

  boolean getCanBeSeenBySelectedPlayer()
  {
    return canBeSeenBySelectedPlayer;
  }

  void setBirthday(int bday)
  {
    //    Utils.println("bday = " + bday);
    birthday = bday;
  }

  int getBirthday()
  {
    return birthday;
  }

  void setGrabbed(boolean grabbed)
  {
    this.grabbed = grabbed;
  }

  boolean getGrabbed()
  {
    return grabbed;
  }

  void setPosition(Position newP)
  {
    p.x = newP.x;
    p.y = newP.y;
  }

  // Used when user rearranges configuration before starting.
  void directlyMoveTo(int newX, int newY)
  {
    p.x = newX;
    p.y = newY;
    setInitialPosition(p);
  }

  void setInitialPosition(Position newP)
  {
    p.x = newP.x;
    p.y = newP.y;
    prev.x = p.x;
    prev.y = p.y;
  }

  Position getPosition()
  {
    return p;
  }

  void setPrevPosition(Position newP)
  {
    prev.x = newP.x;
    prev.y = newP.y;
  }

  Position getPrevPosition()
  {
    return prev;
  }

  void setDirection(int deltaX, int deltaY)
  {
    delta.x = deltaX;
    delta.y = deltaY;
  }

  Position getDirection()
  {
    return delta;
  }

  void setExists(boolean value)
  {
    exists = value;
  }

  boolean exists(int currentTime)
  {
    return exists;
  }

  boolean isMoving()
  {
    return ((getDirection().x != 0 || getDirection().y != 0));
  }

  boolean hasMoved()
  {
    return (p.x != prev.x || p.y != prev.y);
  }

  void setCollided(boolean value)
  {
    collided = value;
  }

  boolean getCollided()
  {
    return collided;
  }

  Position getLastMouseDownPosition()
  {
    return playingField.getLastMouseDownPosition();
  }

  boolean isMouseDown()
  {
    return playingField.isMouseDown();
  }

  public int getCurrentTime() // Let all players read the clock.
  {
    return playingField.getCurrentTime();
  }

  boolean singleStepping()
  {
    return playingField.singleStepping;
  }

  // Determine the angles between the "corners" of this object, relative to the center of the "sender" object.
  void setAngles(Position posOfSender, AnglesAndDistances angles)
  { Position p         = getPosition();
    double   relativeX = p.x - posOfSender.x,
             relativeY = p.y - posOfSender.y;
    double   distance  = Math.sqrt((double)(relativeX * relativeX + relativeY * relativeY)),
             theta     = Math.atan2(relativeY, relativeX);

    // Align with x-axis, compute the two tangential angles, then rotate back.
    angles.setAngles(Math.atan2(-offset, distance) + theta,
                     Math.atan2( offset, distance) + theta);

    if (debugging && playingField.singleStepping)
    {
      Utils.println(relativeX + "," + relativeY
                    + " -> theta = " + Utils.convertToDegrees(theta)
                    + " a1 = " + angles.getAngle1()
                    + " a2 = " + angles.getAngle2());
    }
  }

  // See if this entity can be seen from the sender who's sending
  // a sonar ray out along these lines.
  boolean intersectedByRay(Position posOfSender, int degrees, AnglesAndDistances angles)
  { float   angle1 = angles.getAngle1(),
            angle2 = angles.getAngle2();
    boolean inside = false;

    if (angle1 > angle2)
    { // The acceptable arc crosses the origin (ie, degrees = 0), so need some special cases.
      if (degrees < 180) inside = (degrees < angle2);
      else               inside = (degrees > angle1);
    }
    else inside = (angle1 < degrees && degrees < angle2);

    if (debugging && inside && playingField.singleStepping)
    {
      Utils.println("Angles to " + toString()
                    + " are "    + angle1
                    + " and "    + angle2
                    + " vs "     + degrees);
    }

    return inside;
  }
}


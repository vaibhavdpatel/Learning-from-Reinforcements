public class AnglesAndDistances
{ float   angle1 = 0.0f, angle2 = 0.0f;
  int     distanceSquared = 0;
  boolean inRange = false;

  void setAngles(double angle1, double angle2)
  {
    this.angle1 = posAnglesOnly(Utils.convertToDegrees(angle1));
    this.angle2 = posAnglesOnly(Utils.convertToDegrees(angle2));
  }
  private float posAnglesOnly(double angle)
  { // Need to handle NEGATIVE degrees.
    if (angle < 0) return (float)(360 + angle);
    else return (float)angle;
  }

  float getAngle1()
  {
    return angle1;
  }

  float getAngle2()
  {
    return angle2;
  }

  void setDistanceSquared(int value)
  {
    distanceSquared = value;
  }

  int getDistanceSquared()
  {
    return distanceSquared;
  }

  void setInRange(boolean value)
  {
    inRange = value;
  }

  boolean getInRange()
  {
    return inRange;
  }

}


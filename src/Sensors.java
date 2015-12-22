import java.awt.Color;

//
//
// Sensors
//
//

public class Sensors 
{ public final static int NOTHING   = 0;
  public final static int WALL      = 1;
  public final static int ANIMAL    = 2;
  public final static int MINERAL   = 3;
  public final static int VEGETABLE = 4;

  public final static int DEGREES_BETWEEN_SENSORS =  10; // Spacing between sensors.
  public final static int NUMBER_OF_SENSORS       = 360 / Math.max(1, Math.min(DEGREES_BETWEEN_SENSORS, 90)); 
  
  int    objectType[]; // Allow access to these so they can be sent to C.
  double distance[];

  Sensors()
  {
    distance   = new double[NUMBER_OF_SENSORS];
    objectType = new int[NUMBER_OF_SENSORS];
  }

  // Return a color for sensor arcs that hit this type of entity.
  static Color getColor(int type)
  {
    switch(type) {
    case NOTHING:   return Color.lightGray;
    case WALL:      return PlayingField.WallColor;
    case ANIMAL:    return Player.PlayerColor; // The generic color for players.
    case MINERAL:   return Mineral.MineralColor;
    case VEGETABLE: return Vegetable.VegetableColor;
    default:        return Color.white;
    }
  }

  void setObjectType(int i, int type)
  {
    if (i >= 0 && i < NUMBER_OF_SENSORS && 
        (type == NOTHING || type == WALL || type == ANIMAL ||
         type == MINERAL || type == VEGETABLE))
    {
      objectType[i] = type;
     
    }
    else
    {
      Utils.errorMsg("The valid sensor ID range is [0,"
                      + (NUMBER_OF_SENSORS - 1) + "].  You provided setObjectType("
                      + i + "," + type + ")");
    }
  }
  
  void setDistance(int i, double value)
  {
    if (i >= 0 && i < NUMBER_OF_SENSORS && value >= 0.0) distance[i] = value;
    else
    {
      Utils.errorMsg("The valid sensor ID range is [0,"
                      + (NUMBER_OF_SENSORS - 1) + "].  You provided setDistance("
                      + i + "," + value + ")");
    }
  }

  public int getObjectType(int i)
  {
    if (i >= 0 && i < NUMBER_OF_SENSORS) return objectType[i];
    
    Utils.errorMsg("The valid sensor ID range is [0,"
                    + (NUMBER_OF_SENSORS - 1) + "].  You provided getObjectType("
                    + i + ")");
    return WALL;
  }

  public double getDistance(int i)
  {
    if (i >= 0 && i < NUMBER_OF_SENSORS) return distance[i];
    
    Utils.errorMsg("The valid sensor ID range is [0,"
                    + (NUMBER_OF_SENSORS - 1) + "].  You provided getDistance("
                    + i + ")");
    return Double.MAX_VALUE;
  }
  
  public void copy(Sensors sender)
  {
    System.arraycopy(sender.distance,   0, distance,   0, NUMBER_OF_SENSORS);
    System.arraycopy(sender.objectType, 0, objectType, 0, NUMBER_OF_SENSORS);
  }

}

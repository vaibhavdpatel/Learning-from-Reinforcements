//
//
// Utils
//
//

public class Utils 
{ // Note you may wish to use the first group of these to 
  // normalize inputs to your neural network.

  // The radius of ALL the objects (all are really circles internally).
  static int getObjectRadius()
  {
    return PlayingField.objectSize;
  }
  
  // The size of steps made by the players.
  static int getPlayerStepSize()
  {
    return PlayingField.objectSize;
  }

  // Get the maximum range of the sensors.
  static int getSensorRange()
  {
    return PlayingField.getSensorRange();
  }

  // Given a sensorID in [0, Sensors.NUMBER_OF_SENSORS),
  // return the sensorID that is 180 degrees opposite.
  static int oppositeDirection(int sensorID)
  {
    return (sensorID +     Sensors.NUMBER_OF_SENSORS / 2)
           % Sensors.NUMBER_OF_SENSORS;
  }

  // Choose (uniformly) any of the sensor directions.
  static int getRandomDirection()
  {
    return getRandomIntInRange(0, Sensors.NUMBER_OF_SENSORS - 1);
  }

  // Players must choose a direction to move, expressed in radians [0, 2 x PI).
  // This function helps make this conversion.
  static double convertSensorIDtoRadians(int sensorID)
  {
    return Utils.convertToRadians(Sensors.DEGREES_BETWEEN_SENSORS * sensorID);
  }

  // Sometimes (eg, readability) it is convenient to see the angle (in degrees)
  // corresponding to a sensor.
  static int convertSensorIDtoDegrees(int sensorID)
  {
    return Sensors.DEGREES_BETWEEN_SENSORS * sensorID;
  }

  // Given a sensorID in [0, Sensors.NUMBER_OF_SENSORS),
  // return the sensorID that is rotated 90 degrees CLOCKWISE.
  static int rightTurn(int sensorID)
  {
    return (sensorID +     Sensors.NUMBER_OF_SENSORS / 4)
           % Sensors.NUMBER_OF_SENSORS;
  }

  // Given a sensorID in [0, Sensors.NUMBER_OF_SENSORS),
  // return the sensorID that is rotated 90 degrees COUNTER-CLOCKWISE.
  static int leftTurn(int sensorID)
  {
    return (sensorID + 3 * Sensors.NUMBER_OF_SENSORS / 4)
           % Sensors.NUMBER_OF_SENSORS;
  }

  // Which sensor is K sensors to the right (ie, clockwise)?
  static int rightK_sensor(int sensorID, int K)
  {
    if (K < 0)
    {
      return leftK_sensor(sensorID, -K);
    }
    return (sensorID + K)
           % Sensors.NUMBER_OF_SENSORS;
  }

  // Which sensor is K sensors to the right (ie, counterclockwise)?
  static int leftK_sensor(int sensorID, int K)
  {
    if (K < 0)
    {
      return rightK_sensor(sensorID, -K);
    }
    return (sensorID + Sensors.NUMBER_OF_SENSORS - K)
           % Sensors.NUMBER_OF_SENSORS;
  }


  static void println(String s) // Allow some shorthand.
  {
    System.out.println(Thread.currentThread().toString() + ": " + s + ".");
  }

  static void errorMsg(String s) // Allow some shorthand.
  {
    System.err.println(Thread.currentThread().toString() + " produced the error: " + s + ".");
  }

  static int square(int x)
  {
    return x * x;
  }

  static void waitHere()
  {
    waitHere("Hit ENTER to continue.");
  }
  static void waitHere(String msg)
  {
    System.out.println("");
    System.out.println(msg);
    try { System.in.read(); } catch(Exception e) {} // Ignore any errors here.
  }

  static void exit(int code)
  {
    waitHere("Hit ENTER to exit.");
    System.exit(code);
  }

  static double convertToDegrees(double radians)
  {
    return radians * (180 / Math.PI);
  }

  static double convertToRadians(double degrees)
  {
    return degrees * (Math.PI / 180);
  }

  // Negative radians are interpreted as 'stand still,'
  // so if that isn't the case, be sure to convert.
  static double convertToPositiveRadians(double radians)
  {
    if (radians < 0.0) return 2 * Math.PI + radians;
    else return radians;
  }

  static double convertToPositiveDegrees(double degrees)
  {
    if (degrees < 0.0) return 360 + degrees;
    else return degrees;
  }

  // Get a random integer in [lower, upper], ie inclusively.
  static int getRandomIntInRange(int lower, int upper)
  {
     return lower + (int)(Math.round(Math.random() * (upper - lower)));
  }

  // Truncates a double and returns it as a string with at least one 
  // and at most "decimals" decimal places.
  // Puts space in front of positive numbers - so printouts align.
  static String truncate(double d, int decimals)
  { // This should always produce at least one decimal (by official documentation)
    // but it doesn't in Win95 (bug).
    String  str   = Double.toString(d);
    int     index = str.indexOf(".");

    // Patch for the above bug.  (Doesn't add "decimals" 0's, but that's ok.)
    if (index < 0) return str.concat(".0");
    
    int indexE = Utils.indexOfIgnoreCase(str, "e", index);
    if (indexE > 0) // In scientific notation.
    { String result = str.substring(0, Math.min(indexE, index + decimals + 1))
	                            + "e" + Integer.valueOf(str.substring(indexE + 1));

      if (d < 0) return result; else return " " + result;
    }
    if (d < 0)  return str.substring(0, Math.min(str.length(), index + decimals + 1));
    else return " " +  str.substring(0, Math.min(str.length(), index + decimals + 1));
  }

  // Since there isn't an CASELESS indexOf, adapt the existing code.
  static int indexOfIgnoreCase(String str, String query, int fromIndex)
  { int qlength = query.length();
    int max = (str.length() - qlength);
    
    test:
      for(int i = ((fromIndex < 0) ? 0 : fromIndex); i <= max ; i++)
      {  int k = 0, j = i, n = qlength;
         char schar, qchar;
         while (n-- > 0)
         {
           schar = str.charAt(j++);
           qchar = query.charAt(k++);
           if ((schar != qchar) &&
               (Character.toLowerCase(schar) != Character.toLowerCase(qchar)))
              continue test;
         }
         return i;
      }
    return -1;
  }

}

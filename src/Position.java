import java.awt.Point;

public class Position extends Point
{
  Position(int x, int y)
  {
    super(x, y);
  }

  public String toString()
  {
    return x + "," + y;
  }
}


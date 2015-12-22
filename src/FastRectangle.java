import java.awt.Rectangle;

public class FastRectangle extends Rectangle // Store lower-right corner for faster computation.
{ int lastX, lastY;

  public boolean inside(Position p)
  {
    return (p.x >= x && p.x <= lastX && p.y >= y && p.y <= lastY);
  }
  
}


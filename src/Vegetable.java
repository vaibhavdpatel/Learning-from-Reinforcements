import java.awt.Color;

//
//
// Vegetable - hangs around w/o moving until it dies.
//
//

final class Vegetable extends Entity
{ 
	private static final boolean debuggingThisClass = false;

	static  Color   VegetableColor = Color.green;
	private int     expiresAt, lifespan; // Schedule the death of this vegetable.
	private boolean wasEatenThisStep = false, debugging;
	static  int     Xwidth = PlayingField.objectSize, Ywidth = 3;

	Vegetable(int ID, int currentTime, PlayingField playingField)
	{
		super(ID, currentTime, playingField);
		setLifespan();
		expiresAt = currentTime + lifespan;

		debugging = (debuggingThisClass && AgentWindow.masterDebugging);
	}

	void setEaten(boolean value)
	{
		wasEatenThisStep = value;
	}

	void setLifespan()
	{ 
		this.lifespan = 100 + (int)(800 * Math.random());
	}

	void gameOver()
	{
		setBirthday(0);
		setLifespan();
	}

	void checkIfEaten(int currentTime, FastRectangle limitingRectangle)
	{
		if (wasEatenThisStep)
		{ 
			if (debugging) Utils.println(toString() + " was eaten this step and will regrow nearby later");
			perturbLocation(PlayingField.objectDiameter, limitingRectangle);
			// Wait awhile until regrowing (in a nearby place).
			setBirthday(currentTime + 25 + (int)(Math.random() * lifespan / 2));
			if (Math.random() < 0.01) setLifespan();
			setEaten(false); // Clear the flag.
		}
	}

	void perturbLocation(int range, FastRectangle limitingRectangle)
	{
		perturbLocation(range, range, limitingRectangle);
	}
	void perturbLocation(int rangeX, int rangeY, FastRectangle limitingRectangle)
	{ // Have a small drift to the upper-right to create an asymmetry in the world.
		Position p = getPosition();

		p.x += Utils.getRandomIntInRange(-rangeX, rangeX);
		p.y += Utils.getRandomIntInRange(-rangeY, rangeY);
		if (Math.random() < 0.5) p.x++;
		if (Math.random() < 0.5) p.y--;

		// Watch to make sure doesn't meander out of the legal region.
		// If it does, reappear in any legal place.
		if (p.x < limitingRectangle.x || p.x > limitingRectangle.lastX)
		{
			p.x = limitingRectangle.x + (int)(Math.random() * (limitingRectangle.width  - 1));
		}
		if (p.y < limitingRectangle.y || p.y > limitingRectangle.lastY)
		{
			p.y = limitingRectangle.y + (int)(Math.random() * (limitingRectangle.height - 1));
		}
	}

	void setBirthday(int birthday)
	{     
		super.setBirthday(birthday);
		expiresAt = birthday + lifespan;
	}

	// Specialize by checking birthday and expiration date.
	boolean exists(int currentTime)
	{
		if (currentTime == expiresAt)
		{ // This vegetable has died.  Schedule its rebirth.
			setBirthday(currentTime + 25 + (int)(Math.random() * lifespan));
			if (Math.random() < 0.01) setLifespan();
		}
		return (super.exists(currentTime)    &&
				currentTime >= getBirthday() &&
				currentTime <  expiresAt);
	}

	public String toString()
	{
		return "Vegetable" + getID() + "(@ " + getPosition().x + "," + getPosition().y + ")";
	}

}


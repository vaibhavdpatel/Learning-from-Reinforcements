import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

//
//
// PlayingField
//
//

class PlayingField extends Canvas implements MouseListener
{ 

	private static final boolean debuggingThisClass = false;

	static final int objectSize     = 10, // The radius of the circular objects.
			objectDiameter =  2 * objectSize,
			objectDiameterSquared = Utils.square(objectDiameter);

	static     Color WallColor = Color.blue,
			FieldColor = new Color(25, 150,50); // A green.
	private static final int maxLayoutTries = 1000, // Try random placements no more than this many times.
			maxPlayers     =   25, maxVegetables = 200, maxMinerals = 100;
	private              int sensorRangeSquared,
	desiredWidth   =  150, desiredHeight = 100; // These settings are the minimums.

	private Boolean   synchForPlayerCheckIn = new Boolean(true); // One per field is fine here.

	private Sensors   sensors; // Fill to these, then "synch" copy.
	private Image     offscreen = null; // A double-buffer drawing scheme is used.
	private Graphics  offscreenGraphics;
	private Dimension d = null;
	private Position  centerPosition, tempPosition;
	private Rectangle innerBoxInner, innerBoxOuter, mineralsRectangle, vegetablesRectangle;
	private boolean   configured       = false, // Has the initial configuration been created?                    
			allowReshaping   = true,  // Can this canvas be reshaped?
			started          = false, // Has the START button been pushed?
			circleAllObjects = false, // Show true boundaries of objects.
			reportSlowPlayers           = false,
			onlySeeViewOfSelectedPlayer = false,
			togglingShowingSensors      = false,
			displayOn                   = true,
			debugging;
	private int       offscreenWidth   =  -1, offscreenHeight  = -1,
			playersReadyToGo =   0, playersResumed   =  0,
			clockPeriod      = 100, lastMeasuredTime =  0;

	// Have access to user clicks.
	private Position mouseUpAt, mouseDownAt;
	private boolean  mouseDown      = false;
	private Entity   entityGrabbed  = null, viewFromMe = null;
	private Player   selectedPlayer = null;

	// The manager thread needs access to these.
	Player        player[];
	Mineral       mineral[];
	Vegetable     vegetable[];
	AgentWindow   agentWindow;
	FastRectangle outerBoxInner;
	boolean       showHelp, paused = false, singleStepping = false; // Stop after each step?
	int           playersSoFar = 0, vegetablesSoFar =  0, mineralsSoFar =  0,
			gamesPlayed  = 0, gamesToPlay     = -1, gameDuration  = -1; // Indicate these should be ignored.

	AnglesAndDistances sensorsToPlayers[], sensorsToMinerals[], sensorsToVegetables[];

	private ManagerThread managerThread        = null;
	private WatcherThread managerWatcherThread = null;


	// Constructor
	PlayingField(AgentWindow agentWindow, int width, int height)
	{
		super();
		desiredWidth  = Math.max(width,  desiredWidth);
		desiredHeight = Math.max(height, desiredHeight);
		validate();

		// Set this so no objects will fit between the sensors.
		sensorRangeSquared = Utils.square(Utils.getSensorRange());
		if (debugging) Utils.println("Sensor range = " + Utils.getSensorRange());

		this.agentWindow = agentWindow;
		createManagerThread();

		sensors      = new Sensors();

		centerPosition = new Position( 0,  0);
		tempPosition   = new Position( 0,  0);
		mouseDownAt    = new Position(-1, -1);
		mouseUpAt      = new Position(-1, -1);

		innerBoxInner       = new Rectangle();
		innerBoxOuter       = new Rectangle();
		outerBoxInner       = new FastRectangle();
		mineralsRectangle   = new Rectangle();
		vegetablesRectangle = new Rectangle();

		// Probably should do all of these with extendable vectors ...
		// However, no "records" are created until needed, so the overhead is low.
		player      = new Player[maxPlayers];
		mineral     = new Mineral[maxMinerals];
		vegetable   = new Vegetable[maxVegetables];

		sensorsToPlayers    = new AnglesAndDistances[maxPlayers];
		sensorsToMinerals   = new AnglesAndDistances[maxMinerals];
		sensorsToVegetables = new AnglesAndDistances[maxVegetables];

		setVisible(true);
		super.setSize(width, height);
		allowReshaping = false;
		addMouseListener(this);

		debugging = (debuggingThisClass && AgentWindow.masterDebugging);
	}

	void addPlayer(Player newPlayer, String name, Color color, Score score)
	{
		if (playersSoFar >= maxPlayers)
		{
			Utils.errorMsg("You have reached the maximum number of players (" + maxPlayers + ")");
			return;
		}

		if (newPlayer == null) Utils.errorMsg("Cannot pass NULL to addPlayer");
		else
		{ int id = playersSoFar++;

		newPlayer.setID(id); // Need IDs to match the numbering scheme here.
		player[id] = newPlayer;
		newPlayer.setColor(color);
		newPlayer.setOrigColor(color); // Need this in case of resets.
		newPlayer.setName(name);
		newPlayer.setScoreContainer(score);
		sensorsToPlayers[id] = new AnglesAndDistances();
		}
	}

	void addMineral()
	{
		if (mineralsSoFar < maxMinerals)
		{ int id = mineralsSoFar++;

		mineral[id] = new Mineral(id, getCurrentTime(), this);
		sensorsToMinerals[id] = new AnglesAndDistances();
		}
	}

	void addVegetable()
	{
		if (vegetablesSoFar < maxVegetables)
		{ int id = vegetablesSoFar++; // Recall that internal time is in number of steps taken.

		vegetable[id] = new Vegetable(id, getCurrentTime(), this);
		sensorsToVegetables[id] = new AnglesAndDistances();
		}
	}

	void useSingleStepMode(boolean value)
	{
		singleStepping = value;
		if (!singleStepping) resume();
	}

	// Don't resize/reshape once running.
	public void resize(Dimension d)
	{
		if (allowReshaping) super.resize(d);
	}
	public void setSize(int  width, int  height)
	{
		if (allowReshaping) super.setSize(width, height);
	}
	public void setBounds(int  x, int  y, int  width, int  height)
	{
		if (allowReshaping) super.setBounds(x, y, width, height);
		else
		{ // Keep desired size, but recenter.

			super.setBounds(x + (width  - desiredWidth)  / 2,
					y + (height - desiredHeight) / 2,
					desiredWidth, desiredHeight);
		}
	}
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}
	public Dimension getPreferredSize()
	{
		return new Dimension(desiredWidth, desiredHeight);
	}

	void gamesToPlay(int games)
	{
		gamesToPlay = Math.max(0, games);
	}

	void gameDuration(int duration)
	{
		gameDuration = Math.max(0, duration);
	}

	void setCircleAllObjects(boolean value)
	{
		if (circleAllObjects != value)
		{
			circleAllObjects = value;
			redisplay(true);
			if (value) agentWindow.reportInInfoBar("All objects are internally treated as circles.");
			else agentWindow.reportInInfoBar("");
		}
	}

	void setReportSlowPlayers(boolean value)
	{
		reportSlowPlayers = value; // Need to store in case manager dies.
		managerThread.setReportSlowPlayers(value);
		if (value) agentWindow.reportInInfoBar("Any players who aren't ready when the clock ticks will be reported to the console (and be colored black).");
		else agentWindow.reportInInfoBar("");
	}

	void setOnlySeeViewOfSelectedPlayer(boolean value, boolean deselectSelectedPlayer)
	{
		if (deselectSelectedPlayer) selectedPlayer = null; // Unselect the player.

		if (!value)
		{
			if (onlySeeViewOfSelectedPlayer != value) agentWindow.reportInInfoBar("");
		}
		else agentWindow.reportInInfoBar("Use the mouse to select the player whose viewpoint you wish to see.");
		onlySeeViewOfSelectedPlayer = value;
	}

	void setToggleShowingSensorsOfSelectedPlayer(boolean value, boolean deselectSelectedPlayer)
	{
		if (deselectSelectedPlayer) selectedPlayer = null; // Unselect the player.

		if (!value)
		{
			if (togglingShowingSensors != value) agentWindow.reportInInfoBar("");
		}
		else agentWindow.reportInInfoBar("Use the mouse to select players whose displaySensors flag you wish to toggle.");
		togglingShowingSensors = value;
	}

	void showHelp()
	{
		showHelp = true;
		pause();
		repaint();
	}

	private boolean onlyDrawIfSeen()
	{
		return (onlySeeViewOfSelectedPlayer && selectedPlayer != null);
	}

	void redisplay(boolean override)
	{ // Override having the display being on hold.
		if (override && !isDisplayOn()) agentWindow.setDisplayON();
		else redisplay();
	}
	void redisplay()
	{
		if (displayOn) repaint();
	}

	boolean isDisplayOn()
	{
		return displayOn;
	}

	void setDisplayOn(boolean value)
	{
		if (displayOn != value)
		{
			displayOn = value;
			repaint();
			for(int i = 0; i < playersSoFar; i++) if (player[i].getScoreLabel() != null)
			{
				if (value) player[i].getScoreLabel().showScore();
				else       player[i].getScoreLabel().hideScore();
			}
		}
	}

	// Update the "offscreen" image buffer, then dump it to the screen.
	public void update(Graphics g)
	{
		paint(g);
	}

	public void paint(Graphics g)
	{ 
		if (!configured) // Once configured, won't be changing the size of this canvas.
		{ d = getSize();

		if (debugging && (d.width != desiredWidth || d.height != desiredHeight))
		{
			Utils.println("Inconsistency: w = " + desiredWidth + " vs " + d.width
					+ "  h = " + desiredHeight + " vs " + d.height);
			Utils.exit(-1);
		}

		if (d.width  != offscreenWidth || // Create a new buffered screen if
				d.height != offscreenHeight)  // the screen has been resized.
		{ // Place the players in the middle of the left column.
			innerBoxInner.x      =                  0;
			innerBoxInner.y      =      d.height /  8;
			innerBoxInner.width  =  3 * d.width  / 16;
			innerBoxInner.height =  3 * d.height /  4;

			// Wrap a box around this st. no objects that fall in it will overlap the inner box.
			innerBoxOuter.x      = innerBoxInner.x      -     objectSize;
			innerBoxOuter.y      = innerBoxInner.y      -     objectSize;
			innerBoxOuter.width  = innerBoxInner.width  + objectDiameter;
			innerBoxOuter.height = innerBoxInner.height + objectDiameter;

			// Place the minerals in this rectangle.   They wont be placed in outerBoxInner.
			mineralsRectangle.x      = 0 * d.width / 10;
			mineralsRectangle.y      =                0;
			mineralsRectangle.width  = 8 * d.width / 10;
			mineralsRectangle.height =     d.height;

			// Place the vegetables in this rectangle.  They wont be placed in outerBoxInner.
			vegetablesRectangle.x      = d.width ;
			vegetablesRectangle.y      =            0;
			vegetablesRectangle.width  = d.width ;
			vegetablesRectangle.height =     d.height;

			// A box around the playing field that'll guarantee the player will be on fully on the field.
			outerBoxInner.x      = objectSize + 2; // Positions have objectSize subtracted from them,
			outerBoxInner.y      = objectSize + 2; // and the border is in row/column 0.
			outerBoxInner.width  = d.width  - objectDiameter - 4; // The border takes up four pixels.
			outerBoxInner.height = d.height - objectDiameter - 4;
			outerBoxInner.lastX  = outerBoxInner.x + outerBoxInner.width  - 1;
			outerBoxInner.lastY  = outerBoxInner.x + outerBoxInner.height - 1;

			offscreenWidth    = d.width;
			offscreenHeight   = d.height;
			offscreen         = createImage(d.width, d.height);
			offscreenGraphics = offscreen.getGraphics();
		}
		}

		offscreenGraphics.setColor(FieldColor);
		offscreenGraphics.fillRect(0, 0, d.width,     d.height);
		offscreenGraphics.setColor(WallColor);
		offscreenGraphics.drawRect(0, 0, d.width - 1, d.height - 1);
		offscreenGraphics.drawRect(1, 1, d.width - 3, d.height - 3);

		if (showHelp)
		{ int indent = 15, spacing = 20;
		String s1  = "Help for the Agent World",
				s2  = "Press " + AgentWindow.configureLabel.trim() + " to create an initial configuration.",
				s3  = "   You can use the mouse to move objects around (hold the mouse down over an object and drag it).",
				s4  = "   Or click on reinitialize to get a new random initial state.",
				s5  = "Press " + AgentWindow.startLabel.trim() + " to begin the Agent World.  You can request that the simulator wait for you to press " + AgentWindow.goLabel.trim() + " before each step.",
				s6  = (isDisplayOn()
						? "Press \"" + AgentWindow.displayOnLabel.trim()  + "\" to save cpu cycles for training."
								: "Press \"" + AgentWindow.displayOffLabel.trim() + "\" to see what is happening."),
								s7  = "Clicking on a player reports its name and current score.  Pressing Pause also produces plots of players' scores across games.",
								s8  = "If a FollowMouse player has been created, it'll move toward the mouse location whenever a mouse button is down.",
								s9  = " ",
								s10 = "Additional options are available via the menu:",
								s11 = "   You can have the simulator report which players haven't selected a move in the allotted interval.",
								s12 = "   You can request to only see what the selected player sees.  (See how high you can score guiding a FollowMouse player.)",
								s13 = "   Or you can have the sensors readings of the selected player be displayed.  (Light grey lines indicate nothing sensed.)",
								s14 = "   Finally, you can have the display show the 'true' (circular) shape of all objects.",
								sN  = "Click the mouse on the playing field (i.e., this window) in order to return to the Agent World.";

		offscreenGraphics.setColor(Color.black);
		offscreenGraphics.setFont(AgentWindow.regularBoldFont);
		offscreenGraphics.drawString(s1, indent / 2,  spacing);
		offscreenGraphics.setFont(AgentWindow.helpFont);
		offscreenGraphics.drawString(s2,  indent,  2 * spacing);
		//offscreenGraphics.setFont(AgentWindow.tinyBoldFont);
		offscreenGraphics.drawString(s3,  indent,  3 * spacing);
		offscreenGraphics.drawString(s4,  indent,  4 * spacing);
		offscreenGraphics.setFont(AgentWindow.helpFont);
		offscreenGraphics.drawString(s5,  indent,  5 * spacing);
		offscreenGraphics.drawString(s6,  indent,  6 * spacing);
		offscreenGraphics.drawString(s7,  indent,  7 * spacing);
		offscreenGraphics.drawString(s8,  indent,  8 * spacing);
		offscreenGraphics.drawString(s9,  indent,  9 * spacing);
		//offscreenGraphics.setFont(AgentWindow.tinyBoldFont);
		offscreenGraphics.drawString(s10, indent, 10 * spacing);
		offscreenGraphics.drawString(s11, indent, 11 * spacing);
		offscreenGraphics.drawString(s12, indent, 12 * spacing);
		offscreenGraphics.drawString(s13, indent, 13 * spacing);
		offscreenGraphics.drawString(s14, indent, 14 * spacing);
		offscreenGraphics.setColor(Color.blue);
		offscreenGraphics.setFont(AgentWindow.smallFont);
		offscreenGraphics.drawString(sN,  indent, 16 * spacing);
		}
		else if (!displayOn)
		{
			writeMessageOnPlayingField(AgentWindow.largeFont,
					"The playing field is not being displayed to conserve cpu cycles for training",
					"and testing.  Click on \"" + agentWindow.displayOffLabel.trim() + "\" to resume displaying.");
		}
		else if (configured)
		{ Position p;
		int currentTime = getCurrentTime();

		for(int i = 0; i < playersSoFar; i++) if (player[i].exists(currentTime))
		{ p = player[i].getPosition();

		if (onlyDrawIfSeen() && selectedPlayer != player[i]) continue;

		if (player[i].getShowSensors())
		{ // Draw the sensors first so that the interior lines get covered.
			for(int dir = 0; dir < Sensors.NUMBER_OF_SENSORS; dir++)
			{ double radians  = Utils.convertSensorIDtoRadians(dir);
			double distance = player[i].getSensors().getDistance(dir);

			offscreenGraphics.setColor(Sensors.getColor(player[i].getSensors().getObjectType(dir)));
			offscreenGraphics.drawLine(p.x, p.y,
					p.x + (int)Math.round(distance * Math.cos(radians)),
					p.y + (int)Math.round(distance * Math.sin(radians)));
			}
		}
		}

		for(int i = 0; i < playersSoFar; i++) if (player[i].exists(currentTime))
		{ p = player[i].getPosition();

		if (onlyDrawIfSeen() && selectedPlayer != player[i] && !player[i].getCanBeSeenBySelectedPlayer()) continue;

		offscreenGraphics.setColor(player[i].getColor());
		offscreenGraphics.fillOval(p.x - Player.Xwidth, p.y - Player.Ywidth,
				2 * Player.Xwidth,   2 * Player.Ywidth);
		}

		for(int i = 0; i < mineralsSoFar; i++) if (mineral[i].exists(currentTime))
		{ p = mineral[i].getPosition();

		if (onlyDrawIfSeen() && !mineral[i].getCanBeSeenBySelectedPlayer()) continue;

		if (circleAllObjects)
		{
			offscreenGraphics.setColor(Color.white);
			offscreenGraphics.fillOval(p.x - objectSize, p.y - objectSize,
					objectDiameter,   objectDiameter);
		}
		offscreenGraphics.setColor(Mineral.MineralColor);
		offscreenGraphics.fillOval(p.x - Mineral.Xwidth, p.y - Mineral.Ywidth,
				2 * Mineral.Xwidth,   2 * Mineral.Ywidth);
		if (debugging && i < 10)
		{
			offscreenGraphics.setColor(Color.white);
			offscreenGraphics.drawString(mineral[i].getID() + "",
					p.x - Mineral.Xwidth + 1,
					p.y + 6);
		}
		}

		for(int i = 0; i < vegetablesSoFar; i++) if (vegetable[i].exists(currentTime))
		{ p = vegetable[i].getPosition();

		if (onlyDrawIfSeen() && !vegetable[i].getCanBeSeenBySelectedPlayer()) continue;

		if (circleAllObjects)
		{
			offscreenGraphics.setColor(Color.black);
			offscreenGraphics.fillOval(p.x - objectSize, p.y - objectSize,
					objectDiameter,   objectDiameter);
		}
		offscreenGraphics.setColor(Vegetable.VegetableColor);
		offscreenGraphics.fillOval(p.x - Vegetable.Xwidth, p.y - Vegetable.Ywidth,
				2 * Vegetable.Xwidth,   2 * Vegetable.Ywidth);
		if (debugging && i < 10)
		{
			offscreenGraphics.setColor(Color.black);
			offscreenGraphics.drawString(vegetable[i].getID() + "",
					p.x - Vegetable.Xwidth + 1,
					p.y + 6);
		}
		}

		if ((started && selectedPlayer != null) ||
				(!started && entityGrabbed != null)) // Indicate that the grabbed object is located here.
		{ 
			if (started) p = selectedPlayer.getPosition(); else p = entityGrabbed.getPosition();

			offscreenGraphics.setColor(Color.white);
			offscreenGraphics.fillOval(p.x - Player.Xwidth / 2, p.y - Player.Ywidth / 2,
					Player.Xwidth,           Player.Ywidth);
		}

		if (paused && started && gamesPlayed > 0) // Report game scores.
		{ int xDim      = gamesPlayed + 4, // Have a little interior border, as well.
		yDim      = 40,
		xHalfDim  = xDim / 2,
		yDimUpper = (8 * yDim) / 10,
		yDimLower = (2 * yDim) / 10,
		yScale    = yDimUpper - 2, // Leave a little border.
		maxScoreUpper =  3000,     // Truncate if outside of +/- this value.
		maxScoreLower = -3000 / 4; // Need to match this 4 and the 8/2 ratio above!
		for(int i = 0; i < playersSoFar; i++) if (player[i].exists(currentTime) && player[i].showScores)
		{
			p = player[i].getPosition();

			int yAxis    = p.x - xHalfDim - 2, // Center above the player.
					xAxis    = p.y - Player.Ywidth / 2 - yDimLower - 10; // Recall there is a border around the box.

			offscreenGraphics.setColor(Color.black);
			offscreenGraphics.fillRect(yAxis - 4, xAxis - yDimUpper - 4, xDim + 8, yDim + 8);
			offscreenGraphics.setColor(Color.gray);
			offscreenGraphics.fillRect(yAxis - 2, xAxis - yDimUpper - 2, xDim + 4, yDim + 4);

			// Draw the axes.
			offscreenGraphics.setColor(Color.blue);
			offscreenGraphics.drawLine(yAxis, xAxis, yAxis + gamesPlayed + 2, xAxis); // The xAxis line.
			offscreenGraphics.drawLine(yAxis, xAxis - (yScale * maxScoreLower) / maxScoreUpper,
					yAxis, xAxis - yScale);

			for(int game = 0; game < gamesPlayed; game++)
			{ int temp = Math.max(maxScoreLower, Math.min(player[i].getGameScore(game), maxScoreUpper));

			if      (temp > 0)
			{ // Negate since Y-down is increasing, but want to use the usual convention.

				offscreenGraphics.setColor(Color.green);
				offscreenGraphics.drawLine(yAxis + game + 1, xAxis - 1,
						yAxis + game + 1, xAxis - (yScale * temp) / maxScoreUpper);
			}
			else if (temp < 0)
			{
				offscreenGraphics.setColor(Color.red);
				offscreenGraphics.drawLine(yAxis + game + 1, xAxis + 1,
						yAxis + game + 1, xAxis - (yScale * temp) / maxScoreUpper);
			}
			}
		}
		agentWindow.reportInInfoBar("Scores over "  + gamesPlayed 
				+ " games are plotted in [" + maxScoreLower + "," + maxScoreUpper
				+ "].  If no other options have been selected, clicking on a player will toggle showing its plot.");
		}
		}
		else
		{ 
			writeMessageOnPlayingField(AgentWindow.largeFont,
					"Press Configure to create initial configurations.",
					"Press Start to begin the Agent World.");
		}

		g.drawImage(offscreen, 0, 0, null);
	}

	void writeMessageOnPlayingField(Font fontToUse, String s1, String s2)
	{ FontMetrics fontMetrics = offscreenGraphics.getFontMetrics(fontToUse);

	offscreenGraphics.setColor(WallColor);
	offscreenGraphics.setFont(fontToUse);
	offscreenGraphics.drawString(s1, (d.width - fontMetrics.stringWidth(s1)) / 2, d.height / 2 - 15);
	offscreenGraphics.drawString(s2, (d.width - fontMetrics.stringWidth(s1)) / 2, d.height / 2 + 20);
	}

	void configure(boolean calledFromConfigureButton)
	{
		while (offscreen == null)
		{ // May need to leave time for the AWT process to create the window.
			try { Thread.currentThread().sleep(1000); }
			catch(Exception e) { Utils.println("Waiting for the Agent World window to be created"); } 
		}

		if (calledFromConfigureButton) // First time through?
		{
			agentWindow.reportInInfoBar("Use the mouse to rearrange items or press Start to begin the simulation.  You can also request a new random initialization.");
		}
		configured = true; // Shape can no longer be changed.

		// First, essentially move all objects off the screen.
		for(int i = 0; i < playersSoFar;    i++) player[i].setExists(false);
		for(int i = 0; i < mineralsSoFar;   i++) mineral[i].setExists(false);
		for(int i = 0; i < vegetablesSoFar; i++) vegetable[i].setExists(false);

		for(int i = 0; i < playersSoFar; i++)
		{ int layoutTries = 0; // Prevent infinite loops.

		do
		{ 
			if      (player[i] instanceof Assassin)
			{ // Have the assassins start by lurking out there ...

				player[i].setInitialPosition(createPointInsideBox(mineralsRectangle, null));
			}
			else
			{ // Place regular players in a special place.
				// If createPoint's layoutTries exceeded, don't allow inside the vegetables region
				// (ie, if necessary, place nearby).

				player[i].setInitialPosition(createPointInsideBox(innerBoxInner, vegetablesRectangle));
			}
		}
		while (intersectingPlayer(i) != null && ++layoutTries <= maxLayoutTries);

		if (debugging && intersectingPlayer(i) != null) Utils.errorMsg("Wasn't able to position a player without overlapping another");

		player[i].setExists(true);
		}

		for(int i = 0; i < mineralsSoFar; i++)
		{ int layoutTries = 0;

		do
		{ // Place these outside of the middle box.
			mineral[i].setInitialPosition(createPointInsideBox(mineralsRectangle, innerBoxOuter));
			if (debugging && layoutTries > 0) Utils.println(mineral[i].toString() + " re-layed out");
		} 
		while (intersectingMineral(i) != null && ++layoutTries <= maxLayoutTries);

		if (intersectingMineral(i) != null)
		{
			if (debugging) Utils.errorMsg("Wasn't able to position a mineral without overlapping another object");
			mineral[i].setExists(false); // Leave this one out.
		}
		else mineral[i].setExists(true);
		}

		// Put minerals down last, since some overlaps here are much less significant.
		for(int i = 0; i < vegetablesSoFar; i++)
		{ int layoutTries = 0;

		do
		{ // Place these outside of the middle box.
			vegetable[i].setInitialPosition(createPointInsideBox(vegetablesRectangle, innerBoxOuter));
			if (debugging && layoutTries > 0) Utils.println(vegetable[i].toString() + " re-layed out");
		} 
		while (intersectingVegetable(i) != null && ++layoutTries <= maxLayoutTries);

		if (intersectingVegetable(i) != null)
		{
			vegetable[i].setBirthday(10); // The rebirth code will handle these
		}
		vegetable[i].setExists(true); // Still want the overlapper's to exist,
		}

		redisplay();
	}

	// What position is the middle of the playing field?
	private Position centerPosition()
	{
		centerPosition.x = d.width  / 2;
		centerPosition.y = d.height / 2;

		return centerPosition;
	}

	private Position createPointInsideBox(Rectangle box, Rectangle avoid)
	{ int layoutTries = 0;

	do
	{
		tempPosition.x = (int)(Math.random() * d.width);
		tempPosition.y = (int)(Math.random() * d.height);
	}
	// Be safe and make sure inside the outerBox (for safety) as well as the inner.
	// If avoid != null, don't place inside it.
	while ((!box.contains(tempPosition.x, tempPosition.y) && ++layoutTries < maxLayoutTries) ||
			(avoid != null && avoid.contains(tempPosition.x, tempPosition.y)) ||
			!outerBoxInner.inside(tempPosition));

	return tempPosition;
	}

	// Does player X intersect with any other entity (except itself, of course)?
	private Entity intersectingEntity(Entity entityX)
	{ int currentTime = getCurrentTime();

	for(int i = 0; i < playersSoFar; i++)    if (player[i].exists(currentTime)) // Only consider actives.
	{
		if (distanceSquaredBetweenEntities(player[i], entityX)    <= objectDiameterSquared) return player[i];
	}

	for(int i = 0; i < mineralsSoFar; i++)   if (mineral[i].exists(currentTime))
	{
		if (distanceSquaredBetweenEntities(mineral[i], entityX)   <= objectDiameterSquared) return mineral[i];
	}

	for(int i = 0; i < vegetablesSoFar; i++) if (vegetable[i].exists(currentTime))
	{
		if (distanceSquaredBetweenEntities(vegetable[i], entityX) <= objectDiameterSquared) return vegetable[i];
	}

	return null;
	}

	Entity intersectingPlayer(int X)
	{ 
		return intersectingEntity(player[X]);
	}

	Entity intersectingMineral(int X)
	{ 
		return intersectingEntity(mineral[X]);
	}

	Entity intersectingVegetable(int X)
	{ 
		return intersectingEntity(vegetable[X]);
	}

	// Compute this distance from OUTER EDGES (not the centers), assuming everything is a SQUARE.
	private int distanceSquaredBetweenEntities(Entity a, Entity b)
	{ 
		if (a == b) return Integer.MAX_VALUE; // Use this hack so no self collisions.

		Position ptA = a.getPosition(), ptB = b.getPosition();

		return Utils.square(ptA.x - ptB.x) + Utils.square(ptA.y - ptB.y);
	}

	void updateSensorsAndRewards()
	{ int currentTime = getCurrentTime();

	for(int i = 0; i < playersSoFar; i++) if (player[i].exists(currentTime) && player[i].getComputeSensors())
	{ Position pos = player[i].getPosition();
	boolean  sensingFromSelectedOne = (onlySeeViewOfSelectedPlayer && player[i] == selectedPlayer);

	// First compute the distances and angles to all the other entities.
	for(int j = 0; j < playersSoFar;    j++) if (player[j].exists(currentTime) && i != j)
	{ int distanceSquared;

	if (sensingFromSelectedOne) player[j].setCanBeSeenBySelectedPlayer(false);
	distanceSquared = distanceSquaredBetweenEntities(player[i], player[j]);
	sensorsToPlayers[j].setDistanceSquared(distanceSquared);
	if (distanceSquared < sensorRangeSquared)
	{
		sensorsToPlayers[j].setInRange(true);
		player[j].setAngles(pos, sensorsToPlayers[j]);
	}
	else
	{
		sensorsToPlayers[j].setInRange(false);
	}
	}

	for(int j = 0; j < mineralsSoFar;   j++) if (mineral[j].exists(currentTime))
	{ int distanceSquared;

	if (sensingFromSelectedOne) mineral[j].setCanBeSeenBySelectedPlayer(false);
	distanceSquared = distanceSquaredBetweenEntities(player[i], mineral[j]);
	sensorsToMinerals[j].setDistanceSquared(distanceSquared);

	if (distanceSquared < sensorRangeSquared)
	{
		sensorsToMinerals[j].setInRange(true);
		mineral[j].setAngles(pos, sensorsToMinerals[j]);
	}
	else
	{
		sensorsToMinerals[j].setInRange(false);
	}
	}

	for(int j = 0; j < vegetablesSoFar;  j++) if (vegetable[j].exists(currentTime))
	{ int distanceSquared;

	if (sensingFromSelectedOne) vegetable[j].setCanBeSeenBySelectedPlayer(false);
	distanceSquared = distanceSquaredBetweenEntities(player[i], vegetable[j]);
	sensorsToVegetables[j].setDistanceSquared(distanceSquared);

	if (distanceSquared < sensorRangeSquared)
	{
		sensorsToVegetables[j].setInRange(true);
		vegetable[j].setAngles(pos, sensorsToVegetables[j]);
	}
	else
	{
		sensorsToVegetables[j].setInRange(false);
	}
	}

	// Now step through the angles and record what is seen.
	for(int dir = 0; dir < Sensors.NUMBER_OF_SENSORS; dir++)
	{ Entity entityHit       = null;
	int    entityHitType   = Sensors.NOTHING,
			degrees         = Utils.convertSensorIDtoDegrees(dir),
			distanceSquared = sensorRangeSquared, currentDistanceSquared;

	for(int j = 0; j < playersSoFar;    j++) if (player[j].exists(currentTime) && i != j && sensorsToPlayers[j].getInRange())
	{ 
		currentDistanceSquared = sensorsToPlayers[j].getDistanceSquared();
		if (currentDistanceSquared < distanceSquared && player[j].intersectedByRay(pos, degrees, sensorsToPlayers[j]))
		{
			distanceSquared = currentDistanceSquared;
			entityHit       = player[j];
			entityHitType   = Sensors.ANIMAL;
		}
	}

	for(int j = 0; j < mineralsSoFar;   j++) if (mineral[j].exists(currentTime) && sensorsToMinerals[j].getInRange())
	{ 
		currentDistanceSquared = sensorsToMinerals[j].getDistanceSquared();
		if (currentDistanceSquared < distanceSquared && mineral[j].intersectedByRay(pos, degrees, sensorsToMinerals[j]))
		{
			distanceSquared = currentDistanceSquared;
			entityHit       = mineral[j];
			entityHitType   = Sensors.MINERAL;
		}
	}

	for(int j = 0; j < vegetablesSoFar; j++) if (vegetable[j].exists(currentTime) && sensorsToVegetables[j].getInRange())
	{ 
		currentDistanceSquared = sensorsToVegetables[j].getDistanceSquared();
		if (currentDistanceSquared < distanceSquared && vegetable[j].intersectedByRay(pos, degrees, sensorsToVegetables[j]))
		{
			distanceSquared = currentDistanceSquared;
			entityHit       = vegetable[j];
			entityHitType   = Sensors.VEGETABLE;
		}
	}

	if (entityHit == null)
	{ int    toX, toY;
	double rX,  rY, denomX, denomY, radians = degrees * (Math.PI / 180);
	// Didn't hit an entity, so figure out the distance to the wall along this direction.

	if      (degrees <  90)
	{ 
		toX = d.width  - pos.x;
		toY = d.height - pos.y;

		denomX = Math.cos(radians);
		denomY = Math.sin(radians);
	}
	else if (degrees < 180)
	{ 
		toX =            pos.x;
		toY = d.height - pos.y;

		denomX = Math.sin(radians - Math.PI / 2);
		denomY = Math.cos(radians - Math.PI / 2);
	}
	else if (degrees < 270)
	{ 
		toX =            pos.x;
		toY =            pos.y;

		denomX = Math.cos(radians - Math.PI);
		denomY = Math.sin(radians - Math.PI);
	}
	else
	{ 
		toX = d.width  - pos.x;
		toY =            pos.y;

		denomX = Math.sin(radians - 3 * Math.PI / 2);
		denomY = Math.cos(radians - 3 * Math.PI / 2);
	}

	if (toX == 0 || toY == 0)
	{
		Utils.errorMsg("have toX = " + toX + " and toY = " + toY + " for " + player[i].toString());
		Utils.exit(-1);
	}

	int distanceSqToWall = 0;
	if (Math.abs(denomX) < 0.00001 && Math.abs(denomY) < 0.00001)
	{
		Utils.errorMsg("Unexpectedly, have both a tiny denomX and a tiny denomY in distanceSqToWall.");
		Utils.exit(-1);
	}
	else if (Math.abs(denomX) < 0.00001)
	{
		distanceSqToWall = Utils.square((int)Math.round(toY / denomY));
	}
	else if (Math.abs(denomY) < 0.00001)
	{
		distanceSqToWall = Utils.square((int)Math.round(toX / denomX));
	}
	else distanceSqToWall = Utils.square((int)Math.round(Math.min(toX / denomX, toY / denomY)));

	if (distanceSqToWall < sensorRangeSquared)
	{
		entityHitType   = Sensors.WALL;
		distanceSquared = distanceSqToWall;
	}
	}

	// Store this sensor reading, possibly adding sensor noise. to do
	if (singleStepping && debugging)
	{
		Utils.println(player[i].toString() + ": for " + degrees + " degrees " 
				+ (entityHit != null 
				? entityHit.toString()
						: (entityHitType == Sensors.WALL ? "a wall " : " nothing"))
						+ "@" + Math.sqrt(distanceSquared));
	}

	if (sensingFromSelectedOne && entityHit != null) entityHit.setCanBeSeenBySelectedPlayer(true);

	sensors.setObjectType(dir, entityHitType);
	sensors.setDistance(  dir, Math.sqrt((double)distanceSquared));
	}

	player[i].setSensors(sensors);
	player[i].setReward(currentTime);
	}
	}

	void pause()
	{
		if (debugging) Utils.println("pausing");
		if (managerThread != null) managerThread.suspend();
		paused = true;
		redisplay(true); // Draw the score boards.
	}

	void resume()
	{
		if (debugging) Utils.println("resuming");
		if (managerThread != null) managerThread.resume();
		paused = false;
	}

	// Tell all the players they can process the updated sensors.
	void prepareForNextCycle(int currentTime)
	{
		if (singleStepping && debugging) Utils.println("Preparing for time = " + currentTime);
		synchronized (synchForPlayerCheckIn)
		{
			if (AgentWindow.reportSynchs) AgentWindow.developerLabel2.setText(Thread.currentThread().getName() + " in preparForNextCycle()");
			playersResumed = 0;
			for(int i = 0; i < playersSoFar; i++) if (player[i].exists(currentTime))
			{ 
				player[i].setReadyToMove(false);
				if (player[i].started()) player[i].resume(); else player[i].start();
				playersResumed++;
			}
			playersReadyToGo = 0;
			if (AgentWindow.reportSynchs) AgentWindow.developerLabel2.setText("");
		}
		if (singleStepping && debugging) Utils.println("Done preparing for the next cycle");
	}

	// Keep track of players saying they are done computing their next action.
	// When all have reported, resume the manager.
	void readyToGo(int playerID)
	{
		player[playerID].setReadyToMove(true);
		player[playerID].setColor(player[playerID].getOrigColor());
		if (singleStepping && debugging)
		{
			Utils.println(player[playerID] + " is ready to go [" 
					+ (playersReadyToGo + 1) + " of " + playersResumed + "]");
		}

		// Do this as late as possible, since it might reduce odds of the odd thread deaths occuring.
		synchronized (synchForPlayerCheckIn)
		{
			if (AgentWindow.reportSynchs) AgentWindow.developerLabel2.setText(Thread.currentThread().getName() + " in readyToGo(" + playerID + ")");
			playersReadyToGo++; 
			if (AgentWindow.reportSynchs) AgentWindow.developerLabel2.setText("");
		}
		// Should directly interrupt the Manager thread, but this doesn't
		// seem to be working in Java 1.0.2 - so the Manager periodically awakes and
		// checks if all the players are ready.
	}

	boolean allPlayersReadyToGo()
	{ int a, b;

	synchronized (synchForPlayerCheckIn)
	{
		if (AgentWindow.reportSynchs) AgentWindow.developerLabel2.setText(Thread.currentThread().getName() + " in allPlayersReadyToGo()");
		a = playersReadyToGo;
		b = playersResumed;
		if (AgentWindow.reportSynchs) AgentWindow.developerLabel2.setText("");
	}
	return (a >= b);
	}

	void reportPlayersNotReadyToGo(int currentTime)
	{
		for(int i = 0; i < playersSoFar; i++) if (player[i].exists(currentTime))
		{ 
			if (!player[i].getReadyToMove())
			{
				player[i].recordMissedMove();
				Utils.println(player[i].toString() + " is not ready to move"
						+ (player[i].getConsecutiveMissedMoves() > 1
								? "(#" + player[i].getConsecutiveMissedMoves() + " in a row)"
										: ""));
				player[i].setColor(Player.latePlayerColor);
			}
		}

	}

	// It is possible that all the players are ready to go before
	// the manager even attempts to sleep, so the manager uses this method
	// to decide to go to sleep.
	boolean shouldManagerSleep()
	{
		return (!allPlayersReadyToGo());
	}

	void setClockPeriod(int msecs)
	{
		clockPeriod = msecs; // Need tp save in case manager dies.
		if (managerThread != null) managerThread.setClockPeriod(msecs);
	}

	int getCurrentTime()
	{
		if (managerThread != null)
		{
			lastMeasuredTime = managerThread.currentTime;
			return lastMeasuredTime; // Need in case manager needs to be restarted.
		}
		else return 0;
	}

	public void mouseMoved(MouseEvent   event) {}
	public void mouseClicked(MouseEvent event) {}
	public void mouseEntered(MouseEvent event) {}
	public void mouseExited(MouseEvent  event) {}

	public void mousePressed(MouseEvent event)
	{ int x = event.getX(), y = event.getY();

	if (showHelp)
	{
		showHelp = false;
		resume();
		repaint();
	}

	if (started)
	{
		if (!isDisplayOn())
		{ // Clicking on the playing field when the display is disabled, enables it.
			redisplay(true);
		}
		else if (onlySeeViewOfSelectedPlayer)
		{ 
			selectedPlayer = getNearestPlayer(x, y, 5 * objectSize);
			if (selectedPlayer != null) selectedPlayer.describeSelectedPlayer();
			else agentWindow.reportInInfoBar("");
			if (paused) redisplay();
		}
		else if (togglingShowingSensors)
		{
			selectedPlayer = getNearestPlayer(x, y, 5 * objectSize);
			if (selectedPlayer != null)
			{
				selectedPlayer.describeSelectedPlayer();
				selectedPlayer.toggleShowingSensors();
				if (singleStepping) redisplay(true);
			}
			else agentWindow.reportInInfoBar("");
			if (paused) redisplay();
		}
		else
		{
			selectedPlayer = getNearestPlayer(x, y, 2 * objectSize);

			if (selectedPlayer != null) selectedPlayer.describeSelectedPlayer();
			else agentWindow.reportInInfoBar("");
			if (paused)
			{
				if (selectedPlayer != null) selectedPlayer.showScores = !selectedPlayer.showScores;
				if (paused) redisplay();
			}
		}
		mouseDownAt.x = x;
		mouseDownAt.y = y;
		mouseDown     = true;
	}
	else // Otherwise, grab an object for moving.
	{
		entityGrabbed = getEntityAtThisLocation(x, y);
		if (entityGrabbed != null)
		{
			entityGrabbed.setGrabbed(true);
			redisplay(true);
		}
	}
	}

	public void mouseDragged(MouseEvent event)
	{ int x = event.getX(), y = event.getY();

	if (started)
	{
		mouseDownAt.x = x;
		mouseDownAt.y = y;
		mouseDown     = true;
	}
	else if (entityGrabbed != null) // Move the grabbed object (if any).
	{
		entityGrabbed.directlyMoveTo(x, y);
		redisplay(true);
	}
	}

	public void mouseReleased(MouseEvent event)
	{ int x = event.getX(), y = event.getY();

	if (started)
	{
		mouseUpAt.x = x;
		mouseUpAt.y = y;
		mouseDown   = false;
	}
	else if (entityGrabbed != null) // Release the grabbed object (if any).
	{ int layoutTrials = 0;

	x = Math.max(outerBoxInner.x, Math.min(x, outerBoxInner.lastX));
	y = Math.max(outerBoxInner.y, Math.min(y, outerBoxInner.lastY));

	entityGrabbed.directlyMoveTo(x, y);
	while (intersectingEntity(entityGrabbed) != null && layoutTrials < 1000)
	{ int newX, newY;

	// Find some nearby free space by doing a random walk of ever-increasing step sizes.
	layoutTrials++;
	// Need to keep on board. Also, always walk from initial location.
	do
	{
		newX = x + Utils.getRandomIntInRange(-layoutTrials, layoutTrials);
		newY = y + Utils.getRandomIntInRange(-layoutTrials, layoutTrials);
	}
	while (!outerBoxInner.contains(newX, newY));
	entityGrabbed.directlyMoveTo(newX, newY);
	}
	entityGrabbed.setGrabbed(false);
	entityGrabbed = null;
	redisplay(true);
	}
	}

	Position getLastMouseDownPosition()
	{
		return mouseDownAt;
	}

	Position getLastMouseUpPosition()
	{
		return mouseUpAt;
	}

	boolean isMouseDown()
	{
		return mouseDown;
	}

	boolean pointInsideEntity(int x, int y, Entity e)
	{ Position pos = e.getPosition();

	return Utils.square(pos.x - x) + Utils.square(pos.y - y) <= objectSize;
	}

	// Return first entity that intersects this location.
	Entity getEntityAtThisLocation(int x, int y)
	{ 

		for(int i = 0; i < playersSoFar;  i++)   if (player[i].exists(0))
		{ 
			if (pointInsideEntity(x, y, player[i]))    return player[i];
		}

		for(int i = 0; i < mineralsSoFar; i++)   if (mineral[i].exists(0))
		{ 
			if (pointInsideEntity(x, y, mineral[i]))   return mineral[i];
		}

		for(int i = 0; i < vegetablesSoFar; i++) if (vegetable[i].exists(0))
		{ 
			if (pointInsideEntity(x, y, vegetable[i])) return vegetable[i];
		}

		return null; // Nothing here.
	}

	// Compute this in a static so that users can get this info (eg, to normalize distances).
	static int getSensorRange()
	{ int sensorRangeSquared
		= Utils.square(Math.max(2, Math.min((int)(360 / (Math.PI * Sensors.DEGREES_BETWEEN_SENSORS)), 15))
				* PlayingField.objectSize);

	return (int)Math.sqrt((double)sensorRangeSquared);
	}

	Player getNearestPlayer(int x, int y, int threshold)
	{ int    temp, minDistanceSquared = Utils.square(threshold);
	Player winner = null;

	for(int i = 0; i < playersSoFar;  i++) if (player[i].exists(getCurrentTime()))
	{ Position p = player[i].getPosition();

	temp = Utils.square(p.x - x) + Utils.square(p.y - y);
	if (temp < minDistanceSquared)
	{
		winner = player[i];
		minDistanceSquared = temp;
	}
	}

	return winner;
	}

	private void createManagerThread()
	{
		managerThread = new ManagerThread(this);
		managerThread.setPriority(Thread.NORM_PRIORITY - 2); // Let the AWT thread get in there easily.
		managerWatcherThread = new WatcherThread(this, managerThread);
		managerThread.setPriority(Thread.MIN_PRIORITY); // Watches to see if the manager has hung.
	}

	void restartManager()
	{ int managerThreadsCurrentTime = managerThread.currentTime;

	Utils.println("Creating a new manager thread (current time = "
			+ managerThreadsCurrentTime + ")");
	if (managerThread.isAlive())
	{
		Utils.println("Killing the old manager thread.");
		managerThread.stop();
	}
	createManagerThread();
	managerThread.setReportSlowPlayers(reportSlowPlayers);
	managerThread.setClockPeriod(clockPeriod);
	managerThread.currentTime = Math.max(managerThreadsCurrentTime, lastMeasuredTime);
	managerThread.debugging = true;
	debugging = true;
	managerThread.start();
	}

	void start()
	{ 
		if (debugging) Utils.println("Starting the simulator.");
		try
		{
			if (started) Utils.errorMsg("Already started the agent world");
			if (!configured) configure(false);
			started = true;
			if (debugging) Utils.println("Doing the initial sensor reads");
			updateSensorsAndRewards();
			if (debugging) Utils.println("Starting the manager thread");
			managerThread.start();
			managerWatcherThread.start();
		}
		catch(Exception e)
		{
			Utils.errorMsg(e + ", " + toString() + " has stopped running");
			e.printStackTrace(System.err);
		}
	}

}

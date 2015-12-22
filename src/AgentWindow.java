//         allow a testSuite ...


import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

class AgentWindow extends Frame implements ActionListener, ItemListener
{         static final boolean masterDebugging    = true,
                               reportSynchs       = false; // Report monitor state in the score board?
  private static final boolean debuggingThisClass = false;

  private Color        foregndColor, backgndColor;
  private Button       configure, start, quit, singleStep, help;
          Button       pause, display;
          PlayingField playingField;
  private boolean      singleStepping = false, started = false, debugging;
          Panel        topHolder, top, bottom, buttonBar;
          Label        clock;
  private Label        infoBar;
  static  Label        developerLabel1, developerLabel2; // Report debugging messages.

  static final Font tinyFont        = new Font("TimesRoman", Font.PLAIN, 11),
                    tinyBoldFont    = new Font("TimesRoman", Font.BOLD,  11),
                    smallFont       = new Font("TimesRoman", Font.PLAIN, 12),
                    smallBoldFont   = new Font("TimesRoman", Font.BOLD,  12),
                    buttonFont      = new Font("TimesRoman", Font.PLAIN, 12),
                    helpFont        = new Font("TimesRoman", Font.BOLD,  13),
                    regularFont     = new Font("TimesRoman", Font.PLAIN, 14),
                    regularBoldFont = new Font("TimesRoman", Font.BOLD,  14),
                    bigFont         = new Font("TimesRoman", Font.PLAIN, 16),
                    bigBoldFont     = new Font("TimesRoman", Font.BOLD,  16),
                    largeFont       = new Font("TimesRoman", Font.PLAIN, 20),
                    largeBoldFont   = new Font("TimesRoman", Font.BOLD,  20);

  private CheckboxMenuItem    reportSlowPlayersCheckBox;
  private static final String reportSlowPlayersString   = "Report which players take too long to make a move.";
  private boolean             reportSlowPlayersValue    = false;
  
  private CheckboxMenuItem    onlySeeViewOfPlayerCheckBox;
  private static final String onlySeeViewOfPlayerString = "Only see view seen by the selected player.";
  private boolean             onlySeeViewOfPlayerValue  = false;

  private CheckboxMenuItem    toggleShowingSensorsOfPlayerCheckBox;
  private static final String toggleShowingSensorsOfPlayerString = "Toggle showing sensors of selected player.";
  private boolean             toggleShowingSensorsOfPlayerValue  = false;

  private CheckboxMenuItem    circleAllObjectsCheckBox;
  private static final String circleAllObjectsString    = "Show true shape of all objects.";
  private boolean             circleAllObjectsValue     = false;
  
  private              Menu   helpMenu;
  private static final String helpString      = "Help";

          static final String configureLabel  = "  Configure  ";
  private static final String singleStepLabel = " Stop After Each Step Instead ";
          static final String continuousLabel = "    Run Continuously Instead  ";
          static final String startLabel      = " Start ";
  private static final String pauseLabel      = "  Pause ";
  private static final String resumeLabel     = " Resume ";
          static final String goLabel         = "   GO   ";
          static final String displayOnLabel  = " Turn Graphics OFF ";
          static final String displayOffLabel = "  Turn Graphics ON  ";
  private static final String quitLabel       = " Quit ";
  private static final String helpLabel       = " Help ";

  // The constructors.
  AgentWindow()
  {
    this(true);
  }
  AgentWindow(boolean displayOn)
  {
    this(700, 350, displayOn);
  }
  // Specify the width and height of PLAYING FIELD (want this to be constant
  // independent of the number of player scores reported, etc).
  AgentWindow(int width, int height, boolean displayOn)
  {
    super("The Agent World");
    setLayout(new BorderLayout(5, 0));

    validate();
    addNotify();
    setBounds(5, 5, 100, 100);

    foregndColor = Color.darkGray;
    backgndColor = Color.lightGray;
    setForeground(foregndColor);
    setBackground(backgndColor);
    setFont(regularFont);

    Menu menu = new Menu("Options");
    menu.addActionListener(this);
    menu.setFont(smallFont);

    reportSlowPlayersCheckBox = new CheckboxMenuItem(reportSlowPlayersString);
    reportSlowPlayersCheckBox.setState(reportSlowPlayersValue);
    reportSlowPlayersCheckBox.setFont(smallFont);
    reportSlowPlayersCheckBox.addItemListener(this);
    menu.add(reportSlowPlayersCheckBox);

    onlySeeViewOfPlayerCheckBox  = new CheckboxMenuItem(onlySeeViewOfPlayerString);
    onlySeeViewOfPlayerCheckBox.setState(onlySeeViewOfPlayerValue);
    onlySeeViewOfPlayerCheckBox.setFont(smallFont);
    onlySeeViewOfPlayerCheckBox.addItemListener(this);
    menu.add(onlySeeViewOfPlayerCheckBox);

    toggleShowingSensorsOfPlayerCheckBox  = new CheckboxMenuItem(toggleShowingSensorsOfPlayerString);
    toggleShowingSensorsOfPlayerCheckBox.setState(toggleShowingSensorsOfPlayerValue);
    toggleShowingSensorsOfPlayerCheckBox.setFont(smallFont);
    toggleShowingSensorsOfPlayerCheckBox.addItemListener(this);
    menu.add(toggleShowingSensorsOfPlayerCheckBox);

    circleAllObjectsCheckBox  = new CheckboxMenuItem(circleAllObjectsString);
    circleAllObjectsCheckBox.setState(circleAllObjectsValue);
    circleAllObjectsCheckBox.addItemListener(this);
    circleAllObjectsCheckBox.setFont(smallFont);
    menu.add(circleAllObjectsCheckBox);

    helpMenu = new Menu("Help");
    helpMenu.addActionListener(this);
    helpMenu.setFont(smallFont);
    helpMenu.add(helpString);
    
    MenuBar mainMenu = new MenuBar();
    mainMenu.add(menu);
    mainMenu.add(helpMenu);
    mainMenu.setHelpMenu(helpMenu);
    setMenuBar(mainMenu);

    topHolder     = new Panel();
    top           = new Panel();
    clock         = new Label(" Waiting to begin ...");
    Label left    = new Label(" ");
    playingField  = new PlayingField(this, width, height);
    Label right   = new Label(" ");
    bottom        = new Panel();
    buttonBar     = new Panel();
    infoBar       = new Label(" ", Label.CENTER);
    configure     = new Button(configureLabel);
    start         = new Button(startLabel);
    singleStep    = new Button(singleStepLabel);
    pause         = new Button(pauseLabel);
    display       = new Button(displayOffLabel);
    quit          = new Button(quitLabel);
    help          = new Button(helpLabel);
    
    configure.addActionListener(this);
    start.addActionListener(this);
    singleStep.addActionListener(this);
    pause.addActionListener(this);
    display.addActionListener(this);
    quit.addActionListener(this);
    help.addActionListener(this);

    top.setBackground(PlayingField.FieldColor);

    topHolder.setLayout(new BorderLayout(0, 0)); 
    top.setLayout(      new GridLayout(  0, 4)); // Report players' scores four-across.
    bottom.setLayout(   new BorderLayout(0, 5)); // Space between infoBar and buttonBar.
    buttonBar.setLayout(new FlowLayout(FlowLayout.CENTER));

    clock.setFont(smallFont);
    infoBar.setFont(smallFont);
    configure.setFont(buttonFont);
    start.setFont(buttonFont);
    singleStep.setFont(buttonFont);
    pause.setFont(buttonFont);
    display.setFont(buttonFont);
    quit.setFont(buttonFont);
    help.setFont(buttonFont);

    //Label topSpacerN = new Label("");
    //Label topSpacerE = new Label("");
    //Label topSpacerW = new Label("");
    Label topSpacerS = new Label("");
    //topHolder.add("North",  topSpacerN);
    //topHolder.add("East",   topSpacerE);
    //topHolder.add("West",   topSpacerW);
    topHolder.add("South",  topSpacerS);
    topHolder.add("Center", top);

    bottom.add("North",  infoBar);
    bottom.add("Center", buttonBar);

    top.add(clock); // Put in upper left.
    if (reportSynchs)  // Have some labels for reporting internal state (for debugging purposes).
    {
      developerLabel1 = new Label("");
      developerLabel2 = new Label("");
      developerLabel1.setFont(tinyFont);
      developerLabel2.setFont(tinyFont);
      top.add(developerLabel1);
      top.add(developerLabel2);
    }

    Label spacerA = new Label("");
    Label spacerB = new Label("");
    Label spacerC = new Label("");
    buttonBar.add(configure);
    buttonBar.add(start);
    buttonBar.add(spacerA);
    buttonBar.add(singleStep);
    buttonBar.add(pause);
    buttonBar.add(spacerB);
    buttonBar.add(display);
    buttonBar.add(spacerC);
    buttonBar.add(quit);
    buttonBar.add(help);

    pause.setEnabled(false); // Wait until started.

    add("North",  topHolder);
    add("West",   left);
    add("Center", playingField);
    add("East",   right);
    add("South",  bottom);

    setVisible(true);
    pack();
    setResizable(true);
    
    if (displayOn) setDisplayON(); else setDisplayOFF();

    // Make sure initial settings here match those in the other classes.
    setCircleAllObjects(circleAllObjectsValue);
    setReportSlowPlayers(reportSlowPlayersValue);
    setOnlySeeViewOfSelectedPlayer(onlySeeViewOfPlayerValue, false);
    setToggleShowingSensorsOfSelectedPlayer(toggleShowingSensorsOfPlayerValue, false);

    debugging = (debuggingThisClass && AgentWindow.masterDebugging);

    System.out.println("This is the console of the Agent Window.");
    System.out.println("(Do a control-C here if the program appears to be hanging.)");
    System.out.println("");
  }
 
  // These tests are probably redundant.
  public void setSize(Dimension d)
  { 
    if (isResizable()) super.setSize(d);
  }
  public void setSize(int width, int height)
  {
    if (isResizable()) super.setSize(width, height);
  }
  public void setBounds(int x, int y, int width, int height)
  {
    if (isResizable()) super.setBounds(x, y, width , height);
  }

  public void addPlayer(Player newPlayer, String name, Color color)
  { Score score = new Score(name, color, 0, playingField);
  
    playingField.addPlayer(newPlayer, name, color, score);

    top.add(score.getComponent());
    pack();
  }
  public void addPlayer(Player newPlayer, String name)
  { 
    addPlayer(newPlayer, name, Player.PlayerColor);
  }

  public void addRandomWalker(String name, Color color, boolean showSensors)
  { 
    if (showSensors) Utils.errorMsg("RandomWalker's do not use sensors, so they are not computed");
    addPlayer(new RandomWalker(this), name, color);
  }
  public void addRandomWalker(String name, Color color)
  { 
    addRandomWalker(name, color, false);
  }
  public void addRandomWalker(String name, boolean showSensors)
  { 
    addRandomWalker(name, RandomWalker.RandomWalkerColor, showSensors);
  }
  public void addRandomWalker(String name)
  { 
    addRandomWalker(name, RandomWalker.RandomWalkerColor);
  }
  public void addAnonymousRandomWalkers(int number, Color color)
  { 
    for(int i = 0; i < number; i++)
    { // Don't assign a Score component to these.
      playingField.addPlayer(new RandomWalker(this), "an anonymous Random Walker", color, null);
    }
  }
  public void addAnonymousRandomWalkers(int number)
  { 
    addAnonymousRandomWalkers(number, RandomWalker.RandomWalkerColor);
  }

  public void addSmartPlayer(String name, Color color, boolean showSensors)
  { 
    addPlayer(new VaibhavPlayer(this, showSensors), name, color);
  }
  public void addSmartPlayer(String name, Color color)
  { 
    addSmartPlayer(name, color, false);
  }
  public void addSmartPlayer(String name, boolean showSensors)
  { 
    addSmartPlayer(name, VaibhavPlayer.myColor, showSensors);
  }
  public void addSmartPlayer(String name)
  { 
    addSmartPlayer(name, VaibhavPlayer.myColor);
  }
  public void addAnonymousSmartPlayers(int number, Color color)
  { 
    for(int i = 0; i < number; i++)
    { // Don't assign a Score component to these.
      playingField.addPlayer(new VaibhavPlayer(this), "an anonymous Smart Player", color, null);
    }
  }
  public void addAnonymousSmartPlayers(int number)
  { 
    addAnonymousSmartPlayers(number, VaibhavPlayer.myColor);
  }
  
  public void addAssassin(String name, Color color, boolean showSensors)
  { 
    addPlayer(new Assassin(this, showSensors), name, color);
  }
  public void addAssassin(String name, Color color)
  { 
    addAssassin(name, color, false);
  }
  public void addAssassin(String name, boolean showSensors)
  { 
    addAssassin(name, Assassin.AssassinColor, showSensors);
  }
  public void addAssassin(String name)
  { 
    addAssassin(name, Assassin.AssassinColor);
  }
  public void addAnonymousAssassins(int number, Color color)
  { 
    for(int i = 0; i < number; i++)
    { // Don't assign a Score component to these.
      playingField.addPlayer(new Assassin(this), "an anonymous Assassin", color, null);
    }
  }
  public void addAnonymousAssassins(int number)
  { 
    addAnonymousAssassins(number, Assassin.AssassinColor);
  }

  public void addFollowMousePlayer(String name, Color color, boolean showSensors)
  { 
    if (showSensors) Utils.errorMsg("MouseFollower's do not use sensors, so they are not computed");
    addPlayer(new FollowMouse(this), name, color);
  }
  public void addFollowMousePlayer(String name, Color color)
  { 
    addFollowMousePlayer(name, color, false);
  }
  public void addFollowMousePlayer(String name, boolean showSensors)
  { 
    addFollowMousePlayer(name, FollowMouse.FollowMouseColor, showSensors);
  }
  public void addFollowMousePlayer(String name)
  { 
    addFollowMousePlayer(name, FollowMouse.FollowMouseColor);
  }

  public void addMinerals(int number)
  { 
    for(int i = 0; i < number; i++)
    {
      playingField.addMineral();
    }
  }

  public void addVegetables(int number)
  { 
    for(int i = 0; i < number; i++)
    {
      playingField.addVegetable();
    }
  }

  public void playThisManyGamesEachOfThisManyTimeSteps(int games, int duration)
  {
    playingField.gamesToPlay(games);
    playingField.gameDuration(duration);
    startUp();
  }

  // Allow users to automatically start.
  public void startUp()
  {
    started = true;
    pause.setEnabled(true);
    start.setEnabled(false);     // Don't allow multiple starts.
    configure.setEnabled(false); // Don't allow reconfigurations.
    setResizable(false); // Don't allow resizing once the game has begun.
    playingField.start();
  }

  public void setClockPeriod(int msecs)
  {
    playingField.setClockPeriod(msecs);
  }

  public void reportInInfoBar(String message)
  {
    infoBar.setText(message);
  }

  private void provideHelp()
  {
    playingField.showHelp();
  }

  public void setSingleStepMode(boolean value)
  {
    if (!started)
    {
      if (value) clock.setText(" Press " + goLabel.trim() + " to start ...");
    }
    if (singleStepping != value)
    {
      if (value) singleSteppingON(); else singleSteppingOFF();
    }
  }

  private void singleSteppingON()
  {
    setDisplayON();
    singleStepping = true;
    singleStep.setLabel(continuousLabel);
    pause.setLabel(goLabel);
    if (playingField.paused) pause.setEnabled(true); // Need to resume/go.
    else pause.setEnabled(false);
    playingField.useSingleStepMode(true);
    reportInInfoBar("Single-stepping has been turned ON.  Press "
                    + goLabel.trim() + " to continue.");
  }

  private void singleSteppingOFF()
  {
    singleStepping = false;
    singleStep.setLabel(singleStepLabel);
    if (playingField.paused) pause.setLabel(resumeLabel);
    else pause.setLabel(pauseLabel);
    playingField.useSingleStepMode(false);
    reportInInfoBar("Single-stepping has been turned OFF.");
  }
  
  void setDisplayON()
  {
    display.setLabel(displayOnLabel);
    playingField.setDisplayOn(true);
    playingField.repaint();
    reportInInfoBar("");
  }

  void setDisplayOFF()
  {
    display.setLabel(displayOffLabel);
    playingField.setDisplayOn(false);
    playingField.repaint();
    reportInInfoBar("Clicking in the playing field will also resume the graphics.");
  }
  
  // Function called when checkbox is modified on the options menu.
  public void itemStateChanged(ItemEvent event)
  { Object target = event.getItem();
    String label  = target.toString();

    if (label.equals(circleAllObjectsString))
    {
      setCircleAllObjects(!circleAllObjectsValue);
    }
    else if (label.equals(reportSlowPlayersString))
    {
      setReportSlowPlayers(!reportSlowPlayersValue);
    }
    else if (label.equals(onlySeeViewOfPlayerString))
    { // Only allow one "selected player" option to be set.
      setToggleShowingSensorsOfSelectedPlayer(false, false);
      // If explicitly turning off, then deselect the selected player.
      setOnlySeeViewOfSelectedPlayer(!onlySeeViewOfPlayerValue, onlySeeViewOfPlayerValue);
    }
    else if (label.equals(toggleShowingSensorsOfPlayerString))
    { // Only allow one "selected player" option to be set.
      setOnlySeeViewOfSelectedPlayer(false, false);
      setToggleShowingSensorsOfSelectedPlayer(!toggleShowingSensorsOfPlayerValue,
                                              toggleShowingSensorsOfPlayerValue);
    }
  }

  public void actionPerformed(ActionEvent event)
  { Object target = event.getSource();
    //String label  = event.getActionCommand();

    if      (target == quit)
    {
      System.exit(1); // to do: should confirm
    }
    else if (target == configure)
    {
      setResizable(false);
      configure.setLabel("Reinitialize");
      playingField.configure(true);
    }
    else if (target == pause)
    { String label = pause.getLabel();

      if (label.equals(pauseLabel))
      {
        playingField.pause();
        pause.setLabel(resumeLabel);
      }
      else if (label.equals(resumeLabel))
      {
        pause.setLabel(pauseLabel);
        playingField.resume();
      }
      else
      { // This button is used during single-stepping as well.
        pause.setEnabled(false);
        playingField.resume();
      }
    }
    else if (target == singleStep)
    { String label = singleStep.getLabel();

      if (label.equals(singleStepLabel)) singleSteppingON();
      else                               singleSteppingOFF();
    }
    else if (target == display)
    { String label = display.getLabel();

      if (label.equals(displayOnLabel)) setDisplayOFF();
      else                              setDisplayON();
    }
    else if (target == start)
    {
      reportInInfoBar("");
      startUp();
    }
    else if (target == help)
    {      
      if (debugging) playingField.restartManager();
      else provideHelp();
    }
    else if (target == circleAllObjectsCheckBox)
    {
      setCircleAllObjects(!circleAllObjectsValue);
    }
    else if (target instanceof MenuItem)
    { String label = ((MenuItem)target).getLabel(); 

      if (label.equals(helpString))
      {
        provideHelp();
      }
    }
  }

  void setCircleAllObjects(boolean value)
  {
    circleAllObjectsCheckBox.setState(value);
    circleAllObjectsValue = value;
    playingField.setCircleAllObjects(value);
  }

  void setReportSlowPlayers(boolean value)
  {
    reportSlowPlayersCheckBox.setState(value);
    reportSlowPlayersValue = value;
    playingField.setReportSlowPlayers(value);
  }

  void setOnlySeeViewOfSelectedPlayer(boolean value, boolean deselectSelectedPlayer)
  {
    onlySeeViewOfPlayerCheckBox.setState(value);
    onlySeeViewOfPlayerValue = value;
    playingField.setOnlySeeViewOfSelectedPlayer(value, deselectSelectedPlayer);
  }

  void setToggleShowingSensorsOfSelectedPlayer(boolean value, boolean deselectSelectedPlayer)
  {
    toggleShowingSensorsOfPlayerCheckBox.setState(value);
    toggleShowingSensorsOfPlayerValue = value;
    playingField.setToggleShowingSensorsOfSelectedPlayer(value, deselectSelectedPlayer);
  }
}

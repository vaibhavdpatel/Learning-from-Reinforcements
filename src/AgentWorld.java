import java.awt.Color;
import java.io.File;

// The 'Agent World' for CS 540 and CS 760.
//
//   - copyrighted 1997, 1998 by Jude Shavlik
//     (for educational use only)
//

// YOU SHOULD MAKE YOUR OWN COPY OF THIS FILE AND EDIT IT TO USE
// YOUR OWN PLAYER (AS WELL AS YOUR OWN CHOICE OF OTHER PLAYERS).

// Be sure (in Unix) to do the following in order to have Java find your 
// and my class files:
//
//   setenv CLASSPATH .:./classes:/p/course/cs540-shavlik/public/AgentWorld/classes
//
// On other platforms, you'll have to copy those files over to your machine and
// make sure these files are in a directory that Java searchs.
// You can access these class files via 
//
//     http://www.cs.wisc.edu/~shavlik/cs540/AgentWorld/classes
//
// (this is a WWW link to that /p/course/cs540-shavlik/... directory listed above).

public class AgentWorld 
{ static String neuralNetworkFileName = null; // See comments about this below.

  public static void main(String args[])
  { boolean barrenWorld = true; // For debugging purposes, it can be helpful to
                                 // set this true and only have your own player
                                 // out there.

    try 
    { // Need an AgentWindow instance with which to communicate.
      // This argument indicates whether the game is being displayed.
      // (you might wish to turn this off in the machine learning HW
      // to conserve cpu cycles for training).
      AgentWindow aw = new AgentWindow(true);
      
      aw.addPlayer(new VaibhavPlayer(aw),
              "a Smart Player",  Color.yellow);

      // Create some 'baby' players that simply do random walks.
      // Notice you can specify the colors for players,
      // which will otherwise default depending on the player type.
      if (!barrenWorld) aw.addRandomWalker("Alice", Color.pink);
      if (!barrenWorld) aw.addRandomWalker("Bob",   Color.cyan);

      // Create a malicious player that chases other players.
      if (!barrenWorld) aw.addAssassin("The Jackal");
 
      // This (curently commented-out) player will follow the mouse whenever 
      // the mouse button is down.
      // (You may wish to only display what this player sees, and then see
      // how high you can score by guiding it from its perspective.
      // The ability to maneuver a player is also useful for debugging.)
      //aw.addFollowMousePlayer("The Pied Piper"); 

      // Here's how you'll hook up your player.
      // Name your class <LOGIN>Player, where <LOGIN> is replaced with
      // you login on the course computers - this will allow us
      // to mix-and-match players w/o having to worry about name conflicts,
      // e.g., I'd call mine ShavlikPlayer and create the file ShavlikPlayer.java.
      //
      // Below is a sample player that shows how one's code interacts with
      // the Agent World.  You can view this file at
      //
      //    /p/course/cs760-shavlik/public/AgentWorld/Java/SamplePlayer.java
      //
      //        or
      //    http://cs.wisc.edu/~shavlik/cs540/AgentWorld/Java/SamplePlayer.java
      //
      //
      // to see how the sensors are read, rewards received, etc.  You should
      // also look at Utils.java in that same directory; it contains some
      // possibly useful utility files.
      //
      // Also, start with the sensors being displayed (hence, the 'true').
      if (!barrenWorld) aw.addPlayer(new SamplePlayer(aw, true),
                                     "a Sample Player", Color.blue);

      // The following is a semi-clever, hand-coded player.
      if (!barrenWorld) aw.addPlayer(new VaibhavPlayer(aw),
                                     "a Smart Player",  Color.yellow);

      
      // The following player has been trained, "from scratch,"
      // using artificial neural networks (ANNs) and a machine learning method
      // called "reinforcement learning."
      if (!barrenWorld)
      { neuralNetworkFileName = "savedShavlikNetwork.jws";

        // The above file MUST EXIST IN THE DIRECTORY FROM WHICH YOU INVOKE JAVA
        // OR OTHERWISE THIS PLAYER WILL NOT BE CREATED.
        if (neuralNetworkFileName != null &&
            (new File(neuralNetworkFileName)).exists()) 
        {
          //aw.addPlayer(new ShavlikANNplayer(aw),
          //             "An ML-Trained Player", Color.orange);
        }
      }

      // Add some players whose scores aren't reported on the score board.
      if (!barrenWorld) aw.addAnonymousAssassins(2);
      if (!barrenWorld) aw.addAnonymousRandomWalkers(1);
      if (!barrenWorld) aw.addAnonymousSmartPlayers(1);
      //Actually, the AnonymousSmartPlayers are more copies of ShavlikPlayer.
 
      // Can't have more than 100 of either of the following,
      // nor more than 25 players.
      if (!barrenWorld) aw.addMinerals(  100);
      if (!barrenWorld) aw.addVegetables(100);

      // At some point we may try to have a competition among the 
      // players produced by various students.  Since the
      // training and 'testing' environments, should be approximately
      // equivalent, we'll define the basic initial configuration to have:
      //
      //         10 Players (with exactly 3 Assassin's & 0 FollowMouse's)
      //        100 Minerals
      //        100 Vegetables
      //
      // Of course the intelligence of the players will vary from the above
      // when competing against players of various students.
      // WE'LL POST MORE INFO ABOUT COMBINING PLAYERS OF MULTIPLE STUDENTS
      // ON THE CLASS HOME PAGE (http://www.cs.wisc.edu/~shavlik/cs540.html).

      // The "game manager" sleeps at most this long (in msec) for all the 
      // players to choose their moves.  Players that aren't ready continue
      // in the same direction as their last choice. You may wish to adjust
      // this depending on the speed of your code/machine and the number of
      // players.  (If all players are ready, the simulator proceeds without
      // waiting for the duration to expire, so it isn't too harmful to set
      // this high.  However, the player threads seem to occassionally die/hang
      // for no [obvious] reason; if a player times-out too many times in a row,
      // its thread will automatically be recreated - a low time-out
      // causes this detection and restarting to be accomplished faster.)
      aw.setClockPeriod(150);

      // You can request that the 'manager' wait for you to 
      // press GO before each move.
      aw.setSingleStepMode(false); // The default setting is also false.

      // The following will play the specified number of games,
      // where each game last for the stated number of steps.
      // Each game starts in a new randomized initial configuration.
      // The mean score is reported for each player at the end.
      // We'll say that games always end after 5000 units of time,
      // since the optimal behavior depends on game length.
      // (This command also automatically pushes the Start button.)
      if (!barrenWorld) aw.playThisManyGamesEachOfThisManyTimeSteps(1000, 5000);

      // If the above line is commented out, then you can create
      // the initial configuration before pushing the Start button.
    }
    
    catch(Exception e)
    {
      Utils.println("Exception encountered in main method: " + e);
      Utils.println("The following indicates the location of the problem:");
      e.printStackTrace(System.err);
      Utils.exit(-1);
    }
  }

}

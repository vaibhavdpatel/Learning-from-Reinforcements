//
//
// The Agent World for CS 760.
//
//   - copyrighted by Jude Shavlik
//     (for educational use only)
//

// YOU SHOULD MAKE YOUR OWN COPY OF THIS FILE AND EDIT IT TO USE
// YOUR OWN PLAYER (AS WELL AS YOUR OWN CHOICE OF OTHER PLAYERS).

// Note: the AgentWorld simulator had formerly been a Java "package,"
// but for some reason that is no longer working.  So simply
// keep all the AgentWorld class files in the same directory as your
// own code.  (Feel free to play around with the CLASSPATH environment
// variable if you wish to organize your files better, 
// but that isn't necessary.) 


import java.awt.Color;

public class RunAgentWorld 
{
	public static void main(String args[])
	{ 
		boolean barrenWorld = false; // Set this to 'true' to train a single player 
		// to learn to avoid the walls. (Good way to start HW4.)

		try 
		{ 
			// Need an AgentWindow instance with which to communicate.
			// This argument indicates whether the game is being displayed.
			// (you might wish to turn this off to conservce cpu cycles for training).
			AgentWindow aw = new AgentWindow(true);

			//TODO
			if(barrenWorld)
				aw.addSmartPlayer("Vaibhav Player",  Color.yellow, true);
			// Create some players that simply do random walks.
			// Notice you can specify the colors for players,
			// which will otherwise default depending on the player type.

			//TODO
			//if (!barrenWorld) aw.addRandomWalker("Alice", Color.pink);
			//TODO
			//if (!barrenWorld) aw.addRandomWalker("Bob",   Color.cyan);

			// Create a malicious player that chases other players.
			//TODO
			      if (!barrenWorld) aw.addAssassin("The Jackal");

			// Start with the sensors being displayed for the next player,
			// which has a hand-coded 'brain' of reasonable cleverness.
			if (!barrenWorld) aw.addSmartPlayer("Vaibhav Player",  Color.yellow, true);

			// This player will follow the mouse whenever the mouse button is down.
			// (You may wish to only display what this player sees, and then see
			// how high you can score do guiding it from its perspective.
			// The ability to maneuver a player is also useful for debugging.)
			//aw.addFollowMousePlayer("The Pied Piper"); 

			// Here's how you'll hook up your player.
			// Name your class <LOGIN>Player, where <LOGIN> is replaced with
			// you login on the course computers - this will allow us
			// to mix-and-match players w/o having to worry about name conflicts,
			// eg I'd call mine ShavlikPlayer and create the file ShavlikPlayer.java.
			// You should look at 
			//    /p/course/cs760-shavlik/public/AgentWorld/Java/SamplePlayer.java
			// to see how the sensors are read, rewards received, etc.  You should
			// also look at Utils.java in that same directory.
			// aw.addPlayer(new SamplePlayer(aw), "a Sample Player", Color.blue);

			// Add some players whose scores aren't reported on the score board.
			//TODO
			if (!barrenWorld) aw.addAnonymousAssassins(1);
			//TODO
			if (!barrenWorld) aw.addAnonymousSmartPlayers(1);
			if (!barrenWorld) aw.addAnonymousRandomWalkers(1);


			//TODO
			if (!barrenWorld) aw.addMinerals(   20); // Can't have more than 100 of either of these,
			if (!barrenWorld) aw.addVegetables(200); // nor more than 25 players.

			// At some point we may try to have a competition among the 
			// players trained (via RL) by various students.  Since the
			// training and 'testing' environments, should be approximately
			// equivalent, we'll define the basic initial configuration to have:
			//
			//          7 Players (with exactly 3 Assassin's & 0 FollowMouse's)
			//         50 Minerals
			//        100 Vegetables
			//
			// Of course the intelligence of the players will vary.

			// The "game manager" sleeps at most this long (in msec) for all the 
			// players to choose their moves.  Players that aren't ready continue
			// in the same direction as their last choice. You may wish to adjust
			// this depending on the speed of your code/machine and the number of
			// players.  (If all players are ready, the simulator proceeds without
			// waiting for the duration to expire, so it isn't too harmful to set
			// this high.  However, the player threads seem to die/hang for no
			// [obvious] reason; if a player times-out too many times in a row,
			// its thread will automatically be recreated, and a low time-out
			// causes this detection and restarting to be accomplished faster.)
			aw.setClockPeriod(150);

			// You can request that the 'manager' wait for you to 
			// press GO before each move.
			aw.setSingleStepMode(true); // The default setting is also false.

			// The following will play the specified number of games,
			// where each game last for the stated number of steps.
			// Each game starts in a new randomized initial configuration.
			// The mean score is reported for each player at the end.
			// We'll say that games always end after 5000 units of time,
			// since the optimal behavior to learn depends on game length.
			// (This command also automatically pushes the Start button.)
			if (!barrenWorld) aw.playThisManyGamesEachOfThisManyTimeSteps(100, 5000);

			// If the above line is commented out, then you can choose
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

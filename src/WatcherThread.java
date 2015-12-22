public class WatcherThread extends Thread
{ private PlayingField playingField;
  private ManagerThread managerThread;
  private long lastManagerStepCount;
  private int sleepPeriod = 5 * 60 * 1000; // Every 5 mins check if the managerThread has progressed.

  WatcherThread(PlayingField playingField, ManagerThread managerThread)
  {
    this.playingField  = playingField;
    this.managerThread = managerThread;
  }

  public void run()
  {
    while (isAlive() && (playingField.gamesToPlay < 0 || playingField.gamesPlayed < playingField.gamesToPlay))
    {
      lastManagerStepCount = managerThread.totalTurnsTaken;
      try
      {
        sleep(sleepPeriod);
      }
      catch(InterruptedException e) { } // Just continue if interrupted.
      
      // See if manager did anything while this thread was sleeping
      // (be sure to check if there still are games to be played).
      if (lastManagerStepCount >= managerThread.totalTurnsTaken &&
	  !playingField.paused && 
          (playingField.gamesToPlay < 0 || playingField.gamesPlayed < playingField.gamesToPlay))
      {
        playingField.restartManager();
      }
    }
  }
}



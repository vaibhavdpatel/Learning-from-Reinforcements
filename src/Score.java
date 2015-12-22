import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Label;

//
//
// Score
//
//

class Score 
{ private Label        label; // Label for use in the display.
  private String       name;
  private PlayingField playingField;
  private int          value;
  private Font         regularFont, ejectedFont;

  Score(String name, Color color, int initialValue, PlayingField playingField)
  {
    this.name         = name;
    this.playingField = playingField;
    regularFont       = AgentWindow.bigBoldFont;
    ejectedFont       = AgentWindow.regularFont;
    label = new Label(name + "  0", Label.CENTER);
    label.setFont(regularFont);
    label.setForeground(color); // Buggy in Microsoft's Java (command ignored).
  }

  void setScore(int value)
  {
    if (this.value != value)
    {
      this.value = value;
      if (playingField.isDisplayOn()) redisplayScore();
    }
  }

  void ejected()
  {
    label.setFont(ejectedFont);
  }

  void reset()
  {
    label.setFont(regularFont);
  }

  void hideScore()
  {
    label.setVisible(false);
  }

  void showScore()
  {
    label.setVisible(true);
    redisplayScore();
  }

  void redisplayScore()
  {
    label.setText(name + "  " + value);
    label.validate();
  }

  int getScore()
  {
    return value;
  }

  Component getComponent()
  {
    return label;
  }

}


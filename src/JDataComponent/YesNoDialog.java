/*

   YesNoDialog.java

   A 1.1 compatible YesNoDialog box
   
   Created: 6 February 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.Dialog;

import java.awt.*;
import java.awt.event.*;

import gjt.ButtonPanel;

public class YesNoDialog extends Dialog implements ActionListener {

  Button yesButton;
  Button noButton;
  ButtonPanel buttonPanel;
  ActionListener listener;

  boolean answer = false;

  public YesNoDialog(Frame frame, String title, String message, ActionListener listener)
  {
    super(frame, title, true);

    this.listener = listener;

    buttonPanel = new ButtonPanel();
    yesButton = buttonPanel.add("Yes");
    noButton = buttonPanel.add("No");
    
    yesButton.addActionListener(this);
    noButton.addActionListener(this);

    setLayout(new BorderLayout());
    add("Center", new MessagePanel(message));
    add("South", buttonPanel);
    pack();
  }

  public void setVisible(boolean b)
  {
    if (b)
      {
	answer = false;
	yesButton.requestFocus();
      }

    super.setVisible(b);
  }
  
  public boolean answeredYes()
  {
    return answer;
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == yesButton)
      {
	answer = true;
      }
    else
      {
	answer = false;
      }

    setVisible(false);
    listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
  }

}

class MessagePanel extends Panel {

  public MessagePanel(String message)
  {
    add("Center", new Label(message, Label.CENTER));
  }

  public Insets getInsets()
  {
    return new Insets(10,10,10,10);
  }
}

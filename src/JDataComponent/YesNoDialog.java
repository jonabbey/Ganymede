/*

   YesNoDialog.java

   A 1.1 compatible YesNoDialog box
   
   Created: 6 February 1997
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDialog;

import java.awt.*;
import java.awt.event.*;

import com.sun.java.swing.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     YesNoDialog

------------------------------------------------------------------------------*/

public class YesNoDialog extends JCenterDialog implements ActionListener {

  JButton yesButton;
  JButton noButton;
  ButtonPanel buttonPanel;
  ActionListener listener;

  boolean answer = false;

  /* -- */

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
  }

  /**
   * 
   *@deprecated
   */
  public void show()
  {
    pack();
    super.show();
  }

  public void setVisible(boolean b)
  {
    if (b)
      {
	answer = false;
	yesButton.requestFocus();
      }

    pack();
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

class MessagePanel extends JPanel {

  public MessagePanel(String message)
  {
    add("Center", new Label(message, Label.CENTER));
  }

  public Insets getInsets()
  {
    return new Insets(10,10,10,10);
  }
}

/**
 * Button panel employs a BorderLayout to lay out a
 * Panel to which Buttons are added in the 
 * center.<p>
 *
 * Buttons may be added to the panel via two methods:
 * <dl>
 * <dd> void   add(Button)
 * <dd> Button add(String)
 * </dl>
 * <p>
 *
 * Button add(String) creates a Button and adds it to the
 * panel, then returns the Button created, as a convenience to
 * clients so that they do not have to go through the pain
 * and agony of creating an ImageButton.<p>
 *
 * @version 1.0, Apr 1 1996
 * @author  David Geary
 */

class ButtonPanel extends JPanel {
    JPanel     buttonPanel = new JPanel();
    //    Separator separator   = new Separator();

    public ButtonPanel() 
    {
      setLayout(new BorderLayout(0,5));
      //      add("North",  separator);
      add("Center", buttonPanel);
    }

    public void add(JButton button) 
    {
      buttonPanel.add(button);
    }

    public JButton add(String buttonLabel) 
    {
      JButton addMe = new JButton(buttonLabel);
      buttonPanel.add(addMe);
      return addMe;
    }

    protected String paramString() 
    {
      return super.paramString() + "buttons=" +
        countComponents();
    }
}

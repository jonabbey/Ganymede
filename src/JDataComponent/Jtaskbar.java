/*-----------------------------------------------------------------
  Class 
  Jtaskbar

  This will act like the win95 taskbar and let you go to different
  windows and stuff.

  Override the actionPerformed method to get it to do something.

-----------------------------------------------------------------*/

package arlut.csd.JDataComponent;

import com.sun.java.swing.*;
import gjt.RowLayout;
import java.awt.Color;
import java.awt.Insets;
import java.util.Hashtable;
import java.util.Enumeration;
import java.lang.String;
import java.awt.event.*;


public class Jtaskbar extends JPanel implements ActionListener {

  Hashtable buttons;
  JPanel panel;

  public Jtaskbar()
    {

      buttons = new Hashtable();
      panel = new JPanel();
      add(panel);
    }

  /**
   * Add a button to the bar.
   *
   * @param label Text to use as label or button
   */

  public void addButton(String label)
    {
      //System.out.println("Adding " + label);
      JButton button = new JButton(label);
      button.addActionListener(this);
      panel.add(button);
      buttons.put(label, button);
      panel.doLayout();
      button.setPad(new Insets(5,2,5,2));
      this.validate();
    }

  /**
   * Remove a button from the bar
   *
   * @param label Label of button to remove
   */
  public void removeButton(String label)
    {
      panel.remove((JButton)buttons.get(label));
      buttons.remove(label);
      this.validate();
    }

  /**
   * Rebuild the bar, adding the buttons to the panel again.
   */
  public void rebuildBar()
    {
      panel.removeAll();

      Enumeration enum = buttons.keys();
      while (enum.hasMoreElements())
	{
	  panel.add((JButton)enum.nextElement());

	}

      this.validate();

    }

  /**
   * Change the padding between label and button border
   *
   * @param pad New pad for buttons
   */
  public void setButtonPad(Insets pad)
    {
      
      Enumeration enum = buttons.keys();
      while (enum.hasMoreElements())
	{
	  ((JButton)enum.nextElement()).setPad(pad);
	}

    }

  /**
   * Empty action Performed.  Override this to have the taskbar to what you want.
   */
  public void actionPerformed(ActionEvent e)
    {
      System.out.println("Button clicked in taskbar");
    }


}

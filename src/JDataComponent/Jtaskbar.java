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

  public void addButton(String label)
    {
      //System.out.println("Adding " + label);
      JButton button = new JButton(label);
      button.addActionListener(this);
      panel.add(button);
      buttons.put(label, button);
      panel.doLayout();
      this.validate();
    }
  
  public void removeButton(String label)
    {
      panel.remove((JButton)buttons.get(label));
      buttons.remove(label);
      this.validate();
    }

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

  public void actionPerformed(ActionEvent e)
    {
      System.out.println("Button clicked in taskbar");
    }


}

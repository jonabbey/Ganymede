/*

   YesNoDialog.java

   A 1.1 compatible YesNoDialog box
   
   Created: 6 February 1997
   Release: $Name:  $
   Version: $Revision: 1.9 $
   Last Mod Date: $Date: 1999/01/22 18:04:02 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.JDialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

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
        getComponentCount();
    }
}

/*
   JentryField.java
   
   Created: 12 Jul 1996
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/01/22 18:04:00 $
   Module By: Navin Manohar

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

package arlut.csd.JDataComponent;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;

import javax.swing.*;
import javax.swing.text.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     JstringArea

------------------------------------------------------------------------------*/

public class JstringArea extends JTextArea implements FocusListener{

  final static boolean debug = false;

  // ---

  public boolean allowCallback = false;

  protected boolean changed = false; 

  protected JsetValueCallback my_parent = null;

  String
    value = null,
    allowedChars = null,
    disallowedChars = null;

  /* -- */

  public JstringArea() 
  {
    setEditable(true);

    addFocusListener(this);

    setBorder(BorderFactory.createLineBorder(Color.black));

    enableEvents(AWTEvent.KEY_EVENT_MASK); 
  }

  public JstringArea(int rows, int columns) 
  {
    super(rows, columns);
    setEditable(true);

    setBorder(BorderFactory.createLineBorder(Color.black));

    addFocusListener(this);

    enableEvents(AWTEvent.KEY_EVENT_MASK); 
  }

  ///////////////////
  // Class Methods //
  ///////////////////

  /**
   *  returns true if the value in the JentryField has 
   *  been modified.
   */

  public boolean getChanged()
  {
    return changed;
  }


  public void setAllowedChars(String s)
  {
    allowedChars = s;
  }

  public void setDisallowedChars(String s)
  {
    disallowedChars = s;
  }

  /**
   *  sets the parent of this component for callback purposes
   *
   */

  public void setCallback(JsetValueCallback parent)
  {
    if (parent == null)
      {
	throw new IllegalArgumentException("Invalid Parameter: parent cannot be null");
      }
    
    my_parent = parent;

    allowCallback = true;
  }

  /**
   * sendCallback is called when focus is lost.
   */

  public  void sendCallback()
  {
    String str;

    /* -- */

    // if nothing in the JstringField has changed,
    // we don't need to worry about this event.
    
    str = getText();
    
    if (value != null)
      {
	if (debug)
	  {
	    System.err.println("JstringArea.sendCallback: old value != null");
	  }
	
	changed = !value.equals(str);
      }
    else
      {
	if (debug)
	  {
	    System.err.println("JstringArea.sendCallback: old value == null");
	  }
	
	changed = true;
      }

    if (debug)
      {
	System.err.println("JstringArea.sendCallback(): str == " + str);
      }
    
    if (!changed)
      {
	if (debug)
	  {
	    System.err.println("JstringArea.sendCallback: no change, ignoring");
	  }
	
	return;
      }
    
    if (!allowCallback) 
      {
	value = str;
	return;
      }

    boolean b = false;
	  
    try 
      {
	if (debug)
	  {
	    System.err.println("JstringArea.sendCallback: making callback");
	  }
	
	b = my_parent.setValuePerformed(new JValueObject(this, str, JValueObject.SET));
      }
    catch (RemoteException re)
      {
      }

    // If the setValuePerformed callback failed, we'll revert the value to our last
    // approved value
    
    if (!b) 
      {
	if (debug)
	  {
	    System.err.println("JstringArea.sendCallback: setValue rejected");
		
	    if (value == null)
	      {
		System.err.println("JstringArea.sendCallback: resetting to empty string");
	      }
	    else
	      {
		System.err.println("JstringArea.sendCallback: resetting to " + value);
	      }
	  }
	    
	if (value == null)
	  {
	    super.setText("");
	  }
	else
	  {
	    super.setText(value);
	  }
	    
	changed = false;
      }
    else 
      {
	if (debug)
	  {
	    System.err.println("JstringArea.sendCallback: setValue accepted");
	  }

	value = str;
		
	changed = false;
      }
    }
    

  /**
   *  Stub function that is overriden is subclasses of JentryField
   *
   */

  private boolean isAllowed(char ch)
  {
    if (disallowedChars != null)
      {
	if (disallowedChars.indexOf(ch) != -1)
	  {
	    if (debug)
	      {
		System.err.println("JstringArea.isAllowed() rejecting char " + ch + " as disallowed");
	      }

	    return false;
	  }
      }
    
    if (allowedChars != null)
      {
	if (allowedChars.indexOf(ch) == -1)
	  {
	    if (debug)
	      {
		System.err.println("JstringArea.isAllowed() rejecting char " + ch + " as not allowed");
	      }

	    return false;
	  }
      }
    
    return true;
  }
  
  /**
   *
   * We only want certain keystrokes to be registered by the field.
   *
   * This method overrides the processKeyEvent() method in JComponent and
   * gives us a way of intercepting characters that we don't want in our
   * string field.
   *
   */

  protected void processKeyEvent(KeyEvent e)
  {
    // always pass through useful editing keystrokes.. this seems to be
    // necessary because backspace and delete apparently have defined
    // Unicode representations, so they don't match CHAR_UNDEFINED below

    if ((e.getKeyCode() == KeyEvent.VK_BACK_SPACE) ||
	(e.getKeyCode() == KeyEvent.VK_DELETE) ||
	(e.getKeyCode() == KeyEvent.VK_END) ||
	(e.getKeyCode() == KeyEvent.VK_HOME))
      {
	super.processKeyEvent(e);
	return;
      }

    // We check against KeyEvent.CHAR_UNDEFINED so that we pass
    // through things like backspace, arrow keys, etc.

    if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED)
      {
	super.processKeyEvent(e);
	return;
      }
    else if (isAllowed(e.getKeyChar()))
      {
	super.processKeyEvent(e);
	return;
      }

    // otherwise, we ignore it

    if (debug)
      {
	System.err.println("JstringArea: skipping key event " + e);
      }
  }

  public void focusLost(FocusEvent e)
  {
    if (debug)
      {
	System.out.println("JentryField: focusLost");
      }

    sendCallback();
  }

  public void focusGained(FocusEvent e)
  {
    if (debug)
      {
	System.out.println("focusGained");
      }
  }

  /**
   *
   * Debug rig
   *
   */

  public static final void main(String argv[]) 
  {
    JFrame frame = new JFrame();

    JstringArea area = new JstringArea();
    area.setDisallowedChars("asdf");
    frame.getContentPane().add(area);

    area.setCallback(new JsetValueCallback() {
      public boolean setValuePerformed(JValueObject o) {
	System.out.println("I got an o: " + o.getValue());
	return true;
      }
    });

    frame.pack();
    frame.setSize(200,200);
    frame.setVisible(true);
  }
}

/*
   JentryField.java
   
   Created: 12 Jul 1996

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Navin Manohar

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA
*/

package arlut.csd.JDataComponent;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     JstringArea

------------------------------------------------------------------------------*/

public class JstringArea extends JTextArea implements FocusListener {

  final static boolean debug = false;

  // ---

  public boolean allowCallback = false;

  protected boolean changed = false; 

  protected JsetValueCallback my_parent = null;

  private boolean processingCallback = false;

  String
    value = null,		// last known value, used in comparisons to see if we need to do full callback
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

  public void setText(String s)
  {
    value = s;
    super.setText(s);
  }

  public void setAllowedChars(String s)
  {
    allowedChars = s;
  }

  public void setDisallowedChars(String s)
  {
    disallowedChars = s;
  }

  public void setEditable(boolean val)
  {
    if (!val)
      {
	allowCallback = false;
	my_parent = null;
      }

    super.setEditable(val);
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

  public void sendCallback()
  {
    String str;
    
    /* -- */

    synchronized (this)
      {
	if (processingCallback)
	  {
	    return;
	  }
	
	processingCallback = true;
      }

    try
      {
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
	else			// value == null
	  {
	    if (debug)
	      {
		System.err.println("JstringArea.sendCallback: old value == null");
	      }

	    if (str == null || str.equals(""))
	      {
		changed = false;
	      }
	    else
	      {
		changed = true;
	      }
	  }

	if (debug)
	  {
	    System.err.println("JstringArea.sendCallback(): str == '" + str + "', value == '" + value + "'");
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

	    if (str.equals(""))
	      {
		b = my_parent.setValuePerformed(new JSetValueObject(this, null));
	      }
	    else
	      {
		b = my_parent.setValuePerformed(new JSetValueObject(this, str));
	      }
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
		System.err.println("JstringArea.sendCallback: setValue('" + str + "') accepted");
	      }

	    if (str != null && str.equals(""))
	      {
		value = null;
	      }
	    else
	      {
		value = str;
	      }
		
	    changed = false;
	  }
      }
    finally
      {
	processingCallback = false;
      }
  }

  /**
   *  Stub function that is overriden in subclasses of JentryField
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
	System.out.println("JstringArea: focusLost");
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
    //    area.setDisallowedChars("asdf");
    frame.getContentPane().add(new JScrollPane(area));

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


/*
   JcheckboxField.java

   
   Created: 12 Jul 1996
   Release: $Name:  $
   Version: $Revision: 1.10 $
   Last Mod Date: $Date: 1999/01/22 18:03:57 $
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

import javax.swing.*;

import java.awt.event.*;
import java.awt.*;

/*******************************************************************
                                                      JcheckboxField()

 This class defines a JcheckboxField.

*******************************************************************/
public class JcheckboxField extends JCheckBox implements ItemListener {

  private boolean allowCallback = false;
  private boolean changed = false; 

  private boolean isEditable = true;

  private JsetValueCallback callback = null;

  private boolean value;
  private boolean oldvalue;
  
  private String label;

  private boolean notifyOnFocus = false;

  ///////////////////
  //  Constructors //
  ///////////////////

  /**
   * Constructor that can create a JcheckboxField with a label, a particular state,
   * and with the specified font/foreground/background value
   *
   * @param label the label to use for this JcheckboxField
   * @param state the state to which this JcheckboxField is to be set
   */
  public JcheckboxField(String label,boolean state,boolean editable)
  {
    super(label);

    value = state;
    oldvalue = state;
    
    setSelected(state);
    
    setEnabled(editable);
    isEditable = editable;

    //enableEvents(AWTEvent.FOCUS_EVENT_MASK); 

    addItemListener(this);
  }

  /** Constructor that creates a basic checkbox with default foreground and background
   *
   */
  public JcheckboxField()
  {
    this(null, false, true);
    /*
    super();
    
    value = false;
    oldvalue = false;
    
    setSelected(false);

    enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    enableEvents(AWTEvent.FOCUS_EVENT_MASK);

    //addItemListener(this);
    */
  }
  
  /**
   *
   * @param label the label to use for this JcheckboxField
   * @param state the state to which this JcheckboxField is to be set
   * @param callback the component which can use the value of this JcheckboxField
   *
   */
  public JcheckboxField(String label,boolean state,boolean editable,JsetValueCallback callback)
    {
      this(label,state,editable);

      setCallback(callback);
      
    }

  ///////////////////
  // Class Methods //
  ///////////////////

  /**
   *  sets the parent of this component for callback purposes
   *
   */
  public void setCallback(JsetValueCallback callback)
  {
    if (callback == null)
      {
	throw new IllegalArgumentException("Invalid Parameter: callback cannot be null");
      }
    
    this.callback = callback;

    allowCallback = true;
  }


  /**
   * sets the value back to what it was before it was
   * changed 
   */
  public void resetValue()
  {
    setValue(oldvalue);
  }

  /**
   * sets the value of this JcheckboxField to the boolean
   * value of state.
   * 
   */
  
  public void setValue(boolean state)
  {
    setSelected(state);
  }

  /**
   * returns the value represented by this JcheckboxField
   */
  public boolean getValue()
  {
    return isSelected();
  }

  /**
   * returns true if the value of this JcheckboxField has changed
   * since it was initiallly created.
   */
  
  public boolean getChanged()
  {
    return changed;
  }

  /**
   * sets the label of this JcheckboxField
   *
   */

  public void setText(String label)
  {
    this.label = label;

    super.setText(label);
  }


  public void setSelected(boolean state)
  {
    setSelected(state, true);
  }

  /**
   * sets the state of this JcheckboxField
   */
  public void setSelected(boolean state, boolean sendCallback)
  {
    if (value != state)
      {
	changed = true;
      }

    this.value = state;
    if (sendCallback)
      {
	super.setSelected(state);
      }
    else
      {
	//removeItemListener(this);
	super.setSelected(state);
	//addItemListener(this);
      }


  }


  protected void processFocusEvent(FocusEvent e)
  {
    if (!notifyOnFocus)
      {
	return;
      }
    
    switch (e.getID()) 
      {
      
      case FocusEvent.FOCUS_LOST:
	{
	  if (!changed) 
	    {
	      break;
	    }

 	  value = isSelected();
	  notify(value);

	  break;
	}

      case FocusEvent.FOCUS_GAINED:
	{
	}
    }

    super.processFocusEvent(e);
  }

  /**
   * @see java.awt.event.itemListener
   */  

  public void itemStateChanged(ItemEvent e)
  {
    notify(e.getStateChange() == ItemEvent.SELECTED);
  }

  private void notify(boolean value)
  {
    Boolean bval = new Boolean(value);

    if (allowCallback) 
      {
	// do a callback to talk to the server

	boolean b = false;

	try 
	  {
	    b = callback.setValuePerformed(new JValueObject(this,bval));
	  }
	catch (java.rmi.RemoteException ex) 
	  {
	    throw new RuntimeException("notify caught remote exception: " + ex);
	  }
	
	if (b==false)
	  {
	    resetValue();
	  }
	else
	  {
	    oldvalue = value;
	  }
      }
  }
}
/************************************************************/ 







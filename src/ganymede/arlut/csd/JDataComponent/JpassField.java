/*
   JpassField.java
   
   Created: 22 July 1997
   Release: $Name:  $
   Version: $Revision: 1.13 $
   Last Mod Date: $Date: 2002/10/05 05:38:25 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002
   The University of Texas at Austin.

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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.rmi.RemoteException;

import javax.swing.*;

/*------------------------------------------------------------------------------
                                                                           class 
                                                                      JpassField

------------------------------------------------------------------------------*/

/**
 * <p>JpassField is the composite two-field GUI component used for entering
 * passwords in the Ganymede client.</p>
 */

public class JpassField extends JPanel implements JsetValueCallback {

  public static final boolean debug = false;

  public boolean allowCallback = false;
  protected boolean changed = false; 

  protected JsetValueCallback my_parent = null;

  Frame
    frame;

  JButton 
    changePass = null;

  JpasswordField 
    field1 = null,
    field2 = null;

  private boolean changingPass = false;

  String
    value1 = null,
    value2 = null;

  String validatedPass = null;

  /* -- */

  /**
   *
   * Constructor
   *
   */

  public JpassField(Frame frame,
		    int columns,
		    int maxstrlen,
		    boolean is_editable)
  {
    this(frame, columns, maxstrlen,
	 is_editable,
	 null,
	 null);
  }

  /**
   *
   * Constructor
   *
   */

  public JpassField(Frame frame,
		    int columns, 
		    int maxstrlen, 
		    boolean is_editable,
		    String allowed, 
		    String disallowed) 
  {
    this.frame = frame;

    field1 = new JpasswordField(columns, maxstrlen, 
				is_editable, true, allowed, disallowed);
    
    field1.setCallback(this);
    field1.setEditable(is_editable);
    
    field2 = new JpasswordField(columns, maxstrlen,
				is_editable, true, allowed, disallowed);
    
    field2.setCallback(this);
    field2.setEditable(is_editable);
    
    GridBagLayout gbl;
    GridBagConstraints gbc;
    
    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    
    setLayout(gbl);
    
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(field1, gbc);
    add(field1);
    
    gbc.gridy = 1;
    gbl.setConstraints(field1, gbc);
    add(field2);
  }

  /**
   * <p>The callback our contained components use to report to us.</p>
   */

  public boolean setValuePerformed(JValueObject v)
  {
    JpasswordField s;

    /* -- */

    s = (JpasswordField) v.getSource();

    if (s == field1)
      {
	if (debug)
	  {
	    System.err.println("setting value 1");
	  }

	value1 = (String) v.getValue();

	if (debug)
	  {
	    System.err.println("value 1 = " + value1);
	  }

	field2.setText(null);
	value2 = null;
      }
    else if (s == field2)
      {
	if (debug)
	  {
	    System.err.println("setting value 2");
	  }

	value2 = (String) v.getValue();

	if (debug)
	  {
	    System.err.println("value 2 = " + value2);
	  }

	if (!changingPass && (value1 != null) && (value2 != null) && (value1.equals(value2)))
	  {
	    validatedPass = value2;

	    try
	      {
		changingPass = true;

		try
		  {
		    // if our callback rejects the password, it will
		    // handle informing the user of what and why

		    if (my_parent != null && !my_parent.setValuePerformed(new JValueObject(this, value1)))
		      {
			value1 = null;
			value2 = null;
			field1.setText(null);
			field2.setText(null);

			// try again!

			field1.requestFocus();
			
			return false;
		      }
		  }
		finally
		  {
		    changingPass = false;
		  }
	      }
	    catch (RemoteException ex)
	      {
		reportError("Error communicating with server.. network or server problem?");

		field1.setText(null);
		field2.setText(null);
		value1 = null;
		value2 = null;

		field1.requestFocus();

		return false;
	      }
	  }
	else if ((value1 != null) && (value2 != null) && (!value1.equals(value2)))
	  {
	    reportError("Passwords do not match, please try again.");

	    field1.setText(null);
	    field2.setText(null);
	    value1 = null;
	    value2 = null;
	    validatedPass = null;

	    field1.requestFocus();

	    return false;
	  }
      }
    else
      {
	System.err.println("whatthe?");
	return false;		// ??
      }

    return true;
  }

  /**
   * <p>Returns the plain text password if it has been
   * validly set.</p>
   */

  public String getPassword()
  {
    return validatedPass;
  }
  
  /**
   *  returns true if the value in the JpassField has 
   *  been modified.
   */

  public boolean getChanged()
  {
    return changed;
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
   * <p>This private helper method relays a descriptive error message to
   * our callback interface.</p>
   */

  private void reportError(String errorString)
  {
    if (allowCallback)
      {
	try
	  {
	    my_parent.setValuePerformed(new JValueObject(this, errorString, JValueObject.ERROR));
	  }
	catch (java.rmi.RemoteException rx)
	  {
	    System.out.println("Could not send an error callback.");
	  }
      }
  }
}

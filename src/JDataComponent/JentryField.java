/*
   JentryField.java

   JentryField serves as an abstract base class for all Fields that
   use textfields.  The subclasses of this class should be used.
   
   Created: 12 Jul 1996
   Release: $Name:  $
   Version: $Revision: 1.23 $
   Last Mod Date: $Date: 1999/08/19 02:12:44 $
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

import javax.swing.*;
import javax.swing.text.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     JentryField

------------------------------------------------------------------------------*/

/**
 * <p>JentryField serves as an abstract base class for all GUI fields in the
 * client that use textfields.  All text entry fields in the Ganymede client
 * are done using JentryField.  Among other responsibilities, JentryField is
 * responsible for dispatching a callback event when the user tabs out of
 * a field he has been entering text into.</p>
 *
 * <p>In combination with the {@link arlut.csd.JDataComponent.JentryDocument JentryDocument}
 * class, JentryField makes sure that the user can't type invalid characters,
 * nor too many characters, providing immediate feedback if he tries.</p>
 *
 * <p>See this subclasses of this class for actual usable classes.</p>
 */

abstract public class JentryField extends JTextField implements FocusListener {

  static final boolean debug = false;

  // ---

  public boolean allowCallback = false;
  protected boolean changed = false; 

  protected JsetValueCallback my_parent = null;

  /* -- */

  //////////////////
  // Constructors //
  //////////////////

  public JentryField(int columns) 
  {
    super(columns);
    setEditable(true);

    addFocusListener(this);

    // try to force a validate so that NT isn't so bitchy

    setColumns(columns);

    setDocument(new JentryDocument(this));
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

  public abstract void sendCallback();

  /**
   *  Stub function that is overriden in subclasses of JentryField
   */

  public boolean isAllowed(char ch)
  {
    if (debug)
      {
	System.out.println("isAllowed in JentryField");
      }

    return true;
  }

  /**
   * Returns the maximum acceptable size of this field
   *
   * If no max size has been set, will return -1.
   */

  public int getMaxStringSize()
  {
    return -1;
  }

  /**
   * returns the current size of the contents of this gui field
   */

  public int getLength()
  {
    return 0;
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
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                  JentryDocument

------------------------------------------------------------------------------*/

/**
 * <p>Helper class for {@link arlut.csd.JDataComponent.JentryField JentryField}
 * and its subclasses.  JentryDocument is responsible for guaranteeing that
 * the user cannot enter invalid characters into a string field in the client,
 * nor type too many characters.</p>
 */

class JentryDocument extends PlainDocument {

  final static boolean debug = false;

  // ---

  private JentryField field;

  /* - */

  public JentryDocument(JentryField field)
  {
    this.field = field;
  }

  public void insertString(int offset, String str, AttributeSet a) throws BadLocationException
  {
    StringBuffer buffer = new StringBuffer();

    /* -- */

    if (debug)
      {
	System.err.println("JentryDocument.insertString(" + str +")");
      }

    for (int i = 0; i < str.length(); i++)
      {
	char c = str.charAt(i);

	if (!field.isAllowed(c) ||
	     (field.getMaxStringSize() != -1 && 
	      field.getMaxStringSize() - field.getLength() <= 0))
	  {
	    if (debug)
	      {
		System.err.println("Trying to reject character " + c);
	      }

	    Toolkit.getDefaultToolkit().beep();
	  }
	else
	  {
	    buffer.append(c);
	  }
      }

    if (buffer.length() != 0)
      {
	if (debug)
	  {
	    System.err.println("Inserting string " + buffer.toString());
	  }

	super.insertString(offset, buffer.toString(), a);
      }
  }
}

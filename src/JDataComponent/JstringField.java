/*
   JstringField.java

   This class defines an entry field that is capable of handling
   strings.  It is also possible to restrict the characters which are
   accepted by this gui component.  Furthermore, the maximum size of
   the string that can be entered into this JstringField can be
   preset.
   
   Created: 12 Jul 1996
   Release: $Name:  $
   Version: $Revision: 1.30 $
   Last Mod Date: $Date: 2002/10/05 05:38:25 $
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
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;


/*------------------------------------------------------------------------------
                                                                           class
                                                                    JstringField

------------------------------------------------------------------------------*/

/**
 * <p>This class defines an entry field that is capable of handling
 *    strings.  It is also possible to restrict the characters which
 *    are accepted by this gui component.  Furthermore, the maximum
 *    size of the string that can be entered into this JstringField can
 *    be preset.</p>
 */

public class JstringField extends JentryField {

  public static final boolean debug = false;

  public static int DEFAULT_COLS = 15;
  public static int DEFAULT_SIZE = 4096;

  // ---
  
  private int size = DEFAULT_SIZE;

  private String value = null;
  private String allowedChars = null;
  private String disallowedChars = null;

  private boolean processingCallback = false;

  /* -- */

  /**  Constructors ***/

  /**
   * Base constructor for JstringField
   * 
   * @param columns number of colums in the JstringField
   * @param valueAttr used to determine the foregoudn/background/font for this JstringField
   * @param is_editable true if this JstringField is editable
   */

  public JstringField(int columns,
		      int maxstrlen,
		      boolean is_editable,
		      boolean invisible,
		      String allowed,
		      String disallowed)
  {
    super(columns);
    
    if (maxstrlen <= 0)
      {
	throw new IllegalArgumentException("Invalid Parameter: maximum string size is negative or zero");
      }

    size = maxstrlen;
    
    setEditable(is_editable);  // will this JstringField be editable or not?

    if (allowed != null)
      {
	setAllowedChars(allowed);
      }

    if (disallowed != null)
      {
	setDisallowedChars(disallowed);
      }
  }
  
  /**
   * Constructor which uses default fonts,no parent,
   * default column size, and default foregound/background
   * colors.
   */

  public JstringField()
  {
    this(JstringField.DEFAULT_COLS,JstringField.DEFAULT_SIZE,
	 true,
	 false,
	 null,
	 null);
  }

  /**
   * Simple constructor.
   *
   *
   */

  public JstringField(int cols, boolean is_editable)
  {
    this(cols,JstringField.DEFAULT_SIZE,
	 is_editable,
	 false,
	 null,
	 null);
  }

  /**
    * Constructor that allows for the creation of a JstringField
    * that knows about its parent.
    *
    * @param cols number of colums in the JstringField
    * @param valueAttr used to determine the foregoudn/background/font for this JstringField
    * @param parent the container within which this JstringField is contained
    *        (This container will implement an interface that will utilize the
    *         data contained within this JstringField.)
    *
    */

  public JstringField(int cols,
		      int maxstrlen,
		      boolean is_editable,
		      boolean invisible,
		      String allowed,
		      String disallowed,
		      JsetValueCallback callback)
  {
    this(cols,maxstrlen,is_editable,invisible,allowed,disallowed);

    setCallback(callback);
  }
  
 /************************************************************/
 // JstringField methods

  /**
   *  sets the JstringField to a specific value
   *
   * @param str value to which the JstringField is set
   */

  public void setText(String str)
  {
    if (debug)
      {
	System.out.println("JstringField: setText() in JstringField");
      }

    boolean editable = isEditable();
    setEditable(true);

    if (str == null) 
      {
	if (debug)
	  {
	    System.out.println("JstringField: String is null.");
	  }

	value = new String("");
	
	super.setText("");
      }
    else 
      {
	verifyValue(str);

	if (debug)
	  {
	    System.out.println("JstringField: Calling super.setText(" + str + ")");
	  }

	super.setText(str); 

	value = str;
      }

    setEditable(editable);
  }

  /**
   * <p>Checks the entire string str for compliance with this field's constraints.
   *
   * Throws an IllegalArgumentException if the provided string is not acceptable.
   */

  private void verifyValue(String str)
  {
    if (str.length() > size)
      {
	throw new IllegalArgumentException("string too long");
      }
    
    for (int i = 0; i < str.length(); i++)
      {
	if (!isAllowed(str.charAt(i)))
	  {
	    throw new IllegalArgumentException("invalid char in string: " + 
					       str.charAt(i));
	  }
      }
  }

  /**
   *
   * Return the string contained in this field, whether it has been validated
   * in a callback or not.
   *
   */

  public String getValue() 
  {
    return getText();
  }

  /**
   *  returns the character located at position n in the JstringField value
   *
   * @param n position in the JstringField value from which to retrieve character
   */

  public char getCharAt(int n)
  {
    return this.getText().charAt(n);
  }

  /**
   *  assigns a set of characters which are valid within the JstringField
   *
   * @param s each character in this string will be considered an allowed character
   */

  public void setAllowedChars(String s)
  {
    if (s != null)
      {
	this.allowedChars = s;
      }
    else 
      {
	this.allowedChars = null;
      }
  }
 
  /**
   *  assigns a set of characters which are invalid within the JstringField
   *
   * @param s each character in this string will be considered a disallowed character
   */

  public void setDisallowedChars(String s)
  {
    if (s!= null)
      {
 	this.disallowedChars = s;
      }
    else 
      {
 	this.disallowedChars = null;
      }
  }

  /**
   *   returns the set of allowed characters as a String object
   */

  public String getAllowedChars()
  {
    return this.allowedChars;
  }

  /**
   *  returns the set of disallowed characters as a String object
   */

  public String getDisallowedChars()
  {
    return this.disallowedChars;
  }

  /**
   * returns the maximum size of the string that can be placed in this JstringField
   */

  public int getMaxStringSize()
  {
    return this.size;
  }

  /**
   * returns the current size of the contents of this gui field
   */

  public int getLength()
  {
    String text = super.getText();

    if (text == null)
      {
	return 0;
      }
    else
      {
	return text.length();
      }
  }

  /**
   * determines whether a given character is valid or invalid for a JstringField
   *
   * The JentryDocument object for this field will use this method to
   * allow or disallow the character ch from being added.
   *
   * @param ch the character which is being tested for its validity
   */

  public boolean isAllowed(char ch)
  {
    if (debug)
      {
	System.out.println("JstringField.isAllowed()");
      }

    if (disallowedChars != null)
      {
	if (disallowedChars.indexOf(ch) != -1)
	  {
	    if (debug)
	      {
		System.out.println("Disallowing char: " + ch + " because it is in string: " + disallowedChars);
	      }

	    return false;
	  }
      }
    
    if (allowedChars != null)
      {
	if (allowedChars.indexOf(ch) == -1)
	  {
	    return false;
	  }
      }
    
    return true;
  }

  /**
   * sendCallback is called when focus is lost, or when we are otherwise
   * triggered.  A true value will be returned if the value change was
   * approved and performed, or false if not.  If the value did not change,
   * this method will also return false.
   *
   * @returns -1 on change rejected, 0 on no change required, 1 on change approved
   */

  public int sendCallback()
  {
    synchronized (this)
      {
	if (processingCallback)
	  {
	    return -1;
	  }
	
	processingCallback = true;
      }

    try
      {
	String str;

	/* -- */

	// if nothing in the JstringField has changed,
	// we don't need to worry about this event.
    
	str = getText();
    
	if ((value != null && value.equals(str)) || (value == null && (str == null || str.equals(""))))
	  {
	    return 0;
	  }

	/* we don't need to check the string for validity, since it was checked
	   on a character by character basis on entry by our superclass. */
	
	try 
	  {
	    if (!allowCallback || my_parent.setValuePerformed(new JValueObject(this, str, JValueObject.SET)))
	      {
		value = str;
		return 1;
	      }
	    else
	      {
		super.setText(value == null ? "":value);
		return -1;
	      }
	  }
	catch (RemoteException re)
	  {
	    return -1;
	  }
      }
    finally
      {
	processingCallback = false;
      }
  }
}

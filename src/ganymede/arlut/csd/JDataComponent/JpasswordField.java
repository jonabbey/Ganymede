
/*
   JpasswordField.java


   Created: 12 Jul 1996


   Module By: Navin Manohar

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.JDataComponent;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.rmi.RemoteException;

import javax.swing.JPasswordField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  JpasswordField

------------------------------------------------------------------------------*/

/**
 * <p>This class defines an entry field that is capable of handling
 * strings.  It is also possible to restrict the characters which are
 * accepted by this gui component.  Furthermore, the maximum size of
 * the string that can be entered into this JstringField can be
 * preset.</p>
 */

public class JpasswordField extends JPasswordField implements FocusListener {

  public static final boolean debug = false;

  public static int DEFAULT_COLS = 15;
  public static int DEFAULT_SIZE = 4096;

  // ---

  private int size = DEFAULT_SIZE;

  private String value = null;
  private String allowedChars = null;
  private String disallowedChars = null;

  private JsetValueCallback my_parent;

  private boolean
    allowCallback = false,
    changed = false;

  /* -- */

  /**
   * Base constructor for JpasswordField
   *
   * @param columns number of colums in the JpasswordField
   * @param is_editable true if this JpasswordField is editable
   */

  public JpasswordField(int columns,
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

    setEditable(is_editable);  // will this JpasswordField be editable or not?

    setEchoChar('*');

    if (allowed != null)
      {
	setAllowedChars(allowed);
      }

    if (disallowed != null)
      {
	setDisallowedChars(disallowed);
      }

    addFocusListener(this);
  }

  public boolean isChanged()
  {
    return changed;
  }

  /**
   * Constructor which uses default fonts,no parent,
   * default column size, and default foregound/background
   * colors.
   */

  public JpasswordField()
  {
    this(JpasswordField.DEFAULT_COLS,
	 JpasswordField.DEFAULT_SIZE,
	 true,
	 false,
	 null,
	 null);
  }

  /**
   * Simple constructor.
   */

  public JpasswordField(int cols, boolean is_editable)
  {
    this(cols,
	 JpasswordField.DEFAULT_SIZE,
	 is_editable,
	 false,
	 null,
	 null);
  }

  /**
   * Constructor that allows for the creation of a JpasswordField
   * that knows about its parent.
   *
   * @param cols number of colums in the JpasswordField
   * @param callback An interface for the container within which this
   * JpasswordField is typically contained.  The JpasswordField will
   * call this interface to pass password change notifications.
   */

  public JpasswordField(int cols,
			int maxstrlen,
			boolean is_editable,
			boolean invisible,
			String allowed,
			String disallowed,
			JsetValueCallback callback)
  {
    this(cols, maxstrlen, is_editable, invisible, allowed, disallowed);

    setCallback(callback);
  }

  /**
   *  sets the parent of this component for callback purposes
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
   * Sets the JpasswordField to a specific value
   *
   * @param str value to which the JpasswordField is set
   */

  public void setText(String str)
  {
    if (str == null)
      {
	value = "";

	super.setText("");

	changed = true;
      }
    else
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

	super.setText(str);

	value = str;

	changed = true;
      }
  }

  /**
   * Returns the value of the member variable value
   */

  public String getValue()
  {
    return value;
  }

  /**
   * Returns the character located at position n in the JpasswordField value
   *
   * @param n position in the JpasswordField value from which to retrieve character
   */

  public char getCharAt(int n)
  {
    return this.getPassword()[n];
  }

  /**
   * Assigns a set of characters which are valid within the JpasswordField
   *
   * @param s each character in this string will be considered an allowed character
   */

  public void setAllowedChars(String s)
  {
    this.allowedChars = s;
  }

  /**
   * Assigns a set of characters which are invalid within the JpasswordField
   *
   * @param s each character in this string will be considered a disallowed character
   */

  public void setDisallowedChars(String s)
  {
    this.disallowedChars = s;
  }

  /**
   * Returns the set of allowed characters as a String object
   */

  public String getAllowedChars()
  {
    return this.allowedChars;
  }

  /**
   * Returns the set of disallowed characters as a String object
   */

  public String getDisallowedChars()
  {
    return this.disallowedChars;
  }

  /**
   * Returns the maximum size of the string that can be placed in this
   * JpasswordField
   */

  public int getMaxStringSize()
  {
    return this.size;
  }

  /**
   * Determines whether a given character is valid or invalid for a
   * JpasswordField
   *
   * @param ch the character which is being tested for its validity
   */

  private boolean isAllowed(char ch)
  {
    if (disallowedChars != null)
      {
	if (disallowedChars.indexOf(ch) != -1)
	  {
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

  public void sendCallback()
  {
    String str;

    // if nothing in the JpasswordField has changed,
    // we don't need to worry about this event.

    str = new String(getPassword());

    if (value != null)
      {
	if (debug)
	  {
	    System.err.println("JpasswordField.sendCallback(): old value != null");
	  }

	changed = !value.equals(str);
      }
    else
      {
	if (debug)
	  {
	    System.err.println("JpasswordField.sendCallback(): old value == null");
	  }

	changed = true;
      }

    if (!changed)
      {
	if (debug)
	  {
	    System.err.println("JpasswordField.sendCallback(): no change, ignoring");
	  }

	return;
      }

    if (allowCallback)
      {
	boolean b = false;

	try
	  {
	    if (debug)
	      {
		System.err.println("JpasswordField.sendCallback(): making callback");
	      }

	    b = my_parent.setValuePerformed(new JSetValueObject(this, str));
	  }
	catch (RemoteException re)
	  {
	  }

	if (!b)
	  {
	    if (debug)
	      {
		System.err.println("JpasswordField.sendCallback(): setValue rejected");

		if (value == null)
		  {
		    System.err.println("JpasswordField.sendCallback(): resetting to empty string");
		  }
		else
		  {
		    System.err.println("JpasswordField.sendCallback(): resetting to " + value);
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
		System.err.println("JpasswordField.sendCallback(): setValue accepted");
	      }

	    value = str;

	    changed = true;
	  }
      }
  }

  // FocusListener methods

  public void focusGained(FocusEvent e) {}
  public void focusLost(FocusEvent e)
  {
    sendCallback();
  }
}

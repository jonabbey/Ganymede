/*
   JentryField.java

   JentryField serves as an abstract base class for all Fields that
   use textfields.  The subclasses of this class should be used.

   Created: 12 Jul 1996

   Module By: Navin Manohar

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2011
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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     JentryField

------------------------------------------------------------------------------*/

/**
 * <p>JentryField serves as an abstract base class for all GUI fields
 * in the client that use textfields.  All text entry fields in the
 * Ganymede client are done using JentryField.  Among other
 * responsibilities, JentryField is responsible for dispatching a
 * callback event when the user tabs out of a field he has been
 * entering text into.</p>
 *
 * <p>JentryField includes logic to make sure that the user can't type
 * invalid characters, nor too many characters, providing immediate
 * feedback if he tries.</p>
 *
 * <p>See this subclasses of this class for actual usable classes.</p>
 */

abstract public class JentryField extends JTextField implements FocusListener, ActionListener {

  static final boolean debug = false;

  // ---

  public boolean allowCallback = false;

  /**
   * <p>True if this JentryField is in the process of programmatically
   * loading text.  If it is, we will not bother validating input
   * characters against our filters.  Normally, of course, the
   * JentryDocument automatically filters out any character-inserting
   * keystrokes that the server has requested we filter.</p>
   *
   * <p>Any JentryField subclasses that provide programmatic data
   * setting methods should set this variable to true during the
   * course of the data setting, preferably in a try.. finally block
   * so that loadingText is guaranteed to be set false when it is
   * done.</p>
   */

  protected boolean loadingText = false;

  protected JsetValueCallback my_parent = null;
  protected ActionListener notifier = null;

  /* -- */

  //////////////////
  // Constructors //
  //////////////////

  public JentryField(int columns)
  {
    super(columns);
    setEditable(true);

    addFocusListener(this);
    addActionListener(this);

    // try to force a validate so that NT isn't so bitchy

    setColumns(columns);

    setDocument(new JentryDocument(this));
  }

  ///////////////////
  // Class Methods //
  ///////////////////

  /**
   * Sets the parent of this component for callback purposes
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

  public void setEnterHandler(ActionListener listener)
  {
    this.notifier = listener;
  }

  /**
   * <p>sendCallback is called when focus is lost, or when we are
   * otherwise triggered.</p>
   *
   * @return -1 on change rejected, 0 on no change required, 1 on
   * change approved
   */

  public abstract int sendCallback();

  /**
   * <p>Returns true if this JentryField is in the process of
   * programmatically setting the text.  While isLoading() returns
   * true, the JentryDocument that filters input into this field will
   * let all characters be set.</p>
   *
   * <p>The purpose of this is so that this GUI field will not reject
   * data that was previously loaded into the matching Ganymede
   * database field, even if it was in violation of the current
   * constraints.</p>
   */

  public boolean isLoading()
  {
    return this.loadingText;
  }

  /**
   * <p>Stub function that is overriden in subclasses of JentryField.
   * The JentryDocument object for this field will use this method to
   * allow or disallow the character ch from being added.</p>
   */

  public boolean isAllowed(char ch)
  {
    if (debug)
      {
	System.err.println("isAllowed in JentryField");
      }

    return true;
  }

  /**
   * <p>Returns the maximum acceptable size of this field</p>
   *
   * <p>If no max size has been set, will return -1.</p>
   */

  public int getMaxStringSize()
  {
    return -1;
  }

  /**
   * <p>Returns the current size of the contents of this gui field</p>
   */

  public int getLength()
  {
    return 0;
  }

  public void focusLost(FocusEvent e)
  {
    if (debug)
      {
	System.err.println("JentryField: focusLost");
      }

    sendCallback();
  }

  public void focusGained(FocusEvent e)
  {
    if (debug)
      {
	System.err.println("focusGained");
      }
  }

  /**
   * <p>Handle someone hitting enter.. try to update the value and notify
   * the actionListener if we succeeded.</p>
   */

  public void actionPerformed(ActionEvent e)
  {
    if (debug)
      {
	System.err.println("enter hit");
      }

    if (notifier != null && sendCallback() >= 0)
      {
	if (debug)
	  {
	    System.err.println("enter approved");
	  }

	notifier.actionPerformed(e);
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

    if (field.isLoading())
      {
	buffer.append(str);
      }
    else
      {
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

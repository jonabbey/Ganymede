/*
   JentryField.java

   JentryField serves as an abstract base class for all Fields that
   use textfields.  The subclasses of this class should be used.
   
   Created: 12 Jul 1996
   Version: $Revision: 1.21 $ %D%
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin 

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
 *  JentryField serves as an abstract base class for all Fields that use textfields.
 *  The subclasses of this class should be used.
 */

abstract public class JentryField extends JTextField implements FocusListener{

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

class JentryDocument extends PlainDocument {

  private JentryField field;

  public JentryDocument(JentryField field)
  {
    this.field = field;
  }

  public void insertString(int offset, String str, AttributeSet a) throws BadLocationException
  {
    StringBuffer buffer = new StringBuffer();

    /* -- */

    for (int i = 0; i < str.length(); i++)
      {
	char c = str.charAt(i);

	if (!field.isAllowed(c) ||
	     (field.getMaxStringSize() != -1 && 
	      field.getMaxStringSize() - buffer.length() <= 0))
	  {
	    Toolkit.getDefaultToolkit().beep();
	  }
	else
	  {
	    buffer.append(c);
	  }
      }

    if (buffer.length() != 0)
      {
	super.insertString(offset, buffer.toString(), a);
      }
  }
}

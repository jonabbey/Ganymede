/*
   JnumberField.java

   
   Created: 12 Jul 1996
   Version: 1.1 97/07/16
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin
*/

package arlut.csd.JDataComponent;

import com.sun.java.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.lang.Integer;
/*******************************************************************
                                                      JnumberField()
*******************************************************************/

/**
 * This class defines an entry field that is capable of handling
 * integers.  The maximum and minimum bounds for the range of 
 * integers that can be entered into this JnumberField can also
 * be preset.
 */

public class JnumberField extends JentryField {

  public static int DEFAULT_COLS = 20;

  public static String allowedChars = new String("0123456789.");

  private Integer value = null;

  private Integer oldvalue;

  private boolean limited = false;

  private int maxSize;
  private int minSize;

  ///////////////////
  //  Constructors //
  ///////////////////

 /**
   * Base constructor for JnumberField
   * 
   * @param columns number of colums in the JnumberField
   * @param valueAttr used to determine the foregoudn/background/font for this JnumberField
   * @param is_editable true if this JnumberField is editable
   * @param islimited true if there is a restriction on the range of values
   * @param minsize the minimum limit on the range of values
   * @param maxsize the maximum limit on the range of values
   */ 
  public JnumberField(int columns,
		     JcomponentAttr valueAttr,
		     boolean iseditable,
		     boolean islimited,
		     int minsize,
		     int maxsize)
  {
    super(columns);
    
    if (islimited)
      {
	limited = true;
	
	maxSize = maxsize;
	minSize = minsize;
      }

    if (valueAttr == null)
      throw new IllegalArgumentException("Invalid Paramter: valueAttr is null");

    this.valueAttr = valueAttr;
   
    setEditable(iseditable);  // will this JnumberField be editable or not?

    JcomponentAttr.setAttr(this,valueAttr); 

    enableEvents(AWTEvent.FOCUS_EVENT_MASK);
    enableEvents(AWTEvent.KEY_EVENT_MASK); 
  }

  /**
   * Constructor which uses default fonts,no parent,
   * default column size, and default foregound/background
   * colors.
   */
  public JnumberField()
  {
    this(JnumberField.DEFAULT_COLS,
	 new JcomponentAttr(null,new Font("Helvetica",Font.PLAIN,12),
			   Color.black,Color.white),
	 true,
	 false,
	 0,0);
  }
 
 /**
  * Constructor that allows for the creation of a JnumberField
  * that knows about its parent and can invoke a callback method.
  *  
  * @param columns number of colums in the JnumberField
  * @param valueAttr used to determine the foregoudn/background/font for this JnumberField


  * @param is_editable true if this JnumberField is editable
  * @param islimited true if there is a restriction on the range of values
  * @param minsize the minimum limit on the range of values
  * @param maxsize the maximum limit on the range of values
  * @param parent the container within which this JnumberField is contained
  *        (This container will implement an interface that will utilize the
  *         data contained within this JnumberField.)
  *
  */ 
  public JnumberField(int columns,
		     JcomponentAttr valueAttr,
		     boolean iseditable,
		     boolean limited,
		     int minsize,
		     int maxsize,
		     JsetValueCallback parent)
  {
    this(columns,valueAttr,iseditable,limited,minsize,maxsize);
    
    setCallback(parent);
  }


  ///////////////////
  // Class Methods //
  ///////////////////
 
  /**
   * returns true if <c> is a valid numerical
   * digit.
   *
   * @param c the character to check
   */
  private boolean isAllowed(char c)
  {
    if (allowedChars.indexOf(c) == -1)
      return false;

    return true;
  }


  /**
   * returns the value of this JnumberField as an Integer object
   */
  public Integer getValue()
  {
    return value;
  }

  /**
   * sets the value of this JnumberField to num
   *
   * @param num the number to use
   */ 
  public void setValue(int num)
  {
    if (limited)
      if (num > maxSize || num < minSize)
	throw new IllegalArgumentException("Invalid Parameter: number out of range");
    
    oldvalue = value;

    value = new Integer(num);

    setText(value.toString());
  }

  /**
   * sets the value of this JnumberField using a String object
   *
   * @param num the String object to use
   */

  public void setValue(String num)
  {
    if (num == null)
      throw new IllegalArgumentException("Invalid Parameter: string cannot be null");

    if (num.equals(""))
      throw new IllegalArgumentException("Invalid Parameter: string cannot be blank");

    setValue(Integer.valueOf(num).intValue());
  }


  /**
   * sets the value of this JnumberField using an Integer object
   *
   * @param num the Integer object to use
   */
  public void setValue(Integer num)
  {
    if (num == null)
      throw new IllegalArgumentException("Invalid Parameter: Integer cannot be null");

    setValue(num.intValue());
  }

  /**
   * Sets the limited/non-limited status of this JnumberField
   * If setLimited is given a true value as a parameter, then
   * certain bounds will be imposed on the range of possible 
   * values.
   *
   * @param bool true if a limit is to be set on the range of values
   */
  public void setLimited(boolean bool)
  {
    limited = bool;
  }

  /**
   *  sets the maximum value in the range of possible values.
   *
   * @param n the number to use when setting the maximum value
   */
  public void setMaxValue(int n)
  {
    limited = true;

    maxSize = n;
  }
  
  /**
   *  sets the minimum value in the range of possible values.
   *
   * @param n the number to use when setting the minimum value
   */
  public void setMinValue(int n)
  {
    limited = true;

    minSize = n;
  }


  /**
   * returns true if there is a bound on the range of values that
   * can be entered into this JnumberField
   */
  public boolean isLimited()
  {
    return limited;
  }

  /**
   * returns the maximum value in the range of valid values for this
   * JnumberField
   */
  public int getMaxValue()
  {
    return maxSize;
  }

  /**
   * returns the minimum value in the range of valid values for this
   * JnumberField
   */
  public int getMinValue()
  {
    return minSize;
  }

  /**
   *  invoked whenever a key is pressed within the JnumberField. 
   *  If the key pressed was valid, then it will echo to the
   *  screen.  If the character was invalid, then nothing will
   *  happen.
   *
   * @param e the KeyEvent that needs to be processed
   */
  public void processKeyEvent(KeyEvent e)
  {
    if (e.getID() == KeyEvent.KEY_PRESSED)
      {
	
	// Handle the tab key
	   
	if (e.getKeyCode() == KeyEvent.VK_TAB)
	  {
	    super.processKeyEvent(e);
	    return;
	  }
	
	// At this point, we know that the character pressed is 
	// going to change the string in the JnumberField in some
	// way. 

	// Handle any of the other keys
	if (isAllowed(e.getKeyChar()) || e.getKeyCode() == KeyEvent.VK_BACK_SPACE ||
	    e.getKeyCode() == KeyEvent.VK_DELETE)
	  {
	    changed = true;
	  
	    oldvalue = value;
  
	    super.processKeyEvent(e);
	    return;
	  }
      }
    e.consume();
  }

  /************************************************************/

  /**
   *  Handles the events when this component looses or gains focus
   *
   *
   *
   * @param e the FocusEvent that needs to be processed
   */
  public synchronized void processFocusEvent(FocusEvent e)
    {
      // When the JnumberField widget looses focus, we must check
      // to see whether the number, if any, within the input field
      // is a valid value that falls within the specified valid range.
      
      // But first, if nothing in the field changed, then there is
      // no reason to do anything.  We simply let the focus move
      // on to another Component

      if (!changed)
	return;

      
    changed = false;

    try
      {
	setValue(Integer.valueOf(getText()).intValue());
      }
    catch (NumberFormatException ex)
      {
	if (oldvalue == null)
	  {
	    value = oldvalue;
	    setText("");
	  }
	else
	  setValue(oldvalue.intValue());
      }
    catch (IllegalArgumentException iae)
      {
	if (oldvalue == null)
	  {
	    value = oldvalue;
	    setText("");
	  }
	else
	  setValue(oldvalue.intValue());
      }

    if (value != null && allowCallback)
      {
	//Do a callback

	boolean b = false;
	
	try {

	b = my_parent.setValuePerformed(new JValueObject(this,value));

	}
	catch (java.rmi.RemoteException re) {

	  
	}

	if (!b) {

	  if (oldvalue == null)
	    {
	      value = oldvalue;
	      setText("");
	    }
	  else
	    setValue(oldvalue.intValue());
	}
	else {

	  oldvalue = value;
	  changed = false;

	}
      }
    }
}

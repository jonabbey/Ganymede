
/*
   JstringField.java

   
   Created: 12 Jul 1996
   Version: $Revision: 1.3 $ %D%
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin
*/

package arlut.csd.JDataComponent;

import com.sun.java.swing.*;
import com.sun.java.swing.text.*;

import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;

/*******************************************************************
                                                      JstringField()
*******************************************************************/

/**
 * <p>This class defines an entry field that is capable of handling
 *    strings.  It is also possible to restrict the characters which
 *    are accepted by this gui component.  Furthermore, the maximum
 *    size of the string that can be entered into this JstringField can
 *    be preset.</p>
 */

public class JpasswordField extends JPasswordField implements KeyListener{

  public static final boolean debug = false;

  public static int DEFAULT_COLS = 15;
  public static int DEFAULT_SIZE = 4096;
  
  private int size = DEFAULT_SIZE;

  private String value = null;
  private String allowedChars = null;
  private String disallowedChars = null;

  private JsetValueCallback my_parent;

  private boolean 
    allowCallback = false,
    changed = false,
    addedKeyListener = false, 
    incrementalCallback = false;

  /**  Constructors ***/

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

    //    setText(null);

    enableEvents(AWTEvent.FOCUS_EVENT_MASK);

    setEditable(is_editable);  // will this JpasswordField be editable or not?

    setEchoChar('*');

    // This will take care of masking the characters
    // in the JpasswordField if necessary.
    /* JTextField doesn't have this :(    
    if (invisible)
      setEchoChar(' ');
     */
     
    if (allowed != null)
      {
	setAllowedChars(allowed);
      }

    if (disallowed != null)
      {
	setDisallowedChars(disallowed);
      }

    enableEvents(AWTEvent.FOCUS_EVENT_MASK);
    enableEvents(AWTEvent.KEY_EVENT_MASK); 
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
    this(JpasswordField.DEFAULT_COLS,JpasswordField.DEFAULT_SIZE,
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

  public JpasswordField(int cols, boolean is_editable)
  {
    this(cols,JpasswordField.DEFAULT_SIZE,
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
    * @param parent the container within which this JpasswordField is contained
    *        (This container will implement an interface that will utilize the
    *         data contained within this JpasswordField.)
    *
    */

  public JpasswordField(int cols,
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
 // JpasswordField methods

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
   *  sets the JpasswordField to a specific value
   *
   * @param str value to which the JpasswordField is set
   */

  public void setText(String str)
  {
    if (str == null) 
      {
	value = new String("");
	
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
   *
   *  returns the value of the member variable value
   *
   */

  public String getValue() 
  {
    return value;
  }

  /**
   *  returns the character located at position n in the JpasswordField value
   *
   * @param n position in the JpasswordField value from which to retrieve character
   */

  public char getCharAt(int n)
  {
    return this.getText().charAt(n);
  }

  /**
   *  assigns a set of characters which are valid within the JpasswordField
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

     if (s == null || s.equals(""))
       {
 	return;
       }
     else
       {
	 //	 Keymap map = getKeymap();
	 //	
	 //	 Action insert = new DefaultAction(JTextComponent.insertContentAction);
	 //
	 //	 KeyStroke[] strokes = map.getKeyStrokesForAction(insert);
	 //
	 //	 map.removeBindings();
	 //	 
	 //	 for (int i = 0; i < s.length(); i++)
	 //	   {
	 //	     map.addActionForKeyStroke(KeyStroke.getKeyStroke(s.charAt(i), 0), insert);
	 //	   }
       }
  }
 
  /**
   *  assigns a set of characters which are invalid within the JpasswordField
   *
   * @param s each character in this string will be considered a disallowed character
   */
  public void setDisallowedChars(String s)
  {
    //    Keymap map;

    /* -- */

    //    map = getKeymap();

    if (s != null && !s.equals(""))
      {
	// 	for (int i = 0; i < s.length(); i++)
	// 	  {
	//	    //	    map.removeKeyStrokeBinding(KeyStroke.getKeyStroke(s.charAt(i), 0));
	//	    map.addActionForKeyStroke(KeyStroke.getKeyStroke(s.charAt(i)),
	//			      new DefaultAction("null action"));
	// 	  }
      }

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
   * returns the maximum size of the string that can be placed in this JpasswordField
   */

  public int getMaxStringSize()
  {
    return this.size;
  }


  /**
   *  determines whether a given character is valid or invalid for a JpasswordField
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

  /**
   * When the JpasswordField looses focus, any changes made to 
   * the value in the JpasswordField need to be propogated to the
   * server.  This method will handle that functionality.
   *
   * This method is synchronized to prevent overlapping callbacks
   * if we are in a threaded environment.
   *
   * @param e the FocusEvent that needs to be process
   */

  public synchronized void processFocusEvent(FocusEvent e)
  {
    super.processFocusEvent(e);

    if (debug)
      {
	System.err.println("JpasswordField.processFocusEvent: entering");
      }

    if (e.getID() == FocusEvent.FOCUS_LOST)
      {
	sendCallback();
      }

  }

  public void sendCallback()
  {
    
    String str;
    // if nothing in the JpasswordField has changed,
    // we don't need to worry about this event.
    
    str = getText();
    
    if (value != null)
      {
	if (debug)
	  {
	    System.err.println("JpasswordField.processFocusEvent: old value != null");
	  }
	
	changed = !value.equals(str);
      }
    else
      {
	if (debug)
	  {
	    System.err.println("JpasswordField.processFocusEvent: old value == null");
	  }
	
	changed = true;
      }
    
    if (!changed)
      {
	if (debug)
	  {
	    System.err.println("JpasswordField.processFocusEvent: no change, ignoring");
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
		System.err.println("JpasswordField.processFocusEvent: making callback");
	      }

	    b = my_parent.setValuePerformed(new JValueObject(this, str, JValueObject.SET));
	  }
	catch (RemoteException re)
	  {
	  }
	    
	if (b==false) 
	  {
	    if (debug)
	      {
		System.err.println("JpasswordField.processFocusEvent: setValue rejected");
		
		if (value == null)
		  {
		    System.err.println("JpasswordField.processFocusEvent: resetting to empty string");
		  }
		else
		  {
		    System.err.println("JpasswordField.processFocusEvent: resetting to " + value);
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
		System.err.println("JpasswordField.processFocusEvent: setValue accepted");
	      }

	    value = str;
		
	    changed = true;
	  }
      }
  }
  

  public void keyPressed(KeyEvent e) {}
  public void keyReleased(KeyEvent e) {}
  public void keyTyped(KeyEvent e)
  {
    if (incrementalCallback && allowCallback)
      {
	try
	  {
	    my_parent.setValuePerformed(new JValueObject(this, getText(), JValueObject.ADD));
	  }
	catch (RemoteException rx)
	  {
	    
	  }
      }
    
  }


}

/*
   JpassField.java
   
   Created: 22 July 1997
   Version: 1.1 97/07/22
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin
*/

package arlut.csd.JDataComponent;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.rmi.RemoteException;

import gjt.ColumnLayout;

import com.sun.java.swing.*;
import arlut.csd.Dialog.*;

/*------------------------------------------------------------------------------
                                                                           class 
                                                                      JpassField

------------------------------------------------------------------------------*/

/**
 *  JpassField serves as an abstract base class for all Fields that use textfields.
 *  The subclasses of this class should be used.
 */

public class JpassField extends JPanel implements ActionListener, JsetValueCallback {

  public static final boolean debug = false;

  public boolean allowCallback = false;
  protected boolean changed = false; 

  protected JsetValueCallback my_parent = null;

  protected JcomponentAttr valueAttr = null;

  Frame
    frame;

  JButton 
    changePass = null;

  JstringField 
    field1 = null,
    field2 = null;

  boolean 
    showChangeFields;

  String
    value1 = null,
    value2 = null;

  /* -- */

  /**
   *
   * Constructor
   *
   * @parameter showChangeFields if this is true, JpassField will immediately show
   * a couple of non-echoing text fields, that can both be filled in to set the
   * password.  If false, JpassField will just show a button that can be clicked
   * to set the password.
   *
   */

  public JpassField(Frame frame,
		    boolean showChangeFields,
		    int columns,
		    int maxstrlen,
		    boolean is_editable)
  {
    this(frame, showChangeFields, columns, maxstrlen,
	 new JcomponentAttr(null, new Font("SansSerif", Font.PLAIN, 12), Color.black, Color.white), 
	 is_editable,
	 null,
	 null);
  }

  /**
   *
   * Constructor
   *
   * @parameter showChangeFields if this is true, JpassField will immediately show
   * a couple of non-echoing text fields, that can both be filled in to set the
   * password.  If false, JpassField will just show a button that can be clicked
   * to set the password.
   *
   */

  public JpassField(Frame frame,
		    boolean showChangeFields, 
		    int columns, 
		    int maxstrlen, 
		    JcomponentAttr valueAttr,
		    boolean is_editable,
		    String allowed, 
		    String disallowed) 
  {
    this.frame = frame;
    this.valueAttr = valueAttr;
    this.showChangeFields = showChangeFields;

    if (showChangeFields)
      {
	field1 = new JstringField(columns, maxstrlen, valueAttr, 
				  is_editable, true, allowed, disallowed);

	field1.setCallback(this);
	field1.setEditable(is_editable);

	field2 = new JstringField(columns, maxstrlen, valueAttr, 
				  is_editable, true, allowed, disallowed);

	field2.setCallback(this);
	field2.setEditable(is_editable);

	setLayout(new ColumnLayout());
	add(field1);
	add(field2);
      }
    else
      {
	changePass = new JButton("Set Password");
	changePass.addActionListener(this);
	changePass.setEnabled(is_editable);

	setLayout(new BorderLayout());
	add("Center", changePass);
      }
  }

  public void actionPerformed(ActionEvent e)
  {
    DialogRsrc dr = new DialogRsrc(frame, "Set Password", "Enter your new password, twice");
    dr.addPassword("Password:");
    dr.addPassword("Confirm:");
    StringDialog d = new StringDialog(dr);
    Hashtable result = d.DialogShow();

    if (!allowCallback)
      {
	return;
      }

    changed = (result != null);

    if (changed)
      {
	String s1 = (String) result.get("Password:");
	String s2 = (String) result.get("Confirm:");

	if (s1 != null && s2 != null && s1.equals(s2))
	  {
	    try
	      {
		if (debug)
		  {
		    System.err.println("results are equals, calling back");
		    System.err.println("s1 = " + s1);
		    System.err.println("s2 = " + s2);
		  }

		if (!my_parent.setValuePerformed(new JValueObject(this, s1)))
		  {
		    if (debug)
		      {
			System.err.println("password not accepted");
		      }

		    StringDialog dErr = new StringDialog(frame, "Password not accepted by server",
							 "No change to password made", false);
		    dErr.DialogShow();
		  }
	      }
	    catch (RemoteException ex)
	      {
		StringDialog dErr = new StringDialog(frame, "Could not communicate with server",
						     "No change to password made", false);
		dErr.DialogShow();
	      }
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("Password not valid/confirmed");
	      }
	    StringDialog dErr = new StringDialog(frame, "Password not valid/confirmed",
						 "Password not valid/confirmed\nNo change to password made", false);
	    dErr.DialogShow();
	  }
      }
    else
      {
	if (debug)
	  {
	    System.err.println("user hit cancel");
	  }
      }

    return;
  }

  public boolean setValuePerformed(JValueObject v)
  {
    JstringField s;

    /* -- */

    s = (JstringField) v.getSource();

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

	if ((value1 != null) && (value2 != null) && (value1.equals(value2)))
	  {
	    try
	      {
		if (!my_parent.setValuePerformed(new JValueObject(this, value1)))
		  {
		    if (debug)
		      {
			System.err.println("trying to complain about server not accept");
		      }

		    //		    StringDialog dErr = new StringDialog(frame, "Password not accepted by server",
		    //							 "No change to password made", false);
		    //		    dErr.DialogShow();

		    field1.setText(null);
		    field2.setText(null);
		    value1 = null;
		    value2 = null;

		    return false;
		  }
	      }
	    catch (RemoteException ex)
	      {
		//		StringDialog dErr = new StringDialog(frame, "Could not communicate with server",
		//						     "No change to password made", false);
		//		dErr.DialogShow();

		field1.setText(null);
		field2.setText(null);
		value1 = null;
		value2 = null;

		return false;
	      }
	  }
	else if ((value1 != null) && (value2 != null) && (!value1.equals(value2)))
	  {
	    if (debug)
	      {
		System.err.println("trying to complain about mismatch");
	      }

	    // StringDialog dErr = new StringDialog(frame, "Passwords don't match",
	    //						 "Passwords don't match\nNo change to password made", false);
	    //	    dErr.DialogShow();

	    field1.setText(null);
	    field2.setText(null);
	    value1 = null;
	    value2 = null;

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

  ///////////////////
  // Class Methods //
  ///////////////////

  /**
   *  returns true if the value in the JpassField has 
   *  been modified.
   */

  public boolean getChanged()
  {
    return changed;
  }

  /**
   *  returns a JcomponentAttr object for the JpassField
   */

  public JcomponentAttr getValueAttr()
  {
    return this.valueAttr;
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
   * sets the background color for the JpassField
   * and forces a repaint
   *
   * @param color the color which will be used
   */

  public void setBackground(Color color)
  {
    setValueBackColor(color,true);
  }

  
  /**
   * sets the background color for the JpassField
   *
   * @param color the color which will be used
   * @param repaint true if the value component needs to be repainted
   */

  public void setValueBackColor(Color color,boolean repaint)
  {
    if (valueAttr == null)
      {
	return;
      }
    
    valueAttr.setBackground(color);
    
    setValueAttr(valueAttr,repaint);
  }
  
  
  /**
   * sets the attributes for the JpassField
   *
   * @param attrib the attributes which will be used
   * @param repaint true if the label component needs to be repainted
   */

  public void setValueAttr(JcomponentAttr attributes,boolean repaint)
  {
    this.valueAttr = attributes;

    if (showChangeFields)
      {
	field1.setFont(attributes.font);
	field1.setForeground(attributes.fg);
	field1.setBackground(attributes.bg);
    
	field2.setFont(attributes.font);
	field2.setForeground(attributes.fg);
	field2.setBackground(attributes.bg);

	if (repaint)
	  {
	    field1.repaint();
	    field2.repaint();
	  }
      }
    else
      {
	changePass.setFont(attributes.font);
	changePass.setForeground(attributes.fg);
	changePass.setBackground(attributes.bg);

	if (repaint)
	  {
	    changePass.repaint();
	  }
      }
  }

  /**
   *  sets the font for the JpassField and
   *  forces a repaint
   *
   * @param f the font which will be used
   */

  public void setFont(Font f)
  {
    setValueFont(f,true);
  }
  
  /**
   *  sets the font for the JpassField
   *
   * @param f the font which will be used
   * @param repaint true if the value component needs to be repainted
   */

  public void setValueFont(Font f,boolean repaint)
  {
    if (valueAttr == null)
      {
	return;
      }
    
    valueAttr.setFont(f);

    setValueAttr(valueAttr,repaint);
  }

  /**
   * sets the foreground color for the JpassField
   * and forces a repaint.
   *
   * @param color the color which will be used
   */

  public void setForeground(Color color)
  {
    setValueForeColor(color,true);    
  }

  /**
   * sets the foreground color for the JpassField
   *
   * @param color the color which will be used
   * @param repaint true if the value component needs to be repainted
   */

  public void setValueForeColor(Color color,boolean repaint)
  {
    if (valueAttr == null)
      {
	return;
      }
    
    valueAttr.setForeground(color);

    setValueAttr(valueAttr,repaint);
  } 

//   /**
//    *  processes any focus events generated in this component
//    *
//    * @param e the FocusEvent that needs to be processed
//    */

//   public void processFocusEvent(FocusEvent e)
//   {
//     super.processFocusEvent(e);
//   }
}







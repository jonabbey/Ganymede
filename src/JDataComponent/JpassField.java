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

import javax.swing.*;
import arlut.csd.JDialog.*;

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

  Frame
    frame;

  JButton 
    changePass = null;

  JpasswordField 
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
		    boolean is_editable,
		    String allowed, 
		    String disallowed) 
  {
    this.frame = frame;
    this.showChangeFields = showChangeFields;

    if (showChangeFields)
      {
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
    else
      {
	changePass = new JButton("Set Password");
	changePass.addActionListener(this);
	changePass.setEnabled(is_editable);

	setLayout(new BorderLayout());
	add("Center", changePass);
      }
  }

  public void setText(String text)
  {
    field1.setText(text);
    field2.setText(text);
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
  
}

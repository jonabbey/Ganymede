
/*
  GASH 2

  vectorPanel.java

  This module provides for a generic vector of objects, and can be
  used to implement a collection of date fields, i.p. addresses,
  or edit in place (composite) objects.

  Created: 17 Oct 1996
  Version: $Revision: 1.14 $ %D%
  Module By: Navin Manohar, Mike Mulvaney, Jonathan Abbey
  Applied Research Laboratories, The University of Texas at Austin
*/


package arlut.csd.ganymede.client;

import arlut.csd.ganymede.client.*;
import arlut.csd.ganymede.*;

import java.awt.event.*;
import java.awt.*;

import arlut.csd.JDataComponent.*;
import java.util.*;
import java.rmi.*;
import java.net.*;

import jdj.PackageResources;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     vectorPanel

------------------------------------------------------------------------------*/

/**
 *
 * This module provides for a generic vector of objects, and can be
 * used to implement a collection of date fields, i.p. addresses,
 * or edit in place (composite) objects.
 *
 */

public class vectorPanel extends JPanel implements JsetValueCallback, ActionListener {

  private final static boolean debug = true;

  // class variables

  static JcomponentAttr ca = new JcomponentAttr(null,new Font("Helvetica",Font.PLAIN,12),Color.black,Color.white);

  // --

  // object variables

  Vector
    compVector;

  Hashtable
    ewHash;

  JScrollPane 
    scrollPane;

  JButton
    addB;
  
  Vector 
    choices = null;

  short 
    type;

  LineBorder
   lineBorder = new LineBorder(Color.black);

  JPanel
    bottomPanel,
    centerPanel;

  boolean 
    editable,
    isEditInPlace,
    centerPanelAdded = false;

  Image
    removeImage;

  private db_field my_field;

  protected windowPanel parent;



  containerPanel
    container;

  /* -- */
  
  /**
   *
   *
   */

  public vectorPanel(db_field field, windowPanel parent, boolean editable, boolean isEditInPlace, containerPanel container)
  {

    if (debug)
      {
	System.out.println("Adding new vectorPanel");
      }
			   

    try
      {
	if (field == null)
	  {
	    throw new IllegalArgumentException("Illegal Argument: handle to field is null");
	  }
	
	if (!field.isVector())
	  {
	    throw new IllegalArgumentException("Invalid field type: attempt to populate a vectorPanel with a non-vector field");
	  }
	
	if (field.size() < 0)
	  {
	    throw new IllegalArgumentException("Error: vector field length has a negative value");
	  }
	
	if (parent == null)
	  {
	    throw new IllegalArgumentException("Error: Where's my windowPanel?");
	  }
	
	my_field = field;
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not init vectorPanel: " + rx);
      }

    this.editable = editable;
    this.isEditInPlace = isEditInPlace;
    this.parent = parent;
    this.container = container;

    bottomPanel = new JPanel(false);
    bottomPanel.setLayout(new BorderLayout());
    centerPanel = new JPanel(false);

    //centerPanel.setLayout(new ColumnLayout(Orientation.LEFT,Orientation.TOP));

    centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
    
    try
      {
	type = my_field.getType();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Cannot get field type: " + rx);
      }

    addB = new JButton("Create new element");

    try
      {
	if (editable && my_field.isEditable())
	  {
	    addB.addActionListener(this);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Can't check if field is editable: " + rx);
      }
    
    setLayout(new BorderLayout());
    setBorder(new com.sun.java.swing.border.EtchedBorder());
    //add("South", addB);
    
    compVector = new Vector();
    ewHash = new Hashtable();

    createVectorComponents();

    invalidate();
    container.frame.validate();
  }
  
  private void createVectorComponents()
  {
    if (my_field == null)
      {
	throw new RuntimeException("Error: my_field is null -- cannot create components");
      }

    try
      {
	if (!my_field.isVector())
	  {
	    throw new RuntimeException("Error: my_field is non-vector field");
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Can't tell if field is vector" + rx);
      }

    if (my_field instanceof date_field)
      {
	if (debug)
	  {
	    System.out.println("Adding date vector field");
	  }

	try
	  {
	    date_field datefield = (date_field)my_field;
	    
	    for (int i=0;i<datefield.size();i++) 
	      {
		if (editable)
		  {
		    JdateField df = new JdateField((Date)(datefield.getElement(i)),
						   datefield.isEditable(),
						   datefield.limited(),
						   datefield.minDate(),
						   datefield.maxDate(),
						   ca,
						   this);
		    addElement(df);
		  }
		else
		  {
		    JdateField df = new JdateField((Date)(datefield.getElement(i)),
						   false,
						   false,
						   null,
						   null,
						   ca);
		    addElement(df);
		  }
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't make checkbox field: " + rx);
	  }
	
      }
    else if (my_field instanceof num_field)
      {
	if (debug)
	  {
	    System.out.println("Adding vector number field");
	  }
	  
	try
	  {
	    num_field numfield = (num_field)my_field;
	    
	    for (int i=0;i<numfield.size();i++) 
	      {

		/*
		JnumberField nf = new JnumberField(15,
						   ca,
						   numfield.isEditable() && editable,
						   numfield.limited(),
						   numfield.getMinValue(),
						   numfield.getMaxValue(),
						   this);
						   */
		
		JstringField sf = new JstringField(12,
						   64,
						   ca,
						   (editable && numfield.isEditable()),
						   false,
						   null,
						   null,
						   this);
		    
		sf.setText(((Integer)numfield.getElement(i)).toString());
		addElement(sf);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't make num field: " + rx);
	  }
      }
    else if (my_field instanceof string_field)
      {
	System.out.println("string_field!?!  Use the stringSelector");
      }
    else if (my_field instanceof ip_field)
      {
	if (debug)
	  {
	    System.out.println("Adding ip vector field");
	  }

	try
	  {
	    ip_field ipfield = (ip_field) my_field;
	    
	    for (int i=0;i < ipfield.size();i++) 
	      {
		JIPField ipf = new JIPField(new JcomponentAttr(null,
							       new Font("Helvetica",Font.PLAIN,12),
							       Color.black,Color.white),
					    editable,
					    ipfield.v6Allowed());
		
		ipf.setValue((Byte[]) ipfield.getElement(i));
		ipf.setCallback(this);
		
		addElement(ipf);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't make ip field: " + rx);
	  }
      }
    else if (my_field instanceof invid_field)
      {
	if (debug)
	  {
	    System.out.println("Adding vector invid_field");
	  }

	try
	  {
	    invid_field invidfield = (invid_field)my_field;

	    if (isEditInPlace)
	      {
		if (debug)
		  {
		    System.out.println("Adding edit in place invid vector, size = " + invidfield.size());
		  }
		

		for (int i=0; i < invidfield.size() ; i++)
		  {
		    if (debug)
		      {
			System.out.println("Adding Invid to edit in place vector panel");
		      }
		    Invid inv = (Invid)(invidfield.getElement(i));
		   
		    db_object object = parent.getgclient().getSession().edit_db_object(inv);
		    
		    containerPanel cp = new containerPanel(object,
							   invidfield.isEditable() && editable,
							   parent.parent,
							   parent, container.frame);
		    cp.setBorder(new LineBorder(Color.black));

		    addElement(cp);
		  }
	      }
	    else
	      {
		System.out.println("*** Error - should not handle non edit-in-place Invid's in vector panel ***");
	      }	

	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't make invid field: " + rx);
	  }
      }

    try
      {
	if (my_field.isEditable())
	  {
	    if (debug)
	      {
		System.out.println("Adding add button");
	      }
	    add("South", addB);
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("Field is not editable, no button added");
	      }
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Can't check if field is editable: " + rx);
      }
  } 

  public void addNewElement()
  {
    int size = -1;

    try
      {
	size = my_field.size();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get field size: " + rx);
      }

    if (compVector.size() > size)
      {
	System.err.println("There is already an empty, new field");
      }
    else
      {
	if (debug)
	  {
	    System.out.println("Adding new element");
	  }

	if (isEditInPlace)
	  {
	    if (debug)
	      {
		System.out.println("Adding new edit in place element");
	      }

	    try
	      
	      {
		Invid invid = ((invid_field)my_field).createNewEmbedded();
		db_object object = parent.parent.getSession().edit_db_object(invid);
		containerPanel cp = new containerPanel(object,
						       my_field.isEditable() && editable,
						       parent.parent,
						       parent, container.frame);
		cp.setBorder(new LineBorder(Color.black));
		addElement(cp);
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not create new containerPanel: " + rx);
	      }

	  }
	else if (my_field instanceof date_field)
	  {
	    if (debug)
	      {
		System.out.println("Adding new date type");
	      }
	    
	    date_field datefield = (date_field)my_field;

	    try
	      {
		JdateField df = new JdateField(null,
					       true,
					       datefield.limited(),
					       datefield.minDate(),
					       datefield.maxDate(),
					       ca,
					       this);
			
		addElement(df);
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not get datefield information: " + rx);
	      }
	  }
	else if (my_field instanceof ip_field)
	  {
	    if (debug)
	      {
		System.out.println("Adding new ip vector field");
	      }

	    ip_field ipfield = (ip_field) my_field;
	    
	    try
	      {
		
		JIPField ipf = new JIPField(new JcomponentAttr(null,
							       new Font("Helvetica",Font.PLAIN,12),
							       Color.black,Color.white),
					    true,
					    ipfield.v6Allowed());
		ipf.setCallback(this);
		addElement(ipf);
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not make new ip field: " + rx);
	      }
	  }
	else if (my_field instanceof num_field) 
	  {
	    if (debug)
	      {
		System.out.println("Adding new num_field");
	      }
	    
	    try
	      {
		num_field numfield = (num_field)my_field;
		/*		  JnumberField nf = new JnumberField(10,
				  ca,
				  true,
				  numfield.limited(),
				  numfield.getMinValue(),
				  numfield.getMaxValue(),
				  this);*/
		if (debug)
		  {
		    if (numfield.limited())
		      {
			System.out.println("It is limited");
		      }
		    else
		      {
			System.out.println("NOT limited");
		      }
		  }
		JstringField sf = new JstringField(10,
						   64,
						   ca,
						   true,
						   false,
						   null,
						   null,
						   this);

						     
		  
		addElement(sf);
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not get number field information: " + rx);
	      }
	  }
	else
	  {
	    System.out.println("This type is not supported yet.");
	  }
      }
    }

  public void addElement(Component c)
  {

    addElement("Title(temp)", c);
  }
	
  /* This method is used to add an item to the vector.
   * Since on the last element on the vector has a plus
   * button, this item is going to be added to the end
   * of the vector.
   */
  public void addElement(String title, Component c)
  {
    if (c == null)
      {
	throw new IllegalArgumentException("Component parameter is null");
      }
      
    compVector.addElement(c);

    if (debug)
      {
	System.out.println("Index of element: " + compVector.size());
      }

    //Don't add the buttons for an non-editable field

    if (!centerPanelAdded)
      {
	add("Center", centerPanel);
	centerPanelAdded = true;
      }

    if (editable)
      {
	elementWrapper ew = new elementWrapper(title, c, this);
	ewHash.put(c, ew);
	centerPanel.add(ew);
      }
    else
      {
	centerPanel.add(c);
      }
    invalidate();
    //container.invalidate();
    //container.frame.validate();
    container.frame.validate_general();
  }
  
  public void deleteElement(Component c) 
  {
    if (debug)
      {
	System.out.println("Deleting element");
      }

    if (c == null)
      {
	throw new IllegalArgumentException("Component parameter is null");
      }

    if (my_field == null)
      {
	throw new RuntimeException("Error: vectorPanel.my_field is null ");
      }
      
    try
      {
	if (!my_field.isEditable())
	  {
	    return;
	  }
      }
    catch (RemoteException rx) 
      {
	throw new RuntimeException("Could not check field: " + rx);
      }

      /* If there is only one element remaining in the vector,
	 the the user should not be able to delete the element. */

    if (compVector.size() == 1)
      {
	System.err.println("You cannot delete the only element in a vector");
	return;
      }

    try
      {
	if (debug)
	  {
	    System.out.println("Deleting element number: " + compVector.indexOf(c));
	  }
	my_field.deleteElement(compVector.indexOf(c));
	compVector.removeElement(c);	  
	centerPanel.remove((elementWrapper)ewHash.get(c));
	validate();
	parent.validate();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not delete element:" + rx);
      }
  }

  public void actionPerformed(ActionEvent e)
  {
    if ((e.getSource() == addB) && editable)
      {
	addNewElement();
      }
  }

  public boolean setValuePerformed(JValueObject v)
  {
    boolean returnValue = false;
    
    if (v == null)
      {
	throw new IllegalArgumentException("ValueObject Argument is null");
      }

    if (v.getValue() == null)
      {
	return false;
      }
    else if (v.getValue().equals("plus") )
      {
	System.out.println("You clicked on a plus (no action)?");
      }
    else if (v.getValue().equals("remove") )
      {
	if (debug)
	  {
	    System.out.println("You clicked on a minus");
	  }
	deleteElement(v.getSource());
      }
    else if (v.getSource() instanceof JstringField)
      {
	if (debug)
	  {
	    System.out.println("Stringfield changed.");
	  }
	short index = (short)compVector.indexOf(v.getSource());
	System.out.println(" index = " + index);

	try
	  {
	    if (my_field instanceof string_field)
	      {
		returnValue = changeElement((String)v.getValue(), index);
	      }
	    else if (my_field instanceof num_field)
	      {
		try
		  {
		    returnValue = changeElement( new Integer((String)v.getValue()), index);
		  }
		catch (NumberFormatException ex)
		  {
		    System.err.println("That's not a number!");
		    returnValue = false;
		  }
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not set value: " + rx);
	  }
      }
    else if (v.getSource() instanceof JdateField)
      {
	if (debug)
	  {
	    System.out.println("Date field changed");
	  }

	short index = (short)compVector.indexOf(v.getSource());
	System.out.println(" index = " + index);

	try
	  {
	    returnValue = changeElement((Date)v.getValue(), index);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not set value of date field: " + rx);
	  }
      }
    else if (v.getSource() instanceof JIPField)
      {
	if (debug)
	  {
	    System.out.println("IP field changed");
	  }

	short index = (short)compVector.indexOf(v.getSource());
	System.out.println(" index = " + index);

	try
	  {
	    returnValue = changeElement((Byte[])v.getValue(), index);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not set value of date field: " + rx);
	  }
      }
    else
      {
	System.out.println("Value changed in field that is not yet supported");
      }

    if (returnValue)
      {
	parent.parent.somethingChanged = true;
      }

    return returnValue;
  }
	
  /*
   * This changes an element after a ValueCallback.
   */
  public boolean changeElement(Object obj, short index) throws RemoteException
  {
    if (index >= my_field.size())
      {
	System.out.println("Adding new element");

	if (my_field.addElement(obj))
	  {
	    System.out.println("Add Element returned true");
	    System.out.println("There are now " + my_field.size() + " elements in the field");
	    return true;
	  }
	else
	  {
	    System.out.println("Add Element returned false");
	    System.out.println(" here's why: " + parent.getgclient().getSession().getLastError());
	    return false;
	  }
      }
    else
      {
	System.out.println("Changing element " + index);

	if (my_field.setElement(index, obj))
	  {
	    System.out.println("set Element returned true");
	    return true;
	  }
	else
	  {
	    System.out.println("set Element returned false");
	    System.out.println(" here's why: " + parent.getgclient().getSession().getLastError());
	    return false;
	  }
      }	    
  }
  
  public Session getSession() 
  {
    return null;
  }
  /*
  public void doLayout()
  {
    super.doLayout();
    setSize(getPreferredSize().width, getPreferredSize().height);
  }*/

  /**
   * Override validate to reset the current size.
   */
  /*
  public void validate()
  {
    //System.out.println("validate in vp: " + whereAmI());
    super.validate();
    if (getParent() != null)
      {
	getParent().validate();
      }
    container.validate();
  }
  
  public void invalidate()
  {
    //System.out.println("invalidate in vp: " + whereAmI());
    System.out.println("inv in vp: parent ->" + getParent());
    super.invalidate();
  }
  */
  public String whereAmI()
  {
    if (getParent() instanceof containerPanel)
      {
	return ((containerPanel)getParent()).whereAmI() + "/v";
      }
    else if (getParent() instanceof vectorPanel)
      {
	return ((vectorPanel)getParent()).whereAmI() + "/v";
      }
    else if (getParent() instanceof JPanel)
      {
	return ((vectorPanel)((JPanel)getParent()).getParent()).whereAmI() + "/p/v";
      }
    else if (getParent() instanceof elementWrapper)
      {
	return ((elementWrapper)getParent()).whereAmI() + "/v";
      }
    else 
      {
	System.out.println("What kind of parent is this: " + getParent());
      }
    return "/v";
  }

}


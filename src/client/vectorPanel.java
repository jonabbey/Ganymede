
/*
  GASH 2

  vectorPanel.java

  This module provides for a generic vector of objects, and can be
  used to implement a collection of date fields, i.p. addresses,
  or edit in place (composite) objects.

  Created: 17 Oct 1996
  Version: $Revision: 1.9 $ %D%
  Module By: Navin Manohar
  Applied Research Laboratories, The University of Texas at Austin
*/


package arlut.csd.ganymede.client;

import arlut.csd.ganymede.client.*;
import arlut.csd.ganymede.*;

import java.awt.event.*;
import java.awt.*;
import gjt.*;

import arlut.csd.JDataComponent.*;
import java.util.*;
import java.rmi.*;
import java.net.*;

import jdj.PackageResources;

import com.sun.java.swing.*;

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

public class vectorPanel extends JBufferedPane implements JsetValueCallback, ActionListener {

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

  JBufferedPane
    bottomPanel,
    centerPanel;

  boolean 
    editable,
    isEditInPlace;

  private db_field my_field;

  private windowPanel parent;

  /* -- */
  
  /**
   *
   *
   */

  public vectorPanel(db_field field, windowPanel parent, boolean editable, boolean isEditInPlace)
  {
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

    bottomPanel = new JBufferedPane();
    bottomPanel.setLayout(new BorderLayout());
    centerPanel = new JBufferedPane();

    centerPanel.setLayout(new ColumnLayout(Orientation.LEFT,Orientation.TOP));

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

    JBufferedPane main = new JBufferedPane();
    main.setLayout(new BorderLayout());
    main.setBorderStyle(2);
    main.add("South", bottomPanel);
    main.add("Center", centerPanel);
    
    //scrollPane = new JScrollPane();
    //scrollPane.setViewportView(main);
    //add(scrollPane);

    add(main);

    compVector = new Vector();
    ewHash = new Hashtable();

    this.parent = parent;
    
    createVectorComponents();
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
	System.out.println("Adding date vector field");

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
	System.out.println("Adding vector number field");

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
	System.out.println("Adding vector string field");

	try
	  {
	    string_field stringfield = (string_field)my_field;
	    
	    if (editable && stringfield.isEditable())
	      {
		if (stringfield.size() > 0)
		  {
		    for (int i=0; i < stringfield.size(); i++) 
		      {
			if (isEditInPlace)
			  {
			    /*
			    containerPanel cp = new containerPanel(stringfield, 
								   stringfield.isEditable() && editable, 
								   parent.parent,
								   parent);
			    addElement(cp);
			    */
			  }
			else
			  {
			    
			    JstringField sf = new JstringField(stringfield.maxSize() <= 12 ? 12 : stringfield.maxSize(),
							       stringfield.maxSize() <= 12 ? 12 : stringfield.maxSize(),
							       ca,
							       stringfield.isEditable(),
							       !(stringfield.showEcho()),
							       stringfield.allowedChars(),
							       stringfield.disallowedChars(),
							       this);
			    System.out.println("Setting text");
			    sf.setText((String)(stringfield.getElement(i)));
			    
			    addElement(sf);
			  }
		      }
		  }
		else
		  {
		    System.out.println("No objects in vector");
		  } 
	      }
	    else // Not editable
	      {
		if (stringfield.size() > 0)
		  {
		    Vector strings = new Vector();;

		    for (int i=0;i<stringfield.size();i++) 
		      {
			strings.addElement(stringfield.getElement(i));
		      }

		    JListBox list = new JListBox(strings);
		    centerPanel.add(list);
		  }
		else
		  {
		    System.out.println("No objects in vector");
		  } 
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't make checkbox field: " + rx);
	  }
      }
    else if (my_field instanceof invid_field)
      {
	System.out.println("Adding vector invid_field");

	try
	  {
	    invid_field invidfield = (invid_field)my_field;

	    if (isEditInPlace)
	      {
		System.out.println("Adding edit in place invid vector");

		for (int i=0;i<invidfield.size();i++)
		  {
		    Invid inv = (Invid)(invidfield.getElement(i));
		    db_object object = parent.getgclient().getSession().edit_db_object(inv);
		    containerPanel cp = new containerPanel(object,
							   invidfield.isEditable() && editable,
							   parent.parent,
							   parent);
		  }
	      }
	    else
	      {
		System.out.println("Adding invid vector");

		if (editable)
		  {
		    StringBuffer sb = invidfield.choices();

		    System.out.println("got choice buffer: " + sb.toString());

		    Vector choices = gclient.parseDump(sb);

		    System.out.println("got " + choices.size() + " choices to load");
		  }

		for (int i=0; i < invidfield.size() ;i++) 
		  {
		    Invid inv = (Invid)(invidfield.getElement(i));

		    // Add a series of JChoices
		    
		    JComboBox choice = new JComboBox();

		    choice.addPossibleValue(inv.toString());

		    if (editable)
		      {
			for (int j=0; j< choices.size(); j++)
			  {
			    Result result = (Result) choices.elementAt(j);
			    choice.addPossibleValue(result.toString());
			  }
		      }

		    elementWrapper ew = new elementWrapper(choice, this);
		  }
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
	    System.out.println("Adding add button");
	    bottomPanel.add("Center", addB);
	  }
	else
	  {
	    System.out.println("Field is not editable, no button added");
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
	System.out.println("Adding new element");
	  
	if (isEditInPlace)
	  {
	    System.out.println("Adding new edit in place element");

	    try
	      {
		short type = my_field.getType();
		db_object object = parent.getgclient().getSession().create_db_object(type);
		containerPanel cp = new containerPanel(object,
						       my_field.isEditable() && editable,
						       parent.parent,
						       parent);
		addElement(cp);
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not create new containerPanel: " + rx);
	      }

	  }
	else if (my_field instanceof string_field)
	  {
	    System.out.println("Adding new string type");

	    try
	      {
		JstringField sf = new JstringField(((string_field)my_field).maxSize() <= 12 ? 12 : ((string_field)my_field).maxSize(),
						   ((string_field)my_field).maxSize() <= 12 ? 12 : ((string_field)my_field).maxSize(),
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
		throw new RuntimeException("Could not get field size: " + rx);
	      }
	  }
	else if (my_field instanceof date_field)
	  {
	    System.out.println("Adding new date type");

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
	else if (my_field instanceof num_field) 
	  {
	    System.out.println("Adding new num_field");

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
		if (numfield.limited())
		  {
		    System.out.println("It is limited");
		  }
		else
		  {
		    System.out.println("NOT limited");
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

	
  /* This method is used to add an item to the vector.
   * Since on the last element on the vector has a plus
   * button, this item is going to be added to the end
   * of the vector.
   */
  public void addElement(Component c)
  {
    if (c == null)
      {
	throw new IllegalArgumentException("Component parameter is null");
      }
      
    compVector.addElement(c);

    System.out.println("Index of element: " + compVector.size());
      
    //Don't add the buttons for an non-editable field

    if (editable)
      {
	elementWrapper ew = new elementWrapper(c, this);
	ewHash.put(c, ew);
	centerPanel.add(ew);
      }
    else
      {
	centerPanel.add(c);
      }
    validate();
    parent.validate();
  }
  
  public void deleteElement(Component c) 
  {
    System.out.println("Deleting element");

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
	System.out.println("Deleting element number: " + compVector.indexOf(c));
	my_field.deleteElement(compVector.indexOf(c));
	compVector.removeElement(c);	  
	centerPanel.remove((elementWrapper)ewHash.get(c));
	invalidate();
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
    
    System.out.println();
    
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
	System.out.println("You clicked on a plus");
      }
    else if (v.getValue().equals("minus") )
      {
	System.out.println("You clicked on a minus");
	deleteElement(v.getSource());
      }
    else if (v.getSource() instanceof JstringField)
      {
	System.out.println("Stringfield changed.");
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
	System.out.println("Date field changed");
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
}

  /////////////////////////////////////////////////////////////////////////

  /**
   *  This class will be used as a wrapper for each of the elements in the
   *  vector.  It contains plus and minus buttons that will allow a
   *  component to be deleted or a component to be added to the vector being
   *  displayed.
   *
   */ 

class elementWrapper extends JBufferedPane implements ActionListener {

  // class variables

  private Component my_component = null;
  
  JBufferedPane 
    buttonPanel;

  JButton 
    minus;

  Image
    removeImage;

  vectorPanel
    parent;

  // class methods

  public elementWrapper(Component comp, vectorPanel parent)
  {
    System.out.println("Adding new elementWrapper");

    if (comp == null) 
      {
	throw new IllegalArgumentException("Error: Component parameter is null");
      }

    this.parent = parent;
    
    removeImage = PackageResources.getImageResource(this, "trash.gif", getClass());

    setLayout(new BorderLayout());
      
    buttonPanel = new JBufferedPane();
      
    buttonPanel.setLayout(new BorderLayout());
      
    minus = new JButton(new ImageIcon(removeImage));
    minus.setPad(new Insets(0,0,0,0));
    //minus = new JButton("X");
    minus.addActionListener(this);
      
    buttonPanel.add("Center",minus);
      
    my_component = comp;
      
    add("Center",comp);
    add("East",buttonPanel);
  }

  public Component getComponent() 
  {
    return my_component;
  }


  public void actionPerformed(ActionEvent evt) 
  {
    if (evt.getSource() == minus) 
      {
	JValueObject v = new JValueObject(getComponent(),"minus");
	parent.setValuePerformed(v);
      }
    else
      {
	throw new RuntimeException("actionPerformed invoked by ActionEvent from invalid source");
      }
  }
}





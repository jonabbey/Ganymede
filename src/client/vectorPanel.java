
/*

   vectorPanel.java

   Created: 17 Oct 1996
   Version: $Revision: 1.2 $ %D%
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

import com.sun.java.swing.*;

/**
 *
 *
 *
 *
 *
 */
public class vectorPanel extends JPanel implements JsetValueCallback, ActionListener {

  // class variables

  private Vector compVector = new Vector(5,1);

  JButton
    addB;
  
  Vector 
    choices = null;

  short 
    type;

  JPanel
    bottomPanel,
    centerPanel;

  private db_field my_field;

  private windowPanel parent;

  static JcomponentAttr ca = new JcomponentAttr(null,new Font("Helvetica",Font.PLAIN,12),Color.black,Color.white);
  
  /**
   *
   *
   */
  public vectorPanel(db_field field, windowPanel parent)
  {

    try
      {
	if (field == null)
	  throw new IllegalArgumentException("Illegal Argument: handle to field is null");
	
	if (!field.isVector())
	  throw new IllegalArgumentException("Invalid field type: attempt to populate a vectorPanel with a non-vector field");
	
	if (field.size() < 0)
	  throw new IllegalArgumentException("Error: vector field length has a negative value");
	
	if (parent == null)
	  throw new IllegalArgumentException("Error: ");
	
	


	my_field = field;
	compVector.ensureCapacity(field.size()+1);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not init vectorPanel: " + rx);
      }

    bottomPanel = new JPanel();
    centerPanel = new JPanel();

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
	if (my_field.isEditable())
	  {
	    addB.addActionListener(this);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Can't check if field is editable: " + rx);
      }
    setLayout(new BorderLayout());
    add("South", bottomPanel);
    add("Center", centerPanel);
    

    this.parent = parent;

    createVectorComponents();
  }


  
  private void createVectorComponents()
  {

    if (my_field == null)
      throw new RuntimeException("Error: my_field is null -- cannot create components");
    try
      {
	if (!my_field.isVector())
	  throw new RuntimeException("Error: my_field is non-vector field");
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Can't tell if field is vector" + rx);
      }
    /*    if (compVector.size() > 0) {

      removeAllElements();
    }
    */

    
    if (my_field instanceof boolean_field)
      {
	System.out.println("Adding boolean vector panel");
	boolean_field  boolfield = (boolean_field)my_field;

	boolean labeled = false;
	try
	  {
	    labeled = boolfield.labeled();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Can't access boolfield: " + rx);
	  }
	if (labeled)
	  {
	    // This requires a checkboxgroupField
	    System.err.println("Can't handle labeled booleans yet!");
	    /*		
	    String choices[] = new String[2];

	    choices[0] = boolfield.trueLabel();
	    choices[1] = boolfield.falseLabel();
	    
	    
	    for (int i=0;i<boolfield.size();i++) {


	      
	      checkboxgroupField cbg = new checkboxgroupField(ca,
							      choices,
							      boolfield.isEditable(),
							      this);
	      
	      // This method needs to be added to the checkboxgroupField
	      //cbg.setCheckbox(choices[0]);
	      
	      



	      elementWrapper ew = new elementWrapper(cbg, parent);
	      compVector.addElement(ew);
	      add(ew);
	    }
	    */
	  }
	else 
	  {
	    // this requires a checkboxField
	    try
	      {	    
		for (int i=0;i<boolfield.size();i++)
		  {
		    
		    JCheckbox cb = new JCheckbox();
		    cb.setSelected(((Boolean)boolfield.getElement(i)).booleanValue());
		    cb.setEnabled(boolfield.isEditable());
		    
		    
		    
		    
		    
		    elementWrapper ew = new elementWrapper(cb,this);
		    compVector.addElement(ew);
		    centerPanel.add(ew);
		  }
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("Can't make checkbox field: " + rx);
	      }
	  }
      }
    
      
    else if (my_field instanceof date_field)
      {
	System.out.println("Adding date vector field");
	try
	  {

	    date_field datefield = (date_field)my_field;
	    
	    for (int i=0;i<datefield.size();i++) 
	      {
		
		JdateField df = new JdateField((Date)(datefield.getElement(i)),
					       datefield.isEditable(),
					       datefield.limited(),
					       datefield.minDate(),
					       datefield.maxDate(),
					       ca,
					       this);

		elementWrapper ew = new elementWrapper(df, this);
		compVector.addElement(ew);
		centerPanel.add(ew);
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
		  JnumberField nf = new JnumberField(numberField.DEFAULT_COLS,
		  ca,
		  numfield.isEditable(),
		  numfield.limited(),
		  numfield.getMinValue(),
		  numfield.getMaxValue(),
		  this);
		  */
		JnumberField nf = new JnumberField();
		
		elementWrapper ew = new elementWrapper(nf, this);
		compVector.addElement(ew);	      
		centerPanel.add(ew);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't make checkbox field: " + rx);
	  }
	
      }
    else if (my_field instanceof string_field)
      {
	System.out.println("Adding vector string field");
	try
	  {
	    string_field stringfield = (string_field)my_field;
	    
	    if (stringfield.size() > 0)
	      {
		for (int i=0;i<stringfield.size();i++) 
		  {
		    
		    JstringField sf = new JstringField(20,
						       20,
						       ca,
						       stringfield.isEditable(),
						       !(stringfield.showEcho()),
						       stringfield.allowedChars(),
						       stringfield.disallowedChars(),
						       this);
		    
		    sf.setText((String)(stringfield.getElement(i)));
		    
		    addElement(sf);
		    
		    //elementWrapper ew = new elementWrapper(sf, this);
		    //compVector.addElement(ew);	      
		    //add(ew);
		  }
	      }
	    else
	      {
		System.out.println("No objects in vector");
	      } 
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't make checkbox field: " + rx);
	  }
	
      }
    //else if (my_field instanceof password_field)
    //{

    //}
    else if (my_field instanceof invid_field)
      {
	System.out.println("Adding vector invid_field");
	try
	  {
	    invid_field invidfield = (invid_field)my_field;
	    
	    for (int i=0;i<invidfield.size();i++) 
	      {
		
		Invid inv = (Invid)(invidfield.getElement(i));
		
		JstringField sf = new JstringField();
	  /*
	  JstringField sf = new JstringField(stringField.DEFAULT_COLS,
					   stringField.DEFAULT_COLS,
					   ca,
					   invidfield.isEditable(),
					   false,
					   null,
					   null,
					   this);
					   */
		if (inv == null)
		  sf.setText("");
		else 
		  sf.setText(inv.toString());
		
		elementWrapper ew = new elementWrapper(sf, this);
		compVector.addElement(ew);
		add(ew);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't make checkbox field: " + rx);
	  }
	
      }
    try
      {
	if (my_field.isEditable())
	  {
	    bottomPanel.add("Center", addB);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Can't check if field is editable: " + rx);
      }
  } 
  public void addNewElement()
    {
      System.out.println("Add new element");
      //if (type == FieldType.STRING)
      if (my_field instanceof string_field)
	{
	  System.out.println("Adding new string type");
	  		    
	  JstringField sf = new JstringField(20,
					     20,
					     ca,
					     true,
					     false,
					     null,
					     null,
					     this);
		    
	  addElement(sf);



	}
      else
	{
	  System.out.println("This type is not supported yet.");
	}
    }
   
  public void actionPerformed(ActionEvent e)
    {
      if (e.getSource() == addB)
	{
	  addNewElement();
	}
    }

  public boolean setValuePerformed(JValueObject v) {

    if (v == null)
      {
	throw new IllegalArgumentException("ValueObject Argument is null");
      }
    
    if (v.getValue().equals("plus") )
      {
	
	System.out.println("You clicked on a plus");
	if (v.getSource() instanceof JstringField)
	  {
	    ((JstringField)v.getSource()).setText("plus");
	  }
	else
	  {
	    System.out.println("Whoa, what source is this plus from?");
	  }
      }
    else if (v.getValue().equals("minus") )
      {
	System.out.println("You clicked on a minus");
	if (v.getSource() instanceof JstringField)
	  {
	    ((JstringField)v.getSource()).setText("minus");
	  }
	else
	  {
	    System.out.println("Whoa, what source is this minus from?");
	  }
      }
    else
      {
	
	
      }
    return false;
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

      elementWrapper ew = new elementWrapper(c, this);
      
      if (! compVector.isEmpty())
	{
	  elementWrapper last = (elementWrapper)(compVector.lastElement());
	  
	  if (last != null)
	    {
	      //last.removePlusButton();
	    }
	}
      
      //ew.addPlusButton();
      compVector.addElement(ew);
      centerPanel.add(ew);
      
      invalidate();

      parent.validate();
    }
  
  public void deleteElement(Component c) 
    {
      
      if (c == null)
	throw new IllegalArgumentException("Component parameter is null");
      
      int index = compVector.indexOf(c);
      
      if (index < 0)
	throw new RuntimeException("Error: vectorPanel.processEvent(Object,Component) called with invalid component");
      
      if (my_field == null)
	throw new RuntimeException("Error: vectorPanel.my_field is null ");
      
      try
	{
	  if (!my_field.isEditable())
	    return;
	}
      catch (RemoteException rx) 
	{
	  throw new RuntimeException("Could not check field: " + rx);
	}
      /* If there is only one element remaining in the vector,
	 the the user should not be able to delete the element. */
      if (compVector.size() == 1)
	return;
      
      if (compVector.indexOf(c) == compVector.size()-1)
	{
	  elementWrapper ew = (elementWrapper)compVector.elementAt(compVector.indexOf(c)-1);
	  
	  //ew.addPlusButton();
	}
      
      
      compVector.removeElement(c);
      
      remove(c);
      
      invalidate();
      parent.validate();
      
    }
  
  public Session getSession() 
    {
      return null;
    }
}

  /////////////////////////////////////////////////////////////////////////

  /**
   *  This class will be used as a wrapper for each of the elements in the
   *   vector.  It contians plus and minus buttons that will allow a
   *   component to be deleted or a component to be added to the vector being
   *  displayed.
   */ 
class elementWrapper extends JPanel implements ActionListener {

  // class variables

  private Component my_component = null;
  
  JPanel 
    buttonPanel;

  JButton 
  //    plus,
    minus;

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
    
      setLayout(new BorderLayout());
      
      buttonPanel = new JPanel();
      
      buttonPanel.setLayout(new BorderLayout());
      
      minus = new JButton("remove");
      minus.addActionListener(this);
      
      buttonPanel.add("Center",minus);
      
      my_component = comp;
      
      add("Center",comp);
      add("East",buttonPanel);
    }

  /**
   *
   *
   */
  public Component getComponent() 
    {
      return my_component;
    }


  /**
   *
   *
   *
  public void removePlusButton() {

    if (plus == null)
      {
	return;
      }
    
    remove(plus);
    plus = null;
    repaint();
  }
  */

  /**
   *
   *
   *
  public void addPlusButton() {

    if (plus != null && plus.isVisible())
      {
	return;
      }
    
    plus = new JButton("+");
    plus.addActionListener(this);

    buttonPanel.add("North",plus);
    repaint();
  }
  */
  /**
   *
   *
   */
  public void actionPerformed(ActionEvent evt) {

    /*    if (evt.getSource() == plus) 
      {
	JValueObject v = new JValueObject(getComponent(),"plus");
	parent.setValuePerformed(v);	
      }*/
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






/*

   vectorPanel.java

   Created: 17 Oct 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin
*/


package arlut.csd.ganymede.client;

import arlut.csd.ganymede.client.*;
import arlut.csd.ganymede.*;

import java.awt.event.*;
import java.awt.*;
import gjt.*;
import arlut.csd.DataComponent.*;
import java.util.*;
import java.rmi.*;

/**
 *
 *
 *
 *
 *
 */
public class vectorPanel extends Panel implements setValueCallback {

  // class variables

  private Vector compVector = new Vector(5,1);

  private db_field my_field;

  private containerInterface my_parent;

  static componentAttr ca = new componentAttr(null,new Font("Helvetica",Font.PLAIN,12),Color.black,Color.white);
  
  /**
   *
   *
   */
  public vectorPanel(db_field field,containerInterface parent) throws RemoteException
  {
  
    if (field == null)
      throw new IllegalArgumentException("Illegal Argument: handle to field is null");
    
    if (!field.isVector())
      throw new IllegalArgumentException("Invalid field type: attempt to populate a vectorPanel with a non-vector field");
    
    if (field.length() < 0)
      throw new IllegalArgumentException("Error: vector field length has a negative value");
    
    if (parent == null)
      throw new IllegalArgumentException("Error: ");


    setLayout(new ColumnLayout(Orientation.LEFT,Orientation.TOP));

    my_field = field;

    my_parent = parent;

    compVector.ensureCapacity(field.length()+1);

    createVectorComponents();
  }


  
  private void createVectorComponents() throws RemoteException
  {

    if (my_field == null)
      throw new RuntimeException("Error: my_field is null -- cannot create components");

    if (!my_field.isVector())
      throw new RuntimeException("Error: my_field is non-vector field");

    /*    if (compVector.size() > 0) {

      removeAllElements();
    }
    */

    
    if (my_field instanceof boolean_field)
      {
	boolean_field  boolfield = (boolean_field)my_field;
	
	if (boolfield.labeled())
	  {
	    // This requires a checkboxgroupField
		
	    String choices[] = new String[2];

	    choices[0] = boolfield.trueLabel();
	    choices[1] = boolfield.falseLabel();
	    
	    
	    for (int i=0;i<boolfield.length();i++) {


	      
	      checkboxgroupField cbg = new checkboxgroupField(ca,
							      choices,
							      boolfield.isEditable(),
							      this);
	      
	      // This method needs to be added to the checkboxgroupField
	      //cbg.setCheckbox(choices[0]);
	      
	      



	      elementWrapper ew = new elementWrapper(cbg);
	      compVector.addElement(ew);
	      add(ew);
	    }
	  }
	else 
	  {
	    // this requires a checkboxField
	    
	    for (int i=0;i<boolfield.length();i++) {
	      
	      checkboxField cb = new checkboxField(null,((Boolean)(boolfield.getElement(i))).booleanValue(),
						   ca,boolfield.isEditable(),this);
	      
	      
	      
	      
	      
	      elementWrapper ew = new elementWrapper(cb);
	      compVector.addElement(ew);
	      add(ew);
	    }
	  }
	
      }
    else if (my_field instanceof date_field)
      {
	date_field datefield = (date_field)my_field;
	
	for (int i=0;i<datefield.length();i++) {
	  
	  dateField df = new dateField((Date)(datefield.getElement(i)),
				       datefield.isEditable(),
				       datefield.limited(),
				       datefield.minDate(),
				       datefield.maxDate(),
				       ca,
				       this);




	  elementWrapper ew = new elementWrapper(df);
	  compVector.addElement(ew);
	  add(ew);
	}
      }
    else if (my_field instanceof num_field)
      {
	num_field numfield = (num_field)my_field;
	    
	for (int i=0;i<numfield.length();i++) {
	      
	      
	  numberField nf = new numberField(numberField.DEFAULT_COLS,
					   ca,
					   numfield.isEditable(),
					   numfield.limited(),
					   numfield.getMinValue(),
					   numfield.getMaxValue(),
					   this);


	  elementWrapper ew = new elementWrapper(nf);
	  compVector.addElement(ew);	      
	  add(ew);
	}
      }
    else if (my_field instanceof string_field)
      {
	string_field stringfield = (string_field)my_field;


	for (int i=0;i<stringfield.length();i++) {
	  
	  stringField sf = new stringField(stringField.DEFAULT_COLS,
					   stringField.DEFAULT_COLS,
					   ca,
					   stringfield.isEditable(),
					   !(stringfield.showEcho()),
					   stringfield.allowedChars(),
					   stringfield.disallowedChars(),
					   this);
	  sf.setText((String)(stringfield.getElement(i)));
	      


	  elementWrapper ew = new elementWrapper(sf);
	  compVector.addElement(ew);	      
	  add(ew);
	}
      }
    else if (my_field instanceof invid_field)
      {
	invid_field invidfield = (invid_field)my_field;
	    
	for (int i=0;i<invidfield.length();i++) {
	  
	  Invid inv = (Invid)(invidfield.getElement(i));
	    

	  stringField sf = new stringField(stringField.DEFAULT_COLS,
					   stringField.DEFAULT_COLS,
					   ca,
					   invidfield.isEditable(),
					   false,
					   null,
					   null,
					   this);
	  if (inv == null)
	    sf.setText("");
	  else 
	    sf.setText(inv.toString());
	    
	  elementWrapper ew = new elementWrapper(sf);
	  compVector.addElement(ew);
	  add(ew);
	}
      }
    /*    else if (my_field instanceof einvid_field)
      {
	einvid_field einvidfield = (einvid_field)my_field;

	for (int i=0;i<einvidfield.length();i++) {

	  Invid inv = (Invid)(einvidfield.getElement(i));
	  
	  if (inv == null)
	    throw new RuntimeException("Error: Invid object is null");
	  
	  data_object dbobj = null;
	    
	  Session s = my_parent.getSession();

	  dbobj = s.view_db_object(inv);
	    
	  containerPanel cP = new containerPanel(dbobj,s);
	    
	  elementWrapper ew = new elementWrapper(cP);
	  compVector.addElement(ew);
	  add(ew);
	}
      }
      */
  } 
   


  public boolean setValuePerformed(ValueObject vobj) {

    if (vobj == null)
      {
	throw new IllegalArgumentException("ValueObject Argument is null");
      }
    
    if (vobj.getValue.equals("plus") )
      {
	
	// A plus button has been pushed, so another element must be added to the vector
	
      }
    else if (vobj.getValue.equals("minus") )
      {
	// a minus button has been pushed, so the element which contained the minus button must
	// be removed from the vector.
	
	
      }
    else
      {
	
	
      }
  }
	
	
	
  /**
   *
   *
   *

  public void processEvent(Object obj,Component comp) throws RemoteException {

    int index = compVector.indexOf(comp);

    if (index < 0)
      throw new RuntimeException("Error: vectorPanel.processEvent(Object,Component) called with invalid component");

    if (my_field == null)
      throw new RuntimeException("Error: vectorPanel.my_field is null ");

    if (!my_field.isEditable())
      return;

    (my_field.setElement(index,obj);
  }
   */

    /* This method is used to add an item to the vector.
     * Since on the last element on the vector has a plus
     * button, this item is going to be added to the end
     * of the vector.
     */
    public void addElement(Component c) {
      
      if (c == null)
	throw new IllegalArgumentException("Component parameter is null");
      
      elementWrapper ew = new elementWrapper(c);

      elementWrapper last = (elementWrapper)(compVector.lastElement());

      if (last != null)
	last.removePlusButton();
      
      ew.addPlusButton();
      add(ew);

      invalidate();

      getParent().validate();
    }
  
  public void deleteElement(Component c) {

    if (c == null)
      throw new IllegalArgumentException("Component parameter is null");
    
    int index = compVector.indexOf(c);
    
    if (index < 0)
      throw new RuntimeException("Error: vectorPanel.processEvent(Object,Component) called with invalid component");

    if (my_field == null)
      throw new RuntimeException("Error: vectorPanel.my_field is null ");

    if (!my_field.isEditable())
      return;
    
    /* If there is only one element remaining in the vector,
       the the user should not be able to delete the element. */
    if (compVector.length == 1)
      return;

    if (compVector.indexOf(c) == compVector.length()-1)
      {
	elementWrapper ew = compVector.getElementAt(compVector.indexOf(c)-1);

	ew.addPlusButton();
      }


    compVector.removeElement(c);

    remove(c);
    
    invalidate();
    getParent().validate();

  }

  public Session getSession() {
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
class elementWrapper extends Panel implements ActionListener {

  // class variables

  private Component my_component = null;
  
  private Panel buttonPanel;
  private Button plus;
  private Button minus;
  private setValueCallback _parent;

  // class methods

  /**
   *
   *
   *
   */
  public elementWrapper(Component comp,setValueCallback parent)
  {
    if (comp == null) 
      throw new IllegalArgumentException("Error: Component parameter is null");

    
    setLayout(new BorderLayout());

    buttonPanel = new Panel();

    buttonPanel.setLayout(new BorderLayout());

    minus = new Button("-");

    buttonPanel.add("South",minus);

    my_component = comp;

    add("West",comp);
    add("East",buttonPanel);
  }

  /**
   *
   *
   */
  public Component getComponent() {

    return my_component;

  }

  /**
   *
   *
   */
  public void removePlusButton() {

    if (plus == null)
      return;

    remove(plus);
    plus = null;
    repaint();
  }

  /**
   *
   *
   */
  public void addPlusButton() {

    if (plus != null && plus.isVisible())
      return;

    plus = new Button("+");

    buttonPanel.add("North",plus);
    repaint();
  }

  /**
   *
   *
   */
  public void actionPerformed(ActionEvent evt) {

    if (evt.getSource() == plus) {

      ValueObject v = new ValueObject(this,"plus");

      my_parent.setValuePerformed(v);

    }
    else if (evt.getSource() == minus) {

      ValueObject v = new ValueObject(this,"minus");

      my_parent.setValuePerformed(v);
    }

    else {

      throw new RuntimeException("actionPerformed invoked by ActionEvent from invalid source");
    }
  }
}





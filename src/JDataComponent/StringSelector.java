/*
   StringSelector.java

   Created: 10 October 1997
   Version: 1.10 98/01/20
   Module By: Mike Mulvaney, Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin
*/

package arlut.csd.JDataComponent;

import java.awt.event.*;
import java.awt.*;

import java.util.*;
import java.rmi.*;
import java.net.*;

import jdj.PackageResources;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

import arlut.csd.JTable.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 StringSelector


------------------------------------------------------------------------------*/

/**
 *
 * A two list box for adding strings to lists.
 *
 */

public class StringSelector extends JPanel implements ActionListener, JsetValueCallback {

  static final boolean debug = true;

  // --

  JsetValueCallback
    my_parent;

  JButton
    add,
    remove;

  JstringListBox
    in, 
    out = null;

  JPanel
    inPanel = new JPanel(),
    outPanel = new JPanel();

  JButton
    addCustom;

  JPanel
    lists;

  boolean
    allowCallback = false;

  JstringField 
    custom = null;

  Container
    parent;

  private boolean
    editable,
    canChoose,
    mustChoose;

  BevelBorder
    bborder = new BevelBorder(BevelBorder.RAISED);

  LineBorder
    lborder = new LineBorder(Color.black);

  Vector
    inVector = new Vector(),
    outVector = new Vector();
  
  int rowWidth;

  /**
   *
   * Constructor for StringSelector
   *
   * @param available Vector of listHandles for choices that are available
   * but are not currently in the set of selected values
   *
   * @param chosen Vector of listHandles for available choices
   *
   * @param parent AWT container that the StringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param canChoose Choice must be made from vector of choices
   * @param mustChoose Vector of choices is available
   */

  public StringSelector(Vector available, Vector chosen, Container parent, 
			boolean editable, boolean canChoose, boolean mustChoose, int rowWidth)
  {
    this(available, chosen, parent, editable, canChoose, mustChoose, rowWidth, "Selected", "Available", null, null);
  }

  public StringSelector(Vector available, Vector chosen, Container parent, 
			boolean editable, boolean canChoose, boolean mustChoose)
  {
    this(available, chosen, parent, editable, canChoose, mustChoose, 10, "Selected", "Available", null, null);
  }

  /**
   *
   * Constructor for StringSelector
   *
   * @param available Vector of listHandles for choices that are available
   * but are not currently in the set of selected values
   *
   * @param chosen Vector of listHandles for available choices
   *
   * @param parent AWT container that the StringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param inLabel Label for the list of selected choices
   * @param outLabel Label for the list of available choices
   */

  public StringSelector(Vector available, Vector chosen, Container parent, 
			boolean editable, int rowWidth,
			 String inLabel, String outLabel)
  {
    this(available, chosen, parent, editable, true, false, rowWidth, inLabel, outLabel, null, null);
  }

  /**
   *
   * Constructor for StringSelector
   *
   * @param available Vector of listHandles for choices that are available
   * but are not currently in the set of selected values
   *
   * @param chosen Vector of listHandles for available choices
   *
   * @param parent AWT container that the StringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param rowWidth How many columns wide should each box be?
   *
   */
  
  public StringSelector(Vector available, Vector chosen, Container parent, boolean editable, int rowWidth)
  {
    this(available, chosen, parent, editable, (available != null), false, rowWidth);
  }

  /**
   *
   * Fully specified Constructor for StringSelector
   *
   * @param available Vector of listHandles for choices that are available
   * but are not currently in the set of selected values
   *
   * @param chosen Vector of listHandles for available choices
   *
   * @param parent AWT container that the StringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param canChoose Choice must be made from vector of choices
   * @param mustChoose Vector of choices is available
   * @param rowWidth How many columns wide should each box be?
   * @param inLabel Label for the list of selected choices
   * @param outLabel Label for the list of available choices
   * @param inPopup Popup Menu for in table
   * @param outPopup PopupMenu for out table
   */

  public StringSelector(Vector available, Vector chosen, Container parent, 
			 boolean editable, boolean canChoose, boolean mustChoose, int rowWidth,
			 String inLabel, String outLabel, JPopupMenu inPopup, JPopupMenu outPopup)
  {
    if (debug)
      {
	System.out.println("-Adding new StringSelector-");
      }
    
    setBorder(new com.sun.java.swing.border.EtchedBorder());

    this.parent = parent;
    this.editable = editable;
    this.canChoose = canChoose;
    this.mustChoose = mustChoose;
    this.rowWidth = rowWidth;
    
    //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    setLayout(new BorderLayout());

    // lists holds the outPanel and inPanel.

    lists = new JPanel();
    //lists.setLayout(new BoxLayout(lists, BoxLayout.X_AXIS));
    lists.setLayout(new GridLayout(1,2));

    // Set up the inPanel, which holds the in list and button


    // chosen is a vector of listhandles or Strings.  If it is strings,
    // create a vector of listhandles.
    if ((chosen != null) && (chosen.size() > 0))
      {
	if ((chosen.elementAt(0) == null) || (chosen.elementAt(0) instanceof listHandle))
	  {
	    inVector = chosen;
	  }
	else
	  {
	    String label;
	    for (int i = 0; i < chosen.size(); i++)
	      {
		label = (String) chosen.elementAt(i);
		inVector.addElement(new listHandle(label, label));
	      }	    
	  }
      }

    // JstringListBox does the sorting
    in = new JstringListBox(inVector);
    in.setCallback(this);

    inPanel.setBorder(bborder);
    inPanel.setLayout(new BorderLayout());

    inPanel.add("Center", new JScrollPane(in));
    inPanel.add("North", new JLabel(inLabel));

    if (editable)
      {
	remove = new JButton("remove >>");
	remove.setEnabled(false);
	remove.setOpaque(true);
	remove.setActionCommand("Remove");
	remove.addActionListener(this);
	inPanel.add("South", remove);
      }

    lists.add(inPanel);

    // Set up the outPanel.
    // If we need an out box, build it now.
    if (editable && canChoose) 
      {
	if (available == null)
	  {
	    System.out.println(" HEY!  You tried to make a canChoose StringSelector with a null available vector.  That's ok, we forgive you.");
	  }
	else
	  {
	    String label = null;
	    listHandle lh = null;
	    for (int i = 0; i < available.size(); i++)
	      {
		if (available.elementAt(i) instanceof listHandle)
		  {
		    lh = (listHandle) available.elementAt(i);

		  }
		else
		  {
		    label = (String) available.elementAt(i);
		    lh = new listHandle(label, label);
		  }
	    
		// Don't add them if they are already in the in list

		if (! in.containsItem(lh))
		  {
		    outVector.addElement(lh);
		  }
	      }
	  }
      }

    // JstringListBox does the sorting


    if (editable && canChoose && (available != null))
      {
	add = new JButton("<< add");
	add.setEnabled(false);
	add.setOpaque(true);
	add.setActionCommand("Add");
	add.addActionListener(this);

	out = new JstringListBox(outVector);
	out.setCallback(this);
	
	outPanel.setBorder(bborder);
	outPanel.setLayout(new BorderLayout());
	outPanel.add("Center", new JScrollPane(out));
	outPanel.add("South", add);
	outPanel.add("North", new JLabel(outLabel));
	lists.add(outPanel);
      }

    add("Center", lists);

    custom = new JstringField();
    custom.setBorder(new EmptyBorder(new Insets(0,0,0,4)));
    custom.addActionListener(new ActionListener() 
			     {
			       public void actionPerformed(ActionEvent e)
				 {
				   addCustom.doClick();
				 }
			     });
      
    JPanel customP = new JPanel();
    customP.setLayout(new BorderLayout());
    customP.add("Center", custom);

    if (editable)
      {
	if (! (mustChoose && out == null))
	  {
	    addCustom = new JButton("Add");
	    addCustom.setActionCommand("AddNewString");
	    addCustom.addActionListener(this);
	    customP.add("East", addCustom);
	  }
      }
    
    add("South", customP);

    invalidate();
    parent.validate();

    if (debug)
      {
	System.out.println("Done creating ss");
      }
  }

  // Public methods ------------------------------------------------------------

  public void setVisibleRowCount(int numRows)
  {
    System.out.println("I don't know how to setVisibleRowCount yet.");
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

  public void setAvailableData(Vector available)
  {
    for (int i = 0; i < available.size(); i++)
      {
	if (in.containsItem(available.elementAt(i)))
	  {
	    available.removeElementAt(i);
	  }
      }
  }
  
  public void setChosenData(Vector chosen)
  {
    in.setListData(chosen);
  }

  // Event handling

  public void actionPerformed(ActionEvent e)
  {
    if (!editable)
      {
	return;
      }

    if (e.getActionCommand().equals("Add"))
      {
	if (debug)
	  {
	    System.err.println("StringSelector: add Action");
	  }

	addItem();
      }
    else if (e.getActionCommand().equals("Remove"))
      {
	if (debug)
	  {
	    System.err.println("StringSelector: remove Action");
	  }

	removeItem();
      }
    else if (e.getActionCommand().equals("AddNewString"))
      {
	if (debug)
	  {
	    System.err.println("StringSelector: addNewString Action");
	  }

	String item = custom.getText();

	if (item.equals("") || in.containsLabel(item))
	  {
	    if (debug)
	      {
		System.out.println("That one's already in there.  No soup for you!");
		return;
	      }
	  }

	if (mustChoose) 
	  {
	    // Check to see if it is in there
	    System.out.println("Checking to see if this is a viable option");
	    boolean inThere = false;

	    if (out != null)
	      {
		if (out.containsLabel(item))
		  {
		    out.setSelectedLabel(item);
		    // Not sure if this cast is going to always work.
		    listHandle handle = out.getSelectedHandle();
			
		    boolean ok = true;
		    
		    if (allowCallback)
		      {
			ok = false;
			try
			  {
			    ok = my_parent.setValuePerformed(new JValueObject(this, 
									      0,  //in.getSelectedIndex(),
									      JValueObject.ADD,
									      handle.getObject()));
			  }
			catch (RemoteException rx)
			  {
			    throw new RuntimeException("Could not setValuePerformed: " + rx);
			  }
		      }
		    
		    if (ok)
		      {
			putItemIn(handle);
			custom.setText("");
			inThere = true;
		      }
		    else
		      {
			try
			  {
			    if (out == null)
			      {
				my_parent.setValuePerformed(new JValueObject(this, 
									     0,  
									     JValueObject.ERROR,
									     "You can't choose stuff for this vector.  Sorry."));
			      }
			    else
			      {
				my_parent.setValuePerformed(new JValueObject(this, 
									     0,  
									     JValueObject.ERROR,
									     "That choice is not appropriate.  Please choose from the list."));
			      }
			  }
			catch (RemoteException rx)
			  {
			    throw new RuntimeException("Could not tell parent what is wrong: " + rx);
			  }
		      }

		  }
		
	      }
	

	    if (! inThere)
	      {
		if (debug)
		  {
		    System.out.println("That's not in there, returning.");
		  }
		
		if (allowCallback)
		  {
		    try
		      {
			if (out == null)
			  {
			    my_parent.setValuePerformed(new JValueObject(this, 
									 0,  
									 JValueObject.ERROR,
									 "You can't choose stuff for this vector.  Sorry."));
			  }
			else
			  {
			    my_parent.setValuePerformed(new JValueObject(this, 
									 0,  
									 JValueObject.ERROR,
									 "That choice is not appropriate.  Please choose from the list."));
			  }
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Could not tell parent what is wrong: " + rx);
		      }
		  }
	      }
	    return;
	    
	  }
	// not mustChoose, so you can stick it in there.
	else if (allowCallback)
	  {
	    boolean ok = false;
	    try
	      {
		ok = my_parent.setValuePerformed(new JValueObject(this, 
								  0,  //in.getSelectedIndex(),
								  JValueObject.ADD,
								  item));  //item is a String
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not setValuePerformed: " + rx);
	      }
	    
	    if (ok)
	      {
		in.addItem(new listHandle(item, item));
		
		//	in.setSelectedValue(item, true);
		custom.setText("");
	      }
	    else
	      {
		System.err.println("setValuePerformed returned false.");
	      }
	  }
	else //no callback to check
	  {
	    in.addItem(new listHandle(item, item));
	    //		    in.setSelectedValue(item, true);
	    custom.setText("");
	  }
	invalidate();
	parent.validate();
      }
  }
  
  void addItem()
  {
    boolean ok = false;
    Vector handles;

    if (out == null)
      {
	System.out.println("Can't figure out the handle.  No out box to get it from.");
	return;
      }

    handles = out.getSelectedHandles();

    if (handles == null)
      {
	System.err.println("Error.. got addItem with outSelected == null");
	return;
      }
	
    for (int i = 0; i < handles.size(); i++)
      {
	
	if (allowCallback)
	  {
	    try
	      {
		ok = my_parent.setValuePerformed(new JValueObject(this, 
								  0, // we are not giving a true index
								  JValueObject.ADD,
								  ((listHandle)handles.elementAt(i)).getObject()));
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not setValuePerformed: " + rx);
	      }
	    
	    if (ok)
	      {
		putItemIn((listHandle)handles.elementAt(i));
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("setValuePerformed returned false");
		  }
	      }
	  }
	else
	  {
	    putItemIn((listHandle)handles.elementAt(i));
	  }
      }
    
    invalidate();
    parent.validate();
  }
  
  void removeItem()
  {
    Vector handles;
    listHandle handle;

    /* -- */

    handles = in.getSelectedHandles();

    if (handles == null)
      {
	System.err.println("Error.. got removeItem with inSelected == null");
	return;
      }
	
    for (int i =0; i < handles.size(); i++)
      {
	handle = (listHandle)handles.elementAt(i);
	if (allowCallback)
	  {
	    boolean ok = false;
	    
	    try
	      {
		ok = my_parent.setValuePerformed(new JValueObject(this, 
								  0,
								  JValueObject.DELETE,
								  handle.getObject()));
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not setValuePerformed: " + rx);
	      }
	    
	    if (ok)
	      {
		takeItemOut(handle);
	      }
	    else
	      {
		System.err.println("setValuePerformed returned false.");
	      }
	  }
	else // no callback
	  {
	    takeItemOut(handle);
	  }
      }
  }

  void putItemIn(listHandle item)
  {
    if (debug)
      {
	System.out.println("Add: " + item);
      }

    if (!editable)
      {
	return;
      }

    if (canChoose)
      {
	if (out != null)
	  {
	    out.removeItem(item);
	  }

	if (debug)
	  {
	    System.out.println("Adding handle");
	  }

	in.addItem(item);

	if (debug)
	  {
	    System.out.println("Done Adding handle");
	  }
	//in.setSelectedValue(item, true);
      }
    else
      {
	throw new RuntimeException("Can't add something from the out box to a non-canChoose StringSelector!");
      }

  }
  
  void takeItemOut(listHandle item)
  {
    if (debug)
      {
	System.out.println("Remove." + item);
      }

    if (!editable)
      {
	return;
      }

    in.removeItem(item);

    if (out != null)
      {
	out.addItem(item);
      }

    // This should scroll to the new selected item
    //    
    //    if (out != null)
    //      {
    //	out.setSelectedValue(item, true);
    //      }

    invalidate();
    parent.validate();
  }

  public boolean setValuePerformed(JValueObject o)
  {
    if (o.getSource() == custom)
      {
	addCustom.doClick();
	return true;
      }
    else if (o.getSource() == in)
      {
	if (o.getOperationType() == JValueObject.INSERT)
	  {
	    remove.doClick();
	    return true;
	  }
	else
	  {
	    if (add != null)
	      {
		add.setEnabled(false);
	      }

	    remove.setEnabled(true);

	    if (out != null)
	      {
		out.setSelectedValue(null, false);
	      }
	    
	    return true;
	  }
      }
    else if (o.getSource() == out)
      {
	if (o.getOperationType() == JValueObject.INSERT)
	  {
	    add.doClick();
	    return true;
	  }
	else
	  {
	    add.setEnabled(true);
	    remove.setEnabled(false);
	    in.setSelectedValue(null, false);

	    return true;
	  }
      }
    else
      {	
	if (!editable)
	  {
	    return false;
	  }

	if (debug)
	  {
	    System.out.println("set value in stringSelector");
	  }
	
	System.out.println("Unknown object generated setValuePerformed in stringSelector.");
	
	return false;
      }


  }

}


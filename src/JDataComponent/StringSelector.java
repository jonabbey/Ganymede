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

import javax.swing.*;
import javax.swing.border.*;

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

  static final boolean debug = false;

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


  ////  Constructors  ////

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

  public StringSelector(Vector available, Vector chosen, Container parent, 
			boolean editable, int rowWidth,
			 String inLabel, String outLabel)
  {
    this(available, chosen, parent, editable, true, false, rowWidth, inLabel, outLabel, null, null);
  }
  
  public StringSelector(Vector available, Vector chosen, Container parent, boolean editable, int rowWidth)
  {
    this(available, chosen, parent, editable, (available != null), false, rowWidth);
  }

  public StringSelector(Vector available, Vector chosen, Container parent, boolean editable, String inLabel, String outLabel)
  {
    this(available, chosen, parent, editable, (available != null), false, 10, inLabel, outLabel, null, null);
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
    
    setBorder(new javax.swing.border.EtchedBorder());

    this.parent = parent;
    this.editable = editable;
    this.canChoose = canChoose;
    this.mustChoose = mustChoose;
    this.rowWidth = rowWidth;
    
    setLayout(new BorderLayout());

    // lists holds the outPanel and inPanel.

    lists = new JPanel();
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
    in = new JstringListBox(inVector, false, inPopup, rowWidth);
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
	    if (debug)
	      {
		System.out.println(" HEY!  You tried to make a canChoose StringSelector with a null available vector.  That's ok, we forgive you.");
	      }
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
		else
		  {
		    if (debug)
		      {
			System.out.println("** THIS ONES ALREADY IN THERE:" + lh);
		      }
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

	out = new JstringListBox(outVector, false, outPopup, rowWidth);
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

  /**
   * True if StringSelector is editable.
   *
   * Non-editable StringSelector's only have the chosen list.
   * Editable StringSelector's have both the chosen and available
   * lists.  
   */
  public boolean isEditable()
    {
      return editable;
    }

  /**
   * Update the StringSelector.
   */
  public void update(Vector available, Vector chosen)
  {
    if (available == null)
      {
	if (out != null)
	  {
	    out.model.removeAllElements();
	  }
      }

    // If there is no out box, then we don't need to worry about available stuff
    if (out != null)
      {
	if ((chosen != null) && (available != null)) // If's it null, nothing is chosen.
	  {
	    for (int i = 0; i < chosen.size(); i++)
	      {
		// What whill this do if it is not in available?  I don't know.
		try
		  {
		    available.removeElement(chosen.elementAt(i));
		  }
		catch (Exception e)
		  {
		    System.out.println("Could not remove Element: " + chosen.elementAt(i) + ", not in available vector?");
		  }

	      }
	  }

	try
	  {
	    out.reload(available, false);
	  }
	catch (Exception e)
	  {
	    throw new RuntimeException("Got an exception in out.reload: " + e);
	  }
      }

    try
      {
	in.reload(chosen, false);
      }
    catch (Exception e)
      {
	throw new RuntimeException("Got an exception in in.reload: " + e);
      }
    
    System.out.println("Done updating.");

  }


  /**
   * Change the text on the add button.
   */
  public void setButtonText(String text)
  {
    if (addCustom == null)
      {
	return;
      }

    addCustom.setText(text);
    validate();
  }

  /**
   * This doesn't work anymore.
   */
  public void setVisibleRowCount(int numRows)
  {
    if (debug)
      {
	System.out.println("I don't know how to setVisibleRowCount yet.");
      }
  }

  /**
   * Add a new item to the StringSelector.
   *
   * This is for adding an item that is not in either list, not selecting
   * an item from the out list.
   *
   * @param item Item to be added.  Can be listHandle or String
   * @param ShouldBeIn If true, object will be placed in in list.  Otherwise, it goes in out list.
   */
  public void addNewItem(Object item, boolean ShouldBeIn)
  {
    listHandle lh = null;
    if (item instanceof String)
      {
	lh = new listHandle((String)item, item);
      }
    else if (item instanceof listHandle)
      {
	lh = (listHandle)item;
      }
    else
      {
	System.out.println("What's this supposed to be? " + item);
	return;
      }

    if (ShouldBeIn)
      {
	in.addItem(lh);
      }
    else
      {
	out.addItem(lh);
      }


  }

  /**
   *  sets the parent of this component for callback purposes
   *
   *  Return types:  (Note that the index is always 0, so ignore it.)
   <ul>
   <li><b>PARAMETER</B>  Action from a PopupMenu.  The Paramter is the ActionCommand string.
   <li><b>ADD</b>  Object has been chosen
   <li><b>DELETE</b>  Object has been removed from chosen list.
   <li><b>ERROR</b>  Something went wrong.  Check the error message.
   </ul>
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
   * Sets the data for the out box.
   *
   * I'm not sure if this will work well.
   */

  public void setAvailableData(Vector available)
  {
    for (int i = 0; i < available.size(); i++)
      {
	if (in.containsItem(available.elementAt(i)))
	  {
	    available.removeElementAt(i);
	  }
      }
    out.setListData(available);

  }

  /**
   * Sets the data for the in box.
   *
   * Use with caution, it might not work at all.
   */  
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
	  {	    // Check to see if it is in there
	    if (debug)
	      {
		System.out.println("Checking to see if this is a viable option");
	      }
	    
	    if (out != null)
	      {
		if (out.containsLabel(item)) {
		    out.setSelectedLabel(item);
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
			return;
		      }
		  }
		else  //It's not in the outbox.
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
	// not mustChoose, so you can stick it in there.
	// But see, I need to see if it's in there first, because if it is, IF IT IS, then you have
	// to move the String over.  HA!
	else 
	  {
	    if ((out != null) && out.containsLabel(item))
	      {
		out.setSelectedLabel(item);
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
		
		    if (ok)
		      {
			putItemIn(handle);
			custom.setText("");
		      }	
		  }
		else //no callback to check
		  {
		    in.addItem(new listHandle(item, item));
		    //		    in.setSelectedValue(item, true);
		    custom.setText("");
		  }	
	      }
	    else  //Not in the out box, send up the String
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
		    if (debug)
		      {
			System.err.println("setValuePerformed returned false.");
		      }
		  }
	      }
	    
	    invalidate();
	    parent.validate();
	  }
      }
  }


  // Internal method to move item from out to in  
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

  // internal method to move item from in to out
  
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
		if (debug)
		  {
		    System.err.println("setValuePerformed returned false.");
		  }
	      }
	  }
	else // no callback
	  {
	    takeItemOut(handle);
	  }
      }
  }

  // this actuall does the inserting
  //

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

	// We only want to put it in if it's not already there.
	// Sometimes this happens in Ganymede if we update a field
	// before we are changing.  It happens like this: the "add"
	// button is clicked.  Then the return value decides to update
	// this field, which loads the value in the in box.  Then it
	// returns true, and then the value is already in there.  So
	// if we add it again, we get two of them.  Got it?

	if (! in.containsItem(item))
	  {
	    in.addItem(item);
	  }

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

  // this actually moves it from from in to out
  
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

    // If the item is already in there, don't add it.
    if ((out != null)  &&  (! out.containsItem(item)))
      {
	out.addItem(item);
      }

    in.invalidate();
    if (out != null)
      {
	out.invalidate();
      }
    invalidate();
    //parent.validate();
    if (parent.getParent() != null)
      {
	parent.getParent().validate();
      }
    else
      {
	parent.validate();
      }
  }

  public boolean setValuePerformed(JValueObject o)
  {
    if (o.getSource() == custom)
      {
	if (!editable)
	  {
	    return false;
	  }

	addCustom.doClick();
	return true;
      }
    else if (o.getOperationType() == JValueObject.PARAMETER)  // from the popup menu
      {
	if (allowCallback)
	  {
	    try
	      {
		my_parent.setValuePerformed(new JValueObject(this,
							     o.getIndex(),
							     JValueObject.PARAMETER,
							     o.getValue(),
							     o.getParameter()));
	      }
	    catch (java.rmi.RemoteException rx)
	      {
		System.out.println("could not setValuePerformed from StringSelector: " + rx);
	      }

	    return true;
	  }	
      }
    else if (o.getSource() == in)
      {
	if (!editable)
	  {
	    return false;
	  }

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

	    if (remove != null)
	      {
		remove.setEnabled(true);
	      }

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

    return false;  // should never really get here.
  }

}


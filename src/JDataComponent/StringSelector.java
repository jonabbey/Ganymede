/*
   tStringSelector.java

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
                                                                 tStringSelector


------------------------------------------------------------------------------*/

/**
 *
 * A two list box for adding strings to lists.
 *
 */

public class tStringSelector extends JPanel implements ActionListener, JsetValueCallback {

  static final boolean debug = true;

  // --

  JsetValueCallback
    my_parent;

  JButton
    add,
    remove;

  rowTable
    in, 
    out = null;

  tableHandler
    inHandler,
    outHandler;

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

  Object
    inSelected = null,
    outSelected = null;

  int[] colWidths, colWidths2;

  String[] inHeaders, outHeaders;

  /**
   *
   * Constructor for tStringSelector
   *
   * @param available Vector of listHandles for choices that are available
   * but are not currently in the set of selected values
   *
   * @param chosen Vector of listHandles for available choices
   *
   * @param parent AWT container that the tStringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param canChoose Choice must be made from vector of choices
   * @param mustChoose Vector of choices is available
   */

  public tStringSelector(Vector available, Vector chosen, Container parent, 
			boolean editable, boolean canChoose, boolean mustChoose, int rowWidth)
  {
    this(available, chosen, parent, editable, canChoose, mustChoose, rowWidth, "Selected", "Available", null, null);
  }

  /**
   *
   * Constructor for tStringSelector
   *
   * @param available Vector of listHandles for choices that are available
   * but are not currently in the set of selected values
   *
   * @param chosen Vector of listHandles for available choices
   *
   * @param parent AWT container that the tStringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param inLabel Label for the list of selected choices
   * @param outLabel Label for the list of available choices
   */

  public tStringSelector(Vector available, Vector chosen, Container parent, 
			boolean editable, int rowWidth,
			 String inLabel, String outLabel)
  {
    this(available, chosen, parent, editable, true, false, rowWidth, inLabel, outLabel, null, null);
  }

  /**
   *
   * Constructor for tStringSelector
   *
   * @param available Vector of listHandles for choices that are available
   * but are not currently in the set of selected values
   *
   * @param chosen Vector of listHandles for available choices
   *
   * @param parent AWT container that the tStringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param rowWidth How many columns wide should each box be?
   *
   */
  
  public tStringSelector(Vector available, Vector chosen, Container parent, boolean editable, int rowWidth)
  {
    this(available, chosen, parent, editable, (available != null), false, rowWidth);
  }

  /**
   *
   * Fully specified Constructor for tStringSelector
   *
   * @param available Vector of listHandles for choices that are available
   * but are not currently in the set of selected values
   *
   * @param chosen Vector of listHandles for available choices
   *
   * @param parent AWT container that the tStringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param canChoose Choice must be made from vector of choices
   * @param mustChoose Vector of choices is available
   * @param rowWidth How many columns wide should each box be?
   * @param inLabel Label for the list of selected choices
   * @param outLabel Label for the list of available choices
   * @param inPopup Popup Menu for in table
   * @param outPopup PopupMenu for out table
   */

  public tStringSelector(Vector available, Vector chosen, Container parent, 
			 boolean editable, boolean canChoose, boolean mustChoose, int rowWidth,
			 String inLabel, String outLabel, JPopupMenu inPopup, JPopupMenu outPopup)
  {
    if (debug)
      {
	System.out.println("-Adding new tStringSelector-");
      }
    
    setBorder(new com.sun.java.swing.border.EtchedBorder());

    this.parent = parent;
    this.editable = editable;
    this.canChoose = canChoose;
    this.mustChoose = mustChoose;

    colWidths = new int[1];
    colWidths[0] = rowWidth;

    colWidths2 = new int[1];
    colWidths2[0] = rowWidth;

    inHeaders = new String[1];
    inHeaders[0] = inLabel;

    outHeaders = new String[1];
    outHeaders[0] = outLabel;

    //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    setLayout(new BorderLayout());

    // lists holds the outPanel and inPanel.

    lists = new JPanel();
    //lists.setLayout(new BoxLayout(lists, BoxLayout.X_AXIS));
    lists.setLayout(new GridLayout(1,2));

    // Set up the inPanel, which holds the in list and button

    inHandler = new tableHandler(this);
    in = new rowTable(colWidths, inHeaders, inHandler, false, inPopup);
    in.setRowsVisible(8);
    inHandler.setTable(in);
    
    inPanel.setBorder(bborder);
    inPanel.setLayout(new BorderLayout());

    inPanel.add("Center", in);

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

    if (editable && canChoose && (available != null))
      {
	add = new JButton("<< add");
	add.setEnabled(false);
	add.setOpaque(true);
	add.setActionCommand("Add");
	add.addActionListener(this);

	outHandler = new tableHandler(this);
	out = new rowTable(colWidths2, outHeaders, outHandler, false, outPopup);
	out.setRowsVisible(8);
	outHandler.setTable(out);
	
	outPanel.setBorder(bborder);
	outPanel.setLayout(new BorderLayout());
	outPanel.add("Center", out);
	outPanel.add("South", add);
	lists.add(outPanel);
      }

    add("Center", lists);

    custom = new JstringField();
    custom.setBorder(new EmptyBorder(new Insets(0,0,0,4)));
    custom.setCallback(this);
    
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

    Object key;
    String label;

    if (chosen != null)
      {
	for (int i = 0; i < chosen.size(); i++)
	  {
	    if (chosen.elementAt(i) instanceof listHandle)
	      {
		listHandle item = (listHandle) chosen.elementAt(i);
		key = item.getObject();
		label = item.getLabel();
	      }
	    else
	      {
		label = (String) chosen.elementAt(i);
		key = label;
	      }
	    
	    in.newRow(key);
	    in.setCellText(key, 0, label, false);
	  }
      }

    in.resort(0, false);

    // If we need an out box, build it now.
    if (editable && canChoose) 
      {
	if (available == null)
	  {
	    System.out.println(" HEY!  You tried to make a canChoose tStringSelector with a null available vector.  That's ok, we forgive you.");
	  }
	else
	  {
	    for (int i = 0; i < available.size(); i++)
	      {
		if (available.elementAt(i) instanceof listHandle)
		  {
		    listHandle item = (listHandle) available.elementAt(i);
		    key = item.getObject();
		    label = item.getLabel();
		  }
		else
		  {
		    label = (String) available.elementAt(i);
		    key = label;
		  }
	    
		// Don't add them if they are already in the in list

		if (! in.containsKey(key))
		  {
		    out.newRow(key);
		    out.setCellText(key, 0, label, false);
		  }
	      }
	
	    out.resort(0, false);
	  }
      }

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
    in.setRowsVisible(numRows);

    if (out != null)
      {
    	out.setRowsVisible(numRows);
      }
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
	    System.err.println("tStringSelector: add Action");
	  }

	addItem();
      }
    else if (e.getActionCommand().equals("Remove"))
      {
	if (debug)
	  {
	    System.err.println("tStringSelector: remove Action");
	  }

	removeItem();
      }
    else if (e.getActionCommand().equals("AddNewString"))
      {
	if (debug)
	  {
	    System.err.println("tStringSelector: addNewString Action");
	  }

	String item = custom.getText();

	if (item.equals("") || in.containsKey(item))
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
		Enumeration theKeys = out.keys();
		while (theKeys.hasMoreElements())
		  {
		    Object key = theKeys.nextElement();
		    if (out.getCellText(key, 0).equals(item))
		      {
			out.selectRow(key);
			listHandle handle = null;
			if (outSelected instanceof String)
			  {
			    handle = new listHandle((String)outSelected, null);
			  }
			else
			  {
			    handle = new listHandle(out.getCellText(outSelected, 0), outSelected);
			  }
			
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
	else if (allowCallback)
	  {
	    boolean ok = false;
	    try
	      {
		ok = my_parent.setValuePerformed(new JValueObject(this, 
								  0,  //in.getSelectedIndex(),
								  JValueObject.ADD,
								  item));
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not setValuePerformed: " + rx);
	      }
	    
	    if (ok)
	      {
		in.newRow(item);
		in.setCellText(item, 0, item, true);
		
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
	    in.newRow(item);
	    in.setCellText(item, 0, item, true);
	    
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
    listHandle handle;

    if (outSelected == null)
      {
	System.err.println("Error.. got addItem with outSelected == null");
	return;
      }
	
    if (outSelected instanceof String)
      {
	handle = new listHandle((String)outSelected, null);
      }
    else
      {
	if (out == null)
	  {
	    System.out.println("Can't figure out the handle.  No out box to get it from.");
	    return;
	  }
	else
	  {
	    handle = new listHandle(out.getCellText(outSelected, 0), outSelected);
	  }
      }
    
    if (allowCallback)
      {
	try
	  {
	    ok = my_parent.setValuePerformed(new JValueObject(this, 
							      0, // we are not giving a true index
							      JValueObject.ADD,
							      outSelected));
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not setValuePerformed: " + rx);
	  }
	    
	if (ok)
	  {
	    putItemIn(handle);
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
	putItemIn(handle);
      }

    invalidate();
    parent.validate();
  }
  
  void removeItem()
  {
    listHandle handle;

    /* -- */

    if (inSelected == null)
      {
	System.err.println("Error.. got removeItem with inSelected == null");
	return;
      }
	
    if (inSelected instanceof String)
      {
	handle = new listHandle((String)inSelected, null);
      }
    else
      {
	handle = new listHandle(in.getCellText(inSelected, 0), inSelected);
      }

    if (allowCallback)
      {
	boolean ok = false;

	try
	  {
	    ok = my_parent.setValuePerformed(new JValueObject(this, 
							      0,
							      JValueObject.DELETE,
							      inSelected));
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
	    out.unSelectAll();
	    
	    if (item.getObject() != null)
	      {
		out.deleteRow(item.getObject(), true);
	      }
	    else
	      {
		out.deleteRow(item.getLabel(), true);
	      }
	  }

	if (debug)
	  {
	    System.out.println("Adding handle");
	  }

	if (item.getObject() != null)
	  {
	    in.newRow(item.getObject());
	    in.setCellText(item.getObject(), 0, item.getLabel(), false);
	  }
	else
	  {
	    in.newRow(item.getLabel());
	    in.setCellText(item.getLabel(), 0, item.getLabel(), false);
	  }

	in.resort(0, true);

	if (debug)
	  {
	    System.out.println("Done Adding handle");
	  }
	//	in.setSelectedValue(item, true);
      }
    else
      {
	throw new RuntimeException("Can't add something from the out box to a non-canChoose tStringSelector!");
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

    in.unSelectAll();

    if (item.getObject() != null)
      {
	in.deleteRow(item.getObject(), true);
      }
    else
      {
	in.deleteRow(item.getLabel(), true);
      }

    if (out != null)
      {
	if (item.getObject() != null)
	  {
	    out.newRow(item.getObject());
	    out.setCellText(item.getObject(), 0, item.getLabel(), false);
	  }
	else
	  {
	    out.newRow(item.getLabel());
	    out.setCellText(item.getLabel(), 0, item.getLabel(), false);
	  }

	out.resort(0, true);
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
    if (!editable)
      {
	return false;
      }

    if (debug)
      {
	System.out.println("set value in stringSelector");
      }

    if (o.getSource() == custom)
      {
	return true;
      }
    else
      {
	System.out.println("Unknown object generated setValuePerformed in stringSelector.");
      }
    return false;
  }

}

/*------------------------------------------------------------------------------
                                                                           class
                                                                    tableHandler

------------------------------------------------------------------------------*/

class tableHandler implements rowSelectCallback {

  tStringSelector parent;
  rowTable table;

  /* -- */

  /**
   * Constructor, remembers which table this handler is
   * responsible for.
   *
   */

  public tableHandler(tStringSelector parent)
  {
    this.parent = parent;
  }

  public void setTable(rowTable table)
  {
    this.table = table;
  }

  /**
   * Called when a row is selected in rowTable
   * 
   * @param key Hash key for the selected row
   */

  public void rowSelected(Object key)
  {
    if (table == parent.in)
      {
	parent.inSelected = key;
	
	if (parent.out != null)
	  {
	    parent.out.unSelectAll();
	    parent.add.setEnabled(false);
	  }

	if (parent.remove != null)
	  {
	    parent.remove.setEnabled(true);
	  }
      }
    else if (table == parent.out)
      {
	parent.outSelected = key;
	parent.in.unSelectAll();
	parent.remove.setEnabled(false);
	parent.add.setEnabled(true);
      }
    else
      {
	throw new RuntimeException("row selected in unknown table");
      }
  }

  /**
   * Called when a row is double selected (double clicked) in rowTable
   * 
   * @param key Hash key for the selected row
   */

  public void rowDoubleSelected(Object key)
  {
    if (table == parent.in)
      {
	parent.inSelected = key;
	parent.removeItem();
      }
    else if (table == parent.out)
      {
	parent.outSelected = key;
	parent.addItem();
      }
    else
      {
	throw new RuntimeException("row double-selected in unknown table");
      }

  }
  
  /**
   * Called when a row is unselected in rowTable
   * 
   * @param key Hash key for the unselected row
   * @param endSelected false if the callback should assume that the final
   *                    state of the system due to the user's present 
   *                    action will have no row selected
   */

  public void rowUnSelected(Object key, boolean endSelected)
  {
    if (endSelected)
      {
	if (table == parent.in)
	  {
	    parent.inSelected = null;
	  }
	else if (table == parent.out)
	  {
	    parent.outSelected = null;
	  }
	else
	  {
	    throw new RuntimeException("row unselected in unknown table");
	  }
      }
  }

  /**
   * Called when a popup menu action is performed
   * 
   * @param key Hash key for the row on which the popup menu item was performed
   * @param event the original ActionEvent from the popupmenu.  
   *              See event.getSource() to identify the menu item performed.
   */

  public void rowMenuPerformed(Object key, java.awt.event.ActionEvent event)
  {
    System.out.println("menu performed: " + event.getActionCommand());

    
    if (table == parent.in)
      {
	parent.inSelected = key;
      }
    else
      {
	parent.outSelected = key;
      }
      
    
    if (parent.allowCallback)
      {
	try
	  {
	    parent.my_parent.setValuePerformed(new JValueObject(null, 
								0,
								JValueObject.PARAMETER,
								key,
								event.getActionCommand()));
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not pass back the menu action: " + rx);
	  }
      }
  }

}

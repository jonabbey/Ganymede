/*

   StringSelector.java

   A two list box for adding strings to lists.

   Created: 10 October 1997
   Version: $Revision: 1.27 $
   Last Mod Date: $Date: 2000/09/15 01:58:03 $
   Release: $Name:  $

   Module By: Mike Mulvaney, Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

package arlut.csd.JDataComponent;

import java.awt.event.*;
import java.awt.*;

import java.util.*;
import java.rmi.*;
import java.net.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import arlut.csd.JTable.*;
import arlut.csd.Util.PackageResources;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  StringSelector


------------------------------------------------------------------------------*/

/**
 * <p>A two-paneled GUI component for adding or removing strings
 * and/or labeled objects from a list, with an optional list of
 * available strings and/or objects to choose from.</p>
 *
 * <p>StringSelector consists of one or (optionally) two {@link
 * arlut.csd.JDataComponent.JstringListBox JstringListBox} panels and
 * allows the user to move values back and forth between the two
 * panels.  Pop-up menus can be attached to each panel, allowing the
 * user to command the client to view or edit objects referenced in
 * either panel.  Objects in both panels are sorted alphabetically by
 * label.</p>
 *
 * <p>The setCallback() method takes an object implementing the {@link
 * arlut.csd.JDataComponent.JsetValueCallback JsetValueCallback}
 * interface in order to provide live notification of changes
 * performed by the user.  The JsetValueCallback implementation is
 * given the opportunity to approve any change made by the user before
 * the GUI is updated to show the change.  The JsetValueCallback
 * interface is also used to pass pop-up menu commands to the
 * client.</p>
 *
 * @see JstringListBox
 * @see JsetValueCallback
 *
 * @version $Revision: 1.27 $ $Date: 2000/09/15 01:58:03 $ $Name:  $
 * @author Mike Mulvaney, Jonathan Abbey */

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

  JLabel
    inTitle = new JLabel(),
    outTitle = new JLabel();

  String
    org_in,
    org_out;

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

  /* -- */

  /**
   *
   * @param available Vector of listHandles for choices that are available
   * but are not currently in the set of selected values
   * @param chosen Vector of listHandles for available choices
   * @param parent AWT container that the StringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param canChoose Choice must be made from vector of choices
   * @param mustChoose Vector of choices is available
   * @param rowWidth How many columns wide should each box be?  If <= 0, the
   * StringSelector will auto-size the columns
   */

  public StringSelector(Vector available, Vector chosen, Container parent, 
			boolean editable, boolean canChoose, boolean mustChoose, int rowWidth)
  {
    this(available, chosen, parent, editable, canChoose, mustChoose, rowWidth, "Selected", "Available", null, null);
  }

  /**
   *
   * @param available Vector of listHandles for choices that are available
   * but are not currently in the set of selected values
   * @param chosen Vector of listHandles for available choices
   * @param parent AWT container that the StringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param canChoose Choice must be made from vector of choices
   * @param mustChoose Vector of choices is available
   */

  public StringSelector(Vector available, Vector chosen, Container parent, 
			boolean editable, boolean canChoose, boolean mustChoose)
  {
    this(available, chosen, parent, editable, canChoose, mustChoose, 0, "Selected", "Available", null, null);
  }

  /**
   *
   * @param available Vector of listHandles for choices that are available
   * but are not currently in the set of selected values
   * @param chosen Vector of listHandles for available choices
   * @param parent AWT container that the StringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param rowWidth How many columns wide should each box be?  If <= 0, the
   * StringSelector will auto-size the columns
   * @param inLabel Label for the list of selected choices
   * @param outLabel Label for the list of available choices
   */

  public StringSelector(Vector available, Vector chosen, Container parent, 
			boolean editable, int rowWidth,
			 String inLabel, String outLabel)
  {
    this(available, chosen, parent, editable, true, false, rowWidth, 
	 inLabel, outLabel, null, null);
  }

  /**
   *
   * @param available Vector of listHandles for choices that are available
   * but are not currently in the set of selected values
   * @param chosen Vector of listHandles for available choices
   * @param parent AWT container that the StringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param rowWidth How many columns wide should each box be?  If <= 0, the
   * StringSelector will auto-size the columns
   */
  
  public StringSelector(Vector available, Vector chosen, Container parent, boolean editable, int rowWidth)
  {
    this(available, chosen, parent, editable, (available != null), false, rowWidth);
  }

  /**
   *
   * @param available Vector of listHandles for choices that are available
   * but are not currently in the set of selected values
   * @param chosen Vector of listHandles for available choices
   * @param parent AWT container that the StringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param inLabel Label for the list of selected choices
   * @param outLabel Label for the list of available choices
   */

  public StringSelector(Vector available, Vector chosen, Container parent, boolean editable, 
			String inLabel, String outLabel)
  {
    this(available, chosen, parent, editable, (available != null), false, 0,
	 inLabel, outLabel, null, null);
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
   * @param rowWidth How many columns wide should each box be?  If <= 0, the
   * StringSelector will auto-size the columns
   * @param inLabel Label for the list of selected choices
   * @param outLabel Label for the list of available choices
   * @param inPopup Popup Menu for in table
   * @param outPopup PopupMenu for out table
   */

  public StringSelector(Vector available, Vector chosen, Container parent, 
			boolean editable, boolean canChoose, boolean mustChoose, int rowWidth,
			String inLabel, String outLabel, JPopupMenu inPopup, JPopupMenu outPopup)
  {
    org_in = inLabel;
    org_out = outLabel;

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

    GridBagLayout
      gbl = new GridBagLayout();

    GridBagConstraints
      gbc = new GridBagConstraints();

    lists = new JPanel();
    lists.setLayout(gbl);

    // Set up the inPanel, which holds the in list and button

    // chosen is a vector of listhandles or Strings.  If it is
    // strings, create a vector of listhandles.

    if (chosen != null && chosen.size() > 0)
      {
	if ((chosen.elementAt(0) == null) || (chosen.elementAt(0) instanceof listHandle))
	  {
	    if (debug)
	      {
		if (chosen.elementAt(0) instanceof listHandle)
		  {
		    System.err.println("arlut.csd.JDataComponent.StringSelector(): chosen = listHandle vector");
		  }
		else
		  {
		    System.err.println("arlut.csd.JDataComponent.StringSelector(): chosen[0] = null");
		  }
	      }

	    inVector = chosen;
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("arlut.csd.JDataComponent.StringSelector(): chosen = String vector");
	      }

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
    inTitle.setText(org_in.concat(" : " + String.valueOf(in.getSizeOfList())));
    inPanel.add("North", inTitle);

    if (editable)
      {
	if (canChoose && (available != null))
	  {
	    remove = new JButton("remove >>");
	  }
	else
	  {
	    remove = new JButton("remove");
	  }

	remove.setEnabled(false);
	remove.setOpaque(true);
	remove.setActionCommand("Remove");
	remove.addActionListener(this);
	inPanel.add("South", remove);
      }

    gbc.fill = gbc.BOTH;
    gbc.gridwidth = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbl.setConstraints(inPanel, gbc);

    lists.add(inPanel);

    // Set up the outPanel.
    // If we need an out box, build it now.

    if (editable && canChoose) 
      {
	if (available == null)
	  {
	    if (debug)
	      {
		System.out.println(" HEY!  You tried to make a canChoose StringSelector with a null available vector.");
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

		if (!in.containsItem(lh))
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
	    outTitle.setText(org_out.concat(" : " + String.valueOf(out.getSizeOfList())));	    
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

	out = new JstringListBox(outVector, true, outPopup, rowWidth); // our list of choices is presorted
	out.setCallback(this);
	
	outPanel.setBorder(bborder);
	outPanel.setLayout(new BorderLayout());
	outPanel.add("Center", new JScrollPane(out));
	outPanel.add("North", outTitle);
	outPanel.add("South", add);

	gbc.fill = gbc.BOTH;
	gbc.gridwidth = 1;
	gbc.weightx = 1.0;
	gbc.weighty = 1.0;
	gbc.gridx = 1;
	gbc.gridy = 0;
	gbl.setConstraints(outPanel, gbc);

	lists.add(outPanel);
      }

    add("Center", lists);

    if (editable)
      {
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

	if (!(mustChoose && out == null))
	  {
	    addCustom = new JButton("Add");
	    addCustom.setEnabled(false);
	    addCustom.setActionCommand("AddNewString");
	    addCustom.addActionListener(this);
	    customP.add("West", addCustom);

	    // we only want this add button to be active when the user
	    // has entered something in the text field.  Some users
	    // have been confused by the add button just sitting there
	    // active.

	    custom.getDocument().addDocumentListener(new DocumentListener()
						     {
						       public void changedUpdate(DocumentEvent x) {}
						       public void insertUpdate(DocumentEvent x) 
							 {
							   if (x.getDocument().getLength() > 0)
							     {
							       addCustom.setEnabled(true);
							     }
							 }
						       
						       public void removeUpdate(DocumentEvent x) 
							 {
							   if (x.getDocument().getLength() == 0)
							     {
							       addCustom.setEnabled(false);
							     }
							 }
						     });
	  }

	// if we know they can only type things from the list,
	// implement choice completion

	if (mustChoose)
	  {
	    custom.addKeyListener(new KeyAdapter()
				  {
				    public void keyReleased(KeyEvent ke)
				      {
					int curLen;
					String curVal;

					curVal = custom.getText();
    
					if (curVal != null)
					  {
					    curLen = curVal.length();
					  }
					else
					  {
					    curLen = 0;
					  }

					int keyCode = ke.getKeyCode();

					switch (keyCode)
					  {
					  case KeyEvent.VK_UP:
					  case KeyEvent.VK_DOWN:
					  case KeyEvent.VK_LEFT:
					  case KeyEvent.VK_RIGHT:
					  case KeyEvent.VK_SHIFT:

					  case KeyEvent.VK_DELETE:
					  case KeyEvent.VK_BACK_SPACE:
					    return;
					  }

					if (curLen > 0)
					  {
					    listHandle item;

					    int matching = 0;
					    String matchingItem = null;

					    Enumeration enum = out.model.elements();

					    while (enum.hasMoreElements())
					      {
						item = (listHandle) enum.nextElement();

						if (item.toString().equals(curVal))
						  {
						    // they've typed the full thing here, stop.

						    return;
						  }
						else if (item.toString().startsWith(curVal))
						  {
						    matching++;
						    matchingItem = item.toString();
						  }
					      }

					    if (matching == 1)
					      {
						custom.setText(matchingItem);
						custom.select(curLen, matchingItem.length());
						return;
					      }
					  }
				      }
				    });
	  }

	add("South", customP);
      }

    invalidate();
    parent.validate();

    if (debug)
      {
	System.out.println("Done creating ss");
      }
  }

  // Public methods ------------------------------------------------------------

  /**
   * <P>Returns true if this StringSelector is editable.</P>
   *
   * <P>Non-editable StringSelector's only have the chosen list.
   * Editable StringSelector's have both the chosen and available
   * lists.</P>
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
	    out.reload(available, true); // choice lists are always pre-sorted.
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
   * @deprecated This doesn't work anymore.
   */

  public void setVisibleRowCount(int numRows)
  {
    if (debug)
      {
	System.out.println("I don't know how to setVisibleRowCount yet.");
      }
  }

  /**
   * <p>Add a new item to the StringSelector.</p>
   *
   * <p>This is for adding an item that is not in either list, not selecting
   * an item from the out list.</p>
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
   * <p>Connects this StringSelector to an implementaton of the
   * {@link arlut.csd.JDataComponent.JsetValueCallback JsetValueCallback} interface
   * in order to provide live notification of changes performed by the user.  The
   * JsetValueCallback implementation is given the opportunity to approve any change
   * made by the user before the GUI is updated to show the change.  The JsetValueCallback
   * interface is also used to pass pop-up menu commands to the client.</p>
   *
   * <p>StringSelector uses the following value type constants from
   * {@link arlut.csd.JDataComponent.JValueObject JValueObject} to pass status updates to
   * the callback.
   *
   * <ul>
   * <li><b>PARAMETER</B> Action from a PopupMenu.  The Parameter is the ActionCommand
   * string for the pop-up menu item selected, and the value is the object
   * (or string if no object defined) associated with the item selected when the pop-up menu was fired.</li>
   * <li><b>ADD</b> Object has been added to the selected list.  Value is the object (or string) added.</li>
   * <li><b>DELETE</b> Object has been removed from chosen list.  Value is the object (or string) removed.</li>
   * <li><b>ERROR</b> Something went wrong.  Value is the error message to be displayed to the user in whatever
   * fashion is appropriate.</li>
   * </ul>
   * </p>
   *
   * @see JsetValueCallback
   * @see JValueObject
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
   * <P>Sets the data for the out box.</P>
   *
   * <P>I'm not sure if this will work well.</P>
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
   * <P>Sets the data for the in box.</P>
   *
   * <P>Use with caution, it might not work at all.</P>
   */
  
  public void setChosenData(Vector chosen)
  {
    in.setListData(chosen);
  }

  /**
   *
   * This method handles events from the Add and Remove
   * buttons, and from hitting enter/loss of focus in the
   * custom JstringField.
   *
   */

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

	    if (debug)
	      {
		System.out.println("Checking to see if this is a viable option");
	      }
	    
	    if (out != null)
	      {
		if (out.containsLabel(item)) 
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
	else
	  {
	    // not mustChoose, so you can stick it in there.  But see,
	    // I need to see if it's in there first, because if it is,
	    // IF IT IS, then you have to move the String over.  HA!

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
	    else 
	      {
		//Not in the out box, send up the String
		
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
	    
	    validate();
	  }
      }
  }

  /**
   *
   * Internal method to move item from out to in  
   *
   */

  void addItem()
  {
    boolean ok = false;
    Vector handles;

    /* -- */

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
    
    if (handles.size() > 1)
      {
	ok = true;

	if (allowCallback)
	  {
	    Vector objVector = new Vector(handles.size());

	    for (int i = 0; i < handles.size(); i++)
	      {
		objVector.addElement(((listHandle) handles.elementAt(i)).getObject());
	      }

	    try
	      {
		ok = my_parent.setValuePerformed(new JValueObject(this, 
								  0, // we are not giving a true index
								  JValueObject.ADDVECTOR,
								  objVector));
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not setValuePerformed: " + rx);
	      }
	  } 

	if (ok)
	  {
	    for (int i = 0; i < handles.size(); i++)
	      {
		putItemIn((listHandle)handles.elementAt(i));
	      }
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
	ok = true;

	if (allowCallback)
	  {
	    try
	      {
		ok = my_parent.setValuePerformed(new JValueObject(this, 
								  0, // we are not giving a true index
								  JValueObject.ADD,
								  ((listHandle)handles.elementAt(0)).getObject()));
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not setValuePerformed: " + rx);
	      }
	    
	  }

	if (ok)
	  {
	    putItemIn((listHandle)handles.elementAt(0));
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("setValuePerformed returned false");
	      }
	  }
      }
    
    invalidate();
    parent.validate();
  }

  /**
   *
   * internal method to move item from in to out
   *
   */
  
  void removeItem()
  {
    Vector handles;
    listHandle handle;
    boolean ok;

    /* -- */

    handles = in.getSelectedHandles();

    if (handles == null)
      {
	System.err.println("Error.. got removeItem with inSelected == null");
	return;
      }

    if (handles.size() > 1)
      {
	ok = true;

	if (allowCallback)
	  {
	    Vector objVector = new Vector(handles.size());

	    for (int i = 0; i < handles.size(); i++)
	      {
		objVector.addElement(((listHandle) handles.elementAt(i)).getObject());
	      }

	    try
	      {
		ok = my_parent.setValuePerformed(new JValueObject(this, 
								  0, // we are not giving a true index
								  JValueObject.DELETEVECTOR,
								  objVector));
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not setValuePerformed: " + rx);
	      }
	  } 

	if (ok)
	  {
	    for (int i = 0; i < handles.size(); i++)
	      {
		takeItemOut((listHandle)handles.elementAt(i));
	      }
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
	ok = true;

	if (allowCallback)
	  {
	    try
	      {
		ok = my_parent.setValuePerformed(new JValueObject(this, 
								  0, // we are not giving a true index
								  JValueObject.DELETE,
								  ((listHandle)handles.elementAt(0)).getObject()));
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not setValuePerformed: " + rx);
	      }
	    
	  }

	if (ok)
	  {
	    takeItemOut((listHandle)handles.elementAt(0));
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("setValuePerformed returned false");
	      }
	  }
      }
  }

  /**
   *
   * this actually does the inserting
   *
   */

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
	    outTitle.setText(org_out.concat(" : " + String.valueOf(out.getSizeOfList())));	    
	    //outPanel.add("North", outTitle
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
      	    inTitle.setText(org_in.concat(" : " + String.valueOf(in.getSizeOfList())));
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

  /**
   *
   * this actually moves it from from in to out
   *
   */
  
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
    inTitle.setText(org_in.concat(" : " + String.valueOf(in.getSizeOfList())));	    

    // If the item is already in there, don't add it.

    if ((out != null)  &&  (! out.containsItem(item)))
      {
	out.addItem(item);
        outTitle.setText(org_out.concat(" : " + String.valueOf(out.getSizeOfList())));	    
      }

    remove.setEnabled(false);

    in.invalidate();

    if (out != null)
      {
	out.invalidate();
      }

    invalidate();

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
	else if (o.getOperationType() == JValueObject.ADD)		// selection
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
		out.clearSelection();
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
	else if (o.getOperationType() == JValueObject.ADD)
	  {
	    add.setEnabled(true);
	    remove.setEnabled(false);
	    in.clearSelection();

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


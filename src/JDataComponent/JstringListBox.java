/*

   JstringListBox.java

   An implementation of JListBox used to display strings.

   Created: 21 Aug 1997
   Release: $Name:  $
   Version: $Revision: 1.29 $
   Last Mod Date: $Date: 2001/06/27 20:21:44 $
   Module By: Mike Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

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

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import arlut.csd.Util.VecQuickSort;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  JstringListBox

------------------------------------------------------------------------------*/

/**
 *
 * <p>A sorted listbox that handles {@link arlut.csd.JDataComponent.listHandle listHandle}'s.
 * JstringListBox supports pop-up menus and uses the
 * @link arlut.csd.JDataComponent.JsetValueCallback JsetValueCallback}
 * interface to report selection and pop-up menu activity to the registered
 * callback.</p>
 *
 * <p>listHandles are wrappers that can hold both a String and (optionally) a related
 * object, such as an Invid.  The JstringListBox uses them to allow the client to
 * manipulate labeled object pointers.</p>
 *
 * <p>The {@link arlut.csd.JDataComponent.StringSelector StringSelector} class uses
 * JstringListBoxes to support adding or removing Strings and Objects from String
 * and Invid vector fields.</p>
 *
 * @see arlut.csd.ganymede.Invid
 * @see arlut.csd.JDataComponent.listHandle
 * @see arlut.csd.JDataComponent.StringSelector
 * @see arlut.csd.JDataComponent.JsetValueCallback
 * @version $Revision: 1.29 $ $Date: 2001/06/27 20:21:44 $ $Name:  $
 * @author Mike Mulvaney
 *
 */

public class JstringListBox extends JList implements ActionListener, ListSelectionListener, MouseListener {

  static final boolean debug = false;

  // -- 

  int 
    width,
    popUpIndex = -1,
    numberOfStrings = 0;

  DefaultListModel 
    model = new DefaultListModel();

  boolean
    presorted,
    allowCallback = false;

  JsetValueCallback 
    my_parent;

  JPopupMenu
    popup = null;

  /* -- */

  /**
   *
   * Default Constructor
   *
   */

  public JstringListBox()
  {
    this(null, false, null);
  }

  /**
   *
   * Constructor
   *
   * @param items Vector of items (Strings or listHandles) to show in the list
   *
   */

  public JstringListBox(Vector items)
  {
    this(items, false, null);
  }

  /**
   *
   * Constructor 
   *
   * @param items Vector of items (Strings or listHandles) to show in the list
   * @param presorted If true, JstringListBox will not sort the vector(it is already sorted)
   *
   */

  public JstringListBox(Vector items, boolean presorted)
  {
    this(items, presorted, null);
  }

  /**
   *
   * Constructor 
   *
   * @param items Vector of items (Strings or listHandles) to show in the list
   * @param presorted If true, JstringListBox will not sort the vector(it is already sorted)
   * @param popup JPopupMenu that will be shown on right click.  Callback is of type PARAMETER
   *
   */

  public JstringListBox(Vector items, boolean presorted, JPopupMenu popup)
  {
    this(items, presorted, popup, 0);
  }

  /**
   *
   * Constructor 
   *
   * @param items Vector of items (Strings or listHandles) to show in the list
   * @param presorted If true, JstringListBox will not sort the vector(it is already sorted)
   * @param popup JPopupMenu that will be shown on right click.  Callback is of type PARAMETER
   * @param width Width in pixels of the string list box.  If <= 0, the list box will be
   * auto-sized, with a 20 char minimum width
   *
   */

  public JstringListBox(Vector items, boolean presorted, JPopupMenu popup, int width)
  {
    this.presorted = presorted;
    this.popup = popup;
    this.width = width;

    /* - */

    // longString is used to calculate the minimum width of
    // a JstringListBox in the absence of a defined width.

    String longString = "this is the minimum!";

    /* -- */

    if (popup != null)
      {
	Component[] c = popup.getComponents();

	for (int i = 0; i < c.length; i++)
	  {
	    if (c[i] instanceof JMenuItem)
	      {
		JMenuItem pm = (JMenuItem)c[i];
		pm.addActionListener(this);
	      }
	    else
	      {
		throw new IllegalArgumentException("Hey, you are supposed to use JMenuItems in JPopupMenus, buddy.");
	      }
	  }
      }
	
    //model setSize(items.size());
    //System.out.println("Setting size to " + items.size());

    if ((items != null) && (items.size() > 0))
      {	
	if (items.elementAt(0) instanceof String)
	  {
	    if (!presorted)
	      {
		if (debug)
		  {
		    System.out.println("Sorting...");
		  }

		(new VecQuickSort(items,
				  new arlut.csd.Util.Compare() 
				  {
				    public int compare(Object a, Object b) 
				      {
					String aF, bF;
					
					aF = (String) a;
					bF = (String) b;
					int comp = 0;
					
					comp =  aF.compareTo(bF);
					
					if (comp < 0)
					  {
					    return -1;
					  }
					else if (comp > 0)
					  {
					    return 1;
					  }
					else
					  {
					    return 0;
					  }
				      }
				  }
				  )
		  ).sort();

		if (debug)
		  {
		    System.out.println("Done sorting strings");
		  }
	      } 

	    String string;

	    for (int i=0 ; i<items.size() ; i++)
	      {
		string = (String)items.elementAt(i);

		if (width <= 0)
		  {
		    if (longString == null)
		      {
			longString = string;
		      }
		    else
		      {
			if (string.length() > longString.length())
			  {
			    longString = string;
			  }
		      }
		  }

		if (debug)
		  {
		    System.err.println("JstringListBox: adding string " + string);
		  }
		
		insertHandleAt(new listHandle(string, string), i);
	      }
	  }
	else if (items.elementAt(0) instanceof listHandle)
	  {
	    if (!presorted)
	      {
		if (debug)
		  {
		    System.out.println("Sorting...");
		  }

		(new VecQuickSort(items,
				  new arlut.csd.Util.Compare() 
				  {
				    public int compare(Object a, Object b) 
				      {
					listHandle aF, bF;
					
					aF = (listHandle) a;
					bF = (listHandle) b;
					int comp = 0;
					
					comp =  aF.getLabel().compareTo(bF.getLabel());
					
					if (comp < 0)
					  {
					    return -1;
					  }
					else if (comp > 0)
					  { 
					    return 1;
					  } 
					else
					  { 
					    return 0;
					  }
				      }
				  }
				  )	      
		 ).sort();

		if (debug)
		  {
		    System.out.println("Done sorting.");
		  }
	      }
	    
	    for (int i=0 ; i<items.size() ; i++)
	      {
		listHandle x = (listHandle) items.elementAt(i);

		if (width <= 0)
		  {
		    if (longString == null)
		      {
			longString = x.label;
		      }
		    else
		      {
			if (x.label.length() > longString.length())
			  {
			    longString = x.label;
			  }
		      }
		  }

		if (debug)
		  {
		    System.err.println("JstringListBox: adding listhandle " + x.getLabel() + ">>" + x.getObject());
		  }
		
		insertHandleAt(x, i);
	      }
	  }
	else
	  {
	    System.out.println("Unsupported item type passed to JstringListBox " +
			       "in a vector(needs to be String or ListHandle)");
	  }
      
      }

    setModel(model);
    addMouseListener(this);

    if (width > 0)
      {
	setFixedCellWidth(width);
      }
    else
      {
	setPrototypeCellValue(longString);
      }

    setSelectionMode(DefaultListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
  }

  /**
   *
   * Reload the list box.
   *
   */

  public void reload(Vector items, boolean presorted)
  {
    //model.removeAllElements();

    model = new DefaultListModel();

    if ((items != null) && (items.size() > 0))
      {
	if (items.elementAt(0) instanceof listHandle)
	  {
	    if (!presorted)
	      {
		items = sortListHandleVector(items);
	      }

	    for (int i = 0; i < items.size(); i++)
	      {
		insertHandleAt((listHandle)items.elementAt(i), i);
	      }
	  }
	else  //It must be a string, or it will throw a ClassCastException
	  {
	    if (!presorted)
	      {
		items = sortStringVector(items);
	      }

	    for (int i = 0; i < items.size(); i++)
	      {
		String s = (String)items.elementAt(i);
		insertHandleAt(new listHandle(s,s), i);
	      }
	  }
      }
    else 
      {
	if (debug)
	  {
	    System.out.println("no items to add");
	  }
      }

    setModel(model);
  }

  /**
   *
   * Convenience method to set the size on the model.
   *
   */

  public void setSize(int size)
  {
    model.setSize(size);
  }

  /**
   * <p>Connects this JstringListBox to an implementaton of the
   * {@link arlut.csd.JDataComponent.JsetValueCallback JsetValueCallback} interface
   * in order to provide live notification of changes performed by the user.  The
   * JsetValueCallback implementation is given the opportunity to approve any change
   * made by the user before the GUI is updated to show the change.  The JsetValueCallback
   * interface is also used to pass pop-up menu commands to the client.</p>
   *
   * <p>JstringListBox uses the following value type constants from
   * {@link arlut.csd.JDataComponent.JValueObject JValueObject} to pass status updates to
   * the callback.
   *
   * <ul>
   * <li><b>PARAMETER</B>Action from a PopupMenu.  The Parameter is the ActionCommand
   * string for the pop-up menu item selected, and the value is the object
   * (or string if no object defined) associated with the item selected when the pop-up menu was fired.</li>
   * <li><b>ADD</b>Object has been selected.  Value is the object (or string) selected.</li>
   * <li><b>INSERT</b>Object has been double-clicked.  Value is the object (or string) double-clicked.</li>
   * </ul>
   * </p>
   *
   * @see JsetValueCallback
   * @see JValueObject
   *
   */

  public void setCallback(JsetValueCallback parent)
  {
    if (debug)
      {
	System.out.println("Setting callback in JstringListBox");
      }

    if (parent == null)
      {
	throw new IllegalArgumentException("Invalid Parameter: parent cannot be null");
      }
    
    getSelectionModel().addListSelectionListener(this);

    my_parent = parent;

    allowCallback = true;
  }

  /**
   * Add an item to the list box.
   *
   * @param o Can be a String or listHandle.
   */

  public void addItem(Object o)
  {
    listHandle lh = null;

    if (o instanceof String)
      {
	lh = new listHandle((String)o, (String)o);
      }
    else if (o instanceof listHandle)
      {
	lh = (listHandle)o;
      }
    else
      {
	System.err.println("You gave me an object that is neither String nor listHandle: " + o);
      }

    int i = 0;

    int total = model.getSize();
    while ((i < total) && (lh.getLabel().compareTo(((listHandle)model.getElementAt(i)).getLabel()) > 0))
      {
	i++;
      }
      
    insertHandleAt(lh, i);
    setSelectedValue(lh, true);
  }

  /**
   *
   * Use this one to skip the sorting.  Called by all the add methods.
   *
   */

  public void insertHandleAt(listHandle handle, int row)
  {
    model.insertElementAt(handle, row);
  }

  /**
   * Remove an item from list.
   *
   * @param o can be listHandle or String
   */

  public void removeItem(Object o)
  {
    if (o instanceof listHandle)
      {
	model.removeElement((listHandle)o);
      }
    else if (o instanceof String)
      {
	if (debug)
	  {
	    System.out.println("I am guessing you want me to remove a label...");
	  }

	removeLabel((String) o);
      }
    else
      {
	if (debug)
	  {
	    System.out.println("Ok, i will look for this object in the listHnaldes.");
	  }

	for (int i = 0; i < model.getSize(); i++)
	  {
	    if ((((listHandle)model.elementAt(i)).getObject()).equals(o))
	      {
		model.removeElementAt(i);
		break;
	      }
	  }
      }
  }

  /**
   * Remove an object by label
   *
   * @param s Label of object to remove.
   */

  public void removeLabel(String s)
  {
    for (int i = 0; i < model.getSize(); i++)
      {
	if ((((listHandle)model.elementAt(i)).getLabel()).equals(s))
	  {
	    model.removeElementAt(i);
	    break; //Assume there is only one?
	  }
      }
  }

  public int getSizeOfList()
  {
    return model.getSize();
  }

  /**
   *
   * Returns true if the list contains an object with the specified label.
   *
   */

  public boolean containsLabel(String string)
  {
    return containsString(string);
  }

  /**
   * Returns true if the list contains an object with the specified label.
   *
   * Since everything is a listHandle internally, this is the same as containsLabel
   *
   */

  public boolean containsString(String string)
  {
    for (int i = 0; i < model.getSize(); i++)
      {
	if ((((listHandle)model.elementAt(i)).getLabel()).equals(string))
	  {
	    return true;
	  }
      }

    return false;
  }

  /**
   * Returns true if the item is in the list
   *
   * @param o Can be a String(label) or listHandle
   */

  public boolean containsItem(Object o)
  {
    if (o instanceof String)
      {
	return containsLabel((String)o);
      }
    else if (o instanceof listHandle)
      {
	return model.contains(o);
      }
    else
      {
	for (int i = 0; i < model.getSize(); i++)
	  {
	    if (((listHandle)model.elementAt(i)).getObject().equals(o))
	      {
		return true;
	      }
	  }
      }

    return false;
  }
   
  /**
   *
   * This selects the item with the given label.
   *
   */

  public void setSelectedLabel(String s)
  {
    int size = model.getSize();
    listHandle lh = null;

    for (int i = 0; i < size; i++)
      {
	lh = (listHandle)model.getElementAt(i);

	if (lh.getLabel().equals(s))
	  {
	    setSelected(lh);
	    break;
	  }
      }
  }

  /**
   *
   * Sets the selected item.
   *
   * @param o Can be listHandle or String
   */

  public void setSelected(Object o)
  {
    if (o instanceof listHandle)
      {
	setSelectedValue(o, true); // use the JList.setSelectedValue(Object, boolean shouldScroll)
      }
    else if (o instanceof String)
      {
	setSelectedLabel((String)o);
      }
    else
      {
	System.out.println("What kind of object? " + o);
      }
  }

  /**
   *
   * This returns just the label.
   *
   */

  public String getSelectedLabel()
  {
    return ((listHandle)model.elementAt(getSelectedIndex())).getLabel();
  }

  /**
   *
   * This returns the whole listHandle.
   *
   */

  public listHandle getSelectedHandle()
  {
    return (listHandle)model.elementAt(getSelectedIndex());
  }

  /**
   *
   * Returns all the selected handles.
   *
   */

  public Vector getSelectedHandles()
  {
    Vector v = new Vector();
    Object[] values = getSelectedValues();
    
    for (int i =0; i < values.length; i++)
      {
	v.addElement(values[i]);
      }

    return v;
  }

  /**
   * Returns all handles
   */

  public Vector getHandles()
  {
    Vector v = new Vector();
    
    for (int i =0; i < getModel().getSize(); i++)
      {
	v.addElement(getModel().getElementAt(i));
      }

    return v;
  }

  /**
   *
   * This returns the object, without the label.
   *
   */

  public Object getSelectedItem()
  {
    try
      {
	listHandle lh = (listHandle) model.elementAt(getSelectedIndex());
	return lh.getObject();
      }
    catch (Exception e)
      {
	return null;
      }
  }

  /**
   *
   * For the ListSelectionListener
   *
   */

  public void valueChanged(ListSelectionEvent e)
  {
    if (getSelectedIndex() == -1)
      {
	// don't notify our container on deselect

	return;
      }

    if (allowCallback)
      {
	boolean ok = false;

	try 
	  {
	    ok = my_parent.setValuePerformed(new JValueObject(this, 
							      getSelectedIndex(),
							      JValueObject.ADD));
	  }
	catch (java.rmi.RemoteException rx)
	  {
	    throw new RuntimeException("Could not setValuePerformed: " + rx);
	  }

	if (ok)
	  {
	    //do something
	  }
	else
	  {
	    //put it back
	  }
      }
    else
      {
	if (debug)
	  {
	    System.out.println("allowCallback = false");
	  }
      }
  }

  /**
   *
   * For the mouseListener interface
   *
   */

  public void mouseClicked(MouseEvent e)
  {
    if (allowCallback)
      {
	if (SwingUtilities.isLeftMouseButton(e))
	  {
	    if (e.getClickCount() == 2)
	      {
		boolean ok = false;
	    
		int index = locationToIndex(e.getPoint());

		if (debug)
		  {
		    System.out.println("Double clicked on Item " + index);
		  }

		try
		  {
		    ok = my_parent.setValuePerformed(new JValueObject(this, 
								      index,
								      JValueObject.INSERT));
		  }
		catch (java.rmi.RemoteException rx)
		  {
		    throw new RuntimeException("Double click produced: " + rx);
		  }
	    
		if (debug)
		  {
		    System.out.println("setValue from JstringListBox=" + ok);
		  }
	      }
	  }
	else if (SwingUtilities.isRightMouseButton(e))
	  {
	    if (debug)
	      {
		System.out.println("Its a popup trigger!");
	      }

	    popUpIndex = locationToIndex(e.getPoint());

	    boolean found = false;

	    try
	      {
		if (model.elementAt(popUpIndex) != null)
		  {
		    found = true;
		  }
	      }
	    catch (ArrayIndexOutOfBoundsException ex)
	      {
	      }

	    if (found)
	      {
		clearSelection();
		setSelectedIndex(popUpIndex);

		if (popup != null)
		  {
		    popup.setVisible(false);

		    popup.show(e.getComponent(), e.getX(), e.getY());
		  }
	      }
	  }
      }
  }

  /**
   *
   * For the mouseListener interface
   *
   */

  public void mouseEntered(MouseEvent e)
  {
  }

  /**
   *
   * For the mouseListener interface
   *
   */

  public void mouseExited(MouseEvent e)
  {
  }

  /**
   *
   * For the mouseListener interface
   *
   */

  public void mousePressed(MouseEvent e)
  {
  }

  /**
   *
   * For the mouseListener interface
   *
   */

  public void mouseReleased(MouseEvent e)
  {
  }

  /**
   *
   * For the pop up menu callback.  We use the popUpIndex variable to
   * identify the item in the list that the popup menu was issued on.
   * 
   */

  public void actionPerformed(ActionEvent e)
  {
    if (allowCallback)
      {
	if (e.getSource() instanceof JMenuItem)
	  {
	    String string = ((JMenuItem)e.getSource()).getActionCommand();
	    Object popSelectedItem = null;

	    try
	      {
		popSelectedItem = ((listHandle) model.elementAt(popUpIndex)).getObject();
	      }
	    catch (ArrayIndexOutOfBoundsException ex)
	      {
		popSelectedItem = null;
	      }

	    if (debug)
	      {
		System.err.println("Forwarding selected item.. (" + popUpIndex + ": " +
				   popSelectedItem + ")");
	      }

	    try
	      {
		my_parent.setValuePerformed(new JValueObject(this,
							     popUpIndex,
							     JValueObject.PARAMETER,
							     popSelectedItem,
							     string));
	      }
	    catch (java.rmi.RemoteException rx)
	      {
		throw new RuntimeException("Could not set value from JstringListBox: " + rx);
	      }
	  }
      }
  }
  
  /**
   * sort a vector of listHandles
   *
   * @param v Vector to be sorted
   * @return Vector of sorted listHandles(sorted by label)
   */

  private Vector sortListHandleVector(Vector v)
  {
    (new VecQuickSort(v, 
		      new arlut.csd.Util.Compare() {
      public int compare(Object a, Object b) 
	{
	  listHandle aF, bF;
	  
	  aF = (listHandle) a;
	  bF = (listHandle) b;
	  int comp = 0;
	  
	  comp =  aF.toString().compareTo(bF.toString());
	  
	  if (comp < 0)
	    {
	      return -1;
	    }
	  else if (comp > 0)
	    { 
	      return 1;
	    } 
	  else
	    { 
	      return 0;
	    }
	}
    })).sort();
    
    return v;
  }

  /**
   * Sort a vector of Strings
   *
   * @return Vector of sorted Strings.
   */

  public Vector sortStringVector(Vector v)
  {
    (new VecQuickSort(v, 
		      new arlut.csd.Util.Compare() {
      public int compare(Object a, Object b) 
	{
	  String aF, bF;
	  
	  aF = (String) a;
	  bF = (String) b;
	  int comp = 0;
	  
	  comp =  aF.compareTo(bF);
	  
	  if (comp < 0)
	    {
	      return -1;
	    }
	  else if (comp > 0)
	    { 
	      return 1;
	    } 
	  else
	    { 
	      return 0;
	    }
	}
    })).sort();
    
    return v;
  }

}

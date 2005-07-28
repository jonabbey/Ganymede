/*

   ownerPanel.java

   The ownershipPanel is used in the Ganymede client to display
   objects owned when the user opens a Ganymede Owner Group window.
   
   Created: 9 September 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import arlut.csd.JDataComponent.JAddValueObject;
import arlut.csd.JDataComponent.JAddVectorValueObject;
import arlut.csd.JDataComponent.JDeleteValueObject;
import arlut.csd.JDataComponent.JDeleteVectorValueObject;
import arlut.csd.JDataComponent.JParameterValueObject;
import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDataComponent.StringSelector;
import arlut.csd.JDataComponent.TimedKeySelectionManager;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.db_field;
import arlut.csd.ganymede.rmi.db_object;
import arlut.csd.ganymede.rmi.invid_field;
import arlut.csd.ganymede.rmi.Session;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  ownershipPanel

------------------------------------------------------------------------------*/

/**
 * <P>The ownershipPanel is used in the Ganymede client to display objects owned
 * when the user opens a Ganymede Owner Group window.</P>
 */

public class ownershipPanel extends JPanel implements ItemListener {

  final static boolean debug = false;

  boolean
    editable;
  
  framePanel
    parent;

  gclient
    gc;

  JPanel
    center;

  JComboBox
    bases;

  Hashtable
    objects_owned,   // (Short)Base type -> (Vector)list of objects [all objects]
    paneHash;        // (String) base name -> objectPane holding base objects

  CardLayout
    cards;

  Vector
    owners = null,
    result = null;

  JPanel
    holder;

  QueryDataNode
    node;

  /* -- */

  public ownershipPanel(boolean editable, framePanel parent)
  {
    this.editable = editable;
    this.parent = parent;

    gc = parent.wp.gc;

    setLayout(new BorderLayout());

    holder = new JPanel();
    holder.add(new JLabel("Loading ownershipPanel."));
    add("Center", holder);

    cards = new CardLayout();
    center = new JPanel(false);
    center.setLayout(cards);

    // Build the combo box from the baseList
    JPanel bp = new JPanel(false);
    bases = new JComboBox();
    bases.setKeySelectionManager(new TimedKeySelectionManager());
    bp.add(new JLabel("Object type:"));
    bp.add(bases);

    Vector baseList = gc.getBaseList();
    Hashtable baseNames = gc.getBaseNames();
    Hashtable baseToShort = gc.getBaseToShort();
    paneHash = new Hashtable();

    try
      {
	for (int i = 0; i < baseList.size(); i++)
	  {
	    Base b = (Base)baseList.elementAt(i);

	    if (!b.isEmbedded())
	      {
		String name = (String)baseNames.get(b);
		bases.addItem (name);
		objectPane p = new objectPane(editable, 
					      this,
					      ((Short)baseToShort.get(b)).shortValue());
		paneHash.put(name, p);
		center.add(name, p);
	      }
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("could not load the combobox: " + rx);
      }

    bases.addItemListener(this);

    remove(holder);
    add("North", bp);
    add("Center", center);

    invalidate();
    parent.validate();

    JPanel emptyP = new JPanel();
    center.add("empty", emptyP);

    cards.show(center, "empty");
  }

  public void itemStateChanged(ItemEvent event)
  {
    if (event.getStateChange() == ItemEvent.SELECTED)
      {
	String item = (String)event.getItem();
	
	objectPane op = (objectPane) paneHash.get(item);
	
	if (!op.isStarted())
	  {
	    Thread thread = new Thread(op);
	    thread.setPriority(Thread.NORM_PRIORITY);
	    thread.start();
	  }
	
	cards.show(center, item);
      }
  }

  private void println(String s)
  {
    System.out.println("OwnershipPanel: " + s);
  }

  public void dispose()
  {
    parent = null;
    gc = null;
    removeAll();

    if (center != null)
      {
	center.removeAll();
	center = null;
      }

    bases = null;

    if (objects_owned != null)
      {
	objects_owned.clear();
	objects_owned = null;
      }

    if (paneHash != null)
      {
	paneHash.clear();
	paneHash = null;
      }

    cards = null;

    if (owners != null)
      {
	owners.clear();
	owners = null;
      }

    if (result != null)
      {
	result.clear();
	result = null;
      }

    if (holder != null)
      {
	holder.removeAll();
	holder = null;
      }

    node = null;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      objectPane

------------------------------------------------------------------------------*/

/**
 * <P>The objectPane class is a JPanel subclass used in the Ganymede client
 * to display a list of objects of a given type contained in a Ganymede Owner
 * Group.</P>
 */

class objectPane extends JPanel implements JsetValueCallback, Runnable {

  final static boolean debug = false;

  // ---

  private
    boolean stringSelector_loaded = false;

  private
    StringSelector ss;

  private
    boolean editable;

  private
    Vector owned, possible;

  private
    short type;

  private
    QueryResult result;

  private
    ownershipPanel parent;

  private 
    JPanel filler;

  private 
    boolean isStarted = false;

  private
    gclient gc;

  /* -- */

  // Most of the work is in the create() method, only called after this panel is shown

  public objectPane(boolean editable, ownershipPanel parent, short type)
  {
    this.editable = editable;
    this.type = type;
    this.parent = parent;

    gc = parent.gc;

    setLayout(new BorderLayout());
    filler = new JPanel();
    filler.add(new JLabel("Creating panel, please wait."));

    add("Center", filler);
  }

  public boolean isStarted()
  {
    return isStarted;
  }

  public void run()
  {
    objectList
      list = null;

    /* -- */

    isStarted = true;

    // Get the list of selected choices

    try
      {
	QueryResult qResult;

	// go back to the framePanel to get the invid for this owner
	// group

	QueryDataNode node = new QueryDataNode(SchemaConstants.OwnerListField,
					       QueryDataNode.EQUALS, 
					       QueryDataNode.CONTAINS, 
					       parent.parent.getObjectInvid());

	qResult = gc.getSession().query(new Query(type, node));	// no filtering

	owned = new objectList(qResult).getListHandles(false);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get Query: " + rx);
      }

    // Get the list of possible objects

    Short key = new Short(type);

    try
      {
	if (gc.cachedLists.containsList(key))
	  {
	    list = gc.cachedLists.getList(key);
	    possible = list.getListHandles(false);
	  }
	else
	  {
	    gc.setStatus("Downloading list of owned objects.");

	    result = gc.getSession().query(new Query(type)); // no filtering

	    list = new objectList(result);
	    possible = list.getListHandles(false);
    
	    gc.cachedLists.putList(key, list);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get QueryResult for all objects: " + rx);
      }

    ss = new StringSelector(this, editable, true, true);

    ss.update(possible, true, null, owned, true, null);
    ss.setCellWidth((possible != null && editable) ? 150 : 300);
    ss.setTitles("Selected", "Available");

    // we need to create two separate pop up menus so the
    // StringSelector can attach them to its two
    // arlut.csd.JDataComponent.JstringListBox components without
    // having each listen to the other's events.
    
    JPopupMenu invidTablePopup = new JPopupMenu();
    JMenuItem viewO = new JMenuItem("View object");
    JMenuItem editO = new JMenuItem("Edit object");
    invidTablePopup.add(viewO);
    invidTablePopup.add(editO);

    JPopupMenu invidTablePopup2 = new JPopupMenu();
    JMenuItem viewO2 = new JMenuItem("View object");
    JMenuItem editO2 = new JMenuItem("Edit object");
    invidTablePopup2.add(viewO2);
    invidTablePopup2.add(editO2);

    ss.setPopups(invidTablePopup, invidTablePopup2);
    ss.setCallback(this);
    remove(filler);
    add("Center", ss);
    
    invalidate();
    parent.validate();
    stringSelector_loaded = true;

    gc.setStatus("Done.");
  }

  public boolean isCreated()
  {
    return stringSelector_loaded;
  }

  /**
   *
   * Callback for our stringSelector
   *
   */

  public boolean setValuePerformed(JValueObject e)
  {
    ReturnVal retVal;
    boolean succeeded = false;

    /* -- */

    // First, are we being given a menu operation from StringSelector?
    
    if (e instanceof JParameterValueObject)
      {
	if (debug)
	  {
	    println("MenuItem selected in a StringSelector");
	  }

	String command = (String) e.getParameter();

	if (command.equals("Edit object"))
	  {
	    if (debug)
	      {
		println("Edit object: " + e.getValue());
	      }

	    Invid invid = (Invid) e.getValue();
		    
	    gc.editObject(invid);

	    return true;
	  }
	else if (command.equals("View object"))
	  {
	    if (debug)
	      {
		println("View object: " + e.getValue());
	      }

	    Invid invid = (Invid) e.getValue();
		    
	    gc.viewObject(invid);

	    return true;
	  }
	else
	  {
	    println("Unknown action command from popup: " + command);
	  }
      }
    else if (e instanceof JAddValueObject)
      {
	if (debug)
	  {
	    println("Adding object to list");
	  }

	try
	  {
	    retVal = addToOwnerGroup((Invid) e.getValue());

	    if (retVal != null)
	      {
		gc.handleReturnVal(retVal);
	      }

	    succeeded = (retVal == null) ? true : retVal.didSucceed();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not add value to list: " + rx);
	  }
      }
    else if (e instanceof JAddVectorValueObject)
      {
	if (debug)
	  {
	    println("Adding objects to list");
	  }

	try
	  {
	    retVal = addToOwnerGroup((Vector)e.getValue());

	    if (retVal != null)
	      {
		gc.handleReturnVal(retVal);
	      }

	    succeeded = (retVal == null) ? true : retVal.didSucceed();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not add values to list: " + rx);
	  }
      }
    else if (e instanceof JDeleteValueObject)
      {
	if (debug)
	  {
	    println("Deleting object from list");
	  }

	try
	  {
	    retVal = removeFromOwnerGroup((Invid) e.getValue());

	    if (retVal != null)
	      {
		gc.handleReturnVal(retVal);
	      }

	    succeeded = (retVal == null) ? true : retVal.didSucceed();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not delete value from list: " + rx);
	  }
      }
    else if (e instanceof JDeleteVectorValueObject)
      {
	if (debug)
	  {
	    println("Adding objects to list");
	  }

	try
	  {
	    retVal = removeFromOwnerGroup((Vector)e.getValue());

	    if (retVal != null)
	      {
		gc.handleReturnVal(retVal);
	      }

	    succeeded = (retVal == null) ? true : retVal.didSucceed();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not remove values from list: " + rx);
	  }
      }

    if (debug)
      {
	println("returnValue = " + succeeded);
      }
    
    if (succeeded)
      {
	gc.somethingChanged();
      }
    
    return succeeded;
  }

  /**
   * <p>This private helper method attempts to edit the object whose
   * Invid is provided.  If successful, it will add the Invid for the
   * owner group we are attached to to the Owner List Field.</p>
   */

  private ReturnVal addToOwnerGroup(Invid objectToAdd) throws RemoteException
  {
    ReturnVal retVal = null;
    Session session = gc.getSession();

    retVal = session.edit_db_object(objectToAdd);

    if (!retVal.didSucceed())
      {
	return retVal;
      }

    db_object my_object = retVal.getObject();
    db_field my_field = my_object.getField(SchemaConstants.OwnerListField);

    retVal = my_field.addElement(parent.parent.getObjectInvid());

    return retVal;
  }

  /**
   * <p>This private helper method attempts to edit the objects whose
   * Invid are provided in the Vector parameter.  If successful, it
   * will add the Invid for the owner group we are attached to to the
   * Owner List Field for these objects.</p>
   *
   * <p>If a failure is encountered while we are looping over the
   * vector of objects to add, we will return an error message and
   * revert the objects we've already added.</p>
   */

  private ReturnVal addToOwnerGroup(Vector objectsToAdd) throws RemoteException
  {
    ReturnVal retVal = null;
    Session session = gc.getSession();
    int i;
    boolean success = true;

    for (i = 0; success && i < objectsToAdd.size(); i++)
      {
	Invid objectToAdd = (Invid) objectsToAdd.elementAt(i);

	retVal = session.edit_db_object(objectToAdd);

	if (!retVal.didSucceed())
	  {
	    success = false;
	    break;
	  }

	db_object my_object = retVal.getObject();
	db_field my_field = my_object.getField(SchemaConstants.OwnerListField);

	gc.setStatus("Adding object " + my_object.getLabel() + " to owner group.");

	retVal = my_field.addElement(parent.parent.getObjectInvid());

	if (retVal != null && !retVal.didSucceed())
	  {
	    success = false;
	  }
      }

    if (!success)
      {
	gc.setStatus("Error encountered adding objects to owner group.  Reverting.", 0);

	// we couldn't add all of these objects to the owner group.
	// Go ahead and revert all the ones we successfully added.

	for (int j = 0; j < i; j++)
	  {
	    Invid objectToAdd = (Invid) objectsToAdd.elementAt(j);

	    ReturnVal retVal2 = session.edit_db_object(objectToAdd);

	    if (!retVal2.didSucceed())
	      {
		// weird!  go ahead and try to undo the rest
		continue;
	      }
	    
	    db_object my_object = retVal2.getObject();
	    db_field my_field = my_object.getField(SchemaConstants.OwnerListField);

	    // we won't bother checking for success, if we fail here,
	    // we've got big problems.

	    my_field.deleteElement(parent.parent.getObjectInvid());
	  }

	// clear the status display

	gc.setStatus("");

	// and return our original error message

	return retVal;
      }

    return null;		// success
  }

  /**
   * <p>This private helper method attempts to edit the object whose
   * Invid is provided.  If successful, it will remove the Invid for the
   * owner group we are attached to from the Owner List Field.</p>
   */

  private ReturnVal removeFromOwnerGroup(Invid objectToRemove) throws RemoteException
  {
    ReturnVal retVal = null;
    Session session = gc.getSession();

    retVal = session.edit_db_object(objectToRemove);

    if (!retVal.didSucceed())
      {
	return retVal;
      }

    db_object my_object = retVal.getObject();
    db_field my_field = my_object.getField(SchemaConstants.OwnerListField);

    retVal = my_field.deleteElement(parent.parent.getObjectInvid());

    return retVal;
  }

  /**
   * <p>This private helper method attempts to edit the objects whose
   * Invid are provided in the Vector parameter.  If successful, it
   * will remove the Invid for the owner group we are attached to from
   * the Owner List Field for these objects.</p>
   *
   * <p>If a failure is encountered while we are looping over the
   * vector of objects to add, we will return an error message and
   * revert the objects we've already removed.</p>
   */

  private ReturnVal removeFromOwnerGroup(Vector objectsToRemove) throws RemoteException
  {
    ReturnVal retVal = null;
    Session session = gc.getSession();
    int i;
    boolean success = true;

    for (i = 0; success && i < objectsToRemove.size(); i++)
      {
	Invid objectToRemove = (Invid) objectsToRemove.elementAt(i);

	retVal = session.edit_db_object(objectToRemove);

	if (!retVal.didSucceed())
	  {
	    success = false;
	    break;
	  }

	db_object my_object = retVal.getObject();
	db_field my_field = my_object.getField(SchemaConstants.OwnerListField);

	gc.setStatus("Removing object " + my_object.getLabel() + " from owner group.");

	retVal = my_field.deleteElement(parent.parent.getObjectInvid());

	if (retVal != null && !retVal.didSucceed())
	  {
	    success = false;
	  }
      }

    if (!success)
      {
	// we couldn't remove all of these objects to the owner group.
	// Go ahead and try to revert all the ones we successfully
	// removed.

	gc.setStatus("Error encountered removing objects from owner group.  Reverting.", 0);

	for (int j = 0; j < i; j++)
	  {
	    Invid objectToRemove = (Invid) objectsToRemove.elementAt(j);

	    ReturnVal retVal2 = session.edit_db_object(objectToRemove);

	    if (!retVal2.didSucceed())
	      {
		// weird!  go ahead and try to undo the rest
		continue;
	      }
	    
	    db_object my_object = retVal2.getObject();
	    db_field my_field = my_object.getField(SchemaConstants.OwnerListField);

	    // we won't bother checking for success, if we fail here,
	    // we've got big problems.

	    my_field.addElement(parent.parent.getObjectInvid());
	  }

	gc.setStatus("");

	// and return our original error message

	return retVal;
      }

    return null;		// success
  }

  private void println(String s)
  {
    System.out.println("OwnershipPanel.objectPane: " + s);
  }
}

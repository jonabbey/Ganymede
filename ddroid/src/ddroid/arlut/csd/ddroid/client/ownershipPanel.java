/*

   ownerPanel.java

   The ownershipPanel is used in the Directory Droid client to display
   objects owned when the user opens a Directory Droid Owner Group window.
   
   Created: 9 September 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
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

package arlut.csd.ddroid.client;

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
import arlut.csd.ddroid.common.Invid;
import arlut.csd.ddroid.common.Query;
import arlut.csd.ddroid.common.QueryDataNode;
import arlut.csd.ddroid.common.QueryResult;
import arlut.csd.ddroid.common.ReturnVal;
import arlut.csd.ddroid.common.SchemaConstants;
import arlut.csd.ddroid.rmi.Base;
import arlut.csd.ddroid.rmi.invid_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  ownershipPanel

------------------------------------------------------------------------------*/

/**
 * <P>The ownershipPanel is used in the Directory Droid client to display objects owned
 * when the user opens a Directory Droid Owner Group window.</P>
 */

public class ownershipPanel extends JPanel implements ItemListener {

  final static boolean debug = false;

  invid_field
    field;

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

  public ownershipPanel(invid_field field, boolean editable, framePanel parent)
  {
    this.field = field;
    this.editable = editable;
    this.parent = parent;

    gc = parent.wp.gc;
    //    debug = gc.debug;

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

    Vector baseList = parent.getgclient().getBaseList();
    Hashtable baseNames = parent.getgclient().getBaseNames();
    Hashtable baseToShort = parent.getgclient().getBaseToShort();
    paneHash = new Hashtable();

    try
      {
	for (int i = 0; i < baseList.size(); i++)
	  {
	    Base b = (Base)baseList.elementAt(i);

	    if (b.isEmbedded())
	      {
		if (debug)
		  {
		    println("Skipping embedded field");
		  }
	      }
	    else
	      {
		String name = (String)baseNames.get(b);
		bases.addItem (name);
		objectPane p = new objectPane(editable, 
					      this,
					      ((Short)baseToShort.get(b)).shortValue(),
					      field);
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

    if (debug)
      {
	println("Done in thread, she's loaded!");
      }

    JPanel emptyP = new JPanel();
    center.add("empty", emptyP);

    cards.show(center, "empty");
  }

  public void itemStateChanged(ItemEvent event)
  {
    if (event.getStateChange() == ItemEvent.DESELECTED)
      {
	if (debug)
	  {
	    println("I DON'T CARE IF YOU ARE DESELECTED!");
	  }
      }
    else if (event.getStateChange() == ItemEvent.SELECTED)
      {
	String item = (String)event.getItem();
	
	if (debug)
	  {
	    println("Item selected: " + item);
	  }
	
	objectPane op = (objectPane) paneHash.get(item);
	
	if (!op.isStarted())
	  {
	    if (debug) 
	      {
		println("starting new thread");
	      }

	    parent.getgclient().setStatus("Downloading objects for this base");
	    Thread thread = new Thread(op);
	    thread.start();
	  }
	else if (debug)
	  {
	    println("thread already started?");
	  }
	
	cards.show(center, item);
      }
    else
      {
	if (debug)
	  {
	    println("What the hell kind of item event is this? " + event);
	  }
      }
  }

  private void println(String s)
  {
    System.out.println("OwnershipPanel: " + s);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      objectPane

------------------------------------------------------------------------------*/

/**
 * <P>The objectPane class is a JPanel subclass used in the Directory Droid client
 * to display a list of objects of a given type contained in a Directory Droid Owner
 * Group.</P>
 */

class objectPane extends JPanel implements JsetValueCallback, Runnable {

  final static boolean debug = false;

  boolean
    stringSelector_loaded = false;

  StringSelector 
    ss;

  boolean
    editable;

  Vector
    owned = null,
    possible;

  short
    type;

  QueryResult 
    result;

  ownershipPanel
    parent;

  invid_field
    field;

  JPanel
    filler;

  boolean
    isStarted = false;

  gclient
    gc;

  /* -- */

  // Most of the work is in the create() method, only called after this panel is shown

  public objectPane(boolean editable, ownershipPanel parent, short type, invid_field field)
  {
    this.field = field;
    this.editable = editable;
    this.type = type;
    this.parent = parent;

    gc = parent.gc;
    //    debug = gc.debug;

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

    if (debug)
      {
	println("Loading one of the panels");
      }

    // Get the list of selected choices

    try
      {
	QueryResult qResult;

	// go back to the framePanel to get the invid

	QueryDataNode node = new QueryDataNode(SchemaConstants.OwnerListField,
					       QueryDataNode.EQUALS, 
					       QueryDataNode.CONTAINS, 
					       parent.parent.getObjectInvid());

	qResult = parent.parent.getgclient().getSession().query(new Query(type, node));	// no filtering

	if (debug)
	  {
	    if (qResult == null)
	      {
		println("Hey, the qResult is null.");
	      }
	    else
	      {
		println("Found " + qResult.size() + " matching items.");
	      }
	  }
	
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
	if ((key != null) && (parent.parent.getgclient().cachedLists.containsList(key)))
	  {
	    if (debug)
	      {
		println("using cached copy");
	      }

	    list = parent.parent.getgclient().cachedLists.getList(key);

	    possible = list.getListHandles(false);
	  }
	else
	  {
	    parent.parent.getgclient().setStatus("Downloading list of all objects.");

	    result = parent.parent.getgclient().getSession().query(new Query(type)); // no filtering

	    list = new objectList(result);
	    possible = list.getListHandles(false);
    
	    if (key != null)
	      {
		if (debug)
		  {
		    println("Adding new key to cachedList: " + key);
		  }

		parent.parent.getgclient().cachedLists.putList(key, list);
	      }
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get QueryResult for all objects: " + rx);
      }
    
    if (debug)
      {
	if (owned != null)
	  {
	    println("Creating string selector: owned: " + owned.size());
	  }

	if (possible != null)
	  {
	    println(" possible: " + possible.size());
	  }

	if ((owned == null) && (possible == null))
	  {
	    println("Both owned and possible are null");
	  }
      }

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

    ss = new StringSelector(this, editable, true, true);

    ss.update(possible, true, null, owned, true, null);
    ss.setCellWidth((possible != null && editable) ? 150 : 300);
    ss.setTitles("Selected", "Available");
    ss.setPopups(invidTablePopup, invidTablePopup2);

    if (debug)
      {
	println("Done making StringSelector.");
      }

    ss.setCallback(this);
    remove(filler);
    add("Center", ss);
    
    invalidate();
    parent.validate();
    stringSelector_loaded = true;

    if (debug)
      {
	println("Done with thread, panel is loaded.");
      }

    parent.parent.getgclient().setStatus("Done.");
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
	    retVal = field.addElement((Invid)e.getValue());

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
	    retVal = field.addElements((Vector)e.getValue());

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
	    retVal = field.deleteElement(e.getValue());

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
	    retVal = field.deleteElements((Vector)e.getValue());

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

  private void println(String s)
  {
    System.out.println("OwnershipPanel.objectPane: " + s);
  }
}

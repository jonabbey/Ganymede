/*

   ownerPanel.java

   The individual frames in the windowPanel.
   
   Created: 9 September 1997
   Release: $Name:  $
   Version: $Revision: 1.16 $
   Last Mod Date: $Date: 1999/04/14 19:04:40 $
   Module By: Michael Mulvaney

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

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import javax.swing.*;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  ownershipPanel

------------------------------------------------------------------------------*/

public class ownershipPanel extends JPanel implements ItemListener {

  boolean debug = true;

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

  public ownershipPanel(invid_field field, boolean editable, framePanel parent)
  {
    this.field = field;
    this.editable = editable;
    this.parent = parent;

    gc = parent.wp.gc;
    debug = gc.debug;

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
    println("Done in thread, she's loaded!");

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

class objectPane extends JPanel implements JsetValueCallback, Runnable{

  boolean debug = true;

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
    debug = gc.debug;

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

    println("Loading one of the panels");

    // Get the list of selected choices

    try
      {
	QueryResult qResult;
	db_object object = parent.parent.object;

	// go back to the framePanel to get the invid

	QueryDataNode node = new QueryDataNode(SchemaConstants.OwnerListField,
					       QueryDataNode.EQUALS, 
					       QueryDataNode.CONTAINS, 
					       parent.parent.getObjectInvid());

	qResult = parent.parent.getgclient().getSession().query(new Query(type, node));

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

	    result = parent.parent.getgclient().getSession().query(new Query(type));

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

    ss = new StringSelector(possible, owned, this, editable, true, true);

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

    if (e.getOperationType() == JValueObject.ADD)
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
    else if (e.getOperationType() == JValueObject.DELETE)
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

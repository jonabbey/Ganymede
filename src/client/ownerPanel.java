/*

   ownerPanel.java

   The individual frames in the windowPanel.
   
   Created: 9 September 1997
   Release: $Name:  $
   Version: $Revision: 1.23 $
   Last Mod Date: $Date: 1999/01/22 18:04:17 $
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
import java.rmi.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.*;

public class ownerPanel extends JPanel implements JsetValueCallback, Runnable {

  boolean debug = true;

  boolean
    editable;

  invid_field
    field;

  framePanel
   fp;

  gclient
    gc;

  JPanel
    holdOnPanel;

  /* -- */

  public ownerPanel(invid_field field, boolean editable, framePanel fp)
  {
    this.editable = editable;
    this.field = field;
    this.fp = fp;

    gc = fp.wp.gc;
    debug = gc.debug;

    if (debug)
      {
	System.out.println("Adding ownerPanel");
      }

    setLayout(new BorderLayout());

    setBorder(new EmptyBorder(new Insets(5,5,5,5)));

    holdOnPanel = new JPanel();
    holdOnPanel.add(new JLabel("Loading ownerPanel, please wait.", 
			       new ImageIcon(fp.getWaitImage()), 
			       SwingConstants.CENTER));
      
    add("Center", holdOnPanel);
    invalidate();
    fp.validate();

    // spin off a thread to load the panel.
    
    Thread thread = new Thread(this);
    thread.start();
  }

  public void run()
  {
    if (debug)
      {
	System.out.println("Starting new thread");
      }

    if (field == null)
      {
	// we'll only get this if we are view-only and there are no owners
	// for this object registered.. in this case, the object is
	// effectively owned by supergash.

	if (debug)
	  {
	    System.out.println("ownerPanel: field is null, there is no owner for this object.");
	  }

	JLabel l = new JLabel("Owned by supergash",
			      SwingConstants.CENTER);

	remove(holdOnPanel);
	add("Center", l);
      }
    else
      {
	if (debug)
	  {
	    System.out.println("ownerPanel: field is not null, creating invid selector.");
	  }

	try
	  {
	    StringSelector ownerList = createInvidSelector(field);
	    ownerList.setBorder(new LineBorder(Color.black));
	    ownerList.setVisibleRowCount(-1);
	    remove(holdOnPanel);
	    add("Center", ownerList);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not addDateField: " + rx);
	  }
      }

    invalidate();
    fp.validate();
  }

  private StringSelector createInvidSelector(invid_field field) throws RemoteException
  {
    QueryResult
      results,
      choiceResults = null;

    Vector
      currentOwners = null,
      availableOwners = null;

    objectList
      list;

    /* -- */

    if (debug)
      {
	System.out.println("Adding StringSelector, its a vector of invids!");
      }

    if (editable)
      {
	if (debug)
	  {
	    System.out.println("ownerPanel: It's editable, I'm getting the list of choices.");
	  }

	Object key = field.choicesKey();
	
	if ((key != null) && (fp.getgclient().cachedLists.containsList(key)))
	  {
	    if (debug)
	      {
		System.out.println("Using cached copy...");
	      }

	    availableOwners = fp.getgclient().cachedLists.getListHandles(key, false);
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("Downloading copy");
	      }

	    QueryResult choices = field.choices();

	    if (choices != null)
	      {
		list = new objectList(choices);
	      
		
		availableOwners = list.getListHandles(false);
		
		if (key != null)
		  {
		    if (debug)
		      {
			System.out.println("Saving this under key: " + key);
		      }
		    
		    fp.getgclient().cachedLists.putList(key, list);
		  }
	      }
	  }
      }

    currentOwners = field.encodedValues().getListHandles();

    // availableOwners might be null
    // if editable is false, availableOwners will be null

    JPopupMenu invidTablePopup = new JPopupMenu();
    JMenuItem viewO = new JMenuItem("View object");
    JMenuItem editO = new JMenuItem("Edit object");
    JMenuItem createO = new JMenuItem("Create new Object");
    invidTablePopup.add(viewO);
    invidTablePopup.add(editO);
    invidTablePopup.add(createO);

    JPopupMenu invidTablePopup2 = new JPopupMenu();
    JMenuItem viewO2 = new JMenuItem("View object");
    JMenuItem editO2 = new JMenuItem("Edit object");
    JMenuItem createO2 = new JMenuItem("Create new Object");
    invidTablePopup2.add(viewO2);
    invidTablePopup2.add(editO2);
    invidTablePopup2.add(createO2);

    // We don't want the supergash owner group to show up anywhere,
    // because everything is owned by supergash.

    if (debug)
      {
	System.out.println("ownerPanel: Taking out supergash");
      }

    if (availableOwners != null)
      {
	Invid supergash = new Invid((short)0, 1); // This is supergash

	for (int i = 0; i < availableOwners.size(); i++)
	  {
	    listHandle l = (listHandle)availableOwners.elementAt(i);

	    if (supergash.equals(l.getObject()))
	      {
		availableOwners.removeElementAt(i);
		break;
	      }
	  }
      }

    if (debug)
      {
	System.out.println("ownerPanel: creating string selector");
      }

    StringSelector ss = new StringSelector(availableOwners, 
					   currentOwners, 
					   this, editable, 
					   true, true, 10,
					   "Owners", "Owner Groups",
					   invidTablePopup,
					   invidTablePopup2);
    if (editable)
      {
	ss.setCallback(this);
      }

    return ss;
  }

  public boolean setValuePerformed(JValueObject o)
  {
    ReturnVal retVal;
    boolean succeeded = false;

    /* -- */

    if (!(o.getSource() instanceof StringSelector))
      {
	System.out.println("Where did this setValuePerformed come from?");
	return false;
      }

    if (o.getOperationType() == JValueObject.ERROR)
      {
	fp.getgclient().setStatus((String)o.getValue());
      }
    else if (o.getOperationType() == JValueObject.PARAMETER) 
      {  // From the popup menu
	
	JValueObject v = o; // because this code was originally used with a v
	String command = (String)v.getParameter();
	  
	if (command.equals("Edit object"))
	  {
	    if (debug)
	      {
		System.out.println("Edit object: " + v.getValue());
	      }
		
	    if (v.getValue() instanceof listHandle)
	      {
		Invid invid = (Invid)((listHandle)v.getValue()).getObject();
		    
		gc.editObject(invid);
	      }
	    else if (v.getValue() instanceof Invid)
	      {
		if (debug)
		  {
		    System.out.println("It's an invid!");
		  }
		    
		Invid invid = (Invid)v.getValue();
		    
		gc.editObject(invid);
	      }
		
	    retVal = null;
	  }
	else if (command.equals("View object"))
	  {
	    if (debug)
	      {
		System.out.println("View object: " + v.getValue());
	      }
		
	    if (v.getValue() instanceof Invid)
	      {
		Invid invid = (Invid)v.getValue();
		    
		gc.viewObject(invid);
	      }
		
	    retVal = null;
	  }
	else if (command.equals("Create new Object"))
	  {
	    String label = null;

	    try
	      {
		short type = field.getTargetBase();
		db_object obj = gc.createObject(type, false);
		    
		// Find the label field.
		db_field f = obj.getLabelField();

		if ((f != null) && (f instanceof string_field))
		  {
		    if (debug)
		      {
			System.out.println("Going to get label for this object.");
		      }

		    // Set up a label for this object.

		    DialogRsrc r = new DialogRsrc(gc, "Choose Label for Object", 
						  "What would you like to name this object?",
						  "Ok", null);
		    r.addString("Label:");
		    
		    StringDialog d = new StringDialog(r);
		    Hashtable result = d.DialogShow();
		    ReturnVal setLabel = f.setValue((String)result.get("Label:"));
		    
		    // wizard?
		    
		    setLabel = gc.handleReturnVal(setLabel);
		    
		    if ((setLabel == null) || setLabel.didSucceed())
		      {
			label = (String)result.get("Label:");
			
			if (debug)
			  {
			    System.out.println("The set label worked!");
			  }
		      }
		    else
		      {
			if (debug)
			  {
			    System.out.println("set label failed!!!!");
			  }
		      }
		  }
		
		Invid invid = obj.getInvid();
		retVal = field.addElement(invid);
		
		if (retVal != null)
		  {
		    gc.handleReturnVal(retVal);
		  }
		    
		gc.showNewlyCreatedObject(obj, invid, new Short(type));
		    
		if ((retVal == null) || retVal.didSucceed())
		  {
		    if (debug)
		      {
			System.out.println("--Adding it to the StringSelector");
		      }
			
		    if (label == null)
		      {
			((StringSelector)v.getSource()).addNewItem(new listHandle("New item", invid), true);
		      }
		    else
		      {
			((StringSelector)v.getSource()).addNewItem(new listHandle(label, invid), true);
		      }
		  }
		else
		  {
		    System.out.println("--Something went wrong, so I am NOT adding it to the StringSelector");
		  }
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Exception creating new object from SS menu: " + rx);
	      }
	  }	    
	else
	  {
	    System.out.println("Unknown action command from popup: " + command);
	  }
      } // end of popup processing, now it's just an add or remove kind of thing.
    else if (o.getValue() instanceof Invid)
      {
	Invid invid = (Invid)o.getValue();
	int index = o.getIndex();
	
	try
	  {
	    if (o.getOperationType() == JValueObject.ADD)
	      {
		retVal = field.addElement(invid);

		if (retVal != null)
		  {
		    gc.handleReturnVal(retVal);
		  }

		succeeded = (retVal == null) ? true : retVal.didSucceed();

	      }
	    else if (o.getOperationType() == JValueObject.DELETE)
	      {
		retVal = field.deleteElement(invid);

		if (retVal != null)
		  {
		    gc.handleReturnVal(retVal);
		  }

		succeeded = (retVal == null) ? true : retVal.didSucceed();
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not change owner field: " + rx);
	  }
      }
    
    if (succeeded)
      {
	fp.getgclient().somethingChanged();
      }
    
    return succeeded;
  }
}

/*

   ownerPanel.java

   The individual frames in the windowPanel.
   
   Created: 9 September 1997
   Release: $Name:  $
   Version: $Revision: 1.28 $
   Last Mod Date: $Date: 2001/07/05 22:25:52 $
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

/**
 * <p>GUI panel for displaying the list of owners for a given object in
 * the client.  This panel is created in association with the "Owners"
 * tab in framePanel.</p>
 *
 * @version $Revision: 1.28 $ $Date: 2001/07/05 22:25:52 $ $Name:  $
 * @author Mike Mulvaney
 */

public class ownerPanel extends JPanel implements JsetValueCallback, Runnable {

  boolean debug = false;

  /**
   * If true, we'll allow attempts to edit the owner list for this object.
   */

  boolean
    editable;

  /**
   * Reference to an object's owners field on the server.
   */

  invid_field
    field;

  /**
   * Object window we're contained in.
   */

  framePanel
   fp;

  /**
   * Reference to the client's gclient object, used for utility code.
   */

  gclient
    gc;

  /**
   * Panel to hold our working guy image while we download information from
   * the server.
   */

  JPanel
    holdOnPanel;

  /* -- */

  public ownerPanel(invid_field field, boolean editable, framePanel fp)
  {
    this.editable = editable;
    this.field = field;
    this.fp = fp;

    gc = fp.wp.gc;

    if (!debug)
      {
	debug = gc.debug;
      }

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
    invidTablePopup.add(viewO);
    invidTablePopup.add(editO);

    JPopupMenu invidTablePopup2 = new JPopupMenu();
    JMenuItem viewO2 = new JMenuItem("View object");
    JMenuItem editO2 = new JMenuItem("Edit object");
    invidTablePopup2.add(viewO2);
    invidTablePopup2.add(editO2);

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

    StringSelector ss = new StringSelector(this, editable, true, true);
    
    ss.setCellWidth(0);
    ss.setTitles("Owners", "Owner Groups");
    ss.setPopups(invidTablePopup, invidTablePopup2);
    ss.update(availableOwners, true, null, currentOwners, true, null);
    ss.setCallback(this);

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
	else
	  {
	    System.out.println("Unknown action command from popup: " + command);
	  }
      } // end of popup processing, now it's just an add or remove kind of thing.
    else
      {
	Invid invid = null;	// have to init for javac

	if (o.getValue() instanceof Invid)
	  {
	    invid = (Invid) o.getValue();
	  }
	
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
	    else if (o.getOperationType() == JValueObject.ADDVECTOR)
	      {
		retVal = field.addElements((Vector) o.getValue());

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
	    else if (o.getOperationType() == JValueObject.DELETEVECTOR)
	      {
		retVal = field.deleteElements((Vector) o.getValue());

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

/*

   personaPanel.java

   a panel for handling User's personae.
   
   Created: 6 October 1997
   Release: $Name:  $
   Version: $Revision: 1.24 $
   Last Mod Date: $Date: 2000/06/21 18:36:14 $
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

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import arlut.csd.ganymede.*; 
import arlut.csd.JDialog.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    personaPanel

------------------------------------------------------------------------------*/

public class personaPanel extends JPanel implements ActionListener, ChangeListener{
  
  boolean debug = false;

  framePanel
    fp;

  gclient
    gc;

  invid_field
    field;

  boolean
    editable;
  
  JButton
    add,
    delete;

  JTabbedPane
    middle;

  Vector
    personas = null;

  int 
    total,
    current = -1;

  Vector
    panels = new Vector();

  EmptyBorder
    empty = new EmptyBorder(new Insets(7,7,7,7));

  boolean
    fieldIsEditable = false;

  /* -- */

  public personaPanel(invid_field field, boolean editable, framePanel fp) 
  {
    this.field = field;
    this.editable = editable;
    this.fp = fp;

    gc = fp.wp.gc;
    debug = gc.debug;

    setLayout(new BorderLayout());
    
    try
      {
	fieldIsEditable = field.isEditable();
      }
    catch(RemoteException rx)
      {
	throw new RuntimeException("Could not call field.isEditable in personaPanel: " + rx);
      }

    if (editable && fieldIsEditable)
      {
	// Create the button panel for the bottom
	JPanel bottom = new JPanel(false);

	add = new JButton("Create");
	add.addActionListener(this);
	delete = new JButton("Delete");
	delete.addActionListener(this);

	bottom.add(add);
	bottom.add(delete);
	
	add("South", bottom);
      }

    // Create the middle, content pane
    middle = new JTabbedPane(JTabbedPane.TOP);

    JPanel middleP = new JPanel(new BorderLayout());
    middleP.setBorder(new TitledBorder("Personas"));
    middleP.add("Center", middle);

    add("Center", middleP);

    try
      {
	personas = field.getValues();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get values for persona field: " + rx);
      }

    total = personas.size();

    for (int i = 0; i< total; i++)
      {
	personaContainer pc = null;
	boolean thisOneEditable = false;

	try
	  {
	    thisOneEditable = editable && field.isEditable();

	    Invid thisInvid = (Invid)personas.elementAt(i);

	    if (thisOneEditable)
	      {
		ReturnVal rv = gc.handleReturnVal(gc.getSession().edit_db_object(thisInvid));
		db_object ob = (db_object) rv.getObject();

		if (ob == null)
		  {
		    if (debug)
		      {
			System.out.println("Whoa, got a null object(edit), trying to go to non-editable, cover me.");
		      }
		    
		    ReturnVal Vrv = gc.handleReturnVal(gc.getSession().view_db_object(thisInvid));
		    ob = (db_object) Vrv.getObject();
		    
		    if (ob == null)
		      {
			System.out.println("That didn't work...its still not giving me anything back.  Giving up.");
		      }
		    else
		      {
			pc = new personaContainer(thisInvid, false, this, ob); //Now I know it is not editable
		      }
		  }
		else
		  {
		    pc = new personaContainer(thisInvid, thisOneEditable, this, ob);
		  }
	      }
	    else
	      {
		ReturnVal rv = gc.handleReturnVal(gc.getSession().view_db_object(thisInvid));
		db_object ob = (db_object) rv.getObject();

		if (ob == null)
		  {
		    System.out.println("Whoa, got a null object(view), skipping.");
		  }
		else
		  {
		    pc = new personaContainer(thisInvid, thisOneEditable, this, ob);
		  }
	      }		
      	  }
	catch (RemoteException rx)
	  {
	    if (debug)
	      {
		gc.showErrorMessage("Could not check if the field is editable: " + rx);
	      }
	  }
	
	panels.addElement(pc);
	middle.addTab("Persona " + i, pc);

	Thread t = new Thread(pc);
	t.start();
      }

    // Show the first one(will just be a progress bar for now)

    middle.addChangeListener(this);

    if (total > 0)
      {
	middle.setSelectedIndex(0);
      }
  }

  public void actionPerformed(ActionEvent e)
  {
    ReturnVal retVal;

    /* -- */

    if (debug)
      {
	System.out.println(e.getActionCommand());
      }

    if (e.getActionCommand().equals("Create"))
      {
	gc.setWaitCursor();
	int index = middle.getTabCount();
	// Make sure the default owner is chosen
 	    
	try
	  {
	    if (!fp.getgclient().defaultOwnerChosen())
	      {
		fp.getgclient().chooseDefaultOwner(false);
	      }
	    
	    // Create the object
	    ReturnVal rv = fp.getgclient().handleReturnVal(fp.getgclient().getSession().create_db_object(SchemaConstants.PersonaBase));
	    db_object newObject = (db_object) rv.getObject();
	    Invid user = fp.getObjectInvid();

	    gc.somethingChanged();
	    
	    // Tell the user about the persona

	    fp.getObject().getField(SchemaConstants.UserAdminPersonae).addElement(newObject.getInvid());

	    // Tell the persona about the user

	    newObject.getField(SchemaConstants.PersonaAssocUser).setValue(user);
	    
	    personaContainer pc = new personaContainer(newObject.getInvid(), editable, this, newObject);
	    middle.addTab("New Persona " + index, pc);

	    panels.addElement(pc);

	    pc.run();

	    //Thread t = new Thread(pc);
	    //t.start();

	    pc.waitForLoad();
	    
	    if (debug)
	      {
		System.out.println("Showing: " + index);
	      }
	    
	    middle.setSelectedIndex(index);
	  }
	catch (NullPointerException ne)
	  {
	    gc.showErrorMessage("You don't have permission to create objects of this type.");
	    add.setEnabled(false);
	    gc.setNormalCursor();
	    return;
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not create new persona: " + rx);
	  }

	gc.setNormalCursor();
      }
    else if (e.getActionCommand().equals("Delete"))
      {
	gc.setWaitCursor();
	boolean removed = false;
	boolean deleted = false;

	personaContainer pc = (personaContainer)panels.elementAt(middle.getSelectedIndex());

	Invid invid = pc.getInvid();

	if (invid == null) 
	  {
	    throw new NullPointerException("invid is null");
	  }

	StringDialog d = new StringDialog(gc, 
					  "Confirm deletion",
					  "Are you sure you want to delete persona " + 
					  middle.getTitleAt(middle.getSelectedIndex()) + "?",
					  true);

	gc.setNormalCursor();

	if (d.DialogShow() == null)
	  {
	    if (debug)
	      {
		System.out.println("Cancelled.");
	      }

	    return;
	  }

	gc.somethingChanged();

	if (debug)
	  {
	    System.out.println("invid to delete: " + invid);
	  }
		
	gc.setWaitCursor();

	try
	  {
	    Invid user = fp.getObjectInvid();

	    retVal = fp.getObject().getField(SchemaConstants.UserAdminPersonae).deleteElement(invid);

	    removed = (retVal == null) ? true : retVal.didSucceed();

	    if (retVal != null)
	      {
		gc.handleReturnVal(retVal);
	      }

	    if (removed)
	      {
		if (debug)
		  {
		    System.out.println("removed the element from the field ok");
		  }

		retVal = fp.getgclient().getSession().remove_db_object(invid);

		deleted = (retVal == null) ? true : retVal.didSucceed();
		
		if (retVal != null)
		  {
		    gc.handleReturnVal(retVal);
		  }
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("could not remove the element from the field");
		  }
	      }

	    if (deleted)
	      {
		gc.setStatus("Deleted the object ok");
	      }
	    else
	      {
	       gc.setStatus("Could not delete the object.");
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not delete this persona: " + rx);
	  }

	if (deleted && removed)
	  {
	    int x = middle.getSelectedIndex();
	    if (debug)
	      {
		System.out.println("Selected number: " + x);
		//		System.out.println("Deleting number: " + pc.index);
	      }

	    middle.removeTabAt(x);
	    panels.removeElementAt(x);

	    //middle.invalidate();
	    //validate();
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("Could not fully remove the object.");
	      }
	  }
	
	gc.setNormalCursor();
      }
  }

  public void stateChanged(ChangeEvent e)
  {
    personaContainer pc = (personaContainer)middle.getSelectedComponent();
    if (pc == null)
      {
	return;
      }

    if (delete != null)
      {
	delete.setEnabled(pc.isEditable());
      }
  }
} 

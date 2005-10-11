/*

   personaPanel.java

   a panel for handling User's personae.
   
   Created: 6 October 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Mike Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2005
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.client;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import arlut.csd.JDialog.StringDialog;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.db_object;
import arlut.csd.ganymede.rmi.invid_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    personaPanel

------------------------------------------------------------------------------*/

/**
 * This Panel is used to present a user's personae when viewing or
 * editing a user object in the Ganymede client.
 *
 * @author Mike Mulvaney
 */

public class personaPanel extends JPanel implements ActionListener, ChangeListener{

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.personaPanel");
  
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
    catch (Exception rx)
      {
	gc.processExceptionRethrow(rx, "Could not call field.isEditable() in personaPanel: ");
      }

    if (editable && fieldIsEditable)
      {
	// Create the button panel for the bottom
	JPanel bottom = new JPanel(false);

	// "Create"
	add = new JButton(ts.l("init.create_button"));
	add.setActionCommand("Create");
	add.addActionListener(this);

	// "Delete"
	delete = new JButton(ts.l("init.delete_button"));
	delete.setActionCommand("Delete");
	delete.addActionListener(this);

	bottom.add(add);
	bottom.add(delete);
	
	add("South", bottom);
      }

    // Create the middle, content pane
    middle = new JTabbedPane(JTabbedPane.TOP);

    JPanel middleP = new JPanel(new BorderLayout());

    // "Personae"
    middleP.setBorder(new TitledBorder(ts.l("init.border_title")));
    middleP.add("Center", middle);

    add("Center", middleP);

    try
      {
	personas = field.getValues();
      }
    catch (Exception rx)
      {
	gc.processExceptionRethrow(rx, "Could not get values for persona field: ");
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
			System.err.println("Whoa, got a null object(edit), trying to go to non-editable, cover me.");
		      }
		    
		    ReturnVal Vrv = gc.handleReturnVal(gc.getSession().view_db_object(thisInvid));
		    ob = (db_object) Vrv.getObject();
		    
		    if (ob == null)
		      {
			System.err.println("That didn't work...its still not giving me anything back.  Giving up.");
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
		    System.err.println("Whoa, got a null object(view), skipping.");
		  }
		else
		  {
		    pc = new personaContainer(thisInvid, thisOneEditable, this, ob);
		  }
	      }		
      	  }
	catch (Exception rx)
	  {
	    gc.processExceptionRethrow(rx);
	  }
	
	panels.addElement(pc);

	// We need to have a default name for tabs we create, even
	// though the personaContainer will forcibly set the title to
	// the persona object's actual title upon loading.

	// "Persona {0,number,#}"
	middle.addTab(ts.l("init.default_tab_title", new Integer(i)), pc);

	Thread t = new Thread(pc);
	t.setPriority(Thread.NORM_PRIORITY);
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
	System.err.println(e.getActionCommand());
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

	    if (newObject == null)
	      {
		// "You don''t have permission to create objects of this type."
		gc.showErrorMessage(ts.l("actionPerformed.null_object_created"));
		add.setEnabled(false);
		return;
	      }

	    Invid user = fp.getObjectInvid();

	    gc.somethingChanged();
	    
	    // Tell the user about the persona

	    fp.getObject().getField(SchemaConstants.UserAdminPersonae).addElement(newObject.getInvid());

	    // Tell the persona about the user

	    newObject.getField(SchemaConstants.PersonaAssocUser).setValue(user);
	    
	    personaContainer pc = new personaContainer(newObject.getInvid(), editable, this, newObject);

	    // "New Persona {0,number,#}"
	    middle.addTab(ts.l("actionPerformed.new_tab_title", new Integer(index)), pc);

	    panels.addElement(pc);

	    pc.run();

	    //Thread t = new Thread(pc);
	    //t.start();

	    pc.waitForLoad();
	    
	    if (debug)
	      {
		System.err.println("Showing: " + index);
	      }
	    
	    middle.setSelectedIndex(index);
	  }
	catch (Exception rx)
	  {
	    gc.processExceptionRethrow(rx, "Could not create new persona: ");
	  }
	finally
	  {
	    gc.setNormalCursor();
	  }
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

	// "Confirm Deletion"
	// "Are you sure you want to delete persona {0}?"
	StringDialog d = new StringDialog(gc,
					  ts.l("actionPerformed.deletion_title"),
					  ts.l("actionPerformed.deletion_mesg", middle.getTitleAt(middle.getSelectedIndex())),
					  true);

	gc.setNormalCursor();

	if (d.DialogShow() == null)
	  {
	    if (debug)
	      {
		System.err.println("Cancelled.");
	      }

	    return;
	  }

	gc.somethingChanged();

	if (debug)
	  {
	    System.err.println("invid to delete: " + invid);
	  }
		
	gc.setWaitCursor();

	try
	  {
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
		    System.err.println("removed the element from the field ok");
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
		    System.err.println("could not remove the element from the field");
		  }
	      }

	    if (deleted)
	      {
		// "Deleted the object ok."
		gc.setStatus(ts.l("actionPerformed.deleted_ok"));
	      }
	    else
	      {
		// "Could not delete the object."
		gc.setStatus(ts.l("actionPerformed.deleted_bad"));
	      }
	  }
	catch (Exception rx)
	  {
	    // "Could not delete persona"
	    gc.processExceptionRethrow(rx, ts.l("actionPerformed.deletion_exception"));
	  }

	if (deleted && removed)
	  {
	    int x = middle.getSelectedIndex();
	    if (debug)
	      {
		System.err.println("Selected number: " + x);
		//		System.err.println("Deleting number: " + pc.index);
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
		System.err.println("Could not fully remove the object.");
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

  public void dispose()
  {
    fp = null;
    gc = null;
    field = null;
    removeAll();
    add = null;
    delete = null;
    middle = null;

    if (personas != null)
      {
	personas.clear();
	personas = null;
      }

    if (panels != null)
      {
	panels.clear();
	panels = null;
      }

    empty = null;
  }
} 


/*
   GASH 2

   vectorPanel.java

   This module provides for a generic vector of objects, and can be
   used to implement a collection of date fields, i.p. addresses,
   or edit in place (composite) objects.

   Created: 17 Oct 1996
   Version: $Revision: 1.50 $
   Last Mod Date: $Date: 1999/04/06 04:17:02 $
   Release: $Name:  $

   Module By: Navin Manohar, Mike Mulvaney, Jonathan Abbey

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

import arlut.csd.ganymede.*;

import java.awt.event.*;
import java.awt.*;

import arlut.csd.JDataComponent.*;
import arlut.csd.Util.PackageResources;

import java.util.*;
import java.rmi.*;
import java.net.*;

import javax.swing.*;
import javax.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     vectorPanel

------------------------------------------------------------------------------*/

/**
 * <p>A GUI vector panel that is designed to be placed inside a
 * {@link arlut.csd.ganymede.client.containerPanel containerPanel} within the client.
 * This vector panel is intended to support vectors of discrete GUI components that
 * are mapped to elements in a server-side vector {@link arlut.csd.ganymede.db_field db_field},
 * but right now it only supports IP Address fields and edit-in-place
 * containerPanels.  Most of the code in this class is dedicated to handling
 * vectors of embedded containerPanels.</p>
 *
 * <p>Whatever sort of GUI components/database elements this vectorPanel is managing,
 * all GUI components will be wrapped by
 * {@link arlut.csd.ganymede.client.elementWrapper elementWrapper}'s, which are responsible
 * for hiding and revealing the GUI component on the user's command.</p>
 *
 * <p>vectorPanel handles communications with the server to manage the server-side
 * db_field.</p>
 *
 * @see elementWrapper
 * @see containerPanel
 * @see arlut.csd.ganymede.invid_field
 * @see arlut.csd.ganymede.ip_field
 * 
 * @version $Revision: 1.50 $ $Date: 1999/04/06 04:17:02 $ $Name:  $
 * @author Navin Manohar, Mike Mulvaney, and Jonathan Abbey
 */

public class vectorPanel extends JPanel implements JsetValueCallback, ActionListener, MouseListener, Runnable {

  boolean debug = false;

  /**
   * Vector of GUI components held in this vectorPanel.  This vector contains
   * the actual GUI components added to this vector, not the elementWrappers.
   */

  Vector
    compVector;

  /**
   * Boolean to track editability status of the vector field we are connected
   * to.  This member is a Boolean instead of a boolean so that we can distinguish
   * between false and unknown (null).
   */

  private Boolean
    myFieldIsEditable = null;

  /**
   * The name of the invid field we are managing
   */

  String name = null;

  /**
   * Hash mapping GUI components added to this vectorPanel to the
   * elementWrappers holding them.  Used to update the elementWrappers'
   * titles in response to changes in the fields connected to the
   * components.
   */

  Hashtable
    ewHash;

  /**
   * Button used to add a new element to the vector
   */

  JButton
    addB;

  /**
   * centerPanel holds all of the elementWrappers in a BoxLayout  
   */

  JPanel
    centerPanel;

  boolean 
    editable,
    isEditInPlace,
    centerPanelAdded = false;

  /**
   * Reference to server-side vector {@link arlut.csd.ganymede.invid_field invid_field} or
   * {@link arlut.csd.ganymede.ip_field ip_field}.  This vectorPanel will communicate
   * with the Ganymede server using this reference to make changes to the vector field.
   */

  private db_field my_field;

  /**
   * Reference to the desktop pane containing the client's internal windows.  Used to access
   * some GUI resources and to provide to new containerPanels created for embedded objects.
   */

  protected windowPanel
    wp;

  /**
   * Reference to the containerPanel that we are embedded in.
   */

  containerPanel
    container;

  /**
   * Reference to the client's main class.
   */

  gclient
    gc;

  /**
   * Pop-up menu appears when you right-click anywhere inside the panel
   */

  JPopupMenu
    popupMenu;

  JMenuItem
    closeLevelMI,
    expandLevelMI,
    closeAllMI,
    expandAllMI;

  boolean 
    isCreating;

  /* -- */
  
  /**
   * Constructor
   *
   * @param field db_field for this vectorPanel
   * @param parent windowPanel above this vectorPanel
   * @param editable True if field is editable
   * @param isEditInPlace True if object is editInPlace.  If this is false, it will make a vector of IPFields.
   * @param container containerPanel this vectorPanel is in
   *
   */

  public vectorPanel(db_field field, windowPanel parent, 
		     boolean editable, boolean isEditInPlace, 
		     containerPanel container, boolean isCreating)
  {
    my_field = field;
    
    this.editable = editable;
    this.isEditInPlace = isEditInPlace;
    this.wp = parent;
    this.container = container;
    this.isCreating = isCreating;
    this.gc = container.gc;

    if (!debug)
      {
	debug = gc.debug;
      }

    if (debug)
      {
	System.out.println("Adding new vectorPanel");
      }

    centerPanel = new JPanel(false);
    centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
    
    setLayout(new BorderLayout());

    // Set up the titled border
    //
    // The titled border should say "field.name(): Vector".  For example,
    // "Systems: Vector"  The titledBorder surrounds the entire vectorPanel,
    // separating it from the rest of the components.

    EmptyBorder eb = (EmptyBorder)BorderFactory.createEmptyBorder(10,10,10,10);
    TitledBorder tb;

    try
      {
	name = field.getName();

	if (name == null)
	  {
	    tb = BorderFactory.createTitledBorder("Untitled Vector");
	  }
	else
	  {
	    tb = BorderFactory.createTitledBorder(name + ": Vector");
	  }
      }
    catch (RemoteException ex)
      {
	tb = BorderFactory.createTitledBorder("Vector -- unknown field name");
      }

    CompoundBorder cb = BorderFactory.createCompoundBorder(tb,eb);
    setBorder(cb);

    addB = new JButton("Add " + name);

    // Set up pop up menu
    //
    // The popup menu appears when the right mouse button is clicked inside the
    // TitledBorder but outside of the elementWrappers.  It basically presents a
    // menu with either "expand" or "close" options.

    popupMenu = new JPopupMenu();
    expandLevelMI = new JMenuItem("Expand this level");
    expandLevelMI.addActionListener(this);
    expandAllMI = new JMenuItem("Expand all elements");
    expandAllMI.addActionListener(this);
    popupMenu.add(expandLevelMI);
    popupMenu.add(expandAllMI);
    popupMenu.addSeparator();

    closeLevelMI = new JMenuItem("Close this level");
    closeLevelMI.addActionListener(this);
    closeAllMI = new JMenuItem("Close all elements");
    closeAllMI.addActionListener(this);
    popupMenu.add(closeLevelMI);
    popupMenu.add(closeAllMI);

    addMouseListener(this);

    compVector = new Vector();
    ewHash = new Hashtable();

    createVectorComponents();
  }

  private void showPopupMenu(int x, int y)
  {
    popupMenu.show(this, x, y);
  }

  /**
   * <p>Talks to the server and populates this vectorPanel with
   * elements corresponding to values held in my_field.  Only called
   * on vectorPanel construction.</p>
   */

  private void createVectorComponents()
  {
    if (my_field instanceof ip_field)
      {
	if (debug)
	  {
	    System.out.println("Adding ip vector field");
	  }

	try
	  {
	    ip_field ipfield = (ip_field) my_field;

	    int size = ipfield.size();
	    
	    for (int i=0; (i < size) && container.keepLoading();i++) 
	      {
		JIPField ipf = new JIPField(editable,
					    ipfield.v6Allowed());
		
		ipf.setValue((Byte[]) ipfield.getElement(i));
		ipf.setCallback(this);
		
		addElement(ipf, false);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't make ip field: " + rx);
	  }
      }
    else if (my_field instanceof invid_field)
      {
	if (debug)
	  {
	    System.out.println("Adding vector invid_field");
	  }

	try
	  {
	    invid_field invidfield = (invid_field) my_field;
	    FieldInfo info = invidfield.getFieldInfo();

	    if (!(info.getValue() instanceof Vector))
	      {
		throw new RuntimeException("Error, vectorPanel passed scalar invid_field");
	      }

	    Vector values = (Vector) info.getValue();

	    if (!isEditInPlace)
	      {
		// The vectorPanel only handles edit-in-place fields.
		// If it is not edit-in-place, then the field should
		// just get a StringSelector.

		throw new RuntimeException("Don't give me(the vectorPanel!)  non edit-in-place invid_fields.");		
	      }

	    if (debug)
	      {
		System.out.println("Adding edit in place invid vector, size = " + invidfield.size());
	      }
	    
	    int size = values.size();
	    
	    for (int i=0; (i < size) && container.keepLoading(); i++)
	      {
		if (debug)
		  {
		    System.out.println("Adding Invid to edit in place vector panel");
		  }

		Invid inv = (Invid)(values.elementAt(i));
		   
		// We need to get a server-side db_object reference to pass to the
		// containerPanel.
		
		db_object object = null;

		if (editable)
		  {
		    ReturnVal rv = gc.handleReturnVal(gc.getSession().edit_db_object(inv));
		    object = rv.getObject();
		  }
		else
		  {
		    ReturnVal rv = gc.handleReturnVal(gc.getSession().view_db_object(inv));
		    object = rv.getObject();
		  }

		// create a containerPanel, but don't load it yet.. the elementWrapper will
		// load the containerPanel if the user actually opens it.

		containerPanel cp = new containerPanel(object,
						       editable,
						       gc,
						       wp, container.frame,
						       null, false, null);

		cp.setBorder(wp.lineEmptyBorder);

		addElement(object.getLabel(), cp, false, false);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't make invid field: " + rx);
	  }
      }
    else
      {
	System.out.println("\n*** Error - inappropriate field type passed to vectorPanel constructor");
      }

    if (editable)
      {
	if (debug)
	  {
	    System.out.println("Adding add button");
	  }

	JPanel addPanel = new JPanel();
	addPanel.setLayout(new BorderLayout());
	addB.addActionListener(this);

	addPanel.add("East", addB);

	add("South", addPanel);
      }
    else
      {
	if (debug)
	  {
	    System.out.println("Field is not editable, no button added");
	  }
      }
  } 

  /**
   * <p>Creates a new element in the vector {@link arlut.csd.ganymede.db_field db_field}
   * this vectorPanel is connected to.  Creates and adds the GUI component for the new
   * element to the vectorPanel.</p>
   *
   * <p>This is called when the add button is clicked, but there is no reason
   * why it couldn't be called from other places if you wanted to add a new 
   * element.</p>
   */

  public void addNewElement()
  {
    ReturnVal retVal = null;
    Invid invid = null;
    boolean local_debug = false;

    /* -- */

    if (debug)
      {
	System.out.println("Adding new element");
      }
    
    if (my_field instanceof invid_field)
      {
	if (debug)
	  {
	    System.out.println("Adding new edit in place element");
	  }

	try
	  {
	    retVal = ((invid_field)my_field).createNewEmbedded();
	    gc.handleReturnVal(retVal);

	    if (retVal != null && retVal.didSucceed())
	      {
		if (local_debug)
		  {
		    System.err.println("XX** Hey.. got a result from the server to create a new embedded element");
		  }

		invid = retVal.getInvid();

		if (local_debug)
		  {
		    System.err.println("XX** Invid for new embedded object is " + invid);
		  }
		
		db_object object = (gc.handleReturnVal(gc.getSession().edit_db_object(invid))).getObject();
		
		if (local_debug)
		  {
		    if (object == null)
		      {
			System.err.println("XX** Could not get edit handle on new embedded object");
		      }
		    else
		      {
			System.err.println("XX** Got edit handle on new embedded object");
		      }
		  }

		// create and load the new containerPanel.  Note that we are going to
		// immediately show the new element open to the user, so we go ahead
		// and use one of the default-valued containerPanel constructors that
		// will immediately load the new containerPanel.

		containerPanel cp = new containerPanel(object,
						       isFieldEditable() && editable,
						       gc,
						       wp, 
						       container.frame, 
						       null);

		if (local_debug)
		  {
		    System.err.println("XX** Created container panel");
		  }

		if (local_debug)
		  {
		    System.err.println("XX** Recorded containerpanel in our client");
		  }
		
		cp.setBorder(wp.lineEmptyBorder);

		if (local_debug)
		  {
		    System.err.println("XX** Set border");
		  }

		// display the new containerPanel pre-expanded
		
		addElement("New Element", cp, true);

		if (local_debug)
		  {
		    System.err.println("XX** Added element");
		  }
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not create new containerPanel: " + rx);
	  }
      }
    else if (my_field instanceof ip_field)
      {
	if (debug)
	  {
	    System.out.println("Adding new ip vector field");
	  }

	ip_field ipfield = (ip_field) my_field;
	    
	try
	  {
	    JIPField ipf = new JIPField(true,
					ipfield.v6Allowed());
	    ipf.setCallback(this);
	    addElement(ipf);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not make new ip field: " + rx);
	  }
      }
    else
      {
	System.out.println("vectorPanel.addNewElement(): This type is not supported yet.");
      }
  
    gc.somethingChanged();
    invalidate();
    container.frame.validate();
  }

  /**
   * <p>Adds a new element to the vectorPanel.</p>
   *
   * <p>This element gets the default title, and will not be opened in expanded
   * form.</p>
   *
   * <p>The vectorPanel will be revalidated to trigger a GUI refresh
   * after this element is added.</p>
   *
   * @param c Component to be added
   */

  public void addElement(Component c)
  {
    addElement(null, c, false, true);
  }

  /**
   * <p>Adds a new element to the vectorPanel.</p>
   *
   * <p>This element gets the default title, and will not be expanded
   * immediately.</p>
   *
   * @param c Component to be added
   * @param invalidateNow If true, invalidate()/validate() will be called.
   * When adding several components all at once, set this to false.  
   */

  public void addElement(Component c, boolean invalidateNow)
  {
    addElement(null, c, false, invalidateNow);
  }

  /**
   * <p>Adds a new element to the vectorPanel.</p>
   *
   * <p>The vectorPanel will be revalidated to trigger a GUI refresh
   * after this element is added.</p>
   *
   * @param title String used in the "title" of the elementWrapper
   * @param c Component to be added 
   */

  public void addElement(String title, Component c)
  {
    addElement(title,c,false, true);
  }

  /**
   * <p>Adds a new element to the vectorPanel.</p>
   *
   * <p>This element will be immediately expanded if &lt;expand&gt; is true.</p>
   *
   * <p>The vectorPanel will be revalidated to trigger a GUI refresh
   * after this element is added.</p>
   *
   * @param title String used in the "title" of the elementWrapper
   * @param c Component to be added 
   * @param expand If true, the element will appear pre-opened
   */

  public void addElement(String title, Component c, boolean expand)
  {
    addElement(title, c, expand, true);
  }

  /**
   * Add a new element to the vectorPanel.
   *
   * @param title String used in the "title" of the elementWrapper
   * @param c Component to be added
   * @param expand If true, the elementWrapper will be expanded immediately after creation
   * @param invalidateNow If true, invalidate()/validate() will be called.
   * When adding several components all at once, set this to false.
   */

  public void addElement(String title, Component c, boolean expand, boolean invalidateNow)
  {
    if (c == null)
      {
	throw new IllegalArgumentException("vectorPanel.addElement(): Component parameter is null");
      }

    setStatus("adding new elementWrapper");

    try
      {
	compVector.addElement(c);

	if (debug)
	  {
	    System.out.println("Index of element: " + compVector.indexOf(c));
	  }

	// Make sure the centerPanel has been added.

	if (!centerPanelAdded)
	  {
	    add("Center", centerPanel);
	    centerPanelAdded = true;
	  }

	elementWrapper ew = new elementWrapper(title, c, this, editable && isFieldEditable());

	ew.setIndex(compVector.indexOf(c));

	// Keep track of the elementWrappers in the ewHash.

	ewHash.put(c, ew);
    
	// centerPanel uses a BoxLayout(Y_AXIS), so calling add() will
	// just put the new component at the bottom(which is what we want)

	centerPanel.add(ew);

	// Only expand if it is a containerPanel.  If it is something else, there 
	// isn't anything to expand (the GUI component will be shown inline with
	// the elementWrapper's icons.  We'll pre-expand the element if we are
	// editing a newly created object.  This check is done here in case the
	// server responds to Session.create_db_object() with an object that
	// already contains one or more embedded objects.

	if ((c instanceof containerPanel) && (expand || isCreating))
	  {
	    ew.open();
	  }

	// tell the containerPanel about us so it can update its progress
	// bar, if it is displaying one

	container.vectorElementAdded();

	if (invalidateNow)
	  {
	    invalidate();
	  }
      }
    finally
      {
	setStatus("Done adding elementWrapper");
      }
  }

  /**
   * Removes an element from the vector panel.
   *
   * @param ew Component to be removed.
   */  

  public void deleteElement(elementWrapper ew) 
  {
    if (debug)
      {
	System.out.println("Deleting element");
      }

    if (ew == null)
      {
	throw new IllegalArgumentException("Component parameter is null");
      }

    try
      {
	if (debug)
	  {
	    System.out.println("Deleting element number: " + ew.index + "(" + ew.getValue() + ")");
	  }

	// we use ew.getValue() rather than ew.index here so that we
	// don't risk screwing things up if we get our index numbers
	// misaligned.

	ReturnVal retVal = my_field.deleteElement(ew.getValue());

	gc.handleReturnVal(retVal);

	// handleReturnVal will have called refresh on this
	// vectorPanel for us, because db_field.deleteElement()
	// mandates a field refresh for the field.

	if (debug)
	  {
	    System.out.println("Deleting element (after handleReturnVal)");
	  }

	if ((retVal == null) || (retVal.didSucceed()))
	  {
	    gc.somethingChanged();

	    if (debug)
	      {
		System.out.println("Deleting element (after revalidate)");
	      }
	  }
	else
	  {
	    showErrorMessage("Server will not allow delete.");
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not delete element:" + rx);
      }
  }

  /**
   * <p>Refreshes the vectorPanel.</p>
   *
   * <p>This will refresh every containerPanel and IPField in this vectorPanel, and
   * will add or remove entries as needed to bring this vectorPanel into synchronization
   * with the field on the server that this vectorPanel is attached to.</p>
   *
   * <p>New containerPanel elements will always be added to the end of the vector..
   * we aren't worried about the order of the vector we're handling, just
   * the values and the proper association between the GUI component and the server field.</p>
   */

  public void refresh()
  {
    if (debug)
      {
	System.err.println("vectorPanel.refresh(" + name + ")");
      }

    int size;

    Hashtable serverInvids = new Hashtable();
    Hashtable localInvids = new Hashtable();

    containerPanel cp = null;

    Invid invid = null;

    boolean needFullRefresh = false;

    /* -- */

    try
      {
	size = my_field.size();

	if (my_field instanceof invid_field)
	  {
	    // figure out what invids are currently in my_field
	    // on the server, save them so we can scratch them off
	    // as we sync this vector panel 

	    Vector serverValues = my_field.getValues();

	    if (debug)
	      {
		System.err.println("vectorPanel.refresh(): serverValues.size = " + serverValues.size());
	      }

	    for (int i = 0; i < serverValues.size(); i++)
	      {
		invid = (Invid) serverValues.elementAt(i);

		serverInvids.put(invid, new Integer(i));
	      }

	    // what invid's are currently in this vector panel?

	    for (int i = 0; i < compVector.size(); i++)
	      {
		cp = (containerPanel) compVector.elementAt(i);

		if (cp != null)
		  {
		    invid = cp.getObjectInvid();

		    localInvids.put(invid, invid);
		  }
	      }

	    // now, iterate through the invids being displayed, scratching
	    // them out of serverInvids when we sync each containerPanel

	    int localIndex = 0;

	    while (localInvids.size() > 0)
	      {
		cp = (containerPanel) compVector.elementAt(localIndex);
		invid = cp.getObjectInvid();
	    
		if (serverInvids.containsKey(invid))
		  {
		    // the server has this invid, go ahead and update
		    // this containerPanel's status

		    /*
		      gclient.handleReturnVal() will update the
		      containerPanel's contents if needed
		      
		      cp.updateAll();
		      */

		    elementWrapper ew = (elementWrapper) ewHash.get(cp);

		    ew.setIndex(localIndex);

		    /*
		      Not using this right now.. kind of a klunky UI
		      feature.

		      ew.checkValidation();
		      */

		    ew.refreshTitle();

		    // we've updated this one.. scratch it off the server
		    // and client list

		    serverInvids.remove(invid);
		    localInvids.remove(invid);

		    localIndex++;

		    if (debug)
		      {
			System.err.println("vectorPanel.refresh(): updated " + ew.titleText);
		      }
		  }
		else
		  {
		    // the server doesn't have this invid anymore, so
		    // we need to take it out of this vector panel

		    compVector.removeElement(cp);

		    // and let the frame know not to keep it in its
		    // records anymore

		    container.frame.containerPanels.removeElement(cp);

		    elementWrapper ew = (elementWrapper) ewHash.get(cp);
		    centerPanel.remove(ew);
		    ewHash.remove(cp);

		    needFullRefresh = true;
		    localInvids.remove(invid);

		    if (debug)
		      {
			System.err.println("vectorPanel.refresh(): removed " + ew.titleText);
		      }
		  }
	      }

	    // take care of any new embedded objects to be displayed, adding
	    // them to the end.  Note that we don't care about the ordering
	    // of the invids in the client's vectorPanel, we just want to make
	    // sure that we have the right (unordered) set, and that everything
	    // is up-to-date.

	    Enumeration enum = serverInvids.elements();

	    while (enum.hasMoreElements())
	      {
		invid = (Invid) enum.nextElement();

		localIndex = ((Integer) serverInvids.get(invid)).intValue();

		if (debug)
		  {
		    System.out.println("VectorPanel.refresh(): adding new embedded object: " + invid);
		  }

		ReturnVal rv = editable ? gc.getSession().edit_db_object(invid) :
		  gc.getSession().view_db_object(invid);

		rv = gc.handleReturnVal(rv);

		if (rv == null || rv.didSucceed())
		  {
		    // create the new containerPanel.. don't pre-load it..

		    containerPanel newcp = new containerPanel(rv.getObject(),
							      editable,
							      gc,
							      wp, container.frame,
							      null, false, null);
		    
		    newcp.setBorder(wp.lineEmptyBorder);
		    
		    // the addElement call will take care of most of the niceties of
		    // getting this containerPanel added.
		    
		    addElement(newcp);
		  }
	      }
	  }
	else if (my_field instanceof ip_field)
	  {
	    // this code hasn't been tested

	    for (int i = 0; i < size; i++)
	      {
		Object o = my_field.getElement(i);

		if (i < compVector.size())
		  {
		    JIPField ipf = (JIPField) compVector.elementAt(i);
		    
		    ipf.setValue((Byte[])my_field.getElement(i));

		    elementWrapper ew = (elementWrapper) ewHash.get(cp);

		    ew.setIndex(i);
		  }
		else
		  {
		    ip_field ipfield = (ip_field) my_field;
		    
		    JIPField ipf = new JIPField(editable,
						ipfield.v6Allowed());
		    
		    ipf.setValue((Byte[]) ipfield.getElement(i));
		    ipf.setCallback(this);
		    
		    addElement(ipf, false);
		  }
	      }

	    // Now get rid of extra ip field components

	    int fieldCount = compVector.size();

	    // we count down so that we can remove extra fields off of the
	    // end
	    
	    for (int i = fieldCount; i >= size; i--)
	      {
		centerPanel.remove((Component)ewHash.get(compVector.elementAt(i)));
		compVector.removeElementAt(i);
	      }
	  }
	else
	  {
	    System.err.println("Unknown type in vectorPanel.refresh compVector: " + my_field.getClass());
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("vectorPanel.refresh(): " + rx);
      }

    if (needFullRefresh)
      {
	invalidate();
	container.frame.validate();

	if (debug)
	  {
	    System.err.println("vectorPanel.refresh(): executed fullRefresh");
	  }
      }
    else if (debug)
      {
	System.err.println("vectorPanel.refresh(): skipped fullRefresh");
      }
  }

  /**
   * Expand all the levels.
   */
  
  public void expandAllLevels()
  {
    Enumeration wrappers = ewHash.keys();

    /* -- */

    while (wrappers.hasMoreElements())
      {
	elementWrapper ew = (elementWrapper)ewHash.get(wrappers.nextElement());
	ew.open();

	Component comp = ew.getComponent();
	
	if (comp instanceof containerPanel)
	  {
	    if (debug)
	      {
		System.out.println("Aye, it's a containerPanel");
	      }

	    containerPanel cp = (containerPanel)comp;
	    
	    for (int i = 0; i < cp.vectorPanelList.size(); i++)
	      {
		((vectorPanel)cp.vectorPanelList.elementAt(i)).expandLevels(true);
	      }
	  }
	else
	  {
	    System.out.println("The likes of this I have never seen: " + comp);
	  }
      }

    invalidate();
    container.frame.validate();
  }

  /**
   * <p>Expands all closed elementWrappers in this vectorPanel</p>
   *
   * @param recursive If true, it will expand any vector panels inside
   * the containerPanels in this vectorPanel as well.  The current
   * implementation opens up each level with a separate thread, which
   * may well not be the best way of doing this from a performance/safety
   * perspective.
   */ 

  public void expandLevels(boolean recursive)
  {
    if (recursive)
      {
	// ok, mike did this with threaded recursion, strangely enough.  Note
	// that t.start() is equivalent to calling expandAllLevels() in a new
	// thread, by way of this.run().

	Thread t = new Thread(this);
	t.start();
      }
    else
      {
	setWaitCursor();

	Enumeration wrappers = ewHash.keys();
	
	/* -- */
	
	while (wrappers.hasMoreElements())
	  {
	    elementWrapper ew = (elementWrapper)ewHash.get(wrappers.nextElement());
	    ew.open();
	  }

	setNormalCursor();
      }
  }

  /**
   * This just calls expandAllLevels.. for use in threading this out.
   */

  public void run()
  {
    expandAllLevels();
  }

  /**
   * Close all the levels
   *
   * @param recursive If true, close all vectorPanels inside this
   * vectorPanel.  This one never spawns another thread.
   */

  public void closeLevels(boolean recursive)
  {
    Enumeration wrappers = ewHash.keys();

    /* -- */

    setWaitCursor();

    try
      {
	while (wrappers.hasMoreElements())
	  {
	    elementWrapper ew = (elementWrapper)ewHash.get(wrappers.nextElement());
	    ew.close();

	    if (recursive)
	      {
		Component comp = ew.getComponent();
	    
		if (comp instanceof containerPanel)
		  {
		    containerPanel cp = (containerPanel)comp;
		
		    for (int i = 0; i < cp.vectorPanelList.size(); i++)
		      {
			((vectorPanel)cp.vectorPanelList.elementAt(i)).closeLevels(true);
		      }
		  }
	      }
	  }
    
	// This is necessary for the closing, but the expanding doesn't
	// need validate stuff.  Pretty weird, but thus is swing.

	invalidate();
	container.frame.validate();
      }
    finally
      {
	setNormalCursor();
      }
  }

  public void actionPerformed(ActionEvent e)
  {
    if ((e.getSource() == addB) && editable)
      {
	setWaitCursor();
	addNewElement();
	setNormalCursor();
      }
    else if (e.getSource() instanceof JMenuItem)
      {
	if (debug)
	  {
	    System.out.println("JMenuItem: " + e.getActionCommand());
	  }

	if (e.getActionCommand().equals("Expand all elements"))
	  {
	    expandLevels(true);
	  }
	else if (e.getActionCommand().equals("Expand this level"))
	  {
	    expandLevels(false);
	  }
	else if (e.getActionCommand().equals("Close this level"))
	  {
	    closeLevels(false);
	  }
	else if (e.getActionCommand().equals("Close all elements"))
	  {
	    closeLevels(true);
	  }
      }
  }

  /**
   * <p>JsetValueCallback implementation method.  Any elementWrappers we
   * contain will talk to us using this method, to notify us of element
   * deletion and to pass information to us about IP field value changes.</p>
   */

  public boolean setValuePerformed(JValueObject v)
  {
    elementWrapper ew = (elementWrapper)v.getSource();
    boolean returnValue = false;
    
    if (v == null)
      {
	throw new IllegalArgumentException("ValueObject Argument is null");
      }

    if (v.getValue() == null)
      {
	return false;
      }
    else if (v.getValue().equals("remove"))
      {
	if (debug)
	  {
	    System.out.println("You clicked on an element closebox");
	  }

	if (editable)
	  {
	    deleteElement((elementWrapper)v.getSource());
	  }
	else
	  {
	    setStatus("You can't delete elements in a view window.");
	    returnValue = false;
	  }
      }
    else if (ew.getComponent() instanceof JIPField)
      {
	if (debug)
	  {
	    System.out.println("IP field changed");
	  }
	
	if (editable)
	  {
	    short index = (short)compVector.indexOf(ew.getComponent());
	
	    if (v.getOperationType() == JValueObject.ERROR)
	      {
		setStatus((String)v.getValue());
		returnValue = false;
	      }
	    else
	      {
		try
		  {
		    returnValue = changeElement((Byte[])v.getValue(), index);
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not set value of date field: " + rx);
		  }
	      }
	  }
	else     //editable == false, so can't make changes
	  {
	    returnValue = false; 
	  }
      }
    else
      {
	System.out.println("Value changed in field that is not yet supported");
      }

    if (returnValue)
      {
	gc.somethingChanged();
      }

    return returnValue;
  }

  /**
   * <p>Server dispatch method for value change messages passed us by elementWrappers.</p>
   *
   * <p>Currently used only for IP fields, as embedded containerPanels communicate
   * their field changes directly to the server.</p>
   */

  public boolean changeElement(Object obj, short index) throws RemoteException
  {
    ReturnVal retVal;
    boolean succeeded;
    
    /* -- */

    if (index >= my_field.size())
      {
	if (debug)
	  {
	    System.out.println("Adding new element");
	  }

	retVal = my_field.addElement(obj);

	succeeded = (retVal == null) ? true : retVal.didSucceed();

	if (retVal != null)
	  {
	    gc.handleReturnVal(retVal);
	  }

	if (succeeded)
	  {
	    if (debug)
	      {
		System.out.println("Add Element returned true");
		System.out.println("There are now " + my_field.size() + " elements in the field");
	      }

	    return true;
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("Add Element returned false");
	      }

	    return false;
	  }
      }
    else
      {
	if (debug)
	  {
	    System.out.println("Changing element " + index);
	  }

	retVal = my_field.setElement(index, obj);

	succeeded = (retVal == null) ? true : retVal.didSucceed();

	if (retVal != null)
	  {
	    gc.handleReturnVal(retVal);
	  }

	if (succeeded)
	  {
	    if (debug)
	      {
		System.out.println("set Element returned true");
	      }

	    return true;
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("set Element returned false");
	      }

	    return false;
	  }
      }	    
  }

  /**
   * Pop-up menu dispatch.
   */

  public void mousePressed(java.awt.event.MouseEvent e) 
  {
    if (e.isPopupTrigger())
      {
	showPopupMenu(e.getX(), e.getY());
      }
  }

  public void mouseClicked(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}

  // convenience stuff

  private final void setStatus(String status)
  {
    gc.setStatus(status);
  }

  private final void setNormalCursor()
  {
    gc.setNormalCursor();
  }

  private final void setWaitCursor()
  {
    gc.setWaitCursor();
  }
  
  private final void showErrorMessage(String message)
  {
    gc.showErrorMessage(message);
  }

  /**
   * <p>Returns true if the vector field we are connected to is editable.</p>
   *
   * <p>This method will call the server to query the field the first time it
   * is called.  It will return the cached result thereafter.</p>
   */
  
  public boolean isFieldEditable()
  {
    if (myFieldIsEditable == null)
      {
	try
	  {
	    myFieldIsEditable = new Boolean(my_field.isEditable());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not check if field was editable: " + rx);
	  }
      }
    
    return myFieldIsEditable.booleanValue();
  }
}



/*
  GASH 2

  vectorPanel.java

  This module provides for a generic vector of objects, and can be
  used to implement a collection of date fields, i.p. addresses,
  or edit in place (composite) objects.

  Created: 17 Oct 1996
  Version: $Revision: 1.41 $ %D%
  Module By: Navin Manohar, Mike Mulvaney, Jonathan Abbey
  Applied Research Laboratories, The University of Texas at Austin
*/


package arlut.csd.ganymede.client;

//import arlut.csd.ganymede.client.*;
import arlut.csd.ganymede.*;

import java.awt.event.*;
import java.awt.*;

import arlut.csd.JDataComponent.*;
import java.util.*;
import java.rmi.*;
import java.net.*;

import jdj.PackageResources;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     vectorPanel

------------------------------------------------------------------------------*/

/**
 * This module provides for a generic vector of objects, and can be
 * used to implement a collection edit in place (composite) objects,
 * each with its own containerPanel.
 *
 * Usually this is used to hold a vector of elementWrappers.  Each elementWrapper
 * contains a containerPanel, so any kind of object can be displayed.  The vectorPanel
 * handles the creation and deletion of objects in the vector, as well as displays
 * the elementWrappers.
 *
 * Note: addNewElement is very different from the various addElement
 * methods.  addNewElement does all the work involved in creating a
 * new element, like calling create_db_object and instantiating a new
 * elementWrapper.  addElement takes a created element, and adds it
 * into the vectorPanel, incrementing the size and actually adding it
 * to the panel.
 *
 * @see elementWrapper
 * 
 */

public class vectorPanel extends JPanel implements JsetValueCallback, ActionListener, MouseListener, Runnable {

  boolean debug = false;

  Vector
    compVector;

  private Boolean
    myFieldIsEditable = null;
    

  String 
    name = null;

  // Hash of components to elementWrappers holding them.
  Hashtable
    ewHash;

  // Button used to add a new element to the vector
  JButton
    addB;

  // centerPanel holds all of the elementWrappers in a BoxLayout  
  JPanel
    centerPanel;

  boolean 
    editable,
    isEditInPlace,
    centerPanelAdded = false;

  private db_field
    my_field;

  // references to stuff higher up
  protected windowPanel
    wp;

  containerPanel
    container;

  gclient
    gc;

  // Popupmenu appears when you right-click anywhere inside the panel
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

  public vectorPanel(db_field field, windowPanel parent, boolean editable, boolean isEditInPlace, containerPanel container, boolean isCreating)
  {
    // Took out some checking for null stuff

    my_field = field;
    
    this.editable = editable;
    this.isEditInPlace = isEditInPlace;
    this.wp = parent;
    this.container = container;
    this.isCreating = isCreating;
    this.gc = container.gc;

    debug = gc.debug;

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

  // This method creates the vector panel.  It creates an IPField of
  // elementWrapper for each element of the vector.  This method is
  // only called when the vectorPanel is created.

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
	    
	    for (int i=0;i < size;i++) 
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
	    
	    int size = invidfield.size();
	    
	    for (int i=0; i < size ; i++)
	      {
		if (debug)
		  {
		    System.out.println("Adding Invid to edit in place vector panel");
		  }

		// Sometimes the window is closed or the client logs
		// out.  when this happens,
		// contianerPanel.keepLoading() will return false, and
		// we should stop loading now.  We don't need to clean
		// up or anything, because when keepLoading() is
		// false, the window is gone anyway.

		if (! container.keepLoading())
		  {
		    if (debug)
		      {
			System.out.println("whoa, time to stop loading in the vector panel.");
			break;
		      }
		  }

		Invid inv = (Invid)(invidfield.getElement(i));
		   
		// We need to get an object to give to the
		// containerPanel.  If we are editing, this should be
		// a view_db_object.  Otherwise, it is an
		// edit_db_object.
		
		db_object object = null;
		if (editable)
		  {
		    ReturnVal rv = wp.getgclient().handleReturnVal(wp.getgclient().getSession().edit_db_object(inv));
		    object = rv.getObject();
		  }
		else
		  {
		    ReturnVal rv = wp.getgclient().handleReturnVal(wp.getgclient().getSession().view_db_object(inv));
		    object = rv.getObject();
		  }

		containerPanel cp = new containerPanel(object,
						       editable,
						       wp.gc,
						       wp, container.frame,
						       null, false);
		container.frame.containerPanels.addElement(cp);
		cp.setBorder(wp.lineEmptyBorder);
		    
		addElement((i+1) + ". " + object.getLabel(), cp, false, false);
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
   * Add a new element to the vectorPanel
   *
   * This method constructs the elementWrapper and the component of
   * the appropriate type.  This is called when the add button is
   * clicked, but there is no reason why it couldn't be called from
   * other places if you wanted to add a new element.
   * 
   */

  public void addNewElement()
  {
    ReturnVal retVal = null;
    Invid invid = null;

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
	    container.checkReturnValForRescan(retVal);

	    if (retVal != null && retVal.didSucceed())
	      {
		invid = retVal.getInvid();
		
		db_object object = (gc.handleReturnVal(wp.gc.getSession().edit_db_object(invid))).getObject();
		
		containerPanel cp = new containerPanel(object,
						       isFieldEditable() && editable,
						       wp.gc,
						       wp, container.frame);
		
		// register this containerPanel with the framePanel, so it
		// can be told to stop.  The containerPanel will also
		// register with the gclient, but it can handle this
		// itself.
		
		container.frame.containerPanels.addElement(cp);
		
		cp.setBorder(wp.lineEmptyBorder);
		
		addElement("New Element", cp, true);
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
	System.out.println("This type is not supported yet.");
      }
  
    wp.gc.somethingChanged();
    invalidate();
    container.frame.validate();
  }

  /**
   * Add a new element to the vectorPanel.
   *
   * This element gets the default title, will not be expanded, and
   * will be invalidated.
   *
   * @param c Component to be added 
   */

  public void addElement(Component c)
  {
    addElement(null, c, false, true);
  }

  /**
   * Add a new element to the vectorPanel.
   *
   * This element gets the default title, and will not be expanded
   * immeditately.
   *
   * @param c Component to be added
   * @param invalidateNow If true, invalidate()/validate() will be called.  When adding several components all at once, set this to false.  
   */

  public void addElement(Component c, boolean invalidateNow)
  {
    addElement(null, c, false, invalidateNow);
  }

  /**
   * Add a new element to the vectorPanel.
   *
   * This element will not be expanded, but it will be invalidated
   * right away.
   *
   * @param title String used in the "title" of the elementWrapper
   * @param c Component to be added 
   */

  public void addElement(String title, Component c)
  {
    addElement(title,c,false, true);
  }


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
   * @param invalidateNow If true, invalidate()/validate() will be called.  When adding several components all at once, set this to false.
   */
  public void addElement(String title, Component c, boolean expand, boolean invalidateNow)
  {
    if (c == null)
      {
	throw new IllegalArgumentException("vectorPanel.addElement(): Component parameter is null");
      }

    setStatus("adding new elementWrapper");
      
    
    // Sometimes the element is added in with insertElementAt before we get here.
    if (!compVector.contains(c))
      {
	compVector.addElement(c);
      }

    if (debug)
      {
	System.out.println("Index of element: " + compVector.size());
      }

    // Make sure the centerPanel has been added.

    if (!centerPanelAdded)
      {
	add("Center", centerPanel);
	centerPanelAdded = true;
      }

    elementWrapper ew = new elementWrapper(title, c, this, editable && isFieldEditable());

    // Keep track of the elementWrappers in the ewHash.

    ewHash.put(c, ew);
    
    // centerPanel uses a BoxLayout(Y_AXIS), so calling add() will
    // just put the new component at the bottom(which is what we want)

    centerPanel.add(ew);

    // Only expand if it is a containerPanel.  If it is something else, there isn't anything to expand
    if ((c instanceof containerPanel) && (expand || isCreating))
      {
	ew.open();
      }

    // tell the containerPanel about us
    container.vectorElementAdded();

    if (invalidateNow)
      {
	invalidate();
      }

    setStatus("Done adding elementWrapper");

  }

  /**
   * Remove an element from the vector panel.
   *
   * @param c Component to be removed.
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
	    System.out.println("Deleting element number: " + compVector.indexOf(ew.getComponent()));
	  }

	ReturnVal retVal = my_field.deleteElement(ew.getObjectInvid());

	gc.handleReturnVal(retVal);

	if ((retVal == null) || (retVal.didSucceed()))
	  {
	    compVector.removeElement(ew.getComponent());
	    centerPanel.remove(ew);
	    
	    gc.somethingChanged();
	    invalidate();
	    container.frame.validate();
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
   *
   * Refresh the vectorPanel.
   *
   * This will refresh every containerPanel and IPField in this vectorPanel.
   *
   */

  public void refresh()
  {
    int size;
    int i;

    /* -- */

    try
      {
	size = my_field.size();

	for (i = 0; i < size; i++)
	  {
	    Object o = my_field.getElement(i);

	    if (o instanceof Invid)
	      {
		containerPanel cp = null;
		
		if (i < compVector.size())
		  {
		    cp = (containerPanel)compVector.elementAt(i);
		  }

		if ((cp != null) && cp.getObjectInvid().equals((Invid)o))
		  {
		    if (debug)
		      {
			System.out.println("Calling cp.updateAll()");
		      }

		    cp.updateAll();
		    ((elementWrapper)ewHash.get(cp)).checkValidation();
		    ((elementWrapper)ewHash.get(cp)).refreshTitle();
		  }
		else
		  {
		    System.out.println("There is no component added here, that is the problem.");

		    if (debug)
		      {
			System.out.println("VectorPanel.refresh(): need to add new element.");
		      }

		    ReturnVal rv = editable ? wp.gc.getSession().edit_db_object((Invid)o) : wp.gc.getSession().view_db_object((Invid)o);
		    rv = wp.gc.handleReturnVal(rv);
		    containerPanel newcp = new containerPanel(rv.getObject(),
				 			      editable,
					 		      wp.gc,
						 	      wp, container.frame,
							      null, false);	  
		    container.frame.containerPanels.addElement(newcp);
		    newcp.setBorder(wp.lineEmptyBorder);
		    
		    compVector.insertElementAt(newcp, i);
		    addElement(newcp);

		    System.out.println("Skipping non loaded cp");
		  }
	      

	      }
	    else if (o instanceof ip_field)
	      {
		if (i < compVector.size())
		  {
		    
		    JIPField ipf = (JIPField) compVector.elementAt(i);
		    
		    ipf.setValue((Byte[])my_field.getElement(i));
		  }
		else
		  {
		    ip_field ipfield = (ip_field) my_field;
		    JIPField ipf = new JIPField(editable,
						ipfield.v6Allowed());
		    
		    ipf.setValue((Byte[]) ipfield.getElement(i));
		    ipf.setCallback(this);
		    
		    compVector.insertElementAt(ipf, i);
		    addElement(ipf, false);

		  }
	      }
	    else
	      {
		System.err.println("Unknown type in vectorPanel.refresh compVector: " + compVector.elementAt(i));
	      }
	  }

	// Now get rid of everything after that last i
	for (int j = i; j < compVector.size(); j++)
	  {
	    centerPanel.remove((Component)ewHash.get(compVector.elementAt(i)));
	    compVector.removeElementAt(i);
	  }

      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("vectorPanel.refresh(): " + rx);
      }
  }

  /**
   * This just calls expandAllLevels.
   */
  public void run()
  {
    expandAllLevels();
  }
  
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
   * Expand all elements.
   *
   * @param recursive If true, it will expand any vector panels inside the containerPanels in this vectorPanel(in another thread, too)
   */ 
  public void expandLevels(boolean recursive)
  {
    if (recursive)
      {
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
   * Close all the levels
   *
   * @param recursive If true, close all vectorPanels inside this vectorPanel.  This one never spawns another thread.
   */
  public void closeLevels(boolean recursive)
  {
    Enumeration wrappers = ewHash.keys();

    setWaitCursor();
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
		    ((vectorPanel)cp.vectorPanelList.elementAt(i)).expandLevels(true);
		  }
	      }
	  }
      }
    
    // This is necessary for the closing, but the expanding doesn't need validate stuff.
    // Pretty weird, but thus is swing.
    invalidate();
    container.frame.validate();
    setNormalCursor();
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
   * The elementWrapper talks to us with JValueObjects.
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
    else if (v.getValue().equals("remove") )
      {
	if (debug)
	  {
	    System.out.println("You clicked on a minus");
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
	wp.gc.somethingChanged();
      }

    return returnValue;
  }

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
	
  /*
   * This changes an element after a ValueCallback.
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

  // convienence stuff

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

}


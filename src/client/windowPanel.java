/*

   windowPanel.java

   The window that holds the frames in the client.
   
   Created: 11 July 1997
   Version: $Revision: 1.58 $
   Last Mod Date: $Date: 1999/02/12 20:41:10 $
   Release: $Name:  $

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

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import java.awt.*;
import java.beans.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import arlut.csd.Util.PackageResources;
import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     windowPanel

------------------------------------------------------------------------------*/

/** 
 * windowPanel is the top level window controlling the framePanels.  There is one
 * windowPanel for each client.  windowPanel is responsible for adding new windows,
 * and maintaining the window list in the menubar.  
 */

public class windowPanel extends JDesktopPane implements InternalFrameListener, ActionListener{  

  boolean debug = true;

  final boolean debugProperty = false;
  
  // --

  gclient
    gc;

  int 
    topLayer = 0,
    windowCount = 0;

  Hashtable
    waitWindowHash = new Hashtable(),
    menuItems = new Hashtable(),
    windowList = new Hashtable();
  
  // This is used as the wait image in other classes.  Currently, it
  // returns the men at work animated gif.  Keep it here so each
  // subsequent pane doesn't have to load it.

  Image
    waitImage = null;

  JMenu
    windowMenu;

  // Load images for other packages

  ImageIcon
    // These are all for vectorPanel
    openIcon = new ImageIcon(PackageResources.getImageResource(this, "macdown_off.gif", getClass())),
    closeIcon = new ImageIcon(PackageResources.getImageResource(this, "macright_off.gif", getClass())),
    openPressedIcon = new ImageIcon(PackageResources.getImageResource(this, "macdown_on.gif", getClass())),
    closePressedIcon = new ImageIcon(PackageResources.getImageResource(this, "macright_on.gif", getClass())),
    removeImageIcon = new ImageIcon(PackageResources.getImageResource(this, "x.gif", getClass()));

  LineBorder
    blackLineB = new LineBorder(Color.black);

  EmptyBorder
    emptyBorder3 = (EmptyBorder)BorderFactory.createEmptyBorder(3,3,3,3),
    emptyBorder5 = (EmptyBorder)BorderFactory.createEmptyBorder(5,5,5,5),
    emptyBorder10 = (EmptyBorder)BorderFactory.createEmptyBorder(10,10,10,10),
    emptyBorder15 = (EmptyBorder)BorderFactory.createEmptyBorder(15,15,15,15);
  
  CompoundBorder
    eWrapperBorder = new CompoundBorder(emptyBorder3, new LineBorder(ClientColor.vectorTitles, 2)),
    lineEmptyBorder = new CompoundBorder(blackLineB, emptyBorder15);

  JMenuItem
    removeAllMI,
    toggleToolBarMI;

  /* -- */

  /**
   *
   * windowPanel constructor
   *
   */

  public windowPanel(gclient gc, JMenu windowMenu)
  {
    this.gc = gc;
    debug = gc.debug;
    this.windowMenu = windowMenu;

    // toggleToolBarMI was added to windowMenu in client
    // but we need to name it here so we can reference 
    // it in updateMenu. Note assumption that it is first item
    // in original windowMenu.
    this.toggleToolBarMI = windowMenu.getItem(0);

    if (debug)
      {
	System.out.println("Initializing windowPanel");
      }

    // This is supposed to give us window outline dragging, instead of
    // full window dragging.  Should be faster.

    putClientProperty("JDesktopPane.dragMode", "outline");

    removeAllMI = new JMenuItem("Remove All Windows");
    removeAllMI.setMnemonic('r');
    removeAllMI.addActionListener(this);

    updateMenu();

    setBackground(ClientColor.background);
  }

  /**
   * Get the parent gclient
   */

  public gclient getgclient()
  {
    return gc;
  }

  /**
   * Returns an image used as a generic "wait" image.
   *
   * Currently returns the men-at-work image.
   */

  public Image getWaitImage()
  {
    if (waitImage == null)
      {
	waitImage = PackageResources.getImageResource(this, "atwork01.gif", getClass());
      }
    
    return waitImage;
  }

  /**
   *
   * Create a new view-only window in this windowPanel.
   *
   * @param object an individual object from the server to show
   * in this window
   *
   */

  public void addWindow(db_object object)
  {
    this.addWindow(object, false, null);
  }

  /**
   *
   * Create a new window in this windowPanel.
   *
   * @param object an individual object from the server to show
   * in this window
   * @param editable If true, the new window will be editable
   */

  public void addWindow(db_object object, boolean editable)
  {
    this.addWindow(object, editable, null);
  }

  /**
   *
   * Create a new editable or view-only window in this windowPanel.
   *
   * @param object an individual object from the server to show
   * in this window
   * @param editable if true, the object will be presented as editable
   * @param objectType Used for the title of the new window
   *
   */

  public void addWindow(db_object object, boolean editable, String objectType)
  {
    this.addWindow(object, editable, objectType, false);
  }

  public void addWindow(db_object object, boolean editable, String objectType, boolean isNewlyCreated)
  {
    String temp, title;

    if (object == null)
      {
	gc.showErrorMessage("null object passed to addWindow.");
	return;
      }
    
    // We only want top level windows for top level objects.  No
    // embedded objects.

    try
      {
	while (object.isEmbedded())
	  {
	    db_field parent = object.getField(SchemaConstants.ContainerField);

	    if (parent == null)
	      {
		throw new IllegalArgumentException("Could not find the ContainerField of this " +
						   "embedded object: " + object);
	      }
	    
	    Invid i  = (Invid) ((invid_field)parent).getValue();

	    if (i == null)
	      {
		throw new RuntimeException("Invid value of ContainerField is null");
	      }
	    
	    if (editable)
	      {
		ReturnVal rv = gc.handleReturnVal(gc.getSession().edit_db_object(i));
		object = rv.getObject();

		if (object == null)
		  {
		    throw new RuntimeException("Could not call edit_db_object on " +
					       "the parent of this embedded object.");
		  }
	      }
	    else
	      {
		object = (gc.handleReturnVal(gc.getSession().view_db_object(i))).getObject();

		if (object == null)
		  {
		    throw new RuntimeException("Could not call view_db_object on " +
					       "the parent of this embedded object.");
		  }
	      }
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("RemoteException: " + rx);
      }

    if (editable)
      {
	gc.cancel.setEnabled(true);
      }

    gc.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    if (editable)
      {
	setStatus("Opening object for edit");
      }
    else
      {
	setStatus("Opening object for viewing");
      }

    // First figure out the title, and put it in the hash
    
    try
      {
	if (objectType == null)
	  {
	    objectType = object.getTypeName();
	  }

	if (isNewlyCreated)
	  {
	    title = "Create: " + objectType + " - " + "New Object";
	  }
	else
	  {
	    title = objectType + " - " + object.getLabel();

	    if (editable)
	      {
		title = "Edit: " + title;
	      }
	    else
	      {
		title = "View: " + title;
	      }
	  }

	if (debug)
	  {
	    System.out.println("Setting title to: " + title);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get label of object: " + rx);
      }
    
    // Create a unique title for the new window

    temp = title;
    int num = 2;

    while (windowList.containsKey(title))
      {
	title = temp + num++;
      }

    framePanel w = new framePanel(object, editable, this, title, isNewlyCreated);
    w.setOpaque(true);

    windowList.put(title, w);
      
    if (windowCount > 10)
      {
	windowCount = 0;
      }
    else
      {
	windowCount++;
      }

    w.setBounds(windowCount*20, windowCount*20, 500,400);

    w.setLayer(new Integer(topLayer));
    
    add(w);
    setSelectedWindow(w);

    updateMenu();
    
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  public void setSelectedWindow(JInternalFrame window)
  {
    Enumeration windows = windowList.keys();

    /* -- */
    
    while (windows.hasMoreElements())
      {
	JInternalFrame w = (JInternalFrame)windowList.get(windows.nextElement());

	try
	  {
	    w.setSelected(false);
	  }
	catch (java.beans.PropertyVetoException e)
	  {
	    System.out.println("Could not set selected false.  sorry.");
	  }
      }

    window.moveToFront();
    
    try
      {
	getDesktopManager().deiconifyFrame(window);
	window.setSelected(true);
	window.toFront();
      }
    catch (java.beans.PropertyVetoException e)
      {
	System.out.println("Could not set selected false.  sorry.");
      }
  }

  /**
   * Returns true if an edit window is open for this object.
   */

  public boolean isOpenForEdit(Invid invid)
  {
    Enumeration e = windowList.keys();

    /* -- */

    while (e.hasMoreElements())
      {
	Object o = windowList.get(e.nextElement());

	if (o instanceof framePanel)
	  {
	    framePanel fp = (framePanel) o;

	    if ((fp.isEditable()) && (fp.getObjectInvid().equals(invid)))
	      {
		return true;
	      }
	  }
      }

    return false;
  }

  /**
   * Calls gclient.setStatus
   */

  public final void setStatus(String s)
  {
    gc.setStatus(s);
  }

  public void resetWindowCount()
  {
    windowCount = 0;
  }

  /**
   * Add a table window.  Usually the output of a query.
   */

  public void addTableWindow(Session session, Query query, 
			     DumpResult results, String title)
  {
    gResultTable 
      rt = null;

    String 
      temp = title;

    int
      num;

    /* -- */

    if (results.resultSize() == 0)
      {
	gc.showErrorMessage("Query Result",
			    "No results were found to match your query.");
	return;
      }

    setStatus("Querying object types");
    gc.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    try
      {
	rt = new gResultTable(this, session, query, results);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("could not make results table: " + rx);
      }

    if (rt == null)
      {
	if (debug)
	  {
	    System.out.println("rt == null");
	  }

	setStatus("Could not get the result table.");
      }
    else
      {
	rt.setLayer(new Integer(topLayer));
	rt.setBounds(windowCount*20, windowCount*20, 500,500);
	rt.setResizable(true);
	rt.setClosable(true);
	rt.setMaximizable(true);
	rt.setIconifiable(true);

	rt.addInternalFrameListener(this);

	if (windowCount > 10)
	  {
	    windowCount = 0;
	  }
	else
	  {
	    windowCount++;
	  }

	// Figure out the title

	temp = title;
	num = 2;

	while (windowList.containsKey(title))
	  {
	    title = temp + num++;
	  }
	  
	// System.out.println("Setting title for query table to " + title);

	rt.setTitle(title);

	windowList.put(title, rt);
	  
	add(rt);
	setSelectedWindow(rt);

	updateMenu();
	setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	setStatus("Done.");
      }
  }

  public void addWaitWindow(Runnable key)
  {
    JInternalFrame frame = new JInternalFrame("Query loading");
    frame.setOpaque(true);
    ImageIcon icon = new ImageIcon(getWaitImage());
    frame.setBounds(10,10,icon.getIconWidth() + 180,icon.getIconHeight() + 35);
    
    frame.setIconifiable(true);
    frame.getContentPane().add(new JLabel("Waiting for query", icon, SwingConstants.CENTER));
    frame.setLayer(new Integer(topLayer));

    if (debug)
      {
	System.out.println("Adding wait window");
      }

    waitWindowHash.put(key, frame);

    add(frame);
  }

  public void removeWaitWindow(Runnable key)
  {
    JInternalFrame frame = (JInternalFrame)waitWindowHash.get(key);

    /* -- */

    if (frame == null)
      {
	System.out.println("Couldn't find window to remove.");
	return;
      }

    if (debug)
      {
	System.out.println("Removing wait window");
      }

    remove(frame);
    waitWindowHash.remove(frame);
  }

  /**
   *
   * Returns an Enumeration of all the windows.
   *
   */

  public Enumeration getWindows()
  {
    return windowList.elements();
  }

  /**
   * Returns a vector of framePanels of all the editable windows.
   */

  public Vector getEditables()
  {
    Vector editables = new Vector();
    JInternalFrame w;
    Enumeration windows;

    /* -- */
    
    windows = windowList.keys();      

    while (windows.hasMoreElements())
      {
	w = (JInternalFrame)windowList.get(windows.nextElement());

	if (w instanceof framePanel)
	  {
	    if (((framePanel)w).isEditable())
	      {
		editables.addElement(w);
	      }
	  }
      }
  
    return editables;
  }

  /**
   * Closes all windows that are open for editing.  
   *
   * <p>This should be called by the parent when the transaction is canceled, to get rid of
   * windows that might confuse the user.</p>
   */

  public void closeEditables()
  {
    Enumeration windows;

    /* -- */
    
    windows = windowList.keys();      

    while (windows.hasMoreElements())
      {
	Object o  = windowList.get(windows.nextElement());

	if (o instanceof framePanel)
	  {
	    framePanel w = (framePanel)o;
	    
	    if (w.isEditable())
	      {
		try
		  {
		    w.setClosed(true);
		  }
		catch (java.beans.PropertyVetoException ex)
		  {
		    throw new RuntimeException("beans? " + ex);
		  }
	      }
	  }
      }
  }
  
  /**
   * Closes all internal frames, editable or no.
   *
   */

  public void closeAll()
  {
    JInternalFrame w;
    Enumeration windows;

    /* -- */

    windows = windowList.keys();

    while (windows.hasMoreElements())
      {
	w = (JInternalFrame)windowList.get(windows.nextElement());

	if (w.isClosable())
	  {
	    try
	      {
		w.setClosed(true);
	      }
	    catch (java.beans.PropertyVetoException ex)
	      {
		throw new RuntimeException("beans? " + ex);
	      }
	  }
      }
  }

  public void closeWindow(String title)
  {
    JInternalFrame w;
    Enumeration windows;
      
    /* -- */
    
    setStatus("Closing a window");
    
    windows = windowList.keys();
    
    while (windows.hasMoreElements())
      {
	w = (JInternalFrame)windowList.get(windows.nextElement());
	  
	if (w.getTitle().equals(title))
	  {
	    if (w.isClosable())
	      {
		try 
		  {
		    w.setClosed(true);
		  }
		catch (java.beans.PropertyVetoException ex)
		  {
		    throw new RuntimeException("beans? " + ex);
		  }
	      }
	    else
	      {
		setStatus("You can't close that window.");
	      }
	    break;
	  }
      }
      
    setStatus("Done");
  }

  public JMenu updateMenu()
  {
    Enumeration windows;
    Object obj;
    JMenuItem MI;

    /* -- */

    try 
      {
	windowMenu.removeAll();
      }
    catch (NullPointerException e) 
      {
	// Swing 1.1 is picky, but don't complain publicly

	// System.err.println(e + " - windowMenu.removeAll() found nothing to remove.");
      }

    windowMenu.add(toggleToolBarMI);  
    windowMenu.add(removeAllMI);
    windowMenu.addSeparator();

    windows = windowList.keys();      

    while (windows.hasMoreElements())
      {
	obj = windowList.get(windows.nextElement());
	MI = null;

	if (obj instanceof framePanel)
	  {
	    if (debug)
	      {
		System.out.println("Adding menu item(fp): " + ((framePanel)obj).getTitle());
	      }

	    MI = new JMenuItem(((framePanel)obj).getTitle());
	  }
	else if (obj instanceof gResultTable)
	  {
	    if (debug)
	      {
		System.out.println("Adding menu item: " + ((gResultTable)obj).getTitle());
	      }

	    MI = new JMenuItem(((gResultTable)obj).getTitle());
	  }

	if (MI != null)
	  {
	    MI.setActionCommand("showWindow");
	    MI.addActionListener(this);
	    windowMenu.add(MI);
	  }
      }

    return windowMenu;
  }

  public void showWindow(String title)
  {
    Object obj = windowList.get(title);

    if (obj instanceof framePanel)
      {
	setSelectedWindow((framePanel)obj);
      }
    else if (obj instanceof gResultTable)
      {
	setSelectedWindow((gResultTable)obj);
      }
    else 
      {
	System.out.println("Hmm, don't know what kind of window this is.");
      }
  }

  public void refreshTableWindows()
  {
    Object obj;
    Enumeration enum = windowList.keys();

    while (enum.hasMoreElements())
      {
	obj = windowList.get(enum.nextElement());

	if (obj instanceof gResultTable)
	  {
	    ((gResultTable)obj).refreshQuery();
	  }
      }
  }

  // Event handlers

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() instanceof JMenuItem)
      {
	if (e.getSource() == removeAllMI)
	  {
	    closeAll();
	  }
	else
	  {
	    JMenuItem MI = (JMenuItem)e.getSource();
	    
	    if (e.getActionCommand().equals("showWindow"))
	      {
		showWindow(MI.getText());
	      }
	  }
      }
    else
      {
	System.err.println("Unknown ActionEvent in windowPanel");
      }
  }

  // This is for the beans, when a JInternalFrame closes

  public void internalFrameClosed(InternalFrameEvent event)
  {
    if (debug)
      {
	System.out.println("Closing an internal frame");
      }
    
    if (event.getSource() instanceof framePanel)
      {
	((framePanel)event.getSource()).stopLoading();
      }

    String oldTitle = ((JInternalFrame)event.getSource()).getTitle();
    
    if (oldTitle == null)
      {
	System.out.println("Title is null");
      }
    else
      {
	//System.out.println(" Removing button- " + oldTitle);
	
	windowList.remove(oldTitle);
	
	updateMenu();
      }
  }

  public void internalFrameDeiconified(InternalFrameEvent e) {}
  public void internalFrameClosing(InternalFrameEvent e) {}
  public void internalFrameActivated(InternalFrameEvent e) {}
  public void internalFrameDeactivated(InternalFrameEvent e) {}
  public void internalFrameOpened(InternalFrameEvent e) {}
  public void internalFrameIconified(InternalFrameEvent e) {}
 
  void addRow(JComponent parent, Component comp,  String label, int row)
  {
    JLabel l = new JLabel(label);
    parent.add("0 " + row + " lthwHW", l);
    parent.add("1 " + row + " lthwHW", comp);
  }
}


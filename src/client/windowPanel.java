/*

   windowPanel.java

   The window that holds the frames in the client.
   
   Created: 11 July 1997
   Version: $Revision: 1.26 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import tablelayout.*;
import com.sun.java.swing.*;
import com.sun.java.swing.event.*;


import java.awt.*;
import java.beans.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import jdj.PackageResources;

import arlut.csd.ganymede.*;
import arlut.csd.ganymede.client.*;

import arlut.csd.JDataComponent.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     windowPanel

------------------------------------------------------------------------------*/


public class windowPanel extends JDesktopPane implements PropertyChangeListener, ActionListener{  
  static final boolean debug = true;
  
  // --

  gclient
    parent;

  int 
    topLayer = 0,
    windowCount = 0;

  Hashtable
    menuItems = new Hashtable(),
    Windows = new Hashtable(),
    windowList = new Hashtable();

  Menu
    windowMenu;

  WindowBar 
    windowBar = null;

  /* -- */

  /**
   *
   * windowPanel constructor
   *
   */

  public windowPanel(gclient parent, Menu windowMenu)
  {
    if (debug)
      {
	System.out.println("Initializing windowPanel");
      }

    this.parent = parent;
    this.windowMenu = windowMenu;

    setBackground(ClientColor.background);

  }

  /**
   * Get the parent gclient
   */

  public gclient getgclient()
  {
    return parent;
  }

  /**
   *
   * Attach a WindowBar object to this windowPanel
   *
   */

  public void addWindowBar(WindowBar windowBar)
  {
    this.windowBar = windowBar;
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
    this.addWindow(object, false);
  }

  /**
   *
   * Create a new editable or view-only window in this windowPanel.
   *
   * @param object an individual object from the server to show
   * in this window
   * @param editable if true, the object will be presented as editable
   *
   */

  public void addWindow(db_object object, boolean editable)
  {
    String temp, title;

    if (object == null)
      {
	System.err.println("null object passed to addWindow.");
	return;
      }

    parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    if (editable)
      {
	parent.setStatus("Opening object for edit");
      }
    else
      {
	parent.setStatus("Opening object for viewing");
      }

    // First figure out the title, and put it in the hash
    
    try
      {
	title = object.getLabel();
	
	if (title == null)
	  {
	    title = new String("Null string");
	  }

	if (editable)
	  {
	    title = "Edit: " + title;
	  }
	else
	  {
	    title = "View: " + title;
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

    framePanel w = new framePanel(object, editable, this, title);

    windowList.put(title, w);
      
    if (windowBar != null)
      {
	windowBar.addButton(title);
      }

    if (windowCount > 10)
      {
	windowCount = 0;
      }
    else
      {
	windowCount++;
      }

    w.setBounds(windowCount*20, windowCount*20, 500,350);

    w.setLayer(new Integer(topLayer));
    
    add(w);
    moveToFront(w);
    updateMenu();
    
    parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  public void resetWindowCount()
  {
    windowCount = 0;
  }

  public void addTableWindow(Session session, Query query, DumpResult results, String title)
  {
    gResultTable 
      rt = null;

    String 
      temp = title;

    int
      num;

    /* -- */

    parent.setStatus("Querying object types");
    parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

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
	System.out.println("rt == null");
	parent.setStatus("Could not get the result table.");
      }
    else
      {
	rt.setLayer(new Integer(topLayer));
	rt.setBounds(windowCount*20, windowCount*20, 500,500);
	rt.setResizable(true);
	rt.setClosable(true);
	rt.setMaximizable(true);
	rt.setIconifiable(true);

	rt.addPropertyChangeListener(this);

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
	if (windowBar != null)
	  {
	    windowBar.addButton(title);
	  }
	  
	add(rt);
	moveToFront(rt);
	updateMenu();
	parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	parent.setStatus("Done.");
      }
    
  }

  // This makes a menu bar for the top of the JInternalFrames
  JMenuBar createMenuBar(boolean editable, db_object object, JInternalFrame w)
  {
    // Adding a menu bar, checking it out
    JMenuBar menuBar = new JMenuBar();
    menuBar.setBorderPainted(true);
    //menuBar.setBackground(ClientColor.WindowBG.darker());
    
    JMenu fileM = new JMenu("File");
    JMenu editM = new JMenu("Edit");
    menuBar.add(fileM);
    menuBar.add(editM);
    
    JMenuItem iconifyMI = new JMenuItem("Iconify");
    menuItems.put(iconifyMI, object);
    Windows.put(iconifyMI, w);
    iconifyMI.addActionListener(this);

    JMenuItem closeMI = new JMenuItem("Close");
    menuItems.put(closeMI , object);
    Windows.put(closeMI, w);
    closeMI.setEnabled(!editable);
    closeMI.addActionListener(this);

    JMenu deleteM = new JMenu("Delete");
    JMenuItem reallyDeleteMI = new JMenuItem("Yes, I'm sure");
    deleteM.add(reallyDeleteMI);
    menuItems.put(reallyDeleteMI, object);
    Windows.put(reallyDeleteMI, w);
    reallyDeleteMI.setActionCommand("ReallyDelete");
    reallyDeleteMI.addActionListener(this);
      
    JMenuItem saveMI = new JMenuItem("Save");
    Windows.put(saveMI, w);
    menuItems.put(saveMI , object);
    saveMI.setEnabled(false);
    saveMI.addActionListener(this);

    JMenuItem inactivateMI = new JMenuItem("Inactivate");
    Windows.put(inactivateMI, w);
    menuItems.put(inactivateMI, object);
    inactivateMI.addActionListener(this);
      
    JMenuItem setExpirationMI = new JMenuItem("Set Expiration Date");
    Windows.put(setExpirationMI, w);
    menuItems.put(setExpirationMI, object);
    setExpirationMI.addActionListener(this);

    JMenuItem setRemovalMI = new JMenuItem("Set Removal Date");
    Windows.put(setRemovalMI, w);
    menuItems.put(setRemovalMI, object);
    setRemovalMI.addActionListener(this);

    fileM.add(saveMI);
    fileM.add(inactivateMI);
    fileM.add(iconifyMI);
    fileM.add(deleteM);
    fileM.addSeparator();
    fileM.add(setExpirationMI);
    fileM.add(setRemovalMI);
    fileM.addSeparator();
    fileM.add(closeMI);
      
    JMenuItem queryMI = new JMenuItem("Query");
    Windows.put(queryMI, w);
    queryMI.addActionListener(this);
    menuItems.put(queryMI , object);
    JMenuItem editMI = new JMenuItem("Edit");
    Windows.put(editMI, w);
    menuItems.put(editMI , object);
    editMI.setEnabled(!editable);
    editMI.addActionListener(this);
    
    editM.add(queryMI);
    editM.add(editMI);
    
    return menuBar;
  }

  /**
   * Closes all the windows
   *
   */

  public void closeAll()
  {
    Enumeration windows = windowList.keys();      

    try
      {
	while (windows.hasMoreElements())
	  {
	    JInternalFrame w = (JInternalFrame)windowList.get(windows.nextElement());
	    w.setClosed(true);
	  }
      }
    catch (java.beans.PropertyVetoException ex)
      {
	throw new RuntimeException("beans? " + ex);
      }
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
	  
	// This seems backwards, but only non-editable windows are closable.
	// So if isClosable is false, then it is editable, and we should
	// close it.

	if (w.isClosable())
	  {
	    //This is a view window
	  }
	else
	  {
	    editables.addElement(w);
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
    JInternalFrame w;
    Enumeration windows;

    /* -- */
    
    windows = windowList.keys();      

    while (windows.hasMoreElements())
      {
	w = (JInternalFrame)windowList.get(windows.nextElement());
	  
	// This seems backwards, but only non-editable windows are closable.
	// So if isClosable is false, then it is editable, and we should
	// close it.

	if (w.isClosable())
	  {
	    //This is a view window
	  }
	else
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
  
  /**
   * Closes all non-editable windows
   *
   */

  public void closeNonEditables()
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
    
    parent.setStatus("Closing a window");
    
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
		parent.setStatus("You can't close that window.");
	      }
	    break;
	  }
      }
      
    parent.setStatus("Done");
  }

  public void maxWindow(String title)
  { 
    JInternalFrame w;
    Enumeration windows;
      
    /* -- */
    
    parent.setStatus("Maxing window");
    
    windows = windowList.keys();
      
    while (windows.hasMoreElements())
      {
	w = (JInternalFrame)windowList.get(windows.nextElement());
	  
	if (w.getTitle().equals(title))
	  {
	    try
	      {
		w.setMaximum(true);
	      }
	    catch (java.beans.PropertyVetoException ex)
	      {
		throw new RuntimeException("beans? " + ex);
	      }
	  }
      }
  }

  public Menu updateMenu()
  {
    Enumeration windows;
    Object obj;
    MenuItem MI;

    /* -- */

    windowMenu.removeAll();
    windows = windowList.keys();      

    while (windows.hasMoreElements())
      {
	obj = windowList.get(windows.nextElement());
	MI = null;

	if (obj instanceof JInternalFrame)
	  {
	    MI = new MenuItem(((JInternalFrame)obj).getTitle());
	  }
	else if (obj instanceof gResultTable)
	  {
	    MI = new MenuItem(((gResultTable)obj).getTitle());
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

    if (obj instanceof JInternalFrame)
      {
	((JInternalFrame)obj).moveToFront();
      }
    else if (obj instanceof gResultTable)
      {
	((gResultTable)obj).moveToFront();
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
    if (e.getSource() instanceof MenuItem)
      {
	if (e.getActionCommand().equals("showWindow"))
	  {
	    String label = ((MenuItem)e.getSource()).getLabel();
	    showWindow(label);
	  }
      }
    else if (e.getSource() instanceof JMenuItem)
      {
	System.out.println("Menu item action: " + e.getActionCommand());
	
	JMenuItem MI = (JMenuItem)e.getSource();
	if (e.getActionCommand().equals("Edit"))
	  {
	    if (debug)
	      {
		System.out.println("edit button clicked");
	      }
	    try
	      {
		if (debug)
		  {
		    System.out.println("Opening new edit window");
		  }
		addWindow(parent.session.edit_db_object(((db_object)menuItems.get(MI)).getInvid()), true);
	      }
	    catch (RemoteException rx)
	      {
		parent.setStatus("Something went wrong on the server.");
		throw new RuntimeException("Could not open object for edit: " + rx);
	      }
	  }
	else if (e.getActionCommand().equals("Clone"))
	  {
	    try
	      {
		if (debug)
		  {
		    System.out.println("Opening new edit window on the cloned object");
		  }
		//addWindow(parent.session.clone_db_object(((db_object)Buttons.get(button)).getInvid()), true);
		System.out.println("clone_db_object not there yet");
	      }
	    //catch (RemoteException rx)
	    catch (Exception rx)
	      {
		parent.setStatus("Something went wrong on the server.");
		throw new RuntimeException("Could not clone object: " + rx);
	      }
	  }
	else if (e.getActionCommand().equals("ReallyDelete"))
	  {
	    try
	      {
		if (debug)
		  {
		    System.out.println("Deleting object");
		  }
		parent.session.remove_db_object(((db_object)menuItems.get(MI)).getInvid());
		try
		  {
		    ((JInternalFrame)Windows.get(MI)).setClosed(true);

		  }
		catch (PropertyVetoException ex)
		  {
		    throw new RuntimeException("JInternalFrame will not close: " + ex);
		  }
	      }
	    catch (RemoteException rx)
	      {
		parent.setStatus("Something went wrong on the server.");
		throw new RuntimeException("Could not delete object: " + rx);
	      }
	  }
	else if (e.getActionCommand().equals("Close"))
	  {
	    try
	      {
		((JInternalFrame)Windows.get(MI)).setClosed(true);
		
	      }
	    catch (PropertyVetoException ex)
	      {
		throw new RuntimeException("JInternalFrame will not close: " + ex);
	      }
	  }
	else if (e.getActionCommand().equals("Iconify"))
	  {
	    try
	      {
		((JInternalFrame)Windows.get(MI)).setIcon(true);
	      }
	    catch (PropertyVetoException ex)
	      {
		throw new RuntimeException("JInternalFrame will not close: " + ex);
	      }
	  }
      	else if (e.getActionCommand().equals("Inactivate"))
	  {
	    System.out.println("Can't inactivate yet, right?");
	  }
	else if (e.getActionCommand().equals("Query"))
	  {
	    System.out.println("Not sure what a query should do");
	  }
	else if (e.getActionCommand().equals("Set Expiration Date"))
	  {
	    ((framePanel)Windows.get(MI)).addExpirationDatePanel();
	    ((framePanel)Windows.get(MI)).showExpirationDateTab();
	  }
	else if (e.getActionCommand().equals("Set Removal Date"))
	  {
	    ((framePanel)Windows.get(MI)).addRemovalDatePanel();
	    ((framePanel)Windows.get(MI)).showRemovalDateTab();
	  }
      }
    else
      {
	System.err.println("Unknown ActionEvent in windowPanel");
      }
  }

  // This is for the beans, when a JInternalFrame closes

  public void propertyChange(java.beans.PropertyChangeEvent event)
  {
    //System.out.println("propertyChange: " + event.getSource());
    //System.out.println("getPropertyName: " + event.getPropertyName());
    //System.out.println("getNewValue: " + event.getNewValue());

    if ((event.getPropertyName().equals("isClosed")) && ((Boolean)event.getNewValue()).booleanValue())
      {
	//System.out.println("It's isClosed and true");
	if (event.getSource() instanceof JInternalFrame)
	  {
	    //System.out.println("It's a JInternalFrame");
	    String oldTitle = ((JInternalFrame)event.getSource()).getTitle();
	      
	    if (oldTitle == null)
	      {
		System.out.println("Title is null");
	      }
	    else
	      {
		//System.out.println(" Removing button- " + oldTitle);
		  
		windowList.remove(oldTitle);
		if (windowBar != null)
		  {
		    windowBar.removeButton(oldTitle);
		  }
		updateMenu();
	      }
	  }
	else
	  {
	    System.out.println("propertyChange from something other than a JInternalFrame");
	  }
      }
  }
 
  void addRow(JComponent parent, Component comp,  String label, int row)
  {
    JLabel l = new JLabel(label);
    parent.add("0 " + row + " lthwHW", l);
    parent.add("1 " + row + " lthwHW", comp);
  }
}


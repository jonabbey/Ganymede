/*

   windowPanel.java

   The window that holds the frames in the client.
   
   Created: 11 July 1997
   Version: $Revision: 1.37 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import tablelayout.*;
import com.sun.java.swing.*;
import com.sun.java.swing.border.*;
import com.sun.java.swing.event.*;


import java.awt.*;
import java.beans.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import jdj.PackageResources;

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

public class windowPanel extends JDesktopPane implements PropertyChangeListener, ActionListener{  
  boolean debug = true;
  
  // --

  gclient
    gc;

  int 
    topLayer = 0,
    windowCount = 0;

  Hashtable
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
    openIcon = new ImageIcon(PackageResources.getImageResource(this, "macdown.gif", getClass())),
    closeIcon = new ImageIcon(PackageResources.getImageResource(this, "macright.gif", getClass())),
    removeImageIcon = new ImageIcon(PackageResources.getImageResource(this, "x.gif", getClass()));

  LineBorder
    blackLineB = new LineBorder(Color.black);

  EmptyBorder
    emptyBorder3 = (EmptyBorder)BorderFactory.createEmptyBorder(3,3,3,3),
    emptyBorder5 = (EmptyBorder)BorderFactory.createEmptyBorder(5,5,5,5),
    emptyBorder10 = (EmptyBorder)BorderFactory.createEmptyBorder(10,10,10,10),
    emptyBorder15 = (EmptyBorder)BorderFactory.createEmptyBorder(15,15,15,15);
  
  //BevelBorder
  //raisedBorder = new BevelBorder(BevelBorder.RAISED);
  
  CompoundBorder
  //emptyButtonBorder = new CompoundBorder(emptyBorder15, raisedBorder),
    eWrapperBorder = new CompoundBorder(emptyBorder3, new LineBorder(ClientColor.vectorTitles, 2)),
    lineEmptyBorder = new CompoundBorder(blackLineB, emptyBorder15);

  /* -- */

  /**
   *
   * windowPanel constructor
   *
   */

  public windowPanel(gclient gc, JMenu windowMenu)
  {
    if (debug)
      {
	System.out.println("Initializing windowPanel");
      }

    this.gc = gc;
    debug = gc.debug;
    this.windowMenu = windowMenu;

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
    String temp, title;

    if (object == null)
      {
	gc.showErrorMessage("null object passed to addWindow.");
	return;
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
	    title = object.getLabel();
	  }
	else
	  {
	    title = objectType + " - " + object.getLabel();
	  }

	// If we can't get a label, assume this is a newly created
	// object.  Is that a safe assumption?
	if ((title == null) || (title.equals("null")) || (title.equals("")))
	  {
	    if (objectType == null)
	      {
		title = "Create: New Object";
	      }
	    else
	      {
		title = "Create: " + objectType + " - " + "New Object";
	      }
	  }
	else
	  {
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

    framePanel w = new framePanel(object, editable, this, title);

    windowList.put(title, w);
      
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
    setSelectedWindow(w);

    updateMenu();
    
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  public void setSelectedWindow(JInternalFrame window)
  {
    Enumeration windows = windowList.keys();
    
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
    while (e.hasMoreElements())
      {
	Object o = windowList.get(e.nextElement());
	if (o instanceof framePanel)
	  {
	    framePanel fp = (framePanel)o;
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

  public void addTableWindow(Session session, Query query, DumpResult results, String title)
  {
    gResultTable 
      rt = null;

    String 
      temp = title;

    int
      num;

    /* -- */

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
	  
	add(rt);
	setSelectedWindow(rt);

	updateMenu();
	setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	setStatus("Done.");
      }
    
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

    windowMenu.removeAll();
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
	/*
	((framePanel)obj).moveToFront();
	try
	  {
	    ((framePanel)obj).setSelected(true);
	  }
	catch ( java.beans.PropertyVetoException e)
	  {
	    System.out.println("Couldn't select the window.");
	  }
	*/
      }
    else if (obj instanceof gResultTable)
      {
	setSelectedWindow((gResultTable)obj);
	/*
	((gResultTable)obj).moveToFront();
	try
	  {
	    ((gResultTable)obj).setSelected(true);
	  }
	catch ( java.beans.PropertyVetoException e)
	  {
	    System.out.println("Couldn't select the window.");
	    }*/
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
	JMenuItem MI = (JMenuItem)e.getSource();

	if (e.getActionCommand().equals("showWindow"))
	  {
	    showWindow(MI.getText());
	  }

      }
    else
      {
	System.err.println("Unknown ActionEvent in windowPanel");
      }
  }

  /*
  public void invalidate()
  {
    System.out.println("-- Invalidate windowPanel");
    super.invalidate();
  }

  public void validate()
  {
    System.out.println("-- validate windowPanel");
    super.validate();
  }
  */    


  // This is for the beans, when a JInternalFrame closes

  public void propertyChange(java.beans.PropertyChangeEvent event)
  {
    //System.out.println("propertyChange: " + event.getSource());
    //System.out.println("getPropertyName: " + event.getPropertyName());
    //System.out.println("getNewValue: " + event.getNewValue());

    if ((event.getPropertyName().equals("isClosed")) && ((Boolean)event.getNewValue()).booleanValue())
      {
	if (event.getSource() instanceof JInternalFrame)
	  {
	    if (debug)
	      {
		System.out.println("Closing an internal frame");
	      }

	    if (event.getSource() instanceof framePanel)
	      {
		((framePanel)event.getSource()).stopLoading();
	      }
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


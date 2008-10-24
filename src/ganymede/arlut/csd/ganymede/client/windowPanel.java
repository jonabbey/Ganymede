/*

   windowPanel.java

   The window that holds the frames in the client.
   
   Created: 11 July 1997

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2008
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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import arlut.csd.JDialog.StringDialog;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.DumpResult;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.Session;
import arlut.csd.ganymede.rmi.db_field;
import arlut.csd.ganymede.rmi.db_object;
import arlut.csd.ganymede.rmi.invid_field;

import foxtrot.Task;
import foxtrot.Worker;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     windowPanel

------------------------------------------------------------------------------*/

/**
 * windowPanel is the top level panel containing and controlling the
 * internal {@link arlut.csd.ganymede.client.framePanel} and
 * {@link arlut.csd.ganymede.client.gResultTable gResultTable} windows
 * that are displayed in reaction to actions taken by the user.
 * windowPanel is responsible for adding these windows, and maintaining
 * the window list in the menubar.
 *
 * windowPanel is also responsible for displaying and removing the
 * internal 'guy working' status window that lets the user know the client
 * hasn't frozen up when it is processing a query request.
 *
 * @version $Id$
 * @author Mike Mulvaney
 */

public class windowPanel extends JDesktopPane implements InternalFrameListener, ActionListener {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.windowPanel");

  /**
   * Constant, the front-most layer in which newly created windows are
   * placed.
   */

  final static int topLayer = 0;

  // --

  boolean debug = false;

  /**
   * Reference to the client's main class, used for some utility functions.
   */

  gclient
    gc;

  /**
   * Used to keep track of multiple 'guy working' internal wait windows
   * if we have multiple threads waiting for query results from the server.
   *
   * This hashtable maps Runnable objects (objects downloading query results
   * in their own threads) to JInternalFrame's.
   */
  
  Hashtable waitWindowHash = new Hashtable();

  /**
   * Hashtable mapping window titles to JInternalFrames.  Used
   * to make sure that we have unique titles for all of our
   * internal windows, so that we can properly maintain a 
   * Windows menu to let the user select an active window from
   * the menu bar.
   */

  private Hashtable windowList = new Hashtable();
  
  /**
   * This is used as the wait image in other classes.  Currently, it
   * returns the men at work animated gif.  Keep it here so each
   * subsequent pane doesn't have to load it.
   */

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
    emptyBorder10Right = (EmptyBorder)BorderFactory.createEmptyBorder(10,10,10,0),
    emptyBorder15 = (EmptyBorder)BorderFactory.createEmptyBorder(15,15,15,15);
  
  CompoundBorder
    eWrapperBorder = new CompoundBorder(emptyBorder3, new LineBorder(ClientColor.vectorTitles, 2)),
    eWrapperBorderInvalid = new CompoundBorder(emptyBorder3, new LineBorder(ClientColor.vectorTitlesInvalid, 2)),
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
    setDesktopManager(new clientDesktopMgr());

    this.gc = gc;

    // are we running the client in debug mode?

    if (!debug)
      {
	debug = gc.debug;
      }

    this.windowMenu = windowMenu;

    // toggleToolBarMI was added to windowMenu in client
    // but we need to name it here so we can reference 
    // it in updateWindowMenu. Note assumption that it is first item
    // in original windowMenu.

    this.toggleToolBarMI = windowMenu.getItem(0);

    if (debug)
      {
	System.err.println("Initializing windowPanel");
      }

    // This is supposed to give us window outline dragging, instead of
    // full window dragging.  Should be faster.

    putClientProperty("JDesktopPane.dragMode", "outline");

    // "Remove All Windows"
    removeAllMI = new JMenuItem(ts.l("init.windows_menu_0"));

    String pattern = ts.l("init.windows_menu_0_key_optional");

    if (pattern != null)
      {
	removeAllMI.setMnemonic((int) pattern.charAt(0));
      }

    removeAllMI.addActionListener(this);

    updateWindowMenu();

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
   * Create a new editable or view-only window in this windowPanel.
   *
   * @param object an individual object from the server to show
   * in this window
   * @param editable if true, the object will be presented as editable
   * @param objectType Used for the title of the new window
   */

  public void addWindow(Invid invid, db_object object, boolean editable, String objectType)
  {
    this.addWindow(invid, object, editable, objectType, false, null);
  }

  /**
   * Create a new editable or view-only window in this windowPanel.
   *
 * @param object an individual object from the server to show
   * in this window
   * @param editable if true, the object will be presented as editable
   * @param objectType Used for the title of the new window
   * @param originalWindow If not null, a framePanel that we are going to be replacing
   * with a new window.  Used to replace a view window with an edit window, or to refresh
   * a view window.
   */

  public void addWindow(Invid invid, db_object object, boolean editable, String objectType, framePanel originalWindow)
  {
    this.addWindow(invid, object, editable, objectType, false, originalWindow);
  }

  /**
   * Create a new editable or view-only window in this windowPanel.
   *
   * @param object an individual object from the server to show
   * in this window
   * @param editable if true, the object will be presented as editable
   * @param objectType Used for the title of the new window
   * @param isNewlyCreated if true, this window will be a 'create object' window.
   * @param originalWindow If not null, a framePanel that we are going to be replacing
   * with a new window.  Used to replace a view window with an edit window, or to refresh
   * a view window.
   */

  public void addWindow(Invid invid, db_object object, boolean editable, String objectType, boolean isNewlyCreated, framePanel originalWindow)
  {
    Invid finalInvid = invid;
    String title = null;

    /* -- */

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
	    
	    finalInvid  = (Invid) ((invid_field)parent).getValue();

	    if (finalInvid == null)
	      {
		throw new RuntimeException("Invid value of ContainerField is null");
	      }
	    
	    if (editable)
	      {
		ReturnVal rv = gc.handleReturnVal(gc.getSession().edit_db_object(finalInvid));
		object = (db_object) rv.getObject();

		if (object == null)
		  {
		    throw new RuntimeException("Could not call edit_db_object on " +
					       "the parent of this embedded object.");
		  }
	      }
	    else
	      {
		object = (db_object) (gc.handleReturnVal(gc.getSession().view_db_object(finalInvid))).getObject();

		if (object == null)
		  {
		    throw new RuntimeException("Could not call view_db_object on " +
					       "the parent of this embedded object.");
		  }
	      }
	  }
      }
    catch (Exception rx)
      {
	gc.processExceptionRethrow(rx);
      }

    gc.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    try
      {
	if (editable)
	  {
	    // "Opening object for editing"
	    setStatus(ts.l("addWindow.editing_status"), 1);
	  }
	else
	  {
	    // "Opening object for viewing"
	    setStatus(ts.l("addWindow.viewing_status"), 1);
	  }

	// First figure out the title, and put it in the hash
    
	try
	  {
	    if (objectType == null)
	      {
		objectType = gc.getObjectType(finalInvid);
	      }

	    if (isNewlyCreated)
	      {
		title = getWindowTitle(editable, isNewlyCreated, objectType, null);
	      }
	    else
	      {
		title = getWindowTitle(editable, isNewlyCreated, objectType, object.getLabel());
	      }
	  }
	catch (Exception rx)
	  {
	    gc.processExceptionRethrow(rx, "Could not get label of object: ");
	  }
    
	framePanel w = null;

	try
	  {
	    /*
	      This is a big opportunity to spin the loading and
	      creation of the framepanel off of the AWT/Swing Event
	      Dispatch Thread through the use of Foxtrot, as if we can
	      synchronously block execution of this thread of
	      execution on the EDT while still allowing the EDT to
	      chew through other stuff, we can allow for the refresh
	      of the GUI while we're getting this internal frame
	      created.  That still means we won't pop anything up on
	      screen until the load is completed, but at least we'll
	      have refresh in the meantime.
	      
	      This wouldn't even be feasible, except that we would know that
	      this framePanel won't be made visible (and hence subject to
	      heavy Swing threading constraints) until this constructor
	      returns.
	      
	      That's foxtrot.sourceforge.net, yo.
	    */

	    final Invid localFinalInvid = finalInvid;
	    final db_object localObject = object;
	    final boolean localEditable = editable;
	    final windowPanel localWindowPanel = this;
	    final String localTitle = title;
	    final boolean localIsNewlyCreated = isNewlyCreated;

	    // don't let the user log out while we're creating a new
	    // window in the background

	    gc.logoutMI.setEnabled(false);

	    w = (framePanel) foxtrot.Worker.post(new foxtrot.Task()
	      {
		public Object run() throws Exception
		{
		  try
		    {
		      framePanel foxFP = new framePanel(localFinalInvid,
							localObject, 
							localEditable,
							localWindowPanel,
							localIsNewlyCreated);

		      localWindowPanel.setWindowTitle(foxFP, localTitle);
		      return foxFP;
		    }
		  catch (Throwable ex)
		    {
		      gc.processExceptionRethrow(ex);
		      return null;
		    }
		}
	      }
						    );
	  }
	catch (Exception ex)
	  {
	    gc.processExceptionRethrow(ex);
	  }
	finally
	  {
	    gc.logoutMI.setEnabled(true);
	  }

	w.setOpaque(true);

        if (originalWindow != null)
          {
            w.setBounds(originalWindow.getBounds());
            w.setLayer(new Integer(topLayer));
            add(w);
            w.setVisible(true);
            setSelectedWindow(w);
            originalWindow.closingApproved = true;
            originalWindow.setClosed(true);
          }
        else
          {
            w.setBounds(0, 0, 535, 400);
            placeWindow(w);
            w.setLayer(new Integer(topLayer));
	
            add(w);
            w.setVisible(true);
            setSelectedWindow(w);
          }

	// turn the cancel button on once the window has appeared.

	if (editable)
	  {
	    gc.cancel.setEnabled(true);
	  }
      }
    catch (Throwable ex)
      {
	gc.processException(ex);
      }
    finally
      {
	// if we have an exception creating the framePanel, don't leave
	// the cursor frozen in wait

	gc.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
  }

  /**
   * This method returns a localized string for a framePanel title,
   * according to the operation being performed on the object and its
   * type.
   *
   * It is used in addWindow() to set a window's initial title, and by
   * framePanel to handle relabeling an object whose label field has been
   * altered.
   */

  public String getWindowTitle(boolean editable, boolean newlyCreated, String objectType, String object_label)
  {
    if (newlyCreated)
      {
	if (object_label == null)
	  {
	    // "Create: {0} - New Object"
	    return ts.l("getWindowTitle.create_object_title", objectType);
	  }
	else
	  {
	    // "Create: {0} - {1}"
	    return ts.l("getWindowTitle.create_object_title2", objectType, object_label);
	  }
      }

    if (editable)
      {
	// "Edit: {0} - {1}"
	return ts.l("getWindowTitle.edit_object_title", objectType, object_label);
      }
    else
      {
	// "View: {0} - {1}"
	return ts.l("getWindowTitle.view_object_title", objectType, object_label);
      }
  }

  /**
   * This method is responsible for setting the bounds for a new window
   * so that windows are staggered somewhat.
   */

  public void placeWindow(JInternalFrame window)
  {
    int x_offset;
    int y_offset;
    int width = window.getBounds().width;
    int height = window.getBounds().height;
    int stagger_offset = windowList.size() * 20;
    
    Dimension d = this.getSize();
    java.util.Random randgen = new java.util.Random();

    /* -- */

    if (width > d.width || height > d.height)
      {
	window.setBounds(0,0, width, height);
      }
    else if (width + stagger_offset < d.width && height + stagger_offset < d.height)
      {
	window.setBounds(stagger_offset, stagger_offset, width, height);
      }
    else
      {
	x_offset = (int) ((d.width - width) * randgen.nextFloat());
	y_offset = (int) ((d.height - height) * randgen.nextFloat());
	
	window.setBounds(x_offset, y_offset, width, height);
      }
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
	    System.err.println("Could not set selected false.  sorry.");
	  }
      }

    window.moveToFront();
    
    try
      {
	window.setSelected(true);
	window.toFront();
      }
    catch (java.beans.PropertyVetoException e)
      {
	System.err.println("Could not set selected false.  sorry.");
      }
  }

  /**
   * Returns true if an edit window is open for this object.
   */

  public boolean isOpenForEdit(Invid invid)
  {
    Object ary[];

    /* -- */

    synchronized (windowList)
      {
	ary = windowList.values().toArray();
      }

    for (int i = 0; i < ary.length; i++)
      {
	Object o = ary[i];

	if (o instanceof framePanel)
	  {
	    framePanel fp = (framePanel) o;

	    // we may have a view window and an edit window, so we
	    // need to scan over all editable windows, not just stop
	    // when we see a non-editable window with the invid we are
	    // looking for.

	    if (fp.isEditable() && fp.getObjectInvid().equals(invid))
	      {
		return true;
	      }
	  }
      }

    return false;
  }
  
  /**
   * Returns true if an editable window corresponding to the invid
   * exists and is ready to close.
   */

  public boolean isApprovedForClosing(Invid invid)
  {
    Object ary[];

    /* -- */

    synchronized (windowList)
      {
	ary = windowList.values().toArray();
      }

    for (int i = 0; i < ary.length; i++)
      {
	Object o = ary[i];

	if (o instanceof framePanel)
	  {
	    framePanel fp = (framePanel) o;

	    // we may have a view window and an edit window, so we
	    // need to scan over all editable windows, not just stop
	    // when we see a non-editable window with the invid we are
	    // looking for.

	    if (fp.isEditable() && fp.getObjectInvid().equals(invid))
	      {
		return fp.isApprovedForClosing();
	      }
	  }
      }

    return false;
  }

  /**
   * Convenience method, calls
   * {@link arlut.csd.ganymede.client.gclient#setStatus(java.lang.String) gclient.setStatus}
   * to set some text in the client's status bar, with a time-to-live of the specified number
   * of seconds.
   */

  public final void setStatus(String s, int seconds)
  {
    gc.setStatus(s, seconds);
  }

  /**
   * Convenience method, calls
   * {@link arlut.csd.ganymede.client.gclient#setStatus(java.lang.String) gclient.setStatus}
   * to set some text in the client's status bar, with a time-to-live of the
   * default 5 seconds.
   */

  public final void setStatus(String s)
  {
    gc.setStatus(s);
  }

  /**
   * Create and add an internal query result table window.
   *
   * @param session Reference to the server, used to refresh the query on command
   * @param query The Query whose results are being shown in this window, used to
   * refresh the query on command.
   * @param results The results of the query that is being shown in this window.
   */

  public void addTableWindow(Session session, Query query, DumpResult results)
  {
    gResultTable 
      rt = null;

    int
      num;

    /* -- */

    if (results.resultSize() == 0)
      {
	final gclient my_gc = gc;
	
	EventQueue.invokeLater(new Runnable() 
				   {
				     public void run()
				       {
					 // "Query Result"
					 // "No results were found to match your query."
					 // "Try Again"
					 StringDialog d = new StringDialog(my_gc,
									   ts.l("addTableWindow.query_result_subj"),
									   ts.l("addTableWindow.query_result_txt"),
									   ts.l("addTableWindow.query_result_try_again"),
									   StringDialog.getDefaultCancel());

					 if (d.showDialog() != null)
					   {
					     my_gc.postQuery(null);
					   }
				       }
				   });
	return;
      }

    // "Querying object types"
    setStatus(ts.l("addTableWindow.querying_status"), 1);
    gc.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    try
      {
	rt = new gResultTable(this, session, query, results);
      }
    catch (Exception rx)
      {
	// "Could not make results table"
	gc.processExceptionRethrow(rx, ts.l("addTableWindow.resulting_exception"));
      }

    if (rt == null)
      {
	if (debug)
	  {
	    System.err.println("rt == null");
	  }

	// "Could not get the result table."
	setStatus(ts.l("addTableWindow.failure_status"));

        return;
      }

    rt.setLayer(new Integer(topLayer));
    rt.setBounds(0, 0, 500,500);
    rt.setResizable(true);
    rt.setClosable(true);
    rt.setMaximizable(true);
    rt.setIconifiable(true);

    placeWindow(rt);

    add(rt);
    rt.setVisible(true);	// for Kestrel
    setSelectedWindow(rt);

    rt.addInternalFrameListener(this);

    gc.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

    setStatus(ts.l("addTableWindow.done"), 1); // "Done."

    rt.getToolBar().grabFocus();
  }

  /**
   * Pops up an internal 'wait..' window, showing an animated icon of a
   * guy working.  Used to show the client is still working while a query
   * is being processed.
   */

  public void addWaitWindow(Runnable key)
  {
    // "Query Loading.."
    JInternalFrame frame = new JInternalFrame(ts.l("addWaitWindow.title"));
    frame.setOpaque(true);
    ImageIcon icon = new ImageIcon(getWaitImage());
    frame.setBounds(10,10,icon.getIconWidth() + 180,icon.getIconHeight() + 35);
    frame.setIconifiable(true);

    // "Waiting for query"
    frame.getContentPane().add(new JLabel(ts.l("addWaitWindow.label"),
					  icon,
					  SwingConstants.CENTER));
    frame.setLayer(new Integer(topLayer));

    if (debug)
      {
	System.err.println("Adding wait window");
      }

    waitWindowHash.put(key, frame);

    add(frame);
    frame.setVisible(true);	// for Kestrel
    setSelectedWindow(frame);
  }

  /**
   * Pops down the internal 'wait..' window.
   */

  public void removeWaitWindow(Runnable key)
  {
    JInternalFrame frame = (JInternalFrame)waitWindowHash.get(key);

    /* -- */

    if (frame == null)
      {
	if (debug)
	  {
	    System.err.println("Couldn't find window to remove.");
	  }

	return;
      }

    if (debug)
      {
	System.err.println("Removing wait window");
      }

    frame.setClosable(true);
    
    try
      {
	frame.setClosed(true);
      }
    catch (java.beans.PropertyVetoException ex)
      {
	throw new RuntimeException("beans? " + ex);
      }

    waitWindowHash.remove(frame);
  }

  /**
   * Returns a vector of framePanels of all the editable windows.
   */

  public Vector getEditables()
  {
    Vector editables = new Vector();
    Object ary[];

    /* -- */

    synchronized (windowList)
      {
	ary = windowList.values().toArray();
      }

    for (int i = 0; i < ary.length; i++)
      {
	Object o = ary[i];

	if (o instanceof framePanel)
	  {
	    if (((framePanel)o).isEditable())
	      {
		editables.addElement(o);
	      }
	  }
      }

    return editables;
  }

  /**
   * Closes all windows that are open for editing.
   *
   * This should be called by the parent when the transaction is canceled, to get rid of
   * windows that might confuse the user.
   */

  public void closeEditables()
  {
    Object ary[];

    /* -- */

    synchronized (windowList)
      {
	ary = windowList.values().toArray();
      }

    for (int i = 0; i < ary.length; i++)
      {
	Object o  = ary[i];
	
	if (o instanceof framePanel)
	  {
	    framePanel w = (framePanel)o;
		
	    if (w.isEditable())
	      {
		if (debug)
		  {
		    System.err.println("closing editables.. " + w.getTitle());
		  }
		
		try
		  {
		    w.closingApproved = true;
		    w.setClosed(true);
		  }
		catch (java.beans.PropertyVetoException ex)
		  {
		    // shouldn't happen here
		  }
	      }
	  }
      }
  }

  /**
   * Closes all windows that show a view onto the given Invid
   *
   * This should be called by the parent when the transaction is
   * canceled, to get rid of windows viewing deleted objects.
   */

  public void closeInvidWindows(Invid invid)
  {
    Object ary[];

    /* -- */

    synchronized (windowList)
      {
	ary = windowList.values().toArray();
      }

    for (int i = 0; i < ary.length; i++)
      {
	Object o  = ary[i];
	
	if (o instanceof framePanel)
	  {
	    framePanel w = (framePanel)o;

            if (w.getObjectInvid().equals(invid))
              {
		try
		  {
		    w.closingApproved = true;
		    w.setClosed(true);
		  }
		catch (java.beans.PropertyVetoException ex)
		  {
		    // shouldn't happen here
		  }
              }
	  }
      }
  }
  
  /**
   * Closes all internal frames, editable or no.
   *
   * @param askNoQuestions if true, closeAll() will inhibit the normal
   * dialogs brought up when create/editable windows are closed.
   */

  public void closeAll(boolean askNoQuestions)
  {
    Object ary[];

    /* -- */

    synchronized (windowList)
      {
	ary = windowList.values().toArray();
      }

    for (int i = 0; i < ary.length; i++)
      {
	JInternalFrame o = (JInternalFrame) ary[i];
	
	if (o instanceof framePanel)
	  {
	    framePanel w = (framePanel)o;
		
	    if (!w.isClosable())
	      {
		w.setClosable(true);
	      }
	
	    if (debug)
	      {
		System.err.println("windowPanel.closeAll() - closing window " + w.getTitle());
	      }

	    try
	      {
		if (askNoQuestions)
		  {
                    w.stopNow();	// stop all container threads asap
                    w.closingApproved = true;
		  }

		w.setClosed(true);
	      }
	    catch (java.beans.PropertyVetoException ex)
	      {
		// user decided against this one..
	      }
	  }
	else if (o instanceof gResultTable)
	  {
	    gResultTable w = (gResultTable) o;

	    try
	      {
		w.setClosed(true);
	      }
	    catch (java.beans.PropertyVetoException ex)
	      {
		// something decided against this one.. oh well.
	      }
	  }
      }
  }

  /**
   *
   * This method attempts to close an internal window in the client,
   * as identified by title.  This method will not close windows (as
   * for newly created objects) that are not set to be closeable.
   *
   */

  public void closeWindow(String title)
  {
    JInternalFrame w;
      
    /* -- */

    // "Closing a window."
    setStatus(ts.l("closeWindow.closing_status"), 1);
    
    w = (JInternalFrame)windowList.get(title);

    if (w != null && w.isClosable())
      {
	if (debug)
	  {
	    System.err.println("windowPanel.java closing " + title);
	  }

	if (w instanceof framePanel)
	  {
	    ((framePanel) w).closingApproved = true;
	  }

	try 
	  {
	    w.setClosed(true);
	  }
	catch (java.beans.PropertyVetoException ex)
	  {
	    // okay, so the user decided against it.
	  }

	// "Window closed."
	setStatus(ts.l("closeWindow.yes_sir_status"), 1);
      }
    else
      {
	// "You can''t close that window."
	setStatus(ts.l("closeWindow.no_sir_status"));
      }
  }

  /**
   * This method handles the generation and setting of a unique
   * window title for a window that we are displaying.  The titles for
   * all windows are tracked in windowPanel's windowList, and this
   * method takes care of updating windowList as necessary with the
   * new title.
   *
   * This method can also be used to change a pre-existing window's
   * title, in which case the old title is removed from the windowList in favor
   * of the new one.
   */

  public String setWindowTitle(JInternalFrame frame, String proposedTitle)
  {
    String title = proposedTitle;
    int num = 2;
    Enumeration en;

    /* -- */

    synchronized (windowList)
      {
	// if the frame we're dealing with is already in the windowList
	// hash, remove the old title.

	en = windowList.keys();

	while (en.hasMoreElements())
	  {
	    String enTitle = (String) en.nextElement();
	    JInternalFrame enWindow = (JInternalFrame) windowList.get(enTitle);

	    if (enWindow == frame)
	      {
		windowList.remove(enTitle);
		break;		// we've killed the enumeration, stop looping
	      }
	  }

	// now find a title for the window and set it
	
	while (windowList.containsKey(title))
	  {
	    title = proposedTitle + " <" + num++ + ">";
	  }
	
	windowList.put(title, frame);

	frame.setTitle(title);

	updateWindowMenu();
      }

    return title;		// in case the caller cares about what unique title we wound up with
  }

  public JMenu updateWindowMenu()
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

    boolean emptyList = true;

    synchronized (windowList)
      {
	windows = windowList.keys();

	while (windows.hasMoreElements())
	  {
	    obj = windowList.get(windows.nextElement());
	    MI = null;

	    if (obj instanceof framePanel)
	      {
		if (debug)
		  {
		    System.err.println("Adding menu item(fp): " + ((framePanel)obj).getTitle());
		  }
		
		MI = new JMenuItem(((framePanel)obj).getTitle());
	      }
	    else if (obj instanceof gResultTable)
	      {
		if (debug)
		  {
		    System.err.println("Adding menu item: " + ((gResultTable)obj).getTitle());
		  }
		
		MI = new JMenuItem(((gResultTable)obj).getTitle());
	      }

	    if (MI != null)
	      {
		MI.setActionCommand("showWindow");
		MI.addActionListener(this);
		windowMenu.add(MI);
		emptyList = false;
	      }
	  }
      }

    if (emptyList)
      {
	try
	  {
	    gc.tree.requestFocus();
	  }
	catch (NullPointerException ex)
	  {
	    // if we're closing down, we might have zeroed out the
	    // tree reference already.
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
    else if (debug)
      {
	System.err.println("Hmm, don't know what kind of window this is:" + obj);
      }
  }

  /**
   * This method causes all query result windows to be refreshed,
   * with each query window's query re-issued to the Ganymede
   * server.
   */

  public void refreshTableWindows()
  {
    Object ary[];

    /* -- */

    synchronized (windowList)
      {
	ary = windowList.values().toArray();
      }

    for (int i = 0; i < ary.length; i++)
      {
	if (ary[i] instanceof gResultTable)
	  {
	    ((gResultTable) ary[i]).refreshQuery();
	  }
      }
  }

  /**
   * This method takes an {@link arlut.csd.ganymede.common.Invid}
   * and a {@link arlut.csd.ganymede.common.ReturnVal} that encode
   * field refresh information, and update any open windows with the
   * appropriate information.
   *
   * If invid is set, the only windows which will be refreshed are
   * those that are presenting that object for display or editing.  If
   * it is null, all object windows will be refreshed.
   *
   * The retVal parameter can hold a list of fields that need to be
   * refreshed, or an encoding that forces a refresh of all fields.  If
   * the retVal parameter is null, all fields on all windows that
   * match the invid parameter will be refreshed.
   *
   * If both invid and retVal are null, all fields in all object windows
   * will be refreshed.
   */

  public void refreshObjectWindows(Invid invid, ReturnVal retVal)
  {
    Object ary[];

    /* -- */

    synchronized (windowList)
      {
	ary = windowList.values().toArray();
      }

    for (int i = 0; i < ary.length; i++)
      {
	if (ary[i] instanceof framePanel)
	  {
	    ((framePanel) ary[i]).updateContainerPanels(invid, retVal);
	  }
      }
  }

  /**
   * This method seeks through all open windows and relabels all
   * references to the given Invid.
   */

  public void relabelObject(Invid invid, String newLabel)
  {
    Object ary[];

    /* -- */

    synchronized (windowList)
      {
	ary = windowList.values().toArray();
      }

    for (int i = 0; i < ary.length; i++)
      {
	if (ary[i] instanceof framePanel)
	  {
	    ((framePanel) ary[i]).relabelObject(invid, newLabel);
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
	    closeAll(false);
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
    String oldTitle = ((JInternalFrame)event.getSource()).getTitle();
    
    if (oldTitle == null)
      {
	System.err.println("Title is null");
      }
    else
      {
        windowList.remove(oldTitle);
	
	updateWindowMenu();
      }

    if (debug)
      {
	System.err.println("windowPanel.internalFrameClosed(): exiting");
      }
  }

  public void internalFrameDeiconified(InternalFrameEvent e) {}

  public void internalFrameClosing(InternalFrameEvent e)
  {
    // For some reason, I'm seeing internalFrameClosing() called on
    // gResultTable when the user clicks on the close icon, but
    // internalFrameClosed() is not.
    //
    // So, I'm going to remove the gResultTable from our windowList
    // when the close icon is being processed, here.

    if (e.getSource() instanceof gResultTable)
      {
        gResultTable rt = (gResultTable) e.getSource();

        windowList.remove(rt.getTitle());

	updateWindowMenu();
      }
  }

  public void internalFrameActivated(InternalFrameEvent e) {}
  public void internalFrameDeactivated(InternalFrameEvent e) {}
  public void internalFrameOpened(InternalFrameEvent e) {}
  public void internalFrameIconified(InternalFrameEvent e) {}
}

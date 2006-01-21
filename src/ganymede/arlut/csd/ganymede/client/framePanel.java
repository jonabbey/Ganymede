/*

   framePanel.java

   The individual frames in the windowPanel.
   
   Created: 4 September 1997

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2006
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import arlut.csd.JDialog.StringDialog;
import arlut.csd.Util.booleanSemaphore;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.StringUtils;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.BaseDump;
import arlut.csd.ganymede.common.FieldInfo;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.date_field;
import arlut.csd.ganymede.rmi.db_object;
import arlut.csd.ganymede.rmi.invid_field;
import arlut.csd.ganymede.rmi.string_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      framePanel

------------------------------------------------------------------------------*/

/**
 * An internal client window displaying and/or editing a particular database
 * object from the Ganymede server. A framePanel is a JInternalFrame which contains a
 * tabbed pane which incorporates a
 * {@link arlut.csd.ganymede.client.containerPanel containerPanel} for
 * viewing/editing a server-side database object, as well as several
 * auxiliary panes such as an
 * {@link arlut.csd.ganymede.client.ownerPanel ownerPanel},
 * {@link arlut.csd.ganymede.client.historyPanel historyPanel}, and other
 * panels as appropriate for specific object types.
 *
 * @version $Id$
 * @author Michael Mulvaney 
 */

public class framePanel extends JInternalFrame implements ChangeListener, ActionListener,
							  VetoableChangeListener, InternalFrameListener {
  /**
   * TranslationService object for handling string localization in
   * the Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.framePanel");

  static final String OBJECT_SAVE = "object_save_default_dir";

  // ---

  /**  
   * This will be loaded from gclient anyway.
   */

  boolean debug = false;

  /**
   * Used with vetoableChange() to work around Swing 1.1 bug preventing
   * setDefaultCloseOperation(DO_NOTHING_ON_CLOSE) from doing anything
   * useful.
   *
   * This variable needs to be set to true in order for setClosed() calls
   * in windowPanel to avoid bringing up the dialogs.
   *
   * Actually, this variable has now been overloaded with an
   * additional function.  When we are closing a newly created window,
   * we set closingApproved to true so that the gclient deleteObject()
   * method won't balk at our deleting the newly created objects.
   *
   * This is totally like biological evolution, in which a
   * pre-existing feature is adapted for a new purpose over time.
   * Hot-cha-cha.
   */

  boolean closingApproved = false;

  /**
   * Used with internalFrameClosed() to make our JInternalFrame close
   * interception hack from Swing 1.1 work with Kestrel.
   *
   * If this variable is set to true, internalFrameClosed() will
   * not attempt to call dispose().
   */

  private booleanSemaphore closed = new booleanSemaphore(false);
  private booleanSemaphore running = new booleanSemaphore(false);
  private booleanSemaphore stopped = new booleanSemaphore(false);

  /**
   * We'll show a progressBar while the general panel is loading.  The
   * progressBar is contained in the progressPanel, which will be
   * removed when the general panel is finished loading.
   */
  
  JProgressBar
    progressBar;

  /**
   * Panel to hold the progressBar while we are loading the fields for
   * this object.
   */

  JPanel
    progressPanel;

  /**
   * The tabbed pane holding our various panels.
   */

  JTabbedPane 
    pane;

  /** 
   * A vector of {@link arlut.csd.ganymede.client.containerPanel}s,
   * used to allow the gclient to refresh containerPanels on demand,
   * and to allow the gclient to order any containerPanels contained
   * in this framePanel to stop loading on a transaction cancel.
   *
   * Note that the cleanUp() method in this class can null out
   * this reference, so all methods that loop over containerPanels
   * should be synchronized.  This is also why containerPanels
   * is kept private.
   */

  private Vector containerPanels = new Vector();

  /**
   * Vector of {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}s used
   * by the save() and sendMail() methods to enumerate this object's fields.
   */

  Vector templates;

  /**
   * Vector of {@link arlut.csd.ganymede.client.clientTab} objects
   * representing tabs that need to be created from the server-side
   * tab definitions.
   */

  private Vector tabList = new Vector();

  private personaeTab personae_tab;
  private ownerTab owner_tab;
  private objectsOwnedTab objects_owned_tab;
  private notesTab notes_tab;
  private historyTab history_tab;
  private adminHistoryTab admin_history_tab;
  private expirationRemovalTab expiration_tab;
  private expirationRemovalTab removal_tab;

  /**
   * RMI references to server-side fields that we'll consult to render
   * the various fixed information fields in our object window.
   */

  date_field
    exp_field,
    rem_field;

  boolean
    editable;

  boolean
    expiration_Editable;

  boolean
    removal_Editable;

  /**
   * Remote reference to the server-side object we are viewing or editing.
   */

  db_object
    server_object;

  /**
   * Reference to the desktop pane containing the client's internal windows.  Used to access
   * some GUI resources and to provide to new containerPanels created for embedded objects.
   */

  windowPanel
    wp;

  /**
   * Reference to the client's main class, used for some utility functions.
   */

  gclient
    gc;

  /**
   * Invid of the object edited.  DO NOT access invid directly; use
   * getObjectInvid().  invid will be null until the first time
   * getObjectInvid() is called.
   */

  private Invid invid = null;

  /**
   * If true, this is a newly created object we're editing.  We care about this
   * because we need to handle the user clicking on this window's close box
   * a bit differently.
   */

  boolean isCreating;

  /**
   * If true, we've already done clean up on this framePanel.
   */

  boolean isCleaned;

  /* -- */

  /**
   * @param object RMI reference to a server-side database object
   * @param editable If true, the database object is being edited by this window
   * @param winP The JDesktopPane container for this window
   * @param isCreating if true, this window is for a newly created object, and will
   * be treated specially when closing this window.
   */

  public framePanel(Invid invid, db_object object, boolean editable, windowPanel winP, boolean isCreating)
  {
    this.invid = invid;
    this.wp = winP;
    this.server_object = object;
    this.editable = editable;
    this.gc = winP.gc;
    this.isCreating = isCreating;

    // are we running the client in debug mode?

    if (!debug)
      {
	debug = wp.gc.debug;
      }

    // "Building window."    
    setStatus(ts.l("init.building_window_status"));
    
    // Window properties

    setMaximizable(true);
    setResizable(true);
    setClosable(true);
    setIconifiable(true);

    /*
      we want to be able to take control of closing ourselves.

      Unfortunately, the setDefaultCloseOperation() method is useless
      on JInternalFrames in Swing 1.1.  We'll use a
      VetoableChangeListener instead.

      // setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

      This came around in JDK 1.2/Swing 1.1.1.. bug parade #4176136,
      but the use of a VetoableChangeListener still works just fine,
      so we'll stick with this implementation.
      
      */

    this.addVetoableChangeListener(this);
    this.addInternalFrameListener(this);

    progressPanel = new JPanel(); // flow layout by default
    progressBar = new JProgressBar();
    progressPanel.add(new JLabel(ts.l("init.loading_label"))); // "Loading..."
    progressPanel.add(progressBar);
    
    setContentPane(progressPanel);

    load();
  }

  /**
   * Communicates with the server to download all of the information
   * needed to present the database object associated with this window
   * to the user.  Some of this data (types of fields defined in objects
   * of this type, for instance) will have been already loaded into
   * {@link arlut.csd.ganymede.client.gclient gclient}, but this method
   * is reponsible for loading all data specific to the object being
   * viewed and/or edited.
   *
   * This method also handles the creation of this window's tabbed
   * pane, and adding the various tabs to it.  The actual panels
   * attached to the various tabs will not actually be created and
   * initialized unless and until the user selects the appropriate tab
   * at some point.  The only panel actually created by this method is
   * the general panel, which shows all of the non-built-in fields of
   * the object we're talking to.
   */

  public void load()
  {
    Vector infoVector = null;

    /* -- */

    running.set(true);

    try
      {
	// windowPanel wants to know if framePanel is changed

	addInternalFrameListener(getWindowPanel());
    
	// Now setup the framePanel layout

	pane = new JTabbedPane();

	// the client Loader thread should have already downloaded and
	// cached the field template vector we're getting here.  If not,
	// we'll block here while the Loader gets the information we need.

	short id = getObjectInvid().getType();

	templates = gc.getTemplateVector(id);

	try
	  {
	    infoVector = getObject().getFieldInfoVector();
	  }
	catch (Exception rx)
	  {
	    gc.processExceptionRethrow(rx);
	  }
	
	// loop over the field templates and identify the
	// server-defined tabs

	serverTab newTab = null;
	serverTab oldTab = null;

	for (int i = 0; i < templates.size(); i++)
	  {
	    FieldTemplate template = (FieldTemplate) templates.elementAt(i);

	    // make sure that we don't create a tab if the field isn't
	    // actually present in this instance of the object

	    short fieldID = template.getID();
	    boolean field_present = false;

	    for (int j = 0; !field_present && j < infoVector.size(); j++)
	      {
		FieldInfo field_info = (FieldInfo) infoVector.elementAt(j);

		if (field_info.getID() == fieldID)
		  {
		    field_present = true;
		  }
	      }

	    if (field_present && !template.isBuiltIn() && !(id == SchemaConstants.UserBase && fieldID == SchemaConstants.UserAdminPersonae))
	      {
		if (oldTab == null)
		  {
		    oldTab = new serverTab(this, pane, template.getTabName());
		    oldTab.setInfoVector(infoVector);
		    oldTab.addToPane(tabList);
		  }
		else
		  {
		    if (!StringUtils.stringEquals(template.getTabName(), oldTab.getTabName()))
		      {
			newTab = new serverTab(this, pane, template.getTabName());
			newTab.setInfoVector(infoVector);
			newTab.addToPane(tabList);
			
			oldTab = newTab;
		      }
		  }
	      }
	  }

	// okay, now we we've added the server--side tabs are.  time
	// to start in on the predefined tabs.

	owner_tab = new ownerTab(this, pane, ts.l("load.owner_tab"));	// "Owner"
	owner_tab.addToPane(tabList);

	if (stopped.isSet())
	  {
	    return;
	  }

	// Check to see if this gets an objects_owned panel (for Owner
	// Group objects) or an Admin Personae panel (for User
	// objects)
	//
	// Note that the supergash OwnerBase does not get an
	// objects_owned panel.
    
	try
	  {
	    if (id == SchemaConstants.OwnerBase)
	      {
		if (!getObjectInvid().equals(Invid.createInvid((short)0, 1)))
		  {
		    if (stopped.isSet())
		      {
			return;
		      }

		    objects_owned_tab = new objectsOwnedTab(this, pane, ts.l("load.objects_owned_tab")); // "Objects Owned"
		    objects_owned_tab.addToPane(tabList);
		  }
	      }
	    else if (id == SchemaConstants.UserBase)
	      {
		invid_field persona_field = (invid_field)getObject().getField(SchemaConstants.UserAdminPersonae);
	     
		// If the field is null, then this must be a view-only
		// object with no persona field defined (because
		// editable objects always have all valid fields
		// instantiated)

		if (persona_field != null)
		  {
		    if (stopped.isSet())
		      {
			return;
		      }

		    personae_tab = new personaeTab(this, pane, ts.l("load.personae_tab")); // "Personae"
		    personae_tab.addToPane(tabList);
		  }
	      }
	  }
	catch (Exception rx)
	  {
	    gc.processExceptionRethrow(rx, "Could not check if this is ownerbase: ");
	  }
    
	// Add the notes panel

	if (stopped.isSet())
	  {
	    return;
	  }

	addNotesTab();

	if (stopped.isSet())
	  {
	    return;
	  }

	// Add the history tab

	history_tab = new historyTab(this, pane, ts.l("load.history_tab")); // "History"
	history_tab.addToPane(tabList);

	// If we're an admin persona, add the admin history tab

	if (id == SchemaConstants.PersonaBase)
	  {
	    admin_history_tab = new adminHistoryTab(this, pane, ts.l("load.admin_history_tab")); // "Admin History"
	    admin_history_tab.addToPane(tabList);
	  }

	// Only add the expiration and removal date panels if the date
	// has been set.  In order to set the date, use the menu
	// items.

	if (stopped.isSet())
	  {
	    return;
	  }

	try
	  {
	    exp_field = (date_field)getObject().getField(SchemaConstants.ExpirationField);
	    rem_field = (date_field)getObject().getField(SchemaConstants.RemovalField);
	  
	    expiration_Editable = editable && exp_field.isEditable();
	    removal_Editable = editable && rem_field.isEditable();

	    if ((exp_field != null) && (exp_field.getValue() != null))
	      {
		addExpirationDateTab();
	      }

	    if ((rem_field != null) && (rem_field.getValue() != null))
	      {
		addRemovalDateTab();
	      }
	  }
	catch (Exception rx)
	  {
	    gc.processExceptionRethrow(rx, "Could not get date fields");
	  }
    
	pane.addChangeListener(this);

	if (stopped.isSet())
	  {
	    return;
	  }

	// and let's initialize and show our main tab before we make
	// the tab set visible

	((clientTab) tabList.elementAt(0)).showTab();

	setContentPane(pane);
    
	setJMenuBar(createMenuBar(editable));

	pane.invalidate();
	validate();
      }
    finally
      {
	if (closed.isSet())
	  {
	    cleanUp();
	  }

	running.set(false);
      }
  }

  /**
   * Returns true if this framePanel is editing/creating an object, false if it
   * is viewing only.
   */

  public boolean isEditable()
  {
    return editable;
  }
  
  /**
   * This method returns true if the window is in the middle of closing,
   * which only happens if it has been approved by vetoableChange.
   */
  
  public boolean isApprovedForClosing()
  {
    return closingApproved;
  }

  /**
   * Returns the invid of the object contained in this frame panel.
   *
   * If the invid has not been loaded, this method will load it first.
   *
   * @return The invid of the object in this frame panel.  
   */

  public Invid getObjectInvid()
  {
    if (invid == null)
      {
	try
	  {
	    invid = getObject().getInvid();
	  }
	catch (Exception rx)
	  {
	    // "Could not get object invid"
	    gc.processExceptionRethrow(rx, ts.l("getObjectInvid.error_msg"));
	  }
      }

    return invid;
  }

  /**
   * Returns the type name of the object contained in this frame panel.
   *
   * If the invid has not been loaded, this method will load it first.
   *
   * @return The name of the type of the object in this frame panel.  
   */

  public String getObjectTypeName()
  {
    getObjectInvid();

    return gc.getObjectType(invid);
  }

  /**
   * Used by gclient.commitTransaction to get access to our notes panel.
   * gclient does this so that it can survey any open notes panels to make
   * sure that the contents are updated to the server.  This is ugly as
   * sin, but we don't currently put a change listener on the notes panels,
   * so it needs to be done.
   *
   * This method will often return null if the user hasn't visited the
   * notes panel tab.
   */

  public notesPanel getNotesPanel()
  {
    if (notes_tab == null)
      {
	return null;
      }

    return notes_tab.getNotesPanel();
  }

  /**
   * Refreshes the tab that is showing.
   *
   * Currently, this only refreshes the general panel.  Other panels
   * will generate a nice dialog telling the user to go away.
   */

  public void refresh()
  {
    Component c = pane.getSelectedComponent();
    
    if (c instanceof JScrollPane)
      {
	Component comp = ((JScrollPane)c).getViewport().getView();

	if (comp instanceof containerPanel)
	  {
	    ((containerPanel)comp).updateAll();
	  }
 	else
	  {
	    // this error message isn't localized because i think it
	    // should be eliminated

	    System.err.println("Sorry, you can only refresh the panel containing " +
			       "the general panel at this time.");
	  }
      }
  }

  /**
   * Uses the Ganymede server to e-mail a summary of this object to one
   * or more email addresses.
   */

  public void sendMail()
  {
    SaveObjDialog dialog;
    boolean showHistory = false;
    boolean showTransactions = false;
    Date startDate = null;
    String address;
    String subject;
    StringBuffer body;

    /* -- */

    // "Mail summary for {0} {1}"
    // "Status summary for {0} {1}"
    dialog = new SaveObjDialog(gc, ts.l("sendMail.saveobj_title", getObjectType(), getObjectLabel()),
			       true, ts.l("sendMail.saveobj_subject", getObjectType(), getObjectLabel()));

    if (!dialog.showDialog())
      {
	if (debug)
	  {
	    System.out.println("Dialog returned false, returning in sendMail()");
	  }

	return;
      }

    showHistory = dialog.isShowHistory();
    showTransactions = dialog.isShowTransactions();

    if (showHistory)
      {
	startDate = dialog.getStartDate();
      }

    subject = dialog.getSubject();
    address = dialog.getRecipients();

    if ((address == null) || (address.equals("")))
      {
	// "You must specify at least one recipient."
	gc.showErrorMessage(ts.l("sendMail.no_recipient_msg"));
	return;
      }
    
    body = encodeObjectToStringBuffer(showHistory, showTransactions, startDate);

    if (debug)
      {
	System.out.println("Mailing: \nTo: " + address + "\n\n" + body.toString());
      }

    try
      {
	gc.getSession().sendMail(address, subject, body);
      }
    catch (Exception rx)
      {
	// "Sending Mail"
	gc.processExceptionRethrow(rx, ts.l("sendMail.error_rethrow"));
      }
  }

  /**
   * Saves a summary of this object to disk.  Only available if the Ganymede client
   * was run as an application.
   */

  public void save()
  {
    SaveObjDialog dialog;
    JFileChooser chooser = new JFileChooser();
    int returnValue;
    Date startDate = null;
    boolean showHistory = false;
    boolean showTransactions = false;
    File file;

    FileOutputStream fos = null;
    PrintWriter writer = null;

    /* -- */

    // "Save summary for {0} {1}"
    dialog = new SaveObjDialog(gc, ts.l("save.saveobj_title", getObjectType(), getObjectLabel()),
			       false, null);

    if (!dialog.showDialog())
      {
	if (debug)
	  {
	    System.out.println("dialog returned false, returning");
	  }

	return;
      }

    gc.setWaitCursor();

    showHistory = dialog.isShowHistory();
    showTransactions = dialog.isShowTransactions();

    if (showHistory)
      {
	startDate = dialog.getStartDate();
      }

    chooser.setDialogType(JFileChooser.SAVE_DIALOG);
    chooser.setDialogTitle(ts.l("save.file_dialog_title")); // "Save window as"
    
    String defaultPath = gclient.prefs.get(OBJECT_SAVE, null);

    if (defaultPath != null)
      {
	chooser.setCurrentDirectory(new File(defaultPath));
      }

    returnValue = chooser.showDialog(gc, null);

    if (!(returnValue == JFileChooser.APPROVE_OPTION))
      {
	return;
      }

    file = chooser.getSelectedFile();

    File directory = chooser.getCurrentDirectory();

    try
      {
	gclient.prefs.put(OBJECT_SAVE, directory.getCanonicalPath());
      }
    catch (java.io.IOException ex)
      {
	// we don't really care if we can't save the directory
	// path in our preferences all that much.
      }
    
    if (file.exists())
      {
	// "Warning"
	// "{0} already exists.  Are you sure you want to replace this file?"
	// "Overwrite"
	// "Cancel"
	StringDialog d = new StringDialog(gc,
					  ts.l("save.warning_title"),
					  ts.l("save.conflict_warning", file.getName()),
					  ts.l("save.overwrite_button"),
					  ts.l("global.cancel"),
					  null);
	Hashtable result = d.DialogShow();

	if (result == null)
	  {
	    if (debug)
	      {
		System.out.println("The file exists, and I am backing out.");
	      }

	    return;
	  }
      }

    try
      {
	fos = new FileOutputStream(file);
        writer = new PrintWriter(fos);
      }
    catch (java.io.IOException e)
      {
	// "Save Error"
	// "Could not open the file for writing:\n{0}"
	gc.showErrorMessage(ts.l("save.io_error_title"),
			    ts.l("save.io_error_text", e.toString()));
	return;
      }

    writer.println(encodeObjectToStringBuffer(showHistory, showTransactions, startDate).toString());
    writer.close();

    gc.setNormalCursor();
  }

  /**
   * Generates a String representation of this object for save() and sendMail().
   */

  private StringBuffer encodeObjectToStringBuffer(boolean showHistory, boolean showTransactions, 
						  Date startDate)
  {
    StringBuffer buffer = new StringBuffer();

    try
      {
	buffer = getObject().getSummaryDescription();
      }
    catch (Exception rx)
      {
	gc.processExceptionRethrow(rx);
      }

    if (showHistory)
      {
	if (showTransactions)
	  {
	    // "\nTransactional History:\n\n"
	    buffer.append(ts.l("encodeObjectToStringBuffer.transaction_history_header"));
	  }
	else
	  {
	    // "\nHistory:\n\n"
	    buffer.append(ts.l("encodeObjectToStringBuffer.history_header"));
	  }
	try
	  {
	    buffer.append(gc.getSession().viewObjectHistory(getObjectInvid(), 
							    startDate, showTransactions).toString());
	  }
	catch (Exception rx)
	  {
	    gc.processExceptionRethrow(rx);
	  }
      }

    return buffer;
  }

  public Image getWaitImage()
  {
    return wp.getWaitImage();
  }

  /* Private stuff */

  JMenuBar createMenuBar(boolean editable)
  {
    JMenuBar menuBar = new JMenuBar();
    menuBar.setBorderPainted(true);
    
    // "Object"
    JMenu fileM = new JMenu(ts.l("createMenuBar.object_menu"));

    if (ts.hasPattern("createMenuBar.object_menu_key_optional"))
      {
	fileM.setMnemonic((int) ts.l("createMenuBar.object_menu_key_optional").charAt(0)); // "o"
      }

    menuBar.add(fileM);

    if (!editable)
      {
	JMenuItem refreshMI = new JMenuItem(ts.l("createMenuBar.object_menu_0"));// "Refresh"

	refreshMI.setActionCommand("refresh_obj");

	if (ts.hasPattern("createMenuBar.object_menu_0_key_optional"))
	  {
	    refreshMI.setMnemonic((int) ts.l("createMenuBar.object_menu_0_key_optional").charAt(0)); // "r"
	  }

	if (ts.hasPattern("createMenuBar.object_menu_0_tip_optional"))
	  {
	    // "Update this window with the current state of this object in the database"
	    refreshMI.setToolTipText(ts.l("createMenuBar.object_menu_0_tip_optional"));
	  }

	refreshMI.addActionListener(this);
	fileM.add(refreshMI);
      }

    if (!gc.isApplet())
      {
	JMenuItem saveMI = new JMenuItem(ts.l("createMenuBar.object_menu_1"));  // "Save";

	saveMI.setActionCommand("save_obj");

	if (ts.hasPattern("createMenuBar.object_menu_1_key_optional"))
	  {
	    saveMI.setMnemonic((int) ts.l("createMenuBar.object_menu_1_key_optional").charAt(0)); // "s"
	  }

	if (ts.hasPattern("createMenuBar.object_menu_1_tip_optional"))
	  {
	    // "Saves a text dump of this object''s state and history to disk"
	    saveMI.setToolTipText(ts.l("createMenuBar.object_menu_1_tip_optional"));
	  }

	saveMI.addActionListener(this);
	fileM.add(saveMI);
      }

    JMenuItem mailMI = new JMenuItem(ts.l("createMenuBar.object_menu_2")); // "Mail to..."

    mailMI.setActionCommand("send_mail");

    if (ts.hasPattern("createMenuBar.object_menu_2_key_optional"))
      {
	mailMI.setMnemonic((int) ts.l("createMenuBar.object_menu_2_key_optional").charAt(0)); // "m"
      }

    if (ts.hasPattern("createMenuBar.object_menu_2_tip_optional"))
      {
	// "Mails a text dump of this object''s state and history"
	mailMI.setToolTipText(ts.l("createMenuBar.object_menu_2_tip_optional"));
      }

    mailMI.addActionListener(this);
    fileM.add(mailMI);

    if (editable)
      {
	boolean sepAdded = false;

	try
	  {
	    if (expiration_Editable && getObject().canInactivate())
	      {
		fileM.addSeparator();
		sepAdded = true;

		// "Set Expiration Date"
		JMenuItem setExpirationMI = new JMenuItem(ts.l("createMenuBar.object_menu_3"));

		setExpirationMI.setActionCommand("set_expiration");

		if (ts.hasPattern("createMenuBar.object_menu_3_key_optional"))
		  {
		    setExpirationMI.setMnemonic((int) ts.l("createMenuBar.object_menu_3_key_optional").charAt(0)); // "x"
		  }

		if (ts.hasPattern("createMenuBar.object_menu_3_tip_optional"))
		  {
		    // "Set a date for this object to be inactivated"
		    setExpirationMI.setToolTipText(ts.l("createMenuBar.object_menu_3_tip_optional"));
		  }

		setExpirationMI.addActionListener(this);
		fileM.add(setExpirationMI);
	      }
	  }
	catch (Exception ex)
	  {
	    gc.processExceptionRethrow(ex);
	  }

	if (removal_Editable)
	  {
	    if (!sepAdded)
	      {
		fileM.addSeparator();
	      }

	    // "Set Removal Date"
	    JMenuItem setRemovalMI = new JMenuItem(ts.l("createMenuBar.object_menu_4"));

	    setRemovalMI.setActionCommand("set_removal");

	    if (ts.hasPattern("createMenuBar.object_menu_4_key_optional"))
	      {
		setRemovalMI.setMnemonic((int) ts.l("createMenuBar.object_menu_4_key_optional").charAt(0)); // "v"
	      }
	    
	    if (ts.hasPattern("createMenuBar.object_menu_4_tip_optional"))
	      {
		// "Set a date for this object to be removed from the database"
		setRemovalMI.setToolTipText(ts.l("createMenuBar.object_menu_4_tip_optional"));
	      }

	    setRemovalMI.addActionListener(this);
	    fileM.add(setRemovalMI);
	  }
      }

    if (!editable)
      {
	fileM.addSeparator();

	String typeName = getObjectTypeName();

	// "Edit this {0}"
	JMenuItem editObjMI = new JMenuItem(ts.l("createMenuBar.object_menu_5", typeName));

	if (ts.hasPattern("createMenuBar.object_menu_5_key_optional"))
	  {
	    editObjMI.setMnemonic((int) ts.l("createMenuBar.object_menu_5_key_optional").charAt(0)); // "e"
	  }
	    
	if (ts.hasPattern("createMenuBar.object_menu_5_tip_optional"))
	  {
	    editObjMI.setToolTipText(ts.l("createMenuBar.object_menu_5_tip_optional"));
	  }

	editObjMI.setActionCommand("edit_obj");
	editObjMI.addActionListener(this);
	fileM.add(editObjMI);

	BaseDump bd = (BaseDump) gc.getBaseMap().get(new Short(getObjectInvid().getType()));

	if (bd.canInactivate())
	  {
	    // "Inactivate this {0}"
	    JMenuItem inactObjMI = new JMenuItem(ts.l("createMenuBar.object_menu_6", typeName));

	    if (ts.hasPattern("createMenuBar.object_menu_6_key_optional"))
	      {
		inactObjMI.setMnemonic((int) ts.l("createMenuBar.object_menu_6_key_optional").charAt(0)); // "i"
	      }
	    
	    if (ts.hasPattern("createMenuBar.object_menu_6_tip_optional"))
	      {
		inactObjMI.setToolTipText(ts.l("createMenuBar.object_menu_6_tip_optional"));
	      }

	    inactObjMI.setActionCommand("inact_obj");
	    inactObjMI.addActionListener(this);
	    fileM.add(inactObjMI);
	  }

	// "Delete this {0}"
	JMenuItem delObjMI = new JMenuItem(ts.l("createMenuBar.object_menu_7", typeName));
	
	if (ts.hasPattern("createMenuBar.object_menu_7_key_optional"))
	  {
	    delObjMI.setMnemonic((int) ts.l("createMenuBar.object_menu_7_key_optional").charAt(0)); // "d"
	  }
	
	if (ts.hasPattern("createMenuBar.object_menu_7_tip_optional"))
	  {
	    delObjMI.setToolTipText(ts.l("createMenuBar.object_menu_7_tip_optional"));
	  }

	delObjMI.setActionCommand("del_obj");
	delObjMI.addActionListener(this);
	fileM.add(delObjMI);

	// "Clone this {0}"
	JMenuItem cloneObjMI = new JMenuItem(ts.l("createMenuBar.object_menu_8", typeName));
	
	if (ts.hasPattern("createMenuBar.object_menu_8_key_optional"))
	  {
	    cloneObjMI.setMnemonic((int) ts.l("createMenuBar.object_menu_8_key_optional").charAt(0)); // "c"
	  }
	
	if (ts.hasPattern("createMenuBar.object_menu_8_tip_optional"))
	  {
	    cloneObjMI.setToolTipText(ts.l("createMenuBar.object_menu_8_tip_optional"));
	  }

	cloneObjMI.setActionCommand("clone_obj");
	cloneObjMI.addActionListener(this);
	fileM.add(cloneObjMI);
      }

    if (debug)
      {
	println("Returning menubar.");
      }
    
    return menuBar;
  }

  /**
   * These add the tabs to the framePanel, but they don't create the content
   *
   * The create_ methods create the actual panes, after the pane is selected.
   * If you want to create a panel, call addWhateverPanel, then showWhateverPanel.
   */

  public void addExpirationDateTab()
  {
    if (expiration_tab != null)
      {
	return;
      }

    if (debug)
      {
	println("Adding expiration tab");
      }
    
    expiration_tab = new expirationRemovalTab(this, pane, ts.l("addExpirationDateTab.expiration_tab")); // "Expiration"
    expiration_tab.setImageIcon(new ImageIcon((Image) PackageResources.getImageResource(this, 
											"expire.gif", 
											getClass())));
    expiration_tab.setPanelTitle(ts.l("addExpirationDateTab.panel_title")); // "Expiration date"
    expiration_tab.setDateField(exp_field);
    expiration_tab.addToPane(tabList);
  }

  public void addRemovalDateTab()
  {
    if (removal_tab != null)
      {
	return;
      }

    if (debug)
      {
	println("Adding removal date tabs");
      }

    removal_tab = new expirationRemovalTab(this, pane, ts.l("addRemovalDateTab.removal_tab")); // "Expiration"
    removal_tab.setImageIcon(new ImageIcon((Image) PackageResources.getImageResource(this, 
										     "expire.gif", 
										     getClass())));
    removal_tab.setPanelTitle(ts.l("addRemovalDateTab.panel_title")); // "Expiration date"
    removal_tab.setDateField(rem_field);
    removal_tab.addToPane(tabList);
  }

  public void addNotesTab()
  {
    if (notes_tab != null)
      {
	return;
      }

    if (debug)
      {
	println("Adding notes tab");
      }

    string_field notes_field = null;

    try
      {
	notes_field = (string_field)getObject().getField(SchemaConstants.NotesField);
      }
    catch (Exception rx)
      {
	gc.processExceptionRethrow(rx, "Could not get notes_field: ");
      }

    ImageIcon noteIcon = null;

    try
      {
	if (notes_field != null)
	  {
	    String notesText = (String)notes_field.getValue();

	    if ((notesText != null) && (!notesText.trim().equals("")))
	      {
		if (debug)
		  {
		    println("Setting notes test to *" + notesText + "*.");
		  }

		noteIcon = new ImageIcon((Image) PackageResources.getImageResource(this, 
										   "note02.gif", 
										   getClass()));
	      }
	  }
      }
    catch (Exception rx)
      {
	gc.processExceptionRethrow(rx);
      }

    notes_tab = new notesTab(this, pane, ts.l("addNotesTab.notes_panel_name"));	// "Notes"
    notes_tab.setImageIcon(noteIcon);
    notes_tab.addToPane(tabList);
  }

  /**
   * This method is called by {@link
   * arlut.csd.ganymede.client.containerPanel#update(java.util.Vector)}
   * to cause the expiration date panel to be refreshed whenever we
   * get a {@link arlut.csd.ganymede.common.ReturnVal} back from the
   * server so ordering us.
   */

  public void refresh_expiration_date_panel()
  {
    if (expiration_tab != null)
      {
	expiration_tab.update();
      }
    else
      {
	addExpirationDateTab();
      }
  }

  /**
   * This method is called by {@link
   * arlut.csd.ganymede.client.containerPanel#update(java.util.Vector)}
   * to cause the removal date panel to be refreshed whenever we get a
   * {@link arlut.csd.ganymede.common.ReturnVal} back from the server
   * so ordering us.
   */

  public void refresh_removal_date_panel()
  {
    if (removal_tab != null)
      {
	removal_tab.update();
      }
    else
      {
	addRemovalDateTab();
      }
  }

  public void actionPerformed(ActionEvent e)
  {
    if (debug)
      {
	println("Menu item action: " + e.getActionCommand());
      }
    
    if (e.getActionCommand().equals("save_obj"))
      {
	if (debug)
	  {
	    println("Saving...:");
	  }

	save();
      }
    else if (e.getActionCommand().equals("send_mail"))
      {
	sendMail();
      }
    else if (e.getActionCommand().equals("refresh_obj"))
      {
	refresh();
      }
    else if (e.getActionCommand().equals("set_expiration"))
      {
	addExpirationDateTab();
	expiration_tab.showTab();
      }
    else if (e.getActionCommand().equals("set_removal"))
      {
	addRemovalDateTab();
	removal_tab.showTab();
      }
    else if (e.getActionCommand().equals("edit_obj"))
      {
	gc.editObject(getObjectInvid());
      }
    else if (e.getActionCommand().equals("inact_obj"))
      {
	gc.inactivateObject(getObjectInvid());
      }
    else if (e.getActionCommand().equals("del_obj"))
      {
	gc.deleteObject(getObjectInvid(), true);
      }
    else if (e.getActionCommand().equals("clone_obj"))
      {
	gc.cloneObject(getObjectInvid());
      }
    else
      {
	System.err.println("Unknown action event: " + e);
      }
  }

  // For the ChangeListener

  public void stateChanged(ChangeEvent e)
  {
    // make sure we tell the tab to create itself.

    int index = pane.getSelectedIndex();

    clientTab tab = (clientTab) tabList.elementAt(index);

    tab.showTab();		// causes the tab's contents to be
				// created if it is not already
  }

  // Convienence methods

  final gclient getgclient()
  {
    return getWindowPanel().getgclient();
  }

  final windowPanel getWindowPanel()
  {
    return wp;
  }

  final db_object getObject()
  {
    return this.server_object;
  }

  private final void setStatus(String status)
  {
    wp.gc.setStatus(status);
  }

  String getObjectType()
  {
    String result = null;

    try
      {
	result = getObject().getTypeName();
      }
    catch (Exception ex)
      {
	gc.processExceptionRethrow(ex, "Problem in getObjectType()");
      }

    return result;
  }
  
  String getObjectLabel()
  {
    String result = null;

    try
      {
	result = getObject().getLabel();
      }
    catch (Exception ex)
      {
	gc.processExceptionRethrow(ex, "Problem in getObjectLabel()");
      }

    return result;
  }

  /**
   * Use this instead of System.out.println, in case we want to direct
   * that stuff somewhere else sometime.  Plus, it is easier to type.  
   */
  
  private void println(String s)
  {
    System.out.println(s);
  }

  /**
   * Give the gclient an error message.
   */

  private void showErrorMessage(String message)
  {
    showErrorMessage("Error", message);
  }

  private void showErrorMessage(String title, String message)
  {
    gc.showErrorMessage(title, message);
  }

  public synchronized void addContainerPanel(containerPanel cp)
  {
    if (containerPanels != null)
      {
	containerPanels.addElement(cp);
      }
  }

  public synchronized void removeContainerPanel(containerPanel cp)
  {
    if (containerPanels != null)
      {
	containerPanels.removeElement(cp);
      }
  }

  /**
   * This method is called to force an update on this framePanel.  All
   * fields will be refreshed and the choice lists reloaded.
   */

  public void updateContainerPanels()
  {
    this.updateContainerPanels(null, null);
  }

  /**
   * This method is called to force an update on this framePanel in accordance
   * with the rescan instructions encoded in retVal.  The invid passed is the
   * one we are interested in updating in this method call.  If the invid
   * is null, all contained panels will be forced to update.  Likewise, if
   * the retVal passed is null, all fields in the container panels will
   * be refreshed.
   */

  public synchronized void updateContainerPanels(Invid invid, ReturnVal retVal)
  {
    if (containerPanels == null)
      {
	return;
      }

    // Loop over each containerPanel in the framePanel window.. there
    // may be more than one due to embedded objects and multiple
    // server tabs
    
    // we count down here so that we can handle things if the
    // cp.update*() call causes the count of containerPanels in this
    // frame to decrement (as if an embedded object panel's field is
    // made invisible) we'll be able to handle it.
    
    // if the count of containerPanels increments during this
    // loop, we'll just not see the new panel(s), which is of
    // course just fine.

    for (int i = containerPanels.size() - 1; i >= 0; i--)
      {
	if (i > containerPanels.size() - 1)
	  {
	    i = containerPanels.size() - 1;
	  }

	containerPanel cp = (containerPanel) containerPanels.elementAt(i);

	if (debug)
	  {
	    System.out.println("gclient.handleReturnVal(): Checking containerPanel number " + i);
	    System.out.println("\tcp.invid= " + cp.getObjectInvid() + 
			       " lookng for: " + invid);
	  }
				
	if (invid == null || cp.getObjectInvid().equals(invid))
	  {
	    if (debug)
	      {
		System.out.println("  Found container panel for " + invid +
				   ": " + cp.frame.getTitle());
	      }
			
	    if (retVal == null || retVal.rescanAll(invid))
	      {
		cp.updateAll();
	      }
	    else
	      {
		cp.update(retVal.getRescanList(invid));
	      }
	  }
      }
  }

  /**
   * If this object window contains any editable containerPanels, this
   * method will examine all Invid fields contained within and change
   * the label of the Invid parameter to the newLabel.
   *
   * This method will also relabel the object window to reflect the
   * new title, if we're open for editing.
   */

  public synchronized void relabelObject(Invid invid, String newLabel)
  {
    if (this.invid.equals(invid))
      {
	if (editable)
	  {
	    String newTitle = wp.getWindowTitle(editable, isCreating, gc.getObjectType(invid), newLabel);
	    wp.setWindowTitle(this, newTitle);
	  }

	return;	// don't bother trying to relabel fields in self, though
      }

    if (containerPanels == null)
      {
	return;
      }

    // Loop over each containerPanel in the framePanel window.. there
    // may be more than one due to embedded objects and multiple
    // server tabs
    
    // we count down here so that we can handle things if the
    // cp.update*() call causes the count of containerPanels in this
    // frame to decrement (as if an embedded object panel's field is
    // made invisible) we'll be able to handle it.
    
    // if the count of containerPanels increments during this
    // loop, we'll just not see the new panel(s), which is of
    // course just fine.

    for (int i = containerPanels.size() - 1; i >= 0; i--)
      {
	if (i > containerPanels.size() - 1)
	  {
	    i = containerPanels.size() - 1;
	  }

	containerPanel cp = (containerPanel) containerPanels.elementAt(i);

	cp.updateInvidLabels(invid, newLabel);
      }
  }

  // InternalFrameListener methods

  public void internalFrameActivated(InternalFrameEvent event)
  {
  }

  /**
   * When the internalFrame closes, we need to shut down any auxiliary
   * internalFrames associated with fields in any contained container panels.
   */

  public synchronized void internalFrameClosed(InternalFrameEvent event)
  {
    if (debug)
      {
	System.err.println("framePanel.internalFrameClosed(): frame closed");
      }
    
    if (this.closed.set(true))
      {
	if (debug)
	  {
	    System.err.println("framePanel.internalFrameClosed(): frame already closed, doing nothing.");
	  }
	
	return;
      }

    if (debug)
      {
	System.err.println("framePanel.internalFrameClosed(): going invisible");
      }
    
    if (debug)
      {
	System.err.println("framePanel.internalFrameClosed(): disposing");
      }
    
    this.cleanUp();
    
    this.removeAll();
    
    if (debug)
      {
	System.err.println("framePanel.internalFrameClosed(): disposed");
      }
    
    // finally, shut down any secondary windows and null out all
    // references to make sure that we don't cascade any leaks.. if
    // the load method is still going, we'll leave this alone and let
    // run() take care of this as it leaves

    if (!running.isSet())
      {
	cleanUp();
      }
  }

  public void internalFrameClosing(InternalFrameEvent event)
  {
    if (debug)
      {
	System.err.println("framePanel.internalFrameClosing(): Called");
      }

    if (!closingApproved)
      {
	return;
      }

    if (debug)
      {
	System.err.println("framePanel.internalFrameClosing(): Ok, closing the created window");
      }
  }

  public void internalFrameDeactivated(InternalFrameEvent event)
  {
  }

  public void internalFrameDeiconified(InternalFrameEvent event)
  {
  }

  public void internalFrameIconified(InternalFrameEvent event)
  {
  }

  public void internalFrameOpened(InternalFrameEvent event)
  {
  }

  /**
   * This is a vetoableChangeListener implementation method, and is used
   * to allow the framePanel to intercede in window close attempts for
   * editable objects.  We use this intercession to explain to the user
   * what the meaning of closing an edit-object or create-object window
   * is, and to allow them to think again.
   */

  public void vetoableChange(PropertyChangeEvent pce) throws PropertyVetoException
  {
    if (debug)
      {
	System.err.println("framePanel.vetoableChange()");
      }

    if (pce.getPropertyName().equals(IS_CLOSED_PROPERTY) &&
	pce.getOldValue().equals(Boolean.FALSE) &&
	pce.getNewValue().equals(Boolean.TRUE))
      {
	if (!closingApproved && editable)
	  {
	    StringDialog okToKill;
	    
	    if (isCreating)
	      {
		// "Ok to discard {0}?"

		// "If you close this newly created window before
		// committing this transaction, this newly created
		// object will be forgotten and abandoned on commit."

		okToKill = new StringDialog(gclient.client, 
					    ts.l("vetoableChange.discard_title", getTitle()),
					    ts.l("vetoableChange.discard_text"),
					    ts.l("vetoableChange.discard_button"),
					    ts.l("global.cancel"),
					    gclient.client.getQuestionImage());

		Hashtable result = okToKill.DialogShow();
	    
		if (result == null)
		  {
		    throw new PropertyVetoException("Cancelled", null);
		  }

		if (debug)
		  {
		    System.err.println("framePanel.vetoableChange(): setting closingApproved true");
		  }

		// set closingApproved to true before we call
		// deleteObject() so that the gclient class will
		// consider this deletion approved.
		
		closingApproved = true;

		gclient.client.deleteObject(getObjectInvid(), false);
	      }
	    else
	      {
		// "Ok to hide {0}?"

		// "Closing this window will not undo changes made to
		// it, nor will it make this object available to other
		// Ganymede users to edit.  If you want to undo
		// changes to this object, you either will have to
		// manually undo them, or you will have to cancel this
		// transaction.\n\nIf this window is closed, you will
		// be able to re-open it from the tree later if
		// needed."

		okToKill = new StringDialog(gclient.client,
					    ts.l("vetoableChange.hide_title", getTitle()),
					    ts.l("vetoableChange.hide_text"),
					    ts.l("vetoableChange.hide_button"),
					    ts.l("global.cancel"),
					    gclient.client.getQuestionImage());

		Hashtable result = okToKill.DialogShow();
	    
		if (result == null)
		  {
		    throw new PropertyVetoException("Cancelled", null);
		  }
	      }
	  }
      }
  }

  /**
   * This method is intended to stop any container panels from loading, in the
   * event that the user has pressed the transaction cancel button in the gclient.
   */

  public final void stopNow()
  {
    // try to interrupt the loading thread, and also turn off any
    // loading methods that might run in any container panels in this
    // window

    stopped.set(true);

    // block until we are sure this window has stopped loading

    running.waitForCleared();
  }

  /**
   * This method may be called by objects of other classes who want to check to
   * see if this framePanel has asserted a stop on all loading activities.
   */

  public final boolean isStopped()
  {
    return stopped.isSet();
  }

  /**
   * This method provides a handy way to null out data structures held in
   * relationship to this framePanel, particularly network reference
   * resources.
   *
   * This method should be called on the Java GUI thread.
   */

  public final synchronized void cleanUp()
  {
    if (debug)
      {
	System.err.println("framePanel.cleanUp()");
      }

    if (isCleaned)
      {
	return;
      }

    // let everyone know that we'll do no more loading in this window

    stopNow();

    this.setVisible(false);

    this.removeVetoableChangeListener(this);
    this.removeInternalFrameListener(this);

    if (getWindowPanel() != null)
      {
	this.removeInternalFrameListener(getWindowPanel());
      }

    if (pane != null)
      {
	pane.removeChangeListener(this);
	pane = null;
      }

    if (tabList != null)
      {
	synchronized (tabList)
	  {
	    for (int i = 0; i < tabList.size(); i++)
	      {
		clientTab tab = (clientTab) tabList.elementAt(i);
		tab.dispose();
	      }
	  }

	tabList = null;
      }
    
    progressBar = null;
    progressPanel = null;
    personae_tab = null;
    owner_tab = null;
    objects_owned_tab = null;
    notes_tab = null;
    history_tab = null;
    admin_history_tab = null;
    expiration_tab = null;
    removal_tab = null;

    if (containerPanels != null)
      {
	for (int i = 0; i < containerPanels.size(); i++)
	  {
	    containerPanel cp = (containerPanel)containerPanels.elementAt(i);
	    cp.cleanUp();
	  }

	containerPanels = null;
      }

    templates = null;
    exp_field = null;
    rem_field = null;
    server_object = null;
    wp = null;
    gc = null;
    invid = null;

    isCleaned = true;
  }
}

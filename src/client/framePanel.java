 /*

   framePanel.java

   The individual frames in the windowPanel.
   
   Created: 4 September 1997
   Version: $Revision: 1.56 $
   Last Mod Date: $Date: 1999/05/26 23:17:14 $
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

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;
import java.beans.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import arlut.csd.Util.PackageResources;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      framePanel

------------------------------------------------------------------------------*/

/**
 * <p>An internal client window displaying and/or editing a particular database
 * object from the Ganymede server. A framePanel is a JInternalFrame which contains a
 * tabbed pane which incorporates a
 * {@link arlut.csd.ganymede.client.containerPanel containerPanel} for
 * viewing/editing a server-side database object, as well as several
 * auxiliary panes such as an
 * {@link arlut.csd.ganymede.client.ownerPanel ownerPanel},
 * {@link arlut.csd.ganymede.client.historyPanel historyPanel}, and other
 * panels as appropriate for specific object types.</p>
 *
 * <p>framePanel is itself a Runnable object.  When created, the framePanel
 * constructor will spawn a new thread to execute its run() method.  This run()
 * method communicates with the server in the background, downloading field information
 * needed to present the object to the user for viewing and/or editing.</p>
 *
 * @version $Revision: 1.56 $ $Date: 1999/05/26 23:17:14 $ $Name:  $
 * @author Michael Mulvaney 
 */

public class framePanel extends JInternalFrame implements ChangeListener, Runnable, ActionListener, VetoableChangeListener, InternalFrameListener {

  /**  
   * This will be loaded from gclient anyway.
   */

  boolean debug = false;

  /**
   * <p>used with vetoableChange() to work around Swing 1.1 bug preventing
   * setDefaultCloseOperation(DO_NOTHING_ON_CLOSE) from doing anything
   * useful.</p>
   *
   * <p>This variable needs to be set to true in order for setClosed() calls
   * in windowPanel to avoid bringing up the dialogs.</p>
   */

  boolean closingApproved = false;

  // Indexes for the tabs in the JTabbedPane These numbers have to
  // correspond to the order they are added as tabs, so they are set
  // to current++ when one is added.  -1 means not added yet.(to the
  // pane) An index of > -1 does NOT mean the pane has been created.

  public int 
    current = 0,
    general_index = -1,
    removal_date_index = -1,
    expiration_date_index = -1,
    history_index = -1,
    owner_index = -1,
    admin_history_index = -1,
    notes_index = -1,
    objects_owned_index = -1,
    personae_index = -1;

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

  // Each of these panes is one of the tabs in the tabbedPane.  Some
  // objects don't use every scrollpane.

  /**
   * Holds a containerPanel in the ViewportView
   */

  JScrollPane general;

  /**
   * Holds a datePanel
   */

  JScrollPane expiration_date;

  /**
   * Holds a datePanel
   */

  JScrollPane removal_date;

  /**
   * Holds an ownerPanel
   */

  JScrollPane owner;

  /**
   * holds a notePanel
   */

  JScrollPane notes;

  /**
   * holds an adminHistoryPanel (only for adminPersonae)
   */

  JScrollPane admin_history;

  /**
   * Holds an ownershipPanel (only for owner groups)
   */
 
  JScrollPane objects_owned;

  datePanel
    exp_date_panel,
    rem_date_panel;

  JPanel
    history,      // holds an historyPanel
    personae;

  historyPanel
    history_panel;

  /** 
   * A vector of {@link arlut.csd.ganymede.client.containerPanel}s,
   * used to allow the gclient to refresh containerPanels on demand,
   * and to allow the gclient to order any containerPanels contained
   * in this framePanel to stop loading on a transaction cancel.  
   */

  Vector containerPanels = new Vector();

  /**
   * Vector of {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}s used
   * by the save() and sendMail() methods to enumerate this object's fields.
   */

  Vector templates;

  /**
   * Vector of Integers used to track the tab panels that have been created.
   */

  Vector createdList = new Vector(); 

  date_field
    exp_field,
    rem_field,
    creation_date_field,
    modification_date_field;

  string_field
    notes_field,
    creator_field,
    modifier_field;

  invid_field 
    persona_field,
    objects_owned_field;

  boolean 
    editable;

  /**
   * Remote reference to the server-side object we are viewing or editing.
   */

  db_object
    object;

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

  notesPanel
    my_notesPanel = null;

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

  /* -- */

  /**
   * @param object RMI reference to a server-side database object
   * @param editable If true, the database object is being edited by this window
   * @param winP The JDesktopPane container for this window
   * @param title Title for this window
   * @param isCreating if true, this window is for a newly created object, and will
   * be treated specially when closing this window.
   */

  public framePanel(db_object object, boolean editable, windowPanel winP, String title, boolean isCreating)
  {
    this.wp = winP;
    this.object = object;
    this.editable = editable;
    this.gc = winP.gc;
    this.isCreating = isCreating;

    // are we running the client in debug mode?

    if (!debug)
      {
	debug = wp.gc.debug;
      }
    
    setStatus("Building window.");
    
    // Window properties

    setMaximizable(true);
    setResizable(true);

    setClosable(true);

    setIconifiable(true);
    setTitle(title);

    /*
      we want to be able to take control of closing ourselves.

      Unfortunately, the setDefaultCloseOperation() method is useless
      on JInternalFrames in Swing 1.1.  We'll use a VetoableChangeListener
      instead.

      // setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

      */

    // gah!  we need to work around a bug in JDK 1.2/Swing 1.1.1.. bug parade #4176136

    this.addVetoableChangeListener(this);
    this.addInternalFrameListener(this);

    //setFrameIcon(new ImageIcon((Image)PackageResources.getImageResource(this, "folder-red.gif", getClass())));
    
    progressPanel = new JPanel();
    progressBar = new JProgressBar();
    progressPanel.add(new JLabel("Loading..."));
    progressPanel.add(progressBar);
    
    setContentPane(progressPanel);
    
    Thread thread = new Thread(this);
    thread.start();
  }

  /**
   * <p>Communicates with the server to download all of the information
   * needed to present the database object associated with this window
   * to the user.  Some of this data (types of fields defined in objects
   * of this type, for instance) will have been already loaded into
   * {@link arlut.csd.ganymede.client.gclient gclient}, but this method
   * is reponsible for loading all data specific to the object being
   * viewed and/or edited.</p>
   *
   * <p>This method also handles the creation of this window's tabbed
   * pane, and adding the various tabs to it.  The actual panels
   * attached to the various tabs will not actually be created and
   * initialized unless and until the user selects the appropriate tab
   * at some point.  The only panel actually created by this method is
   * the general panel, which shows all of the non-built-in fields of
   * the object we're talking to.</p>
   */

  public void run()
  {
    if (debug)
      {
	println("Starting thread in framePanel");
      }

    // windowPanel wants to know if framePanel is changed
    // Maybe this should be replaced with InternalFrameListener?

    addInternalFrameListener(getWindowPanel());
    
    // Now setup the framePanel layout

    pane = new JTabbedPane();
    
    // Add the panels to the tabbedPane (add just the panels that
    // every object has.)

    general = new JScrollPane();
    pane.addTab("General", null, general);
    general_index = current++;
    owner = new JScrollPane();
    pane.addTab("Owner", null, owner);
    owner_index = current++;
    
    // Check to see if this gets an objects_owned panel
    //
    // Only OwnerBase objects get an objects_owned panel.  The
    // supergash OwnerBase does not get an objects_owned panel.
    //
    // Only user objects get a persona panel.
    
    short id = -1;

    try
      {
	id = object.getTypeID();

	if (id == SchemaConstants.OwnerBase)
	  {
	    if (getObjectInvid().equals(new Invid((short)0, 1)))
	      {
		if (debug)
		  {
		    System.out.println("framePanel:  Supergash doesn't get an ownership panel.");
		  }
	      }
	    else
	      {
		objects_owned = new JScrollPane();
		pane.addTab("Objects Owned", null, objects_owned);
		objects_owned_index = current++;
	      }
	  }
	else if (id == SchemaConstants.UserBase)
	  {
	    persona_field = (invid_field)object.getField(SchemaConstants.UserAdminPersonae);
	     
	    // If the field is null, then that means that the aren't
	    // any personas, and this is jsut a view window, so we
	    // don't need the panel at all

	    if (persona_field != null)
	      {
		personae = new JPanel(false);
		pane.addTab("Personae", null, personae);
		personae_index = current++;
	      }
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check if this is ownerbase: " + rx);
      }
    
    // the client Loader thread should have already downloaded and
    // cached the field template vector we're getting here.  If not,
    // we'll block here while the Loader gets the information we need.

    templates = gc.getTemplateVector(id);
    
    // Add the notes panel

    try
      {
	notes_field = (string_field)object.getField(SchemaConstants.NotesField);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get notes_field: " + rx);
      }
    
    notes = new JScrollPane();
    addNotesPanel();

    // Add the history tab

    history = new JPanel(new BorderLayout());
    pane.addTab("History", null, history);
    history_index = current++;

    if (id == SchemaConstants.PersonaBase)
      {
	admin_history = new JScrollPane();
	pane.addTab("Admin History", null, admin_history);
	admin_history_index = current++;
      }

    // Only add the date panels if the date has been set.  In order
    // to set the date, use the menu items.
    
    try
      {
	exp_field = (date_field)object.getField(SchemaConstants.ExpirationField);
	rem_field = (date_field)object.getField(SchemaConstants.RemovalField);
	  
	if ((exp_field != null) && (exp_field.getValue() != null))
	  {
	    addExpirationDatePanel();
	  }

	if ((rem_field != null) && (rem_field.getValue() != null))
	  {
	    addRemovalDatePanel();
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get date fields: " + rx);
      }
    
    pane.addChangeListener(this);

    createPanel(general_index);
    showTab(general_index);

    // Under Swing 1.1, making a JTabbedPane a contentPane for an internal
    // frame causes the JTabbedPane to have some rendering bugs, so we
    // wrap our JTabbedPane with a generic JPanel to avoid this.

    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new BorderLayout());
    contentPanel.add("Center", pane);

    setContentPane(contentPanel);
    
    // Need to add the menubar at the end, so the user doesn't get
    // into the menu items before the tabbed pane is all set up
    
    JMenuBar mb = createMenuBar(editable);
    
    try
      {
	setJMenuBar(mb);
      }
    catch (Error ex)
      {
	// Swing 1.0.2 doesn't have this method, it is only in 1.0.3 and later
	
	System.err.println("Not running recent version of swing.. no setJMenuBar method.");
      }
    
    pane.invalidate();
    validate();
  }

  /**
   * <p>Stop loading all the container panels in this framePanel.</p>
   *
   * <p>This will not make the window go away, it simply tells all the
   * containerPanels inside to stop loading.</p>
   *
   * <p>windowPanel(the porpertyChangeListener/InternalFrameListener)
   * calls this when a frame is closed.</p>
   */

  public void stopLoading()
  {
    if (debug)
      {
	println("Stopping all the containerPAnels.");
      }

    Runnable r = new Runnable() {
      public void run() {
	synchronized (containerPanels)
	  {
	    for (int i = 0; i < containerPanels.size(); i++)
	      {
		if (debug)
		  {
		    println("Telling a containerPanel to stop loading.");
		  }
		
		containerPanel cp = (containerPanel)containerPanels.elementAt(i);
		cp.stopLoading();
	      }
	  }
      }};

    Thread t = new Thread(r);
    t.start();
  }

  /**
   */

  public boolean isEditable()
  {
    return editable;
  }

  /**
   * <p>Returns the invid of the object contained in this frame panel.</p>
   *
   * <p>If the invid has not been loaded, this method will load it first.</p>
   *
   * @return The invid of the object in this frame panel.  
   */

  public Invid getObjectInvid()
  {
    if (invid == null)
      {
	try
	  {
	    invid = object.getInvid();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get object's invid");
	  }
      }
    return invid;
  }

  /**
   * <p>Used by gclient.commitTransaction to get access to our notes panel.
   * gclient does this so that it can survey any open notes panels to make
   * sure that the contents are updated to the server.  This is ugly as
   * sin, but we don't currently put a change listener on the notes panels,
   * so it needs to be done.</p>
   *
   * <p>This method will often return null if the user hasn't visited the
   * notes panel tab.</p>
   */

  public notesPanel getNotesPanel()
  {
    return my_notesPanel;
  }

  /**
   * <p>Refreshes the tab that is showing.</p>
   *
   * <p>Currently, this only refreshes the general panel.  Other panels
   * will generate a nice dialog telling the user to go away.</p>
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
	    getgclient().showErrorMessage("Not implemented yet",
					  "Sorry, you can only refresh the panel containing " +
					  "the general panel at this time.");
	  }
      }
  }
    
  /**
   * <p>Print a nasty-looking image of the frame.</p>
   *
   * <p>This hardly works.</p>
   */

  public void printObject()
  {
    PrintJob j = Toolkit.getDefaultToolkit().getPrintJob(getgclient(), 
							 "Print window", 
							 new Properties());
    if (j == null)
      {
	if (debug)
	  {
	    println("Cancelled");
	  }

	return;
      }

    Graphics page = j.getGraphics();

    int index = pane.getSelectedIndex();

    if (index < 0)
      {
	if (debug)
	  {
	    println("No pane selected?");
	  }
      }
    else
      {
	if (debug)
	  {
	    println("Printing " + index);
	  }

	// The thinking here is that some components other than the
	// scrollpane might be a little bit smarter about what they
	// should be printing.

	if (pane.getComponentAt(index) instanceof JScrollPane)
	  {
	    if (debug)
	      {
		println("Printing out the contents of the ScrollPane.");
	      }

	    JScrollPane sp = (JScrollPane)pane.getComponentAt(index);
	    sp.getViewport().getView().print(page);
	  }
	else if (debug)
	  {
	    println("The selected index is not a scrollpane.");
	    showErrorMessage("The current selection cannot be printed.");
	  }
      }

    page.dispose();
    j.end();
  }

  /**
   * Uses the Ganymede server to e-mail a summary of this object to one
   * or more email addresses.
   */

  public void sendMail()
  {
    SaveDialog dialog = new SaveDialog(gc, true);
    boolean showHistory = false;
    Date startDate = null;
    String address;
    StringBuffer body;
    //Vector choices = new Vector();

    /* -- */

    //choices.addElement("Html");
    //choices.addElement("Plain text");
    //choices.addElement("Tab separated");
    //dialog.setFormatChoices(choices);

    if (!dialog.showDialog())
      {
	if (debug)
	  {
	    System.out.println("Dialog returned false, returning in sendMail()");
	  }

	return;
      }

    System.out.println("Format: " + dialog.getFormat());

    showHistory = dialog.isShowHistory();
    if (showHistory)
      {
	startDate = dialog.getStartDate();
      }

    address = dialog.getRecipients();
    if ((address == null) || (address.equals("")))
      {
	gc.showErrorMessage("You must specify at least one recipient.");
	return;
      }
    
    body = encodeObjectToStringBuffer(showHistory, startDate);

    if (debug)
      {
	System.out.println("Mailing: \nTo: " + address + "\n\n" + body.toString());
      }

    try
      {
	gc.getSession().sendMail(address, "Ganymede results", body);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("sending mail: " + rx);
      }
  }

  /**
   * Saves a summary of this object to disk.  Only available if the Ganymede client
   * was run as an application.
   */

  public void save()
  {
    SaveDialog dialog = new SaveDialog(gc, false);
    JFileChooser chooser = new JFileChooser();
    int returnValue;
    Date startDate = null;
    boolean showHistory = false;
    File file;

    FileOutputStream fos = null;
    PrintWriter writer = null;

    /* -- */

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
    if (showHistory)
      {
	startDate = dialog.getStartDate();
      }

    chooser.setDialogType(JFileChooser.SAVE_DIALOG);
    chooser.setDialogTitle("Save window as");

    returnValue = chooser.showDialog(gc, null);

    if (!(returnValue == JFileChooser.APPROVE_OPTION))
      {
	return;
      }

    file = chooser.getSelectedFile();
    
    if (file.exists())
      {
	StringDialog d = new StringDialog(gc, "Warning", 
					  file.getName() + 
					  " exists.  Are you sure you want to replace this file?",
					  "Overwrite", "Cancel", null);
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
	gc.showErrorMessage("Trouble saving", "Could not open the file.");
	return;
      }

    writer.println(encodeObjectToStringBuffer(showHistory, startDate).toString());
    writer.close();

    gc.setNormalCursor();
  }

  /**
   * Generates a String representation of this object for save() and sendMail().
   */

  private StringBuffer encodeObjectToStringBuffer(boolean showHistory, Date startDate)
  {
    StringBuffer buffer = new StringBuffer();
    FieldTemplate template;
    db_field field;

    // Loop through all the fields, and get their values

    try
      {
	for (int i = 0; i < templates.size();  ++i)
	  {
	    template = (FieldTemplate)templates.elementAt(i);
	    field = object.getField(template.getID());

	    if (field != null)
	      {
		buffer.append(template.getName() + "\t" + 
			      field.getValueString() + 
			      System.getProperty("line.separator")); 
	      }
	    else
	      {
		buffer.append(template.getName() + "\tUndefined" + 
			      System.getProperty("line.separator"));

		if (debug)
		  {
		    System.out.println("Field is null: " + template.getName());
		  }
	      }
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get field info for encoding: " + rx);
      }

    if (showHistory)
      {
	try
	  {
	    buffer.append("\nHistory:\n\n" + 
			  gc.getSession().viewObjectHistory(getObjectInvid(), startDate).toString());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("RemoteException getting history: " + rx);
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
    
    JMenu fileM = new JMenu("File");

    menuBar.add(fileM);

    JMenu deleteM = new JMenu("Delete");
    JMenuItem reallyDeleteMI = new JMenuItem("Yes, I'm sure");
    deleteM.add(reallyDeleteMI);
    reallyDeleteMI.setActionCommand("ReallyDelete");
    reallyDeleteMI.addActionListener(this);
      
    JMenuItem inactivateMI = new JMenuItem("Inactivate");
    inactivateMI.addActionListener(this);

    JMenuItem refreshMI = new JMenuItem("Refresh");
    refreshMI.addActionListener(this);

    JMenuItem setExpirationMI = null;
    JMenuItem setRemovalMI = null;

    if (editable)
      {
	setExpirationMI = new JMenuItem("Set Expiration Date");
	setExpirationMI.addActionListener(this);
	
        setRemovalMI = new JMenuItem("Set Removal Date");
	setRemovalMI.addActionListener(this);
      }

    JMenuItem printMI = new JMenuItem("Print");
    printMI.addActionListener(this);

    JMenuItem saveMI = null;
    if (!gc.isApplet())
      {
	saveMI = new JMenuItem("Save");
	saveMI.addActionListener(this);
      }

    JMenuItem mailMI = new JMenuItem("Mail to...");
    mailMI.addActionListener(this);
    

    fileM.add(inactivateMI);
    fileM.add(deleteM);
    fileM.addSeparator();
    fileM.add(printMI);

    if (!gc.isApplet())
      {
	fileM.add(saveMI);
      }
    fileM.add(mailMI);

    fileM.add(refreshMI);
    if (editable)
      {
	fileM.addSeparator();
	fileM.add(setExpirationMI);
	fileM.add(setRemovalMI);
      }

    fileM.addSeparator();

    if (!editable)
      {
	JMenuItem editMI = new JMenuItem("Edit");
	editMI.addActionListener(this);
	
	fileM.add(editMI);
	
      }

    if (debug)
      {
	println("Returning menubar.");
      }
    
    return menuBar;
  }

  void create_general_panel()
  {
    if (debug)
      {
	println("Creating general panel");
      }
    
    containerPanel cp = new containerPanel(object, editable, wp.gc, wp, this, progressBar, false, isCreating, null);
    cp.load();
    cp.setBorder(wp.emptyBorder10);
    
    general.getVerticalScrollBar().setUnitIncrement(15);
    general.setViewportView(cp);
    createdList.addElement(new Integer(general_index));
    setStatus("Done");
  }

  void create_expiration_date_panel()
  {
    if (debug)
      {
	println("Creating expiration date panel");
      }
    
    if (exp_field == null)
      {
	try
	  {
	    exp_field = (date_field) object.getField(SchemaConstants.ExpirationField);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get removal field: " + rx);
	  }
      }

    expiration_date.getVerticalScrollBar().setUnitIncrement(15);

    exp_date_panel = new datePanel(exp_field, "Expiration date", editable, this);
    expiration_date.setViewportView(exp_date_panel);
    
    createdList.addElement(new Integer(expiration_date_index));
  }

  void refresh_expiration_date_panel()
  {
    if (debug)
      {
	println("Refreshing expiration date panel");
      }
    
    if (exp_date_panel == null)
      {
	addExpirationDatePanel();
	create_expiration_date_panel();
	return;
      }
    else
      {
	exp_date_panel.refresh();
      }
  }

  void create_removal_date_panel()
  {
    if (debug)
      {
	println("Creating removal date panel");
      }
    
    if (rem_field == null)
      {
	try
	  {
	    rem_field = (date_field)object.getField(SchemaConstants.RemovalField);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get removal field: " + rx);
	  }
      }

    removal_date.getVerticalScrollBar().setUnitIncrement(15);

    rem_date_panel = new datePanel(rem_field, "Removal date", editable, this);
    removal_date.setViewportView(rem_date_panel);
	  
    createdList.addElement(new Integer(removal_date_index));

    removal_date.invalidate();
    validate();
  }

  void refresh_removal_date_panel()
  {
    if (debug)
      {
	println("Refreshing expiration date panel");
      }
    
    if (rem_date_panel == null)
      {
	addExpirationDatePanel();
	create_removal_date_panel();
	return;
      }
    else
      {
	rem_date_panel.refresh();
      }
  }

  void create_owner_panel()
  {
    if (debug)
      {
	println("Creating owner panel");
      }

    try
      {
	owner.getVerticalScrollBar().setUnitIncrement(15);

	invid_field invf = (invid_field) object.getField(SchemaConstants.OwnerListField);
	owner.setViewportView(new ownerPanel(invf, editable, this));
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not generate Owner field: " + rx);
      }
    
    createdList.addElement(new Integer(owner_index));

    owner.invalidate();
    validate();
  }

  void create_history_panel()
  {
    setStatus("Creating history panel");

    try
      {
	creation_date_field = (date_field)object.getField(SchemaConstants.CreationDateField);
	creator_field = (string_field)object.getField(SchemaConstants.CreatorField);
	modification_date_field = (date_field)object.getField(SchemaConstants.ModificationDateField);
	modifier_field = (string_field)object.getField(SchemaConstants.ModifierField);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get field information: " + rx);
      }

    history_panel = new historyPanel(getObjectInvid(),
				     getgclient(),
				     creator_field,
				     creation_date_field,
				     modifier_field,
				     modification_date_field);

    history.add("Center", history_panel);

    // remember that we've already created the history panel

    createdList.addElement(new Integer(history_index));
      
    history.invalidate();
    validate();
  }

  void create_admin_history_panel()
  {
    setStatus("Creating admin history panel");
    admin_history.getVerticalScrollBar().setUnitIncrement(15);
    admin_history.setViewportView(new adminHistoryPanel(getObjectInvid(), getgclient()));
    
    createdList.addElement(new Integer(admin_history_index));
    
    admin_history.invalidate();
    validate();
  }

  void create_notes_panel()
  {
    if (debug)
      {
	println("Creating notes panel");
      }

    my_notesPanel = new notesPanel(notes_field, 
				   editable, this);

    notes.getVerticalScrollBar().setUnitIncrement(15);
    notes.setViewportView(my_notesPanel);

    createdList.addElement(new Integer(notes_index));
    
    notes.invalidate();
    validate();
  }
  
  void create_objects_owned_panel()
  {
    if (debug)
      {
	println("Creating ownership panel");
      }

    invid_field oo = null;

    try
      {
	oo = (invid_field)object.getField(SchemaConstants.OwnerObjectsOwned);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get owner objects owned: " + rx);
      }

    if (oo == null)
      {
	JPanel null_oo = new JPanel();
	null_oo.add(new JLabel("There are no objects owned here."));
	objects_owned.getVerticalScrollBar().setUnitIncrement(15);
	objects_owned.setViewportView(null_oo);
      }
    else
      {
	objects_owned.getVerticalScrollBar().setUnitIncrement(15);
	objects_owned.setViewportView(new ownershipPanel(oo, editable, this));
	createdList.addElement(new Integer(objects_owned_index));
      }

    objects_owned.invalidate();
    validate();
  }

  void create_personae_panel()
  {
    if (debug)
      {
	println("Creating personae panel()");
      }
    
    invid_field p = null;
      
    personae.setLayout(new BorderLayout());
    personae.add("Center", new personaPanel(persona_field, editable, this));
    createdList.addElement(new Integer(personae_index));
    
    personae.invalidate();
    validate();
  }

  /**
   * These add the tabs to the framePanel, but they don't create the content
   *
   * The create_ methods create the actual panes, after the pane is selected.
   * If you want to create a panel, call addWhateverPanel, then showWhateverPanel.
   */

  public void addExpirationDatePanel()
  {
    if (expiration_date_index == -1)
      {
	if (debug)
	  {
	    println("Adding date tabs");
	  }

	expiration_date = new JScrollPane();
	pane.addTab("Expiration", null, expiration_date);
	expiration_date_index = current++;
      }
  }

  public void addRemovalDatePanel()
  {
    if (removal_date_index == -1)
      {
	if (debug)
	  {
	    println("Adding removal date tabs");
	  }
	  
	removal_date = new JScrollPane();
	pane.addTab("Removal", null, removal_date);
	removal_date_index = current++;
      }
  }

  public void addNotesPanel()
  {
    if (debug)
      {
	println("Adding notes tab");
      }

    pane.addTab("Notes", null, notes);
    notes_index = current++;
      
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

		ImageIcon noteIcon = new ImageIcon((Image) PackageResources.getImageResource(this, 
											     "note02.gif", 
											     getClass()));
		pane.setIconAt(notes_index, noteIcon);
	      }
	    else if (debug)
	      {
		println("Empty notes");
	      }
	  }
	else if (debug)
	  {
	    System.err.println("notes_field is null in framePanel");
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get notes Text: " + rx);
      }
  }

  //
  // showPanel(int) will generate the actual panel.  I don't know if we need
  // showPanel after each of these, or just for the first one.  It is kinda
  // a hack anyway, since the whole framePanel changed so many times.  Maybe
  // the whole thing should be overhauled.
  //

  // I don't know if need these at all.  There could be one showTab(int).  Why use these
  // anyway?

  public void showTab(int index)
  {
    if (index == -1)
      {
	println("This tab has not been added to the pane yet.");
      }

    pane.setSelectedIndex(index);
  }

  public void actionPerformed(ActionEvent e)
  {
    if (debug)
      {
	println("Menu item action: " + e.getActionCommand());
      }
    
    if (e.getActionCommand().equals("Edit"))
      {
	if (debug)
	  {
	    println("edit button clicked");
	  }

	gc.editObject(getObjectInvid());
      }
    else if (e.getActionCommand().equals("Save"))
      {
	if (debug)
	  {
	    println("Saving...:");
	  }

	save();
      }
    else if (e.getActionCommand().equals("Mail to..."))
      {
	sendMail();
      }
    else if (e.getActionCommand().equals("ReallyDelete"))
      {
	if (debug)
	  {
	    println("Deleting object");
	  }

	if (gc.deleteObject(getObjectInvid()))
	  {
	    try
	      {
		setClosed(true);
	      }
	    catch (PropertyVetoException ex)
	      {
		throw new RuntimeException("JInternalFrame will not close: " + ex);
	      }
	  }
	else
	  {
	    showErrorMessage("Error", "Could not delete object.");
	  }
      }
    else if (e.getActionCommand().equals("Close"))
      {
	try
	  {
	    if (debug)
	      {
		System.err.println("Processing close action command");
	      }

	    setClosed(true);
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
	    setIcon(true);
	  }
	catch (PropertyVetoException ex)
	  {
	    throw new RuntimeException("JInternalFrame will not change icon: " + ex);
	  }
      }
    else if (e.getActionCommand().equals("Print"))
      {
	printObject();
      }
    else if (e.getActionCommand().equals("Refresh"))
      {
	refresh();
      }
    else if (e.getActionCommand().equals("Inactivate"))
      {
	boolean success = gc.inactivateObject(getObjectInvid());
	
	if (success)
	  {
	    if (debug)
	      {
		System.err.println("removing window after inactivation");
	      }

	    try
	      {
		setClosed(true);
	      }
	    catch (PropertyVetoException ex)
	      {
		throw new RuntimeException("JInternalFrame will not close: " + ex);
	      }		
	  }
	else
	  {
	    showErrorMessage("Could not inactivate object.");
	  }
      }
    else if (e.getActionCommand().equals("Set Expiration Date"))
      {
	addExpirationDatePanel();
	showTab(expiration_date_index);
      }
    else if (e.getActionCommand().equals("Set Removal Date"))
      {
	addRemovalDatePanel();
	showTab(removal_date_index);
      }
    else
      {
	System.err.println("Unknow action event: " + e);
      }
  }

  // For the ChangeListener

  public void stateChanged(ChangeEvent e)
  {
    int index = pane.getSelectedIndex();

    if (!createdList.contains(new Integer(index)))
      {
	createPanel(index);
      }
  }

  /**
   * This checks to see if the panel is created, and creates it if needed.
   */

  public void createPanel(int index)
  {
    wp.gc.setWaitCursor();

    if (debug)
      {
	println("index = " + index + " general_index= " + general_index);
      }
    
    if (index == general_index)
      {
	setStatus("Creating general panel");
	create_general_panel();
      }
    else if (index == expiration_date_index)
      {
	setStatus("Creating dates panel");
	create_expiration_date_panel();
      }
    else if (index == removal_date_index)
      {
	setStatus("Creating dates panel");
	create_removal_date_panel();
      }
    else if (index == notes_index)
      {
	setStatus("Creating notes panel");
	create_notes_panel();
      }
    else if (index == owner_index)
      {
	setStatus("Creating owner panel");
	create_owner_panel();
      }
    else if (index == objects_owned_index)
      {
	setStatus("Creating objects owned panel");
	create_objects_owned_panel();
      }
    else if (index == personae_index)
      {
	setStatus("Creating persona panel");
	create_personae_panel();
      }
    else if (index == history_index)
      {
	setStatus("Creating history panel.");
	create_history_panel();
      }
    else if (index == admin_history_index)
      {
	setStatus("Creating admin history");
	create_admin_history_panel();
      }
    else
      {
	System.err.println("Unknown pane index: " + pane.getSelectedIndex());
      }
    
    wp.gc.setNormalCursor();
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

  private final void setStatus(String status)
  {
    wp.gc.setStatus(status);
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

  // InternalFrameListener methods

  public void internalFrameActivated(InternalFrameEvent event)
  {
  }

  /**
   * When the internalFrame closes, we need to shut down any auxiliary
   * internalFrames associated with fields in any contained container panels.
   */

  public void internalFrameClosed(InternalFrameEvent event)
  {
    if (debug)
      {
	System.err.println("framePanel.internalFrameClosed(): frame closed");
      }

    for (int i = 0; i < containerPanels.size(); i++)
      {
	containerPanel cp = (containerPanel) containerPanels.elementAt(i);
	
	cp.unregister();
      }
    
    if (history_panel != null)
      {
	history_panel.unregister();
      }

    this.dispose();
  }

  public void internalFrameClosing(InternalFrameEvent event)
  {
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
   * <p>This is a vetoableChangeListener implementation method, and is used
   * to allow the framePanel to intercede in window close attempts for
   * editable objects.  We use this intercession to explain to the user
   * what the meaning of closing an edit-object or create-object window
   * is, and to allow them to think again.</p>
   */

  public void vetoableChange(PropertyChangeEvent pce) throws PropertyVetoException
  {
    if (pce.getPropertyName().equals(IS_CLOSED_PROPERTY) &&
	pce.getOldValue().equals(Boolean.FALSE) &&
	pce.getNewValue().equals(Boolean.TRUE))
      {
	if (!closingApproved && editable)
	  {
	    StringDialog okToKill;
	    
	    if (isCreating)
	      {
		okToKill = new StringDialog(gclient.client, 
					    "Ok to discard " + getTitle() + "?",
					    "If you close this newly created window before committing this " +
					    "transaction, this newly created object will be forgotten and " +
					    "abandoned on commit.",
					    "Discard It",
					    "Cancel",
					    gclient.client.getQuestionImage());
	      }
	    else
	      {
		okToKill = new StringDialog(gclient.client, 
					    "Ok to hide " + getTitle() + "?",
					    "Closing this window will not undo changes made to it, nor will it make " +
					    "this object available to other Ganymede users to edit. If you want to " +
					    "undo changes to this object, you will have to either manually undo them " +
					    "or cancel the transaction.\n\n" +
					    "If this window is closed, you will be able to re-open it from the tree " +
					    "later if needed.",
					    "Hide it",
					    "Cancel",
					    gclient.client.getQuestionImage());
	      }

	    Hashtable result = okToKill.DialogShow();
	    
	    if (result == null)
	      {
		throw new PropertyVetoException("Cancelled", null);
	      }

	    // ah, we get to close.. delete us first.. this will cause the
	    // server to drop this object on commit.

	    if (isCreating)
	      {
		gclient.client.deleteObject(getObjectInvid());
	      }

	    closingApproved = true;
	  }
      }
  }
}

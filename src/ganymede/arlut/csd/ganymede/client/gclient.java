/*
   gclient.java

   Ganymede client main module

   Created: 24 Feb 1997
   Last Commit: $Format:%cd$

   Module By: Mike Mulvaney, Jonathan Abbey, and Navin Manohar

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2009
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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import java.util.prefs.*;

import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicToolBarUI;

import org.python.core.PySystemState;

import arlut.csd.JDataComponent.JSetValueObject;
import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JErrorValueObject;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDataComponent.LAFMenu;
import arlut.csd.JDataComponent.listHandle;
import arlut.csd.JDialog.DialogRsrc;
import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.JDialog.messageDialog;
import arlut.csd.JDialog.aboutGanyDialog;
import arlut.csd.JDialog.aboutJavaDialog;
import arlut.csd.JTree.treeCallback;
import arlut.csd.JTree.treeControl;
import arlut.csd.JTree.treeMenu;
import arlut.csd.JTree.treeNode;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VecQuickSort;
import arlut.csd.ganymede.common.BaseDump;
import arlut.csd.ganymede.common.CatTreeNode;
import arlut.csd.ganymede.common.CategoryDump;
import arlut.csd.ganymede.common.CategoryTransport;
import arlut.csd.ganymede.common.DumpResult;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.InvidPool;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectHandle;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.RegexpException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.windowSizer;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.Category;
import arlut.csd.ganymede.rmi.CategoryNode;
import arlut.csd.ganymede.rmi.Session;
import arlut.csd.ganymede.rmi.db_object;

import foxtrot.Task;
import foxtrot.Worker;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         gclient

------------------------------------------------------------------------------*/

/**
 * Main ganymede client class.  When {@link
 * arlut.csd.ganymede.client.glogin glogin} is run and a user logs in
 * to the server, the client obtains a {@link
 * arlut.csd.ganymede.rmi.Session Session} reference that allows it to
 * talk to the server on behalf of a user, and a single instance of
 * this class is created to handle all client GUI and networking
 * operations for that user.
 *
 * gclient creates a {@link arlut.csd.ganymede.client.windowPanel
 * windowPanel} object to contain internal object ({@link
 * arlut.csd.ganymede.client.framePanel framePanel}) and query windows
 * on the right side of a Swing JSplitPane.  The left side contains a
 * custom {@link arlut.csd.JTree.treeControl treeControl} GUI
 * component displaying object categories, types, and instances for
 * the user to browse and edit.
 *
 * @version $Id$
 * @author Mike Mulvaney, Jonathan Abbey, and Navin Manohar
 */

public final class gclient extends JFrame implements treeCallback, ActionListener, JsetValueCallback {

  public static boolean debug = false;  

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.gclient");

  /**
   * If reportVersionToServer is true, the Ganymede client will report
   * the Java version information to the server that it has logged
   * into on startup.
   */

  private static final boolean reportVersionToServer = false;

  /**
   * Preferences object for the Ganymede client.  Using this object,
   * we can save and retrieve preferences data from a system-dependent
   * backing-store.. the Registry on Windows, a XML file under
   * ~/.java/user-prefs/ on Linux/Unix/Mac, and who-knows-what on
   * other platforms.
   */

  public static final Preferences prefs;

  // If we're running as an applet, we might not be able to
  // successfully load our static Preferences reference.  Make sure
  // that we don't block this class' static initialization if we can't
  // get Preferences.

  static
  {
    Preferences _prefs = null;

    try
      {
	_prefs = Preferences.userNodeForPackage(gclient.class);
      }
    catch (Throwable ex)
      {
	ex.printStackTrace();
      }

    prefs = _prefs;
  }

  public static final windowSizer sizer = new windowSizer(prefs);

  /**
   * we're only going to have one gclient at a time per running client (singleton pattern).
   */

  public static gclient client = null;

  // Image numbers

  static final int NUM_IMAGE = 17;
  
  static final int OPEN_BASE = 0;
  static final int CLOSED_BASE = 1;

  static final int OPEN_FIELD = 2;
  static final int OPEN_FIELD_DELETE = 3;
  static final int OPEN_FIELD_CREATE = 4;
  static final int OPEN_FIELD_CHANGED = 5;
  static final int OPEN_FIELD_REMOVESET = 6;
  static final int OPEN_FIELD_EXPIRESET = 7;
  static final int CLOSED_FIELD = 8;
  static final int CLOSED_FIELD_DELETE = 9;
  static final int CLOSED_FIELD_CREATE = 10;
  static final int CLOSED_FIELD_CHANGED = 11;
  static final int CLOSED_FIELD_REMOVESET = 12;
  static final int CLOSED_FIELD_EXPIRESET = 13;

  static final int OPEN_CAT = 14;
  static final int CLOSED_CAT = 15;

  static final int OBJECTNOWRITE = 16;

  /* our fixed (no localization needed) action command strings for
   * node-attached popup menus. */

  private static String 
    hide_pop_action = "Hide Non-Editables",
    show_pop_action = "Show Non-Editables",
    query_pop_action = "Query",
    report_edit_pop_action = "Report editable",
    report_pop_action = "Report all",
    create_pop_action = "Create",
    view_pop_action = "View Object",
    edit_pop_action = "Edit Object",
    clone_pop_action = "Clone Object",
    delete_pop_action = "Delete Object",
    inactivate_pop_action = "Inactivate Object",
    reactivate_pop_action = "Reactivate Object";

  /* our fixed (no localization needed) action command strings for
   * menus and toolbar items. */

  private static String
    persona_action = "change persona",
    query_action = "compose a query",
    create_action = "create new object",
    edit_action = "open object for editing",
    view_action = "open object for viewing",
    delete_action = "delete an object",
    clone_action = "clone an object",
    inactivate_action = "inactivate an object",
    access_invid_action = "Show me an Invid",
    owner_filter_action = "Set Owner Filter",
    default_owner_action = "Set Default Owner",
    help_action = "Help",
    about_action = "About Ganymede",
    java_version_action = "Java Version",
    motd_action = "Message of the day";

  /**
   * This is a convenience method used by the server to get a
   * stack trace from a throwable object in String form.
   */

  static public String stackTrace(Throwable thing)
  {
    StringWriter stringTarget = new StringWriter();
    PrintWriter writer = new PrintWriter(stringTarget);
    
    thing.printStackTrace(writer);
    writer.close();

    return stringTarget.toString();
  }

  // ---

  /**
   * Main remote interface for communications with the server.
   */
  
  Session session;

  /**
   * Reference to the applet which instantiated us.
   */

  glogin _myglogin;

  /**
   * Local copy of the category/object tree downloaded from
   * the server by the {@link arlut.csd.ganymede.client.gclient#buildTree() buildTree()}
   * method.
   */

  CategoryDump dump;

  /**
   * Name of the currently active persona.
   */

  String currentPersonaString;

  // set up a bunch of borders
  // Turns out we don't need to do this anyway, since the BorderFactory does it for us.

  public EmptyBorder
    emptyBorder5 = (EmptyBorder) BorderFactory.createEmptyBorder(5,5,5,5),
    emptyBorder10 = (EmptyBorder) BorderFactory.createEmptyBorder(10,10,10,10);

  public BevelBorder
    raisedBorder = (BevelBorder) BorderFactory.createBevelBorder(BevelBorder.RAISED),
    loweredBorder = (BevelBorder) BorderFactory.createBevelBorder(BevelBorder.LOWERED);
      
  public LineBorder
    lineBorder = (LineBorder) BorderFactory.createLineBorder(Color.black);

  public CompoundBorder
    statusBorder = BorderFactory.createCompoundBorder(loweredBorder, emptyBorder5),
    statusBorderRaised = BorderFactory.createCompoundBorder(raisedBorder, emptyBorder5);

  //
  // Yum, caches
  //

  /** 
   * Cache of {@link arlut.csd.ganymede.common.Invid invid}'s for objects
   * that might have been changed by the client.  The keys and the
   * values in this hash are the same.  The collection of tree nodes
   * corresponding to invid's listed in changedHash will be refreshed
   * by the client when a server is committed or canceled.
   */

  private Hashtable changedHash = new Hashtable();

  /** 
   * Mapping of {@link arlut.csd.ganymede.common.Invid invid}'s for objects
   * that the client has requested be deleted by the server to
   * {@link arlut.csd.ganymede.client.CacheInfo CacheInfo} objects
   * which hold information about the object used to make decisions
   * about managing the client's tree display.
   */

  private Hashtable deleteHash = new Hashtable();

  /**  
   * Mapping of {@link arlut.csd.ganymede.common.Invid invid}'s for objects
   * that the client has requested be created by the server to
   * {@link arlut.csd.ganymede.client.CacheInfo CacheInfo} objects
   * which hold information about the object used to make decisions
   * about managing the client's tree display.
   */

  private Hashtable createHash = new Hashtable();

  /**
   * Hash of {@link arlut.csd.ganymede.common.Invid invid}'s corresponding
   * to objects that have been created by the client but which have not
   * had nodes created in the client's tree display.  Once nodes are
   * created for these objects, the invid will be taken out of this
   * hash and put into createHash.
   */

  private Hashtable createdObjectsWithoutNodes = new Hashtable();

  /**
   * Hash mapping Short {@link arlut.csd.ganymede.rmi.Base Base} id's to
   * the corresponding {@link arlut.csd.ganymede.client.BaseNode BaseNode}
   * displayed in the client's tree display.
   */

  protected Hashtable shortToBaseNodeHash = new Hashtable();

  /**
   * Hash mapping {@link arlut.csd.ganymede.common.Invid Invid}'s for objects
   * referenced by the client to the corresponding
   * {@link arlut.csd.ganymede.client.InvidNode InvidNode} displayed in the
   * client's tree display.
   */

  protected Hashtable invidNodeHash = new Hashtable();

  /**
   * Our main cache, keeps information about all objects we've learned
   * about via {@link arlut.csd.ganymede.common.QueryResult QueryResult}'s returned
   * to us by the server.
   *
   * We can get QueryResults from the server by doing direct
   * {@link arlut.csd.ganymede.rmi.Session#query(arlut.csd.ganymede.common.Query) query}
   * calls on the server, or by calling choices() on an 
   * {@link arlut.csd.ganymede.rmi.invid_field invid_field} or on a
   * {@link arlut.csd.ganymede.rmi.string_field string_field}.  Information from
   * both sources may be integrated into this cache.
   */

  protected objectCache cachedLists = new objectCache();

  /**
   * Background processing thread, downloads information on
   * object and field types defined in the server when run.
   */
 
  Loader loader;
  
  //
  // Status tracking
  //

  private boolean
    toolToggle = true,
    somethingChanged = false;  // This will be set to true if the user changes anything

  private int
    buildPhase = -1;		// unknown
  
  helpPanel
    help = null;

  messageDialog
    motd = null;

  aboutGanyDialog
    about = null;

  aboutJavaDialog
    java_ver_dialog = null;

  Vector
    personae,
    ownerGroups = null;  // Vector of owner groups

  // Dialog and GUI objects


  JToolBar 
    toolBar;
    
  JFilterDialog
    filterDialog = null;

  PersonaDialog
    personaDialog = null;

  JDefaultOwnerDialog
    defaultOwnerDialog = null;

  openObjectDialog
    openDialog;

  createObjectDialog
    createDialog = null;

  Image images[];

  JButton 
    commit,
    cancel;

  private JPanel
    statusPanel = new JPanel(new BorderLayout());

  private JSplitPane sPane = null;

  /**
   * Status, build status, and login status labels at the bottom of the client.
   */

  JLabel
    statusLabel = new JLabel(),
    buildLabel = new JLabel(),
    loginLabel = new JLabel();

  /**
   * The client's GUI tree component.
   */

  treeControl tree;

  /**
   * The currently selected node from the client's GUI tree.
   */

  treeNode
    selectedNode;

  // The top lines

  Image
    errorImage = null,
    questionImage = null,
    infoImage = null,
    search,
    queryIcon,
    cloneIcon,
    pencil,
    personaIcon,
    inactivateIcon,
    treepencil,
    trash,
    treetrash,
    creation,
    treecreation,
    newToolbarIcon,
    ganymede_logo,
    createDialogImage;

  ImageIcon
    idleIcon, buildIcon, buildIcon2, buildUnknownIcon;

  /**
   * JDesktopPane on the right side of the client's display, contains
   * the object and query result internal windows that are created
   * during the client's execution.
   */

  windowPanel
    wp;

  //
  // Menu resources
  //

  treeMenu 
    pMenuAll = new treeMenu(),
    pMenuEditable= new treeMenu(),
    pMenuEditableCreatable = new treeMenu(),
    pMenuAllCreatable = new treeMenu(),
    objectViewPM = new treeMenu(),
    objectReactivatePM = new treeMenu(),
    objectInactivatePM = new treeMenu(),
    objectRemovePM = new treeMenu();
  
  JMenuBar 
    menubar;

  JMenuItem 
    logoutMI,
    clearTreeMI,
    filterQueryMI,
    defaultOwnerMI,
    showHelpMI,
    toggleToolBarMI,
    submitXMLMI;

  JCheckBoxMenuItem
    hideNonEditablesMI;

  /**
   * If true, the client will only display object types that the
   * user has permission to edit, and by default will only show objects
   * in the tree that the user can edit.  If false, all objects and
   * object types the the user has permission to view will be shown
   * in the tree.  Toggled by the user manipulating the hideNonEditablesMI
   * check box menu item.
   */

  boolean    hideNonEditables = true;

  boolean defaultOwnerChosen = false;

  JMenuItem
    changePersonaMI,
    editObjectMI,
    viewObjectMI,
    createObjectMI,
    deleteObjectMI,
    inactivateObjectMI,
    menubarQueryMI = null;

  String
    my_username;

  JMenu 
    actionMenu,
    windowMenu,
    fileMenu,
    helpMenu,
    PersonaMenu = null;
  
  LAFMenu
    LandFMenu;

  /**
   * Listener to react to persona dialog events
   */

  PersonaListener
    personaListener = null;

  /**
   * Query dialog that is displayed when the user chooses to perform
   * a query on the server.
   */

  querybox
    my_querybox = null;

  /**
   * This thread is used to clear the statusLabel after some interval after
   * it is set.
   *
   * Whenever the gclient's
   * {@link arlut.csd.ganymede.client.gclient#setStatus(java.lang.String,int) setStatus}
   * method is called, this thread has a countdown timer started, which will
   * clear the status label if it is not reset by another call to setStatus.
   */

  public StatusClearThread statusThread, loginStatusThread;

  /**
   * This thread is set up to launder RMI build status updates from the server.
   *
   * In some versions of Sun's JDK, RMI callbacks are not allowed to manipulate
   * the GUI event queue.  To get around this, this securityThread is created
   * to launder these RMI callbacks so that the Swing event queue is messed with
   * by a client-local thread.
   */

  public SecurityLaunderThread securityThread;

  /**
   * this is true during the handleReturnVal method, while a wizard is
   * active.  If a wizard is active, don't allow the window to close.
   */

  private int wizardActive = 0;

  /* -- */

  /**
   * This is the main constructor for the gclient class.. it handles the
   * interactions between the user and the server once the user has
   * logged in.
   *
   * @param s Connection to the server created for us by the glogin applet.
   * @param g The glogin applet which is creating us.
   */

  public gclient(Session s, glogin g)
  {
    JPanel
      leftP,
      leftTop,
      rightTop,
      mainPanel;   // Everything is in this, so it is double buffered

    /* -- */

    if (gclient.client != null)
      {
        throw new IllegalStateException("Singleton gclient already created.");
      }

    client = this;

    Invid.setAllocator(new InvidPool(3257)); // modest sized prime, should be adequate for the client

    if (!debug)
      {
	debug = g.debug;
      }

    if (debug)
      {
	System.err.println("Starting client");
      }

    enableEvents(AWTEvent.WINDOW_EVENT_MASK);

    if (s == null)
      {
	throw new IllegalArgumentException("Ganymede Error: Parameter for Session s is null");
      }

    session = s;
    _myglogin = g;
    my_username = g.getUserName();

    try
      {
	currentPersonaString = session.getActivePersonaName();

	// In gclient, currentPersonaString must not be null.. if we
	// didn't get a privileged persona, assume unprivileged
	// end-user account name.

	if (currentPersonaString == null)
	  {
	    currentPersonaString = my_username;
	  }

	// "Ganymede Client: {0} logged in"
	setTitle(ts.l("global.logged_in_title", currentPersonaString));
      }
    catch (RemoteException rx)
      {
	processExceptionRethrow(rx);
      }

    if (reportVersionToServer)
      {
        try
          {
            session.reportClientVersion(aboutJavaDialog.getVersionInfoString());
          }
        catch (RemoteException ex)
          {
            // nada
          }
      }

    JDefaultOwnerDialog.clear();

    mainPanel = new JPanel(true);
    mainPanel.setLayout(new BorderLayout());

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add("Center", mainPanel);

    if (debug)
      {
	System.err.println("Creating menu bar");
      }

    // Make the menu bar

    menubar = createMenuBar();
    setJMenuBar(menubar);

    createTree();

    if (debug)
      {
	System.err.println("Adding left and right panels");
      }

    leftP = new JPanel(false);
    leftP.setLayout(new BorderLayout());
    leftP.add("Center", tree);
    
    try
      {
	buildTree();
      }
    catch (Exception ex)
      {
	processExceptionRethrow(ex, "caught remote exception in buildTree");
      }

    // The right panel which will contain the windowPanel

    JPanel rightP = new JPanel(true);
    rightP.setLayout(new BorderLayout());

    wp = new windowPanel(this, windowMenu);

    rightP.add("Center", wp);
    rightTop = new JPanel(false);
    rightTop.setBorder(statusBorderRaised);
    rightTop.setLayout(new BorderLayout());
    
    toolBar = createToolbar();

    getContentPane().add("North", toolBar);

    // "Commit"    
    commit = new JButton(ts.l("init.commit_button"));
    commit.setEnabled(false);
    commit.setOpaque(true);
    commit.setToolTipText(ts.l("init.commit_tooltip")); // "Click this to commit your transaction to the database"
    commit.addActionListener(this);

    // "Cancel"
    cancel = new JButton(ts.l("init.cancel_button"));
    cancel.setEnabled(false);
    cancel.setOpaque(true);
    cancel.setToolTipText(ts.l("init.cancel_tooltip"));	// "Click this to cancel your transaction"
    cancel.addActionListener(this);

    // Button bar at bottom, includes commit/cancel panel and taskbar

    JPanel bottomButtonP = new JPanel(false);

    bottomButtonP.add(commit);
    bottomButtonP.add(cancel);
    bottomButtonP.setBorder(loweredBorder);

    // Create the pane splitter

    sPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftP, rightP);
    sPane.setOneTouchExpandable(true);
   
    mainPanel.add("Center",sPane);

    // Create the bottomBar, for the bottom of the window

    JPanel bottomBar = new JPanel(false);
    bottomBar.setLayout(new BorderLayout());

    JPanel statusPanel = new JPanel(false);
    statusPanel.setLayout(new BorderLayout());
    statusPanel.setBorder(statusBorder);

    statusLabel.setOpaque(false);
    loginLabel.setOpaque(false);
    loginLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);

    statusPanel.add("West", statusLabel);
    statusPanel.add("East", loginLabel);

    statusThread = new StatusClearThread(statusLabel);
    statusThread.start();

    // start the securityThread to launder RMI calls from the server
    
    securityThread = new SecurityLaunderThread(this);
    securityThread.start();

    JPanel lP = new JPanel(new BorderLayout());
    lP.setBorder(statusBorder);
    lP.add("Center", buildLabel);

    bottomBar.add("West", lP);
    bottomBar.add("Center", statusPanel);
    bottomBar.add("East", bottomButtonP);

    mainPanel.add("South", bottomBar);

    // "Starting up"
    setStatus(ts.l("init.startup_msg"));

    // Since we're logged in and have a session established, create the
    // background loader thread to read in object and field type information

    loader = new Loader(session, debug);
    loader.start();

    try
      {
	ownerGroups = session.getOwnerGroups().getListHandles();

	if (ownerGroups.size() == 0)
	  {
	    defaultOwnerMI.setEnabled(false);
	  }
	else if (ownerGroups.size() == 1)
	  {
	    chooseDefaultOwner(false); // tells the session our default owner
	    defaultOwnerMI.setEnabled(false);
	  }
      }
    catch (RemoteException ex)
      {
	processExceptionRethrow(ex);
      }

    pack();

    if (!sizer.restoreSize(this))
      {
	setSize(800, 600);
	this.setLocationRelativeTo(null); // center gclient frame
	sizer.saveSize(this);
      }

    tree.requestFocus();

    this.setVisible(true);

    setLoginCount(g.getInitialLoginCount());

    getContentPane().validate();
  }

  /* Private stuff */

  /**
   * Create our client's menu bar from localization resources.
   */

  private JMenuBar createMenuBar()
  {
    JMenuBar menubar = new JMenuBar();

    //menubar.setBorderPainted(true);
    
    // File menu

    // "File"
    fileMenu = new JMenu(ts.l("createMenuBar.file_menu"));
    setMenuMnemonic(fileMenu, ts.l("createMenuBar.file_menu_key_optional"));

    // "Clear Tree"
    clearTreeMI = new JMenuItem(ts.l("createMenuBar.file_menu_0"));
    setMenuMnemonic(clearTreeMI, ts.l("createMenuBar.file_menu_0_key_optional"));
    clearTreeMI.addActionListener(this);

    // "Set Owner Filter"
    filterQueryMI = new JMenuItem(ts.l("createMenuBar.file_menu_1"));
    setMenuMnemonic(filterQueryMI, ts.l("createMenuBar.file_menu_1_key_optional"));
    filterQueryMI.setActionCommand("Set Owner Filter"); 
    filterQueryMI.addActionListener(this);

    // "Set Default Owner"
    defaultOwnerMI = new JMenuItem(ts.l("createMenuBar.file_menu_2"));
    setMenuMnemonic(defaultOwnerMI, ts.l("createMenuBar.file_menu_2_key_optional"));
    defaultOwnerMI.setActionCommand("Set Default Owner");
    defaultOwnerMI.addActionListener(this);

    // "Hide non-editable objects"
    hideNonEditablesMI = new JCheckBoxMenuItem(ts.l("createMenuBar.file_menu_3"), true);
    setMenuMnemonic(hideNonEditablesMI, ts.l("createMenuBar.file_menu_3_key_optional"));
    hideNonEditablesMI.addActionListener(this);

    // "Submit XML data"
    submitXMLMI = new JMenuItem(ts.l("createMenuBar.file_menu_5"));
    setMenuMnemonic(submitXMLMI, ts.l("createMenuBar.file_menu_5_key_optional"));
    submitXMLMI.setActionCommand("Submit XML"); 
    submitXMLMI.addActionListener(this);

    // "Logout"
    logoutMI = new JMenuItem(ts.l("createMenuBar.file_menu_4"));
    setMenuMnemonic(logoutMI, ts.l("createMenuBar.file_menu_4_key_optional"));    
    logoutMI.addActionListener(this);

    fileMenu.add(clearTreeMI);
    fileMenu.add(filterQueryMI);
    fileMenu.add(defaultOwnerMI);
    fileMenu.add(hideNonEditablesMI);
    fileMenu.addSeparator();
    fileMenu.add(submitXMLMI);
    fileMenu.addSeparator();
    fileMenu.add(logoutMI);

    // Action menu

    // "Actions"
    actionMenu = new JMenu(ts.l("createMenuBar.action_menu"));
    setMenuMnemonic(actionMenu, ts.l("createMenuBar.action_menu_key_optional"));

    // Personae init

    try
      {
	personae = session.getPersonae();
      }
    catch (Exception rx)
      {
	processExceptionRethrow(rx);
      }

    personaListener = new PersonaListener(session, this);

    if ((personae != null) && personae.size() > 1)
      {
	// "Change Persona"
	changePersonaMI = new JMenuItem(ts.l("createMenuBar.action_menu_0"));
	setMenuMnemonic(changePersonaMI, ts.l("createMenuBar.action_menu_0_key_optional"));
	changePersonaMI.setActionCommand(persona_action);
	changePersonaMI.addActionListener(this);
      }

    // "Query"
    menubarQueryMI = new JMenuItem(ts.l("createMenuBar.action_menu_1"));
    setMenuMnemonic(menubarQueryMI, ts.l("createMenuBar.action_menu_1_key_optional"));
    menubarQueryMI.setActionCommand(query_action);
    menubarQueryMI.addActionListener(this);

    // "View Object"
    viewObjectMI = new JMenuItem(ts.l("createMenuBar.action_menu_2"));
    setMenuMnemonic(viewObjectMI, ts.l("createMenuBar.action_menu_2_key_optional"));
    viewObjectMI.setActionCommand(view_action);
    viewObjectMI.addActionListener(this);

    // "Create Object"    
    createObjectMI = new JMenuItem(ts.l("createMenuBar.action_menu_3"));
    setMenuMnemonic(createObjectMI, ts.l("createMenuBar.action_menu_3_key_optional"));
    createObjectMI.setActionCommand(create_action);
    createObjectMI.addActionListener(this);

    // "Edit Object"    
    editObjectMI = new JMenuItem(ts.l("createMenuBar.action_menu_4"));
    setMenuMnemonic(editObjectMI, ts.l("createMenuBar.action_menu_4_key_optional"));
    editObjectMI.setActionCommand(edit_action);
    editObjectMI.addActionListener(this);

    // "Delete Object"
    deleteObjectMI = new JMenuItem(ts.l("createMenuBar.action_menu_5"));
    setMenuMnemonic(deleteObjectMI, ts.l("createMenuBar.action_menu_5_key_optional"));
    deleteObjectMI.setActionCommand(delete_action);
    deleteObjectMI.addActionListener(this);

    // "Inactivate Object"
    inactivateObjectMI = new JMenuItem(ts.l("createMenuBar.action_menu_6"));
    setMenuMnemonic(inactivateObjectMI, ts.l("createMenuBar.action_menu_6_key_optional"));
    inactivateObjectMI.setActionCommand(inactivate_action);
    inactivateObjectMI.addActionListener(this);

    if (changePersonaMI != null)
      {
	actionMenu.add(changePersonaMI);
      }

    actionMenu.add(menubarQueryMI);
    actionMenu.addSeparator();
    actionMenu.add(viewObjectMI);
    actionMenu.add(createObjectMI);
    actionMenu.add(editObjectMI);
    actionMenu.add(deleteObjectMI);
    actionMenu.add(inactivateObjectMI);

    if (debug)
      {
	// "Access Invid"
	JMenuItem viewAnInvid = new JMenuItem(ts.l("createMenuBar.action_menu_7"));
	setMenuMnemonic(viewAnInvid, ts.l("createMenuBar.action_menu_7_key_optional"));
	viewAnInvid.setActionCommand(access_invid_action);
	viewAnInvid.addActionListener(this);
	actionMenu.addSeparator();
	actionMenu.add(viewAnInvid);
      }

    // windowMenu

    // "Windows"
    windowMenu = new JMenu(ts.l("createMenuBar.window_menu"));
    setMenuMnemonic(windowMenu, ts.l("createMenuBar.window_menu_key_optional"));

    // "Toggle Toolbar"
    toggleToolBarMI = new JMenuItem(ts.l("createMenuBar.window_menu_0"));
    setMenuMnemonic(toggleToolBarMI, ts.l("createMenuBar.window_menu_0_key_optional"));
    toggleToolBarMI.addActionListener(this);

    windowMenu.add(toggleToolBarMI);
   
    // Look and Feel menu

    LandFMenu = new arlut.csd.JDataComponent.LAFMenu(this);
    LandFMenu.setMnemonic('l');	// XXX need to localize.. probably should be done in LAFMenu itself
    LandFMenu.setCallback(this);

    // Help menu

    // "Help"
    helpMenu = new JMenu(ts.l("createMenuBar.help_menu"));
    setMenuMnemonic(helpMenu, ts.l("createMenuBar.help_menu_key_optional"));

    // These use action commands, so you don't need to globally
    // declare these

    // "About Ganymede"
    JMenuItem showAboutMI = new JMenuItem(ts.l("createMenuBar.help_menu_0"));
    setMenuMnemonic(showAboutMI, ts.l("createMenuBar.help_menu_0_key_optional"));
    showAboutMI.setActionCommand(about_action);
    showAboutMI.addActionListener(this);
    helpMenu.add(showAboutMI);

    // "Message of the day"
    JMenuItem showMOTDMI = new JMenuItem(ts.l("createMenuBar.help_menu_1"));
    setMenuMnemonic(showMOTDMI, ts.l("createMenuBar.help_menu_1_key_optional"));
    showMOTDMI.setActionCommand(motd_action);
    showMOTDMI.addActionListener(this);
    helpMenu.add(showMOTDMI);

    helpMenu.addSeparator();

    // "Java Version"
    JMenuItem javaVersionMI = new JMenuItem(ts.l("createMenuBar.help_menu_2"));
    setMenuMnemonic(javaVersionMI, ts.l("createMenuBar.help_menu_2_key_optional"));
    javaVersionMI.setActionCommand(java_version_action);
    javaVersionMI.addActionListener(this);
    helpMenu.add(javaVersionMI);

    menubar.add(fileMenu);
    menubar.add(LandFMenu);
    menubar.add(actionMenu);
    menubar.add(windowMenu);
    // we want to force the helpMenu to be on the far right side of
    // the menu bar..
    menubar.add(Box.createGlue());
    menubar.add(helpMenu);

    return menubar;
  }

  /**
   * This helper method sets the mnemonic character for a menu item.
   * The pattern string will generally be retrieved from the
   * localization properties, and may be null.  This method is
   * designed to deal with null patterns, since any given menu item
   * may not, in fact, have a mnemonic set in the properties resource.
   */

  private void setMenuMnemonic(JMenuItem item, String pattern)
  {
    if (pattern != null)
      {
	item.setMnemonic((int) pattern.charAt(0));
      }
  }

  /**
   * Create the tree component used in the left hand side of the client.
   */

  private void createTree()
  {
    if (debug)
      {
	System.err.println("Creating tree");
      }

    /* pick up the pop-up menu strings from our localization
     * resources */

    String
      hide = ts.l("createTree.hide_non_editable"), // "Hide Non-Editables"
      show = ts.l("createTree.show_non_editable"), // "Show Non-Editables"
      query = ts.l("createTree.query"),	// "Query"
      report_editable = ts.l("createTree.report_editable"), // "Report Editable"
      report = ts.l("createTree.report"), // "Report All"
      create = ts.l("createTree.create"), // "Create"
      view = ts.l("createTree.view_object"), // "View Object"
      edit = ts.l("createTree.edit_object"), // "Edit Object"
      clone = ts.l("createTree.clone_object"), // "Clone Object"
      delete = ts.l("createTree.delete_object"), // "Delete Object"
      inactivate = ts.l("createTree.inactivate_object"), // "Inactivate Object"
      reactivate = ts.l("createTree.reactivate_object"); // "Reactivate Object"

    /* but our action commands are fixed. */

    // note that a lot of these are given the same text, but a
    // JMenuItem can only belong to one menu at a time, so we have to
    // create multiple copies

    pMenuAll.add(createMenuItem(hide, hide_pop_action));
    pMenuAll.add(createMenuItem(query, query_pop_action));
    pMenuAll.add(createMenuItem(report_editable, report_edit_pop_action));
    pMenuAll.add(createMenuItem(report, report_pop_action));

    pMenuEditable.add(createMenuItem(show, show_pop_action));
    pMenuEditable.add(createMenuItem(query, query_pop_action));
    pMenuEditable.add(createMenuItem(report_editable, report_edit_pop_action));
    pMenuEditable.add(createMenuItem(report, report_pop_action));

    pMenuAllCreatable.add(createMenuItem(hide, hide_pop_action));
    pMenuAllCreatable.add(createMenuItem(query, query_pop_action));
    pMenuAllCreatable.add(createMenuItem(report_editable, report_edit_pop_action));
    pMenuAllCreatable.add(createMenuItem(report, report_pop_action));
    pMenuAllCreatable.add(createMenuItem(create, create_pop_action));

    pMenuEditableCreatable.add(createMenuItem(show, show_pop_action));
    pMenuEditableCreatable.add(createMenuItem(query, query_pop_action));
    pMenuEditableCreatable.add(createMenuItem(report_editable, report_edit_pop_action));
    pMenuEditableCreatable.add(createMenuItem(report, report_pop_action));
    pMenuEditableCreatable.add(createMenuItem(create, create_pop_action));

    objectViewPM.add(createMenuItem(view, view_pop_action));
    objectViewPM.add(createMenuItem(clone, clone_pop_action));

    objectRemovePM.add(createMenuItem(view, view_pop_action));
    objectRemovePM.add(createMenuItem(edit, edit_pop_action));
    objectRemovePM.add(createMenuItem(clone, clone_pop_action));
    objectRemovePM.add(createMenuItem(delete, delete_pop_action));

    objectInactivatePM.add(createMenuItem(view, view_pop_action));
    objectInactivatePM.add(createMenuItem(edit, edit_pop_action));
    objectInactivatePM.add(createMenuItem(clone, clone_pop_action));
    objectInactivatePM.add(createMenuItem(delete, delete_pop_action));
    objectInactivatePM.add(createMenuItem(inactivate, inactivate_pop_action));

    objectReactivatePM.add(createMenuItem(view, view_pop_action));
    objectReactivatePM.add(createMenuItem(edit, edit_pop_action));
    objectReactivatePM.add(createMenuItem(clone, clone_pop_action));
    objectReactivatePM.add(createMenuItem(delete, delete_pop_action));
    objectReactivatePM.add(createMenuItem(reactivate, reactivate_pop_action));

    if (debug)
      {
	System.err.println("Loading images for tree");
      }

    ganymede_logo = _myglogin.ganymede_logo;

    Image openFolder = PackageResources.getImageResource(this, "openfolder.gif", getClass());
    Image closedFolder = PackageResources.getImageResource(this, "folder.gif", getClass());
    Image list = PackageResources.getImageResource(this, "list.gif", getClass());
    Image listnowrite = PackageResources.getImageResource(this, "listnowrite.gif", getClass());
    Image redOpenFolder = PackageResources.getImageResource(this, "openfolder-red.gif", getClass());
    Image redClosedFolder = PackageResources.getImageResource(this, "folder-red.gif", getClass());
    
    search = PackageResources.getImageResource(this, "srchfol2.gif", getClass());
    queryIcon = PackageResources.getImageResource(this, "query.gif", getClass());
    cloneIcon = PackageResources.getImageResource(this, "clone.gif", getClass());
    idleIcon = new ImageIcon(PackageResources.getImageResource(this, "nobuild.gif", getClass()));
    buildUnknownIcon = new ImageIcon(PackageResources.getImageResource(this, "buildunknown.gif", getClass()));
    buildIcon = new ImageIcon(PackageResources.getImageResource(this, "build1.gif", getClass()));
    buildIcon2 = new ImageIcon(PackageResources.getImageResource(this, "build2.gif", getClass()));
    trash = PackageResources.getImageResource(this, "trash.gif", getClass());
    creation = PackageResources.getImageResource(this, "creation.gif", getClass());
    newToolbarIcon = PackageResources.getImageResource(this, "newicon.gif", getClass());
    pencil = PackageResources.getImageResource(this, "pencil.gif", getClass());
    personaIcon = PackageResources.getImageResource(this, "persona.gif", getClass());
    //    inactivateIcon = PackageResources.getImageResource(this, "inactivate.gif", getClass());

    // we'll use the pencil/editing image for our client's application icon

    setIconImage(pencil);

    createDialogImage = PackageResources.getImageResource(this, "wiz3b.gif", getClass());

    treepencil = PackageResources.getImageResource(this, "treepencil.gif", getClass());
    treetrash = PackageResources.getImageResource(this, "treetrash.gif", getClass());
    treecreation = PackageResources.getImageResource(this, "treenewicon.gif", getClass());

    Image remove = PackageResources.getImageResource(this, "remove.gif", getClass());
    Image expire = PackageResources.getImageResource(this, "expire.gif", getClass());

    images = new Image[NUM_IMAGE];
    images[OPEN_BASE] =  openFolder;
    images[CLOSED_BASE ] = closedFolder;
    
    images[OPEN_FIELD] = list;
    images[OPEN_FIELD_DELETE] = treetrash;
    images[OPEN_FIELD_CREATE] = treecreation;
    images[OPEN_FIELD_CHANGED] = treepencil;
    images[OPEN_FIELD_EXPIRESET] = expire;
    images[OPEN_FIELD_REMOVESET] = remove;
    images[CLOSED_FIELD] = list;
    images[CLOSED_FIELD_DELETE] = treetrash;
    images[CLOSED_FIELD_CREATE] = treecreation;
    images[CLOSED_FIELD_CHANGED] = treepencil;
    images[CLOSED_FIELD_EXPIRESET] = expire;
    images[CLOSED_FIELD_REMOVESET] = remove;
    
    images[OPEN_CAT] = redOpenFolder;
    images[CLOSED_CAT] = redClosedFolder;

    images[OBJECTNOWRITE] = listnowrite;

    tree = new treeControl(new Font("SansSerif", Font.PLAIN, 12),
			   Color.black, Color.white, this, images,
			   null);

    tree.setMinimumWidth(200);
  }

  private JMenuItem createMenuItem(String text, String actionCommand)
  {
    JMenuItem menuItem = new JMenuItem(text);

    menuItem.setActionCommand(actionCommand);

    return menuItem;
  }

  /**
   *
   * This method handles the start-up tasks after the gclient
   * has gotten initialized.  Called by glogin.
   * 
   */

  public void start()
  {
    // open an initial transaction, in case the user doesn't change
    // personae

    try
      {
	ReturnVal rv = session.openTransaction("Ganymede GUI Client");
	rv = handleReturnVal(rv);

	if ((rv != null) && (!rv.didSucceed()))
	  {
	    return;
	  }
      }
    catch (Exception rx)
      {
	// "Could not open transaction."
	processExceptionRethrow(rx, ts.l("start.transaction_open_failure"));
      }

    // If user has multiple personae and he has logged in without
    // specifying an admin persona, ask which to start with.

    if ((personae != null)  && personae.size() > 1 && currentPersonaString.indexOf(':') == -1)
      {
	// changePersona will block until the user does something
	// with the persona selection dialog

	changePersona(false);
	personaDialog.updatePassField(currentPersonaString);
      }

    // Check for MOTD on another thread

    Thread motdThread = new Thread(new Runnable() {
      public void run() {
	try
	  {
	    // "Checking MOTD"
	    setStatus(ts.l("start.motd_msg"), 1);

	    StringBuffer m;
	    boolean html = true;

	    m = session.getMessageHTML("motd", true);

	    // if there wasn't an html motd message, maybe there's a
	    // txt message?

	    if (m == null)
	      {
		m = session.getMessage("motd", true);
		html = false;
	      }

	    // if we didn't get any message, fold it up, we're done

	    if (m == null)
	      {
		return;
	      }

	    // and pop up the motd box back on the main GUI thread

	    // create final locals to bridge the gap into another
	    // method in the runnable to go on the GUI thread

	    final String textString = m.toString();
	    final boolean doHTML = html;

	    EventQueue.invokeLater(new Runnable() {
	      public void run() {
		showMOTD(textString, doHTML);
	      }
	    });
	  }
	catch (Exception rx)
	  {
	    // "Could not get MOTD"
	    processExceptionRethrow(rx, ts.l("start.motd_failure"));
	  }
      }
    });

    motdThread.setPriority(Thread.NORM_PRIORITY);
    motdThread.start();
  }
  
  /**
   * Returns a vector of 
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}'s.
   *
   * @param id Object type id to retrieve field information for.
   */

  public Vector getTemplateVector(short id)
  {
    return loader.getTemplateVector(Short.valueOf(id));
  }

  /**
   * Returns a {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}
   * based on the short type id for the containing object and the
   * short field id for the field.
   */

  public FieldTemplate getFieldTemplate(short objType, short fieldId)
  {
    Vector vect = loader.getTemplateVector(Short.valueOf(objType));

    for (int i = 0; i < vect.size(); i++)
      {
	FieldTemplate template = (FieldTemplate) vect.elementAt(i);

	if (template.getID() == fieldId)
	  {
	    return template;
	  }
      }

    return null;
  }

  /**
   * Returns a vector of 
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}'s
   * listing fields and field informaton for the object type identified by 
   * id.
   *
   * @param id The id number of the object type to be returned the base id.
   */

  public Vector getTemplateVector(Short id)
  {
    return loader.getTemplateVector(id);
  }

  /**
   * Clears out the client's 
   * {@link arlut.csd.ganymede.client.objectCache objectCache},
   * which holds object labels, and activation status for invid's returned 
   * by various query and {@link arlut.csd.ganymede.rmi.db_field db_field} 
   * choices() operations.
   */

  public void clearCaches()
  {
    if (debug)
      {
        System.err.println("Clearing caches");
      }

    cachedLists.clearCaches();
  }

  /**
   * Gets a list of objects from the server, in
   * a form appropriate for use in constructing a list of nodes in the
   * tree under an object type (object base) folder.
   *
   * This method supports client-side caching.. if the list required
   * has already been retrieved, the cached list will be returned.  If
   * it hasn't, getObjectList() will get the list from the server and
   * save a local copy in an 
   * {@link arlut.csd.ganymede.client.objectCache objectCache}
   * for future requests.
   */

  public objectList getObjectList(Short id, boolean showAll)
  {
    objectList objectlist = null;

    /* -- */

    if (cachedLists.containsList(id))
      {
	if (debug)
	  {
	    System.err.println("gclient.getObjectList(" + id + ", " + showAll +
			       ") getting objectlist from the cachedLists.");
	  }

	objectlist = cachedLists.getList(id);

	// If we are being asked for a *complete* list of objects of
	// the given type and we only have editable objects of this
	// type cached, we may need to go back to the server to
	// get the full list.

	if (showAll && !objectlist.containsNonEditable())
	  {
	    if (debug)
	      {
		System.err.println("gclient.getObjectList(" + id + ", " + showAll +
				   ") objectList incomplete, downloading non-editables.");
	      }

	    try
	      {
		Query objQuery = new Query(id.shortValue(), null, false);
		objQuery.setFiltered(true);

		QueryResult qr = session.query(objQuery);
		
		if (qr != null)
		  {
		    if (debug)
		      {
			System.err.println("gclient.getObjectList(): augmenting");
		      }
		    
		    objectlist.augmentListWithNonEditables(qr);
		  }
	      }
	    catch (Exception rx)
	      {
		// "Could not do the query"
		processExceptionRethrow(rx, ts.l("getObjectList.query_exception"));
	      }
	    
	    cachedLists.putList(id, objectlist);
	  }
      }
    else
      {
	if (debug)
	  {
	    System.err.println("gclient.getObjectList(" + id + ", " + showAll +
			       ") downloading objectlist from the server.");
	  }

	try
	  {
	    Query objQuery = new Query(id.shortValue(), null, !showAll);
	    objQuery.setFiltered(true);

	    QueryResult qr = session.query(objQuery);

	    if (debug)
	      {
		System.err.println("gclient.getObjectList(): caching copy");
	      }
	    
	    objectlist = new objectList(qr);
	    cachedLists.putList(id, objectlist);
	  }
	catch (Exception rx)
	  {
	    // "Could not get dump"
	    processExceptionRethrow(rx, ts.l("getObjectList.dump_exception"));
	  }
      }

    return objectlist;
  }

  /**
   * Public accessor for the SecurityLaunderThread
   */

  public int getBuildPhase()
  {
    return buildPhase;
  }

  /**
   * By overriding update(), we can eliminate the annoying flash as
   * the default update() method clears the frame before rendering.
   */

  public void update(Graphics g)
  {
    paint(g);
  }

  /** 
   * Get the session
   */

  public final Session getSession()
  {
    return session;
  }

  /**
   * Loads and returns the error Image for use in client dialogs.
   * 
   * Once the image is loaded, it is cached for future calls to 
   * getErrorImage().
   */

  public final Image getErrorImage()
  {
    if (errorImage == null)
      {
	errorImage = PackageResources.getImageResource(this, "error.gif", getClass());
      }
    
    return errorImage;
  }

  /**
   * Loads and returns the question-mark Image for use in client dialogs.
   * 
   * Once the image is loaded, it is cached for future calls to 
   * getQuestionmage().
   */

  public final Image getQuestionImage()
  {
    if (questionImage == null)
      {
	questionImage = PackageResources.getImageResource(this, "question.gif", getClass());
      }
    
    return questionImage;
  }

  /**
   * Loads and returns the neutral 'Info' Image for use in client dialogs.
   * 
   * Once the image is loaded, it is cached for future calls to 
   * getInfoImage().
   */

  public final Image getInfoImage()
  {
    if (infoImage == null)
      {
	infoImage = PackageResources.getImageResource(this, "ok.gif", getClass());
      }
    
    return infoImage;
  }

  /**
   * Returns a hash mapping {@link arlut.csd.ganymede.common.BaseDump BaseDump}
   * references to their title.
   *
   * Checks to see if the baseNames was loaded, and if not, it loads it.
   * Always use this instead of trying to access baseNames directly.
   */

  public final Hashtable getBaseNames()
  {
    return loader.getBaseNames();
  }

  /**
   * Returns a Vector of {@link arlut.csd.ganymede.common.BaseDump BaseDump} objects,
   * providing a local cache of {@link arlut.csd.ganymede.rmi.Base Base}
   * references that the client consults during operations.
   *
   * Checks to see if the baseList was loaded, and if not, it loads it.
   * Always use this instead of trying to access the baseList
   * directly.
   */

  public final synchronized Vector getBaseList()
  {
    return loader.getBaseList();
  }

  /**
   * Returns a hash mapping Short {@link arlut.csd.ganymede.rmi.Base Base} id's to
   * {@link arlut.csd.ganymede.common.BaseDump BaseDump} objects.
   *
   * Checks to see if the baseMap was loaded, and if not, it loads it.
   * Always use this instead of trying to access the baseMap
   * directly.
   */

  public Hashtable getBaseMap()
  {
    return loader.getBaseMap();
  }

  /**
   * Returns a hashtable mapping {@link arlut.csd.ganymede.common.BaseDump BaseDump}
   * references to their object type id in Short form.  This is
   * a holdover from a time when the client didn't create local copies
   * of the server's Base references.
   *
   * Checks to see if the basetoShort was loaded, and if not, it loads it.
   * Always use this instead of trying to access the baseToShort
   * directly.
   */

  public Hashtable getBaseToShort()
  {
    return loader.getBaseToShort();
  }

  /**
   * Returns the type name for a given object.
   *
   * If the loader thread hasn't yet downloaded that information, this
   * method will block until the information is available.
   */

  public String getObjectType(Invid objId)
  {
    return loader.getObjectType(objId);
  }

  /**
   * This method returns a concatenated string made up of the object
   * type and object name.  This string is localized in the
   * gclient.properties file, and should be structured in a fashion
   * suitable for inclusion in gclient message strings with their own
   * localizations.
   *
   * Both the client and the server cache object information until a
   * transaction deleting the object is committed, so it is explicitly
   * legal to call getObjectDesignation() on Invids for objects that are
   * being deleted in the current transaction.
   */

  public String getObjectDesignation(Invid objId)
  {
    ObjectHandle h = getObjectHandle(objId, null);
    // "{0} {1}"
    return ts.l("getObjectDesignation.combine_str", getObjectType(objId), h.getLabel());
  }

  /**
   * Pulls a object handle for an invid out of the
   * client's cache, if it has been cached.
   *
   * If no handle for this invid has been cached, this method
   * will attempt to retrieve one from the server.
   */

  public ObjectHandle getObjectHandle(Invid invid)
  {
    return this.getObjectHandle(invid, null);
  }

  /**
   * Pulls a object handle for an invid out of the
   * client's cache, if it has been cached.
   *
   * If no handle for this invid has been cached, this method will
   * attempt to retrieve one from the server.
   *
   * The Short type parameter is just a micro-optimizing
   * convenience for code that already has such a Short
   * constructed.  This method will work perfectly well if
   * the type parameter is null.
   */

  public ObjectHandle getObjectHandle(Invid invid, Short type)
  {
    ObjectHandle handle = null;

    if (type == null)
      {
	type = Short.valueOf(invid.getType());
      }

    if (cachedLists.containsList(type))
      {
	handle = cachedLists.getInvidHandle(type, invid);

	if (handle != null)
	  {
	    return handle;
	  }
      }

    // okay, we haven't found it.  try to pull this invid down from the
    // server, and cache it

    Vector paramVec = new Vector();

    paramVec.addElement(invid);

    try
      {
	QueryResult result = session.queryInvids(paramVec);

	Vector handleList = result.getHandles();

	if (handleList.size() > 0)
	  {
	    handle = (ObjectHandle) handleList.elementAt(0);
	  }
      }
    catch (Exception ex)
      {
	processExceptionRethrow(ex);
      }

    return handle;
  }

  /**
   * This method is called to update the client's display of the
   * number of users concurrently logged into the server.
   *
   * @param status The number of users logged into the Ganymede server
   */

  public final void setLoginCount(int loginCount)
  {
    if (loginCount == 1)
      {
        loginLabel.setText(""); // we're the only users, we don't need a message
      }
    else
      {
        // "{0,number,#} users logged in."
        loginLabel.setText(ts.l("setLoginCount.multi_login", Integer.valueOf(loginCount)));
      }
  }

  /**
   * Sets the text that will appear in the status bar when no other
   * status is being displayed.
   *
   * @param status The text to display
   */

  public final void setDefaultStatus(String status)
  {
    statusThread.setDefaultMessage(status);
  }

  /**
   * Sets text in the status bar, with a 5 second countdown before
   * the status bar is cleared.
   *
   * @param status The text to display
   */

  public final void setStatus(String status)
  {
    setStatus(status, 5);
  }

  /**
   * Sets text in the status bar, with a defined countdown before
   * the status bar is cleared.
   *
   * @param status The text to display
   * @param timeToLive Number of seconds to wait until clearing the status bar.
   * If zero or negative, the status bar timer will not clear the field until
   * the status bar is changed by another call to setStatus.
   */

  public final void setStatus(String status, int timeToLive)
  {
    if (debug)
      {
	System.err.println("Setting status: " + status);
      }

    final String fStatus = status;

    // use EventQueue.invokeLater so that we play nice
    // with the Java display thread
    
    EventQueue.invokeLater(new Runnable() {
      public void run() {
	statusLabel.setText(fStatus);
	statusLabel.paintImmediately(statusLabel.getVisibleRect());
      }
    });

    statusThread.setClock(timeToLive);
  }

  /**
   * This method is triggered by the Ganymede server if the client
   * is idle long enough.  This method will downgrade the user's
   * login to a minimum privilege level if possible, requiring
   * the user to enter their admin password again to regain
   * admin privileges.
   */

  public final void softTimeout()
  {
    // we use invokeLater so that we free up the RMI thread
    // which messaged us, and so we play nice with the Java
    // display thread
    
    EventQueue.invokeLater(new Runnable() {
      public void run() {
	personaListener.softTimeOutHandler();
      }
    });
  }

  /**
   * Updates the status icon, based on an enumerated list of strings
   * that can be provided from the server.  These are "idle",
   * "building", and "building2".  The "building" state applies when
   * the server is currently running builderPhase1.  That is, when the
   * server is (at least partially) locked while it dumps out data
   * files.
   *
   * The "building2" phase is in effect when the server is unlocked
   * and has one or more threads waiting for the completion of
   * external build scripts.
   *
   * @param status The text to key off of.
   */

  public final void setBuildStatus(String status)
  {
    if (debug)
      {
	System.err.println("Setting build status: " + status);
      }

    if (status.equals("idle"))
      {
	buildPhase = 0;
      }
    else if (status.equals("building"))
      {
	buildPhase = 1;
      }
    else if (status.equals("building2"))
      {
	buildPhase = 2;
      }
    else
      {
	buildPhase = -1;
      }

    try
      {
	securityThread.setBuildStatus(buildPhase);
      }
    catch (NullPointerException ex)
      {
      }
  }

  /**
   * Returns the node of the object currently selected in the tree, if
   * any.  Returns null if there are no nodes selected in the tree, of
   * if the node selected in the tree is not an object node.
   */

  public InvidNode getSelectedObjectNode()
  {
    // get our own copy of the current node so
    // that we don't get tripped up by threading

    treeNode myNode = selectedNode;

    if ((myNode == null) ||
	!(myNode instanceof InvidNode))
      {
	return null;
      }
    else
      {
	return (InvidNode) myNode;
      }
  }

  /**
   * Get the current text from the client's status field
   */

  public String getStatus()
  {
    return statusLabel.getText();
  }
  
  /**
   * Show the help window.
   *
   * This might someday take an argument, which would show a starting page
   * or some more specific help.
   */
   
  public void showHelpWindow()
  {
    if (help == null)
      {
	help = new helpPanel(this);
      }
    else
      {
	help.setVisible(true);
      }
  }

  /**
   * Shows the Java Version dialog.
   */

  public void showJavaVersion()
  {
    if (java_ver_dialog == null)
      {
	// "Java Version"
	java_ver_dialog = new aboutJavaDialog(this, ts.l("showJavaVersion.dialog_title"));
      }

    java_ver_dialog.setVisible(true);
  }

  /**
   * Shows the About... dialog.
   */

  public void showAboutMessage()
  {
    if (about == null)
      {
	// "About Ganymede"
	about = new aboutGanyDialog(this, ts.l("showAboutMessage.dialog_title"));
      }
    else
      {
	// "About Ganymede"
	about.setTitle(ts.l("showAboutMessage.dialog_title"));
      }

    about.setVisible(true);
  }

  /**
   * Shows the server's message of the day in a dialog.
   */

  public void showMOTD()
  {
    // This will always get the MOTD, even if we've seen it

    StringBuffer m;

    try
      {
	m = session.getMessageHTML("motd", false);

	if (m == null)
	  {
	    m = session.getMessage("motd", false);

	    if (m != null)
	      {
		showMOTD(m.toString(), false);
	      }
	  }
	else
	  {
	    showMOTD(m.toString(), true);
	  }
      }
    catch (Exception rx)
      {
	processExceptionRethrow(rx, "Could not get motd");
      }
  }

  /**
   * This method generates a message-of-the-day dialog.
   *
   * @param message The message to display.  May be multiline.  
   * @param html If true, showMOTD() will display the motd with a html
   * renderer, in Swing 1.1b2 and later.
   */

  public void showMOTD(String message, boolean html)
  {
    if (motd == null)
      {
	// "MOTD"
	motd = new messageDialog(client, ts.l("showMOTD.dialog_title"), null);
      }

    if (html)
      {
	motd.setHtmlText(message);
      }
    else
      {
	motd.setPlainText(message);
      }

    motd.setVisible(true);
  }

  /**
   * This method is used to display an error dialog for the given exception,
   * and to rethrow it as a RuntimeException.
   *
   * Potentially useful when catching RemoteExceptions from the server.
   */

  public final void processExceptionRethrow(Throwable ex)
  {
    processException(ex);

    throw new RuntimeException(ex);
  }

  /**
   * This method is used to display an error dialog for the given exception,
   * and to rethrow it as a RuntimeException.
   *
   * Potentially useful when catching RemoteExceptions from the server.
   */

  public final void processExceptionRethrow(Throwable ex, String message)
  {
    processException(ex, message);

    throw new RuntimeException(ex);
  }

  /**
   * This method is used to display an error dialog for the given
   * exception.
   */

  public final void processException(Throwable ex)
  {
    processException(ex, null);
  }

  /**
   * This method is used to display an error dialog for the given
   * exception.
   */

  public final void processException(Throwable ex, String message)
  {
    // make sure we're not processing an exception that has been
    // rethrown by processExceptionRethrow..

    boolean foundRethrow = false;

    StackTraceElement[] frames = ex.getStackTrace();

    for (int i = 0; !foundRethrow && i < frames.length; i++)
      {
	StackTraceElement frame = frames[i];

	if (frame.getMethodName().equals("processExceptionRethrow") &&
	    frame.getClassName().endsWith("gclient"))
	  {
	    foundRethrow = true;
	  }
      }

    if (foundRethrow)
      {
	return;
      }

    StringWriter stringTarget = new StringWriter();
    PrintWriter writer = new PrintWriter(stringTarget);
    
    ex.printStackTrace(writer);
    writer.close();

    if (ex instanceof NotLoggedInException)
      {
	showNotLoggedIn();
      }
    else
      {
	if (ex instanceof RegexpException)
	  {
	    // don't bother showing them the stack trace (or offering
	    // to transmit the error to the server) if they entered a
	    // bad regexp into a dialog

	    showErrorMessage(ex.getMessage());
	  }
	else
	  {
	    String text;

	    if (message != null)
	      {
		text = message + "\n" + stringTarget.toString();
	      }
	    else
	      {
		text = stringTarget.toString();
	      }

	    // "Exception"
	    showExceptionMessage(ts.l("processException.exception"),
				 text);
	  }
      }

    setNormalCursor();
  }

  /** 
   * Show an Exception dialog, and offer to transmit the error message
   * to the server.  By default, the icon displayed will be the
   * standard Ganymede error icon.
   *
   * @param title title of dialog.
   * @param message Text of dialog.
   */

  public final void showExceptionMessage(String title, String message)
  {
    this.showExceptionMessage(title, message, getErrorImage());
  }

  /** 
   * Show an Exception dialog, and offer to transmit the error message to
   * the server.
   *
   * @param title title of dialog.
   * @param message Text of dialog.
   * @param icon optional icon to display.
   */

  public final void showExceptionMessage(String title, String message, Image icon)
  {
    if (debug)
      {
	System.err.println("Error message: " + message);
      }

    final gclient gc = this;
    final String Title = title;
    final String Message = message;
    final Image fIcon = icon;

    EventQueue.invokeLater(new Runnable() 
			       {
				 public void run()
				   {
				     ExceptionDialog x = new ExceptionDialog(gc, Title, Message, fIcon); // implicit show

				     if (x.didRequestReport())
				       {
					 boolean success = false;

					 try
					   {
					     session.reportClientBug(aboutJavaDialog.getVersionInfoString(), Message);
					     success = true;
					   }
					 catch (Throwable ex)
					   {
					     // ignore
					   }

					 if (success)
					   {
					     // "Exception Reported"
					     // "This possible error
					     // condition has been
					     // reported to the
					     // Ganymede
					     // server.\n\nThank you!"

					     new JErrorDialog(gc,
							      ts.l("showExceptionMessage.exception_reported"),
							      ts.l("showExceptionMessage.thank_you"),
							      getInfoImage()); // implicit show
					   }
					 else
					   {
					     // "Failure Reporting Exception"
					     // "This error condition
					     // could not be reported
					     // successfully to the
					     // server.  Perhaps the
					     // server or your network
					     // has gone down?"

					     new JErrorDialog(gc,
							      ts.l("showExceptionMessage.failure_reporting"),
							      ts.l("showExceptionMessage.failure_explanation"),
							      getErrorImage()); // implicit show
					   }
				       }
				   }
			       });

    setStatus(title + ": " + message, 10);
  }

  /**
   * Display a message explaining that the user is no longer logged in
   */

  public final void showNotLoggedIn()
  {
    // "Error"
    // "Not logged in to the server"
    showErrorMessage(ts.l("global.error"),
		     ts.l("showNotLoggedIn.not_logged_in"));
  }

  /**
   * Pops up an error dialog with the default title.
   */

  public final void showErrorMessage(String message)
  {
    // "Error"
    showErrorMessage(ts.l("global.error"), message);
  }

  /**
   * Pops up an error dialog.  Pre-defines the icon for the dialog as
   * the standard Ganymede error icon.
   */

  public final void showErrorMessage(String title, String message)
  {
    showErrorMessage(title, message, getErrorImage());
  }

  /** 
   * Show an error dialog.
   *
   * @param title title of dialog.
   * @param message Text of dialog.
   * @param icon optional icon to display.
   */

  public final void showErrorMessage(String title, String message, Image icon)
  {
    if (debug)
      {
	System.err.println("Error message: " + message);
      }

    final gclient gc = this;
    final String Title = title;
    final String Message = message;
    final Image fIcon = icon;

    EventQueue.invokeLater(new Runnable() 
			       {
				 public void run()
				   {
				     new JErrorDialog(gc, Title, Message, fIcon); // implicit show
				   }
			       });

    setStatus(title + ": " + message, 10);
  }

  /**
   * Set the cursor to a wait cursor(usually a watch.)
   */

  public void setWaitCursor()
  {
    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }

  /**
   * Set the cursor to the normal cursor(usually a pointer.)
   *
   * This is dependent on the operating system.
   */

  public void setNormalCursor()
  {
    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  /**
   * This indeicates that something in the database was changed, so
   * cancelling this transaction will have consequences.
   *
   * This should be called whenever the client makes any changes to
   * the database.  That includes creating objects, editting fields of
   * objects, removing objects, renaming, expiring, deleting,
   * inactivating, and so on.  It is very important to call this
   * whenever something might have changed.  
   */

  public final void somethingChanged()
  {
    commit.setEnabled(true);
    cancel.setEnabled(true);
    setSomethingChanged(true);
  }

  /**
   * Sets or clears the client's somethingChanged flag.
   */
  private void setSomethingChanged(boolean state)
  {
    if (debug)
      {
	System.err.println("Setting somethingChanged to " + state);
      }

    somethingChanged = state;
  }

  /**
   * True if something has been changed since the last commit/cancel
   *
   */

  public boolean getSomethingChanged()
  {
    return somethingChanged;
  }

  /**
   * True if we are in an applet context, meaning we don't have access
   * to local files, etc.
   */

  public boolean isApplet()
  {
    return _myglogin.isApplet();
  }

  /**
   * This method takes a ReturnVal object from the server and, if
   * necessary, runs through a wizard interaction sequence, possibly
   * displaying several dialogs before finally returning a final
   * result code.
   *
   * Use the ReturnVal returned from this function after this
   * function is called to determine the ultimate success or failure
   * of any operation which returns ReturnVal, because a wizard
   * sequence may determine the ultimate result.
   *
   * This method should not be synchronized, since handleReturnVal
   * may pop up modal (thread-blocking) dialogs, and if we we
   * synchronize this, some Swing or AWT code seems to block on our
   * synchronization when we do pop-up dialogs.  It's not any of my
   * code, so I assume that AWT tries to synchronize on the frame when
   * parenting a new dialog. 
   */

  public ReturnVal handleReturnVal(ReturnVal retVal)
  {
    Hashtable dialogResults;

    /* -- */

    if (debug)
      {
	System.err.println("gclient.handleReturnVal(): Entering");
      }

    wizardActive++;

    try
      {
	while ((retVal != null) && (retVal.getDialog() != null))
	  {
	    if (debug)
	      {
		System.err.println("gclient.handleReturnVal(): retrieving dialog");
	      }

	    JDialogBuff jdialog = retVal.getDialog();

	    if (debug)
	      {
		System.err.println("gclient.handleReturnVal(): extracting dialog");
	      }

	    DialogRsrc resource = jdialog.extractDialogRsrc(this, null);

	    if (debug)
	      {
		System.err.println("gclient.handleReturnVal(): constructing dialog");
	      }

	    StringDialog dialog = new StringDialog(resource);

	    if (debug)
	      {
		System.err.println("gclient.handleReturnVal(): displaying dialog");
	      }

	    setWaitCursor();

	    try
	      {
		// display the Dialog sent to us by the server, get the
		// result of the user's interaction with it.
	    
		dialogResults = dialog.showDialog();
	      }
	    finally
	      {
		setNormalCursor();
	      }

	    if (debug)
	      {
		System.err.println("gclient.handleReturnVal(): dialog done");
	      }

	    if (retVal.getCallback() != null)
	      {
		try
		  {
		    if (debug)
		      {
			System.err.println("gclient.handleReturnVal(): Sending result to callback: " + dialogResults);
		      }

		    // send the dialog results to the server

		    retVal = retVal.getCallback().respond(dialogResults);

		    if (debug)
		      {
			System.err.println("gclient.handleReturnVal(): Received result from callback.");
		      }
		  }
		catch (Exception ex)
		  {
		    processExceptionRethrow(ex);
		  }
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("gclient.handleReturnVal(): No callback, breaking");
		  }

		break;		// we're done
	      }
	  }
      }
    finally
      {
	wizardActive--;
      }
    
    if (debug)
      {
	System.err.println("gclient.handleReturnVal(): Done with wizards, checking retVal for rescan.");
      }

    // Check for objects that need to be rescanned

    if (retVal != null && retVal.objectLabelChanged())
      {
	wp.relabelObject(retVal.getInvid(), retVal.getNewLabel());
      }

    if (retVal == null || !retVal.doRescan())
      {
	return retVal;
      }
	
    if (debug)
      {
	System.err.println("gclient.handleReturnVal(): rescan dump: " + retVal.dumpRescanInfo());
      }

    Vector objects = retVal.getRescanObjectsList();
	    
    if (objects == null)
      {
	if (debug)
	  {
	    System.err.println("gclient.handleReturnVal(): Odd, was told to rescan, but there's nothing there!");
	  }

	return retVal;
      }

    if (debug)
      {
	System.err.println("gclient.handleReturnVal(): Rescanning " + objects.size() + " objects.");
      }
    
    Enumeration invids = objects.elements();

    // Loop over all the invids, and try to find
    // containerPanels for them.
    
    while (invids.hasMoreElements())
      {
	Invid invid = (Invid) invids.nextElement();

	if (debug)
	  {
	    System.err.println("gclient.handleReturnVal(): updating invid: " + invid);
	  }

	wp.refreshObjectWindows(invid, retVal);
      }

    if (debug)
      {
	System.err.println("gclient.handleReturnVal(): Exiting handleReturnVal");
      }

    return retVal;
  }

  // Private methods

  /**
   * Creates and initializes the client's toolbar.
   */

  JToolBar createToolbar()
  {
    Insets insets = new Insets(0,0,0,0);
    JToolBar toolBarTemp = new JToolBar();

    toolBarTemp.setBorderPainted(true);
    toolBarTemp.setFloatable(true);
    toolBarTemp.setMargin(insets);

    // "Create"
    JButton b = new JButton(ts.l("createToolbar.create_button"), new ImageIcon(newToolbarIcon));
    b.setMargin(insets);
    b.setActionCommand(create_action);
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    // "Create a new object"
    b.setToolTipText(ts.l("createToolbar.create_tooltip"));
    b.addActionListener(this);
    toolBarTemp.add(b);

    // "Edit"
    b = new JButton(ts.l("createToolbar.edit_button"), new ImageIcon(pencil));
    b.setMargin(insets);
    b.setActionCommand(edit_action);
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    // "Edit an object"
    b.setToolTipText(ts.l("createToolbar.edit_tooltip"));
    b.addActionListener(this);
    toolBarTemp.add(b);

    // "Delete"
    b = new JButton(ts.l("createToolbar.delete_button"), new ImageIcon(trash));
    b.setMargin(insets);
    b.setActionCommand(delete_action);
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    // "Delete an object"
    b.setToolTipText(ts.l("createToolbar.delete_tooltip"));
    b.addActionListener(this);
    toolBarTemp.add(b);

    // "Clone"
    b = new JButton(ts.l("createToolbar.clone_button"), new ImageIcon(cloneIcon));
    b.setMargin(insets);
    b.setActionCommand(clone_action);
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    // "Clone an object"
    b.setToolTipText(ts.l("createToolbar.clone_tooltip"));
    b.addActionListener(this);
    toolBarTemp.add(b);

    // "View"
    b = new JButton(ts.l("createToolbar.view_button"), new ImageIcon(search));
    b.setMargin(insets);
    b.setActionCommand(view_action);
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    // "View an object"
    b.setToolTipText(ts.l("createToolbar.view_tooltip"));
    b.addActionListener(this);
    toolBarTemp.add(b);

    // "Query"
    b = new JButton(ts.l("createToolbar.query_button"), new ImageIcon(queryIcon));
    b.setMargin(insets);
    b.setActionCommand(query_action);
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    // "Compose a query"
    b.setToolTipText(ts.l("createToolbar.query_tooltip"));
    b.addActionListener(this);
    toolBarTemp.add(b);

    // If we decide to have an inactivate-type button on toolbar...
//     b = new JButton("Inactivate", new ImageIcon(inactivateIcon));
//     //b = new JButton(new ImageIcon(inactivateIcon));
//     b.setMargin(insets);
//     b.setActionCommand("inactivate an object");
//     b.setVerticalTextPosition(b.BOTTOM);
//     b.setHorizontalTextPosition(b.CENTER);
//     b.setToolTipText("Inactivate an object");
//     b.addActionListener(this);
//     toolBarTemp.add(b);

    if ((personae != null)  && personae.size() > 1)
      {
	// "Persona"
	b = new JButton(ts.l("createToolbar.persona_button"), new ImageIcon(personaIcon));  
	b.setMargin(insets);
	b.setActionCommand(persona_action);
	b.setVerticalTextPosition(b.BOTTOM);
	b.setHorizontalTextPosition(b.CENTER);
	// "Change Persona"
	b.setToolTipText(ts.l("createToolbar.persona_tooltip"));
	b.addActionListener(this);
	toolBarTemp.add(b);
      }
   
    return toolBarTemp;
  }


  ///////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////
  ////            Tree Stuff
  ////
  ///////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Clears out the client's tree.
   *
   * All Nodes will be removed, and the Category and BaseNodes will
   * be rebuilt.  No InvidNodes will be added.
   */

  void clearTree()
  {
    tree.clearTree();

    try
      {
	buildTree();
      }
    catch (Exception rx)
      {
	// "Could not rebuild tree"
	processExceptionRethrow(rx, ts.l("clearTree.exception"));
      }
  }

  /**
   * This method builds the initial data structures for the object
   * selection tree, using the base information in the baseHash
   * hashtable gained from the {@link arlut.csd.ganymede.client.Loader Loader}
   * thread.
   */

  void buildTree() throws RemoteException
  {
    if (debug)
      {
	System.err.println("gclient.buildTree(): Building tree");
      }

    // clear the invidNodeHash

    invidNodeHash.clear();

    CategoryTransport transport = session.getCategoryTree(hideNonEditables);

    // get the category dump, save it

    dump = transport.getTree();

    if (debug)
      {
	System.err.println("gclient.buildTree(): got root category: " + dump.getName());
      }

    recurseDownCategories(null, dump);

    if (debug)
      {
	System.err.println("gclient.buildTree(): Refreshing tree");
      }

    tree.refresh();

    if (debug)
      {
	System.err.println("gclient.buildTree(): Done building tree,");
      }
  }

  /**
   * Recurses down the category tree obtained from the server, loading
   * the client's tree with category and object folder nodes.
   */

  void recurseDownCategories(CatTreeNode node, Category c) throws RemoteException
  {
    Vector
      children;

    CategoryNode cNode;

    treeNode 
      prevNode;

    /* -- */
      
    children = c.getNodes();

    prevNode = null;

    for (int i = 0; i < children.size(); i++)
      {
	// find the CategoryNode at this point in the server's category tree

	cNode = (CategoryNode) children.elementAt(i);

	if (cNode instanceof Base)
	  {
	    Base base = (Base) cNode;
	    
	    if (base.isEmbedded())
	      {
		continue;	// we don't want to present embedded objects
	      }
	  }

	// if we have a single category at this level, we don't want
	// to bodily insert it into the tree.. we'll just continue to
	// recurse down with it.

	if ((cNode instanceof Category) && (children.size() == 1))
	  {
	    recurseDownCategories(node, (Category) cNode);
	  }
	else
	  {
	    prevNode = insertCategoryNode(cNode, prevNode, node);

	    if (prevNode instanceof CatTreeNode)
	      {
		recurseDownCategories((CatTreeNode)prevNode, (Category) cNode);
	      }
	  }
      }
  }

  /**
   * Helper method for building tree
   */

  treeNode insertCategoryNode(CategoryNode node, treeNode prevNode, treeNode parentNode) throws RemoteException
  {
    treeNode newNode = null;
      
    if (node instanceof Base)
      {
	Base base = (Base)node;
	boolean canCreate = base.canCreate(getSession());

	newNode = new BaseNode(parentNode, base.getName(), base, prevNode,
			       true, 
			       OPEN_BASE, 
			       CLOSED_BASE,
			       canCreate ? pMenuEditableCreatable : pMenuEditable,
			       canCreate);

	((BaseNode) newNode).showAll(!hideNonEditables);

	shortToBaseNodeHash.put(((BaseNode)newNode).getTypeID(), newNode);
      }
    else if (node instanceof Category)
      {
	Category category = (Category)node;
	newNode = new CatTreeNode(parentNode, category.getName(), category,
				  prevNode, true, 
				  OPEN_CAT, 
				  CLOSED_CAT, 
				  null);
      }
    else
      {
        // for FindBugs.  Shouldn't happen due to tree structure
	throw new RuntimeException("gclient.insertCategoryNode(): Unknown instance: " + node);
      }

    if ((newNode.getParent() == null) && (newNode.getPrevSibling() == null))
      {
	tree.setRoot(newNode);
      }
    else
      {
	tree.insertNode(newNode, false);
      }
    
    return newNode;
  }

  /**
   * This method is used to update the list of object nodes under a given
   * base node in our object selection tree, synchronizing the tree with
   * the actual objects on the server.
   *
   * @param node Tree node corresponding to the object type being refreshed
   * in the client's tree.
   * @param doRefresh If true, causes the tree to update its display.
   */
  
  void refreshObjects(BaseNode node, boolean doRefresh) throws RemoteException
  {
    Invid invid = null;
    String label = null;
    InvidNode oldNode, newNode, fNode;

    ObjectHandle handle = null;
    Vector objectHandles;
    objectList objectlist = null;

    Short Id;

    /* -- */

    Id = node.getTypeID();

    // get the object list.. this call will automatically handle
    // caching for us.

    objectlist = getObjectList(Id, node.isShowAll());

    objectHandles = objectlist.getObjectHandles(true, node.isShowAll()); // include inactives

    // **
    //
    // The loop below goes over the sorted list of objectHandles and
    // the sorted list of nodes in the tree under this particular baseNode,
    // comparing as the loop progresses, adding or removing nodes from the
    // tree to match the current contents of the objectHandles list
    //
    // The important variables in the loop are fNode, which points to the
    // current node in the subtree that we're examining, and i, which
    // is counting our way through the objectHandles Vector.
    // 
    // **

    oldNode = null;
    fNode = (InvidNode) node.getChild();
    int i = 0;
	
    while ((i < objectHandles.size()) || (fNode != null))
      {
	if (i < objectHandles.size())
	  {
	    handle = (ObjectHandle) objectHandles.elementAt(i);

	    if (!node.isShowAll() && !handle.isEditable())
	      {
		i++;		// skip this one, we're not showing non-editables
		continue;
	      }

	    invid = handle.getInvid();
	    label = handle.getLabel();
	  }
	else
	  {
	    // We've gone past the end of the list of objects in this
	    // object list.. from here on out, we're going to wind up
	    // removing anything we find in this subtree

	    handle = null;
	    label = null;
	    invid = null;
	  }

	// insert a new node in the tree, change the label, or remove
	// a node

	if ((fNode == null) ||
	    ((invid != null) && 
	     ((label.compareToIgnoreCase(fNode.getText())) < 0)))
	  {
	    // If we have an invid/label in the object list that's not
	    // in the tree, we need to insert it

	    // "{0} (inactive)"

	    InvidNode objNode = new InvidNode(node, 
					      handle.isInactive() ? ts.l("global.inactive_pattern", label):label,
					      invid,
					      oldNode, false,
					      handle.isEditable() ? OPEN_FIELD : OBJECTNOWRITE,
					      handle.isEditable() ? CLOSED_FIELD : OBJECTNOWRITE,
					      handle.isEditable() ? (node.canInactivate()
								     ? objectInactivatePM : objectRemovePM) : objectViewPM,
					       
					      handle);
	    
	    if (invid != null)
	      {
		invidNodeHash.put(invid, objNode);

		if (createdObjectsWithoutNodes.containsKey(invid))
		  {
		    if (false)
		      {
			System.err.println("Found this object in the creating objectsWithoutNodes hash: " + 
					   handle.getLabel());
		      }

		    // "New Object"
		    createHash.put(invid, new CacheInfo(node.getTypeID(),
							(handle.getLabel() == null) ? ts.l("global.new_object") : handle.getLabel(),
							null, handle));
		    createdObjectsWithoutNodes.remove(invid);
		  }
		
		setIconForNode(invid);
	      }

	    tree.insertNode(objNode, false);

	    oldNode = objNode;
	    fNode = (InvidNode) oldNode.getNextSibling();
	    
	    i++;
	  }
	else if ((invid == null) ||
		 ((label.compareToIgnoreCase(fNode.getText())) > 0))
	  {
	    // We've found a node in the tree without a matching
	    // node in the object list.  Delete it!

	    newNode = (InvidNode) fNode.getNextSibling();
	    tree.deleteNode(fNode, false);

	    fNode = newNode;
	  }
	else if (fNode.getInvid().equals(invid))
	  {
	    // we've got a node in the tree that matches the
	    // invid of the current object in the object list,
	    // but the label may possibly have changed, so we'll
	    // go ahead and re-set the label, just to be sure

	    if (handle.isInactive())
	      {
		// {0} (inactive)
		fNode.setText(ts.l("global.inactive_pattern", label));
	      }
	    else
	      {
		fNode.setText(label);
	      }

	    oldNode = fNode;
	    fNode = (InvidNode) oldNode.getNextSibling();

	    setIconForNode(invid);

	    i++;
	  }
      }

    if (doRefresh)
      {
	tree.refresh();
      }
  }

  /**
   * Updates the tree for the nodes that might have changed.
   *
   * This method fixes all the icons, removing icons that were
   * marked as to-be-deleted or dropped, and cleans out the various
   * hashes.  Only call this when commit is clicked.  This replaces
   * refreshTree(boolean committed), because all the refreshing to be
   * done after a cancel is now handled in the cancelTransaction()
   * method directly.
   *
   * This method is precisely analagous in function to
   * {@link arlut.csd.ganymede.client.gclient#cleanUpAfterCancel() cleanUpAfterCancel()},
   * except for use after a commit. 
   */

  void refreshTreeAfterCommit() throws RemoteException
  {
    Invid invid = null;
    InvidNode node = null;

    /* -- */

    //
    // First get rid of deleted nodes
    //

    synchronized (deleteHash)
      {
	Enumeration deleted = deleteHash.keys();

	while (deleted.hasMoreElements())
	  {
	    invid = (Invid)deleted.nextElement();
	    node = (InvidNode)invidNodeHash.get(invid);
	    
	    if (node != null)
	      {
		if (debug)
		  {
		    System.err.println("gclient.refreshTreeAfterCommit(): Deleteing node: " + node.getText());
		  }
		
		tree.deleteNode(node, false);
		invidNodeHash.remove(invid);
	      }

            // and be sure we close any view windows held open that
            // show the deleted object

            wp.closeInvidWindows(invid);
	  }
    
	deleteHash.clear();
      }

    //
    // Now change the created nodes
    //

    invid = null;

    Vector changedInvids = new Vector();
    Enumeration created = createHash.keys();

    while (created.hasMoreElements())
      {
	invid = (Invid) created.nextElement();

	changedInvids.addElement(invid);
      }

    createHash.clear();
    createdObjectsWithoutNodes.clear();

    invid = null;
    Enumeration changed = changedHash.keys();
    
    while (changed.hasMoreElements())
      {
	invid = (Invid) changed.nextElement();

	changedInvids.addElement(invid);
      }

    changedHash.clear();

    if (changedInvids.size() > 0)
      {
	if (debug)
	  {
	    System.err.println("gclient.refreshTreeAfterCommit(): refreshing created objects");
	  }

	refreshChangedObjectHandles(changedInvids, true);

	if (debug)
	  {
	    System.err.println("gclient.refreshTreeAfterCommit(): done refreshing created objects");
	  }
      }

    tree.refresh();
  }

  /**
   * Queries the server for status information on a vector of 
   * {@link arlut.csd.ganymede.common.Invid invid}'s that were touched
   * in some way by the client during the recent transaction.
   * The results from the queries are used to update the icons
   * in the tree.
   *
   * Called by refreshTreeAfterCommit().
   *
   * This method is called from
   * {@link arlut.csd.ganymede.client.gclient#refreshTreeAfterCommit() refreshTreeAfterCommit()}.
   *
   * @param paramVect Vector of invid's to refresh.  
   * @param afterCommit If true, this method will update the client's status
   * bar as it progresses.
   */

  public void refreshChangedObjectHandles(Vector paramVect, boolean afterCommit)
  {
    Invid invid;
    Short objectTypeKey = null;

    /* -- */

    try
      {
	QueryResult result = session.queryInvids(paramVect);

	// now get the results
	    
	Vector handleList = result.getHandles();
	    
	// and update anything we've got in the tree
	
	for (int i = 0; i < handleList.size(); i++)
	  {
	    ObjectHandle newHandle = (ObjectHandle) handleList.elementAt(i);
	    invid = newHandle.getInvid();

	    objectTypeKey = Short.valueOf(invid.getType());

	    InvidNode nodeToUpdate = (InvidNode) invidNodeHash.get(invid);
		
	    if (nodeToUpdate != null)
	      {
		if (debug)
		  {
		    System.err.println("gclient.refreshChangedObjectHandles(): got object handle refresh for " + 
				       newHandle.debugDump());
		  }
		
		nodeToUpdate.setHandle(newHandle);
		    
		if (paramVect == null)
		  {
		    changedHash.remove(newHandle.getInvid());
		  }

		setIconForNode(newHandle.getInvid());
	      }
	    else if (debug)
	      {
		System.err.println("gclient.refreshChangedObjectHandles(): null node for " + 
				   newHandle.debugDump());
	      }
	    
	    // and update our tree cache for this item

	    objectList list = cachedLists.getList(objectTypeKey);

	    if (list != null)
	      {
		list.removeInvid(newHandle.getInvid());
		list.addObjectHandle(newHandle);
	      }
	  }
      }
    catch (Exception ex)
      {
        // "Couldn''t refresh object tree"
	processExceptionRethrow(ex, ts.l("refreshChangedObjectHandles.exception"));
      }
  }

  /**
   * This method does the same thing as refreshChangedObjectHandles(), but
   * for a single object only.
   */

  public void refreshChangedObject(Invid invid)
  {
    Vector paramVec = new Vector();

    paramVec.addElement(invid);

    refreshChangedObjectHandles(paramVec, false);
  }

  /**
   * Updates a database object's icon in the tree display.  This method
   * uses the various client-side caches and hashes to determine the proper
   * icon for the node.
   *
   * This method does not actually induce the tree to refresh itself,
   * and may be called in bulk for a lot of nodes efficiently.
   */

  public void setIconForNode(Invid invid)
  {
    final boolean treeNodeDebug = false;

    InvidNode node = (InvidNode) invidNodeHash.get(invid);

    if (node == null)
      {
	return;
      }

    ObjectHandle handle = node.getHandle();

    // if we can't edit it, assume it'll never be anything other
    // than inaccessible

    if (!handle.isEditable())
      {
        node.setImages(OBJECTNOWRITE, OBJECTNOWRITE);
        node.setMenu(objectViewPM);
        return;
      }

    // The order here matters, because it might be in more than
    // one hash.  So put the most important stuff first

    if (deleteHash.containsKey(invid))
      {
        if (treeNodeDebug)
          {
            System.err.println("Setting icon to delete.");
          }

        node.setImages(OPEN_FIELD_DELETE, CLOSED_FIELD_DELETE);
      }
    else if (createHash.containsKey(invid))
      {
        if (treeNodeDebug)
          {
            System.err.println("Setting icon to create.");
          }

        node.setImages(OPEN_FIELD_CREATE, CLOSED_FIELD_CREATE);
      }
    else
      {
        if (handle.isInactive())
          {
            if (treeNodeDebug)
              {
                System.err.println("inactivate");
              }

            // "{0} (inactive)"
            node.setText(ts.l("global.inactive_pattern", handle.getLabel()));

            node.setMenu(objectReactivatePM);
            node.setImages(OPEN_FIELD_REMOVESET, CLOSED_FIELD_REMOVESET);
          }
        else
          {
            node.setText(handle.getLabel());

            BaseDump bd = (BaseDump) getBaseMap().get(Short.valueOf(node.getInvid().getType()));

            if (bd.canInactivate())
              {
                node.setMenu(objectInactivatePM);
              }
            else 
              {
                node.setMenu(objectRemovePM);
              }

            // now take care of the rest of the menus.

            if (handle.isExpirationSet())
              {
                if (treeNodeDebug)
                  {
                    System.err.println("isExpirationSet");
                  }

                node.setImages(OPEN_FIELD_EXPIRESET, CLOSED_FIELD_EXPIRESET);
              }
            else if (handle.isRemovalSet())
              {
                if (treeNodeDebug)
                  {
                    System.err.println("isRemovalSet()");
                  }

                node.setImages(OPEN_FIELD_REMOVESET, CLOSED_FIELD_REMOVESET);
              }
            else if (changedHash.containsKey(invid))
              {
                if (treeNodeDebug)
                  {
                    System.err.println("Setting icon to edit.");
                  }
		
                node.setImages(OPEN_FIELD_CHANGED, CLOSED_FIELD_CHANGED);
              }
            else // nothing special in handle
              {
                node.setImages(OPEN_FIELD, CLOSED_FIELD);
              } 
          }
      }
  }

  /**
   * This method is called to prompt the user for an XML file to be
   * submitted to the server, which it will proceed to do.
   */

  public void processXMLSubmission()
  {
    File file = null;
    JFileChooser chooser = new JFileChooser();

    /* -- */

    chooser.setDialogType(JFileChooser.OPEN_DIALOG);
    chooser.setDialogTitle(ts.l("processXMLSubmission.file_dialog_title")); // "Ganymede XML File"

    if (this.prefs != null)
      {
	String defaultPath = this.prefs.get("file_load_default_dir", null);
	
	if (defaultPath != null)
	  {
	    chooser.setCurrentDirectory(new File(defaultPath));
	  }
      }

    int returnValue = chooser.showDialog(this, null);

    if (!(returnValue == JFileChooser.APPROVE_OPTION))
      {
	return;
      }

    file = chooser.getSelectedFile();

    File directory = chooser.getCurrentDirectory();

    setWaitCursor();

    try
      {
	if (this.prefs != null)
	  {
	    try
	      {
		this.prefs.put("file_load_default_dir", directory.getCanonicalPath());
	      }
	    catch (java.io.IOException ex)
	      {
		// we don't really care if we can't save the directory
		// path in our preferences all that much.
	      }
	  }
    
        String result = xmlclient.submitXML(this, file);

        StringDialog resultDialog = new StringDialog(this,
                                                     ts.l("processXMLSubmission.results_title"),  // "XML Submission Results"
                                                     result,
                                                     false);

        resultDialog.showDialog();
      }
    finally
      {
        setNormalCursor();
      }
  }


  /********************************************************************************
   *
   * actions on objects.
   *
   *
   * These are the methods to use to do something to an object.
   *
   ********************************************************************************/

  /** 
   * Opens a new {@link arlut.csd.ganymede.client.framePanel framePanel} 
   * window to allow the user to edit an object.
   *
   * Use this to edit objects, so gclient can keep track of the
   * caches, tree nodes, and all the other dirty work.  This should be
   * the only place windowPanel.addWindow() is called for editing
   * purposes.
   *
   * @param invid id for the object to be edited in the new window.
   */

  public void editObject(Invid invid)
  {
    editObject(invid, null, null);
  }

  /** 
   * Opens a new {@link arlut.csd.ganymede.client.framePanel framePanel} 
   * window to allow the user to edit an object.
   *
   * Use this to edit objects, so gclient can keep track of the
   * caches, tree nodes, and all the other dirty work.  This should be
   * the only place windowPanel.addWindow() is called for editing
   * purposes.
   *
   * @param invid id for the object to be edited in the new window.
   * @param originalWindow The framePanel that we are replacing with
   * the editing version.  If null, we won't do any frame
   * replacement.. we'll just create a new frame.
   */

  public void editObject(Invid invid, framePanel originalWindow)
  {
    editObject(invid, null, originalWindow);
  }

  /**
   * Opens a new {@link arlut.csd.ganymede.client.framePanel framePanel}
   * window to allow the user to edit an object.
   *
   * Use this to edit objects, so gclient can keep track of the
   * caches, tree nodes, and all the other dirty work.  This should be
   * the only place windowPanel.addWindow() is called for editing
   * purposes.
   *
   * @param invid id for the object to be edited in the new window.
   * @param objectType String describing the kind of object being edited,
   * used in the titlebar of the window created.
   * @param originalWindow The framePanel that we are replacing with
   * the editing version.  If null, we won't do any frame
   * replacement.. we'll just create a new frame.
   */

  public void editObject(Invid invid, String objectType, framePanel originalWindow)
  {
    if (deleteHash.containsKey(invid))
      {
	// "{0} has already been deleted.\n\nCancel this transaction if you do not wish to delete this object, after all."
	showErrorMessage(ts.l("editObject.already_deleted", getObjectDesignation(invid)));
	return;
      }

    if (wp.isOpenForEdit(invid))
      {
	// "Object Already Being Edited"
	// "You already have a window open to edit {0}."
	showErrorMessage(ts.l("editObject.already_editing_subj"),
			 ts.l("editObject.already_editing_txt", getObjectDesignation(invid)));
	return;
      }

    if (objectType == null || objectType.equals(""))
      {
	objectType = getObjectType(invid);
      }

    ObjectHandle handle = getObjectHandle(invid);
    
    if (handle != null && handle.isInactive())
      {
	Hashtable dialogResults = null;

	// "Edit or Reactivate?"
	// "Warning, {0} is currently inactivated.  If you are seeking
	// to reactivate this object, it is recommended that you use
	// the server's reactivation wizard rather than manually editing it.
	//
	// Can I go ahead and shift you over to the server's
	// reactivation wizard?"
	// "Yes, Reactivate"
	// "No, I want to edit it!"

	DialogRsrc rsrc = new DialogRsrc(this,
					 ts.l("editObject.reactivate_subj"),
					 ts.l("editObject.reactivate_txt", getObjectDesignation(invid)),
					 ts.l("editObject.reactivate_yes"),
					 ts.l("editObject.reactivate_no"),
					 "question.gif", null);

	StringDialog verifyDialog = new StringDialog(rsrc);

	dialogResults = verifyDialog.showDialog();

	if (dialogResults != null)
	  {
	    reactivateObject(invid);
	    return;
	  }
      }
    
    try
      {
	ReturnVal rv = handleReturnVal(session.edit_db_object(invid));

	db_object o = (db_object) rv.getObject();

	if (o == null)
	  {
	    // handleReturnVal threw up a dialog for us if needed

	    return;
	  }

	wp.addWindow(invid, o, true, originalWindow);
	  
	changedHash.put(invid, invid);

	// we don't need to do a full refresh of it, since we've just
	// checked it out..

	setIconForNode(invid);
	tree.refresh();
      }
    catch(Exception rx)
      {
	// "Could not edit object"
	processExceptionRethrow(rx, ts.l("editObject.exception_txt"));
      }
  }

  /** 
   * Clones an object based on origInvid on the server and opens a new
   * client {@link arlut.csd.ganymede.client.framePanel framePanel}
   * window to allow the user to edit the new object.
   *
   * @param origInvid ID of object to be cloned
   */

  public void cloneObject(Invid origInvid)
  {
    Invid invid = null;
    db_object obj = null;

    /* -- */

    if (deleteHash.containsKey(origInvid))
      {
	// "Can't Clone a Deleted Object"
	// "{0} has already been deleted.\n\nCancel this transaction if you do not wish to delete this object after all."
	showErrorMessage(ts.l("cloneObject.deleted_subj"),
			 ts.l("cloneObject.deleted_txt", getObjectDesignation(origInvid)));
	return; 
      }

    // if the admin is a member of more than one owner group, ask what
    // owner groups they want new objects to be placed in

    if (!defaultOwnerChosen)
      {
	if (!chooseDefaultOwner(false))
	  {
	    // They manually closed the default object dialog chooser, so
	    // we won't proceed with the object cloning.

	    return;
	  }
      }

    setWaitCursor();

    try
      {
	final Invid local_origInvid = origInvid;
	obj = null;
	
	try
	  {
	    ReturnVal rv;

	    try
	      {
		/*
		  Use foxtrot to keep the GUI refreshing while we're waiting
		  for the server to clone the object for us.
		*/

		rv = (ReturnVal) foxtrot.Worker.post(new foxtrot.Task()
		  {
		    public Object run() throws Exception
		    {
		      return session.clone_db_object(local_origInvid);
		    }
		  });
	      }
	    catch (java.security.AccessControlException ex)
	      {
		rv = session.clone_db_object(origInvid);
	      }

	    rv = handleReturnVal(rv);
	    obj = (db_object) rv.getObject();
	  }
	catch (Exception rx)
	  {
	    // "Exception creating new object"
	    processExceptionRethrow(rx, ts.l("cloneObject.exception_txt"));
	  }

	// we'll depend on handleReturnVal() above showing the user a rejection
	// dialog if the object create was rejected

	if (obj == null)
	  {
	    return;
	  }

	try
	  {
	    invid = obj.getInvid();
	  }
	catch (Exception rx)
	  {
	    processExceptionRethrow(rx);
	  }

	// "New Object"
	ObjectHandle handle = new ObjectHandle(ts.l("global.new_object"), invid, false, false, false, true);
       
	wp.addWindow(invid, obj, true, true, null);

	Short typeShort = Short.valueOf(invid.getType());
    
	if (cachedLists.containsList(typeShort))
	  {
	    objectList list = cachedLists.getList(typeShort);
	    list.addObjectHandle(handle);
	  }
    
	// If the base node is open, deal with the node.

	BaseNode baseN = null;

	if (shortToBaseNodeHash.containsKey(typeShort))
	  {
	    baseN = (BaseNode)shortToBaseNodeHash.get(typeShort);

	    if (baseN.isLoaded())
	      {
		InvidNode objNode = new InvidNode(baseN, 
						  handle.getLabel(),
						  invid,
						  null, false,
						  OPEN_FIELD_CREATE,
						  CLOSED_FIELD_CREATE,
						  baseN.canInactivate() ? objectInactivatePM : objectRemovePM,
						  handle);
	    
		createHash.put(invid, new CacheInfo(typeShort, handle.getLabel(), null, handle));

		invidNodeHash.put(invid, objNode);
		setIconForNode(invid);

		tree.insertNode(objNode, true);  // the true means the tree will refresh
	      }
	    else
	      {
		// this hash is used when creating the node for the object
		// in the tree.  This way, if a new object is created
		// before the base node is expanded, the new object will
		// have the correct icon.

		createdObjectsWithoutNodes.put(invid, baseN);
	      }
	  }
	
	somethingChanged();
      }
    finally
      {
	setNormalCursor();
      }
  }

  /** 
   * Creates a new object on the server and opens a new
   * client {@link arlut.csd.ganymede.client.framePanel framePanel}
   * window to allow the user to edit the new object.
   *
   * @param type Type of object to be created
   */

  public db_object createObject(short type)
  {
    Invid invid = null;
    db_object obj = null;

    /* -- */

    // if the admin is a member of more than one owner group, ask what
    // owner groups they want new objects to be placed in

    if (!defaultOwnerChosen)
      {
	if (!chooseDefaultOwner(false))
	  {
	    // They manually closed the default object dialog chooser, so
	    // we won't proceed with object creation.

	    return null;
	  }
      }
    
    setWaitCursor();

    try
      {
	try
	  {
	    final short local_type = type;

	    ReturnVal rv; 

	    try
	      {
		/*
		  Use foxtrot to keep the GUI refreshing while we're
		  waiting for the server to create the object for us.
		*/

		rv = (ReturnVal) foxtrot.Worker.post(new foxtrot.Task()
		  {
		    public Object run() throws Exception
		    {
		      return session.create_db_object(local_type);
		    }
		  });
	      }
	    catch (java.security.AccessControlException ex)
	      {
		rv = session.create_db_object(local_type);
	      }

	    rv = handleReturnVal(rv);
	    obj = (db_object) rv.getObject();
	  }
	catch (Exception rx)
	  {
	    // "Exception encountered creating new object"
	    processExceptionRethrow(rx, ts.l("createObject.exception_txt"));
	  }

	// we'll depend on handleReturnVal() above showing the user a rejection
	// dialog if the object create was rejected

	if (obj == null)
	  {
	    return null;
	  }

	try
	  {
	    invid = obj.getInvid();
	  }
	catch (Exception rx)
	  {
	    processExceptionRethrow(rx);
	  }

	// "New Object"
	ObjectHandle handle = new ObjectHandle(ts.l("global.new_object"), invid, false, false, false, true);
       
	wp.addWindow(invid, obj, true, true, null);

	Short typeShort = Short.valueOf(type);
    
	if (cachedLists.containsList(typeShort))
	  {
	    objectList list = cachedLists.getList(typeShort);
	    list.addObjectHandle(handle);
	  }
    
	// If the base node is open, deal with the node.

	BaseNode baseN = null;

	if (shortToBaseNodeHash.containsKey(typeShort))
	  {
	    baseN = (BaseNode)shortToBaseNodeHash.get(typeShort);

	    if (baseN.isLoaded())
	      {
		InvidNode objNode = new InvidNode(baseN, 
						  handle.getLabel(),
						  invid,
						  null, false,
						  OPEN_FIELD_CREATE,
						  CLOSED_FIELD_CREATE,
						  baseN.canInactivate() ? objectInactivatePM : objectRemovePM,
						  handle);
	    
		createHash.put(invid, new CacheInfo(typeShort, handle.getLabel(), null, handle));

		invidNodeHash.put(invid, objNode);
		setIconForNode(invid);

		tree.insertNode(objNode, true);  // the true means the tree will refresh
	      }
	    else
	      {
		// this hash is used when creating the node for the object
		// in the tree.  This way, if a new object is created
		// before the base node is expanded, the new object will
		// have the correct icon.

		createdObjectsWithoutNodes.put(invid, baseN);
	      }
	  }
	
	somethingChanged();
      }
    finally
      {
	setNormalCursor();
      }

    return obj;
  }

  /**
   * Opens a new {@link arlut.csd.ganymede.client.framePanel framePanel} 
   * window to view the object corresponding to the given invid.
   */

  public void viewObject(Invid invid)
  {
    viewObject(invid, null);
  }

  /**
   * Opens a new {@link arlut.csd.ganymede.client.framePanel framePanel}
   * window to view the object corresponding to the given invid.
   *
   * @param objectType Type of the object to be viewed.. if this is
   * null, the server will be queried to determine the type of object
   * for the title-bar of the view object window.  By providing it
   * here from a local cache, and server-call can be saved.
   */

  public void viewObject(Invid invid, String objectType)
  {
    if (deleteHash.containsKey(invid))
      {
	// "{0} has already been deleted.\n\nCancel this transaction if you do not wish to delete this object after all."
	showErrorMessage(ts.l("viewObject.deleted_txt", getObjectDesignation(invid)));
	return;
      }

    try
      {
	ReturnVal rv = handleReturnVal(session.view_db_object(invid));
	db_object object = (db_object) rv.getObject();

	// we'll assume handleReturnVal() will display any rejection
	// dialogs from the server

	if (object == null)
	  {
	    return;
	  }

	wp.addWindow(invid, object, false);
      }
    catch (Exception rx)
      {
	// "Could not view object"
	processExceptionRethrow(rx, ts.l("viewObject.exception_txt"));
      }
  }

  /**
   * Marks an object on the server as deleted.  The object will not
   * actually be removed from the database until the transaction is
   * committed.
   *
   * This method does a fair amount of internal bookkeeping to manage
   * the client's tree display, status caching, etc.
   *
   * @param invid The object invid identifier to be deleted
   * @param showDialog If true, we'll show a dialog box asking the user
   * if they are sure they want to delete the object in question.
   */

  public void deleteObject(Invid invid, boolean showDialog)
  {
    ReturnVal retVal;
    boolean ok = false;
    String label = null;

    /* -- */

    if (deleteHash.containsKey(invid))
      {
	// "Object Already Deleted"
	// "{0} has already been marked as deleted.\n\nYou can hit the
	// commit button to permanently get rid of this object, or you
	// can hit the cancel button to undo everything."

	showErrorMessage(ts.l("deleteObject.deleted_subj"),
			 ts.l("deleteObject.deleted_txt", getObjectDesignation(invid)));
	return; 
      }
    
  
    // we can delete objects if they are newly created.. the server
    // has support for discarding newly created objects, in fact.  If
    // the user attempted to close a created object window, the
    // framePanel.vetoableChange() method will have set
    // closingApproved on the editing frame, which will cause the
    // wp.isApprovedForClosing(invid) check here to be true, and we'll
    // skip this question.

    if (wp.isOpenForEdit(invid) && !wp.isApprovedForClosing(invid))
      {	
	// "Object being edited"
	// "You are currently editing {0}.  I can''t delete this
	// object while you are actively editing it.\n\nYou must
	// commit or cancel this transaction before this object can be
	// deleted."

	showErrorMessage(ts.l("deleteObject.edited_subj"),
			 ts.l("deleteObject.edited_txt", getObjectDesignation(invid)));
	return;
      }
    
    if (showDialog)
      {
	// "Verify Object Deletion"
	// "Are you sure you want to delete {0}?"
	StringDialog d = new StringDialog(this,
					  ts.l("deleteObject.verify_subj"),
					  ts.l("deleteObject.verify_txt", getObjectDesignation(invid)),
					  StringDialog.getDefaultOk(),
					  StringDialog.getDefaultCancel(),
					  getQuestionImage());
	Hashtable result = d.showDialog();
	
	if (result == null)
	  {
	    // "Canceled!"
	    setStatus(ts.l("deleteObject.canceled"));
	    
	    return;
	  }
      }

    setWaitCursor();

    try
      {
	Short id = Short.valueOf(invid.getType());

	if (debug)
	  {
	    System.err.println("Deleting invid= " + invid);
	  }

	// Delete the object

	retVal = handleReturnVal(session.remove_db_object(invid));

	if (ReturnVal.didSucceed(retVal))
	  {
	    // InvidNode node = (InvidNode)invidNodeHash.get(invid);

	    // Check out the deleteHash.  If this one is already on there,
	    // then I don't know what to do.  If it isn't, then add a new
	    // cache info.  I guess maybe update the name or something,
	    // if it is on there.

	    CacheInfo info = null;
	    label = session.viewObjectLabel(invid);

	    // Take this object out of the cachedLists, if it is in there

	    if (cachedLists.containsList(id))
	      {
		if (debug)
		  {
		    System.err.println("This base has been hashed.  Removing: " + label);
		  }

		objectList list = cachedLists.getList(id);

		ObjectHandle h = list.getObjectHandle(invid);
		list.removeInvid(invid);

		info = new CacheInfo(id, label, null, h);
	      }
	    else
	      {
		info = new CacheInfo(id, label, null);
	      }

	    if (deleteHash.containsKey(invid))
	      {
		if (debug)
		  {
		    System.err.println("already deleted, nothing to change, right?");
		  }
	      }
	    else
	      {
		deleteHash.put(invid, info);
	      }

	    if (invidNodeHash.containsKey(invid))
	      {
		setIconForNode(invid);
		tree.refresh();
	      }

	    // "{0} will be deleted when commit is clicked."
	    setStatus(ts.l("deleteObject.ready_to_delete", getObjectDesignation(invid)));
	    somethingChanged();
	  }
	else
	  {
	    // "Delete Failed"
	    setStatus(ts.l("deleteObject.failed"));
	  }
      }
    catch (Exception rx)
      {
	// "Error attempting to delete {0}.  Object not deleted."
	processExceptionRethrow(rx, ts.l("deleteObject.exception_txt", getObjectDesignation(invid)));
      }
    finally
      {
	setNormalCursor();
      }

    return;
  }

  /** 
   * Marks an object on the server as inactivated.  The object will not
   * actually be removed from the database until the transaction is
   * committed.  Note that the inactivation request will typically cause
   * a dialog to come back from the server requesting the user fill in
   * parameters describing how the object is to be inactivated.
   *
   * This method does a fair amount of internal bookkeeping to manage
   * the client's tree display, status caching, etc.
   */

  public void inactivateObject(Invid invid)
  {
    boolean ok = false;
    ReturnVal retVal;

    /* -- */

    if (deleteHash.containsKey(invid))
      {
	// "Object Already Deleted"
	// "{0} has already been deleted.\n\nCancel this transaction if you do not wish to delete this object after all."
	showErrorMessage(ts.l("inactivateObject.deleted_subj"),
			 ts.l("inactivateObject.deleted_txt", getObjectDesignation(invid)));
	return;
      }

    if (wp.isOpenForEdit(invid))
      {
	// "Object Being Edited"
	// "I can''t inactivate this object while you have a window open to edit {0}."
	showErrorMessage(ts.l("inactivateObject.edited_subj"),
			 ts.l("inactivateObject.edited_txt", getObjectDesignation(invid)));
	return;
      }

    String designation = getObjectDesignation(invid);

    // "Inactivating {0}."
    setStatus(ts.l("inactivateObject.inactivating", designation), 2);
    setWaitCursor();

    try
      {
	retVal = session.inactivate_db_object(invid);
	    
	if (retVal != null)
	  {
	    retVal = handleReturnVal(retVal);

	    if (retVal == null)
	      {
		ok = true;
	      }
	    else
	      {
		ok = retVal.didSucceed();
	      }
	  }
	else
	  {
	    ok = true;
	  }

	if (ok)
	  {
	    // remember that we changed this object for the refreshChangedObjectHandles

	    changedHash.put(invid, invid);

	    // refresh it now
		
	    refreshChangedObject(invid);

	    // and update the tree

	    tree.refresh();
	    // "{0} inactivated."
	    setStatus(ts.l("inactivateObject.success", designation), 2);
	    somethingChanged();
	  }
	else
	  {
	    // "Could not inactivate {0}."
	    setStatus(ts.l("inactivateObject.failure", designation));
	  }
      }
    catch (Exception rx)
      {
	// "Could not inactivate {0}."
	processExceptionRethrow(rx, ts.l("inactivateObject.failure", designation));
      }
    finally
      {
	setNormalCursor();
      }
  }

  /**
   * Reactivates an object that was previously inactivated. The
   * object's status will not actually be changed in the database
   * until the transaction is committed.  Note that the reactivation
   * request will typically cause a dialog to come back from the
   * server requesting the user fill in parameters describing how the
   * object is to be reactivated.
   *
   * Typically reactivating an object involves clearing the removal
   * date from I think you should call this from the expiration date
   * panel if the date is cleared.
   */

  public boolean reactivateObject(Invid invid)
  {
    ReturnVal retVal;
    boolean ok = false;

    /* -- */

    try
      {
	setWaitCursor();
	retVal = session.reactivate_db_object(invid);

	if (retVal == null)
	  {
	    // It worked
	    ok = true;
	  }
	else
	  {
	    retVal = handleReturnVal(retVal);

	    if (retVal == null)
	      {
		ok = true;
	      }
	    else
	      {
		ok = retVal.didSucceed();
	      }
	  }
      }
    catch (Exception rx)
      {
	// "Could not reactivate {0}."
	processExceptionRethrow(rx, ts.l("reactivateObject.failure", getObjectDesignation(invid)));
      }

    if (ok)
      {
	somethingChanged();

	// "{0} reactivated."
	setStatus(ts.l("reactivateObject.success", getObjectDesignation(invid)), 2);

	// remember that this invid has been edited, and will need
	// to be refreshed on commit

	changedHash.put(invid, invid);

	refreshChangedObject(invid);

	tree.refresh();
	setNormalCursor();
      }

    return ok;    
  }

  /**
   * Show the create object dialog, let the user choose
   * to create or not create an object.
   */

  void createObjectDialog()
  {
    // The dialog is modal, and will set itself visible when created.
    // If we have already created it, we'll just pack it and make it
    // visible

    if (createDialog == null)
      {
	createDialog = new createObjectDialog(this);
      }
    else
      {
	createDialog.pack();	// force it to re-center itself.
	createDialog.setVisible(true);
      }
  }
  
  /**
   * Opens a dialog to let the user choose an object for editing, and 
   * if cancel is not chosen, the object is opened for editing.
   *
   * If an object node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected node.
   */

  void editObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }
    else
      {
	// if we have a node selected, recreate the dialog so that it
	// will get re-initialized.

	if (selectedNode != null && selectedNode instanceof InvidNode)
	  {
	    openDialog.dispose();
	    openDialog = new openObjectDialog(this);	    
	  }
      }

    // "Open object for editing"
    openDialog.setText(ts.l("editObjectDialog.dialog_txt"));
    openDialog.setIcon(new ImageIcon(pencil));
    openDialog.setReturnEditableOnly(true);
    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	if (debug)
	  {
	    System.err.println("Canceled");
	  }
      }
    else
      {
	editObject(invid, openDialog.getTypeString(), null);
      }
  }

  /**
   * Opens a dialog to let the user choose an object for viewing,
   * and if cancel is not chosen, the object is opened for viewing.
   *
   * If an object node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected node.
   */

  void viewObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }
    else
      {
	// if we have a node selected, recreate the dialog so that it
	// will get re-initialized.

	if (selectedNode != null && selectedNode instanceof InvidNode)
	  {
	    openDialog.dispose();
	    openDialog = new openObjectDialog(this);	    
	  }
      }

    // "Open object for viewing"
    openDialog.setText(ts.l("viewObjectDialog.dialog_txt"));
    openDialog.setIcon(new ImageIcon(search));
    openDialog.setReturnEditableOnly(false);

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	if (debug)
	  {
	    System.err.println("Canceled");
	  }
      }
    else
      {
	viewObject(invid);
      }
  }

  /**
   * Opens a dialog to let the user choose an object for inactivation,
   * and if cancel is not chosen, the object is opened for inactivation.
   *
   * If an object node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected node.
   */

  void inactivateObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }
    else
      {
	// if we have a node selected, recreate the dialog so that it
	// will get re-initialized.

	if (selectedNode != null && selectedNode instanceof InvidNode)
	  {
	    openDialog.dispose();
	    openDialog = new openObjectDialog(this);	    
	  }
      }

    // "Choose object to be inactivated"
    openDialog.setText(ts.l("inactivateObjectDialog.dialog_txt"));
    openDialog.setIcon(null);
    openDialog.setReturnEditableOnly(true);

    Invid invid = openDialog.chooseInvid();
    
    inactivateObject(invid);
  }

  /**
   * Opens a dialog to let the user choose an object for deletion,
   * and if cancel is not chosen, the object is opened for deletion.
   *
   * If a node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected object.
   */

  void deleteObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }
    else
      {
	// if we have a node selected, recreate the dialog so that it
	// will get re-initialized.

	if (selectedNode != null && selectedNode instanceof InvidNode)
	  {
	    openDialog.dispose();
	    openDialog = new openObjectDialog(this);	    
	  }
      }

    // "Choose object to be deleted"
    openDialog.setText(ts.l("deleteObjectDialog.dialog_txt"));
    openDialog.setIcon(new ImageIcon(trash));
    openDialog.setReturnEditableOnly(true);

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	if (debug)
	  {
	    System.err.println("Canceled");
	  }
      }
    else
      {
	deleteObject(invid, true);
      }
  }

  /**
   * Opens a dialog to let the user choose an object for cloning,
   * and if cancel is not chosen, the object is opened for cloning.
   *
   * If a node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected object.
   */

  void cloneObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }
    else
      {
	// if we have a node selected, recreate the dialog so that it
	// will get re-initialized.

	if (selectedNode != null && selectedNode instanceof InvidNode)
	  {
	    openDialog.dispose();
	    openDialog = new openObjectDialog(this);	    
	  }
      }

    // "Choose object to be cloned"
    openDialog.setText(ts.l("cloneObjectDialog.dialog_txt"));
    openDialog.setIcon(new ImageIcon(cloneIcon));
    openDialog.setReturnEditableOnly(false);

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	if (debug)
	  {
	    System.err.println("Canceled");
	  }
      }
    else
      {
	cloneObject(invid);
      }
  }

  /**
   * Creates and presents a dialog to let the user change their selected persona.
   *
   * gclient's personaListener reacts to events from the persona change
   * dialog and will react appropriately as needed.  This method doesn't
   * actually do anything other than display the dialog.
   *
   * PersonaDialog is modal, however, so this method will block until the
   * user makes a choice in the dialog box.
   */

  void changePersona(boolean requirePassword)
  {
    personaDialog = new PersonaDialog(client, requirePassword);
    personaDialog.pack();	// force it to re-center itself.
    personaDialog.setVisible(true); // block
  }

  /**
   * Returns a reference to the most recently created persona dialog.
   */

  PersonaDialog getPersonaDialog()
  {
    return personaDialog;
  }

  /**
   * Logs out from the client.
   *
   * This method does not do any checking, it just logs out.
   */

  void logout()
  {
    // glogin's logout method will call our cleanUp() method on the
    // GUI thread.

    sizer.saveSize(this);

    _myglogin.logout();
  }

  /**
   * Create a custom query filter.
   *
   * The filter is used to limit the output on a query, so that
   * supergash can see the world through the eyes of a less-privileged
   * persona.  This seemed like a good idea at one point, not sure how
   * valuable this really is anymore.
   */

  public void chooseFilter()
  {
    if (filterDialog == null)
      {
	filterDialog = new JFilterDialog(this);
      }
    else
      {
	filterDialog.setVisible(true);
      }
  }

  /**
   * This method is called by the {@link
   * arlut.csd.ganymede.client.JFilterDialog JFilterDialog} class when
   * the owner list filter is changed, to refresh the tree's display
   * of all object lists loaded into the client so that only those
   * objects matching the owner list filter are visible.
   */

  public void updateAfterFilterChange()
  {
    clearCaches();
    updateTreeAfterFilterChange(tree.getRoot());
    tree.refresh();

    // update all our object editing windows so that we can refresh
    // the choice lists.. ? -- not sure why this logic is being done
    // here, actually.. this dates back to revision 4596, Oct 30, 2001

    wp.refreshObjectWindows(null, null);
  }

  /**
   * This method updates all category and base nodes at or under
   * the given node, and all category and base nodes that are nextSiblings
   * to the given node.
   */

  private void updateTreeAfterFilterChange(treeNode node)
  {
    if (node == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("updateAfterFilterChange examining: " + node);
      }

    if (node instanceof BaseNode)
      {
	while (node instanceof BaseNode)
	  {
	    if (node.getChild() != null)
	      {
		if (debug)
		  {
		    System.err.println("Updating " + node);
		  }

		try
		  {
		    refreshObjects((BaseNode) node, false);
		  }
		catch (Exception ex)
		  {
		    // "Could not refresh object base {0}"
		    processExceptionRethrow(ex, ts.l("updateTreeAfterFilterChange.exception_txt", node.getText()));
		  }
	      }

	    node = node.getNextSibling();
	  }
      }

    if (node instanceof CatTreeNode)
      {
	updateTreeAfterFilterChange(node.getChild());
	updateTreeAfterFilterChange(node.getNextSibling());
      }
  }

  /**
   * Chooses the default owner group for a newly created object.
   *
   * This must be called before Session.create_db_object is called.
   */

  public boolean chooseDefaultOwner(boolean forcePopup)
  {
    ReturnVal retVal = null;
    
    if (ownerGroups == null)
      {
	try
	  {
	    ownerGroups = session.getOwnerGroups().getListHandles();

	    if (ownerGroups == null)
	      {
		throw new NullPointerException();
	      }
	  }
	catch (Exception rx)
	  {
	    // "Exception encountered attempting to load owner groups from the server"
	    processExceptionRethrow(rx, ts.l("chooseDefaultOwner.exception_txt"));
	  }
      }

    // we know that the supergash owner group, at least, should exist
    // on the server, so if we see zero ownerGroups, we know the user
    // just doesn't have the required permissions.

    if (ownerGroups.size() == 0)
      {
	// "Permissions Error"
	// "Your account doesn''t have permission to access any owner groups on the server.\n\nYou cannot create new objects with this account."
	showErrorMessage(ts.l("chooseDefaultOwner.permissions_subj"),
			 ts.l("chooseDefaultOwner.permissions_txt"));
	return false;
      }
    else if (!forcePopup && (ownerGroups.size() == 1))
      {
	// if forcePopup is false and there's only one group
	// available, we'll go ahead and pick the default owner for
	// objects created by this user ourselves.

	defaultOwnerChosen = true;

	Vector owners = new Vector();

	for (int i = 0; i < ownerGroups.size(); i++)
	  {
	    owners.addElement(((listHandle)ownerGroups.elementAt(i)).getObject());
	  }

	try
	  {
	    handleReturnVal(session.setDefaultOwner(owners));
	  }
	catch (Exception rx)
	  {
	    // "Exception encountered attempting to set the default owner group for newly created objects."
	    processExceptionRethrow(rx, ts.l("chooseDefaultOwner.exception2_txt"));
	  }

	return true;
      }
    
    defaultOwnerDialog = new JDefaultOwnerDialog(this, ownerGroups);

    retVal = defaultOwnerDialog.chooseOwner();

    handleReturnVal(retVal);

    if ((retVal == null) || (retVal.didSucceed()))
      {
	defaultOwnerChosen = true;
      }
    else
      {
	// if the user voluntarily popped up the choose default owner
	// dialog (i.e., by using the menu) and then forcibly closed
	// the dialog window rather than clicking 'Ok', we'll get a
	// negative return value from the chooseOwner() method, but we
	// shouldn't take that as meaning that the defaultOwnerChosen
	// flag should be set false, as it might previously have been
	// appropriately set.

	if (!forcePopup)
	  {
	    defaultOwnerChosen = false;
	  }
      }

    return defaultOwnerChosen;
  }

  /**
   * True if a default owner has already been chosen.
   */

  public boolean defaultOwnerChosen()
  {
    return defaultOwnerChosen;
  }

  /**
   * Check for changes in the database before logging out.
   *
   * This checks to see if anything has been changed.  Basically, if edit panels are
   * open and have been changed in any way, then somethingChanged will be true and 
   * the user will be warned.  If edit panels are open but have not been changed, then
   * it will return true(it is ok to proceed).
   */

  boolean OKToProceed()
  {
    // we're using the enabled status of the logoutMI as a flag to
    // control whether we're in the middle of bringing up an internal
    // frame.  by gating on this, we can prevent the client from
    // throwing an exception due to a mistimed logout/window close.

    if (!logoutMI.isEnabled())
      {
	return false;
      }

    if (wizardActive > 0)
      {
	if (debug)
	  {
	    System.err.println("gclient: wizard is active, not ok to logout.");
	  }

	return false;
      }

    if (getSomethingChanged())
      {
	// "Warning: Changes have been made."
	// "You have made changes in objects on the server that have
	// not been committed.  If you log out now, those changes will
	// be lost.\n\nAre you sure you want to log out and lose these
	// changes?"
	// "Yes, Discard Changes"
	StringDialog dialog = new StringDialog(this,
					       ts.l("OKToProceed.changes_warning_subj"),
					       ts.l("OKToProceed.changes_warning_txt"),
					       ts.l("OKToProceed.yes"),
					       StringDialog.getDefaultCancel());

	// if showDialog is null, cancel was clicked So return will be
	// false if cancel was clicked

	return (dialog.showDialog() != null);
      }
    else
      {
	return true;
      }
  }

  /**
   * Updates the note panels in the open windows.
   *
   * The note panel doesn't have a listener on the TextArea, so when a transaction is
   * committed, this must be called on each notePanel in order to update the server.
   *
   * This basically does a field.setValue(notesArea.getValue()) on each notesPanel.
   *
   * THIS IS A PRETTY BIG HACK.
   */

  void updateNotePanels()
  {
    Vector windows = wp.getEditables();

    for (int i = 0; i < windows.size(); i++)
      {
	if (debug)
	  {
	    System.err.println("Updating window number " + i);
	  }

	framePanel fp = (framePanel)windows.elementAt(i);

	if (fp == null)
	  {
	    if (debug)
	      {
		System.err.println("null frame panel in updateNotesPanels");
	      }
	  }
	else
	  {
	    notesPanel np = fp.getNotesPanel();

	    if (np == null)
	      {
		if (debug)
		  {
		    System.err.println("null notes panel in frame panel");
		  }
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("Calling update notes.");
		  }

		np.updateNotes();
	      }
	  }
      }
  }

  /** 
   * Commits the currently open transaction on the server.  All
   * changes made by the user since the last openNewTransaction() call
   * will be integrated into the database on the Ganymede server.
   *
   * For various reasons, the server may reject the transaction as
   * incomplete.  Usually this will be a non-fatal error.. the user
   * will see a dialog telling him what else needs to be filled out in
   * order to commit the transaction.  In this case,
   * commitTransaction() will have had no effect and the user is free
   * to try again.
   *
   * If the transaction is committed successfully, the relevant
   * object nodes in the tree will be fixed up to reflect their state
   * after the transaction is committed.  commitTransaction() will
   * close all open editing windows, and will call openNewTransaction()
   * to prepare the server for further changes by the user. 
   */

  public void commitTransaction()
  {
    ReturnVal retVal = null;
    boolean succeeded = false;

    /* -- */

    setWaitCursor();

    try
      {
	// We need to check to see if any notes panels need to
	// have their text flushed to the server.. 

	updateNotePanels();
	
	retVal = session.commitTransaction();

	if (retVal != null)
	  {
	    retVal = handleReturnVal(retVal);
	  }

	succeeded = ((retVal == null) || retVal.didSucceed());

	// if we succeed, we clean up.  If we don't,
	// retVal.doNormalProcessing can be false, in which case the
	// serve aborted our transaction utterly.  If
	// retVal.doNormalProcessing is true, the user can do
	// something to make the transaction able to complete
	// successfully.  In this case, handleReturnVal() will have
	// displayed a dialog telling the user what needs to be done.

	if (succeeded)
	  {
	    // "Transaction successfully commited."
	    setStatus(ts.l("commitTransaction.success"));
	    wp.closeEditables();
	
	    wp.refreshTableWindows();

	    openNewTransaction();

	    //
	    // This fixes all the icons in the tree, and closes any
	    // view windows on objects that we have deleted
	    //

	    refreshTreeAfterCommit();

	    if (debug)
	      {
		System.err.println("Done committing");
	      }
	  }
	else if (!retVal.doNormalProcessing)
	  {
	    // "Transaction aborted on the server."
	    setStatus(ts.l("commitTransaction.failure"));

	    // This is just like a cancel.  Something went wrong, and
	    // the server canceled our transaction.  We don't need to
	    // call cancelTransaction ourselves.

	    // "Commit Failure"
	    // "The server experienced a failure committing this transaction, and was forced to abort the transaction entirely."

	    showErrorMessage(ts.l("commitTransaction.error_subj"),
			     ts.l("commitTransaction.error_txt"));
	  }
      }
    catch (Exception ex)
      {
	// "An exception was caught while attempting to commit a transaction."
	processExceptionRethrow(ex, ts.l("commitTransaction.exception_txt"));
      }
    finally
      {
	setNormalCursor();

	if (!succeeded && (retVal == null || !retVal.doNormalProcessing))
	  {
	    wp.closeEditables();
	    cleanUpAfterCancel();
	    openNewTransaction();
	  }
      }
  }

  /**
   * Cancels the current transaction.  Any changes made by the user since
   * the last openNewTransaction() call will be forgotten as if they
   * never happened.  The client's tree display will be reverted to the
   * state it was when the transaction was started, and all open windows
   * will be closed.
   */

  public synchronized void cancelTransaction()
  {
    ReturnVal retVal;

    /* -- */

    // close all the client windows.. this causes the windows to cancel
    // their loading activity

    wp.closeAll(true);

    try
      {
	retVal = session.abortTransaction();

	if (retVal != null)
	  {
	    retVal = handleReturnVal(retVal);
	  }

	if (retVal == null || retVal.didSucceed())
	  {
	    // "Transaction canceled."
	    setStatus(ts.l("cancelTransaction.canceled"), 3);

	    if (debug)
	      {
		System.err.println("Cancel succeeded");
	      }
	  }
	else
	  {
	    // "Error on server, transaction cancel failed."
	    setStatus(ts.l("cancelTransaction.error_canceling"));

	    if (debug)
	      {
		System.err.println("Everytime I think I'm out, they pull me back in! " +
				   "Something went wrong with the cancel.");
	      }
	    
	    return;
	  }
      }
    catch (Exception rx)
      {
	// "An exception was caught while attempting to cancel a transaction."
	processExceptionRethrow(rx, ts.l("cancelTransaction.exception_txt"));
      }

    cleanUpAfterCancel();

    openNewTransaction();
  }

  /**
   * Cleans up the tree and gclient's caches.
   *
   * This method is precisely analagous in function to
   * {@link arlut.csd.ganymede.client.gclient#refreshTreeAfterCommit() refreshTreeAfterCommit()},
   * except for use after a cancel, when nodes marked as deleted are not removed from the tree,
   * and nodes marked as created are not kept.
   */

  private synchronized void cleanUpAfterCancel()
  {
    ObjectHandle handle;
    Invid invid;
    InvidNode node;
    objectList list;
    CacheInfo info;

    /* -- */

    synchronized (deleteHash)
      {
	Enumeration dels = deleteHash.keys();
    
	while (dels.hasMoreElements())
	  {
	    invid = (Invid)dels.nextElement();
	    info = (CacheInfo)deleteHash.get(invid);
	
	    list = cachedLists.getList(info.getBaseID());	    
	
	    if (list != null)
	      {
		if (createHash.containsKey(invid))
		  {
		    if (debug)
		      {
			System.err.println("Can't fool me: you just created this object!");
		      }
		  }
		else
		  {
		    if (debug)
		      {
			System.err.println("This one is hashed, sticking it back in.");
		      }
		
		    handle = info.getOriginalObjectHandle();

		    if (handle != null)
		      {
			list.addObjectHandle(handle);
			node = (InvidNode)invidNodeHash.get(invid);
			
			if (node != null)
			  {
			    node.setHandle(handle);
			  }
		      }
		  }
	      }
	    
	    deleteHash.remove(invid);
	    setIconForNode(invid);
	  }
      }
    
    node = null;
    invid = null;
    list = null;
    info = null;

    // Next up is created list: remove all the added stuff.

    synchronized (createHash)
      {
	Enumeration created = createHash.keys();
    
	while (created.hasMoreElements())
	  {
	    invid = (Invid) created.nextElement();
	    info = (CacheInfo)createHash.get(invid);
	    
	    list = cachedLists.getList(info.getBaseID());
	    
	    if (list != null)
	      {
		if (debug)
		  {
		    System.err.println("This one is hashed, taking a created object out.");
		  }
	    
		list.removeInvid(invid);
	      }
	    
	    createHash.remove(invid);
	    
	    node = (InvidNode)invidNodeHash.get(invid);
	    
	    if (node != null)
	      {
		tree.deleteNode(node, false);
		invidNodeHash.remove(invid);
	      }
	  }
      }

    createdObjectsWithoutNodes.clear();
    
    // Now go through changed list and revert any names that may be needed

    Vector changedInvids = new Vector();

    synchronized (changedHash)
      {
	Enumeration changed = changedHash.keys();

	while (changed.hasMoreElements())
	  {
	    changedInvids.addElement(changed.nextElement());
	  }

	changedHash.clear();
      }

    refreshChangedObjectHandles(changedInvids, true);

    if (debug && createHash.isEmpty() && deleteHash.isEmpty())
      {
	System.err.println("Woo-woo the hashes are all empty");
      }

    tree.refresh(); // To catch all the icon changing.
  }

  /**
   * Initializes a new transaction on the server
   */

  private void openNewTransaction()
  {
    try
      {
	// "Ganymede GUI Client"
	ReturnVal rv = session.openTransaction(ts.l("openNewTransaction.client_name"));
	
	handleReturnVal(rv);

	if ((rv != null) && (!rv.didSucceed()))
	  {
	    // "Could not open a new transaction on the Ganymede server."
	    showErrorMessage(ts.l("openNewTransaction.error_txt"));
	  }
	
	tree.refresh();
      }
    catch (Exception rx)
      {
	// "Could not open a new transaction on the Ganymede server."
	processExceptionRethrow(rx, ts.l("openNewTransaction.error_txt"));
      }

    setSomethingChanged(false);
    cancel.setEnabled(false);
    commit.setEnabled(false);

    tree.requestFocus();

    clearCaches();
  }

  /**
   * toggles the toolbar on and off
   */

  void toggleToolBar()
  {
    if (toolToggle == true) 
      {
	if (((BasicToolBarUI)toolBar.getUI()).isFloating()) 
	  {
	    ((BasicToolBarUI)toolBar.getUI()).setFloating(false, new Point(0,0));
	  }
	
	toolBar.setVisible(false);
	toolToggle = false;
      } 
    else if (toolToggle == false)
      { 
	toolBar.setVisible(true);
	toolToggle = true;
      }

    getContentPane().validate();
  }
  
  // ActionListener Methods

  /**
   * Handles button and menu picks.  Includes logic for threading
   * out queries and message panels to avoid locking the Java GUI
   * thread.
   */
  
  public void actionPerformed(java.awt.event.ActionEvent event)
  {
    Object source = event.getSource();
    String command = event.getActionCommand();

    /* -- */

    if (debug)
      {
	System.err.println("Action: " + command);
      }

    if (source == cancel)
      {
	if (debug)
	  {
	    System.err.println("cancel button clicked");
	  }
	
	cancelTransaction();
      }
    else if (source == commit)
      {
	if (debug)
	  {
	    System.err.println("commit button clicked");
	  }
	
	commitTransaction();
      }
    else if ((source == menubarQueryMI) || (command.equals(query_action)))
      {
	postQuery(null);
      }
    else if (source == clearTreeMI)
      {
	clearTree();
      }
    else if (source == submitXMLMI)
      {
        processXMLSubmission();
      }
    else if (source == hideNonEditablesMI)
      {
	hideNonEditables = hideNonEditablesMI.getState();
	clearTree();
      }
    else if (source == logoutMI)
      {
	if (OKToProceed())
	  {
	    logout();
	  }
      }
    else if (source == toggleToolBarMI)
      {
	toggleToolBar();
      }
    else if (command.equals(persona_action))
      {
	changePersona(false);
      }
    else if (command.equals(create_action))
      {
	createObjectDialog();
      }
    else if (command.equals(edit_action))
      {
	editObjectDialog();
      }
    else if (command.equals(view_action))
      {
	viewObjectDialog();
      }
    else if (command.equals(delete_action))
      {
	deleteObjectDialog();
      }
    else if (command.equals(clone_action))
      {
	cloneObjectDialog();
      }
    else if (command.equals(inactivate_action))
      {
	inactivateObjectDialog();
      }
    else if (command.equals(access_invid_action))
      {
	openAnInvid();
      }
    else if (command.equals(owner_filter_action))
      {
	chooseFilter();
      }
    else if (command.equals(default_owner_action))
      {
	chooseDefaultOwner(true);
      }
    else if (command.equals(help_action))
      {
	// XXX at the present time, nothing triggers this, and the
	// window the user would get wouldn't be helpful if something
	// did.

	showHelpWindow();
      }
    else if (command.equals(java_version_action))
      {
	Thread thread = new Thread(new Runnable() {
	  public void run() {
	    showJavaVersion();
	  }});
	thread.setPriority(Thread.NORM_PRIORITY);
	thread.start();
      }
    else if (command.equals(about_action))
      {
	Thread thread = new Thread(new Runnable() {
	  public void run() {
	    showAboutMessage();
	  }});
	thread.setPriority(Thread.NORM_PRIORITY);
	thread.start();
      }
    else if (command.equals(motd_action))
      {
	Thread thread = new Thread(new Runnable() {
	  public void run() {
	    showMOTD();
	  }});
	thread.setPriority(Thread.NORM_PRIORITY);
	thread.start();
      }
    else
      {
	// "Unknown action event generated in Ganymede client.. client error?"
	setStatus(ts.l("actionPerformed.unknown"));
      }
  }

  /**
   * Pop up the query box
   */

  void postQuery(BaseDump base)
  {
    if (my_querybox == null)
      {
	// "Query Panel"
	my_querybox = new querybox(base, this, this, ts.l("postQuery.query_title"));
      }
    else if (my_querybox.isVisible())
      {
	return;
      }
    
    if (base != null)
      {
	my_querybox.selectBase(base);
      }

    my_querybox.setVisible(true);
  }

  /**
   * This is a debugging hook, to allow the user to enter an invid in 
   * string form for direct viewing.
   *
   * Since this is just for debugging, I haven't bothered to localize
   * this method.
   */

  void openAnInvid()
  {
    DialogRsrc r = new DialogRsrc(this,
				  "Open an invid",
				  "This will open an invid by number.  This is for " +
				  "debugging purposes only.  Invid's have the format " +
				  "number:number, like 21:423");
    r.addString("Invid number:");
    StringDialog d = new StringDialog(r);
    
    Hashtable result = d.showDialog();

    /* -- */

    if (result == null)
      {
	if (debug)
	  {
	    System.err.println("Ok, nevermind.");
	  }
	return;
      }

    String invidString = (String)result.get("Invid number:");

    if (invidString == null)
      {
	if (debug)
	  {
	    System.err.println("Ok, nevermind.");
	  }

	return;
      }

    viewObject(Invid.createInvid(invidString));
  }

  public void addTableWindow(Session session, Query query, DumpResult buffer)
  {
    wp.addTableWindow(session, query, buffer);
  }
  
  protected void processWindowEvent(WindowEvent e) 
  {
    if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
	if (debug)
	  {
	    System.err.println("Window closing");
	  }

	if (OKToProceed())
	  {
	    if (debug)
	      {
		System.err.println("It's ok to log out.");
	      }

	    logout();

	    sizer.saveSize(this);

	    super.processWindowEvent(e);
	  }
	else if (debug)
	  {
	    System.err.println("No log out!");
	  }
      }
    else
      {
	super.processWindowEvent(e);
      }
  }

  // Callbacks

  /**
   * This method comprises the JsetValueCallback interface, and is how
   * some data-carrying components notify us when something changes.
   *
   * @see arlut.csd.JDataComponent.JsetValueCallback
   * @see arlut.csd.JDataComponent.JValueObject
   */

  public boolean setValuePerformed(JValueObject o)
  {
    if (o instanceof JErrorValueObject)
      {
	showErrorMessage((String)o.getValue());
      }
    else if (o instanceof JSetValueObject && o.getSource() == LandFMenu)
      {
        sizer.saveLookAndFeel();
	SwingUtilities.updateComponentTreeUI(_myglogin);
	
	if (about != null)
	  {
	    SwingUtilities.updateComponentTreeUI(about);
	  }

	if (motd != null)
	  {
	    SwingUtilities.updateComponentTreeUI(motd);
	  }

	if (java_ver_dialog != null)
	  {
	    SwingUtilities.updateComponentTreeUI(java_ver_dialog);
	  }

	if (filterDialog != null)
	  {
	    SwingUtilities.updateComponentTreeUI(filterDialog);
	  }

	if (openDialog != null)
	  {
	    SwingUtilities.updateComponentTreeUI(openDialog);
	  }

	if (createDialog != null)
	  {
	    SwingUtilities.updateComponentTreeUI(createDialog);
	  }

	if (defaultOwnerDialog != null)
	  {
	    SwingUtilities.updateComponentTreeUI(defaultOwnerDialog);
	  }

	if (my_querybox != null)
	  {
	    SwingUtilities.updateComponentTreeUI(my_querybox);
	  }

	if (personaDialog != null)
	  {
	    SwingUtilities.updateComponentTreeUI(personaDialog);
	  }
      }
    else
      {
	if (debug)
	  {
	    System.err.println("I don't know what to do with this setValuePerformed: " + o);
	  }

	return false;
      }

    return true;
  }

  // treeCallback methods

  /**
   * Called when a node is expanded, to allow the
   * user of the tree to dynamically load the information
   * at that time.
   *
   * @param node The node opened in the tree.
   *
   * @see arlut.csd.JTree.treeCanvas
   */

  public void treeNodeExpanded(treeNode node)
  {
    if (node instanceof BaseNode && !((BaseNode) node).isLoaded())
      {
	// "Loading objects for base {0}."
	setStatus(ts.l("treeNodeExpanded.loading", node.getText()), 1);
	setWaitCursor();

	try
	  {
	    refreshObjects((BaseNode)node, true);
	  }
	catch (Exception ex)
	  {
	    // "Exception encountered loading objects for base {0}."
	    processExceptionRethrow(ex, ts.l("treeNodeExpanded.exception_txt", node.getText()));
	  }

	// "Done loading objects for base {0}."
	setStatus(ts.l("treeNodeExpanded.loaded", node.getText()), 1);

	((BaseNode) node).markLoaded();
	setNormalCursor();
      }
  }

  /**
   * Called when a node is closed.
   *
   * @see arlut.csd.JTree.treeCanvas
   */

  public void treeNodeContracted(treeNode node)
  {
  }

  /**
   * Called when an item in the tree is selected
   *
   * @param node The node selected in the tree.
   *
   * @see arlut.csd.JTree.treeCanvas
   */

  public void treeNodeSelected(treeNode node)
  {
    selectedNode = node;
    validate();
  }

  public void treeNodeDoubleClicked(treeNode node)
  {
    if (node instanceof InvidNode)
      {
	viewObject(((InvidNode)node).getInvid());
      }
  }

  /**
   * Called when an item in the tree is unselected
   *
   * @param node The node selected in the tree.
   * @param otherNode If true, this node is being unselected by the selection
   *                         of another node.
   *
   * @see arlut.csd.JTree.treeCanvas
   */
  public void treeNodeUnSelected(treeNode node, boolean otherNode)
  {
    selectedNode = null;
  }

  /**
   *
   * Called when a popup menu item is selected
   * on a treeNode
   *
   * @param node The node selected in the tree.
   *
   * @see arlut.csd.JTree.treeCanvas
   */

  public void treeNodeMenuPerformed(treeNode node,
				    java.awt.event.ActionEvent event)
  {
    boolean treeMenuDebug = false;
    
    if (event.getActionCommand().equals(create_pop_action))
      {
	if (treeMenuDebug)
	  {
	    System.err.println("createMI");
	  }

	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;

	    short id = baseN.getTypeID().shortValue();
	    
	    createObject(id);
	  }
	else
	  {
	    System.err.println("not a base node, can't create");
	  }
      }
    else if ((event.getActionCommand().equals(report_edit_pop_action)) ||
	     (event.getActionCommand().equals(report_pop_action)))
      {
	if (treeMenuDebug)
	  {
	    System.err.println("viewMI/viewAllMI");
	  }

	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;

	    Query listQuery = null;

	    if (event.getActionCommand().equals(report_edit_pop_action))
	      {
		listQuery = baseN.getEditableQuery();
	      }
	    else
	      {
		listQuery = baseN.getAllQuery();
	      }

	    // we still want to filter

	    listQuery.setFiltered(true);

	    // inner classes can only refer to final method variables,
	    // so we'll make some final references to keep our inner
	    // class happy.

	    final Query q = listQuery;
	    final gclient thisGclient = this;
	    final String tempText = node.getText();

	    Thread t = new Thread(new Runnable() {
	      public void run() {
		
		thisGclient.wp.addWaitWindow(this);
		DumpResult buffer = null;

		try
		  {
		    try
		      {
			buffer = thisGclient.getSession().dump(q);
		      }
		    catch (Exception ex)
		      {
			processExceptionRethrow(ex);
		      }
		    catch (Error ex)
		      {
			// "Could not complete query.  We may have run out of memory in the client.\n\n{0}"
			new JErrorDialog(thisGclient,
					 ts.l("treeNodeMenuPerformed.error", ex.getMessage()));
			throw ex;
		      }
		    
		    if (buffer == null)
		      {
			// "No results found from query operation on base {0}."
			setStatus(ts.l("treeNodeMenuPerformed.empty_results", tempText),2);
		      }
		    else
		      {
			// "Results returned from server query on base {0} - building table widget."
			setStatus(ts.l("treeNodeMenuPerformed.results", tempText), 1);
		    
			thisGclient.wp.addTableWindow(thisGclient.getSession(), q, buffer);
		      }
		  }
		finally
		  {
		    thisGclient.wp.removeWaitWindow(this);
		  }
	      }});

	    t.setPriority(Thread.NORM_PRIORITY);
	    t.start();

	    // "Sending query on base {0} to server."	    
	    setStatus(ts.l("treeNodeMenuPerformed.sending", node.getText()), 0);
	  }
	else
	  {
	    System.err.println("viewMI from a node other than a BaseNode");
	  }
      }
    else if (event.getActionCommand().equals(query_pop_action))
      {
	if (treeMenuDebug)
	  {
	    System.err.println("queryMI");
	  }

	if (node instanceof BaseNode)
	  {
	    setWaitCursor();
	    BaseDump base = (BaseDump)((BaseNode) node).getBase();
	    setNormalCursor();

	    postQuery(base);
	  }
      }
    else if (event.getActionCommand().equals(show_pop_action) && (node instanceof BaseNode))
      {
	BaseNode bn = (BaseNode) node;

	/* -- */

	if (treeMenuDebug)
	  {
	    System.err.println("show all objects");
	  }

	setWaitCursor();
	
	try
	  {
	    bn.showAll(true);
	    node.setMenu(bn.canCreate() ? pMenuAllCreatable : pMenuAll);

	    if (bn.isOpen())
	      {
		try
		  {
		    if (treeMenuDebug)
		      {
			System.err.println("Refreshing objects");
		      }

		    refreshObjects(bn, true);
		  }
		catch (Exception ex)
		  {
		    processExceptionRethrow(ex);
		  }
	      }
	  }
	finally
	  {
	    setNormalCursor();
	  }
      }
    else if (event.getActionCommand().equals(hide_pop_action) && (node instanceof BaseNode))
      {
	BaseNode bn = (BaseNode) node;

	/* -- */

	bn.showAll(false);
	bn.setMenu(bn.canCreate() ? pMenuEditableCreatable : pMenuEditable);

	if (bn.isOpen())
	  {
	    // this makes the ratchet operation in refreshObjects() faster
	    // in the common case where there are many more editables than
	    // non-editables.

	    tree.removeChildren(bn, false);
	    tree.expandNode(bn, false);
	    
	    try
	      {
		refreshObjects(bn, true);
	      }
	    catch (Exception ex)
	      {
		processExceptionRethrow(ex);
	      }
	  }
      }
    else if (event.getActionCommand().equals(view_pop_action))
      {
	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;

	    viewObject(invidN.getInvid(), invidN.getTypeText());
	  }
      }
    else if (event.getActionCommand().equals(edit_pop_action))
      {
	if (treeMenuDebug)
	  {
	    System.err.println("objEditMI");
	  }

	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;

	    editObject(invidN.getInvid(), invidN.getTypeText(), null);
	  }
      }
    else if (event.getActionCommand().equals(clone_pop_action))
      {
	if (treeMenuDebug)
	  {
	    System.err.println("objCloneMI");
	  }

	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;

	    cloneObject(invidN.getInvid());
	  }
      }
    else if (event.getActionCommand().equals(delete_pop_action))
      {
	// Need to change the icon on the tree to an X or something to show that it is deleted

	if (treeMenuDebug)
	  {
	    System.err.println("Deleting object");
	  }

	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;
	    Invid invid = invidN.getInvid();

            if ((event.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) != 0)
              {
                deleteObject(invid, false); // don't prompt for confirmation
              }
            else
              {
                deleteObject(invid, true);
              }
	  }
      }
    else if (event.getActionCommand().equals(inactivate_pop_action))
      {
	if (treeMenuDebug)
	  {
	    System.err.println("objInactivateMI");
	  }

	if (node instanceof InvidNode)
	  {
	    inactivateObject(((InvidNode)node).getInvid());
	  }
      }
    else if (event.getActionCommand().equals(reactivate_pop_action))
      {
	if (treeMenuDebug)
	  {
	    System.err.println("Reactivate item.");
	  }

	if (node instanceof InvidNode)
	  {
	    reactivateObject(((InvidNode)node).getInvid());
	  }
      }
    else
      {
	System.err.println("Unknown MI chosen");
      }
  }

  // Utilities

  /**
   * sort a vector of listHandles
   *
   * @param v Vector to be sorted
   * @return Vector of sorted listHandles(sorted by label)
   */

  public Vector sortListHandleVector(Vector v)
  {
    (new VecQuickSort(v, 
		      new Comparator() {
      public int compare(Object a, Object b) 
	{
	  listHandle aF, bF;
	  
	  aF = (listHandle) a;
	  bF = (listHandle) b;
	  int comp = 0;
	  
	  comp =  aF.toString().compareToIgnoreCase(bF.toString());
	  
	  if (comp < 0)
	    {
	      return -1;
	    }
	  else if (comp > 0)
	    { 
	      return 1;
	    } 
	  else
	    { 
	      return 0;
	    }
	}
    })).sort();
    
    return v;
  }

  /**
   * Sort a vector of Strings
   *
   * @return Vector of sorted Strings.
   */

  public Vector sortStringVector(Vector v)
  {
    new VecQuickSort(v, null).sort();
    
    return v;
  }

  /**
   * This method does all the clean up required to let garbage
   * collection tear everything completely down.
   *
   * This method must be called from the Java GUI thread.
   */

  public void cleanUp()
  {
    if (debug)
      {
	System.err.println("gclient.cleanUp()");
      }

    this.removeAll();

    client = null;

    session = null;
    _myglogin = null;
    dump = null;
    currentPersonaString = null;
    emptyBorder5 = null;
    emptyBorder10 = null;
    raisedBorder = null;
    loweredBorder = null;
    lineBorder = null;
    statusBorder = null;
    statusBorderRaised = null;

    if (changedHash != null)
      {
	changedHash.clear();
	changedHash = null;
      }

    if (deleteHash != null)
      {
	deleteHash.clear();
	deleteHash = null;
      }
    
    if (createHash != null)
      {
	createHash.clear();
	createHash = null;
      }

    if (createdObjectsWithoutNodes != null)
      {
	createdObjectsWithoutNodes.clear();
	createdObjectsWithoutNodes = null;
      }

    if (shortToBaseNodeHash != null)
      {
	shortToBaseNodeHash.clear();
	shortToBaseNodeHash = null;
      }

    if (invidNodeHash != null)
      {
	invidNodeHash.clear();
	invidNodeHash = null;
      }

    if (cachedLists != null)
      {
	cachedLists.clearCaches();
	cachedLists = null;
      }

    if (loader != null)
      {
	loader.cleanUp();
	loader = null;
      }

    help = null;
    motd = null;
    about = null;

    if (personae != null)
      {
	personae.setSize(0);
	personae = null;
      }

    if (ownerGroups != null)
      {
	ownerGroups.setSize(0);
	ownerGroups = null;
      }

    toolBar = null;

    if (filterDialog != null)
      {
	filterDialog.dispose();
	filterDialog = null;
      }

    if (personaDialog != null)
      {
	personaDialog.dispose();
	personaDialog = null;
      }

    if (defaultOwnerDialog != null)
      {
	defaultOwnerDialog.dispose();
	defaultOwnerDialog = null;
      }

    if (openDialog != null)
      {
	openDialog.dispose();
	openDialog = null;
      }

    if (createDialog != null)
      {
	createDialog.dispose();
	createDialog = null;
      }

    images = null;
    commit = null;
    cancel = null;

    if (statusPanel != null)
      {
	statusPanel.removeAll();
	statusPanel = null;
      }

    buildLabel = null;
    tree = null;
    selectedNode = null;

    errorImage = null;
    questionImage = null;
    search = null;
    queryIcon = null;
    cloneIcon = null;
    pencil = null;
    personaIcon = null;
    inactivateIcon = null;
    treepencil = null;
    trash = null;
    treetrash = null;
    creation = null;
    treecreation = null;
    newToolbarIcon = null;
    ganymede_logo = null;
    createDialogImage = null;

    idleIcon = null;
    buildIcon = null;
    buildIcon2 = null;
    
    wp.closeAll(true);
    wp = null;

    objectViewPM = null;
    objectReactivatePM = null;
    objectInactivatePM = null;
    objectRemovePM = null;

    pMenuAll = null;
    pMenuEditable= null;
    pMenuEditableCreatable = null;
    pMenuAllCreatable = null;

    menubar = null;

    logoutMI = null;
    clearTreeMI = null;
    filterQueryMI = null;
    defaultOwnerMI = null;
    showHelpMI = null;
    toggleToolBarMI = null;

    hideNonEditablesMI = null;

    changePersonaMI = null;
    editObjectMI = null;
    viewObjectMI = null;
    createObjectMI = null;
    deleteObjectMI = null;
    inactivateObjectMI = null;
    menubarQueryMI = null;

    my_username = null;

    if (actionMenu != null)
      {
	actionMenu.removeAll();
	actionMenu = null;
      }

    if (windowMenu != null)
      {
	windowMenu.removeAll();
	windowMenu = null;
      }

    if (fileMenu != null)
      {
	fileMenu.removeAll();
	fileMenu = null;
      }

    if (helpMenu != null)
      {
	helpMenu.removeAll();
	helpMenu = null;
      }

    if (PersonaMenu != null)
      {
	PersonaMenu.removeAll();
	PersonaMenu = null;
      }

    if (LandFMenu != null)
      {
	LandFMenu.removeAll();
	LandFMenu = null;
      }

    personaListener = null;

    if (my_querybox != null)
      {
	my_querybox.dispose();
	my_querybox = null;
      }

    if (statusThread != null)
      {
	try
	  {
	    statusThread.shutdown();
	  }
	catch (NullPointerException ex)
	  {
	  }

	statusThread = null;
      }

    if (securityThread != null)
      {
	try
	  {
	    securityThread.shutdown();
	  }
	catch (NullPointerException ex)
	  {
	  }

	securityThread = null;
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 PersonaListener

------------------------------------------------------------------------------*/

/**
 * Listener class to handle interaction with the client's persona selection
 * dialog.
 */

class PersonaListener implements ActionListener {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.PersonaListener");

  Session session;

  gclient
    gc;

  // ---

  PersonaListener(Session session, gclient parent)
  {
    this.session = session;
    this.gc = parent;
  }

  public void actionPerformed(ActionEvent event)
  {
    // Check to see if we need to commit the transaction first.
    
    String newPersona = null;
    
    if (event.getSource() instanceof JRadioButton)
      {
	newPersona = event.getActionCommand();
	gc.getPersonaDialog().updatePassField(newPersona);
      }
    else if (event.getSource() instanceof JButton)
      {    
	newPersona = gc.getPersonaDialog().getNewPersona();

	if (!gc.getPersonaDialog().requirePassword && newPersona.equals(gc.currentPersonaString))
	  {
	    return;
	  }

	// Deal with trying to change w/ uncommitted transactions
	if (gc.getSomethingChanged() && !gc.getPersonaDialog().requirePassword)
	  {
	    // "Changing Personae"
	    // "Changing personae requires that the current transaction be closed out.\n\nWould you like to commit this transaction now?"
	    // "Yes, Commit My Changes"
	    // "No, Never Mind"
	    StringDialog d = new StringDialog(gc,
					      ts.l("actionPerformed.commit_dialog_subj"),
					      ts.l("actionPerformed.commit_dialog_txt"),
					      ts.l("actionPerformed.commit_dialog_yes"),
					      ts.l("actionPerformed.commit_dialog_no"));
	    Hashtable result = d.showDialog();
	    
	    if (result == null)
	      {
		// "Persona Change Canceled"
		gc.setStatus(ts.l("actionPerformed.canceled"));
		return;
	      }
	    else
	      {
		// "Committing Transaction"
		gc.setStatus(ts.l("actionPerformed.committing"));
		gc.commitTransaction();
	      }
	  }
	
	// Now change the persona
	
	String password = null;
	
	// All admin level personae have a : in them.  Only admin level
	// personae need passwords, unless we are forcing a password
	
	if (gc.getPersonaDialog().requirePassword || newPersona.indexOf(":") > 0)
	  {
	    password = gc.getPersonaDialog().getPasswordField();
	  }

	if (!setPersona(newPersona, password))
	  {
	    if (gc.getPersonaDialog().requirePassword)
	      {
		return;
		//		gc.showErrorMessage("Wrong password"); 
	      }
	    else
	      {
		// "Persona Change Failed
		// "Your attempt to change personae was
		// unsuccessful.\n\nThis is probably due to your
		// password being entered incorrectly."

		gc.showErrorMessage(ts.l("actionPerformed.error_subj"),
				    ts.l("actionPerformed.error_txt"));
	      }
	  }
	else
	  {
	    gc.getPersonaDialog().changedOK = true;
	    gc.getPersonaDialog().setHidden(true);
	  }
      }
  }

  public synchronized boolean setPersona(String newPersona, String password)
  {
    boolean personaChangeSuccessful = false;	

    try
      {
	personaChangeSuccessful = session.selectPersona(newPersona, password);

	// when we change personae, we lose our filter.  Clear the
	// reference to our filterDialog so that we will recreate it
	// from scratch if we need to.

	gc.filterDialog = null;

	if (personaChangeSuccessful)
	  {
	    gc.setWaitCursor();

            glogin.active_username = newPersona;
            glogin.active_passwd = password;

	    gc.createDialog = null;
	    JDefaultOwnerDialog.clear(); // forget our default owner groups selection

	    // "Changing Persona"
	    gc.setStatus(ts.l("setPersona.changing_status"));

	    // "Ganymede Client: {0} logged in"

	    gc.setTitle(ts.l("setPersona.new_window_title", newPersona));

	    // List of creatable object types might have changed.
		
	    gc.ownerGroups = null;
	    gc.clearCaches();
	    gc.loader.clear();  // This reloads the hashes on a new background thread
	    gc.cancelTransaction();
	    gc.buildTree();
	    gc.currentPersonaString = newPersona;
	    gc.defaultOwnerChosen = false; // force a new default owner to be chosen

	    try
	      {
		gc.ownerGroups = session.getOwnerGroups().getListHandles();

		if (gc.ownerGroups.size() == 1)
		  {
		    gc.chooseDefaultOwner(false);
		    gc.defaultOwnerMI.setEnabled(false);
		  }
		else if (gc.ownerGroups.size() > 1)
		  {
		    gc.defaultOwnerMI.setEnabled(true);
		  }
		else if (gc.ownerGroups.size() == 0)
		  {
		    gc.defaultOwnerMI.setEnabled(false);
		  }
	      }
	    catch (RemoteException ex)
	      {
		gc.processExceptionRethrow(ex);
	      }

	    gc.setNormalCursor();

	    // "Successfully changed persona to {0}."		
	    gc.setStatus(ts.l("setPersona.changed_status", newPersona));

	    return true;
	  }
	else
	  {
	    return false;
	  }
      }
    catch (Exception rx)
      {
	// "Exception encountered trying to change persona to {0}:\n"
	gc.processException(rx, ts.l("setPersona.exception_txt", newPersona));
	return false;
      }
  }

  public void softTimeOutHandler()
  {
    setPersona(gc.my_username, null);
    gc.changePersona(true);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       CacheInfo

------------------------------------------------------------------------------*/

/**
 * Client-side cache object, used by the
 * {@link arlut.csd.ganymede.client.gclient gclient} class to track object status for
 * nodes in the client tree display.
 */

class CacheInfo {

  private String
    originalLabel,
    currentLabel;

  private Short
    baseID;

  private ObjectHandle
    originalHandle = null,
    handle;

  private static final boolean debug = false;

  /* -- */

  public CacheInfo(Short baseID, String originalLabel, String currentLabel)
  {
    this(baseID, originalLabel, currentLabel, null);
  }

  public CacheInfo(Short baseID, String originalLabel, String currentLabel, ObjectHandle handle)
  {
    this(baseID, originalLabel, currentLabel, handle, null);

    if (handle != null)
      {
	try
	  {
	    originalHandle = (ObjectHandle) handle.clone();

	    if (debug) 
	      {
		System.err.println("a cloned handle.");
	      }
	  }
	catch (Exception x)
	  {
	    originalHandle = null;

	    if (debug)
	      {
		System.err.println("Clone is not supported: " + x);
	      }
	  }
      }
    else
      {
	originalHandle = null;

	if (debug)
	  {
	    System.err.println("a null handle.");
	  }
      }
  }

  public CacheInfo(Short baseID, String originalLabel, 
		   String currentLabel, ObjectHandle handle, ObjectHandle originalHandle)
  {
    this.baseID = baseID;
    this.originalLabel = originalLabel;
    this.currentLabel = currentLabel;
    this.handle = handle;
    this.originalHandle = originalHandle;
  }

  public void setOriginalLabel(String label)
  {
    originalLabel = label;
  }

  public void changeLabel(String newLabel)
  {
    currentLabel = newLabel;
  }
    
  public Short getBaseID()
  {
    return baseID;
  }

  public String getOriginalLabel()
  {
    return originalLabel;
  }

  public String getCurrentLabel()
  {
    return currentLabel;
  }

  public ObjectHandle getObjectHandle()
  {
    return handle;
  }

  public ObjectHandle getOriginalObjectHandle()
  {
    return originalHandle;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                               StatusClearThread

------------------------------------------------------------------------------*/

/**
 * Background thread designed to clear the status label in 
 * {@link arlut.csd.ganymede.client.gclient gclient}
 * some seconds after the setClock() method is called.
 */

class StatusClearThread extends Thread {

  final static boolean debug = false;

  boolean done = false;
  boolean resetClock = false;

  private String defaultMessage = "";

  JLabel statusLabel;

  int sleepSecs = 0;

  /* -- */

  public StatusClearThread(JLabel statusLabel)
  {
    this.statusLabel = statusLabel;
  }

  public synchronized void run()
  {
    while (!done)
      {
	if (debug)
	  {
	    System.err.println("StatusClearThread.run(): entering loop");
	  }

	resetClock = false;

	try
	  {
	    if (sleepSecs > 0)
	      {
		if (debug)
		  {
		    System.err.println("StatusClearThread.run(): waiting " + sleepSecs + " seconds");
		  }

		wait(sleepSecs * 1000);
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("StatusClearThread.run(): waiting indefinitely");
		  }

		wait();
	      }
	  }
	catch (InterruptedException ex)
	  {
	  }

	if (!resetClock && !done)
	  {
	    // this has to be invokeLater or else we'll risk getting
	    // deadlocked.

	    if (debug)
	      {
		System.err.println("StatusClearThread.run(): invoking label clear");
	      }

	    EventQueue.invokeLater(new Runnable() {
	      public void run() {
		statusLabel.setText(defaultMessage);
		statusLabel.paintImmediately(statusLabel.getVisibleRect());
	      }
	    });

	    sleepSecs = 0;
	  }
      }
  }

  public synchronized void setDefaultMessage(String message)
  {
    this.defaultMessage = message;

    if (sleepSecs == 0)
      {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
              statusLabel.setText(defaultMessage);
              statusLabel.paintImmediately(statusLabel.getVisibleRect());
            }
          });
      }
  }

  /**
   * This method resets the clock in the StatusClearThread, such that
   * the status label will be cleared in countDown seconds, unless
   * another setClock follows on closely enough to interrupt the
   * countdown, effectively.
   *
   * @param countDown seconds to wait before clearing the status field.  If
   * countDown is zero or negative, the timer will suspend until a later
   * call to setClock sets a positive countdown.
   */

  public synchronized void setClock(int countDown)
  {
    if (debug)
      {
	System.err.println("StatusClearThread.setClock(" + countDown + ")");
      }

    resetClock = true;
    sleepSecs = countDown;
    notifyAll();

    if (debug)
      {
	System.err.println("StatusClearThread.setClock(" + countDown + ") - done");
      }
  }

  /**
   * This method causes the run() method to gracefully terminate
   * without taking any further action.
   */

  public synchronized void shutdown()
  {
    this.done = true;
    notifyAll();
  }
}


/*------------------------------------------------------------------------------
                                                                           class
                                                           SecurityLaunderThread

------------------------------------------------------------------------------*/

/**
 * Background client thread designed to launder build status messages
 * from the server on a non-RMI thread.  We do this so that RMI calls
 * from the server are granted permission to put events on the GUI
 * thread for apropriately synchronized icon setitng.  Set up and torn
 * down by the {@link arlut.csd.ganymede.client.gclient gclient}
 * class.
 */

class SecurityLaunderThread extends Thread {

  gclient client;
  boolean done = false;
  boolean messageSet = false;
  int buildPhase = -1;		// unknown

  /* -- */

  public SecurityLaunderThread(gclient client)
  {
    this.client = client;

    // assume we were constructed on the GUI thread by the main
    // gclient constructor

    switch (client.getBuildPhase())
      {
      case 0:
	client.buildLabel.setIcon(client.idleIcon);
	break;
	
      case 1:
	client.buildLabel.setIcon(client.buildIcon);
	break;
	
      case 2:
	client.buildLabel.setIcon(client.buildIcon2);
	break;
	
      default:
	client.buildLabel.setIcon(client.buildUnknownIcon);
      }
    
    client.buildLabel.validate();
  }

  public synchronized void run()
  {
    while (!done)
      {
	try
	  {
	    wait();
	  }
	catch (InterruptedException ex)
	  {
	  }

	if (messageSet)
	  {
	    EventQueue.invokeLater(new Runnable() {
	      public void run() {

		switch (buildPhase)
		  {
		  case 0:
		    client.buildLabel.setIcon(client.idleIcon);
		    break;
		    
		  case 1:
		    client.buildLabel.setIcon(client.buildIcon);
		    break;

		  case 2:
		    client.buildLabel.setIcon(client.buildIcon2);
		    break;

		  default:
		    client.buildLabel.setIcon(client.buildUnknownIcon);
		  }
		  
		client.buildLabel.validate();
	      }
	    });	

	    messageSet = false;
	  }
      }

    // done!

    client = null;
  }

  /**
   * This method is called to trigger a build status icon update.
   * Called by {@link arlut.csd.ganymede.client.gclient#setBuildStatus(java.lang.String) gclient.setBuildStatus()}.
   */

  public synchronized void setBuildStatus(int phase)
  {
    this.messageSet = true;
    this.buildPhase = phase;

    this.notifyAll();		// wakey-wakey!
  }

  /**
   * This method causes the run() method to gracefully terminate
   * without taking any further action.
   */

  public synchronized void shutdown()
  {
    this.done = true;
    notifyAll();
  }
}

/*
   gclient.java

   Ganymede client main module

   Created: 24 Feb 1997
   Version: $Revision: 1.162 $
   Last Mod Date: $Date: 1999/10/29 17:53:03 $
   Release: $Name:  $

   Module By: Mike Mulvaney, Jonathan Abbey, and Navin Manohar

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

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.beans.PropertyVetoException;

import arlut.csd.JDialog.*;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.JDataComponent.*;
import arlut.csd.Util.*;
import arlut.csd.JTree.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicToolBarUI;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         gclient

------------------------------------------------------------------------------*/

/**
 * <p>Main ganymede client class.  When {@link arlut.csd.ganymede.client.glogin glogin}
 * is run and a user logs in to the server, the client obtains a
 * {@link arlut.csd.ganymede.Session Session} reference that allows it to talk
 * to the server on behalf of a user, and a single instance of this class
 * is created to handle all client GUI and networking operations for that user.</p>
 *
 * <p>gclient creates a {@link arlut.csd.ganymede.client.windowPanel windowPanel}
 * object to contain internal object and query windows on the right side of
 * a Swing JSplitPane.  The left side contains a custom {@link arlut.csd.JTree.treeControl
 * treeControl} GUI component displaying object categories, types, and instances
 * for the user to browse and edit.</p>
 *
 * @version $Revision: 1.162 $ $Date: 1999/10/29 17:53:03 $ $Name:  $
 * @author Mike Mulvaney, Jonathan Abbey, and Navin Manohar
 */

public class gclient extends JFrame implements treeCallback, ActionListener, JsetValueCallback {

  public static boolean debug = false;

  /**
   * we're only going to have one gclient at a time per running client (singleton pattern).
   */

  public static gclient client;

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

  static String release_name = "$Name:  $";
  static String release_date = "$Date: 1999/10/29 17:53:03 $";
  static String release_number = null;

  // ---

  String
    creditsMessage = null,
    aboutMessage = null;

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
   * <p>Vector of {@link arlut.csd.ganymede.BaseDump BaseDump} objects,
   * providing a local cache of {@link arlut.csd.ganymede.Base Base}
   * references that the client consults during operations.</p>
   *
   * <p>Loaded by the {@link arlut.csd.ganymede.client.Loader Loader}
   * thread.</p>
   */

  private Vector baseList;

  /**
   * <p>Cache mapping possibly remote {@link arlut.csd.ganymede.Base Base}
   * references to their title.</p>
   *
   * <p>Loaded by the {@link arlut.csd.ganymede.client.Loader Loader}
   * thread.</p>
   */

  private Hashtable baseNames = null;

  /**
   * <p>Cache mapping possibly remote {@link arlut.csd.ganymede.Base Base}
   * references to field vectors.</p>
   *
   * <p>Loaded by the {@link arlut.csd.ganymede.client.Loader Loader}
   * thread.</p>
   */

  private Hashtable baseHash = null;

  /**
   * <p>Cache mapping Short {@link arlut.csd.ganymede.Base Base} id's to
   * a possibly remote reference to the corresponding {@link arlut.csd.ganymede.Base Base}.</p>
   *
   * <p>Loaded by the {@link arlut.csd.ganymede.client.Loader Loader}
   * thread.</p>
   */

  private Hashtable baseMap = null;

  /** 
   * Cache of {@link arlut.csd.ganymede.Invid invid}'s for objects
   * that might have been changed by the client.  The keys and the
   * values in this hash are the same.  The collection of tree nodes
   * corresponding to invid's listed in changedHash will be refreshed
   * by the client when a server is committed or cancelled.  
   */

  private Hashtable changedHash = new Hashtable();

  /** 
   * Mapping of {@link arlut.csd.ganymede.Invid invid}'s for objects
   * that the client has requested be deleted by the server to
   * {@link arlut.csd.ganymede.client.CacheInfo CacheInfo} objects
   * which hold information about the object used to make decisions
   * about managing the client's tree display.
   */

  private Hashtable deleteHash = new Hashtable();

  /**  
   * Mapping of {@link arlut.csd.ganymede.Invid invid}'s for objects
   * that the client has requested be created by the server to
   * {@link arlut.csd.ganymede.client.CacheInfo CacheInfo} objects
   * which hold information about the object used to make decisions
   * about managing the client's tree display.
   */

  private Hashtable createHash = new Hashtable();

  /**
   * Hash of {@link arlut.csd.ganymede.Invid invid}'s corresponding
   * to objects that have been created by the client but which have not
   * had nodes created in the client's tree display.  Once nodes are
   * created for these objects, the invid will be taken out of this
   * hash and put into createHash.
   */

  private Hashtable createdObjectsWithoutNodes = new Hashtable();

  /** 
   * <p>Cache mapping possibly remote {@link arlut.csd.ganymede.Base Base}
   * references to their object type id in Short form.  This is
   * a holdover from a time when the client didn't create local copies
   * of the server's Base references.</p>
   *
   * <p>Loaded by the {@link arlut.csd.ganymede.client.Loader Loader}
   * thread.</p>
   */

  private Hashtable baseToShort = null;

  /**
   * <p>Hash mapping Short {@link arlut.csd.ganymede.Base Base} id's to
   * the corresponding {@link arlut.csd.ganymede.client.BaseNode BaseNode}
   * displayed in the client's tree display.</p>
   */

  protected Hashtable shortToBaseNodeHash = new Hashtable();

  /**
   * <p>Hash mapping {@link arlut.csd.ganymede.Invid Invid}'s for objects
   * referenced by the client to the corresponding
   * {@link arlut.csd.ganymede.client.InvidNode InvidNode} displayed in the
   * client's tree display.</p>
   */

  protected Hashtable invidNodeHash = new Hashtable();

  /**
   * <p>Hash mapping Short object type id's to Vectors of
   * {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}'s,
   * used by the client to quickly look up information about fields 
   * in order to populate 
   * {@link arlut.csd.ganymede.client.containerPanel containerPanel}'s.</p>
   *
   * <p>This hash is used by
   * {@link arlut.csd.ganymede.client.gclient#getTemplateVector(java.lang.Short) getTemplateVector}.</p>
   */

  protected Hashtable templateHash;

  /**
   * <p>Our main cache, keeps information about all objects we've learned
   * about via {@link arlut.csd.ganymede.QueryResult QueryResult}'s returned
   * to us by the server.</p>
   *
   * <p>We can get QueryResults from the server by doing direct
   * {@link arlut.csd.ganymede.Session#query(arlut.csd.ganymede.Query) query}
   * calls on the server, or by calling choices() on an 
   * {@link arlut.csd.ganymede.invid_field invid_field} or on a
   * {@link arlut.csd.ganymede.string_field string_field}.  Information from
   * both sources may be integrated into this cache.</p>
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
    showToolbar = true,       // Show the toolbar
    somethingChanged = false;  // This will be set to true if the user changes anything
  
  helpPanel
    help = null;

  messageDialog
    motd = null,
    credits = null,
    about = null;

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

  /**
   * Status field at the bottom of the client.
   */
  
  final JTextField
    statusLabel = new JTextField();

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
    search,
    queryIcon,
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
    objectViewPM,
    objectReactivatePM,
    objectInactivatePM,
    objectRemovePM;

  treeMenu 
    pMenuAll = new treeMenu(),
    pMenuEditable= new treeMenu(),
    pMenuEditableCreatable = new treeMenu(),
    pMenuAllCreatable = new treeMenu();
  
  JMenuBar 
    menubar;

  JMenuItem 
    logoutMI,
    clearTreeMI,
    filterQueryMI,
    defaultOwnerMI,
    showHelpMI,
    toggleToolBarMI;

  boolean
    defaultOwnerChosen = false;

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
   * <p>This thread is used to clear the statusLabel after some interval after
   * it is set.</p>
   *
   * <p>Whenever the gclient's
   * {@link arlut.csd.ganymede.client.gclient#setStatus(java.lang.String,int) setStatus}
   * method is called, this thread has a countdown timer started, which will
   * clear the status label if it is not reset by another call to setStatus.</p>
   */

  public StatusClearThread statusThread;

  /**
   * this is true during the handleReturnVal method, while a wizard is
   * active.  If a wizard is active, don't allow the window to close.
   */

  private int wizardActive = 0;

  /* -- */

  /**
   * <p>This is the main constructor for the gclient class.. it handles the
   * interactions between the user and the server once the user has
   * logged in.</p>
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
      mainPanel;   //Everything is in this, so it is double buffered

    /* -- */

    if (release_number == null)
      {
	// cut off leading $Name:  $, clean up whitespace

	if (release_name.length() > 9)
	  {
	    release_name = release_name.substring(6, release_name.length()-1);
	    release_name.trim();

	    // we use ganymede_XXX for our CVS tags

	    if (release_name.indexOf('_') != -1)
	      {
		release_number = release_name.substring(release_name.indexOf('_') + 1, 
							release_name.length());
	      }
	  }

	if (release_number == null)
	  {
	    release_number = "version unknown";
	  }
    
	release_number = release_number + " - " + release_date;
      }

    try
      {
	setTitle("Ganymede Client: " + s.getMyUserName() + " logged in");
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not talk to server: " + rx);
      }

    client = this;

    if (!debug)
      {
	debug = g.debug;
      }

    if (debug)
      {
	System.out.println("Starting client");
      }

    enableEvents(AWTEvent.WINDOW_EVENT_MASK);

    if (s == null)
      {
	throw new IllegalArgumentException("Ganymede Error: Parameter for Session s is null");;
      }

    session = s;
    _myglogin = g;
    my_username = g.getUserName().toLowerCase();
    currentPersonaString = my_username;

    mainPanel = new JPanel(true);
    mainPanel.setLayout(new BorderLayout());

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add("Center", mainPanel);

    templateHash = new Hashtable();

    if (debug)
      {
	System.out.println("Creating menu bar");
      }

    // Make the menu bar

    menubar = new JMenuBar();

    //menubar.setBorderPainted(true);
    
    // File menu

    fileMenu = new JMenu("File");
    fileMenu.setMnemonic('f');
    fileMenu.setDelay(0);

    toggleToolBarMI = new JMenuItem("Toggle Toolbar");
    toggleToolBarMI.setMnemonic('t');
    toggleToolBarMI.addActionListener(this);

    logoutMI = new JMenuItem("Logout");
    logoutMI.setMnemonic('l');
    logoutMI.addActionListener(this);

    clearTreeMI = new JMenuItem("Clear Tree");
    clearTreeMI.setMnemonic('c');
    clearTreeMI.addActionListener(this);

    filterQueryMI = new JMenuItem("Filter Query");
    filterQueryMI.setMnemonic('q');
    filterQueryMI.addActionListener(this);
    defaultOwnerMI = new JMenuItem("Set Default Owner");
    defaultOwnerMI.setMnemonic('d');
    defaultOwnerMI.addActionListener(this);

    fileMenu.add(clearTreeMI);
    fileMenu.add(filterQueryMI);
    fileMenu.add(defaultOwnerMI);
    fileMenu.addSeparator();
    fileMenu.add(logoutMI);

    // Action menu

    actionMenu = new JMenu("Actions");
    actionMenu.setMnemonic('a');

    createObjectMI = new JMenuItem("Create Object");
    createObjectMI.setMnemonic('c');
    createObjectMI.setActionCommand("create new object");
    createObjectMI.addActionListener(this);
    
    editObjectMI = new JMenuItem("Edit Object");
    editObjectMI.setMnemonic('e');
    editObjectMI.setActionCommand("open object for editing");
    editObjectMI.addActionListener(this);

    viewObjectMI = new JMenuItem("View Object");
    viewObjectMI.setMnemonic('v');
    viewObjectMI.setActionCommand("open object for viewing");
    viewObjectMI.addActionListener(this);
    
    deleteObjectMI = new JMenuItem("Delete Object");
    deleteObjectMI.setMnemonic('d');
    deleteObjectMI.setActionCommand("delete an object");
    deleteObjectMI.addActionListener(this);

    inactivateObjectMI = new JMenuItem("Inactivate Object");
    inactivateObjectMI.setMnemonic('i');
    inactivateObjectMI.setActionCommand("inactivate an object");
    inactivateObjectMI.addActionListener(this);

    menubarQueryMI = new JMenuItem("Query");
    menubarQueryMI.setMnemonic('q');
    menubarQueryMI.addActionListener(this);

   // Personae init

    boolean personasExist = false;

    try
      {
	personae = session.getPersonae();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not load personas: " + rx);
      }

    personaListener = new PersonaListener(session, this);

    if ((personae != null) && personae.size() > 1)
      {
	changePersonaMI = new JMenuItem("Change Persona");
	changePersonaMI.setMnemonic('p');
	changePersonaMI.setActionCommand("change persona");
	changePersonaMI.addActionListener(this);
	actionMenu.add(changePersonaMI);
	personasExist = true;
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
	JMenuItem viewAnInvid = new JMenuItem("Show me an Invid");
	viewAnInvid.addActionListener(this);
	actionMenu.addSeparator();
	actionMenu.add(viewAnInvid);
      }

    // windowMenu

    windowMenu = new JMenu("Windows");
    windowMenu.setMnemonic('w');
    windowMenu.add(toggleToolBarMI);
   
    // Look and Feel menu

    LandFMenu = new arlut.csd.JDataComponent.LAFMenu(this);
    LandFMenu.setMnemonic('l');
    LandFMenu.setCallback(this);

    // Help menu

    helpMenu = new JMenu("Help");
    helpMenu.setMnemonic('h');

    // we don't have anything done for help.. disable the help menu for now.

    //    showHelpMI = new JMenuItem("Help");
    //    showHelpMI.setMnemonic('h');  // swing can't handle menu and menuitem with same mnemonic
    //    showHelpMI.addActionListener(this);
    //    helpMenu.add(showHelpMI);
    //
    //    helpMenu.addSeparator();

    // This uses action commands, so you don't need to globally declare these

    JMenuItem showAboutMI = new JMenuItem("About Ganymede");
    showAboutMI.setMnemonic('a');
    showAboutMI.addActionListener(this);
    helpMenu.add(showAboutMI);

    JMenuItem showCreditsMI = new JMenuItem("Credits");
    showCreditsMI.setMnemonic('c');
    showCreditsMI.addActionListener(this);
    helpMenu.add(showCreditsMI);

    JMenuItem showMOTDMI = new JMenuItem("Message of the day");
    showMOTDMI.setMnemonic('m');
    showMOTDMI.addActionListener(this);
    helpMenu.add(showMOTDMI);

    menubar.add(fileMenu);
    menubar.add(LandFMenu);
    menubar.add(actionMenu);
    menubar.add(windowMenu);

    menubar.add(Box.createGlue());
    menubar.add(helpMenu);    
    setJMenuBar(menubar);

    // Create menus for the tree

    pMenuAll.add(new MenuItem("List editable"));
    pMenuAll.add(new MenuItem("List all"));
    pMenuAll.add(new MenuItem("Query"));
    pMenuAll.add(new MenuItem("Hide Non-Editables"));

    pMenuEditable.add(new MenuItem("List editable"));
    pMenuEditable.add(new MenuItem("List all"));
    pMenuEditable.add(new MenuItem("Query"));
    pMenuEditable.add(new MenuItem("Show All Objects"));

    pMenuEditableCreatable.add(new MenuItem("List editable"));
    pMenuEditableCreatable.add(new MenuItem("List all"));
    pMenuEditableCreatable.add(new MenuItem("Create"));
    pMenuEditableCreatable.add(new MenuItem("Query"));
    pMenuEditableCreatable.add(new MenuItem("Show All Objects"));

    pMenuAllCreatable.add(new MenuItem("List editable"));
    pMenuAllCreatable.add(new MenuItem("List all"));
    pMenuAllCreatable.add(new MenuItem("Create"));
    pMenuAllCreatable.add(new MenuItem("Query"));
    pMenuAllCreatable.add(new MenuItem("Hide Non-Editables"));

    if (debug)
      {
	System.out.println("Loading images for tree");
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
    trash = PackageResources.getImageResource(this, "trash.gif", getClass());
    creation = PackageResources.getImageResource(this, "creation.gif", getClass());
    newToolbarIcon = PackageResources.getImageResource(this, "newicon.gif", getClass());
    pencil = PackageResources.getImageResource(this, "pencil.gif", getClass());
    //    inactivateIcon = PackageResources.getImageResource(this, "inactivate.gif", getClass());
    personaIcon = PackageResources.getImageResource(this, "persona.gif", getClass());
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

    tree = new treeControl(new Font("SansSerif", Font.BOLD, 12),
			   Color.black, Color.white, this, images,
			   null);

    tree.setMinimumWidth(200);

    if (debug)
      {
	System.out.println("Adding left and right panels");
      }

    //    Box leftBox = new Box(tree, "Objects");

    leftP = new JPanel(false);
    leftP.setLayout(new BorderLayout());
    leftP.add("Center", tree);
    
    if (!showToolbar)
      {
	leftTop = new JPanel(false);
	leftTop.setBorder(statusBorderRaised);
	
	leftTop.setLayout(new BorderLayout());

	leftTop.add("Center", new JLabel("Objects"));
	
	leftP.add("North", leftTop);
      }

    if (debug)
      {
	System.out.println("Creating pop up menus");
      }

    objectViewPM = new treeMenu();
    objectViewPM.add(new MenuItem("View Object"));

    objectRemovePM = new treeMenu();
    objectRemovePM.add(new MenuItem("View Object"));
    objectRemovePM.add(new MenuItem("Edit Object"));
    objectRemovePM.add(new MenuItem("Clone Object"));
    objectRemovePM.add(new MenuItem("Delete Object"));

    objectInactivatePM = new treeMenu();
    objectInactivatePM.add(new MenuItem("View Object"));
    objectInactivatePM.add(new MenuItem("Edit Object"));
    objectInactivatePM.add(new MenuItem("Clone Object"));
    objectInactivatePM.add(new MenuItem("Delete Object"));
    objectInactivatePM.add(new MenuItem("Inactivate Object"));

    objectReactivatePM = new treeMenu();
    objectReactivatePM.add(new MenuItem("View Object"));
    objectReactivatePM.add(new MenuItem("Edit Object"));
    objectReactivatePM.add(new MenuItem("Clone Object"));
    objectReactivatePM.add(new MenuItem("Delete Object"));
    objectReactivatePM.add(new MenuItem("Reactivate Object"));

    try
      {
	buildTree();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception in buildTree: " + ex);
      }

    // The right panel which will contain the windowPanel

    JPanel rightP = new JPanel(true);
    rightP.setBackground(ClientColor.background);
    rightP.setLayout(new BorderLayout());

    wp = new windowPanel(this, windowMenu);

    rightP.add("Center", wp);
    rightTop = new JPanel(false);
    rightTop.setBorder(statusBorderRaised);
    rightTop.setLayout(new BorderLayout());
    
    toolBar = createToolbar();

    if (showToolbar)
      {
	getContentPane().add("North", toolBar);
      }
    
    commit = new JButton("Commit");
    commit.setEnabled(false);
    commit.setOpaque(true);
    commit.setBackground(Color.lightGray);
    commit.setForeground(Color.black);
    commit.setToolTipText("Click this to commit all changes to database");
    commit.addActionListener(this);

    cancel = new JButton("Cancel");
    cancel.setEnabled(false);
    cancel.setOpaque(true);
    cancel.setBackground(Color.lightGray);
    cancel.setForeground(Color.black);
    cancel.setToolTipText("Click this to cancel all changes");
    cancel.addActionListener(this);

    // Button bar at bottom, includes commit/cancel panel and taskbar

    JPanel bottomButtonP = new JPanel(false);

    bottomButtonP.add(commit);
    bottomButtonP.add(cancel);
    bottomButtonP.setBorder(loweredBorder);

    // Create the pane splitter

    JSplitPane sPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftP, rightP);
   
    mainPanel.add("Center",sPane);

    // Create the bottomBar, for the bottom of the window

    JPanel bottomBar = new JPanel(false);
    bottomBar.setLayout(new BorderLayout());

    statusLabel.setEditable(false);
    statusLabel.setOpaque(false);
    statusLabel.setBorder(statusBorder);

    statusThread = new StatusClearThread(statusLabel);
    statusThread.start();

    JLabel l = new JLabel("Status: ");
    JPanel lP = new JPanel(new BorderLayout());
    lP.setBorder(statusBorder);
    lP.add("Center", l);

    bottomBar.add("West", lP);
    bottomBar.add("Center", statusLabel);
    bottomBar.add("East", bottomButtonP);

    mainPanel.add("South", bottomBar);

    setStatus("Starting up");

    try
      {
	ReturnVal rv = session.openTransaction("Ganymede GUI Client");
	rv = handleReturnVal(rv);

	if ((rv != null) && (!rv.didSucceed()))
	  {
	    throw new RuntimeException("Could not open transaction.");
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not open transaction: " + rx);
      }

    // Since we're logged in and have a session established, create the
    // background loader thread to read in object and field type information

    loader = new Loader(session, debug);
    loader.start();

    pack();
    setSize(800, 600);
    show();

    // Adjust size of toolbar buttons to that of largest button
    // Must be done after components are displayed. Otherwise, 
    // getWidth & getHeight return 0's.

    // Not sure about status of "uniform buttons" so
    // using toggle to save having to comment out and in.

    boolean sameSize = true;

    if (sameSize) 
      { 
	int width=0;
	int height=0;

	// Get width/height for biggest button
      
	for (int i = 0; i<toolBar.getComponentCount(); i++) 
	  {
	    JButton b = (JButton)toolBar.getComponent(i);
	
	    int temp = b.getWidth();

	    if (temp > width) 
	      {
		width = temp;
	      }
	
	    int temp2 = b.getHeight();
	  
	    if (temp2 > height) 
	      {
		height = temp2;
	      }
	  }
      
	Dimension buttonSize = new Dimension(width,height);    

	// Set width/height of all buttons to that of biggest
        
	for (int j = 0; j<toolBar.getComponentCount(); j++) 
	  {
	    JButton b = (JButton)toolBar.getComponent(j);

	    b.setMaximumSize(buttonSize);
	    b.setMinimumSize(buttonSize);
	    b.setPreferredSize(buttonSize);
	  }
      }

    getContentPane().validate();
  }

  /**
   *
   * This method handles the start-up tasks after the gclient
   * has gotten initialized.  Called by glogin.
   * 
   */

  public void start()
  {
    // If user has multiple personas, ask which to start with.

    if ((personae != null)  && personae.size() > 1)
      {
	// changePersona will block until the user does something
	// with the persona selection dialog

	changePersona();
	personaDialog.updatePassField(currentPersonaString);
      }

    // Check for MOTD

    setStatus("Checking MOTD");

    try
      {
	StringBuffer m;

	m = session.getMessageHTML("motd", true);

	if (m != null)
	  {
	    showMOTD(m.toString(), true);
	  }
	else
	  {
	    m = session.getMessage("motd", true);

	    if (m != null)
	      {
		showMOTD(m.toString(), false);
	      }
	  }
      }
    catch ( RemoteException rx)
      {
	throw new RuntimeException("Could not get motd: " + rx);
      }
    
    setStatus("Ready.", 0);
  }
  
  /**
   * <p>Returns a vector of 
   * {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}'s.</p>
   *
   * @param id Object type id to retrieve field information for.
   */

  public Vector getTemplateVector(short id)
  {
    return getTemplateVector(new Short(id));
  }

  /**
   * <p>Returns a vector of 
   * {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}'s
   * listing fields and field informaton for the object type identified by 
   * id.</p>
   *
   * @param id The id number of the object type to be returned the base id.
   */

  public Vector getTemplateVector(Short id)
  {
    Vector result = null;

    /* -- */

    if (templateHash.containsKey(id))
      {
	result = (Vector) templateHash.get(id);
      }
    else
      {
	try
	  {
	    result = session.getFieldTemplateVector(id.shortValue());
	    templateHash.put(id, result);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get field templates: " + rx);
	  }
      }
    
    return result;
  }

  /**
   * <p>Clears out the client's 
   * {@link arlut.csd.ganymede.client.objectCache objectCache},
   * which holds object labels, and activation status for invid's returned 
   * by various query and {@link arlut.csd.ganymede.db_field db_field} 
   * choices() operations.</p>
   */

  public void clearCaches()
  {
    if (debug)
      {
        System.out.println("Clearing caches");
      }

    cachedLists.clearCaches();
  }

  /**
   * <p>Clears out all the cached data structures refering to bases.  We 
   * need to clear these when our persona changes, as different personas
   * may have a different list of visible bases.</p>
   */

  public void clearLoaderLists()
  {
    baseList = null;
    baseToShort = baseNames = baseHash = baseMap = null;
  }

  /**
   * <p>Gets a list of objects from the server, in
   * a form appropriate for use in constructing a list of nodes in the
   * tree under an object type (object base) folder.</p>
   *
   * <p>This method supports client-side caching.. if the list required
   * has already been retrieved, the cached list will be returned.  If
   * it hasn't, getObjectList() will get the list from the server and
   * save a local copy in an 
   * {@link arlut.csd.ganymede.client.objectCache objectCache}
   * for future requests.</p>
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
		QueryResult qr = session.query(new Query(id.shortValue(), null, false));
		
		if (qr != null)
		  {
		    if (debug)
		      {
			System.out.println("gclient.getObjectList(): augmenting");
		      }
		    
		    objectlist.augmentListWithNonEditables(qr);
		  }
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not do the query: " + rx);
	      }
	    
	    cachedLists.putList(id, objectlist);
	  }
      }
    else
      {
	if (debug)
	  {
	    System.out.println("gclient.getObjectList(" + id + ", " + showAll +
			       ") downloading objectlist from the server.");
	  }

	try
	  {
	    QueryResult qr = session.query(new Query(id.shortValue(), null, !showAll));

	    if (debug)
	      {
		System.out.println("gclient.getObjectList(): caching copy");
	      }
	    
	    objectlist = new objectList(qr);
	    cachedLists.putList(id, objectlist);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get dump: " + rx);
	  }
      }

    return objectlist;
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
   * <p>Loads and returns the error Image for use in client dialogs.</p>
   * 
   * <p>Once the image is loaded, it is cached for future calls to 
   * getErrorImage().</p>
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
   * <p>Loads and returns the question-mark Image for use in client dialogs.</p>
   * 
   * <p>Once the image is loaded, it is cached for future calls to 
   * getQuestionmage().</p>
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
   * <p>Returns {@link arlut.csd.ganymede.client.gclient#baseNames baseNames}.</p>
   *
   * <p>Checks to see if the baseNames was loaded, and if not, it loads it.
   * Always use this instead of trying to access baseNames directly.</p>
   */

  public final Hashtable getBaseNames()
  {
    if (baseNames == null)
      {
	baseNames = loader.getBaseNames();
      }

    return baseNames;
  }

  /**
   * <p>Returns {@link arlut.csd.ganymede.client.gclient#baseList baseList}.</p>
   *
   * <p>Checks to see if the baseList was loaded, and if not, it loads it.
   * Always use this instead of trying to access the baseList
   * directly.</p>
   */

  public final synchronized Vector getBaseList()
  {
    if (baseList == null)
      {
	if (debug)
	  {
	    System.err.println("getBaseList(): retrieving baseList from Loader thread");
	  }

	baseList = loader.getBaseList();
      }

    return baseList;
  }

  /**
   * <p>Returns {@link arlut.csd.ganymede.client.gclient#baseMap baseMap}.</p>
   *
   * <p>Checks to see if the baseMap was loaded, and if not, it loads it.
   * Always use this instead of trying to access the baseMap
   * directly.</p>
   */

  public Hashtable getBaseMap()
  {
    if (baseMap == null)
      {
	if (debug)
	  {
	    System.err.println("getBaseList(): retrieving baseMap from Loader thread");
	  }

	baseMap = loader.getBaseMap();
      }

    return baseMap;
  }

  /**
   * <p>Returns {@link arlut.csd.ganymede.client.gclient#baseToShort baseToShort}.</p>
   *
   * <p>Checks to see if the basetoShort was loaded, and if not, it loads it.
   * Always use this instead of trying to access the baseToShort
   * directly.</p>
   */

  public Hashtable getBaseToShort()
  {
    if (baseToShort == null)
      {
	if (debug)
	  {
	    System.err.println("getBaseList(): retrieving baseToShort hash from Loader thread");
	  }

	baseToShort = loader.getBaseToShort();
      }
    
    return baseToShort;
  }

  /**
   * <p>Returns the type name for a given object.</p>
   *
   * <p>If the loader thread hasn't yet downloaded that information, this
   * method will block until the information is available.</p>
   */

  public String getObjectType(Invid objId)
  {
    try
      {
	Hashtable baseMap = getBaseMap(); // block
	BaseDump base = (BaseDump) baseMap.get(new Short(objId.getType()));

	return base.getName();
      }
    catch (NullPointerException ex)
      {
	return "<unknown>";
      }
  }

  /**
   * <p>Pulls a object handle for an invid out of the
   * client's cache, if it has been cached.</p>
   *
   * <p>If no handle for this invid has been cached, this method
   * will return null.</p>
   */

  public ObjectHandle getObjectHandle(Invid invid, Short type)
  {
    ObjectHandle handle = null;
      
    if (type == null)
      {
	type = new Short(invid.getType());
      }

    handle = null;

    if (cachedLists.containsList(type))
      {
	handle = cachedLists.getList(type).getObjectHandle(invid);
      }

    return handle;
  }

  /**
   * <p>Sets text in the status bar, with a 5 second countdown before
   * the status bar is cleared.</p>
   *
   * @param status The text to display
   */

  public final void setStatus(String status)
  {
    setStatus(status, 5);
  }

  /**
   * <p>Sets text in the status bar, with a defined countdown before
   * the status bar is cleared.</p>
   *
   * @param status The text to display
   * @param timeToLive Number of seconds to wait until clearing the status bar.
   * If zero or negative, the status bar timer will not clear the field until
   * the status bar is changed by another call to setStatus.
   */

  public final synchronized void setStatus(String status, int timeToLive)
  {
    if (debug)
      {
	System.out.println("Setting status: " + status);
      }

    final String fStatus = status;
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	statusLabel.setText(fStatus);
	statusLabel.paintImmediately(statusLabel.getVisibleRect());
      }
    });

    statusThread.setClock(timeToLive);
  }

  /**
   * <p>Returns the node of the object currently selected in the tree, if
   * any.  Returns null if there are no nodes selected in the tree, of
   * if the node selected in the tree is not an object node.</p>
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
   * <p>Show the help window.</p>
   *
   * <p>This might someday take an argument, which would show a starting page
   * or some more specific help.</p>
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
   * Shows the About... dialog.
   */

  public void showAboutMessage()
  {
    if (about == null)
      {
	if (aboutMessage == null)
	  {
	    StringBuffer buffer = new StringBuffer();

	    buffer.append("<head></head>");
	    buffer.append("<h1>Ganymede Directory Management System</h1><p>");
	    buffer.append("Release number: ");
	    buffer.append(release_number);
	    buffer.append("<p>Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.</p>");
	    buffer.append("<p>Ganymede is licensed and distributed under the GNU General Public License ");
	    buffer.append("and comes with ABSOLUTELY NO WARRANTY.</p>");
	    buffer.append("<p>This is free software, and you are welcome to redistribute it ");
	    buffer.append("under the conditions of the GNU General Public License.</p>");
	    buffer.append("<p>Written by Jonathan Abbey, Michael Mulvaney, Navin Manohar, ");
	    buffer.append("Erik Grostic, and Brian O'Mara.</p>");

	    aboutMessage = buffer.toString();
	  }

	about = new messageDialog(this, "About Ganymede",  null);
	about.setHtmlText(aboutMessage);
      }

    about.setVisible(true);
  }

  /**
   * Shows the credits dialog.
   */

  public void showCredits()
  {
    if (credits == null)
      {
	if (creditsMessage == null)
	  {
	    creditsMessage = ("<head></head><h1>Ganymede credits</h1>" +
			      "<p>Ganymede was developed by the Computer Science Division of the Applied " +
			      "Research Laboratories at The University of Texas at Austin</p>" +
			      "<p>The primary designer and author of Ganymede was Jonathan Abbey, " +
			      "jonabbey@arlut.utexas.edu.</p>  <p>Michael Mulvaney, mikem@mail.utexas.edu, "+
			      "developed large portions of the client.</p> <p>Significant portions of the client " +
			      "were initially developed by Navin Manohar.  Erik Grostic and Brian O'Mara " +
			      "contributed code to the client.</p><p>Navin, Erik, and Brian worked on Ganymede " +
			      "while working as student employees at ARL.</p>" +
			      "<p>Dan Scott, dscott@arlut.utexas.edu, oversaw the development " +
			      "of Ganymede and its predecessor, GASH, and provided high-level " +
			      "direction and support.</p><br>" +
			      "<p>The Ganymede web page is currently at " +
			      "<a href=\"http://www.arlut.utexas.edu/gash2\">" +
			      "http://www.arlut.utexas.edu/gash2</a>.</p>");
	  }
	
	credits = new messageDialog(this, "Credits", null);
	credits.setHtmlText(creditsMessage);
      }

    credits.setVisible(true);
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
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get motd: " + rx);
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
	motd = new messageDialog(client, "MOTD", null);
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
   * Pops up an error dialog with the default title.
   */

  public final void showErrorMessage(String message)
  {
    showErrorMessage("Error", message);
  }

  /**
   * Pops up an error dialog.
   */

  public final void showErrorMessage(String title, String message)
  {
    showErrorMessage(title, message, null);
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
	System.out.println("Error message: " + message);
      }

    final gclient gc = this;
    final String Title = title;
    final String Message = message;
    final Image fIcon = icon;

    SwingUtilities.invokeLater(new Runnable() 
			       {
				 public void run()
				   {
				     JErrorDialog d = new JErrorDialog(gc, Title, Message, fIcon);
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
   * <p>Set the cursor to the normal cursor(usually a pointer.)</p>
   *
   * <p>This is dependent on the operating system.</p>
   */

  public void setNormalCursor()
  {
    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  /**
   * <p>This indeicates that something in the database was changed, so
   * cancelling this transaction will have consequences.</p>
   *
   * <p>This should be called whenever the client makes any changes to
   * the database.  That includes creating objects, editting fields of
   * objects, removing objects, renaming, expiring, deleting,
   * inactivating, and so on.  It is very important to call this
   * whenever something might have changed. </p> 
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
	System.out.println("Setting somethingChanged to " + state);
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
   * <p>This method takes a ReturnVal object from the server and, if necessary,
   * runs through a wizard interaction sequence, possibly displaying several
   * dialogs before finally returning a final result code.</p>
   *
   * <p>Use the ReturnVal returned from this function after this function is
   * called to determine the ultimate success or failure of any operation
   * which returns ReturnVal, because a wizard sequence may determine the
   * ultimate result.</p>
   *
   * <p>This method should not be synchronized, since handleReturnVal
   * may pop up modal (thread-blocking) dialogs, and if we we
   * synchronize this, some Swing or AWT code seems to block on our
   * synchronization when we do pop-up dialogs.  It's not any of my
   * code, so I assume that AWT tries to synchronize on the frame when
   * parenting a new dialog.</p> 
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

	    DialogRsrc resource = jdialog.extractDialogRsrc(this);

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
	    
		dialogResults = dialog.DialogShow();
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
			System.out.println("gclient.handleReturnVal(): Sending result to callback: " + dialogResults);
		      }

		    // send the dialog results to the server

		    retVal = retVal.getCallback().respond(dialogResults);

		    if (debug)
		      {
			System.out.println("gclient.handleReturnVal(): Received result from callback.");
		      }
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("Caught remote exception: " + ex.getMessage());
		  }
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("gclient.handleReturnVal(): No callback, breaking");
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
	System.out.println("gclient.handleReturnVal(): Done with wizards, checking retVal for rescan.");
      }

    // Check for objects that need to be rescanned

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
	System.out.println("gclient.handleReturnVal(): Rescanning " + objects.size() + " objects.");
      }
    
    Enumeration invids = objects.elements();

    // Loop over all the invids, and try to find
    // containerPanels for them.
    
    while (invids.hasMoreElements())
      {
	Invid invid = (Invid) invids.nextElement();
		
	if (debug)
	  {
	    System.out.println("gclient.handleReturnVal(): updating invid: " + invid);
	  }

	Enumeration windows = wp.getWindows();

	// Loop over each window

	while (windows.hasMoreElements())
	  {
	    Object o = windows.nextElement();

	    if (o instanceof framePanel)
	      {
		framePanel fp = (framePanel)o;

		if (debug)
		  {
		    System.out.println("Checking framePanel: " + fp.getTitle());
		  }

		// Loop over each containerPanel in the framePanel
		// window.. there may be more than one due to embedded
		// objects

		// we count down here so that we can handle things if
		// the cp.update*() call causes the count of
		// containerPanels in this frame to decrement we'll be
		// able to handle it.

		// if the count of containerPanels increments during this
		// loop, we'll just not see the new panel(s), which is of
		// course just fine.

		for (int i = fp.containerPanels.size() - 1; i >= 0; i--)
		  {
		    if (i > fp.containerPanels.size() - 1)
		      {
			i = fp.containerPanels.size() - 1;
		      }

		    containerPanel cp = (containerPanel) fp.containerPanels.elementAt(i);

		    if (debug)
		      {
			System.out.println("gclient.handleReturnVal(): Checking containerPanel number " + i);
			System.out.println("\tcp.invid= " + cp.getObjectInvid() + 
					   " lookng for: " + invid);
		      }
				
		    if (cp.getObjectInvid().equals(invid))
		      {
			if (debug)
			  {
			    System.out.println("  Found container panel for " + invid +
					       ": " + cp.frame.getTitle());
			  }
			
			if (retVal.rescanAll(invid))
			  {
			    cp.updateAll();
			  }
			else
			  {
			    cp.update(retVal.getRescanList(invid));
			  }

			// Don't break the loop, because there
			// might be multiple containerPanels
			// or multiple windows containing this
			// invid.
		      }
		  }
	      }
	  }
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

    JButton b = new JButton("Create", new ImageIcon(newToolbarIcon));
    b.setMargin(insets);
    b.setActionCommand("create new object");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Create a new object");
    b.addActionListener(this);
    toolBarTemp.add(b);

    b = new JButton("Edit", new ImageIcon(pencil));
    b.setMargin(insets);
    b.setActionCommand("open object for editing");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Edit an object");
    b.addActionListener(this);
    toolBarTemp.add(b);

    b = new JButton("Delete", new ImageIcon(trash));
    b.setMargin(insets);
    b.setActionCommand("delete an object");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Delete an object");
    b.addActionListener(this);
    toolBarTemp.add(b);

    b = new JButton("View", new ImageIcon(search));
    b.setMargin(insets);
    b.setActionCommand("open object for viewing");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("View an object");
    b.addActionListener(this);
    toolBarTemp.add(b);

    b = new JButton("Query", new ImageIcon(queryIcon));
    b.setMargin(insets);
    b.setActionCommand("compose a query");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Compose a query");
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
	b = new JButton("Persona", new ImageIcon(personaIcon));  
	b.setMargin(insets);
	b.setActionCommand("change persona");
	b.setVerticalTextPosition(b.BOTTOM);
	b.setHorizontalTextPosition(b.CENTER);
	b.setToolTipText("Change Persona");
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
   * <p>Clears out the client's tree.</p>
   *
   * <p>All Nodes will be removed, and the Category and BaseNodes will
   * be rebuilt.  No InvidNodes will be added.</P>
   */

  void clearTree()
  {
    tree.clearTree();

    try
      {
	buildTree();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not rebuild tree: " + rx);
      }
  }

  /**
   * <p>This method builds the initial data structures for the object
   * selection tree, using the base information in the baseHash
   * hashtable gained from the {@link arlut.csd.ganymede.client.Loader Loader}
   * thread.</p>
   */

  void buildTree() throws RemoteException
  {
    if (debug)
      {
	System.out.println("gclient.buildTree(): Building tree");
      }

    CategoryTransport transport = session.getCategoryTree();

    // get the category dump, save it

    dump = transport.getTree();

    // remember that we'll want to refresh our base list

    baseList = null;
    
    if (debug)
      {
	System.out.println("gclient.buildTree(): got root category: " + dump.getName());
      }

    try
      {
	recurseDownCategories(null, dump);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Cound't recurse down catagories: " + rx);
      }

    if (debug)
      {
	System.out.println("gclient.buildTree(): Refreshing tree");
      }

    tree.refresh();

    if (debug)
      {
	System.out.println("gclient.buildTree(): Done building tree,");
      }
  }

  /**
   * <p>Recurses down the category tree obtained from the server, loading
   * the client's tree with category and object folder nodes.</p>
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
	    recurseDownCategories(null, (Category) cNode);
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
    else if (debug)
      {
	System.out.println("gclient.insertCategoryNode(): Unknown instance: " + node);
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
   * <p>This method is used to update the list of object nodes under a given
   * base node in our object selection tree, synchronizing the tree with
   * the actual objects on the server.</p>
   *
   * @param node Tree node corresponding to the object type being refreshed
   * in the client's tree.
   * @param doRefresh If true, causes the tree to update its display.
   */
  
  void refreshObjects(BaseNode node, boolean doRefresh) throws RemoteException
  {
    Base base;
    Invid invid = null;
    String label = null;
    Vector vect;
    BaseNode parentNode;
    InvidNode oldNode, newNode, fNode;
    Query _query = null;

    ObjectHandle handle = null;
    Vector objectHandles;
    objectList objectlist = null;

    Short Id;

    /* -- */

    base = node.getBase();    
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

    //parentNode = node;
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
	     ((label.compareTo(fNode.getText())) < 0)))
	  {
	    // If we have an invid/label in the object list that's not
	    // in the tree, we need to insert it

	    InvidNode objNode = new InvidNode(node, 
					      handle.isInactive() ? (label + " (inactive)") :label,
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
			System.out.println("Found this object in the creating objectsWithoutNodes hash: " + 
					   handle.getLabel());
		      }
		    		    
		    createHash.put(invid, new CacheInfo(node.getTypeID(),
							(handle.getLabel() == null) ? "New Object" : handle.getLabel(),
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
		 ((label.compareTo(fNode.getText())) > 0))
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
		fNode.setText(label + " (inactive)");
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
   * <p>Updates the tree for the nodes that might have changed.</p>
   *
   * <p>This method fixes all the icons, removing icons that were
   * marked as to-be-deleted or dropped, and cleans out the various
   * hashes.  Only call this when commit is clicked.  This replaces
   * refreshTree(boolean committed), because all the refreshing to be
   * done after a cancel is now handled in the cancelTransaction()
   * method directly.</p>
   *
   * <p>This method is precisely analagous in function to
   * {@link arlut.csd.ganymede.client.gclient#cleanUpAfterCancel() cleanUpAfterCancel()},
   * except for use after a commit.</p> 
   */

  void refreshTreeAfterCommit() throws RemoteException
  {
    Invid invid = null;
    InvidNode node = null;

    /* -- */

    //
    // First get rid of deleted nodes
    //

    Enumeration deleted = deleteHash.keys();

    while (deleted.hasMoreElements())
      {
	invid = (Invid)deleted.nextElement();
	node = (InvidNode)invidNodeHash.get(invid);

	if (node != null)
	  {
	    if (debug)
	      {
		System.out.println("gclient.refreshTreeAfterCommit(): Deleteing node: " + node.getText());
	      }

	    tree.deleteNode(node, false);
	    invidNodeHash.remove(invid);
	  }
      }
    
    deleteHash.clear();

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
   * <p>Queries the server for status information on a vector of 
   * {@link arlut.csd.ganymede.Invid invid}'s that were touched
   * in some way by the client during the recent transaction.
   * The results from the queries are used to update the icons
   * in the tree.</p>
   *
   * <p>Called by refreshTreeAfterCommit().</p>
   *
   * <p>This method is called from
   * {@link arlut.csd.ganymede.client.gclient#refreshTreeAfterCommit() refreshTreeAfterCommit()}.</p>
   *
   * @param paramVect Vector of invid's to refresh.  
   * @param afterCommit If true, this method will update the client's status
   * bar as it progresses.
   */

  public void refreshChangedObjectHandles(Vector paramVect, boolean afterCommit)
  {
    Enumeration enum;
    Invid invid;
    Short objectTypeKey = null;

    /* -- */

    if (afterCommit)
      {
	setStatus("refreshing object handles after commit");
      }

    try
      {
	QueryResult result = session.queryInvids(paramVect);

	// now get the results
	    
	Vector handleList = result.getHandles();
	    
	// and update anything we've got in the tree
	
	if (afterCommit)
	  {
	    setStatus("Updating object handles in tree");
	  }
	    
	for (int i = 0; i < handleList.size(); i++)
	  {
	    ObjectHandle newHandle = (ObjectHandle) handleList.elementAt(i);
	    invid = newHandle.getInvid();

	    objectTypeKey = new Short(invid.getType());

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
    catch (RemoteException ex)
      {
	setStatus("Couldn't get object handle vector refresh");
      }

    if (afterCommit)
      {
	setStatus("Completed refresh of changed objects in the tree.");
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
   * <p>Updates a database object's icon in the tree display.  This method
   * uses the various client-side caches and hashes to determine the proper
   * icon for the node.</p>
   *
   * <p>This method does not actually induce the tree to refresh itself,
   * and may be called in bulk for a lot of nodes efficiently.</p>
   */

  public void setIconForNode(Invid invid)
  {
    boolean treeNodeDebug = false;

    InvidNode node = (InvidNode) invidNodeHash.get(invid);

    if (node == null)
      {
	return;
      }

    if (node == null)
      {
	if (debug)
	  {
	    System.out.println("gclient.setIconForNode(): There is no node for this invid, silly!");
	  }
      }
    else
      {
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
		System.out.println("Setting icon to delete.");
	      }

	    node.setImages(OPEN_FIELD_DELETE, CLOSED_FIELD_DELETE);
	  }
	else if (createHash.containsKey(invid))
	  {
	    if (treeNodeDebug)
	      {
		System.out.println("Setting icon to create.");
	      }

	    node.setImages(OPEN_FIELD_CREATE, CLOSED_FIELD_CREATE);
	  }
	else if (handle != null)
	  {
	    if (handle.isInactive())
	      {
		if (treeNodeDebug)
		  {
		    System.out.println("inactivate");
		  }

		node.setText(handle.getLabel() + " (inactive)");

		node.setMenu(objectReactivatePM);
		node.setImages(OPEN_FIELD_REMOVESET, CLOSED_FIELD_REMOVESET);
	      }
	    else
	      {
		node.setText(handle.getLabel());

		BaseNode parentNode = (BaseNode) node.getParent();

		if (parentNode.canInactivate())
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
			System.out.println("isExpirationSet");
		      }

		    node.setImages(OPEN_FIELD_EXPIRESET, CLOSED_FIELD_EXPIRESET);
		  }
		else if (handle.isRemovalSet())
		  {
		    if (treeNodeDebug)
		      {
			System.out.println("isRemovalSet()");
		      }

		    node.setMenu(objectReactivatePM);
		    node.setImages(OPEN_FIELD_REMOVESET, CLOSED_FIELD_REMOVESET);
		  }
		else if (changedHash.containsKey(invid))
		  {
		    if (treeNodeDebug)
		      {
			System.out.println("Setting icon to edit.");
		      }
		
		    node.setImages(OPEN_FIELD_CHANGED, CLOSED_FIELD_CHANGED);
		  }
		else // nothing special in handle
		  {
		    node.setImages(OPEN_FIELD, CLOSED_FIELD);
		  } 
	      }
	  }
	else // no handle
	  {
	    if (changedHash.containsKey(invid))
	      {
		if (treeNodeDebug)
		  {
		    System.out.println("Setting icon to edit.");
		  }
		
		node.setImages(OPEN_FIELD_CHANGED, CLOSED_FIELD_CHANGED);
	      }
	    else
	      {
		if (treeNodeDebug)
		  {
		    System.out.println("normal");
		  }

		node.setImages(OPEN_FIELD, CLOSED_FIELD);
	      }
	  }
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
   * <p>Opens a new {@link arlut.csd.ganymede.client.framePanel framePanel} 
   * window to allow the user to edit an object.</p>
   *
   * <p>Use this to edit objects, so gclient can keep track of the
   * caches, tree nodes, and all the other dirty work.  This should be
   * the only place windowPanel.addWindow() is called for editing
   * purposes.</p>
   *
   * @param invid id for the object to be edited in the new window.  */

  public void editObject(Invid invid)
  {
    editObject(invid, null);
  }

  /**
   * <p>Opens a new {@link arlut.csd.ganymede.client.framePanel framePanel}
   * window to allow the user to edit an object.</p>
   *
   * <p>Use this to edit objects, so gclient can keep track of the
   * caches, tree nodes, and all the other dirty work.  This should be
   * the only place windowPanel.addWindow() is called for editing
   * purposes.</p>
   *
   * @param invid id for the object to be edited in the new window.
   * @param objectType String describing the kind of object being edited,
   * used in the titlebar of the window created.
   */

  public void editObject(Invid invid, String objectType)
  {
    if (wp.isOpenForEdit(invid))
      {
	showErrorMessage("That object is already open for editing.");
	return;
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

	wp.addWindow(o, true, objectType);

	InvidNode node = null;

	if (invidNodeHash.containsKey(invid))
	  {
	    node = (InvidNode)invidNodeHash.get(invid);
	  }

	Short type = new Short(invid.getType());
	ObjectHandle handle = getObjectHandle(invid, type);
	  
	changedHash.put(invid, invid);

	// we don't need to do a full refresh of it, since we've just
	// checked it out..

	setIconForNode(invid);
	tree.refresh();
      }
    catch(RemoteException rx)
      {
	throw new RuntimeException("Could not edit object: " + rx);
      }
  }

  /** 
   * <p>Creates a new object on the server and opens a new
   * client {@link arlut.csd.ganymede.client.framePanel framePanel}
   * window to allow the user to edit the new object.</p>
   *
   * @param type Type of object to be created
   */

  public db_object cloneObject(Invid origInvid)
  {
    Invid invid = null;
    db_object obj = null;

    /* -- */

    // if the admin is a member of more than one owner group, ask what
    // owner groups they want new objects to be placed in

    if (!defaultOwnerChosen)
      {
	chooseDefaultOwner(false);
      }
    
    setWaitCursor();

    try
      {
	try
	  {
	    ReturnVal rv = handleReturnVal(session.clone_db_object(origInvid));
	    obj = (db_object) rv.getObject();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Exception creating new object: " + rx);
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
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get invid: " + rx);
	  }

	ObjectHandle handle = new ObjectHandle("New Object", invid, false, false, false, true);
       
	wp.addWindow(obj, true, null, true);

	Short typeShort = new Short(invid.getType());
    
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
   * <p>Creates a new object on the server and opens a new
   * client {@link arlut.csd.ganymede.client.framePanel framePanel}
   * window to allow the user to edit the new object.</p>
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
	chooseDefaultOwner(false);
      }
    
    setWaitCursor();

    try
      {
	try
	  {
	    ReturnVal rv = handleReturnVal(session.create_db_object(type));
	    obj = (db_object) rv.getObject();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Exception creating new object: " + rx);
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
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get invid: " + rx);
	  }

	ObjectHandle handle = new ObjectHandle("New Object", invid, false, false, false, true);
       
	wp.addWindow(obj, true, null, true);

	Short typeShort = new Short(type);
    
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
   * <p>Opens a new {@link arlut.csd.ganymede.client.framePanel framePanel} 
   * window to view the object corresponding to the given invid.</p>
   */

  public void viewObject(Invid invid)
  {
    viewObject(invid, null);
  }

  /**
   * <p>Opens a new {@link arlut.csd.ganymede.client.framePanel framePanel}
   * window to view the object corresponding to the given invid.</p>
   *
   * @param objectType Type of the object to be viewed.. if this is
   * null, the server will be queried to determine the type of object
   * for the title-bar of the view object window.  By providing it
   * here from a local cache, and server-call can be saved.
   */

  public void viewObject(Invid invid, String objectType)
  {
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

	wp.addWindow(object, false, objectType);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not edit object: " + rx);
      }
  }

  /**
   * <p>Marks an object on the server as deleted.  The object will not
   * actually be removed from the database until the transaction is
   * committed.</p>
   *
   * <p>This method does a fair amount of internal bookkeeping to manage
   * the client's tree display, status caching, etc.</p>
   */

  public boolean deleteObject(Invid invid, boolean showDialog)
  {
    ReturnVal retVal;
    boolean ok = false;

    /* -- */

    if (showDialog)
      {
	try
	  {
	    StringDialog d = new StringDialog(this, "Verify deletion", 
					      "Are you sure you want to delete " + 
					      getObjectType(invid) + " " +
					      session.viewObjectLabel(invid) + "?", 
					      "Yes", "No", getQuestionImage());
	    Hashtable result = d.DialogShow();
	    
	    if (result == null)
	      {
		setStatus("Cancelled!");

		return false;
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not verify invid to be inactivated: " + rx);
	  }
      }

    setWaitCursor();

    try
      {
	Short id = new Short(invid.getType());

	if (debug)
	  {
	    System.out.println("Deleting invid= " + invid);
	  }

	// Delete the object

	retVal = session.remove_db_object(invid);

	ok = (retVal == null) ? true : retVal.didSucceed();

	if (retVal != null)
	  {
	    retVal = handleReturnVal(retVal);
	  }

	if (ok)
	  {
	    // InvidNode node = (InvidNode)invidNodeHash.get(invid);

	    // Check out the deleteHash.  If this one is already on there,
	    // then I don't know what to do.  If it isn't, then add a new
	    // cache info.  I guess maybe update the name or something,
	    // if it is on there.

	    CacheInfo info = null;

	    // Take this object out of the cachedLists, if it is in there

	    if (cachedLists.containsList(id))
	      {
		String label = session.viewObjectLabel(invid);

		if (debug)
		  {
		    System.out.println("This base has been hashed.  Removing: " + label);
		  }

		objectList list = cachedLists.getList(id);

		ObjectHandle h = list.getObjectHandle(invid);
		list.removeInvid(invid);

		info = new CacheInfo(id, label, null, h);
	      }
	    else
	      {
		String label = session.viewObjectLabel(invid);
		info = new CacheInfo(id, label, null);
	      }

	    if (deleteHash.containsKey(invid))
	      {
		if (debug)
		  {
		    System.out.println("already deleted, nothing to change, right?");
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

	    setStatus("Object will be deleted when commit is clicked.");
	    somethingChanged();
	  }
	else
	  {
	    setStatus("Delete Failed.");
	  }
      }
    catch(RemoteException rx)
      {
	throw new RuntimeException("Could not delete base: " + rx);
      }
    finally
      {
	setNormalCursor();
      }

    return ok;
  }

  /** 
   * <p>Marks an object on the server as inactivated.  The object will not
   * actually be removed from the database until the transaction is
   * committed.  Note that the inactivation request will typically cause
   * a dialog to come back from the server requesting the user fill in
   * parameters describing how the object is to be inactivated.</p>
   *
   * <p>This method does a fair amount of internal bookkeeping to manage
   * the client's tree display, status caching, etc.</p>
   */

  public boolean inactivateObject(Invid invid)
  {
    boolean ok = false;
    ReturnVal retVal;

    /* -- */

    if (invid == null)
      {
	if (debug)
	  {
	    System.out.println("Canceled");
	  }
      }
    else
      {
	setStatus("inactivating " + invid);
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
		Short type = new Short(invid.getType());
		ObjectHandle handle = getObjectHandle(invid, type);

		// remember that we changed this object for the refreshChangedObjectHandles

		changedHash.put(invid, invid);

		// refresh it now
		
		refreshChangedObject(invid);

		// and update the tree

		tree.refresh();
		setStatus("Object inactivated.");
		somethingChanged();
	      }
	    else
	      {
		setStatus("Could not inactivate object.");
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not verify invid to be inactivated: " + rx);
	  }
	finally
	  {
	    setNormalCursor();
	  }
      }

    return ok;
  }

  /** <p>Reactivates an object that was previously inactivated. The
   * object's status will not actually be changed in the database
   * until the transaction is committed.  Note that the reactivation
   * request will typically cause a dialog to come back from the
   * server requesting the user fill in parameters describing how the
   * object is to be reactivated. </p>
   *
   * <p>Typically reactivating an object involves clearing the removal
   * date from  I think you should
   * call this from the expiration date panel if the date is cleared.
   * 
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
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not reactivate object: " + rx);
      }

    if (ok)
      {
	somethingChanged();
	setStatus("Object reactivated.");

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
   * <p>Opens a dialog to let the user choose an object for editing, and 
   * if cancel is not chosen, the object is opened for editing.</p>
   *
   * <p>If an object node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected node.</p>
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

    openDialog.setText("Open object for editing");
    openDialog.setIcon(new ImageIcon(pencil));
    openDialog.setReturnEditableOnly(true);
    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	if (debug)
	  {
	    System.out.println("Canceled");
	  }
      }
    else
      {
	editObject(invid, openDialog.getTypeString());
      }
  }

  /**
   * <p>Opens a dialog to let the user choose an object for viewing,
   * and if cancel is not chosen, the object is opened for viewing.</p>
   *
   * <p>If an object node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected node.</p>
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

    openDialog.setText("Open object for viewing");
    openDialog.setIcon(new ImageIcon(search));
    openDialog.setReturnEditableOnly(false);

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	if (debug)
	  {
	    System.out.println("Canceled");
	  }
      }
    else
      {
	viewObject(invid);
      }
  }

  /**
   * <p>Opens a dialog to let the user choose an object for inactivation,
   * and if cancel is not chosen, the object is opened for inactivation.</p>
   *
   * <p>If an object node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected node.</p>
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

    openDialog.setText("Choose object to be inactivated");
    openDialog.setIcon(null);
    openDialog.setReturnEditableOnly(true);

    Invid invid = openDialog.chooseInvid();
    
    inactivateObject(invid);
  }

  /**
   * <p>Opens a dialog to let the user choose an object for deletion,
   * and if cancel is not chosen, the object is opened for deletion.</p>
   *
   * <p>If a node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected object.</p>
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

    openDialog.setText("Choose object to be deleted");
    openDialog.setIcon(new ImageIcon(trash));
    openDialog.setReturnEditableOnly(true);

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	if (debug)
	  {
	    System.out.println("Canceled");
	  }
      }
    else
      {
	deleteObject(invid, true);
      }
  }

  /**
   * <p>Creates and presents a dialog to let the user change their selected persona.</p>
   *
   * <p>gclient's personaListener reacts to events from the persona change
   * dialog and will react appropriately as needed.  This method doesn't
   * actually do anything other than display the dialog.</p>
   *
   * <p>PersonaDialog is modal, however, so this method will block until the
   * user makes a choice in the dialog box.</p>
   */

  void changePersona()
  {
    personaDialog = new PersonaDialog(client);
    personaDialog.pack();	// force it to re-center itself.
    personaDialog.setVisible(true); // block
  }

  /**
   * <p>Returns a reference to the most recently created persona dialog.</p>
   */

  PersonaDialog getPersonaDialog()
  {
    return personaDialog;
  }

  /**
   * <p>Logs out from the client.</p>
   *
   * <p>This method does not do any checking, it just logs out.</p>
   */

  void logout()
  {
    _myglogin.logout();

    try 
      {
	this.dispose();
      }
    catch (NullPointerException e)
      {
	// Swing 1.1 complains about this.

	// System.err.println(e + " - logout() tried to remove something that wasn't there.");
      }
  }

  /**
   * <p>Create a custom query filter.</p>
   *
   * <p>The filter is used to limit the output on a query, so that
   * supergash can see the world through the eyes of a less-privileged
   * persona.  This seemed like a good idea at one point, not sure how
   * valuable this really is anymore.</p>
   */

  public void chooseFilter()
  {
    // This could be moved, only cache if filter is changed?

    clearCaches();

    if (filterDialog == null)
      {
	filterDialog = new JFilterDialog(this);
      }
    else
      {
	filterDialog.setVisible(true);
      }

    clearTree();
  }

  /**
   * <p>Chooses the default owner group for a newly created object.</p>
   *
   * <p>This must be called before Session.create_db_object is called.</p>
   */

  public void chooseDefaultOwner(boolean forcePopup)
  {
    ReturnVal retVal = null;
    
    if (ownerGroups == null)
      {
	try
	  {
	    ownerGroups = session.getOwnerGroups().getListHandles();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Can't figure out owner groups: " + rx);
	  }
	if (ownerGroups == null)
	  {
	    throw new RuntimeException("Whoa!  groups is null");
	  }
      }

    if (ownerGroups.size() == 0)
      {
	showErrorMessage("You don't have access to \nany owner groups.");
	return;
      }
    else if (ownerGroups.size() == 1)
      {
	if (!forcePopup) //Otherwise, just show the dialog.
	  {
	    defaultOwnerChosen = true;

	    Vector owners = new Vector();
	    for (int i = 0; i < ownerGroups.size(); i++)
	      {
		owners.addElement(((listHandle)ownerGroups.elementAt(i)).getObject());
	      }
	    try
	      {
		retVal = session.setDefaultOwner(owners);
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not set default owner: " + rx);
	      }
	    return;
	  }
      }
    
    if (defaultOwnerDialog == null)
      {
	defaultOwnerDialog = new JDefaultOwnerDialog(this, ownerGroups);
      }

    retVal = defaultOwnerDialog.chooseOwner();

    handleReturnVal(retVal);

    if ((retVal == null) || (retVal.didSucceed()))
      {
	defaultOwnerChosen =  true;
      }
    else
      {
	defaultOwnerChosen = false;
      }
  }

  /**
   * True if a default owner has already been chosen.
   */

  public boolean defaultOwnerChosen()
  {
    return defaultOwnerChosen;
  }

  /**
   * <p>Check for changes in the database before logging out.</p>
   *
   * <p>This checks to see if anything has been changed.  Basically, if edit panels are
   * open and have been changed in any way, then somethingChanged will be true and 
   * the user will be warned.  If edit panels are open but have not been changed, then
   * it will return true(it is ok to proceed).</p>
   */

  boolean OKToProceed()
  {
    if (wizardActive > 0)
      {
	if (debug)
	  {
	    System.out.println("gclient: wizard is active, not ok to logout.");
	  }

	return false;
      }

    if (getSomethingChanged())
      {
	StringDialog dialog = new StringDialog(this, 
					       "Warning: changes have been made",
					       "You have made changes in objects without commiting " +
					       "those changes.  If you continue, those changes will be lost",
					       "Discard Changes",
					       "Cancel");

	// if DialogShow is null, cancel was clicked So return will be
	// false if cancel was clicked

	return (dialog.DialogShow() != null);
      }
    else
      {
	return true;
      }
  }

  /**
   * <p>Updates the note panels in the open windows.</p>
   *
   * <p>The note panel doesn't have a listener on the TextArea, so when a transaction is
   * committed, this must be called on each notePanel in order to update the server.</p>
   *
   * <p>This basically does a field.setValue(notesArea.getValue()) on each notesPanel.</p>
   */

  void updateNotePanels()
  {
    Vector windows = wp.getEditables();

    for (int i = 0; i < windows.size(); i++)
      {
	if (debug)
	  {
	    System.out.println("Updating window number " + i);
	  }

	framePanel fp = (framePanel)windows.elementAt(i);

	if (fp == null)
	  {
	    if (debug)
	      {
		System.out.println("null frame panel in updateNotesPanels");
	      }
	  }
	else
	  {
	    notesPanel np = fp.getNotesPanel();

	    if (np == null)
	      {
		if (debug)
		  {
		    System.out.println("null notes panel in frame panel");
		  }
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("Calling update notes.");
		  }

		np.updateNotes();
	      }
	  }
      }
  }

  /** 
   * <p>Commits the currently open transaction on the server.  All
   * changes made by the user since the last openNewTransaction() call
   * will be integrated into the database on the Ganymede server.</p>
   *
   * <p>For various reasons, the server may reject the transaction as
   * incomplete.  Usually this will be a non-fatal error.. the user
   * will see a dialog telling him what else needs to be filled out in
   * order to commit the transaction.  In this case,
   * commitTransaction() will have had no effect and the user is free
   * to try again.</p>
   *
   * <p>If the transaction is committed successfully, the relevant
   * object nodes in the tree will be fixed up to reflect their state
   * after the transaction is committed.  commitTransaction() will
   * close all open editing windows, and will call openNewTransaction()
   * to prepare the server for further changes by the user.</p> 
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

	succeeded = (retVal == null) ? true : retVal.didSucceed();

	// if we succeed, we clean up.  If we don't,
	// retVal.doNormalProcessing can be false, in which case the
	// serve aborted our transaction utterly.  If
	// retVal.doNormalProcessing is true, the user can do
	// something to make the transaction able to complete
	// successfully.  In this case, handleReturnVal() will have
	// displayed a dialog telling the user what needs to be done.

	if (succeeded)
	  {
	    setStatus("Transaction successfully committed.");
	    wp.closeEditables();
	
	    wp.refreshTableWindows();

	    openNewTransaction();

	    //
	    // This fixes all the icons in the tree
	    //

	    refreshTreeAfterCommit();

	    wp.resetWindowCount();

	    if (debug)
	      {
		System.out.println("Done committing");
	      }
	  }
	else if (!retVal.doNormalProcessing)
	  {
	    setStatus("Transaction could not successfully commit.");

	    // This is just like a cancel.  Something went wrong, and
	    // the server cancelled our transaction.  We don't need to
	    // call cancelTransaction ourselves.

	    showErrorMessage("Error: commit failed", "Could not commit your changes.");
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not commit transaction" + rx);
      }
    catch (Exception e)
      {
	e.printStackTrace();
	showErrorMessage("Exception during commit: " + e.getMessage());
	throw new RuntimeException("Exception during commit: " + e.getMessage());
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
   * <p>Cancels the current transaction.  Any changes made by the user since
   * the last openNewTransaction() call will be forgotten as if they
   * never happened.  The client's tree display will be reverted to the
   * state it was when the transaction was started, and all open windows
   * will be closed.</p>
   */

  public synchronized void cancelTransaction()
  {
    ObjectHandle handle;
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

	if (retVal == null)
	  {
	    setStatus("Transaction cancelled.");

	    if (debug)
	      {
		System.out.println("Cancel succeeded");
	      }
	  }
	else
	  {
	    if (retVal.didSucceed())
	      {
		setStatus("Transaction cancelled.");
		
		if (debug)
		  {
		    System.out.println("Cancel succeeded");
		  }
	      }
	    else
	      {
		setStatus("Error on server, transaction cancel failed.");

		if (debug)
		  {
		    System.out.println("Everytime I think I'm out, they pull me back in! " +
				       "Something went wrong with the cancel.");
		  }

		return;
	      }
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not talk to server: " + rx);
      }

    cleanUpAfterCancel();

    openNewTransaction();
  }

  /**
   * <p>Cleans up the tree and gclient's caches.</p>
   *
   * <p>This method is precisely analagous in function to
   * {@link arlut.csd.ganymede.client.gclient#refreshTreeAfterCommit() refreshTreeAfterCommit()},
   * except for use after a cancel, when nodes marked as deleted are not removed from the tree,
   * and nodes marked as created are not kept.</p>
   */

  private synchronized void cleanUpAfterCancel()
  {
    ObjectHandle handle;
    Invid invid;
    InvidNode node;
    objectList list;
    CacheInfo info;

    /* -- */

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
		    System.out.println("Can't fool me: you just created this object!");
		  }
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("This one is hashed, sticking it back in.");
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
    
    node = null;
    invid = null;
    list = null;
    info = null;

    // Next up is created list: remove all the added stuff.
    
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
		System.out.println("This one is hashed, taking a created object out.");
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

    createdObjectsWithoutNodes.clear();
    
    // Now go through changed list and revert any names that may be needed

    Vector changedInvids = new Vector();
    Enumeration changed = changedHash.keys();

    while (changed.hasMoreElements())
      {
	changedInvids.addElement(changed.nextElement());
      }

    changedHash.clear();

    refreshChangedObjectHandles(changedInvids, true);

    if (debug && createHash.isEmpty() && deleteHash.isEmpty())
      {
	System.out.println("Woo-woo the hashes are all empty");
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
	ReturnVal rv = session.openTransaction("Ganymede GUI Client");
	
	handleReturnVal(rv);
	if ((rv != null) && (!rv.didSucceed()))
	  {
	    showErrorMessage("Could not open new transaction.");
	  }
	
	tree.refresh();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not open new transaction: " + rx);
      }

    setSomethingChanged(false);
    cancel.setEnabled(false);
    commit.setEnabled(false);

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
   * <p>Handles button and menu picks.  Includes logic for threading
   * out queries and message panels to avoid locking the Java GUI
   * thread.</p>
   */
  
  public void actionPerformed(java.awt.event.ActionEvent event)
  {
    Object source = event.getSource();
    String command = event.getActionCommand();

    /* -- */

    if (debug)
      {
	System.out.println("Action: " + command);
      }

    if (source == cancel)
      {
	if (debug)
	  {
	    System.out.println("cancel button clicked");
	  }
	
	cancelTransaction();
      }
    else if (source == commit)
      {
	if (debug)
	  {
	    System.out.println("commit button clicked");
	  }
	
	commitTransaction();
      }
    else if ((source == menubarQueryMI) || (command.equals("compose a query")))
      {
	if (my_querybox == null)
	  {
	    my_querybox = new querybox(this, this, "Query Panel");
	  }

	// need some final variables for the inner class
	// to use.

	final Query q = my_querybox.myshow();
	final Session s = getSession();
	final gclient thisGclient = this;

	Thread t = new Thread(new Runnable() {
	  public void run() {
	    if (q != null)
	      {
		thisGclient.wp.addWaitWindow(this);
		DumpResult buffer = null;
		
		try
		  {
		    try
		      {
			buffer = s.dump(q);
		      }
		    catch (RemoteException ex)
		      {
			throw new RuntimeException("caught remote: " + ex);
		      }
		    catch (Error ex)
		      {
			new JErrorDialog(thisGclient, 
					 "Could not complete query.. may have run out of memory.\n\n" +
					 ex.getMessage());
			throw ex;
		      }

		    thisGclient.wp.addTableWindow(session, q, buffer, "Query Results");
		  }
		finally
		  {
		    thisGclient.wp.removeWaitWindow(this);
		  }
	      }
	  }});

	t.start();
      }
    else if (source == clearTreeMI)
      {
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
    else if (command.equals("change persona"))
      {
	changePersona();
      }
    else if (command.equals("create new object"))
      {
	createObjectDialog();
      }
    else if (command.equals("open object for editing"))
      {
	editObjectDialog();
      }
    else if (command.equals("open object for viewing"))
      {
	viewObjectDialog();
      }
    else if (command.equals("delete an object"))
      {
	deleteObjectDialog();
      }
    else if (command.equals("inactivate an object"))
      {
	inactivateObjectDialog();
      }
    else if (command.equals("Show me an Invid"))
      {
	openAnInvid();
      }
    else if (command.equals("Filter Query"))
      {
	chooseFilter();
      }
    else if (command.equals("Set Default Owner"))
      {
	chooseDefaultOwner(true);
      }
    else if (command.equals("Help"))
      {
	showHelpWindow();
      }
    else if (command.equals("About Ganymede"))
      {
	Thread thread = new Thread(new Runnable() {
	  public void run() {
	    showAboutMessage();
	  }});
	thread.start();
      }
    else if (command.equals("Credits"))
      {
	Thread thread = new Thread(new Runnable() {
	  public void run() {
	    showCredits();
	  }});
	thread.start();
      }
    else if (command.equals("Message of the day"))
      {
	Thread thread = new Thread(new Runnable() {
	  public void run() {
	    showMOTD();
	  }});
	thread.start();
      }
    else
      {
	System.err.println("Unknown action event generated");
      }
  }

  /**
   * This is a debugging hook, to allow the user to enter an invid in 
   * string form for direct viewing.
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
    
    Hashtable result = d.DialogShow();

    /* -- */

    if (result == null)
      {
	if (debug)
	  {
	    System.out.println("Ok, nevermind.");
	  }
	return;
      }

    String invidString = (String)result.get("Invid number:");

    if (invidString == null)
      {
	if (debug)
	  {
	    System.out.println("Ok, nevermind.");
	  }

	return;
      }

    viewObject(new Invid(invidString));
  }

  public void addTableWindow(Session session, Query query, DumpResult buffer, String title)
  {
    wp.addTableWindow(session, query, buffer, title);
  }
  
  protected void processWindowEvent(WindowEvent e) 
  {
    if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
	if (debug)
	  {
	    System.out.println("Window closing");
	  }

	if (OKToProceed())
	  {
	    if (debug)
	      {
		System.out.println("It's ok to log out.");
	      }

	    logout();
	    super.processWindowEvent(e);
	  }
	else if (debug)
	  {
	    System.out.println("No log out!");
	  }
      }
    else
      {
	super.processWindowEvent(e);
      }
  }

  // Callbacks

  /**
   * <p>This method comprises the JsetValueCallback interface, and is how
   * some data-carrying components notify us when something changes.</p>
   *
   * @see arlut.csd.JDataComponent.JsetValueCallback
   * @see arlut.csd.JDataComponent.JValueObject
   */

  public boolean setValuePerformed(JValueObject o)
  {
    if (o.getOperationType() == JValueObject.ERROR)
      {
	showErrorMessage((String)o.getValue());
      }
    else
      {
	if (debug)
	  {
	    System.out.println("I don't know what to do with this setValuePerformed: " + o);
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
	setStatus("Loading objects for base " + node.getText(), 0);
	setWaitCursor();

	try
	  {
	    refreshObjects((BaseNode)node, true);
	  }
	catch (RemoteException ex)
	  {
	    setStatus("Remote exception loading objects for base " + node.getText());
	    throw new RuntimeException("remote exception in trying to fill this base " + ex);
	  }

	setStatus("Done loading objects for base " + node.getText());

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
   * Called when an item in the tree is unselected
   *
   * @param node The node selected in the tree.
   * @param someNodeSelected If true, this node is being unselected by the selection
   *                         of another node.
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

  public void treeNodeUnSelected(treeNode node, boolean otherNode)
  {
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
    
    if (event.getActionCommand().equals("Create"))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("createMI");
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
    else if ((event.getActionCommand().equals("List editable")) ||
	     (event.getActionCommand().equals("List all")))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("viewMI/viewAllMI");
	  }

	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;

	    Query listQuery = null;

	    if (event.getActionCommand().equals("List editable"))
	      {
		listQuery = baseN.getEditableQuery();
	      }
	    else
	      {
		listQuery = baseN.getAllQuery();
	      }

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
		    catch (RemoteException ex)
		      {
			throw new RuntimeException("caught remote: " + ex);
		      }
		    catch (Error ex)
		      {
			new JErrorDialog(thisGclient, 
					 "Could not complete query.. may have run out of memory.\n\n" +
					 ex.getMessage());
			throw ex;
		      }
		    
		    if (buffer == null)
		      {
			setStatus("No results from list operation on base " + tempText);
		      }
		    else
		      {
			setStatus("List returned from server on base " + tempText +
				  " - building table");
		    
			thisGclient.wp.addTableWindow(thisGclient.getSession(), q,
						      buffer, "Query Results");
		      }
		  }
		finally
		  {
		    thisGclient.wp.removeWaitWindow(this);
		  }
	      }});

	    t.start();
	    
	    setStatus("Sending query for base " + node.getText() + " to server", 0);
	  }
	else
	  {
	    System.out.println("viewMI from a node other than a BaseNode");
	  }
      }
    else if (event.getActionCommand().equals("Query"))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("queryMI");
	  }

	if (node instanceof BaseNode)
	  {
	    setWaitCursor();
	    BaseDump base = (BaseDump)((BaseNode) node).getBase();

	    querybox box = new querybox(base, this,  this, "Query Panel");

	    setNormalCursor();

	    // inner classes can only refer to final method variables,
	    // so we'll make some final references to keep our inner
	    // class happy.

	    final Query q = box.myshow();
	    final gclient thisGclient = this;
	    final String text = node.getText();

	    Thread t = new Thread(new Runnable() {
	      public void run() {
		if (q != null)
		  {
		    thisGclient.wp.addWaitWindow(this);

		    DumpResult buffer = null;
		    
		    try
		      {
			try
			  {
			    thisGclient.setStatus("Sending query for base " + text + " to server", 0);
			
			    buffer = thisGclient.getSession().dump(q);
			  }
			catch (RemoteException ex)
			  {
			    throw new RuntimeException("caught remote: " + ex);
			  }
			catch (Error ex)
			  {
			    new JErrorDialog(thisGclient, 
					     "Could not complete query.. may have run out of memory.\n\n" +
					     ex.getMessage());
			    throw ex;
			  }
		    
			if (buffer != null)
			  {
			    thisGclient.setStatus("Server returned results for query on base " + 
						  text + " - building table");
			
			    thisGclient.addTableWindow(session, q, buffer, "Query Results");
			  }
			else
			  {
			    thisGclient.setStatus("results == null");
			  }
		      }
		    finally
		      {
			thisGclient.wp.removeWaitWindow(this);
		      }
		  }
	      }});
	      
	      t.start();
	  }
      }
    else if (event.getActionCommand().equals("Show All Objects"))
      {
	BaseNode bn = (BaseNode) node;
	Base base = bn.getBase();
	Short id;

	/* -- */

	if (treeMenuDebug)
	  {
	    System.out.println("show all objects");
	  }

	setWaitCursor();
	
	try
	  {
	    id = bn.getTypeID();
	    bn.showAll(true);
	    node.setMenu(((BaseNode)node).canCreate() ? pMenuAllCreatable : pMenuAll);

	    if (bn.isOpen())
	      {
		try
		  {
		    if (treeMenuDebug)
		      {
			System.out.println("Refreshing objects");
		      }

		    refreshObjects(bn, true);
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("oops, couldn't refresh base" + ex);
		  }
	      }
	  }
	finally
	  {
	    setNormalCursor();
	  }
      }
    else if (event.getActionCommand().equals("Hide Non-Editables"))
      {
	BaseNode bn = (BaseNode) node;
	Base base = bn.getBase();
	Short id;

	/* -- */

	id = bn.getTypeID();

	bn.showAll(false);
	bn.setMenu(((BaseNode)node).canCreate() ? pMenuEditableCreatable : pMenuEditable);

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
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("oops, couldn't refresh base" + ex);
	      }
	  }
      }
    else if (event.getActionCommand().equals("View Object"))
      {
	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;

	    if (deleteHash.containsKey(invidN.getInvid()))
	      {
		// This one has been deleted
		showErrorMessage("This object has already been deleted.");
	      }
	    else
	      {
		viewObject(invidN.getInvid(), invidN.getTypeText());
	      }
	  }
	else
	  {
	    System.err.println("not an object node, can't create");
	  }
      }
    else if (event.getActionCommand().equals("Edit Object"))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("objEditMI");
	  }

	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;

	    if (deleteHash.containsKey(invidN.getInvid()))
	      {
		showErrorMessage("This object has already been deleted.");
	      }
	    else
	      {
		Invid invid = invidN.getInvid();
		
		editObject(invid, invidN.getTypeText());
	      }
	  }
	else
	  {
	    System.err.println("not an object node, can't create");
	  }
      }
    else if (event.getActionCommand().equals("Clone Object"))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("objEditMI");
	  }

	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;

	    if (deleteHash.containsKey(invidN.getInvid()))
	      {
		showErrorMessage("This object has already been deleted.");
	      }
	    else
	      {
		Invid invid = invidN.getInvid();
		
		cloneObject(invid);
	      }
	  }
	else
	  {
	    System.err.println("not an object node, can't clone");
	  }
      }
    else if (event.getActionCommand().equals("Delete Object"))
      {
	// Need to change the icon on the tree to an X or something to show that it is deleted

	if (treeMenuDebug)
	  {
	    System.out.println("Deleting object");
	  }

	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;
	    Invid invid = invidN.getInvid();

	    deleteObject(invid, true);
	  }
	else  // Should never get here, but just in case...
	  {
	    System.out.println("Not a InvidNode node, can't delete this.");
	  }
      }
    else if(event.getActionCommand().equals("Inactivate Object"))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("objInactivateMI");
	  }

	if (node instanceof InvidNode)
	  {
	    inactivateObject(((InvidNode)node).getInvid());
	  }
      }
    else if (event.getActionCommand().equals("Reactivate Object"))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("Reactivate item.");
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
		      new arlut.csd.Util.Compare() {
      public int compare(Object a, Object b) 
	{
	  listHandle aF, bF;
	  
	  aF = (listHandle) a;
	  bF = (listHandle) b;
	  int comp = 0;
	  
	  comp =  aF.toString().compareTo(bF.toString());
	  
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

  Session session;

  DialogRsrc
    resource = null;

  gclient
    gc;

  boolean debug = false;

  boolean
    listen = true;

  PersonaListener(Session session, gclient parent)
  {
    this.session = session;
    this.gc = parent;
  }

  public void actionPerformed(ActionEvent event)
  {
    if (debug) { System.out.println("personaListener: action Performed!"); }
    
    // Check to see if we need to commit the transaction first.
    
    String newPersona = null;
    
    if (event.getSource() instanceof JRadioButton)
      {
	if (debug) { System.out.println("From radiobutton"); }
	
	newPersona = event.getActionCommand();
	gc.getPersonaDialog().updatePassField(newPersona);

	if (debug) { System.out.println("radiobutton says: " + newPersona); }
      }

    else if (event.getSource() instanceof JButton)
      {    
	newPersona = gc.getPersonaDialog().getNewPersona();

	if (newPersona.equals(gc.currentPersonaString))
	  {
	    if (debug) {gc.showErrorMessage("You are already in that persona."); }
	    return;
	  }

	// Deal with trying to change w/ uncommitted transactions
	if (gc.getSomethingChanged())
	  {
	    // need to ask: commit, cancel, abort?
	    StringDialog d = new StringDialog(gc,
					      "Changing personas",
					      "Before changing personas, the transaction must " +
					      "be closed.  Would you like to commit your changes?",
					      "Commit",
					      "Cancel");
	    Hashtable result = d.DialogShow();
	    
	    if (result == null)
	      {
		gc.setStatus("Persona change cancelled");
		gc.getPersonaDialog().updatePersonaMenu();
		return;
	      }
	    else
	      {
		gc.setStatus("Committing transaction.");
		gc.commitTransaction();
	      }
	  }
	
	// Now change the persona
	
	boolean personaChangeSuccessful = false;	
	String password = null;
	
	// All admin level personas have a : in them.  Only admin level
	// personas need passwords.
	
	if (newPersona.indexOf(":") > 0)
	  {
	    password = gc.getPersonaDialog().getPasswordField();
	  }
	
	try
	  {	      
	    personaChangeSuccessful = session.selectPersona(newPersona, password);
	    
	    if (personaChangeSuccessful)
	      {
		gc.setWaitCursor();
		gc.setStatus("Changing persona.");
		
		// List of creatable object types might have changed.
		
		gc.createDialog = null;
		gc.setTitle("Ganymede Client: " + newPersona + " logged in.");
		
		gc.ownerGroups = null;
		gc.clearCaches();
		gc.loader.clear();  // This reloads the hashes
		gc.clearLoaderLists();
		gc.cancelTransaction();
		gc.buildTree();
		gc.currentPersonaString = newPersona;
		gc.defaultOwnerChosen = false; // force a new default owner to be chosen
		gc.setNormalCursor();
		
		gc.setStatus("Successfully changed persona to " + newPersona);
		gc.getPersonaDialog().updatePersonaMenu();	
	      }
	    else
	      {
		gc.showErrorMessage("Error: could not change persona", 
				    "Perhaps the password was wrong.", 
				    gc.getErrorImage());
		
		gc.setStatus("Persona change failed");
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not set persona to " + newPersona + ": " + rx);
	  }

	gc.getPersonaDialog().setHidden(true);
      }
    else
      {
	System.out.println("Persona Listener doesn't understand that action.");
      }    
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

  private final boolean debug = false;

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
		System.out.println("a cloned handle.");
	      }
	  }
	catch (Exception x)
	  {
	    originalHandle = null;

	    if (debug)
	      {
		System.out.println("Clone is not supported: " + x);
	      }
	  }
      }
    else
      {
	originalHandle = null;

	if (debug)
	  {
	    System.out.println("a null handle.");
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

  JTextField statusLabel;

  int sleepSecs = 0;

  /* -- */

  public StatusClearThread(JTextField statusLabel)
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

	    SwingUtilities.invokeLater(new Runnable() {
	      public void run() {
		statusLabel.setText("");
		statusLabel.paintImmediately(statusLabel.getVisibleRect());
	      }
	    });

	    sleepSecs = 0;
	  }
      }
  }

  /**
   * <p>This method resets the clock in the StatusClearThread, such that
   * the status label will be cleared in countDown seconds, unless
   * another setClock follows on closely enough to interrupt the
   * countdown, effectively.</p>
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
   * <p>This method causes the run() method to gracefully terminate
   * without taking any further action.</p>
   */

  public synchronized void shutdown()
  {
    this.done = true;
    notifyAll();
  }
}

/*
   gclient.java

   Ganymede client main module

   Created: 24 Feb 1997
   Version: $Revision: 1.111 $ %D%
   Module By: Mike Mulvaney, Jonathan Abbey, and Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin

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

import jdj.*;

import arlut.csd.JDialog.*;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.JDataComponent.*;
import arlut.csd.Util.*;
import arlut.csd.JTree.*;

import javax.swing.*;
import javax.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         gclient

------------------------------------------------------------------------------*/

public class gclient extends JFrame implements treeCallback,ActionListener, JsetValueCallback {

  // we're only going to have one gclient at a time per running client

  public static gclient client;

  // ---

  String
    creditsMessage = null,
    aboutMessage = null;
  
  // Image numbers
  final int NUM_IMAGE = 17;
  
  final int OPEN_BASE = 0;
  final int CLOSED_BASE = 1;

  final int OPEN_FIELD = 2;
  final int OPEN_FIELD_DELETE = 3;
  final int OPEN_FIELD_CREATE = 4;
  final int OPEN_FIELD_CHANGED = 5;
  final int OPEN_FIELD_REMOVESET = 6;
  final int OPEN_FIELD_EXPIRESET = 7;
  final int CLOSED_FIELD = 8;
  final int CLOSED_FIELD_DELETE = 9;
  final int CLOSED_FIELD_CREATE = 10;
  final int CLOSED_FIELD_CHANGED = 11;
  final int CLOSED_FIELD_REMOVESET = 12;
  final int CLOSED_FIELD_EXPIRESET = 13;

  final int OPEN_CAT = 14;
  final int CLOSED_CAT = 15;

  final int OBJECTNOWRITE = 16;

  public static boolean debug = true;

  Session session;
  glogin _myglogin;

  CategoryDump dump;

  // This keeps track of the current persona

  String 
    currentPersonaString;

  // set up a bunch of borders
  // Turns out we don't need to do this anyway, since the BorderFactory does it for us.

  public EmptyBorder
    emptyBorder5 = (EmptyBorder)BorderFactory.createEmptyBorder(5,5,5,5),
    emptyBorder10 = (EmptyBorder)BorderFactory.createEmptyBorder(10,10,10,10);

  public BevelBorder
    raisedBorder = (BevelBorder)BorderFactory.createBevelBorder(BevelBorder.RAISED),
    loweredBorder = (BevelBorder)BorderFactory.createBevelBorder(BevelBorder.LOWERED);
      
  public LineBorder
    lineBorder = (LineBorder)BorderFactory.createLineBorder(Color.black);

  public CompoundBorder
    statusBorder = BorderFactory.createCompoundBorder(loweredBorder, emptyBorder5),
    statusBorderRaised = BorderFactory.createCompoundBorder(raisedBorder, emptyBorder5);

  //
  // Yum, caches
  //

  private Vector
    containerPanels = new Vector(),
    baseList;            // List of base types.  Vector of Bases.

  private Hashtable
    baseNames = null,                // used to map Base -> Base.getName(String)
    baseHash = null,	             // used to reduce the time required to get listings
                                     // of bases and fields.. keys are Bases, values
		      	             // are vectors of fields
    baseMap = null,                  // Hash of Short to Base
    changedHash = new Hashtable(),   // Hash of objects that might have changed
    deleteHash = new Hashtable(),    // Hash of objects waiting to be deleted
    createHash = new Hashtable(),    // Hash of objects waiting to be created

    // Hash of objects that are created before nodes are available
    // after the node is created, the invid is taken out of
    // createdObjectsWithoutNodes and put into createHash.

    createdObjectsWithoutNodes = new Hashtable(),
                                     // Create and Delete are pending on the Commit button. 
    baseToShort = null;              // Map of Base to Short
   
  protected Hashtable
    shortToBaseNodeHash = new Hashtable(),
    invidNodeHash = new Hashtable(),
    templateHash;

  // our main cache, keeps information on all objects we've had
  // references returned to us via QueryResult

  protected objectCache 
    cachedLists = new objectCache();

  // 
  //  Background processing thread
  //

  Loader 
    loader;      // Use this to do start up stuff in a thread
  
  //
  // Status tracking
  //

  private boolean
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

  protected JComboBox
    personaCombo = null;  // ComboBox showing current persona on the toolbar

  JFilterDialog
    filterDialog = null;

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
  
  final JTextField
    statusLabel = new JTextField();

  private JSplitPane
    sPane;

  treeControl tree;

  // The top lines

  JPanel
    leftP,
    leftTop,
    rightTop,
    mainPanel;   //Everything is in this, so it is double buffered

  Image
    errorImage = null,
    search,
    pencil,
    trash,
    creation,
    newToolbarIcon,
    ganymede_logo,
    createDialogImage;

  public JLabel
    leftL,
    rightL,
    timerLabel;

  //
  // Another background thread, to maintain a display of
  // time connected
  //

  connectedTimer
    timer;

  windowPanel
    wp;

  treeNode
    selectedNode;

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
    showHelpMI;

  private boolean
    defaultOwnerChosen = false;

  JMenuItem
    editObjectMI,
    viewObjectMI,
    createObjectMI,
    cloneObjectMI,
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

  PersonaListener
    personaListener = null;

  ButtonGroup
    personaGroup;  // This is the group of persona menu items.  Only one can be chosen.

  querybox
    my_querybox = null;


  // this is true during the handleReturnVal method, while a wizard is
  // active.  If a wizard is active, don't allow the window to close.
  private boolean
    wizardActive = false;

  /* -- */

  /**
   *
   * This is the main constructor for the gclient class.. it handles the
   * interactions between the user and the server once the user has
   * logged in.
   *
   */

  public gclient(Session s, glogin g)
  {
    //super("Ganymede Client: "+g.my_client.getName() +" logged in");

    try
      {
	setTitle("Ganymede Client: " + s.getMyUserName() + " logged in");
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not talk to server: " + rx);
      }

    setIconImage(pencil);

    client = this;

    debug = g.debug;

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
    logoutMI = new JMenuItem("Logout");
    logoutMI.setMnemonic('l');
    logoutMI.addActionListener(this);

    clearTreeMI = new JMenuItem("Clear Tree");
    clearTreeMI.addActionListener(this);

    filterQueryMI = new JMenuItem("Filter Query");
    filterQueryMI.addActionListener(this);
    defaultOwnerMI = new JMenuItem("Set Default Owner");
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
    
    //cloneObjectMI = new JMenuItem("Clone Object");
    //cloneObjectMI.setActionCommand("choose an object for cloning");
    //cloneObjectMI.addActionListener(this);

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

    // Look and Feel menu

    LandFMenu = new arlut.csd.JDataComponent.LAFMenu(this);
    LandFMenu.setMnemonic('l');
    LandFMenu.setCallback(this);

    // Personae menu

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

    if ((!showToolbar) && (personae != null))
      {
	PersonaMenu = new JMenu("Persona");
	personaGroup = new ButtonGroup();
	
	for (int i = 0; i < personae.size(); i++)
	  {
	    String p = (String)personae.elementAt(i);
	    JCheckBoxMenuItem mi = new JCheckBoxMenuItem(p, false);

	    if (p.equals(my_username))
	      {
		currentPersonaString = p;
		mi.setState(true);
	      }

	    personaGroup.add(mi);
	    mi.addActionListener(personaListener);
	    PersonaMenu.add(mi);
	  }

	personasExist = true;
      }
    else if (showToolbar && personae != null)
      {
	currentPersonaString = my_username;
      }

    // Help menu

    helpMenu = new JMenu("Help");
    helpMenu.setMnemonic('h');
    showHelpMI = new JMenuItem("Help");
    showHelpMI.setMnemonic('h');
    showHelpMI.addActionListener(this);
    helpMenu.add(showHelpMI);

    helpMenu.addSeparator();

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

    if (personasExist)
      {
	menubar.add(PersonaMenu);
      }

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
    trash = PackageResources.getImageResource(this, "trash.gif", getClass());
    creation = PackageResources.getImageResource(this, "creation.gif", getClass());
    newToolbarIcon = PackageResources.getImageResource(this, "newicon.gif", getClass());
    pencil = PackageResources.getImageResource(this, "pencil.gif", getClass());
    createDialogImage = PackageResources.getImageResource(this, "wiz3b.gif", getClass());

    Image remove = PackageResources.getImageResource(this, "remove.gif", getClass());
    Image expire = PackageResources.getImageResource(this, "expire.gif", getClass());

    images = new Image[NUM_IMAGE];
    images[OPEN_BASE] =  openFolder;
    images[CLOSED_BASE ] = closedFolder;
    
    images[OPEN_FIELD] = list;
    images[OPEN_FIELD_DELETE] = trash;
    images[OPEN_FIELD_CREATE] = creation;
    images[OPEN_FIELD_CHANGED] = pencil;
    images[OPEN_FIELD_EXPIRESET] = expire;
    images[OPEN_FIELD_REMOVESET] = remove;
    images[CLOSED_FIELD] = list;
    images[CLOSED_FIELD_DELETE] = trash;
    images[CLOSED_FIELD_CREATE] = creation;
    images[CLOSED_FIELD_CHANGED] = pencil;
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
	
	leftL = new JLabel("Objects");
	leftTop.setLayout(new BorderLayout());
	//leftTop.setBackground(ClientColor.menu);
	//leftTop.setForeground(ClientColor.menuText);
	leftTop.add("Center", leftL);
	
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
    //objectRemovePM.add(new MenuItem("Clone Object"));
    objectRemovePM.add(new MenuItem("Delete Object"));

    objectInactivatePM = new treeMenu();
    objectInactivatePM.add(new MenuItem("View Object"));
    objectInactivatePM.add(new MenuItem("Edit Object"));
    //objectInactivatePM.add(new MenuItem("Clone Object"));
    objectInactivatePM.add(new MenuItem("Inactivate Object"));

    objectReactivatePM = new treeMenu();
    objectReactivatePM.add(new MenuItem("View Object"));
    objectReactivatePM.add(new MenuItem("Edit Object"));;
    //objectReactivatePM.add(new MenuItem("Clone Object"));
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
    
    if (showToolbar)
      {
	getContentPane().add("North", createToolbar());
      }
    else
      {
	rightL = new JLabel("Open objects");
	
	rightTop.add("West", rightL);
	//timerLabel = new JLabel("                                       ", JLabel.RIGHT);
	timerLabel = new JLabel("00:00:00", JLabel.RIGHT);
	timer = new connectedTimer(timerLabel, 5000, true);
	timerLabel.setMinimumSize(new Dimension(200, timerLabel.getPreferredSize().height));
	rightTop.add("East", timerLabel);

	rightP.add("North", rightTop);	
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

    sPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftP, rightP);
   
    mainPanel.add("Center",sPane);

    // Create the bottomBar, for the bottom of the window

    JPanel bottomBar = new JPanel(false);
    bottomBar.setLayout(new BorderLayout());

    statusLabel.setEditable(false);
    statusLabel.setOpaque(false);
    statusLabel.setBorder(statusBorder);

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
	ReturnVal rv = session.openTransaction("gclient");
	handleReturnVal(rv);
	if ((rv != null) && (!rv.didSucceed()))
	  {
	    throw new RuntimeException("Could not open transaction.");
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not open transaction: " + rx);
      }

    if (timer != null)
      {
	timer.start();
      }

    loader = new Loader(session, debug);
    loader.start();

    pack();
    setSize(800, 600);
    show();

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
    
    setStatus("Ready.");
  }
  
  /**
   * Returns a vector of FieldTemplates.
   *
   * The id number is the base id.
   */

  public Vector getTemplateVector(short id)
  {
    return getTemplateVector(new Short(id));
  }

  /**
   * Returns a vector of FieldTemplates.
   *
   * The id number is the base id.
   */

  public Vector getTemplateVector(Short id)
  {
    Vector result = null;
    Hashtable th = getTemplateHash();

    if (th.containsKey(id))
      {
	if (debug)
	  {
	    System.out.println("Found the template, using cache for base: " + id);
	  }

	result = (Vector) th.get(id);
      }
    else
      {
	try
	  {
	    if (debug)
	      {
		System.out.println("template not found, downloading and caching: " + id);
	      }

	    result = session.getFieldTemplateVector(id.shortValue());
	    th.put(id, result);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get field templates: " + rx);
	  }
      }
    
    return result;
  }

  /**
   * Returns the templateHash.
   *
   * Template Hash is a hash of object type ID's (Short) -> Vector of FieldTemplates
   * Use this instead of templateHash directly, because you never know where we will
   * get it from(evil grin).
   *
   */

  private Hashtable getTemplateHash()
  {
    return templateHash;
  }

  /**
   * Clear out all the client side hashes.
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
   *
   * This method clears out all the cached data structures
   * refering to bases.  We need to clear these when our
   * persona changes.
   *
   */

  public void clearLoaderLists()
  {
    baseList = null;
    baseToShort = baseNames = baseHash = baseMap = null;
  }

  /**
   * This method is used to get a list of objects from the server, in
   * a form appropriate for use in constructing a list of nodes in the
   * tree under an object type (object base) folder.<br><br>
   *
   * This method supports client-side caching.. if the list required
   * has already been retrieved, the cached list will be returned.  If
   * it hasn't, getObjectList() will get the list from the server and
   * save a local copy for future requests.
   * 
   */

  public objectList getObjectList(Short id, boolean showAll)
  {
    objectList objectlist = null;

    /* -- */

    if (cachedLists.containsList(id))
      {
	if (debug)
	  {
	    System.out.println("++ getting objectlist from the cachedLists.");
	  }

	objectlist = cachedLists.getList(id);

	// If we are being asked for a *complete* list of objects of
	// the given type and we only have editable objects of this
	// type cached, we may need to go back to the server to
	// get the full list.

	if (showAll && !objectlist.containsNonEditable())
	  {
	    try
	      {
		QueryResult qr = session.query(new Query(id.shortValue(), null, false));
		
		if (qr != null)
		  {
		    if (debug)
		      {
			System.out.println("augmenting");
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
	    System.out.println("++ objectlist not in cached lists, downloading a new one.");
	  }

	try
	  {
	    QueryResult qr = session.query(new Query(id.shortValue(), null, !showAll));

	    if (debug)
	      {
		System.out.println("Caching copy");
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
   *
   * Update the persona menu so it shows the correct persona as chosen.
   *
   */

  public void updatePersonaMenu()
  {
    if (debug)
      {
	System.out.println("--Updating persona menu");
      }

    // If we are configured to use the persona pull-down menu from the
    // client's menu bar, handle that

    if (personaGroup != null)
      {
	Enumeration buttons = personaGroup.getElements();
	
	while (buttons.hasMoreElements())
	  {
	    JCheckBoxMenuItem mi = (JCheckBoxMenuItem)buttons.nextElement();

	    if (mi.getActionCommand().equals(currentPersonaString))
	      {
		if (debug)
		  {
		    System.out.println("Calling setState(true)");
		  }
		
		mi.removeActionListener(personaListener);
		mi.setState(true);
		mi.addActionListener(personaListener);

		break; // Don't need to set the rest of false, because only one can be selected via the ButtonGroup
		// besides, if I do some setState(false)'s, then actions will be performed.
	      }
	  }
      }

    // else, if we're using the combo box for our persona selection,
    // update that too

    if (personaCombo != null)
      {
	SwingUtilities.invokeLater(new Runnable() {
	  public void run() {
	    personaCombo.removeActionListener(personaListener);
	    personaCombo.setSelectedItem(currentPersonaString);
	    personaCombo.addActionListener(personaListener);
	  }
	});
      }
  }

  /**
   *
   * By overriding update(), we can eliminate the annoying flash as
   * the default update() method clears the frame before rendering
   * 
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
   * Returns the error Image.
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
   * Returns the base hash.
   *
   * <p>Checks to see if the baseHash was loaded, and if not, it loads it.
   * Always use this instead of trying to access the baseHash
   * directly.</p>
   *
   */

  public final Hashtable getBaseHash()
  {
    if (baseHash == null)
      {
	baseHash = loader.getBaseHash();
      }

    return baseHash;
  }

  /**
   * Returns the base names.
   *
   * <p>Checks to see if the baseNames was loaded, and if not, it loads it.
   * Always use this instead of trying to access baseNames
   * directly.</p>
   *
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
   * Returns the base list.
   *
   * <p>Checks to see if the baseList was loaded, and if not, it loads it.
   * Always use this instead of trying to access the baseList
   * directly.</p>
   *
   */

  public synchronized final Vector getBaseList()
  {
    if (baseList == null)
      {
	baseList = loader.getBaseList();
      }

    return baseList;
  }

  /**
   * Returns the base map.
   *
   * <p>Checks to see if the baseMap was loaded, and if not, it loads it.
   * Always use this instead of trying to access the baseMap
   * directly.</p>
   *
   */

  public Hashtable getBaseMap()
  {
    if (baseMap == null)
      {
	baseMap = loader.getBaseMap();
      }

    return baseMap;
  }

  /**
   * Returns the base to short.
   *
   * <p>Checks to see if the basetoShort was loaded, and if not, it loads it.
   * Always use this instead of trying to access the baseToShort
   * directly.</p>
   *
   */

  public Hashtable getBaseToShort()
  {
    if (baseToShort == null)
      {
	baseToShort = loader.getBaseToShort();
      }
    
    return baseToShort;
  }

  /**
   *
   * This method pulls a object handle for an invid out of the
   * client's cache, if it has been cached.<br><br>
   *
   * If no handle for this invid has been cached, this method
   * will return null.
   *
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
   * Change the text in the status bar
   *
   * @param status The text to display
   */

  public final void setStatus(String status)
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
  }

  /**
   *
   * Returns the name of the object currently selected in the tree, if
   * any.  Returns null if there are no nodes selected in the tree, of
   * if the node selected is not an object node.
   * 
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
   * Get the status line for the window
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
   * Show the About... dialog.
   */

  public void showAboutMessage()
  {
    if (about == null)
      {
	if (aboutMessage == null)
	  {
	    aboutMessage = "<head></head><h1>About Ganymede</h1><p>Ganymede is a moon of Jupiter.</p>";
	  }

	about = new messageDialog(this, "About Ganymede",  ganymede_logo);
	about.setHtmlText(aboutMessage);
      }

    about.setVisible(true);
  }

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
			      "jonabbey@arlut.utexas.edu.  Michael Mulvaney, mikem@mail.utexas.edu, "+
			      "developed large portions of the client.  Significant portions of the client " +
			      "were initially developed by Navin Manohar.   Erik Grostic also contributed code " +
			      "to the client.  Both Navin and Erik worked on Ganymede while working as student " +
			      "employees at ARL.  Dan Scott, dscott@arlut.utexas.edu, oversaw the development " +
			      " of Ganymede and its predecessor, GASH, and provided high-level " +
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
   *
   * This method generates a message-of-the-day dialog.
   *
   * @param message The message to display.  May be multiline.  
   * @param html If true, showMOTD() will display the motd with a html
   * renderer, in Swing 1.1b2 and later.
   * 
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
   * Popup a dialog with the default title.
   */

  public final void showErrorMessage(String message)
  {
    showErrorMessage("Error", message);
  }

  /**
   * Popup an error dialog.
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

    setStatus(title + ": " + message);
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
   * canceling this transaction will have consequences.
   *
   * <p>This should be called whenever the client makes any changes to
   * the database.  That includes creating objects, editting fields of
   * objects, removing objects, renaming, expiring, deleting,
   * inactivating, and so on.  It is very important to call this
   * whenever something might have changed. </p> 
   *
   */

  public final void somethingChanged()
  {
    commit.setEnabled(true);
    cancel.setEnabled(true);
    setSomethingChanged(true);
  }

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
   * True if we are in an applet context.
   */

  public boolean isApplet()
  {
    return _myglogin.isApplet();
  }

  /**
   *
   * This method takes a ReturnVal object from the server and, if necessary,
   * runs through a wizard interaction sequence, possibly displaying several
   * dialogs before finally returning a final result code.
   *
   * Use the ReturnVal returned from this function after this function is
   * called.  Always.
   */

  public ReturnVal handleReturnVal(ReturnVal retVal)
  {
    if (debug)
      {
	System.err.println("** gclient: Entering handleReturnVal");
      }

    wizardActive = true;

    while ((retVal != null) && (retVal.getDialog() != null))
      {
	if (debug)
	  {
	    System.err.println("** gclient: retrieving dialog");
	  }

	JDialogBuff jdialog = retVal.getDialog();

	if (debug)
	  {
	    System.err.println("** gclient: extracting dialog");
	  }

	DialogRsrc resource = jdialog.extractDialogRsrc(this);

	if (debug)
	  {
	    System.err.println("** gclient: constructing dialog");
	  }

	StringDialog dialog = new StringDialog(resource);

	if (debug)
	  {
	    System.err.println("** gclient: displaying dialog");
	  }

	setNormalCursor();

	// display the Dialog sent to us by the server, get the
	// result of the user's interaction with it.

	Hashtable result = dialog.DialogShow();

	setWaitCursor();

	if (debug)
	  {
	    System.err.println("** gclient: dialog done");
	  }

	if (retVal.getCallback() != null)
	  {
	    try
	      {
		if (debug)
		  {
		    System.out.println("Sending result to callback: " + result);
		  }

		// send the dialog results to the server

		retVal = retVal.getCallback().respond(result);

		if (debug)
		  {
		    System.out.println("Received result from callback.");
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
		System.out.println("No callback, breaking");
	      }

	    break;		// we're done
	  }
      }

    wizardActive = false;
    
    if (debug)
      {
	System.out.println("Done with wizards, checking retVal for rescan.");
      }

    // Check for objects that need to be rescanned

    if (retVal == null)
      {
	return retVal;
      }

    if (debug)
      {
	System.err.println("** gclient: rescan dump: " +retVal.dumpRescanInfo());
      }

    if (!retVal.doRescan())
      {
	return retVal;
      }

    Vector objects = retVal.getRescanObjectsList();
	    
    if (objects == null)
      {
	if (debug)
	  {
	    System.err.println("Odd, was told to rescan, but there's nothing there!");
	  }

	return retVal;
      }
    
    if (debug)
      {
	System.out.println("Rescanning " + objects.size() + " objects.");
      }
    
    Enumeration invids = objects.elements();

    // Loop over all the invids, and try to find
    // containerPanels for them.
    
    while (invids.hasMoreElements())
      {
	if (debug)
	  {
	    System.out.println("Casting to Invid");
	  }

	Invid invid = (Invid) invids.nextElement();
		
	if (debug)
	  {
	    System.out.println("objectResultSet: updating invid: " + invid);
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

		// Loop over each containerPanel in the framePanel window.. there
		// may be more than one due to embedded objects

		for (int i = 0; i < fp.containerPanels.size(); i++)
		  {
		    containerPanel cp = (containerPanel)fp.containerPanels.elementAt(i);

		    if (debug)
		      {
			System.out.println("Checking containerPanel number " + i);
			System.out.println("  cp.invid= " + cp.getObjectInvid() + " lookng for: " + invid);
		      }
				
		    if (cp.getObjectInvid().equals(invid))
		      {
			if (debug)
			  {
			    System.out.println("  Found container panel for " + invid + ": " + cp.frame.getTitle());
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
	System.err.println("** gclient: Exiting handleReturnVal");
      }

    return retVal;
  }

  // Private methods

  /**
   * Note that this actually returns a JPanel.
   *
   * That's so I can put the ComboBox in.
   */

  JPanel createToolbar()
  {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(0,4,0,4));
    JToolBar toolBar = new JToolBar();
    toolBar.setBorderPainted(true);
    Insets insets = new Insets(0,0,0,0);
    
    toolBar.setMargin(insets);

    JButton b = new JButton("Create", new ImageIcon(newToolbarIcon));
    b.setActionCommand("create new object");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Create a new object");
    b.addActionListener(this);
    b.setMargin(insets);
    toolBar.add(b);

    b = new JButton("Edit", new ImageIcon(pencil));
    b.setActionCommand("open object for editing");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Edit an object");
    b.addActionListener(this);
    b.setMargin(insets);
    toolBar.add(b);

    b = new JButton("Delete", new ImageIcon(trash));
    b.setActionCommand("delete an object");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Delete an object");
    b.addActionListener(this);
    b.setMargin(insets);
    toolBar.add(b);

    b = new JButton("View", new ImageIcon(search));
    b.setActionCommand("open object for viewing");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("View an object");
    b.setMargin(insets);
    b.addActionListener(this);
    toolBar.add(b);

    panel.add("West", toolBar);
    
    if ((personae != null)  && personae.size() > 0)
      {
	if (debug)
	  {
	    System.out.println("Adding persona stuff");
	  }
	
	personaCombo = new JComboBox();

	for(int i =0; i< personae.size(); i++)
	  {
	    personaCombo.addItem((String)personae.elementAt(i));
	  }

	personaCombo.setSelectedItem(my_username);

	personaCombo.addActionListener(personaListener);

	// Check this out

	JPanel POuterpanel = new JPanel(new BorderLayout());
	JPanel PInnerpanel = new JPanel(new BorderLayout());
	PInnerpanel.add("Center", new JLabel("Persona:", SwingConstants.RIGHT));
	PInnerpanel.add("East", personaCombo);
	POuterpanel.add("South", PInnerpanel);
	panel.add("East", POuterpanel);
      }
    else if (debug)
      {
	System.out.println("No personas.");
      }

//     // Now the connected timer.
//     timerLabel = new JLabel("00:00:00", JLabel.RIGHT);
//     timer = new connectedTimer(timerLabel, 5000, true);
//     timerLabel.setMinimumSize(new Dimension(200,timerLabel.getPreferredSize().height));
//     panel.add("East", timerLabel);

    return panel;
  }

  /**
   * Update the persona combo box(on toolbar) to the correct persona.
   */

  protected void setPersonaCombo(String persona)
  {
    if (personaCombo != null)
      {
	personaCombo.setSelectedItem(persona);
      }
  }

  ///////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////
  ////            Tree Stuff
  ////
  ///////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////


  /**
   * This clears out the tree.
   *
   * <p>All Nodes will be removed, and the Category and BaseNodes will be rebuilt.  No InvidNodes will be added.</P>
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
   *
   * This method builds the initial data structures for the object
   * selection tree, using the base information in the baseHash
   * hashtable.
   * 
   */

  void buildTree() throws RemoteException
  {
    if (debug)
      {
	System.out.println("Building tree");
      }

    CategoryTransport transport = session.getCategoryTree();

    // get the category dump, save it

    dump = transport.getTree();

    // remember that we'll want to refresh our base list

    baseList = null;
    
    if (debug)
      {
	System.out.println("got root category: " + dump.getName());
      }

    //    CatTreeNode firstNode = new CatTreeNode(null, dump.getName(), dump,
    //					    null, true, 
    //					    OPEN_CAT, CLOSED_CAT, null);
    //    tree.setRoot(firstNode);

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
	System.out.println("Refreshing tree");
      }

    tree.refresh();

    if (debug)
      {
	System.out.println("Done building tree,");
      }
  }

  void recurseDownCategories(CatTreeNode node, Category c) throws RemoteException
  {
    Vector
      children;

    //    Category c;
    CategoryNode cNode;

    treeNode 
      prevNode;

    /* -- */
      
    //    c = node.getCategory();
    
    //    node.setText(c.getName());
    
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
   *
   * Helper method for building tree
   *
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
	System.out.println("Unknown instance: " + node);
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
   *
   * This method is used to update the list of object nodes under a given
   * base node in our object selection tree.
   *
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
		    if (debug)
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
   *
   * This method updates the tree for the nodes that might have changed.
   *
   * This method fixes all the icons, and cleans out the various hashes.  Only call this 
   * when commit is clicked.  This replaces refreshTree(boolean committed), because all the
   * refreshing to be done after a cancel is now handled in the cancelTransaction() method
   * directly.
   *
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
		System.out.println("Deleteing node: " + node.getText());
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
   * This method scans through the changedHash hashtable and generates
   * queries to the server for all invids that have been touched by
   * the client after transaction commit.  The results from the
   * queries are used to update the icons in the tree.
   *
   * @param paramVect Vector of invid's to refresh.
   * @param afterCommit If true, this method will update the client's
   * status bar as it progresses.
   * 
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
		    System.err.println("got object handle refresh for " + newHandle.debugDump());
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
		System.err.println("gclient.refreshChangedObjectHandles(): null node for " + newHandle.debugDump());
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
   *
   * This method changes the icon for the tree node for the
   * provided invid, depending on the various hashes and the object's
   * objectHandle.
   *
   * This method does not actually induce the tree to refresh itself.
   *
   */

  public void setIconForNode(Invid invid)
  {
    InvidNode node = (InvidNode)invidNodeHash.get(invid);

    if (node == null)
      {
	return;
      }

    if (node == null)
      {
	if (debug)
	  {
	    System.out.println("There is no node for this invid, silly!");
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

	if (createHash.containsKey(invid))
	  {
	    if (debug)
	      {
		System.out.println("Setting icon to create.");
	      }

	    node.setImages(OPEN_FIELD_CREATE, CLOSED_FIELD_CREATE);
	  }
	else if (deleteHash.containsKey(invid))
	  {
	    if (debug)
	      {
		System.out.println("Setting icon to delete.");
	      }

	    node.setImages(OPEN_FIELD_DELETE, CLOSED_FIELD_DELETE);
	  }
	else if (handle != null)
	  {
	    if (handle.isInactive())
	      {
		if (debug)
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
		    if (debug)
		      {
			System.out.println("isExpirationSet");
		      }

		    node.setImages(OPEN_FIELD_EXPIRESET, CLOSED_FIELD_EXPIRESET);
		  }
		else if (handle.isRemovalSet())
		  {
		    if (debug)
		      {
			System.out.println("isRemovalSet()");
		      }

		    node.setMenu(objectReactivatePM);
		    node.setImages(OPEN_FIELD_REMOVESET, CLOSED_FIELD_REMOVESET);
		  }
		else if (changedHash.containsKey(invid))
		  {
		    if (debug)
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
		if (debug)
		  {
		    System.out.println("Setting icon to edit.");
		  }
		
		node.setImages(OPEN_FIELD_CHANGED, CLOSED_FIELD_CHANGED);
	      }
	    else
	      {
		if (debug)
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
   * edit an object, and open a new window for it.
   *
   * Use this to edit objects, so gclient can keep track of the caches, tree nodes,
   * and all the other dirty work.  This should be the only place windowPanel.addWindow()
   * is called for editing purposes.
   */
  public void editObject(Invid invid)
  {
    editObject(invid, null);
  }

  /**
   * open a new window to edit the object.
   *
   * Use this to edit objects, so gclient can keep track of the caches, tree nodes,
   * and all the other dirty work.  This should be the only place windowPanel.addWindow
   * is called for editing purposes.
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

	db_object o = rv.getObject();
	if (o == null)
	  {
	    // Assume the returnVal threw up a dialog for us
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
   * Open a new window with a newly created object.
   *
   * @type Type of object to be created
   * @showNow if true, a new window will be shown.  If false, just return the object.
   *
   * Call showNewlyCreatedObject() to show it later.
   */

  public db_object createObject(short type, boolean showNow)
  {
    Invid invid = null;
    db_object obj = null;

    /* -- */

    if (!defaultOwnerChosen)
      {
	chooseDefaultOwner(false);
      }
    
    setWaitCursor();

    try
      {
	ReturnVal rv = handleReturnVal(session.create_db_object(type));
	obj = rv.getObject();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Exception creating new object: " + rx);
      }

    if (obj == null)
      {
	showErrorMessage("Could not create object for some reason.  Check the Admin " +
			 "console, or the server debuggin information.");

	throw new RuntimeException("Could not create object for some reason- server returned " +
				   "a null object.  Check the Admin console.");
      }

    if (showNow)
      {
	showNewlyCreatedObject(obj, invid, new Short(type));
      }
    else
      {
	setNormalCursor();
      }

    somethingChanged();

    return obj;
  }

  /**
   * Add a new window and everything for a new object.
   *
   * obj can be null!  If it is, then this will create a new object of the type.
   *
   * @param obj the object created, can be null.  If you give a non-null object, 
   *            then this method will not create a new object.
   *
   * @param type The type of the object, used in creating.
   */

  public void showNewlyCreatedObject(db_object obj, Invid invid, Short type)
  {
    String label = null;

    setWaitCursor();

    try
      {
	label = getSession().viewObjectLabel(invid);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get the object label. " + rx);
      }

    if (label == null)
      {
	label = "New Object";
      }
    
    ObjectHandle handle = new ObjectHandle(label, invid, false, false, false, true);
       
    wp.addWindow(obj, true, null, true);
    
    if (invid == null)
      {
	try
	  {
	    invid = obj.getInvid();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get invid: " + rx);
	  }
      }
    
    if (cachedLists.containsList(type))
      {
	objectList list = cachedLists.getList(type);
	list.addObjectHandle(handle);
      }
    
    // If the base node is open, deal with the node.

    BaseNode baseN = null;

    if (shortToBaseNodeHash.containsKey(type))
      {
	baseN = (BaseNode)shortToBaseNodeHash.get(type);

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
	    
	    createHash.put(invid, new CacheInfo(type, handle.getLabel(), null, handle));
	    
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

    setNormalCursor();
  }

  /**
   * Open a view window on this object.
   */

  public void viewObject(Invid invid)
  {
    viewObject(invid, null);
  }

  /**
   * Open a new window to view the current object.
   */

  public void viewObject(Invid invid, String objectType)
  {
    try
      {
	ReturnVal rv = handleReturnVal(session.view_db_object(invid));
	db_object object = rv.getObject();
	if (object == null)
	  {
	    showErrorMessage("You are not allowed to \nview that object.");
	  }
	else
	  {
	    wp.addWindow(object, false, objectType);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not edit object: " + rx);
      }
  }

  /**
   * Delete the object.
   *
   * Also takes care of caches and treeNodes.  
   */

  public boolean deleteObject(Invid invid)
  {
    ReturnVal retVal;
    boolean ok = false;

    /* -- */

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
	    //InvidNode node = (InvidNode)invidNodeHash.get(invid);

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

    setNormalCursor();

    return ok;
  }


  /** 
   * Inactivate an object.
   *
   * This takes care of all the hashes and everything.
   */
  public boolean inactivateObject(Invid invid)
  {
    boolean ok = false;
    ReturnVal retVal;

    if (invid == null)
      {
	if (debug)
	  {
	    System.out.println("Canceled");
	  }
      }
    else
      {
	try
	  {
	    setStatus("inactivating " + invid);
	    setWaitCursor();
	    
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
      }

    setNormalCursor();
    return ok;
  }

  /** 
   * Reactivate an object.
   *
   * This is to reactivate a deactivated object.  I think you should call this from the 
   * expiration date panel if the date is cleared.
   */

  public boolean reactivateObject(Invid invid)
  {
    ReturnVal retVal;
    boolean ok = false;

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
   *
   * Show the create object dialog, let the user choose
   * to create or not create an object.
   *
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
   * Opens a panel used to choose an object for editing.
   *
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
   * Open an object for viewing.
   *
   * This displays a window with a chooser for the base and field for the name.
   *
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
   * Open a dialog and then clone the object selected.
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

    openDialog.setText("Choose object to be cloned");
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
	try
	  {
	    ReturnVal rv = handleReturnVal(session.clone_db_object(invid));
	    wp.addWindow(rv.getObject(), true);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not edit object: " + rx);
	  }
      }
  }

  /**
   * Offer a selection and the inactivate the object chosen.
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
    openDialog.setReturnEditableOnly(true);

    Invid invid = openDialog.chooseInvid();
    
    inactivateObject(invid);
  }

  /**
   * Offer a selection and then delete the object chosen.
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
	try
	  {
	    StringDialog d = new StringDialog(this, "Verify deletion", 
					      "Are you sure you want to delete " + 
					      session.viewObjectLabel(invid), 
					      "Yes", "No");
	    Hashtable result = d.DialogShow();

	    if (result == null)
	      {
		setStatus("Cancelled!");
	      }
	    else
	      {
		deleteObject(invid);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not verify invid to be inactivated: " + rx);
	  }
      }
  }

  /**
   *
   * Logout from the client.
   *
   * This method does not do any checking, it just logs out.
   *
   */

  void logout()
  {
    try
      {
	if (timer != null)
	  {
	    timer.stop();
	  }

	_myglogin.logout();
	this.dispose();
      }
    catch (RemoteException rx)
      {
	throw new IllegalArgumentException("could not logout: " + rx);
      }
  }

  /**
   * Create a custom query filter.
   *
   * The filter is used to limit the output on a query.  This pops up a JFilterDialog.
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
   * Choose the default owner group for a newly created object.
   *
   * This must be called before Session.create_db_object is called.
   * @see defaultOwnerChosen.
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
   * Inform the gclient that a new container panel is starting.
   *
   * <p>The client must know about all container panels that are currently loading,
   * so it can tell them all to stop if the cancel button is clicked.  This
   * method should be called by any container panel before it starts the big loop.</p>
   */

  public synchronized void registerNewContainerPanel(containerPanel cp)
  {
    containerPanels.addElement(cp);
  }

  /**
   * Inform the gclient that a previously registered containerPanel is now finished loading.
   *
   * @see registerNewContainerPanel
   */

  public synchronized void containerPanelFinished(containerPanel cp)
  {
    containerPanels.removeElement(cp);
    this.notify();
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
    if (wizardActive)
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
   * Update the note panels in the open windows.
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
   * Commit the current transaction to the data base.
   */

  public void commitTransaction()
  {
    ReturnVal retVal;

    /* -- */

    try
      {
	setWaitCursor();
	updateNotePanels();
	
	retVal = session.commitTransaction();

	if (retVal != null)
	  {
	    retVal = handleReturnVal(retVal);
	  }

	boolean succeeded = (retVal == null) ? true : retVal.didSucceed();

	if (succeeded)
	  {
	    setStatus("Transaction successfully committed.");
	    wp.closeEditables();

	    setSomethingChanged(false);
	    cancel.setEnabled(false);
	    commit.setEnabled(false);
	
	    wp.refreshTableWindows();
	    ReturnVal rv = session.openTransaction("gclient");
	    handleReturnVal(rv);

	    if ((rv != null) && (!rv.didSucceed()))
	      {
		showErrorMessage("Could not open transaction.");
	      }

	    //
	    // This fixes all the icons in the tree
	    //

	    refreshTreeAfterCommit();

	    setNormalCursor();

	    cachedLists.clearCaches();
	
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
	    // the server cancelled our transaction.  We dont' need to
	    // call cancelTransaction, however, and we don't need to
	    // openNewTransaction.
	    cleanUpAfterCancel();
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
	showErrorMessage("Exception during commit: " + e);
	throw new RuntimeException("Exception during commit: " + e);
      }

  }

  /**
   * Cancel the current transaction.
   *
   * This will close all the editable windows, and fix up the icons in the tree.
   */

  public synchronized void cancelTransaction()
  {
    ObjectHandle handle;
    ReturnVal retVal;

    /* -- */

    try
      {
	// First we need to tell all the container Panels to stop loading

	for (int i = 0; i < containerPanels.size(); i++)
	  {
	    if (debug)
	      {
		System.out.println("Shutting down containerPanel");
	      }

	    containerPanel cp = (containerPanel)containerPanels.elementAt(i);
	    
	    cp.stopLoading();
	  }
	
	long startTime = System.currentTimeMillis(); // Only going to wait 10 seconds

	while ((containerPanels.size() > 0) && (System.currentTimeMillis() - startTime < 10000))
	  {
	    try
	      {
		if (debug)
		  {
		    System.out.println("Waiting for containerPanels to shut down.");
		  }

		this.wait(1000);
	      }
	    catch (InterruptedException x)
	      {
		throw new RuntimeException("Interrupted while waiting for container panels to stop: " + x);
	      }
	  }

	if (debug && (containerPanels.size() > 0))
	  {
	    System.out.println("Hmm, containerPanels is still not empty, the timeout must have kicked in.  Oh well.");
	  }
	
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

  private void cleanUpAfterCancel()
  {
    ObjectHandle handle;
    Invid invid;
    InvidNode node;
    objectList list;
    CacheInfo info;
    
    wp.closeEditables();

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
    setSomethingChanged(false);
    cancel.setEnabled(false);
    commit.setEnabled(false);
  }

  private void openNewTransaction()
  {
    try
      {
	ReturnVal rv = session.openTransaction("glient");
	
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
  }
  
  // ActionListener Methods
  
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
    else if (source == menubarQueryMI)
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
    else if (command.equals("choose an object for cloning"))
      {
	cloneObjectDialog();
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
   *
   * This is a debugging hook, to allow the user to enter an invid in 
   * string form for direct viewing.
   *
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
   *
   * Called when a node is expanded, to allow the
   * user of the tree to dynamically load the information
   * at that time.
   *
   * @param node The node opened in the tree.
   *
   * @see arlut.csd.Tree.treeCanvas
   */

  public void treeNodeExpanded(treeNode node)
  {
    if (node instanceof BaseNode && !((BaseNode) node).isLoaded())
      {
	setStatus("Loading objects for base " + node.getText());
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
   *
   * Called when a node is closed.
   *
   * @see arlut.csd.Tree.treeCanvas
   */

  public void treeNodeContracted(treeNode node)
  {
  }

  /**
   *
   * Called when an item in the tree is unselected
   *
   * @param node The node selected in the tree.
   * @param someNodeSelected If true, this node is being unselected by the selection
   *                         of another node.
   *
   * @see arlut.csd.Tree.treeCanvas
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

  public void treeNodeMenuPerformed(treeNode node,
				    java.awt.event.ActionEvent event)
  {
    // Unclear why the following bit was here.. I've commented it out for
    // now.  As far as I can see, the only possible impact to commenting this
    // out is to cause object creation to not have the new node updated properly..
    // this is a minor thing compared to unnecessary delay as we load the object nodes
    //
    // 23 Jan 1998 - Jon

    //    if (node instanceof BaseNode)
    //      {
    //	// make sure we've got the list updated
    //
    //	treeNodeExpanded(node);
    //      }
    
    if (event.getActionCommand().equals("Create"))
      {
	if (debug)
	  {
	    System.out.println("createMI");
	  }

	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;

	    short id = baseN.getTypeID().shortValue();
	    
	    createObject(id, true);
	  }
	else
	  {
	    System.err.println("not a base node, can't create");
	  }
      }
    else if ((event.getActionCommand().equals("List editable")) ||
	     (event.getActionCommand().equals("List all")))
      {
	if (debug)
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
	    
	    setStatus("Sending query for base " + node.getText() + " to server");
	  }
	else
	  {
	    System.out.println("viewMI from a node other than a BaseNode");
	  }
      }
    else if (event.getActionCommand().equals("Query"))
      {
	if (debug)
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
			    thisGclient.setStatus("Sending query for base " + text + " to server");
			
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

	if (debug)
	  {
	    System.out.println("show all objects");
	  }

	setWaitCursor();

	id = bn.getTypeID();


	bn.showAll(true);
	node.setMenu(((BaseNode)node).canCreate() ? pMenuAllCreatable : pMenuAll);

	if (bn.isOpen())
	  {
	    try
	      {
		if (debug)
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
	setNormalCursor();
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
	    System.err.println("not a base node, can't create");
	  }
      }
    else if (event.getActionCommand().equals("Edit Object"))
      {
	if (debug)
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
	    System.err.println("not a base node, can't create");
	  }
      }
    else if (event.getActionCommand().equals("Delete Object"))
      {
	// Need to change the icon on the tree to an X or something to show that it is deleted

	if (debug)
	  {
	    System.out.println("Deleting object");
	  }

	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;
	    Invid invid = invidN.getInvid();

	    deleteObject(invid);
	  }
	else  // Should never get here, but just in case...
	  {
	    System.out.println("Not a InvidNode node, can't delete this.");
	  }
      }
    else if (event.getActionCommand().equals("Clone Object"))
      {
	showErrorMessage("Clone is not yet supported.\n\nWe may not even do it at all.\nWhat do you think?");
      }
    else if(event.getActionCommand().equals("Inactivate Object"))
      {
	if (debug)
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
	if (debug)
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
    (new VecQuickSort(v, 
		      new arlut.csd.Util.Compare() {
      public int compare(Object a, Object b) 
	{
	  String aF, bF;
	  
	  aF = (String) a;
	  bF = (String) b;
	  int comp = 0;
	  
	  comp =  aF.compareTo(bF);
	  
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


  /** why is this here, Mike??

      Hey, don't blame me.  -Mike
  */
  public void start() throws Exception 
  {

  }
}

/*---------------------------------------------------------------------
                                                                  class 
                                                              InvidNode

---------------------------------------------------------------------*/

class InvidNode extends arlut.csd.JTree.treeNode {

  final static boolean debug = true;

  private Invid invid;

  private String typeText;

  private ObjectHandle handle;

  public InvidNode(treeNode parent, String text, Invid invid, treeNode insertAfter,
		   boolean expandable, int openImage, int closedImage, treeMenu menu,
		   ObjectHandle handle)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);

    this.invid = invid;
    this.typeText = parent.getText();
    this.handle = handle;

    if (debug)
      {
	if (invid == null)
	  {
	    System.out.println(" null invid in InvidNode: " + text);
	  }
      }
  }

  public Invid getInvid()
  {
    return invid;
  }

  // Can't think of why you would ever want this

  public void setInvid(Invid invid)
  {
    this.invid = invid;
  }

  public String getTypeText()
  {
    return typeText;
  }  

  public ObjectHandle getHandle()
  {
    return handle;
  }

  public void setHandle(ObjectHandle handle)
  {
    this.handle = handle;
  }
}

/*------------------------------------------------------------------------------
                                                                           Class
                                                                        BaseNode

------------------------------------------------------------------------------*/

class BaseNode extends arlut.csd.JTree.treeNode {

  private Base base;

  private Query 
    editableQuery = null,
    allQuery = null;

  private boolean loaded = false;
  private boolean canBeInactivated = false;
  private boolean showAll = false;
  private boolean canCreate;
  private Short type = null;

  /* -- */

  BaseNode(treeNode parent, String text, Base base, treeNode insertAfter,
	   boolean expandable, int openImage, int closedImage, treeMenu menu, boolean canCreate)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
    this.base = base;
    this.canCreate = canCreate;
    
    try
      {
	canBeInactivated = base.canInactivate();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check inactivate.");
      }
  }

  public Short getTypeID()
  {
    if (type == null)
      {
	try
	  {
	    type = new Short(base.getTypeID());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get the base type in BaseNode: " + rx);
	  }
      }

    return type;
  }

  public Base getBase()
  {
    return base;
  }

  public boolean canInactivate()
  {
    return canBeInactivated;
  }

  public boolean canCreate()
  {
    return canCreate;
  }

  public boolean isShowAll()
  {
    return showAll;
  }

  public void showAll(boolean showAll)
  {
    this.showAll = showAll;
  }

  public void setBase(Base base)
  {
    this.base = base;
  }

  public void setEditableQuery(Query query)
  {
    this.editableQuery = query;
  }

  public void setAllQuery(Query query)
  {
    this.allQuery = query;
  }

  public Query getEditableQuery()
  {
    if (editableQuery == null)
      {	
	editableQuery = new Query(getTypeID().shortValue(), null, true);// include all, even non-editables
      }

    return editableQuery;
  }

  public Query getAllQuery()
  {
    if (allQuery == null)
      {
        allQuery = new Query(getTypeID().shortValue(), null, false);
      }

    return allQuery;
  }

  public boolean isLoaded()
  {
    return loaded;
  }

  public void markLoaded()
  {
    loaded = true;
  }

  public void markUnLoaded()
  {
    loaded = false;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 PersonaListener

------------------------------------------------------------------------------*/

class PersonaListener implements ActionListener {

  Session session;

  DialogRsrc
    resource = null;

  gclient
    gc;

  boolean
    listen = true;

  PersonaListener(Session session, gclient parent)
  {
    this.session = session;
    this.gc = parent;
  }

  public void actionPerformed(ActionEvent event)
  {
    if (gc.debug)
      {
	System.out.println("action Performed!");
      }
    
    //Check to see if we need to commit the transaction first.
    
    String newPersona = null;
    
    if (event.getSource() instanceof JMenuItem)
      {
	if (gc.debug)
	  {
	    System.out.println("From menu");
	  }
	
	//JMenuItem good
	newPersona = event.getActionCommand();
      }
    else if (event.getSource() instanceof JComboBox)
      {
	if (gc.debug)
	  {
	    System.out.println("From box");
	  }
	
	//JComboBox bad

	newPersona = (String) gc.personaCombo.getSelectedItem();
	
	if (gc.debug)
	  {
	    System.out.println("Box says: " + newPersona);
	  }
      }
    else
      {
	System.out.println("Persona Listener doesn't understand that action.");
      }

    if (newPersona == null)
      {
	gc.updatePersonaMenu();
	return;
      }
    
    if (newPersona.equals(gc.currentPersonaString))
      {
	if (gc.debug)
	  {
	    gc.showErrorMessage("You are already in that persona.");	
	  }
	
	return;
      }
    
    if (gc.getSomethingChanged())
      {
	// need to ask: commit, cancel, abort?
	StringDialog d = new StringDialog(gc,
					  "Changing personas",
					  "Before changing personas, the transaction must be closed.  Would you like to commit your changes?",
					  "Commit",
					  "Cancel");
	Hashtable result = d.DialogShow();
	
	if (result == null)
	  {
	    gc.setStatus("Persona change cancelled");
	    gc.updatePersonaMenu();
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

    resource = new DialogRsrc(gc, "Change Persona", 
				"Enter the password for " + newPersona + ":");
    resource.addPassword("Password:");

    if (gc.debug)
      {
	System.out.println("MenuItem action command: " + newPersona);
      }
    
    Hashtable result = null;
    String password = null;
      
    // All admin level personas have a : in them.  Only admin level
    // personas need passwords.
    
    if (newPersona.indexOf(":") > 0)
      {
	StringDialog d = new StringDialog(resource);
	result = d.DialogShow();
	
	if (result != null)
	  {
	    password = (String) result.get("Password:");
	  }
	else
	  {
	    gc.updatePersonaMenu();
	    return;		// they canceled.
	  }
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

	    //gc.setPersonaCombo(newPersona);

	    gc.ownerGroups = null;
	    gc.clearCaches();
	    gc.loader.clear();  // This reloads the hashes
	    gc.clearLoaderLists();
	    gc.cancelTransaction();
	    gc.buildTree();
	    gc.currentPersonaString = newPersona;
	    gc.setNormalCursor();

	    gc.setStatus("Successfully changed persona.");
	  }
	else
	  {
	    gc.showErrorMessage("Error: could not change persona", 
				"Perhaps the password was wrong.", gc.getErrorImage());
	      
	    gc.setStatus("Persona change failed");
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not set persona to " + newPersona + ": " + rx);
      }
    
    gc.updatePersonaMenu();
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       CacheInfo

  This may not be needed any more, since there are more hashes.

------------------------------------------------------------------------------*/

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
	    originalHandle = (ObjectHandle)handle.clone();

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

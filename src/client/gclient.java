/*
   gclient.java

   Ganymede client main module

   Created: 24 Feb 1997
   Version: $Revision: 1.63 $ %D%
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

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         gclient

------------------------------------------------------------------------------*/

public class gclient extends JFrame implements treeCallback,ActionListener {

  // we're only going to have one gclient at a time per running client

  public static gclient client;

  // ---
  
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

  final boolean debug = true;

  Session session;
  glogin _myglogin;

  long lastClick = 0;

  // set up a bunch of borders

  public EmptyBorder
    emptyBorder5 = (EmptyBorder)BorderFactory.createEmptyBorder(5,5,5,5),
    emptyBorder10 = (EmptyBorder)BorderFactory.createEmptyBorder(10,10,10,10);

  public BevelBorder
    raisedBorder = new BevelBorder(BevelBorder.RAISED),
    loweredBorder = new BevelBorder(BevelBorder.LOWERED);
      
  public LineBorder
    lineBorder = new LineBorder(Color.black);

  public CompoundBorder
    statusBorder = new CompoundBorder(loweredBorder, emptyBorder5),
    statusBorderRaised = new CompoundBorder(raisedBorder, emptyBorder5);

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
    inactivateHash = new Hashtable(),
    expireHash = new Hashtable(),
    reactivatedHash = new Hashtable(),
    removeHash = new Hashtable(),
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
    showToolbar = false,       // Show the toolbar
    somethingChanged = false;  // This will be set to true if the user changes anything
  
  helpPanel
    help = null;

  protected Vector
    filter = new Vector();    // List of owner groups to show, these are listHandles

  Vector
    personae,
    ownerGroups = null;  // Vector of owner groups

  // Dialog and GUI objects

  private JComboBox
    personaCombo = null;  // ComboBox showing current persona on the toolbar

  JFilterDialog
    filterDialog = null;

  JDefaultOwnerDialog
    defaultOwnerDialog = null;

  openObjectDialog
    openDialog;

  Image images[];

  JButton 
    commit,
    cancel;
  
  JTextField
    statusLabel;

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
    search,
    pencil,
    trash,
    creation;

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
    pMenuEditable= new treeMenu();
  
  JMenuBar 
    menubar;

  JMenuItem 
    logoutMI,
    removeAllMI,
    clearTreeMI,
    filterQueryMI,
    defaultOwnerMI,
    showHelpMI;

  private boolean
    defaultOwnerChosen = false;

  JMenuItem
    editObjectMI,
    viewObjectMI,
    cloneObjectMI,
    deleteObjectMI,
    inactivateObjectMI,
    menubarQueryMI = null;

  JCheckBoxMenuItem
    javaLFMI,
    motifMI,
    win95MI;

  String
    my_username;

  JMenu 
    actionMenu,
    windowMenu,
    fileMenu,
    helpMenu,
    LandFMenu,
    PersonaMenu = null;

  PersonaListener
    personaListener = null;

  WindowBar
    windowBar;

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
    super("Ganymede Client: "+g.my_client.getName()+" logged in");

    client = this;

    //System.out.println("Shortcut key mask: " + KeyEvent.getKeyText(Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    System.out.println("Starting gclient");


    enableEvents(AWTEvent.WINDOW_EVENT_MASK);

    if (s == null)
      {
	throw new IllegalArgumentException("Ganymede Error: Parameter for Session s is null");;
      }

    session = s;
    _myglogin = g;
    my_username = g.getUserName();

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
    logoutMI = new JMenuItem("Logout", new MenuShortcut(KeyEvent.VK_L));
    logoutMI.addActionListener(this);

    removeAllMI = new JMenuItem("Remove All Windows");
    removeAllMI.addActionListener(this);
    clearTreeMI = new JMenuItem("Clear Tree", new MenuShortcut(KeyEvent.VK_T));
    clearTreeMI.addActionListener(this);

    filterQueryMI = new JMenuItem("Filter Query");
    filterQueryMI.addActionListener(this);
    defaultOwnerMI = new JMenuItem("Set Default Owner");
    defaultOwnerMI.addActionListener(this);

    fileMenu.add(clearTreeMI);
    fileMenu.add(removeAllMI);
    fileMenu.add(filterQueryMI);
    fileMenu.add(defaultOwnerMI);
    fileMenu.addSeparator();
    fileMenu.add(logoutMI);

    // Action menu

    actionMenu = new JMenu("Actions");

    editObjectMI = new JMenuItem("Edit Object", new MenuShortcut(KeyEvent.VK_E));
    editObjectMI.setActionCommand("open object for editing");
    editObjectMI.addActionListener(this);

    viewObjectMI = new JMenuItem("View Object", new MenuShortcut(KeyEvent.VK_V));
    viewObjectMI.setActionCommand("open object for viewing");
    viewObjectMI.addActionListener(this);
    
    cloneObjectMI = new JMenuItem("Clone Object", new MenuShortcut(KeyEvent.VK_C));
    cloneObjectMI.setActionCommand("choose an object for cloning");
    cloneObjectMI.addActionListener(this);

    deleteObjectMI = new JMenuItem("Delete Object", new MenuShortcut(KeyEvent.VK_D));
    deleteObjectMI.setActionCommand("delete an object");
    deleteObjectMI.addActionListener(this);

    inactivateObjectMI = new JMenuItem("Inactivate Object", new MenuShortcut(KeyEvent.VK_I));
    inactivateObjectMI.setActionCommand("inactivate an object");
    inactivateObjectMI.addActionListener(this);

    menubarQueryMI = new JMenuItem("Query", new MenuShortcut(KeyEvent.VK_Q));
    menubarQueryMI.addActionListener(this);

    actionMenu.add(menubarQueryMI);
    actionMenu.addSeparator();
    actionMenu.add(editObjectMI);
    actionMenu.add(cloneObjectMI);
    actionMenu.add(viewObjectMI);
    actionMenu.add(deleteObjectMI);
    actionMenu.add(inactivateObjectMI);

    // windowMenu

    windowMenu = new JMenu("Windows");

    // Look and Feel menu

    LandFMenu = new arlut.csd.JDataComponent.LAFMenu(this);

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

    if (personae != null)
      {
	PersonaMenu = new JMenu("Persona");
	ButtonGroup personaGroup = new ButtonGroup();
	
	for (int i = 0; i < personae.size(); i++)
	  {
	    String p = (String)personae.elementAt(i);
	    JCheckBoxMenuItem mi = new JCheckBoxMenuItem(p, false);

	    if (p.equals(my_username))
	      {
		mi.setState(true);
	      }

	    personaGroup.add(mi);
	    mi.addActionListener(personaListener);
	    PersonaMenu.add(mi);
	  }

	personasExist = true;
      }

    // Help menu

    helpMenu = new JMenu("Help");
    showHelpMI = new JMenuItem("Help", new MenuShortcut(KeyEvent.VK_H));
    showHelpMI.addActionListener(this);
    helpMenu.add(showHelpMI);

    menubar.add(fileMenu);
    menubar.add(LandFMenu);
    menubar.add(actionMenu);
    menubar.add(windowMenu);
    menubar.add(helpMenu);
    menubar.setHelpMenu(helpMenu);

    if (personasExist)
      {
	menubar.add(PersonaMenu);
      }
    
    setJMenuBar(menubar);

    // Create menus for the tree

    pMenuAll.add(new MenuItem("List editable"));
    pMenuAll.add(new MenuItem("List all"));
    pMenuAll.add(new MenuItem("Create"));
    pMenuAll.add(new MenuItem("Query"));
    pMenuAll.add(new MenuItem("Hide Non-Editables"));

    pMenuEditable.add(new MenuItem("List editable"));
    pMenuEditable.add(new MenuItem("List all"));
    pMenuEditable.add(new MenuItem("Create"));
    pMenuEditable.add(new MenuItem("Query"));
    pMenuEditable.add(new MenuItem("Show All Objects"));

    if (debug)
      {
	System.out.println("Loading images for tree");
      }

    Image openFolder = PackageResources.getImageResource(this, "openfolder.gif", getClass());
    Image closedFolder = PackageResources.getImageResource(this, "folder.gif", getClass());
    Image list = PackageResources.getImageResource(this, "list.gif", getClass());
    Image listnowrite = PackageResources.getImageResource(this, "listnowrite.gif", getClass());
    Image redOpenFolder = PackageResources.getImageResource(this, "openfolder-red.gif", getClass());
    Image redClosedFolder = PackageResources.getImageResource(this, "folder-red.gif", getClass());
    
    search = PackageResources.getImageResource(this, "srchfol2.gif", getClass());
    trash = PackageResources.getImageResource(this, "trash.gif", getClass());
    creation = PackageResources.getImageResource(this, "creation.gif", getClass());
    pencil = PackageResources.getImageResource(this, "pencil.gif", getClass());

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
    objectRemovePM.add(new MenuItem("Clone Object"));
    objectRemovePM.add(new MenuItem("Delete Object"));

    objectInactivatePM = new treeMenu();
    objectInactivatePM.add(new MenuItem("View Object"));
    objectInactivatePM.add(new MenuItem("Edit Object"));
    objectInactivatePM.add(new MenuItem("Clone Object"));
    objectInactivatePM.add(new MenuItem("Inactivate Object"));

    objectReactivatePM = new treeMenu();
    objectReactivatePM.add(new MenuItem("View Object"));
    objectReactivatePM.add(new MenuItem("Edit Object"));;
    objectReactivatePM.add(new MenuItem("Clone Object"));
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
    commit.setOpaque(true);
    commit.setBackground(Color.lightGray);
    commit.setForeground(Color.black);
    commit.setToolTipText("Click this to commit all changes to database");
    commit.addActionListener(this);

    cancel = new JButton("Cancel");
    cancel.setOpaque(true);
    cancel.setBackground(Color.lightGray);
    cancel.setForeground(Color.black);
    cancel.setToolTipText("Click this to cancel all changes");
    cancel.addActionListener(this);

    // Button bar at bottom, includes commit/cancel panel and taskbar

    JPanel bottomButtonP = new JPanel(false);

    if (showToolbar)
      {
	rightTop.add("East", bottomButtonP);
      }
    else
      {
	//rightP.add(bottomButtonP,"South");
      }

    bottomButtonP.add(commit);
    bottomButtonP.add(cancel);

    // Create the pane splitter

    sPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftP, rightP);
   
    mainPanel.add("Center",sPane);

    // Create the bottomBar, for the bottom of the window

    JPanel bottomBar = new JPanel(false);
    bottomBar.setLayout(new BorderLayout());

    statusLabel = new JTextField();
    statusLabel.setEditable(false);
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
	session.openTransaction("gclient");
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not open transaction: " + rx);
      }

    timer.start();


    loader = new Loader(session);
    loader.start();

    pack();
    setSize(800, 600);
    show();
    
    setStatus("Ready.");
  }

  /**
   * Returns the templateHash.
   *
   * Template Hash is a hash of object type ID's (Short) -> Vector of FieldTemplates
   * Use this instead of templateHash directly, because you never know where we will
   * get it from(evil grin).
   */

  public Hashtable getTemplateHash()
  {
    return templateHash;
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
	    System.out.println("Found the template, using cache.");
	  }
	result = (Vector)th.get(id);

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

  public void update(Graphics g)
  {
    paint(g);
  }

  /** 
   * Get the session
   */

  public Session getSession()
  {
    return session;
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
  public final Vector getBaseList()
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

    statusLabel.setText(status);
    statusLabel.paintImmediately(statusLabel.getVisibleRect());
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
    //JOptionPane.showInternalMessageDialog(mainPanel, message, title, JOptionPane.ERROR_MESSAGE);
    JErrorDialog d = new JErrorDialog(this, title, message);
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
   * This indeicates that something in the database was changed, so canceling this transaction will have consequences.
   *
   * <p>This should be called whenever the client makes any changes to the database.  That includes
   * creating objects, editting fields of objects, removing objects, renaming, expiring, deleting,
   * inactivating, and so on.  It is very important to call this whenever something might have changed. </P>
   */
  public final void somethingChanged()
  {
      somethingChanged = true;
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
    System.err.println("** gclient: Entering handleReturnVal");

    while ((retVal != null) && (retVal.getDialog() != null))
      {
	// System.err.println("** gclient: retrieving dialog");

	JDialogBuff jdialog = retVal.getDialog();

	// System.err.println("** gclient: constructing dialog");

	StringDialog dialog = new StringDialog(jdialog.extractDialogRsrc(this));

	// System.err.println("** gclient: displaying dialog");

	setNormalCursor();

	Hashtable result = dialog.DialogShow();

	setWaitCursor();

	// System.err.println("** gclient: dialog done");

	if (retVal.getCallback() != null)
	  {
	    try
	      {
		System.out.println("Sending result to callback: " + result);
		retVal = retVal.getCallback().respond(result);
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("Caught remote exception: " + ex.getMessage());
	      }
	  }
	else
	  {
	    System.out.println("No callback, breaking");
	    break;		// we're done
	  }
      }

    if ((retVal == null) || retVal.didSucceed()) 
      {
	somethingChanged(); 
      }

    System.err.println("** gclient: Exiting handleReturnVal");

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
    toolBar.setBorderPainted(false);
    Insets insets = new Insets(0,0,0,0);
    
    toolBar.setMargin(insets);

    JButton b = new JButton(new ImageIcon(pencil));
    b.setActionCommand("open object for editing");
    b.setToolTipText("Edit an object");
    b.addActionListener(this);
    b.setMargin(insets);
    toolBar.add(b);

    b = new JButton(new ImageIcon(trash));
    b.setActionCommand("delete an object");
    b.setToolTipText("Delete an object");
    b.addActionListener(this);
    b.setMargin(insets);
    toolBar.add(b);

    b = new JButton(new ImageIcon(search));
    b.setActionCommand("open object for viewing");
    b.setToolTipText("View an object");
    b.setMargin(insets);
    b.addActionListener(this);
    toolBar.add(b);

    panel.add("West", toolBar);
    
    if ((personae != null)  && personae.size() > 0)
      {
	System.out.println("Adding persona stuff");
	
	personaCombo = new JComboBox();
	for(int i =0; i< personae.size(); i++)
	  {
	    personaCombo.addItem((String)personae.elementAt(i));
	  }
	personaCombo.setSelectedItem(my_username);

	personaCombo.addActionListener(personaListener);

	// Check this out
	JPanel Ppanel = new JPanel(new BorderLayout());
	Ppanel.add("Center", new JLabel("Persona:", SwingConstants.RIGHT));
	Ppanel.add("East", personaCombo);
	panel.add("Center", Ppanel);
      }
    else
      {
	System.out.println("No personas.");
      }

    // Now the connected timer.
    timerLabel = new JLabel("00:00:00", JLabel.RIGHT);
    timer = new connectedTimer(timerLabel, 5000, true);
    timerLabel.setMinimumSize(new Dimension(200,timerLabel.getPreferredSize().height));
    panel.add("East", timerLabel);

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
    Category firstCat = transport.getTree();

    System.out.println("got root category: " + firstCat.getName());

    CatTreeNode firstNode = new CatTreeNode(null, firstCat.getName(), firstCat,
					    null, true, 
					    OPEN_CAT, CLOSED_CAT, null);
    tree.setRoot(firstNode);

    try
      {
	recurseDownCategories(firstNode);
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

  void recurseDownCategories(CatTreeNode node) throws RemoteException
  {
    Vector
      children;

    Category c;
    CategoryNode cNode;

    treeNode 
      thisNode,
      prevNode;

    /* -- */
      
    c = node.getCategory();
    
    node.setText(c.getName());
    
    children = c.getNodes();

    prevNode = null;
    thisNode = node.getChild();

    for (int i = 0; i < children.size(); i++)
      {
	// find the CategoryNode at this point in the server's category tree
	cNode = (CategoryNode)children.elementAt(i);

	if (cNode instanceof Base)
	  {
	    Base base = (Base) cNode;
	    
	    if (base.isEmbedded())
	      {
		continue;	// we don't want to present embedded objects
	      }
	  }
	  
	prevNode = insertCategoryNode(cNode, prevNode, node);

	if (prevNode instanceof CatTreeNode)
	  {
	    recurseDownCategories((CatTreeNode)prevNode);
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
	newNode = new BaseNode(parentNode, base.getName(), base, prevNode,
			       true, 
			       OPEN_BASE, 
			       CLOSED_BASE,
			       pMenuEditable);
	shortToBaseNodeHash.put(new Short(base.getTypeID()), newNode);
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
	System.out.println("Unknown instance: " + node);
      }

    tree.insertNode(newNode, true);
      
    //    if (newNode instanceof BaseNode)
    //      {
    //	refreshObjects((BaseNode)newNode, false);
    //      }
    
    return newNode;
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

    // First get rid of deleted nodes

    Enumeration deleted = deleteHash.keys();

    while (deleted.hasMoreElements())
      {
	invid = (Invid)deleted.nextElement();
	node = (InvidNode)invidNodeHash.get(invid);
	if (node != null)
	  {
	    System.out.println("Deleteing node: " + node.getText());
	    tree.deleteNode(node, false);
	    invidNodeHash.remove(invid);
	  }
      }
    
    deleteHash.clear();
    invid = null;
    node = null;

    //
    // Now change the created nodes
    //

    Enumeration created = createHash.keys();
    while (created.hasMoreElements())
      {
	invid = (Invid)created.nextElement();
	node = (InvidNode)invidNodeHash.get(invid);
	if (node != null)
	  {
	    System.out.println("Committing created node: " + node.getText());
	    // change the icon
	    node.setImages(OPEN_FIELD, CLOSED_FIELD);
	    node.setText(session.viewObjectLabel(invid));
	  }
      }
    createHash.clear();
    invid = null;
    node = null;

    //
    // Last change the changed nodes.
    //

    Enumeration changed = changedHash.keys();
    while (changed.hasMoreElements())
      {
	invid = (Invid)changed.nextElement();
	node = (InvidNode)invidNodeHash.get(invid);
	if (node != null)
	  {
	    if (debug)
	      {
		// This shouldn't be the original label.
		System.out.println("Updating node: " + node.getText());
	      }

	    node.setText(session.viewObjectLabel(invid));
	    changedHash.remove(invid);
	    setIconForNode(invid);
	  }
      }

    if (!changedHash.isEmpty())
      {
	System.out.println("Changed hash is not empty, clearing.");

	changedHash.clear();
      }

    invid = null;
    node = null;
    
    changed = reactivatedHash.keys();

    while (changed.hasMoreElements())
      {
	invid = (Invid)reactivatedHash.get(changed);
	node = (InvidNode)invidNodeHash.get(invid);

	CacheInfo info = (CacheInfo)reactivatedHash.get(invid);

	if (cachedLists.containsList(info.getBaseID()))
	  {

	    objectList list = cachedLists.getList(info.getBaseID());
	    
	    ObjectHandle handle = list.getObjectHandle(invid);
	    if (handle != null)
	      {
		handle.setInactive(false);
		System.out.println("Setting handle to not-inactive.");
	      }
	  }
	  
	if (node != null)
	  {
	    if (debug)
	      {
		System.out.println("removing invid from reactivated hash");
	      }

	    // Maybe this one said "Inactivated" on it.
	    if (node.getText().indexOf("Inactivated") > 0)
	      {
		System.out.println("Fixing this one: " + node.getText()); 
		node.setText(node.getText().substring(0, node.getText().indexOf("(Inactivated)")));
	      }
	    
	    reactivatedHash.remove(invid);
	    setIconForNode(invid);
	  }
      }

    tree.refresh();
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

    short id;

    /* -- */

    base = node.getBase();    

    try
      {
	id = base.getTypeID();
	//Now get all the children
	_query = new Query(id, null, false);// include all, even non-editables
	node.setQuery(_query);
      }
    catch (RemoteException rx)
      {
	throw new IllegalArgumentException("It's the Query! " + rx);
      }

    Short Id = new Short(id);

    if (cachedLists.containsList(Id))
      {
	objectlist = cachedLists.getList(Id);
      }
    else
      {
	try
	  {
	    QueryResult qr = session.query(_query);

	    if (qr != null)
	      {
		System.out.println("Caching copy");
		objectlist = new objectList(qr);
		cachedLists.putList(Id, objectlist);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get dump: " + rx);
	  }
      }
    
    objectHandles = objectlist.getObjectHandles(true); // include inactives, non-editables

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

    parentNode = node;
    oldNode = null;
    fNode = (InvidNode) node.getChild();
    int i = 0;
	
    while ((i < objectHandles.size()) || (fNode != null))
      {
	//System.out.println("Looking at the next node");
	//System.out.println("i = " + i + " length = " + unsorted_objects.length);
	
	if (i < objectHandles.size())
	  {
	    handle = (ObjectHandle) objectHandles.elementAt(i);

	    if (!node.isShowAll() && !handle.isEditable())
	      {
		i++;		// skip this one
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

	    //System.out.println("Object is null");

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
	    
	    invidNodeHash.put(invid, objNode);
	    setIconForNode(invid);
	   
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

	    // System.out.println("Removing this node");
	    // System.err.println("Deleting: " + fNode.getText());

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

	    // System.err.println("Setting: " + object.getName());

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
   * This method changes the icon for the tree node for the
   * provided invid, depending on the various hashes and the object's
   * objectHandle.
   *
   */

  public void setIconForNode(Invid invid)
  {
    InvidNode node = (InvidNode)invidNodeHash.get(invid);

    if (node == null)
      {
	return;
      }

    ObjectHandle handle = node.getHandle();

    if (node == null)
      {
	System.out.println("There is no node for this invid, silly!");
      }
    else
      {
	// if we can't edit it, assume it'll never be anything other
	// than inaccessible

	if (!handle.isEditable())
	  {
	    node.setImages(OBJECTNOWRITE, OBJECTNOWRITE);
	    return;
	  }

	if (deleteHash.containsKey(invid))
	  {
	    if (true)
	      {
		System.out.print("Setting icon to delete.");
	      }
	    node.setImages(OPEN_FIELD_DELETE, CLOSED_FIELD_DELETE);
	  }
	else if (createHash.containsKey(invid))
	  {
	    if (true)
	      {
		System.out.print("Setting icon to create.");
	      }
	    node.setImages(OPEN_FIELD_CREATE, CLOSED_FIELD_CREATE);
	  }
	else if (removeHash.containsKey(invid))
	  {
	    System.out.println("remove");
	    node.setImages(OPEN_FIELD_REMOVESET, CLOSED_FIELD_REMOVESET);
	  }
	else if (expireHash.containsKey(invid))
	  {
	    System.out.println("expire");
	    node.setImages(OPEN_FIELD_EXPIRESET, CLOSED_FIELD_EXPIRESET);
	  }
	else if (inactivateHash.containsKey(invid))
	  {
	    System.out.println("inactivate");
	    node.setMenu(objectReactivatePM);
	    String text = node.getText();
	    if (text.indexOf(" (inactive)") > 0)
	      {
		System.out.println("It already says inactivated.");
	      }
	    else
	      {
		node.setText(node.getText() + " (inactive)");
	      }

	    node.setImages(OPEN_FIELD_REMOVESET, CLOSED_FIELD_REMOVESET);
	    tree.refresh();
	  }
	else if (changedHash.containsKey(invid))
	  {
	    if (debug)
	      {
		System.out.println("Setting icon to edit.");
	      }
	    node.setImages(OPEN_FIELD_CHANGED, CLOSED_FIELD_CHANGED);
	  }
	else if (reactivatedHash.containsKey(invid))
	  {
	    int index = node.getText().indexOf(" (inactive)");
	    if (index > 0)
	      {
		System.out.println("Fixing this one: " + node.getText()); 
		node.setText(node.getText().substring(0, index));
	      }
	    node.setMenu(objectInactivatePM);
	    node.setImages(OPEN_FIELD, CLOSED_FIELD);
	    tree.refresh();
	  }
	else if (handle != null)
	  {
	    if (handle.isExpirationSet())
	      {
		System.out.println("isExpirationSet");
		node.setMenu(objectReactivatePM);
		node.setImages(OPEN_FIELD_EXPIRESET, CLOSED_FIELD_EXPIRESET);
	      }
	    else if (handle.isRemovalSet())
	      {
		System.out.println("isRemovalSet()");
		node.setMenu(objectReactivatePM);
		node.setImages(OPEN_FIELD_REMOVESET, CLOSED_FIELD_REMOVESET);
	      }
	    else // nothing special in handle
	      {

		node.setImages(OPEN_FIELD, CLOSED_FIELD);
	      } 
	  }
	else // no handle
	  {
		System.out.println("normal");
	    node.setImages(OPEN_FIELD, CLOSED_FIELD);
	  }
      }

  }

  ///////////////////////////////
  ///// Hash stuff
  //////////////////////////////

  /**
   * This adds an invid to the expireHash.
   *
   * Objects in the expire hash have had their expiration dates set in the current session.
   */
  public void addToExpireHash(Invid invid)
  {
    if (! expireHash.containsKey(invid))
      {
	try
	  {
	    Short type = new Short(invid.getType());
	    ObjectHandle handle = getObjectHandle(invid, type);
	    expireHash.put(invid, new CacheInfo(type, session.viewObjectLabel(invid), null, handle));
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("coult not update expireHash: " + rx);
	  }
      }
    setIconForNode(invid);
    tree.refresh();
  }
  /**
   * Add invid to the removal hash.
   *
   * Things in the removal hash have been removed in the current session.
   */

  public void addToRemoveHash(Invid invid)
  {
    if ( ! removeHash.containsKey(invid))
      {
	try
	  {
	    Short type = new Short(invid.getType());
	    ObjectHandle handle = getObjectHandle(invid, type);
	    removeHash.put(invid, new CacheInfo(type, session.viewObjectLabel(invid), null, handle));
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not update removeHash: " + rx);
	  }
      }
    setIconForNode(invid);

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
    try
      {
	db_object o = session.edit_db_object(invid);
	if (o == null)
	  {
	    setStatus("edit_db_object returned a null pointer, aborting");
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
	  
	changedHash.put(invid, new CacheInfo(type, session.viewObjectLabel(invid), null, handle));

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
    try
      {
	obj = session.create_db_object(type);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Exception creating new object: " + rx);
      }

    if (obj == null)
      {
	throw new RuntimeException("Could not create object for some reason.  Check the Admin console.");
      }

    if (showNow)
      {
	showNewlyCreatedObject(obj, invid, new Short(type));
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
    ObjectHandle handle = new ObjectHandle("New Object", invid, false, false, false, true);
       
    wp.addWindow(obj, true);
    
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
					      "New Object", 
					      invid,
					      null, false,
					      OPEN_FIELD_CREATE,
					      CLOSED_FIELD_CREATE,
					      baseN.canInactivate() ? objectInactivatePM : objectRemovePM,
					      handle);
	    
	    createHash.put(invid, new CacheInfo(type, "New Object", null, handle));
	    
	    invidNodeHash.put(invid, objNode);
	    setIconForNode(invid);
	    
	    tree.insertNode(objNode, true);
	  }
      }
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
	wp.addWindow(session.view_db_object(invid), false, objectType);
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

		System.out.println("This base has been hashed.  Removing: " + label);

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
		System.out.println("already deleted, nothing to change, right?");
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
	System.out.println("Canceled");
      }
    else
      {
	try
	  {
	    StringDialog d = new StringDialog(this, 
					      "Verify invalidation", 
					      "Are you sure you want to inactivate " + 
					      session.viewObjectLabel(invid), "Yes", "No");
	    Hashtable result = d.DialogShow();

	    if (result == null)
	      {
		setStatus("Cancelled!");
	      }
	    else
	      {
		setStatus("inactivating " + invid);

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

		    inactivateHash.put(invid, new CacheInfo(type, session.viewObjectLabel(invid), null, handle));
		    setIconForNode(invid);
		    tree.refresh();
		    setStatus("Object inactivated.");
		  }
		else
		  {
		    setStatus("Could not inactivate object.");
		  }
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not verify invid to be inactivated: " + rx);
	  }
      }

    return ok;
  }

  /** 
   * Reactivate an object.
   *
   * This is to reactivate a deacivated object.  I think you should call this from the 
   * expiration date panel if the date is cleared.
   */

  public boolean reactivateObject(Invid invid)
  {
    ReturnVal retVal;
    boolean ok = false;

    try
      {
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

	setStatus("Object reactivated.");

	// We need to fix the handle.
	ObjectHandle h = null;
	ObjectHandle oh = null;
	Short type = null;


	type = new Short(invid.getType());	

	if (cachedLists.containsList(type))
	  {
	    System.out.println("Getting the handle");
	    objectList list = cachedLists.getList(type);
	    h = list.getObjectHandle(invid);
	    try
	      {
		oh = (ObjectHandle)h.clone();
	      }
	    catch (Exception x)
	      {
		System.out.println("Can't clone the ObjectHandle: " + x);
	      }

	    if (retVal == null)
	      {
		// set everthing to false
		h.setRemovalSet(false);
		h.setExpirationSet(false);
		h.setInactive(false);
	      }
	    else
	      {
		// need to make some decisions
		h.setRemovalSet(false);
		h.setInactive(false);
		if (retVal.getObjectStatus() == ReturnVal.EXPIRATIONSET)
		  {
		    System.out.println("Still expriation set.");
		    h.setExpirationSet(true);
		  }
		else
		  {
		    System.out.println("Clearing everything.");
		    h.setExpirationSet(false);
		  }
	      }
	  }

	// If this is in the inactivateHash, then it was just inactivated in this 
	// session.  If not, it was inactive before, so we need to stick it in 
	// the reactivatedHash, because we might need to change the icon back if
	// the session is canceled.

	if (inactivateHash.containsKey(invid))
	  {
	    System.out.println("-removing from inactivateHash");
	    inactivateHash.remove(invid);
	    
	    // Probably should get the original handle, and stick it in the real cache

	  }
	else
	  {
	    try
	      {
		if (h == null)
		  {
		    System.out.println("The handle is null.");
		  }
		reactivatedHash.put(invid, new CacheInfo(type ,session.viewObjectLabel(invid), null, h, oh));
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not get object label. " + rx);
	      }
	  }

      }
    
    setIconForNode(invid);
    return ok;
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

    openDialog.setText("Open object for editing");

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	System.out.println("Canceled");
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

    openDialog.setText("Open object for viewing");

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	System.out.println("Canceled");
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

    openDialog.setText("Choose object to be cloned");

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	System.out.println("Canceled");
      }
    else
      {
	try
	  {
	    wp.addWindow(session.clone_db_object(invid), true);
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

    /* -- */

    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }

    openDialog.setText("Choose object to be inactivated");

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

    openDialog.setText("Choose object to be deleted");

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	System.out.println("Canceled");
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
   * Logout from the client.
   *
   * This method does not do any checking, it just logs out.
   */
  void logout()
  {
    try
      {
	timer.stop();
	session.logout();
	this.dispose();
	_myglogin.enableButtons(true);
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
    // What to do here?  don't check for null, because maybe forcePopup was false.
    // Have to think about this one, maybe keep groups Vector in gclient (ie not local here)
    // yeah, looks like that's what I did
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
	throw new RuntimeException("Whoa!  groups is empty");
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
		session.setDefaultOwner(owners);
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
    else
      {
	defaultOwnerDialog.setVisible(true);
      }

    defaultOwnerChosen =  true;

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
    if (somethingChanged)
      {
	StringDialog dialog = new StringDialog(this, 
					       "Warning: changes have been made",
					       "You have made changes in objects without \ncommiting those changes.  If you continue, \nthose changes will be lost",
					       "Discard Changes",
					       "Cancel");
	// if DialogShow is null, cancel was clicked
	// So return will be false if cancel was clicked
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
	framePanel fp = (framePanel)windows.elementAt(i);

	if (fp == null)
	  {
	    System.out.println("null frame panel in updateNotesPanels");
	  }
	else
	  {
	    notesPanel np = fp.getNotesPanel();
	    if (np == null)
	      {
		System.out.println("null notes panel in frame panel");
	      }
	    else
	      {
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
	
	wp.closeEditables();

	retVal = session.commitTransaction();

	if (retVal != null)
	  {
	    retVal = handleReturnVal(retVal);
	  }

	boolean succeeded = (retVal == null) ? true : retVal.didSucceed();

	if (succeeded)
	  {
	    somethingChanged = false;
	
	    // Might need to fix the tree nodes
	    // Now go through changed list and revert any names that may be needed

	    /*	    
	    Enumeration changed = changedHash.keys();
	    
	    while (changed.hasMoreElements())
	      {
		Invid invid = (Invid)changed.nextElement();
		CacheInfo info = (CacheInfo)changedHash.get(invid);
		String label = null;
		
		try
		  {
		    label = session.viewObjectLabel(invid);
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not get label: " + rx);
		  }
	        
		InvidNode node = (InvidNode)invidNodeHash.get(invid);
		
		if (node != null)
		  {
		    node.setText(label);
		  }
		
		changedHash.remove(invid);
		setIconForNode(invid);
	      }	    
	    */
	      
	    wp.refreshTableWindows();
	    session.openTransaction("gclient");

	    System.out.println("Done committing");

	    //
	    // This fixes all the icons in the tree
	    //

	    refreshTreeAfterCommit();

	    setNormalCursor();

	    cachedLists.clearCaches();
	
	    wp.resetWindowCount();
	  }
	else
	  {
	    showErrorMessage("Error: commit failed", "Could not commit your changes.");
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not commit transaction" + rx);
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
	    System.out.println("Shutting down containerPanel");
	    containerPanel cp = (containerPanel)containerPanels.elementAt(i);
	    
	    cp.stopLoading();
	  }
	
	long startTime = System.currentTimeMillis(); // Only going to wait 10 seconds

	while ((containerPanels.size() > 0) && (System.currentTimeMillis() - startTime < 10000))
	  {
	    try
	      {
		System.out.println("Waiting for containerPanels to shut down.");
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
	
	wp.closeEditables();

	retVal = session.abortTransaction();

	if (retVal != null)
	  {
	    retVal = handleReturnVal(retVal);
	  }

	if (retVal == null)
	  {
	    if (debug)
	      System.out.println("Cancel succeeded");
	  }
	else
	  {
	    if (retVal.didSucceed())
	      {
		System.out.println("Cancel succeeded");
	      }
	    else
	      {
		System.out.println("Everytime I think I'm out, they pull me back in!  Something went wrong with the cancel.");
		return;
	      }
	  }

	// Now we need to fix up the caches, and clean up all the changes made
	// during the transaction

	// any objects that we 'deleted' we'll clear the deleted bit

	Invid invid;
	InvidNode node;
	objectList list;
	CacheInfo info;

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
		    System.out.println("Can't fool me: you just created this object!");
		  }
		else
		  {
		    System.out.println("This one is hashed, sticking it back in.");


		    
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
	    invid = (Invid)created.nextElement();
	    info = (CacheInfo)createHash.get(invid);

	    list = cachedLists.getList(info.getBaseID());
	    if (list != null)
	      {
		System.out.println("This one is hashed, taking a created object out.");

		list.removeInvid(invid);
	      }
	    createHash.remove(invid);
	    setIconForNode(invid);
	  }

	// Now go through changed list and revert any names that may be needed
	invid = null;
	list = null;
	info = null;
	Enumeration changed = changedHash.keys();

	while (changed.hasMoreElements())
	  {
	    invid = (Invid)changed.nextElement();
	    info = (CacheInfo)changedHash.get(invid);
	    
	    list = cachedLists.getList(info.getBaseID());
	    if (list != null)
	      {
		System.out.println("This changed base is cached, fixing it back.");

		list.removeInvid(invid);
		handle = info.getOriginalObjectHandle();
		list.addObjectHandle(handle);
		node = (InvidNode)invidNodeHash.get(invid);
		if (node != null)
		  {
		    node.setHandle(handle);
		  }
	      }
	    changedHash.remove(invid);
	    setIconForNode(invid);
	  }

	node = null;
	invid = null;
	list = null;
	info = null;

	changed = reactivatedHash.keys();
	while (changed.hasMoreElements())
	  {
	    invid = (Invid)changed.nextElement();
	    node = (InvidNode)invidNodeHash.get(invid);
	    info = (CacheInfo)reactivatedHash.get(invid); 

	    list = cachedLists.getList(info.getBaseID());
	    if (list != null)
	      {

		ObjectHandle original = info.getOriginalObjectHandle();
		if (original != null)
		  {
		    System.out.println("Reverting to original ObjectHandle.  isInactive: " + original.isInactive() + " isRemovaldate: " + original.isRemovalSet() + " isExpire: " + original.isExpirationSet());
		    
		    list.removeInvid(invid);
		    list.addObjectHandle(original);
		    node = (InvidNode)invidNodeHash.get(invid);
		    if (node != null)
		      {
			node.setHandle(original);
		      }
		  }
		else
		  {
		    System.out.println("No original to put in...");
		  }
	      }

	    reactivatedHash.remove(invid);

	    // Do a little more stuff to the node here, because this should
	    // be just a normal object.  setIconForNode doesn't change
	    // the menu or text of a normal object, so I want to do it here.
	    //
	    // If setIconForNode changed the menu and text for everything, it
	    // would be too slow, because most objects are not in the reactivate/inactivate
	    // group.

	    System.out.println("Fixing text of reactivated object.");
	    node.setMenu(objectReactivatePM);  // set it back to the reactivate method
	    int index = node.getText().indexOf(" (inactive)");
	    if (index < 0)
	      {
		System.out.println("Fixing this one: " + node.getText()); 
		node.setText(node.getText() + " (inactive)");
	      }

	    setIconForNode(invid);
	  }

	invid = null;
	node = null;
	info = null;
	list = null;
	
	changed = inactivateHash.keys();
	while (changed.hasMoreElements())
	  {
	    invid = (Invid)changed.nextElement();
	    info = (CacheInfo)inactivateHash.get(invid);
	    
	    node = (InvidNode)invidNodeHash.get(invid);
	    if (node.getText().indexOf("Inactivated") > 0)
	      {
		System.out.println("Fixing this one: " + node.getText()); 
		node.setText(node.getText().substring(0, node.getText().indexOf("(Inactivated)")));
	      }

	    list = cachedLists.getList(info.getBaseID());
	    if (list != null)
		{
		  list.removeInvid(invid);
		  handle = info.getOriginalObjectHandle();
		  list.addObjectHandle(handle);
		  node = (InvidNode)invidNodeHash.get(invid);
		  if (node != null)
		    {
		      node.setHandle(handle);
		    }
		}

	    inactivateHash.remove(invid);
	    node.setMenu(objectInactivatePM);
	    setIconForNode(invid);
	  }

	if (createHash.isEmpty() && deleteHash.isEmpty() && changedHash.isEmpty() && inactivateHash.isEmpty() && reactivatedHash.isEmpty())
	  {
	    System.out.println("Woo-woo the hashes are all empty");
	  }

	somethingChanged = false;
	session.openTransaction("glient");
	tree.refresh();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not abort transaction" + rx);
      }
  }

  // ActionListener Methods
  
  public void actionPerformed(java.awt.event.ActionEvent event)
  {
    Object source = event.getSource();
    String command = event.getActionCommand();
    System.out.println("Action: " + command);
    
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
	querybox box = new querybox(getBaseHash(), getBaseMap(), this, "Query Panel");
	Query q = box.myshow();

	if (q != null)
	  {
	    DumpResult buffer = null;

	    try
	      {
		buffer = session.dump(q);
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("caught remote: " + ex);
	      }

	    wp.addTableWindow(session, q, buffer, "Query Results");
	  }
      }
    else if (source == removeAllMI)
      {
	if (OKToProceed())
	  {
	    wp.closeAll();
	  }
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
    else
      {
	System.err.println("Unknown action event generated");
      }
  }
  
  protected void processWindowEvent(WindowEvent e) 
  {
    super.processWindowEvent(e);

    if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
	System.out.println("Window closing");

	if (OKToProceed())
	  {
	    if (debug)
	      {
		System.out.println("It's ok to log out.");
	      }
	    logout();
	  }
	else if (debug)
	  {
	    System.out.println("No log out!");
	  }
      }
  }

  // treeCallback methods

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

  public void treeNodeContracted(treeNode node)
  {
  }

  public void treeNodeSelected(treeNode node)
  {
    validate();
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
	System.out.println("createMI");

	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;

	    try
	      {
		short id = baseN.getBase().getTypeID();
		createObject(id, true);

	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not create object: " + rx);
	      }
	  }
	else
	  {
	    System.err.println("not a base node, can't create");
	  }
      }
    else if (event.getActionCommand().equals("List editable"))
      {
	System.out.println("viewMI");

	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;

	    try
	      {
		Query _query = new Query(baseN.getBase().getTypeID(), null, true);

		setStatus("Sending query for base " + node.getText() + " to server");

		DumpResult buffer = session.dump(_query);

		if (buffer == null)
		  {
		    setStatus("results == null");
		    System.out.println("results == null");
		  }
		else
		  {
		    setStatus("Server returned results for query on base " + node.getText() + " - building table");

		    System.out.println();

		    wp.addTableWindow(session, baseN.getQuery(), buffer, "Query Results");
		  }
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not get query: " + rx);
	      }
	  }
	else
	  {
	    System.out.println("viewMI from a node other than a BaseNode");
	  }
      }
    else if (event.getActionCommand().equals("List all"))
      {
	if (debug)
	  {
	    System.out.println("viewAllMI");
	  }
	
	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;
	    
	    try
	      {
		Query _query = new Query(baseN.getBase().getTypeID(), null, false);

		setStatus("Sending query for base " + node.getText() + " to server");

		DumpResult buffer = session.dump(_query);
		
		if (buffer == null)
		  {
		    setStatus("results == null");
		    System.out.println("results == null");
		  }
		else
		  {
		    setStatus("Server returned results for query on base " + node.getText() + " - building table");

		    System.out.println();
		    
		    wp.addTableWindow(session, baseN.getQuery(), buffer, "Query Results");
		  }
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not get query: " + rx);
	      }
	  }
	else
	  {
	    System.out.println("viewAllMI from a node other than a BaseNode");
	  }
      }
    else if (event.getActionCommand().equals("Query"))
      {
	System.out.println("queryMI");

	if (node instanceof BaseNode)
	  {
	    Base base = ((BaseNode) node).getBase();

	    querybox box = new querybox(base, getBaseHash(), getBaseMap(),  this, "Query Panel");

	    Query q = box.myshow();

	    if (q != null)
	      {
		DumpResult buffer = null;

		try
		  {
		    setStatus("Sending query for base " + node.getText() + " to server");

		    buffer = session.dump(q);
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("caught remote: " + ex);
		  }

		if (buffer != null)
		  {
		    setStatus("Server returned results for query on base " + 
			      node.getText() + " - building table");
		    
		    wp.addTableWindow(session, q, buffer, "Query Results");
		  }
		else
		  {
		    setStatus("results == null");
		    System.out.println("results == null");
		  }
	      }
	  }
      }
    else if (event.getActionCommand().equals("Show All Objects"))
      {
	BaseNode bn = (BaseNode) node;
	Base base = bn.getBase();
	Short id;

	/* -- */

	try
	  {
	    id = new Short(base.getTypeID());
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("couldn't get the base id" + ex);
	  }

	bn.showAll(true);
	node.setMenu(pMenuAll);

	if (bn.isOpen())
	  {
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
    else if (event.getActionCommand().equals("Hide Non-Editables"))
      {
	BaseNode bn = (BaseNode) node;
	Base base = bn.getBase();
	Short id;

	/* -- */

	try
	  {
	    id = new Short(base.getTypeID());
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("couldn't get the base id" + ex);
	  }

	bn.showAll(false);
	bn.setMenu(pMenuEditable);

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
	System.out.println("objEditMI");

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
	System.out.println("Deleting object");
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
	System.out.println("objCloneMI");
      }
    else if(event.getActionCommand().equals("Inactivate Object"))
      {
	System.out.println("objInactivateMI");
	if (node instanceof InvidNode)
	  {
	    inactivateObject(((InvidNode)node).getInvid());
	  }
      }
    else if (event.getActionCommand().equals("Reactivate Object"))
      {
	System.out.println("Reactivate item.");
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
  private Query query;
  private boolean loaded = false;
  private boolean canBeInactivated = false;
  private boolean showAll = false;

  /* -- */

  BaseNode(treeNode parent, String text, Base base, treeNode insertAfter,
	   boolean expandable, int openImage, int closedImage, treeMenu menu)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
    this.base = base;
    
    try
      {
	canBeInactivated = base.canInactivate();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check inactivate.");
      }
  }

  public Base getBase()
  {
    return base;
  }

  public boolean canInactivate()
  {
    return canBeInactivated;
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

  public void setQuery(Query query)
  {
    this.query = query;
  }

  public Query getQuery()
  {
    return query;
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

class PersonaListener implements ActionListener{

  Session session;

  DialogRsrc
    resource = null;

  gclient
    gc;

  PersonaListener(Session session, gclient parent)
    {
      this.session = session;
      this.gc = parent;
    }

  public void actionPerformed(ActionEvent event)
  {
    //Check to see if we need to commit the transaction first.
    String newPersona = null;

    if (event.getSource() instanceof JMenuItem)
      {
	System.out.println("From menu");
	//JMenuItem good
	newPersona = event.getActionCommand();
      }
    else if (event.getSource() instanceof JComboBox)
      {
	System.out.println("From box");
	//JComboBox bad
	newPersona = (String)((JComboBox)event.getSource()).getSelectedItem();
      }
    else
      {
	System.out.println("Persona Listener doesn't understand that action.");
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

    if (resource == null)
      {
	resource = new DialogRsrc(gc, "Change Persona", "Enter the persona password:");
	resource.addPassword("Password:");
      }

      System.out.println("MenuItem action command: " + newPersona);
      
      Hashtable result = null;
      String password = null;

      StringDialog d = new StringDialog(resource);
      result = d.DialogShow();
      
      if (result != null)
	{
	  password = (String)result.get("Password:");
	}
      else
	{
	  return;		// they canceled.
	}

      if (password != null)
	{
	  try
	    {	      
	      personaChangeSuccessful = session.selectPersona(newPersona, password);
	      
	      if (personaChangeSuccessful)
		{
		  gc.setStatus("Successfully changed persona.");
		  gc.setTitle("Ganymede Client: " + newPersona + " logged in.");
		  //gc.setPersonaCombo(newPersona);
		  gc.ownerGroups = null;
		  gc.clearCaches();
		  gc.loader.clear();  // This reloads the hashes
		  gc.commitTransaction();
		  gc.buildTree();
		}
	      else
		{
		  gc.setStatus("Danger Danger!");
		  (new StringDialog(gc, "Error: persona no changie", 
				    "Could not change persona.",
				    false)).DialogShow();

		  gc.setStatus("Persona change failed");
		}
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not set persona to " + newPersona + ": " + rx);
	    }

	}
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
	    System.out.println("a cloned handle.");
	  }
	catch (Exception x)
	  {
	    originalHandle = null;
	    System.out.println("Clone is not supported: " + x);
	  }
      }
    else
      {
	originalHandle = null;
	System.out.println("a null handle.");
      }
  }

  public CacheInfo(Short baseID, String originalLabel, String currentLabel, ObjectHandle handle, ObjectHandle originalHandle)
  {
    System.out.print(">>>>Creating new cache info with ");

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

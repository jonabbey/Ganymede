/*
   gclient.java

   Ganymede client main module

   Created: 24 Feb 1997
   Version: $Revision: 1.42 $ %D%
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
import arlut.csd.JDataComponent.*;
import arlut.csd.DataComponent.*;
import arlut.csd.ganymede.client.*;
import arlut.csd.Util.*;
import arlut.csd.Tree.*;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         gclient

------------------------------------------------------------------------------*/

public class gclient extends JFrame implements treeCallback,ActionListener {
  
  // Image numbers
  final int NUM_IMAGE = 12;
  
  final int OPEN_BASE = 0;
  //  final int OPEN_BASE_DELETE = 1;
  //final int OPEN_BASE_CREATE = 2;
  //final int OPEN_BASE_CHANGED = 3;
  final int CLOSED_BASE = 1;
  //final int CLOSED_BASE_DELETE = 5;
  //final int CLOSED_BASE_CREATE = 6;
  //final int CLOSED_BASE_CHANGED = 7;

  final int OPEN_FIELD = 2;
  final int OPEN_FIELD_DELETE = 3;
  final int OPEN_FIELD_CREATE = 4;
  final int OPEN_FIELD_CHANGED = 5;
  final int CLOSED_FIELD = 6;
  final int CLOSED_FIELD_DELETE = 7;
  final int CLOSED_FIELD_CREATE = 8;
  final int CLOSED_FIELD_CHANGED = 9;

  final int OPEN_CAT = 10;
  final int CLOSED_CAT = 11;

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


  private Vector
    baseList;            // List of base types.  Vector of Bases.

  private Hashtable
    baseHash = null,	             // used to reduce the time required to get listings
                                     // of bases and fields.. keys are Bases, values
		      	             // are vectors of fields
    baseMap = null,                  // Hash of Short to Base
    changedHash = new Hashtable(),   // Hash of objects that might have changed
    deleteHash = new Hashtable(),    // Hash of objects waiting to be deleted
    createHash = new Hashtable();    // Hash of objects waiting to be created
                                     // Create and Delete are pending on the Commit button. 
   
  protected Hashtable
    templateHash,
    cachedLists = new Hashtable();


  Loader 
    loader;      // Use this to do start up stuff in a thread
  
  boolean
    somethingChanged = false;
  
  protected Vector
    filter = new Vector();    // List of owner groups to show, these are listHandles

  Vector
    ownerGroups = null;  // Vector of owner groups

  JFilterDialog
    filterDialog = null;

  JDefaultOwnerDialog
    defaultOwnerDialog = null;

  Image images[];

  JButton 
    commit,
    cancel;
  
  JTextField
    statusLabel;

  JSplitPane
    sPane;

  treeControl tree;

  // The top lines
  JPanel
    leftP,
    leftTop,
    rightTop,
    mainPanel;   //Everything is in this, so it is double buffered

  public JLabel
    leftL,
    rightL,
    timerLabel;

  connectedTimer
    timer;

  windowPanel
    wp;

  treeMenu objectPM;
  MenuItem
    objViewMI,
    objEditMI,
    objCloneMI,
    objInactivateMI,
    objDeleteMI;

  treeMenu 
    pMenu = new treeMenu();
  
  MenuItem 
    createMI = null,
    viewMI = null,
    viewAllMI = null,
    queryMI = null;


  JMenuBar 
    menubar;

  JMenuItem 
    logoutMI,
    removeAllMI,
    rebuildTreeMI,
    filterQueryMI,
    defaultOwnerMI;

  private boolean
    defaultOwnerChosen = false;

  JMenuItem
    editObjectMI,
    viewObjectMI,
    cloneObjectMI,
    deleteObjectMI,
    inactivateObjectMI,
    menubarQueryMI = null;

  JMenuItem
    roseMI,
    win95MI;

  JMenu 
    actionMenu,
    windowMenu,
    fileMenu,
    LandFMenu,
    PersonaMenu = null;

  PersonaListener
    personaListener = null;

  WindowBar
    windowBar;

  openObjectDialog
    openDialog;

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

    System.out.println("Starting gclient");


    enableEvents(AWTEvent.WINDOW_EVENT_MASK);

    if (s == null)
      {
	throw new IllegalArgumentException("Ganymede Error: Parameter for Session s is null");;
      }

    session = s;
    _myglogin = g;

    mainPanel = new JPanel(true);
    mainPanel.setLayout(new BorderLayout());

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add("Center", mainPanel);

    //BorderLayout layout = new BorderLayout();
    //setLayout( layout );

    templateHash = new Hashtable();

    if (debug)
      {
	System.out.println("Creating menu bar");
      }

    // Make the menu bar
    menubar = new JMenuBar();    
    menubar.setBorderPainted(true);
    
    // File menu
    fileMenu = new JMenu("File");
    logoutMI = new JMenuItem("Logout");
    logoutMI.addActionListener(this);
    removeAllMI = new JMenuItem("Remove All Windows");
    removeAllMI.addActionListener(this);
    rebuildTreeMI = new JMenuItem("Rebuild Tree");
    rebuildTreeMI.addActionListener(this);

    filterQueryMI = new JMenuItem("Filter Query");
    filterQueryMI.addActionListener(this);
    defaultOwnerMI = new JMenuItem("Set Default Owner");
    defaultOwnerMI.addActionListener(this);

    fileMenu.add(rebuildTreeMI);
    fileMenu.add(removeAllMI);
    fileMenu.add(filterQueryMI);
    fileMenu.add(defaultOwnerMI);
    fileMenu.addSeparator();
    fileMenu.add(logoutMI);


    // Action menu
    actionMenu = new JMenu("Actions");

    editObjectMI = new JMenuItem("Edit Object");
    editObjectMI.setActionCommand("open object for editing");
    editObjectMI.addActionListener(this);

    viewObjectMI = new JMenuItem("View Object");
    viewObjectMI.setActionCommand("open object for viewing");
    viewObjectMI.addActionListener(this);
    
    cloneObjectMI = new JMenuItem("Clone Object");
    cloneObjectMI.setActionCommand("choose an object for cloning");
    cloneObjectMI.addActionListener(this);

    deleteObjectMI = new JMenuItem("Delete Object");
    deleteObjectMI.setActionCommand("delete an object");
    deleteObjectMI.addActionListener(this);

    inactivateObjectMI = new JMenuItem("Inactivate Object");
    inactivateObjectMI.setActionCommand("inactivate an object");
    inactivateObjectMI.addActionListener(this);

    menubarQueryMI = new JMenuItem("Query");
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
    LandFMenu = new JMenu("Look");
    roseMI = new JMenuItem("Rose");
    roseMI.addActionListener(this);
    win95MI = new JMenuItem("Win95");
    win95MI.addActionListener(this);
    LandFMenu.add(roseMI);
    LandFMenu.add(win95MI);

    // Personae menu
    boolean personasExist = false;
    try
      {
	Vector personae = session.getPersonae();
	if (personae != null)
	  {
	    PersonaMenu = new JMenu("Persona");
	    personaListener = new PersonaListener(session, this);
	    ButtonGroup personaGroup = new ButtonGroup();
	    
	    for (int i = 0; i < personae.size(); i++)
	      {
		JRadioButtonMenuItem mi = new JRadioButtonMenuItem((String)personae.elementAt(i));
		personaGroup.add(mi);
		mi.addActionListener(personaListener);
		PersonaMenu.add(mi);
	      }
	    personasExist = true;
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not load personas: " + rx);
      }

    menubar.add(fileMenu);
    menubar.add(LandFMenu);
    menubar.add(actionMenu);
    menubar.add(windowMenu);
    if (personasExist)
      {
	menubar.add(PersonaMenu);
      }
    
    setJMenuBar(menubar);

    // Create menus for the tree

    createMI = new MenuItem("Create");
    viewMI = new MenuItem("List editable");
    viewAllMI = new MenuItem("List all");
    queryMI = new MenuItem("Query");

    pMenu.add(viewMI);
    pMenu.add(viewAllMI);
    pMenu.add(createMI);
    pMenu.add(queryMI);

    if (debug)
      {
	System.out.println("Loading images for tree");
      }

    Image openFolder = PackageResources.getImageResource(this, "openfolder.gif", getClass());
    Image closedFolder = PackageResources.getImageResource(this, "folder.gif", getClass());
    Image list = PackageResources.getImageResource(this, "list.gif", getClass());
    Image redOpenFolder = PackageResources.getImageResource(this, "openfolder-red.gif", getClass());
    Image redClosedFolder = PackageResources.getImageResource(this, "folder-red.gif", getClass());
    
    Image trash = PackageResources.getImageResource(this, "trash.gif", getClass());
    Image creation = PackageResources.getImageResource(this, "creation.gif", getClass());
    Image pencil = PackageResources.getImageResource(this, "pencil.gif", getClass());

    images = new Image[NUM_IMAGE];
    images[OPEN_BASE] =  openFolder;
    //images[OPEN_BASE_DELETE] = openFolder;
    //images[OPEN_BASE_CREATE] = openFolder;
    //images[OPEN_BASE_CHANGED] = openFolder;
    images[CLOSED_BASE ] = closedFolder;
    //images[CLOSED_BASE_DELETE] = closedFolder;
    //images[CLOSED_BASE_CREATE] = closedFolder;
    //images[CLOSED_BASE_CHANGED] = closedFolder;
    
    images[OPEN_FIELD] = list;
    images[OPEN_FIELD_DELETE] = trash;
    images[OPEN_FIELD_CREATE] = creation;
    images[OPEN_FIELD_CHANGED] = pencil;
    images[CLOSED_FIELD] = list;
    images[CLOSED_FIELD_DELETE] = trash;
    images[CLOSED_FIELD_CREATE] = creation;
    images[CLOSED_FIELD_CHANGED] = pencil;
    
    images[OPEN_CAT] = redOpenFolder;
    images[CLOSED_CAT] = redClosedFolder;


    // What is this for?
    for (int j = 0; j < 3; j++)
      {
	if (images[j] == null)
	  {
	    System.out.println("Image is null " + j);
	  }
      }

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

    leftTop = new JPanel(false);
    leftTop.setBorder(statusBorderRaised);
    
    leftL = new JLabel("Objects");
    leftTop.setLayout(new BorderLayout());
    leftTop.setBackground(ClientColor.menu);
    leftTop.setForeground(ClientColor.menuText);
    leftTop.add("Center", leftL);

    leftP.add("North", leftTop);

    if (debug)
      {
	System.out.println("Creating pop up menus");
      }

    objectPM = new treeMenu();
    objViewMI = new MenuItem("View Object");
    objEditMI = new MenuItem("Edit Object");
    objCloneMI = new MenuItem("Clone Object");
    objInactivateMI = new MenuItem("Inactivate Object");
    objDeleteMI = new MenuItem("Delete Object");

    objectPM.add(objViewMI);
    objectPM.add(objEditMI);
    objectPM.add(objCloneMI);
    objectPM.add(objInactivateMI);
    objectPM.add(objDeleteMI);

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

    rightL = new JLabel("Open objects");
    
    rightTop = new JPanel(false);
    rightTop.setBackground(ClientColor.menu);
    rightTop.setForeground(ClientColor.menuText);
    rightTop.setBorder(statusBorderRaised);
    rightTop.setLayout(new BorderLayout());
    rightTop.add("West", rightL);
    timerLabel = new JLabel("                                       ", JLabel.RIGHT);

    timer = new connectedTimer(timerLabel, 5000);
    timerLabel.setMinimumSize(new Dimension(200,timerLabel.getPreferredSize().height));
    rightTop.add("East", timerLabel);
    
    rightP.add("North", rightTop);

    // Button bar at bottom, includes commit/cancel panel and taskbar
    JPanel bottomButtonP = new JPanel(false);
    //JPanel leftButtonP = new JPanel(false);
    //bottomButtonP.setLayout(new BorderLayout());
    //bottomButtonP.add("West", leftButtonP);
    rightP.add(bottomButtonP,"South");

    // Taskbar to track windows
    //WindowBar windowBar = new WindowBar(wp, rightP);
    //wp.addWindowBar(windowBar);

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

    bottomButtonP.add(commit);
    bottomButtonP.add(cancel);

    sPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftP, rightP);
   
    mainPanel.add("Center",sPane);

    // Create the bottomBar, for the bottom of the window

    JPanel bottomBar = new JPanel(false);
    bottomBar.setBackground(ClientColor.menu);
    bottomBar.setForeground(ClientColor.menuText);
    bottomBar.setLayout(new BorderLayout());

    statusLabel = new JTextField();
    statusLabel.setOpaque(true);
    statusLabel.setEditable(false);
    statusLabel.setBackground(ClientColor.menu);
    statusLabel.setForeground(ClientColor.menuText);
    statusLabel.setBorder(statusBorder);

    JLabel l = new JLabel("Status: ");
    l.setBackground(ClientColor.menu);
    l.setForeground(ClientColor.menuText);
    JPanel lP = new JPanel(new BorderLayout());
    lP.setBorder(statusBorder);
    lP.add("Center", l);

    bottomBar.add("West", lP);
    bottomBar.add("Center", statusLabel);
    mainPanel.add("South", bottomBar);

    statusLabel.setText("Starting up");

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
    loader.start()
;
    pack();
    setSize(800, 600);
    show();
  }

  /**
   * Returns the templateHash.
   *
   * Template Hash is a hash of object type ID's (Short) -> Vector of FieldTemplates
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

    if (templateHash.containsKey(id))
      {
	if (debug)
	  {
	    System.out.println("Found the template, using cache.");
	  }
	result = (Vector)templateHash.get(id);

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
	    templateHash.put(id, result);


	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get field templates: " + rx);
	  }

      }
    
    return result;

  }

  public void clearCaches()
  {
    cachedLists.clear();
  }

  /*  public Vector getHandleVector(Object id)
  {
    Vector result = null;

    if (cachedLists.containsKey(id))
      {
	result = (Vector)cachedLists.get(id);
	setStatus("Using cached copy");
      }
    else
      {
	try
	{
	setStatus("Downloading new copy");
	    
	    }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not load template vector: " + rx);
	  }
      }
    return result;
    
  }*/

  /*
  public void invalidate()
  {
    System.out.println("--Invalidate gclient");
    super.invalidate();
  }

  public void validate()
  {
    System.out.println("--validate gclient");
    super.validate();
  }

  public void doLayout()
  {
    System.out.println("{{doLayout gclient: ");
    super.doLayout();
    System.out.println("}}doLayout gclient over");
  }

  */

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
   * Checks to see if the baseHash was loaded, and if not, it loads it.
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

  public final Vector getBaseList()
  {
    if (baseList == null)
      {
	baseList = loader.getBaseList();
      }

    return baseList;
  }


  public Hashtable getBaseMap()
  {
    if (baseMap == null)
      {
	baseMap = loader.getBaseMap();
      }

    return baseMap;

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
  
  public void setRose()
  {
    try 
      {
	UIManager.setLookAndFeel("com.sun.java.swing.rose.RoseLookAndFeel");
	SwingUtilities.updateComponentTreeUI(this);
	leftP.invalidate();
	invalidate();
	validate();
      } 
    catch (Exception e) 
      {
	System.out.println(e);
      }
  }
  
  public void setBasic() 
  {
    try
      {
	UIManager.setLookAndFeel("com.sun.java.swing.basic.BasicLookAndFeel");
	SwingUtilities.updateComponentTreeUI(this);
	leftP.invalidate();
	invalidate();
	validate();
      } 
    catch (Exception e) 
      {
	System.out.println(e);
      }
  }

  // Private methods


  /**
   * This clears out the tree and completely rebuilds it.
   */

  void rebuildTree()
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

    Category firstCat = session.getRootCategory();

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
			       pMenu);
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
   * @param committed True if commit was clicked, false if cancel was clicked.
   */

  void refreshTree(boolean committed) throws RemoteException
  {
    // First get rid of deleted nodes

    Enumeration deleted = deleteHash.keys();

    while (deleted.hasMoreElements())
      {
	InvidNode node = ((CacheInfo)deleteHash.get(deleted.nextElement())).getNode();
	if (committed)
	  {
	    System.out.println("Deleteing node: " + node.getText());
	    tree.deleteNode(node, false);
	  }
	else
	  {
	    System.out.println("Canceling the delete");
	    // Change icon back
	    node.setImages(OPEN_FIELD, CLOSED_FIELD);
	  }
      }
    
    deleteHash.clear();

    //
    // Now change the created nodes
    //

    Enumeration created = createHash.keys();
    while (created.hasMoreElements())
      {
	Invid invid = (Invid)created.nextElement();
	InvidNode node = ((CacheInfo)createHash.get(invid)).getNode();
	if (committed)
	  {
	    System.out.println("Committing created node: " + node.getText());
	    // change the icon
	    node.setImages(OPEN_FIELD, CLOSED_FIELD);
	    node.setText(session.viewObjectLabel(invid));
	  }
	else
	  {
	    System.out.println("Canceling created node: " + node.getText());
	    tree.deleteNode(node, false);

	  }
      }

    createHash.clear();

    //
    // Last change the changed nodes.
    //

    Enumeration changed = changedHash.keys();
    while (changed.hasMoreElements())
      {
	Invid invid = (Invid)changed.nextElement();
	CacheInfo info = (CacheInfo)changedHash.get(invid);
	InvidNode node = info.getNode();
	if (committed)
	  {
	    if (debug)
	      {
		// This shouldn't be the original label.
		System.out.println("Updating node: " + node.getText() + " to " + info.getOriginalLabel());
	      }
	    node.setText(info.getOriginalLabel());
	  }
	else
	  {
	    System.out.println("Cancelled, no change to object?");
	    // Don't know what to do here, maybe change back?  then
	    // don't need the change up there either.
	  }
	node.setImages(OPEN_FIELD, CLOSED_FIELD);

      }

    changedHash.clear();

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
    Vector unsorted_objects = new Vector();
    short id;

    /* -- */

    base = node.getBase();    

    try
      {
	id = base.getTypeID();
	//Now get all the children
	_query = new Query(id);
	node.setQuery(_query);
      }
    catch (RemoteException rx)
      {
	throw new IllegalArgumentException("It's the Query! " + rx);
      }

    if (_query == null)
      {
	System.out.println("query == null");
      }
    else
      {
	Short Id = new Short(id);
	//Vector resultDump = getHandleVector(Id);

	Vector resultDump = null;
	if (cachedLists.containsKey(Id))
	  {
	    resultDump = (Vector)cachedLists.get(Id);
	  }
	else
	  {
	    try
	      {
		Query q = new Query(id);
		QueryResult qr = session.query(q);
		resultDump = qr.getListHandles();
		if (resultDump != null)
		  {
		    System.out.println("Caching copy");
		    cachedLists.put(Id, resultDump);
		  }

	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not get dump: " + rx);
	      }

	  }

	int resultDumpSize = resultDump.size();
	for (int i = 0; i < resultDumpSize; i++)
	  {
	    listHandle lh = (listHandle)resultDump.elementAt(i);
	    String l = lh.getLabel();
	    //System.out.println(" Checking: " + l);
	    if ((l == null) || (l.equals("")))
	      {
		l = "null str";
		System.out.println("Setting string to null str");
	      }
	    unsorted_objects.addElement(new Result((Invid)lh.getObject(), l));
	  }

	//System.out.println("There are " + unsorted_objects.size() + " objects in the query");

	if (unsorted_objects.size()  == 0)
	  {
	    System.out.println("unsorted_objects or sorted_results == null");
	  }
	else
	  {
	    (new VecQuickSort(unsorted_objects, 
			      new arlut.csd.Util.Compare() 
			      {
				public int compare(Object a, Object b) 
				  {
				    Result aF, bF;
				     
				    aF = (Result) a;
				    bF = (Result) b;
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
			      }
			      )).sort();
	  }
      }

    parentNode = node;
    oldNode = null;
    fNode = (InvidNode) node.getChild();
    int i = 0;
	
    while ((i < unsorted_objects.size()) || (fNode != null))
      {
	//System.out.println("Looking at the next node");
	//System.out.println("i = " + i + " length = " + unsorted_objects.length);
	
	if (i < unsorted_objects.size())
	  {
	    invid = ((Result) unsorted_objects.elementAt(i)).getInvid();
	    label = unsorted_objects.elementAt(i).toString();
	  }
	else
	  {
	    //System.out.println("Object is null");
	    invid = null;
	  }

	if ((fNode == null) ||
	    ((invid != null) && 
	     ((label.compareTo(fNode.getText())) < 0)))
	  {
	    // insert a new object node
	    //System.out.println("Adding this node");
	    //newNode = new InvidNode(parentNode, object.getName(), object,
	    //			    oldNode, false, 2, 2, objectMenu);

	    InvidNode objNode = new InvidNode(node, 
					      label,
					      invid,
					      oldNode, false,
					      OPEN_FIELD,
					      CLOSED_FIELD,
					      objectPM);
	    
	    tree.insertNode(objNode, false);

	    oldNode = objNode;
	    fNode = (InvidNode) oldNode.getNextSibling();
	    
	    i++;
	  }
	else if ((invid == null) ||
		 ((label.compareTo(fNode.getText())) > 0))
	  {
	    // delete a object node
	    //System.out.println("Removing this node");
	    // System.err.println("Deleting: " + fNode.getText());

	    newNode = (InvidNode) fNode.getNextSibling();
	    tree.deleteNode(fNode, false);

	    fNode = newNode;
	  }
	else
	  {
	    //System.out.println("No change for this node");

	    fNode.setText(label);

	    // System.err.println("Setting: " + object.getName());

	    oldNode = fNode;
	    fNode = (InvidNode) oldNode.getNextSibling();

	    i++;
	  }
      }

    if (doRefresh)
      {
	tree.refresh();
      }
  }

  

  // Actions on objects

  public void editObject(Invid invid, InvidNode node)
  {
    try
      {
	db_object o = session.edit_db_object(invid);
	wp.addWindow(o, true);
	changedHash.put(invid, new CacheInfo(new Short(o.getTypeID()), session.viewObjectLabel(invid), null, node));
	if (node != null)
	  {
	    node.setImages(OPEN_FIELD_CHANGED, CLOSED_FIELD_CHANGED);
	    tree.refresh();
	  }
      }
    catch(RemoteException rx)
      {
	throw new RuntimeException("Could not edit object: " + rx);
      }
  }

  public void viewObject(Invid invid)
  {
    try
      {
	wp.addWindow(session.view_db_object(invid), false);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not edit object: " + rx);
      }
    

  }

  public boolean deleteObject(Invid invid, InvidNode node)
  {
    boolean ok = false;
    try
      {
	Short id = new Short(invid.getType());
	if (debug)
	  {
	    System.out.println("Deleting invid= " + invid);
	  }

	// Delete the object
	ok = session.remove_db_object(invid);
	if (ok)
	  {
	    // Check out the deleteHash.  If this one is already on there,
	    // then I don't know what to do.  If it isn't, then add a new
	    // cache info.  I guess maybe update the name or something,
	    // if it is on there.
	    if (deleteHash.containsKey(invid))
	      {
		System.out.println("already deleted, nothing to change, right?");
	      }
	    else
	      {
		deleteHash.put(invid, new CacheInfo(id, session.viewObjectLabel(invid), null,node));
	      }

	    // Take this object out of the cachedLists, if it is in there
	    if (cachedLists.containsKey(id))
	      {
		String label = session.viewObjectLabel(invid);
		System.out.println("This base has been hashed.  Removing: " + label);
		Vector lhs = (Vector)cachedLists.get(id);
		for (int i = 0; i < lhs.size(); i++)
		  {
		    //System.out.println("Checking " + ((listHandle)lhs.elementAt(i)).getLabel());
		    if (((Invid)((listHandle)lhs.elementAt(i)).getObject()) == invid)
		      {
			System.out.println("Found it!");
			lhs.removeElementAt(i);
			break;
		      }
		  }
	      }

	    if (node != null)
	      {
		node.setImages(OPEN_FIELD_DELETE, CLOSED_FIELD_DELETE);
		tree.refresh();
	      }

	    setStatus("Object will be deleted when commit is clicked.");
	  }
	else
	  {
	    String error = session.getLastError();
	    System.out.println("Could not delete object: " + error);
	    setStatus("Delete Failed: " + error);
	  }
      }
    catch(RemoteException rx)
      {
	throw new RuntimeException("Could not delete base: " + rx);
      }

    return ok;
  }

  /**
   * Show a panel which takes a string, and a combo box of types.
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
	try
	  {
	    wp.addWindow(session.edit_db_object(invid), true);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not edit object: " + rx);
	  }
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

  void inactivateObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }

    openDialog.setText("Choose object to be inactivated");

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	System.out.println("Canceled");
      }
    else
      {
	try
	  {
	    StringDialog d = new StringDialog(this, "Verify invalidation", "Are you sure you want to inactivate " + session.viewObjectLabel(invid), "Yes", "No");
	    Hashtable result = d.DialogShow();
	    if (result == null)
	      {
		setStatus("Cancelled!");
	      }
	    else
	      {
		setStatus("inactivating " + invid);
		boolean ok = session.inactivate_db_object(invid);
		if (ok)
		  {
		    setStatus("Object inactivated.");
		  }
		else
		  {
		    setStatus("Could not inactivate object.");
		    arlut.csd.JDialog.JErrorDialog ed = new arlut.csd.JDialog.JErrorDialog(this, "Could not inactivate object: " + session.getLastError());
		  }
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not verify invid to be inactivated: " + rx);
	  }
      }
  }

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
	    StringDialog d = new StringDialog(this, "Verify deletion", "Are you sure you want to delete " + session.viewObjectLabel(invid), "Yes", "No");
	    Hashtable result = d.DialogShow();
	    if (result == null)
	      {
		setStatus("Cancelled!");
	      }
	    else
	      {
		deleteObject(invid, null);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not verify invid to be inactivated: " + rx);
	  }
      }
  }


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


  public void chooseFilter()
  {
    // This could be moved, only cache is filter is changed?
    System.out.println("Clearing caches");
    cachedLists.clear();
    if (filterDialog == null)
      {
	filterDialog = new JFilterDialog(this);
      }
    else
      {
	filterDialog.setVisible(true);
      }
    rebuildTree();
    
  }

  public void chooseDefaultOwner(boolean forcePopup)
  {
    // What to do here?  don't check for null, because maybe forcePopup was false.
    // Have to think about this one, maybe keep groups Vector in gclient (ie not local here)

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

  public boolean defaultOwnerChosen()
  {
    return defaultOwnerChosen;
  }

  /*
   * This checks to see if anything has been changed.  Basically, if edit panels are
   * open and have been changed in any way, then somethingChanged will be true and 
   * the user will be warned.  If edit panels are open but have not been changed, then
   * it will return true(it is ok to proceed)
   */

  boolean OKToProceed()
  {
    if (somethingChanged)
      {
	StringDialog dialog = new StringDialog(this, 
					       "Warning: changes have been made",
					       "You have made changes in objects without \ncommiting those changes.  If you continue, \nthose changes will be lost",
					       "Continue",
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

  public void commitTransaction()
  {
    try
      {
	this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	updateNotePanels();
	
	wp.closeEditables();
	somethingChanged = false;
	session.commitTransaction();
	
	wp.refreshTableWindows();
	session.openTransaction("gclient");

	System.out.println("Done committing");
	this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

	refreshTree(true);

	cachedLists.clear();
	
	wp.resetWindowCount();

	
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not commit transaction" + rx);
      }
  }

  public void cancelTransaction()
  {
    try
      {
	wp.closeEditables();
	session.abortTransaction();

	// Now we need to fix up the caches, and clean up all the changes made
	// during the transaction
	Enumeration dels = deleteHash.keys();
	while (dels.hasMoreElements())
	  {
	    Invid invid = (Invid)dels.nextElement();
	    CacheInfo info = (CacheInfo)deleteHash.get(invid);

	    if (cachedLists.containsKey(info.getBaseID()))
		{
		  System.out.println("This one is hashed, sticking it back in.");
		  Vector list = (Vector)cachedLists.get(info.getBaseID());
		  list.addElement(new listHandle(info.getOriginalLabel(), invid));
		}

	  }
	
	// Next up is created list: remove all the added stuff.
	Enumeration created = createHash.keys();
	while (created.hasMoreElements())
	  {
	    Invid invid = (Invid)created.nextElement();
	    CacheInfo info = (CacheInfo)createHash.get(invid);

	    if (cachedLists.containsKey(info.getBaseID()))
	      {
		System.out.println("This one is hashed, taking a created object out.");
		Vector list = (Vector)cachedLists.get(info.getBaseID());
		for (int i = 0; i < list.size(); i++)
		  {
		    if (invid == (Invid)((listHandle)list.elementAt(i)).getObject())
		      {
			System.out.println("Found it! removing it.");
			list.removeElementAt(i);
			break;
		      }
		  }
	      }
	  }



	// Now go through changed list and revert any names that may be needed
	Enumeration changed = changedHash.keys();
	while (changed.hasMoreElements())
	  {
	    Invid invid = (Invid)changed.nextElement();
	    CacheInfo info = (CacheInfo)changedHash.get(invid);

	    if (cachedLists.containsKey(info.getBaseID()))
	      {
		System.out.println("This changed base is cached, fixing it back.");
		Vector list = (Vector)cachedLists.get(info.getBaseID());
		for (int i = 0; i < list.size(); i++)
		  {
		    if (invid == (Invid)((listHandle)list.elementAt(i)).getObject())
		      {
			System.out.println("Found it! removing it.");
			list.removeElementAt(i);
			list.addElement(new listHandle(info.getOriginalLabel(), invid));
			break;
		      }
		  }
	      }
	  }

	somethingChanged = false;
	session.openTransaction("glient");
	// This will fix up the tree (remove the trash cans)
	refreshTree(false);

	// Now we are done
	deleteHash.clear();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not abort transaction" + rx);
      }
  }
  // ActionListener Methods
  
  public void actionPerformed(java.awt.event.ActionEvent event)
  {
    if (event.getSource() == cancel)
      {
	System.err.println("cancel button clicked");

	cancelTransaction();


      }
    else if (event.getSource() == commit)
      {
	System.out.println("commit button clicked");

	commitTransaction();
	
      }
    else if (event.getSource() == menubarQueryMI)
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
    else if (event.getSource() == removeAllMI)
      {
	if (OKToProceed())
	  {
	    wp.closeAll();
	  }
      }
    else if (event.getSource() == rebuildTreeMI)
      {
	rebuildTree();
      }
    else if (event.getActionCommand().equals("open object for editing"))
      {
	editObjectDialog();
      }
    else if (event.getActionCommand().equals("open object for viewing"))
      {
	viewObjectDialog();
      }
    else if (event.getActionCommand().equals("choose an object for cloning"))
      {
	cloneObjectDialog();
      }
    else if (event.getActionCommand().equals("delete an object"))
      {
	deleteObjectDialog();
      }
    else if (event.getActionCommand().equals("inactivate an object"))
      {
	inactivateObjectDialog();
      }
    else if (event.getActionCommand().equals("Filter Query"))
      {
	chooseFilter();
      }
    else if (event.getActionCommand().equals("Set Default Owner"))
      {
	chooseDefaultOwner(true);
      }
    

    else if (event.getSource() == logoutMI)
      {
	if (OKToProceed())
	  {
	    logout();
	  }
      }
    else if (event.getSource() == roseMI)
      {
	setRose();
      }
    else if (event.getSource() == win95MI)
      {
	setBasic();
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

    if (node instanceof BaseNode)
      {
	// make sure we've got the list updated

	treeNodeExpanded(node);
      }
    
    if (event.getSource() == createMI)
      {
	System.out.println("createMI");

	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;

	    try
	      {
		Short id = new Short(baseN.getBase().getTypeID());

		if (!defaultOwnerChosen)
		  {
		    chooseDefaultOwner(false);
		  }
		db_object obj = session.create_db_object(baseN.getBase().getTypeID());
		wp.addWindow(obj, true);

		Invid invid = obj.getInvid();

		InvidNode objNode = new InvidNode(baseN, 
						  "New Object", 
						  invid,
						  null, false,
						  OPEN_FIELD_CREATE,
						  CLOSED_FIELD_CREATE,
						  objectPM);

		// If the base node is closed, open it.

		if (!baseN.isOpen())
		  {
		    tree.expandNode(baseN, false);
		  }

		if (cachedLists.containsKey(id))
		  {
		    Vector list = (Vector)cachedLists.get(id);
		    list.addElement(new listHandle("New Object", invid));

		  }

		// Redraw the tree now
		tree.insertNode(objNode, true);
		createHash.put(obj.getInvid(), new CacheInfo(id, "New Object", null, objNode));
		somethingChanged = true;
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
    else if (event.getSource() ==  viewMI)
      {
	System.out.println("viewMI");

	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;

	    try
	      {
		Query _query = new Query(baseN.getBase().getTypeID(), null, true);
		DumpResult buffer = session.dump(_query);

		if (buffer == null)
		  {
		    System.out.println("results == null");
		  }
		else
		  {
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
    
    else if (event.getSource() ==  viewAllMI)
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
		DumpResult buffer = session.dump(_query);
		
		if (buffer == null)
		  {
		    System.out.println("results == null");
		  }
		else
		  {
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
    else if (event.getSource() ==  queryMI)
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
		    buffer = session.dump(q);
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("caught remote: " + ex);
		  }

		wp.addTableWindow(session, q, buffer, "Query Results");
	      }
	  }
      }
    else if (event.getSource() == objViewMI)
      {
	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;
	  
	    viewObject(invidN.getInvid());
	  }
	else
	  {
	    System.err.println("not a base node, can't create");
	  }
      }
    else if (event.getSource() == objEditMI)
      {
	System.out.println("objEditMI");

	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;
	  
	    Invid invid = invidN.getInvid();
		
	    editObject(invid, invidN);
	  }
	else
	  {
	    System.err.println("not a base node, can't create");
	  }
      }
    else if (event.getSource() == objDeleteMI)
      {
	// Need to change the icon on the tree to an X or something to show that it is deleted
	System.out.println("Deleting object");
	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;
	    Invid invid = invidN.getInvid();

	    deleteObject(invid, invidN);

	  }
	else  // Should never get here, but just in case...
	  {
	    System.out.println("Not a base node, can't delete this.");
	  }
      }
    else if (event.getSource() == objCloneMI)
      {
	System.out.println("objCloneMI");
      }
    else if(event.getSource() == objInactivateMI)
      {
	System.out.println("objInactivateMI");
      }
    else
      {
	System.err.println("Unknown MI chosen");
      }
  }

  public void start() throws Exception {

  }



}

/*---------------------------------------------------------------------
                                                       Class InvidNode
---------------------------------------------------------------------*/

class InvidNode extends arlut.csd.Tree.treeNode {

  final static boolean debug = true;

  private Invid invid;

  public InvidNode(treeNode parent, String text, Invid invid, treeNode insertAfter,
		    boolean expandable, int openImage, int closedImage, treeMenu menu)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
    this.invid = invid;

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
    
}


/*------------------------------------------------------------------------------
                                                                           Class
                                                                        BaseNode

------------------------------------------------------------------------------*/

class BaseNode extends arlut.csd.Tree.treeNode {

  private Base base;
  private Query query;
  private boolean loaded = false;

  /* -- */

  BaseNode(treeNode parent, String text, Base base, treeNode insertAfter,
	   boolean expandable, int openImage, int closedImage, treeMenu menu)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
    this.base = base;

  }

  public Base getBase()
  {
    return base;
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
      if (gc.somethingChanged)
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

      System.out.println("MenuItem action command: " + event.getActionCommand());
      
      Hashtable result = null;
      String password = null;

      if (event.getActionCommand().indexOf(":") > 0)
	{
	  StringDialog d = new StringDialog(resource);
	  result = d.DialogShow();
	  password = (String)result.get("Password:");
	}
      else
	{
	  password = "yada";
	}

      if (password != null)
	{
	  try
	    {	      
	      personaChangeSuccessful = session.selectPersona(event.getActionCommand(), password);
	      
	      if (personaChangeSuccessful)
		{
		  gc.setStatus("Successfully changed persona.");
		  gc.setTitle("Ganymede Client: " + event.getActionCommand() + " logged in.");
		  gc.ownerGroups = null;
		  gc.clearCaches();
		  gc.commitTransaction();
		  gc.rebuildTree();
		}
	      else
		{
		  gc.setStatus("Danger Danger!");
		  (new StringDialog(gc, "Error: persona no changie", 
				    "Could not change persona:\n" + gc.getSession().getLastError(),
				    false)).DialogShow();

		  gc.setStatus("Persona change failed");
		}
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not set persona to " + event.getActionCommand() + ": " + rx);
	    }

	}
      

    }

}

//-------------------------------------------------------------------------
class CacheInfo {

  private String
    originalLabel,
    currentLabel;

  private InvidNode
    node;

  private Short
    baseID;

  public CacheInfo(Short baseID, String originalLabel, String currentLabel, InvidNode node)
  {
    this.baseID = baseID;
    this.originalLabel = originalLabel;
    this.currentLabel = currentLabel;
    this.node = node;
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

  public InvidNode getNode()
  {
    return node;
  }
}

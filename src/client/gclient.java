/*
   gclient.java

   Ganymede client main module

   Created: 24 Feb 1997
   Version: $Revision: 1.37 $ %D%
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

import gjt.ImageCanvas;
import gjt.Util;
import gjt.Box;
import gjt.RowLayout;
import jdj.*;

import arlut.csd.JDialog.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.DataComponent.*;
import arlut.csd.ganymede.*;
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

  // set up a bunch of borders
  public EmptyBorder
    emptyBorder5 = new EmptyBorder(new Insets(5,5,5,5)),
    emptyBorder10 = new EmptyBorder(new Insets(10,10,10,10));  

  public BevelBorder
    raisedBorder = new BevelBorder(BevelBorder.RAISED),
    loweredBorder = new BevelBorder(BevelBorder.LOWERED);
      

  public CompoundBorder
    statusBorder = new CompoundBorder(loweredBorder, emptyBorder5);


  private Hashtable
    baseHash = null,	             // used to reduce the time required to get listings
                                     // of bases and fields.. keys are Bases, values
		      	             // are vectors of fields
    baseMap = null,                  // Hash of Short to Base
    changedHash = new Hashtable(),   // Hash of objects that might have changed
    deleteHash = new Hashtable(),    // Hash of objects waiting to be deleted
    createHash = new Hashtable();    // Hash of objects waiting to be created
                                     // Create and Delete are pending on the Commit button. 
   
  Loader 
    loader;      // Use this to do start up stuff in a thread
  
  boolean
    somethingChanged = false;

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
    menubarQueryMI = null;

  JMenuItem
    roseMI,
    win95MI;

  JMenu 
    windowMenu,
    fileMenu,
    LandFMenu,
    PersonaMenu;

  PersonaListener
    personaListener;

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

    System.out.println("Starting gclient");

    if (s == null)
      {
	throw new IllegalArgumentException("Ganymede Error: Parameter for Session s is null");;
      }

    session = s;
    _myglogin = g;

    mainPanel = new JPanel(true);
    mainPanel.setLayout(new BorderLayout());

    setLayout(new BorderLayout());
    add("Center", mainPanel);

    //BorderLayout layout = new BorderLayout();
    //setLayout( layout );

    if (debug)
      {
	System.out.println("Creating menu bar");
      }

    // Make the menu bar
    menubar = new JMenuBar();    

    // File menu
    fileMenu = new JMenu("File");
    //fileMenu.setBackground(ClientColor.menu);
    //fileMenu.setForeground(ClientColor.menuText);
    logoutMI = new JMenuItem("Logout");
    logoutMI.addActionListener(this);
    removeAllMI = new JMenuItem("Remove All Windows");
    removeAllMI.addActionListener(this);
    rebuildTreeMI = new JMenuItem("Rebuild Tree");
    rebuildTreeMI.addActionListener(this);

    menubarQueryMI = new JMenuItem("Query");
    menubarQueryMI.addActionListener(this);

    fileMenu.add(menubarQueryMI);
    fileMenu.addSeparator();
    fileMenu.add(rebuildTreeMI);
    fileMenu.add(removeAllMI);
    fileMenu.addSeparator();
    fileMenu.add(logoutMI);

    // Window list menu
    windowMenu = new JMenu("Windows");
    //windowMenu.setBackground(ClientColor.menu);
    //windowMenu.setForeground(ClientColor.menuText);

    // Look and Feel menu
    LandFMenu = new JMenu("Look");
    //LandFMenu.setBackground(ClientColor.menu);
    //LandFMenu.setForeground(ClientColor.menuText);
    roseMI = new JMenuItem("Rose");
    roseMI.addActionListener(this);
    win95MI = new JMenuItem("Win95");
    win95MI.addActionListener(this);
    LandFMenu.add(roseMI);
    LandFMenu.add(win95MI);

    // Personae menu
    PersonaMenu = new JMenu("Persona");
    personaListener = new PersonaListener(session, this);
    try
      {
	Vector personae = session.getPersonae();
	if (personae != null)
	  {
	    for (int i = 0; i < personae.size(); i++)
	      {
		JMenuItem mi = new JMenuItem((String)personae.elementAt(i));
		mi.addActionListener(personaListener);
		PersonaMenu.add(mi);
	      }
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not load personas: " + rx);
      }

    menubar.add(fileMenu);
    menubar.add(LandFMenu);
    menubar.add(windowMenu);
    menubar.add(PersonaMenu);
    menubar.setBorder(emptyBorder5);
    menubar.setBackground(ClientColor.menu);
    menubar.setForeground(ClientColor.menuText);

    JPanel mp = new JPanel(new BorderLayout());
    mp.add("Center", menubar);
    mp.setBorder(raisedBorder);
    mp.setOpaque(true);
    mp.setBackground(ClientColor.menu);
    //this.setJMenuBar(menubar);
    add("North", mp);

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
    leftTop.setBorder(new EmptyBorder(new Insets(4,4,4,4)));
    leftTop.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.RAISED), new EmptyBorder(new Insets(4,4,4,4))));
    
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
    rightTop.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.RAISED), new EmptyBorder(new Insets(4,4,4,4))));
    rightTop.setLayout(new BorderLayout());
    rightTop.add("West", rightL);

    timerLabel = new JLabel();
    timerLabel.setMinimumSize(new Dimension(100,5));
    timer = new connectedTimer(timerLabel, 5000);
    rightTop.add("East", timerLabel);

    rightP.add("North", rightTop);

    // Button bar at bottom, includes commit/cancel panel and taskbar
    JPanel bottomButtonP = new JPanel(false);
    JPanel leftButtonP = new JPanel(false);
    JPanel rightButtonP = new JPanel(false);
    bottomButtonP.setLayout(new BorderLayout());
    bottomButtonP.add("West", leftButtonP);
    bottomButtonP.add("Center", rightButtonP);
    rightP.add(bottomButtonP,"South");
    leftButtonP.setLayout(new RowLayout());
    rightButtonP.setLayout(new RowLayout());

    // Taskbar to track windows
    //WindowBar windowBar = new WindowBar(wp, rightP);
    //wp.addWindowBar(windowBar);

    commit = new JButton("Commit");
    commit.setOpaque(true);
    commit.setBackground(Color.lightGray);
    commit.setForeground(Color.black);
    commit.setToolTipText("Click this to commit all changes to database");
    cancel = new JButton("Cancel");
    cancel.setOpaque(true);
    cancel.setBackground(Color.lightGray);
    cancel.setForeground(Color.black);
    cancel.setToolTipText("Click this to cancel all changes");
    commit.addActionListener(this);
    cancel.addActionListener(this);

    leftButtonP.add(commit);
    leftButtonP.add(cancel);
    //rightButtonP.add(windowBar);

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

  public Hashtable getBaseHash()
  {
    if (baseHash == null)
      {
	baseHash = loader.getBaseHash();
      }

    return baseHash;
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

  public void setStatus(String status)
  {
    if (debug)
      {
	System.out.println("Setting status: " + status);
      }
    statusLabel.setText(status);
    //repaint();
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
	recurseDownCatagories(firstNode);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Cound't recurse down catagories: " + rx);
      }

    tree.refresh();

    if (debug)
      {
	System.out.println("Done building tree,");
      }
  }

  void recurseDownCatagories(CatTreeNode node) throws RemoteException
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
	    recurseDownCatagories((CatTreeNode)prevNode);
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
	InvidNode node = (InvidNode)deleteHash.get(deleted.nextElement());
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
	InvidNode node = (InvidNode)createHash.get(invid);
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
	InvidNode node = (InvidNode)changedHash.get(invid);
	if (committed)
	  {
	    System.out.println("Updating node: " + node.getText() + " to " + session.viewObjectLabel(invid));
	    node.setText(session.viewObjectLabel(invid));
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

    /* -- */

    base = node.getBase();    

    try
      {
	//Now get all the children
	_query = new Query(base.getTypeID());
	node.setQuery(_query);
      }
    catch (RemoteException rx)
      {
	throw new IllegalArgumentException("It's the Query! " + rx);
      }

    try
      {
	if (_query == null)
	  {
	    System.out.println("query == null");
	  }
	else
	  {
	    QueryResult resultDump =  session.query(_query);

	    for (int i = 0; i < resultDump.size(); i++)
	      {
		unsorted_objects.addElement(new Result(resultDump.getInvid(i), resultDump.getLabel(i)));
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
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get object labels: " + rx);
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
	    //System.out.println("Dealing with " + object.getLabel());
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
	somethingChanged = false;
	session.openTransaction("glient");
	refreshTree(false);
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
    else if (event.getSource() == logoutMI)
      {
	if (OKToProceed())
	  {
	    logout();
	    /*
	      try
	      {
	      session.logout();
	      this.dispose();
	      _myglogin.connector.setEnabled(true);
	      _myglogin._quitButton.setEnabled(true);
	      }
	      catch (RemoteException rx)
	      {
	      throw new IllegalArgumentException("could not logout: " + rx);
	      }*/
	  }
      }
    else if (event.getSource() == roseMI)
      {
	setRose();
	/*	try
	  {
	    setStatus("Switching to Rose look and feel");
	    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    UIManager.setLookAndFeel("com.sun.java.swing.rose.RoseFactory");
	    this.invalidate();
	    this.validate();
	    this.repaint();
	    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    setStatus("Done.");
	  }
	catch (ClassNotFoundException ex)
	  {
	    System.out.println("Could not load Rose: " + ex);
	  }
	catch (IllegalAccessException ex)
	  {
	    System.out.println("Could not load new look: " + ex);
	  }
	catch (InstantiationException ex)
	  {
	    System.out.println("Could not load new look: " + ex);
	  }
	  */	
      }
    else if (event.getSource() == win95MI)
      {
	setBasic();

	/*
	try
	  {
	    setStatus("Switching to win95 look and feel");
	    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    UIManager.setLookAndFeel("com.sun.java.swing.basic.BasicFactory");
	    this.invalidate();
	    this.validate();
	    this.repaint();
	    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    setStatus("Done");
	  }
	catch (ClassNotFoundException ex)
	  {
	    System.out.println("Could not load Rose: " + ex);
	  }
	catch (IllegalAccessException ex)
	  {
	    System.out.println("Could not load new look: " + ex);
	  }
	catch (InstantiationException ex)
	  {
	    System.out.println("Could not load new look: " + ex);
	  }
	  */
      }

    else
      {
	System.err.println("Unknown action event generated");
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
		db_object obj = session.create_db_object(baseN.getBase().getTypeID());
		wp.addWindow(obj, true);

		InvidNode objNode = new InvidNode(baseN, 
						  "New Object", 
						  obj.getInvid(),
						  null, false,
						  OPEN_FIELD_CREATE,
						  CLOSED_FIELD_CREATE,
						  objectPM);

		// If the base node is closed, open it.

		if (!baseN.isOpen())
		  {
		    tree.expandNode(baseN, false);
		  }

		// Redraw the tree now
		tree.insertNode(objNode, true);
		createHash.put(obj.getInvid(), objNode);
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
	  
	    try
	      {
		wp.addWindow(session.view_db_object(invidN.getInvid()), false);
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
    else if (event.getSource() == objEditMI)
      {
	System.out.println("objEditMI");

	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;
	  
	    try
	      {	
		wp.addWindow(session.edit_db_object(invidN.getInvid()), true);
		changedHash.put(invidN.getInvid(), invidN);
		invidN.setImages(OPEN_FIELD_CHANGED, CLOSED_FIELD_CHANGED);
		tree.refresh();
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
    else if (event.getSource() == objDeleteMI)
      {
	// Need to change the icon on the tree to an X or something to show that it is deleted
	System.out.println("Deleting object");
	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;

	    try
	      {
		if (debug)
		  {
		    System.out.println("Deleting invid= " + invidN.getInvid());
		  }
		if (session.remove_db_object(invidN.getInvid()))
		  {
		    deleteHash.put(invidN.getInvid(), invidN);
		    invidN.setImages(OPEN_FIELD_DELETE, CLOSED_FIELD_DELETE);
		    tree.refresh();
		    setStatus("Object will be deleted when commit is clicked.");
		  }
		else
		  {
		    setStatus(session.getLastError());
		  }
	      }
	    catch(RemoteException rx)
	      {
		throw new RuntimeException("Could not delete base: " + rx);
	      }
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
    parent;

  PersonaListener(Session session, gclient parent)
    {
      this.session = session;
      this.parent = parent;
    }

  public void actionPerformed(ActionEvent event)
    {

      if (parent.somethingChanged)
	{
	  // need to ask: commit, cancel, abort?
	  StringDialog d = new StringDialog(parent,
					    "Changing personas",
					    "Before changing personas, the transaction must be closed.  Would you like to commit your changes?",
					    "Commit",
					    "Cancel");
	  Hashtable result = d.DialogShow();
	  if (result == null)
	    {
	      parent.setStatus("Persona change cancelled");
	      return;
	    }
	  else
	    {
	      parent.setStatus("Committing transaction.");
	      parent.commitTransaction();
	    }
	}


      boolean personaChangeSuccessful = false;
      if (resource == null)
	{
	  resource = new DialogRsrc(parent, "Change Persona", "Enter the persona password:");
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
		  parent.setStatus("Successfully changed persona.");
		  parent.setTitle("Ganymede Client: " + event.getActionCommand() + " logged in.");
		  parent.commitTransaction();
		}
	      else
		{
		  parent.setStatus("Danger Danger!");
		  (new StringDialog(parent, "Error: persona no changie", 
				    "Could not change persona:\n" + parent.getSession().getLastError(),
				    false)).DialogShow();

		  parent.setStatus("Persona change failed");
		}
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not set persona to " + event.getActionCommand() + ": " + rx);
	    }

	}
      

    }

}

/*
   gclient.java

   Ganymede client main module

   --

   Created: 24 Feb 1997
   Version: $Revision: 1.15 $ %D%
   Module By: Mike Mulvaney, Jonathan Abbey, and Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

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

/*------------------------------------------------------------------------------
                                                                           class
                                                                         gclient

------------------------------------------------------------------------------*/

public class gclient extends JFrame implements treeCallback,ActionListener {
  
  final boolean debug = true;

  Session session;
  glogin _myglogin;

  Hashtable baseHash = null;	// used to reduce the time required to get listings
				// of bases and fields.. keys are Bases, values
				// are vectors of fields
  boolean
    somethingChanged = false;

  Image images[];

  JButton 
    commit,
    cancel;
  
  TextField
    statusLabel;

  treeControl tree;

  windowPanel
    wp;

  PopupMenu objectPM;
  MenuItem
    objViewMI,
    objEditMI,
    objCloneMI,
    objInactivateMI,
    objDeleteMI;

  PopupMenu 
    pMenu = new PopupMenu();
  
  MenuItem 
    createMI = null,
    viewMI = null,
    queryMI = null,
    menubarQueryMI = null;

  MenuBar 
    menubar;

  MenuItem 
    logoutMI,
    removeAllMI;

  MenuItem
    roseMI,
    win95MI;

  Menu 
    windowMenu,
    fileMenu,
    LandFMenu;

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

    setLayout(new BorderLayout());

    BorderLayout layout = new BorderLayout();
    setLayout( layout );

    // Make the menu bar
    menubar = new MenuBar();

    // File menu
    fileMenu = new Menu("File");
    logoutMI = new MenuItem("Logout");
    logoutMI.addActionListener(this);
    removeAllMI = new MenuItem("Remove All Windows");
    removeAllMI.addActionListener(this);

    menubarQueryMI = new MenuItem("Query");
    menubarQueryMI.addActionListener(this);

    fileMenu.add(menubarQueryMI);
    fileMenu.addSeparator();
    fileMenu.add(removeAllMI);
    fileMenu.addSeparator();
    fileMenu.add(logoutMI);

    // Window list menu
    windowMenu = new Menu("Windows");

    // Look and Feel menu
    LandFMenu = new Menu("Look");
    roseMI = new MenuItem("Rose");
    roseMI.addActionListener(this);
    win95MI = new MenuItem("Win95");
    win95MI.addActionListener(this);
    LandFMenu.add(roseMI);
    LandFMenu.add(win95MI);

    menubar.add(fileMenu);
    menubar.add(LandFMenu);
    menubar.add(windowMenu);
    this.setMenuBar(menubar);

    createMI = new MenuItem("Create");
    viewMI = new MenuItem("List");
    queryMI = new MenuItem("Query");

    pMenu.add(viewMI);
    pMenu.add(createMI);
    pMenu.add(queryMI);

    images = new Image[3];
    images[0] = PackageResources.getImageResource(this, "openfolder.gif", getClass());
    images[1] = PackageResources.getImageResource(this, "folder.gif", getClass());
    images[2] = PackageResources.getImageResource(this, "list.gif", getClass());

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

    Box leftBox = new Box(tree, "Objects");

    InsetPanel leftP = new InsetPanel();
    leftP.setLayout(new BorderLayout());
    leftP.add("Center", leftBox);
    
    add("West", leftP);

    objectPM = new PopupMenu();
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
	loadBaseHash();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception in loadBaseHash: " + ex);
      }

    try
      {
	buildTree();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception in buildTree: " + ex);
      }

    // The right panel which will contain the windowPanel

    JPanel rightP = new JPanel();

    rightP.setLayout(new BorderLayout());

    wp = new windowPanel(this, windowMenu);

    rightP.add("Center", wp);

    // Button bar at bottom, includes commit/cancel panel and taskbar
    JPanel bottomButtonP = new JPanel();
    JPanel leftButtonP = new JPanel();
    JPanel rightButtonP = new JPanel();
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
    commit.setToolTipText("Click this to commit all changes to database");
    cancel = new JButton("Cancel");
    cancel.setToolTipText("Click this to cancel all changes");
    commit.addActionListener(this);
    cancel.addActionListener(this);

    leftButtonP.add(commit);
    leftButtonP.add(cancel);
    //rightButtonP.add(windowBar);
   
    add("Center",rightP);

    JPanel statusBar = new JPanel();
    statusBar.setLayout(new BorderLayout());
    statusLabel = new TextField();
    statusLabel.setEditable(false);
    statusLabel.setBackground(Color.white);
    statusBar.add("West", new JLabel("Status:"));
    statusBar.add("Center", statusLabel);
    add("South", statusBar);

    statusLabel.setText("Starting up");

    try
      {
	session.openTransaction();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not open transaction: " + rx);
      }
    pack();
    setSize(800, 600);
    show();
  }

  /** 
   * Get the session
   */
  public Session getSession()
    {
      return session;
    }

  /**
   * Change the text in the status bar
   *
   * @param status The text to display
   */

  public void setStatus(String status)
  {
    statusLabel.setText(status);
  }

  /**
   * Get the status line for the window
   */

  public String getStatus()
  {
    return statusLabel.getText();
  }

  /**
   * Set the look and feel for the window
   *
   * @param look path to UIFactory
   */

  public void setLook(String look)
  {
    try
      {
	UIManager.setUIFactory(look, this);
      }
    catch (ClassNotFoundException ex)
      {
	System.out.println("Could not load Rose: " + ex);
      }
  }

  // Private methods

  /**
   *
   * loadBaseHash is used to prepare a hash table mapping Bases to
   * Vector's of BaseField.. this is used to allow different pieces
   * of client-side code to get access to the Base/BaseField information,
   * which changes infrequently (not at all?) while the client is
   * connected.. the perm_editor panel created by the windowPanel class
   * benefits from this, as does buildTree() below. 
   *
   */

  void loadBaseHash() throws RemoteException
  {
    Base base;
    Vector typesV;

    /* -- */

    typesV = session.getTypes();

    if (baseHash != null)
      {
	baseHash.clear();
      }
    else
      {
	baseHash = new Hashtable(typesV.size());
      }
    
    for (int i = 0; i < typesV.size(); i++)
      {
	base = (Base) typesV.elementAt(i);
	baseHash.put(base, base.getFields());
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
    Base base;
    BaseNode t;

    /* -- */

    treeNode typesnode = new treeNode(null,"Objects",null,true,0,1);
    tree.setRoot(typesnode);

    Enumeration enum = baseHash.keys();

    while (enum.hasMoreElements())
      {
	base = (Base) enum.nextElement();
	t = new BaseNode(typesnode, base.getName(), base, null,true,0,1, pMenu);
	tree.insertNode(t,false);
	  
	refreshObjects(t, false);
      } 
    if (debug)
      {
	System.out.println("Done building tree,");
      }
  }

  /**
   *
   * This method fully updates the tree, refreshing the object
   * subtree of each base in the tree.
   *
   */

  void refreshTree()
  {
    treeNode root = tree.getRoot();
    BaseNode child = (BaseNode)root.getChild();

    while (child != null)
      {
	try
	  {
	    refreshObjects(child, false);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not refresh tree: " + rx);
	  }
	child = (BaseNode)child.getNextSibling();
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
    db_object object;
    Vector vect;
    BaseNode parentNode;
    ObjectNode oldNode, newNode, fNode;
    int i;
    Result sorted_results[] = null;
    Query _query = null;

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
	    Vector unsorted_objects =  session.query(_query);
	    //System.out.println("There are " + unsorted_objects.size() + " objects in the query");

	    if (unsorted_objects.size() > 0)
	      {
		sorted_results = new Result[unsorted_objects.size()];
	      }
	    else
	      {
		sorted_results = null;
	      }

	    if ((unsorted_objects == null) || (sorted_results == null))
	      {
		System.out.println("unsorted_objects or sorted_results == null");
	      }
	    else
	      {
		for (int j = 0; j < unsorted_objects.size() ; j++)
		  {
		    //System.out.println("Adding " + (Result)unsorted_objects.elementAt(j));
		    sorted_results[j] = (Result)unsorted_objects.elementAt(j);
		  }

		System.out.println("sorted_results.length == " + sorted_results.length);
		
		(new QuickSort(sorted_results, 
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
    fNode = (ObjectNode) node.getChild();
    i = 0;
	
    while ((sorted_results != null) && ((i < sorted_results.length) || (fNode != null)))
      {
	//System.out.println("Looking at the next node");
	//System.out.println("i = " + i + " length = " + sorted_results.length);
	
	if (i < sorted_results.length)
	  {
	    object = sorted_results[i].getObject();
	    //System.out.println("Dealing with " + object.getLabel());
	  }
	else
	  {
	    //System.out.println("Object is null");
	    object = null;
	  }

	if ((fNode == null) ||
	    ((object != null) && 
	     ((object.getLabel().compareTo(fNode.getObject().getLabel())) < 0)))
	  {
	    // insert a new object node
	    //System.out.println("Adding this node");
	    //newNode = new ObjectNode(parentNode, object.getName(), object,
	    //			    oldNode, false, 2, 2, objectMenu);
	    ObjectNode objNode = new ObjectNode(node, 
						object.getLabel(),
						object,
						oldNode, false, 2,2, objectPM);
	    
	    tree.insertNode(objNode, false);
	    
	    oldNode = objNode;
	    fNode = (ObjectNode) oldNode.getNextSibling();
	    
	    i++;
	  }
	else if ((object == null) ||
		 ((object.getLabel().compareTo(fNode.getObject().getLabel())) > 0))
	  {
	    // delete a object node
	    //System.out.println("Removing this node");
	    // System.err.println("Deleting: " + fNode.getText());
	    newNode = (ObjectNode) fNode.getNextSibling();
	    tree.deleteNode(fNode, false);

	    fNode = newNode;
	  }
	else
	  {
	    //System.out.println("No change for this node");
	    fNode.setText(object.getLabel());
	    // System.err.println("Setting: " + object.getName());

	    oldNode = fNode;
	    fNode = (ObjectNode) oldNode.getNextSibling();

	    i++;
	  }
      }

    if (doRefresh)
      {
	tree.refresh();
      }
  }

  /*
  JMenuBar createMenuBar()
    {
      JMenuBar menuBar = new JMenuBar();
      JMenuItem mi;

      JMenu file = (JMenu)menuBar.add(new JMenu("File"));
      mi = (JMenuItem)file.add(new JMenuItem("Close All Windows"));
      mi.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) 
	  {
	    wp.closeAll();
	  }

      });
      mi.add(new JSeparator());

      mi = (JMenuItem)file.add(new JMenuItem("Logout"));
      mi.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e)
	  {
	    logout();
	  }
      });

      // Now the Look and Feel menu
      JMenu LandF = (JMenu)menuBar.add(new JMenu("Look"));
      mi = (JMenuItem)file.add(new JMenuItem("Rose"));
      mi.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e)
	  {
	    setLook("com.sun.java.swing.rose.RoseFactory");
	  }
      });
      mi = (JMenuItem)file.add(new JMenuItem("Basic"));

      mi.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e)
	  {
	    setLook("com.sun.java.swing.basic.Basic.Factory");
	  }
      });
	
      return menuBar;
    }
    */
  void logout()
    {

      try
	{
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

  // ActionListener Methods
  
  public void actionPerformed(java.awt.event.ActionEvent event)
  {
    if (event.getSource() == cancel)
      {
	System.err.println("cancel button clicked");

	try
	  {
	    wp.closeEditables();
	    session.abortTransaction();
	    somethingChanged = false;
	    session.openTransaction();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not abort transaction" + rx);
	  }
      }
    else if (event.getSource() == commit)
      {
	System.out.println("commit button clicked");

	try
	  {
	    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    wp.closeEditables();
	    somethingChanged = false;
	    session.commitTransaction();
	    wp.refreshTableWindows();
	    session.openTransaction();

	    refreshTree();
	    System.out.println("Done committing");
	    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not commit transaction" + rx);
	  }
      }
    else if (event.getSource() == menubarQueryMI)
      {
	querybox box = new querybox(baseHash, this, "Query Panel");
	box.myshow(true);
      }
    else if (event.getSource() == removeAllMI)
      {
	if (OKToProceed())
	  {
	    wp.closeAll();
	  }
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
	try
	  {
	    setStatus("Switching to Rose look and feel");
	    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    UIManager.setUIFactory("com.sun.java.swing.rose.RoseFactory", this);
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
	  
      }
    else if (event.getSource() == win95MI)
      {
	try
	  {
	    setStatus("Switching to win95 look and feel");
	    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    UIManager.setUIFactory("com.sun.java.swing.basic.BasicFactory", this);
	    this.invalidate();
	    this.validate();
	    this.repaint();
	    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    setStatus("Done");
	  }
	catch (ClassNotFoundException ex)
	  {
	    System.out.println("Could not load basic factory: " + ex);
	  }
      }
    else
      {
	System.err.println("Unknown action event generated");
      }
  }
  

  // treeCallback methods

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
    
    if (event.getSource() == createMI)
      {
	System.out.println("createMI");

	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;
	  
	    try
	      {
		wp.addWindow(session.create_db_object(baseN.getBase().getTypeID()), true);
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
		Query _query = new Query(baseN.getBase().getTypeID());
		Vector results =  session.query(_query);

		if (results == null)
		  {
		    System.out.println("results == null");
		  }
		else
		  {
		    wp.addTableWindow(session, baseN.getQuery(), results, "Query Results");
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
    else if (event.getSource() ==  queryMI)
      {
	System.out.println("queryMI");

	if (node instanceof BaseNode)
	  {
	    Base base = ((BaseNode) node).getBase();

	    querybox box = new querybox(base, baseHash, this, "Query Panel");
	    box.myshow(true);
	  }
      }
    else if (event.getSource() == objViewMI)
      {
	if (node instanceof ObjectNode)
	  {
	    ObjectNode objectN = (ObjectNode)node;
	  
	    try
	      {
		wp.addWindow(session.view_db_object(objectN.getObject().getInvid()), false);
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

	if (node instanceof ObjectNode)
	  {
	    ObjectNode objectN = (ObjectNode)node;
	  
	    try
	      {		
		System.out.println("edit invid= " + objectN.getObject().getInvid());
		wp.addWindow(session.edit_db_object(objectN.getObject().getInvid()), true);
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
	if (node instanceof ObjectNode)
	  {
	    ObjectNode objectN = (ObjectNode)node;

	    try
	      {
		System.out.println("Deleting invid= " + objectN.getObject().getInvid());
		session.remove_db_object(objectN.getObject().getInvid());
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
                                                       Class ObjectNode
---------------------------------------------------------------------*/

class ObjectNode extends arlut.csd.Tree.treeNode {

  private db_object object;

  public ObjectNode(treeNode parent, String text, db_object object, treeNode insertAfter,
		    boolean expandable, int openImage, int closedImage, PopupMenu menu)
    {
      super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
      this.object = object;
    }

  public db_object getObject()
    {
      return object;
    }

  // Can't think of why you would ever want this
  public void setObject(db_object object)
    {
      this.object = object;
    }
    
}



/*------------------------------------------------------------------------------
                                                                           Class
                                                                        BaseNode

v------------------------------------------------------------------------------*/

class BaseNode extends arlut.csd.Tree.treeNode {

  private Base base;
  private Query query;

  /* -- */

  BaseNode(treeNode parent, String text, Base base, treeNode insertAfter,
	   boolean expandable, int openImage, int closedImage, PopupMenu menu)
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

}


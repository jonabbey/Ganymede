/*
   gclient.java

   Ganymede client main module

   --

   Created: 24 Feb 1997
   Version: $Revision: 1.5 $ %D%
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

import arlut.csd.DataComponent.*;
import arlut.csd.ganymede.*;
import arlut.csd.ganymede.client.*;
import arlut.csd.Tree.*;
import arlut.csd.Dialog.*;
import arlut.csd.Util.*;

import com.sun.java.swing.JButton;

public class gclient extends Frame implements treeCallback,ActionListener {

  Session session;
  glogin _myglogin;

  //containerPanel _cPanel;
  //Panel centerPanel;

  Image images[];

  JButton commit;
  JButton cancel;
  
  treeControl tree;

  windowPanel
    wp;

  PopupMenu objectPM;
  MenuItem
    objViewMI,
    objEditMI,
    objInactivateMI;

  PopupMenu 
    pMenu = new PopupMenu();
  
  MenuItem 
    createMI = null,
    editMI = null,
    viewMI = null,
    inactivateMI = null,
    queryMI = null;

  MenuBar 
    menubar;

  MenuItem 
    logoutMI,
    removeAllMI;
  Menu 
    windowMenu,
    fileMenu;

  public gclient(Session s,glogin g) {

    super("Ganymede Client: "+g.my_client.getName()+" logged in");

    if (s == null)
      throw new IllegalArgumentException("Ganymede Error: Parameter for Session s is null");;

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
    fileMenu.add(removeAllMI);
    fileMenu.addSeparator();
    fileMenu.add(logoutMI);

    // Window list menu
    windowMenu = new Menu("Windows");
    
    menubar.add(fileMenu);
    menubar.add(windowMenu);
    this.setMenuBar(menubar);
    
    //centerPanel.propagateInvalidate();

    createMI = new MenuItem("Create");
    editMI = new MenuItem("Edit");
    viewMI = new MenuItem("View");
    inactivateMI = new MenuItem("Inactivate");
    queryMI = new MenuItem("Query");
    

    pMenu.add(viewMI);
    pMenu.add(createMI);
    pMenu.add(editMI);
    pMenu.add(inactivateMI);
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
    objInactivateMI = new MenuItem("Inactivate Object");
    objectPM.add(objViewMI);
    objectPM.add(objEditMI);
    objectPM.add(objInactivateMI);

    // Build the tree
    /*
    try
      {
	Vector  typesV = session.getTypes();
	treeNode typesnode = new treeNode(null,"Objects",null,true,0,1);
	tree.setRoot(typesnode);
	
	for (int i=0;i<typesV.size();i++)
	  {
	    Base tempBase = null;
	    BaseNode t;
	    try 
	      {
		tempBase = (Base)typesV.elementAt(i);
		t = new BaseNode(typesnode,tempBase.getName(), tempBase, null,true,0,1, pMenu);
		tree.insertNode(t,false);
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("Could not get bases: " + rx);
	      }
	    
   

	    Query _query;
	    try
	      {
	        //Now get all the children
	        _query = new Query(tempBase.getTypeID());
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
		    Vector objects =  session.query(_query);
		    if (objects == null)
		      {
			System.out.println("objects == null");
		      }
		    else
		      {
			for (int j=0 ; j < objects.size(); j++)
			  {
			    ObjectNode objNode = new ObjectNode(t, 
								((Result)objects.elementAt(j)).toString(), 
								((Result)objects.elementAt(j)).getObject(),
							    null, false, 2,2, objectPM);
			    tree.insertNode(objNode, false); 
			  }
		      }
		  }
	      }
	    catch (RemoteException ex) 
	      {
		throw new IllegalArgumentException("Could not build object part of tree: " + ex);
	      }
      

	  }
      }
    catch (RemoteException ex) 
      {
	throw new IllegalArgumentException("Could not build whole tree: " + ex);
      }
      */
    
    refreshTree();

    // The right panel which will contain the containerPanel

    Panel rightP = new Panel();

    rightP.setLayout(new BorderLayout());
      
    //    ScrollPane _scroll = new ScrollPane();
    //    _scroll.setLayout(new BorderLayout());
    wp = new windowPanel(windowMenu);
    
    //_scroll.add(wp);

    //rightP.add(_scroll,"Center");
    rightP.add("Center", wp);

    Panel bottomButtonP = new Panel();
    rightP.add(bottomButtonP,"South");
    bottomButtonP.setLayout(new RowLayout());

    commit = new JButton("Commit");
    cancel = new JButton("Cancel");
    commit.addActionListener(this);
    cancel.addActionListener(this);

    bottomButtonP.add(commit);
    bottomButtonP.add(cancel);


   
    add("Center",rightP);
    /*
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setSize( d.width / 2, d.height / 2 );
    */


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

  void refreshTree()
    {
      
      try
	{
	  Vector  typesV = session.getTypes();
	  treeNode typesnode = new treeNode(null,"Objects",null,true,0,1);
	  tree.setRoot(typesnode);
	  
	  for (int i=0;i<typesV.size();i++)
	    {
	      Base tempBase = null;
	      BaseNode t;
	      try 
		{
		  tempBase = (Base)typesV.elementAt(i);
		  t = new BaseNode(typesnode,tempBase.getName(), tempBase, null,true,0,1, pMenu);
		  tree.insertNode(t,false);
		}
	      catch (RemoteException rx)
		{
		  throw new IllegalArgumentException("Could not get bases: " + rx);
		}
	      refreshObjects(t, false);
	    }
	}
      catch (RemoteException ex) 
	{
	  throw new IllegalArgumentException("Could not build whole tree: " + ex);
	}
    }
  
  void refreshObjects(BaseNode node, boolean doRefresh) throws RemoteException
  {
    Base base;
    db_object object;
    Vector vect;
    BaseNode parentNode;
    ObjectNode oldNode, newNode, fNode;
    int i;
    Result sorted_results[] = null;


    /* -- */

    base = node.getBase();


  
    
    Query _query = null;
    try
      {
	//Now get all the children
	_query = new Query(base.getTypeID());
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
	    System.out.println("There are " + unsorted_objects.size() + " objects in the query");
	    if (unsorted_objects.size() > 0)
	      {
		sorted_results = new Result[unsorted_objects.size()];
	      }
	    else
	      {
		sorted_results = null;
	      }
	    if (unsorted_objects == null)
	      {
		System.out.println("unsorted_objects == null");
	      }
	    else
	      {
		for (int j = 0; j < unsorted_objects.size() ; j++)
		  {
		    System.out.println("Adding " + (Result)unsorted_objects.elementAt(j));
		    sorted_results[j] = (Result)unsorted_objects.elementAt(j);
		  }
		
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
	System.out.println("");
	System.out.println("Looking at the next node");
	System.out.println("i = " + i + " length = " + sorted_results.length);
	if (fNode == null)
	  {
	    System.out.println("fNode is null");
	  }
	else
	  {
	    System.out.println("fNode = " +fNode.getObject().getLabel());
	  }
	
	if (i < sorted_results.length)
	  {
	    object = sorted_results[i].getObject();
	    System.out.println("Dealing with " + object.getLabel());
	  }
	else
	  {
	    System.out.println("Object is null");
	    object = null;
	  }

	if ((fNode == null) ||
	    ((object != null) && 
	     ((object.getLabel().compareTo(fNode.getObject().getLabel())) < 0)))
	  {
	    // insert a new object node
	    System.out.println("Adding this node");
	    //newNode = new ObjectNode(parentNode, object.getName(), object,
	    //			    oldNode, false, 2, 2, objectMenu);
	    ObjectNode objNode = new ObjectNode(node, 
						object.getLabel(),
						object,
						oldNode, false, 2,2, objectPM);
	    
	    tree.insertNode(objNode, false);
	    
	    oldNode = objNode;
	    fNode = (ObjectNode) oldNode.getNextSibling();
	    if (fNode != null)
	      System.out.println("setting fNode to " + fNode.getObject().getLabel());

	    i++;
	  }
	else if ((object == null) ||
		 ((object.getLabel().compareTo(fNode.getObject().getLabel())) > 0))
	  {
	    // delete a object node
	    System.out.println("Removing this node");
	    // System.err.println("Deleting: " + fNode.getText());
	    newNode = (ObjectNode) fNode.getNextSibling();
	    tree.deleteNode(fNode, false);

	    fNode = newNode;
	  }
	else
	  {
	    System.out.println("No change for this node");
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
	      wp.closeEditables();
	      session.commitTransaction();
	      session.openTransaction();

	      tree.refresh();
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not commit transaction" + rx);
	    }
	}
      else if (event.getSource() == removeAllMI)
	{
	  wp.closeAll();
	}
      else if (event.getSource() == logoutMI)
	{
	  try
	    {
	      session.abortTransaction();
	      session.logout();
	      this.dispose();
	      _myglogin.connector.setEnabled(true);
	      _myglogin._quitButton.setEnabled(true);
	    }
	  catch (RemoteException rx)
	    {
	      throw new IllegalArgumentException("could not logout: " + rx);
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
    else if (event.getSource() == editMI)
      {
	System.out.println("editMI");
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
		    
		    wp.addTableWindow(session, results, "Query Results");
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
    else if (event.getSource() ==  inactivateMI)
      {
	System.out.println("inactivateMI");	
      }
    else if (event.getSource() ==  queryMI)
      {
	System.out.println("queryMI");	
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
    else if (event.getSource() ==    objEditMI)
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

  public void setObject(db_object object)
    {
      this.object = object;
    }
    
}



/*------------------------------------------------------------------------------
                                                                           class
                                                                        BaseNode

v------------------------------------------------------------------------------*/

class BaseNode extends arlut.csd.Tree.treeNode {

  private Base base;

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
}


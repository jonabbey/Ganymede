/*
   gclient.java

   Ganymede client main module

   --

   Created: 24 Feb 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Navin Manohar
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

import com.sun.java.swing.JButton;

public class gclient extends Frame implements treeCallback,ActionListener {

  Session _mySession;
  glogin _myglogin;

  //containerPanel _cPanel;
  //Panel centerPanel;

  Image images[];

  JButton commit;
  JButton cancel;
  
  treeControl tree;
  
  PopupMenu pMenu = new PopupMenu();
  MenuItem createMI = null;
  MenuItem editMI = null;
  MenuItem viewMI = null;
  MenuItem inactivateMI = null;
  MenuItem queryMI = null;

  MenuBar menubar;
  MenuItem logoutMI;
  Menu fileMenu;

  public gclient(Session s,glogin g) {

    super("Ganymede Client: "+g.my_client.getName()+" logged in");

    if (s == null)
      throw new IllegalArgumentException("Ganymede Error: Parameter for Session s is null");;

    _mySession = s;
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
    fileMenu.add(logoutMI);
    
    menubar.add(fileMenu);
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
			   pMenu);


    Box leftBox = new Box(tree, "Objects");

    InsetPanel leftP = new InsetPanel();
    leftP.setLayout(new BorderLayout());
    leftP.add("Center", leftBox);
    
    add("West", leftP);

    // Build the tree

    try
      {
	Vector  typesV = _myglogin.my_session.getTypes();
	treeNode typesnode = new treeNode(null,"Objects",null,true,0,1);
	tree.setRoot(typesnode);
	
	for (int i=0;i<typesV.size();i++)
	  {
	    Base tempBase = null;
	    treeNode t;
	    try 
	      {
		tempBase = (Base)typesV.elementAt(i);
		t = new treeNode(typesnode,tempBase.getName(),null,true,0,1);
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
		    Vector objects =  _myglogin.my_session.query(_query);
		    if (objects == null)
		      {
			System.out.println("objects == null");
		      }
		    else
		      {
			for (int j=0 ; j < objects.size(); j++)
			  {
			    treeNode objNode = new treeNode(t, 
							    ((Result)objects.elementAt(j)).toString(), 
							    null, false, 2,2);
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


    // The right panel which will contain the containerPanel

    Panel rightP = new Panel();

    rightP.setLayout(new BorderLayout());
      
    //    ScrollPane _scroll = new ScrollPane();
    //    _scroll.setLayout(new BorderLayout());
    windowPanel wp = new windowPanel();
    wp.makeNewWindow("New Window from the client");
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
    pack();
    setSize(800, 600);
    show();


  }

  // ActionListener Methods

  
  public void actionPerformed(java.awt.event.ActionEvent event)
    {
      if (event.getSource() == cancel)
	{
	  System.err.println("cancel button clicked");
	}
      else if (event.getSource() == commit)
	{
	  System.out.println("commit button clicked");
	}
      else if (event.getSource() == logoutMI)
	{
	  try
	    {
	      _myglogin.my_session.logout();
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
	try
	  {
	    _myglogin.my_session.openTransaction();
	    _myglogin.my_session.create_db_object((short)1);
	    _myglogin.my_session.commitTransaction();


	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not create object: " + rx);
	  }
      }
    else if (event.getSource() == editMI)
      {
	System.out.println("editMI");
      }
    else if (event.getSource() ==  viewMI)
      {
	System.out.println("viewMI");
      }
    else if (event.getSource() ==  inactivateMI)
      {
	System.out.println("inactivateMI");	
      }
    else if (event.getSource() ==  queryMI)
      {
	System.out.println("queryMI");	
      }

  }


  public void start() throws Exception {

  }
}

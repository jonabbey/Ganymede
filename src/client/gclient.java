/*
   gclient.java

   Ganymede client main module

   --

   Created: 24 Feb 1997
   Version: $Revision: 1.1 $ %D%
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

import gjt.ImageCanvas;
import gjt.Util;
import arlut.csd.DataComponent.*;
import arlut.csd.ganymede.*;
import arlut.csd.Tree.*;
import edu.nyu.cs.buff.common.*;

public class gclient extends Frame implements treeCallback,ActionListener {

  Session _mySession;
  glogin _myglogin;

  //containerPanel _cPanel;
  MosaicPanel centerPanel;

  Image images[];

  Button _commit;
  Button _cancel;
  
  treeControl tree;
  
  PopupMenu pMenu = new PopupMenu();
  MenuItem createMI = null;
  MenuItem editMI = null;
  MenuItem viewMI = null;
  MenuItem inactivateMI = null;
  MenuItem queryMI = null;


  public gclient(Session s,glogin g) {

    super("Ganymede Client: "+g.my_client.getName()+" logged in");

    if (s == null)
      throw new IllegalArgumentException("Ganymede Error: Parameter for Session s is null");;

    _mySession = s;
    _myglogin = g;


    setLayout(new BorderLayout());


    centerPanel = new MosaicPanel();
    add("Center",centerPanel);

    MosaicLayout layout = new MosaicLayout();
    centerPanel.setLayout( layout );
    
    centerPanel.propagateInvalidate();


    layout.setConstraints( "Horz", new MosaicConstraints(MosaicConstraints.CENTER, MosaicConstraints.HORIZONTAL, 1 ) );
    
    layout.setConstraints( "Vert", new MosaicConstraints(MosaicConstraints.CENTER, MosaicConstraints.VERTICAL, 1 ) );
    
    layout.setConstraints( "Sticky", new MosaicConstraints(MosaicConstraints.CENTER, MosaicConstraints.BOTH, 1 ) );


    // The left panel

    Panel _leftP = new Panel();

    _leftP.setLayout(new BorderLayout());

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

    try
      {
	images[0] = Toolkit.getDefaultToolkit().getImage(new URL("http://www.arlut.utexas.edu/~broccol/gash2/openfolder.gif"));
	Util.waitForImage(this, images[0]);

	//	System.err.println("image 0 width " + images[0].getWidth(this));

	images[1] = Toolkit.getDefaultToolkit().getImage(new URL("http://www.arlut.utexas.edu/~broccol/gash2/folder.gif"));
	Util.waitForImage(this, images[1]);

	//	System.err.println("image 1 width " + images[1].getWidth(this));

	images[2] = Toolkit.getDefaultToolkit().getImage(new URL("http://www.arlut.utexas.edu/~broccol/gash2/list.gif"));
	Util.waitForImage(this, images[2]);

	//	System.err.println("image 2 width " + images[2].getWidth(this));
      }
    catch (MalformedURLException e)
      {
	System.err.println("Bad URL");
      }

    tree = new treeControl(new Font("SansSerif", Font.BOLD, 12),
			 Color.black, Color.white, this, images,
			 pMenu);



    // 

// Commented out for now since the session no longer gives Type objects.
// This code will need to be modified so that it works with the DBStore 
/*
    try {
            Type typeList[] = _myglogin.my_session.types();
	    treeNode typesnode = new treeNode(null,"Objects",null,true,0,1);
	    tree.setRoot(typesnode);
	    
	    for (int i=0;i<typeList.length;i++)
	      {
		treeNode t = new treeNode(typesnode,typeList[i].name(),null,true,0,1);
		tree.insertNode(t,false);
		
	      }
    }
    catch (RemoteException ex) {}
*/	  

    // Just to have something in the tree
    treeNode typesnode = new treeNode(null,"Objects",null,true,0,1);
    tree.setRoot(typesnode);

    // The right panel which will contain the containerPanel

    Panel _rightP = new Panel();

    _rightP.setLayout(new BorderLayout());
      
    Panel _bottomP = new Panel();
    Label test1 = new Label("_bottomP");
    _bottomP.add(test1);

    _rightP.add(_bottomP,"South");


    ScrollPane _scroll = new ScrollPane();
    Label test2 = new Label("_scroll");
    _scroll.add(test2);

    _rightP.add(_scroll,"Center");
    

    _bottomP.setLayout(new BorderLayout());

    _commit = new Button("Commit");
    _cancel = new Button("Cancel");
    

    _bottomP.add(_commit,"Center");
    _bottomP.add(_cancel,"East");

    layout.setPos( 0, 0, 0, 0 );

    centerPanel.add("Sticky",tree);
   
    layout.setPos( 0, 0, 1, 0, 0, 0 ); 

    centerPanel.add("Sticky",_rightP);
    
    /*    layout.setPos(0,0,1,0,1,0);

    centerPanel.add("",new Button("Testing 123"));

    layout.setPos(0,0,1,0,1,1);

    centerPanel.add("",new Button("Testing 456"));
    */
    

    //    pack();
    
    
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setSize( d.width / 2, d.height / 2 );

    show();

  }

  // ActionListener Methods

  
  public void actionPerformed(java.awt.event.ActionEvent event)
    {
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
    tree.deleteNode(node, true);
  }


  public void start() throws Exception {

  }
}

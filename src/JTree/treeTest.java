/*

  treeTest.java

  test applet for the arlut.csd.Tree.treeCanvas component.

  Copyright (C) 1997  The University of Texas at Austin.

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

  Created: 4 March 1997
  Version: $Revision: 1.1 $ %D%
  Module By: Jonathan Abbey              jonabbey@arlut.utexas.edu
  Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JTree;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.applet.*;
import java.util.*;

import gjt.Util;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   treeTestFrame

------------------------------------------------------------------------------*/

class treeTestFrame extends Frame implements treeCallback {

  Image
    images[];

  treeMenu popMenu = null;
  MenuItem m1 = null;

  treeControl tc;

  public treeTestFrame(String title)
  {
    super(title);
    addNotify();

    popMenu = new treeMenu();
    m1 = new MenuItem("Delete node");

    popMenu.add(m1);

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

    tc = new treeControl(new Font("SansSerif", Font.BOLD, 12),
			 Color.black, Color.white, this, images,
			 popMenu);

    treeNode TypesNode = new treeNode(null, "Types", null, true, 0, 1);
    tc.setRoot(TypesNode);
    
    treeNode UsersNode = new treeNode(TypesNode, "Users", null, false, 2, 2);
    tc.insertNode(UsersNode, false);

    treeNode PersonsNode = new treeNode(TypesNode, "Persons", UsersNode, false, 2, 2);
    tc.insertNode(PersonsNode, false);

    treeNode SystemsNode = new treeNode(TypesNode, "Systems", PersonsNode, true, 0, 1);
    tc.insertNode(SystemsNode, false);

    treeNode IPNode = new treeNode(SystemsNode, "IP Field", null, false, 2, 2);
    tc.insertNode(IPNode, false);

    treeNode EtherNode = new treeNode(SystemsNode, "Ethernet Field", IPNode, false, 2, 2);
    tc.insertNode(EtherNode, false);

    treeNode ACLNode = new treeNode(null, "Access Control", TypesNode, true, 0, 1);
    tc.insertNode(ACLNode, false);

    treeNode ANode = new treeNode(ACLNode, "control list", null, false, 2, 2);
    tc.insertNode(ANode, false);

    for (int i = 0; i < 500; i++)
      {
	tc.insertNode(new treeNode(ACLNode, "# " + i, null, false, 2, 2), false);
      }

    add(tc);

    pack();
    show();
  }

  // treeCallback methods

  public void treeNodeSelected(treeNode node)
  {
    System.out.println("node " + node.getText() + " selected");
  }

  public void treeNodeUnSelected(treeNode node, boolean otherNodeSelected)
  {
    System.out.println("node " + node.getText() + " unselected");
  }

  public void treeNodeMenuPerformed(treeNode node,
				    java.awt.event.ActionEvent event)
  {
    tc.deleteNode(node, true);
  }

  public void treeNodeExpanded(treeNode node)
  {
    return;
  }

  public void treeNodeContracted(treeNode node)
  {
    return;
  }
}

/*------------------------------------------------------------------------------
                                                                          class
                                                                        treeTest

------------------------------------------------------------------------------*/
public class treeTest extends Applet implements treeCallback {

  static treeTestFrame frame = null;

  treeMenu popMenu = null;
  MenuItem m1 = null;

  treeControl tc;

  /* -- */

  // Our primary constructor.  This will always be called, either
  // from main(), below, or by the environment building our applet.

  public treeTest() 
  {

  }
  
  public void init() 
  {
    Image
      images[];
    
    /* -- */

    popMenu = new treeMenu();
    m1 = new MenuItem("Delete node");

    popMenu.add(m1);

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

    tc = new treeControl(new Font("SansSerif", Font.BOLD, 12),
			 Color.black, Color.white, this, images,
			 popMenu);

    treeNode TypesNode = new treeNode(null, "Types", null, true, 0, 1);
    tc.setRoot(TypesNode);
    
    treeNode UsersNode = new treeNode(TypesNode, "Users", null, false, 2, 2);
    tc.insertNode(UsersNode, false);

    treeNode PersonsNode = new treeNode(TypesNode, "Persons", UsersNode, false, 2, 2);
    tc.insertNode(PersonsNode, false);

    treeNode SystemsNode = new treeNode(TypesNode, "Systems", PersonsNode, true, 0, 1);
    tc.insertNode(SystemsNode, false);

    treeNode IPNode = new treeNode(SystemsNode, "IP Field", null, false, 2, 2);
    tc.insertNode(IPNode, false);

    treeNode EtherNode = new treeNode(SystemsNode, "Ethernet Field", IPNode, false, 2, 2);
    tc.insertNode(EtherNode, false);

    treeNode ACLNode = new treeNode(null, "Access Control", TypesNode, true, 0, 1);
    tc.insertNode(ACLNode, false);

    treeNode ANode = new treeNode(ACLNode, "control list", null, false, 2, 2);
    tc.insertNode(ANode, false);

    for (int i = 0; i < 500; i++)
      {
	tc.insertNode(new treeNode(ACLNode, "# " + i, null, false, 2, 2), false);
      }

    setLayout(new BorderLayout());

    add("Center",tc);

  }

  public static void main(String[] argv)
  {
    frame = new treeTestFrame("treeCanvas test");
  }

  // treeCallback methods

  public void treeNodeSelected(treeNode node)
  {
    System.out.println("node " + node.getText() + " selected");
  }

  public void treeNodeUnSelected(treeNode node, boolean otherNodeSelected)
  {
    System.out.println("node " + node.getText() + " unselected");
  }

  public void treeNodeMenuPerformed(treeNode node,
				    java.awt.event.ActionEvent event)
  {
    tc.deleteNode(node, true);
  }

  public void treeNodeExpanded(treeNode node)
  {
    return;
  }

  public void treeNodeContracted(treeNode node)
  {
    return;
  }
}
 

/*

   treeMenu.java

   This class is intended to serve as a subclass of popupMenu
   that keeps track of whether it has had its listener set
   by the tree.
   
   Created: 5 September 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JTree;

import java.awt.*;
import java.awt.event.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        treeMenu

------------------------------------------------------------------------------*/

public class treeMenu extends PopupMenu {

  boolean registered = false;

  /* -- */

  public treeMenu()
  {
    super();
  }

  public treeMenu(String label)
  {
    super(label);
  }

  public void registerItems(treeControl listener)
  {
    MenuItem temp;

    /* -- */

    if (registered)
      {
	return;
      }

    for (int i = 0; i < getItemCount(); i++)
      {
	temp = this.getItem(i);
	temp.addActionListener((ActionListener)listener);
      }

    registered = true;
  }
}

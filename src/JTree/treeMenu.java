/*

   treeMenu.java

   This class is intended to serve as a subclass of popupMenu
   that keeps track of whether it has had its listener set
   by the tree.
   
   Created: 5 September 1997
   Version: $Revision: 1.2 $ %D%
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

  /**
   * This method registers all menu items in this menu with the
   * appropriate listener.  If this menu has already been registered
   * with anything, it won't add items to the listener.<p>
   *
   * This means that a treeMenu cannot be used in multiple trees
   * simultaneously, as written.<p>
   *
   * @return a boolean, true if this menu is being registered
   * for the first time.
   * 
   */

  public boolean registerItems(treeControl listener)
  {
    MenuItem temp;

    /* -- */

    if (registered)
      {
	return false;
      }

    for (int i = 0; i < getItemCount(); i++)
      {
	temp = this.getItem(i);
	temp.addActionListener((ActionListener)listener);
      }

    registered = true;

    return true;
  }
}

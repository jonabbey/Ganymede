/*

   CategoryNode.java

   This interface provides support for an object to be managed
   in the server's objectbase category hierarchy.
   
   Created: 12 August 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    CategoryNode

------------------------------------------------------------------------------*/

/**
 *
 * This interface provides support for an object to be managed
 * in the server's objectbase category hierarchy.
 *
 */

public interface CategoryNode extends Remote {

  /**
   *
   * Returns the display order of this Base within the containing
   * category.
   *
   */

  public int getDisplayOrder() throws RemoteException;

  /**
   *
   * Sets the display order of this Base within the containing
   * category.
   *
   */

  public void setDisplayOrder(int order) throws RemoteException;

  /**
   *
   * This method returns the category that this
   * category node belongs to.
   *
   */

  public Category getCategory() throws RemoteException;

  /**
   *
   * This method tells the CategoryNode what it's containing
   * category is.
   *
   */

  public void setCategory(Category category) throws RemoteException;

  /**
   *
   * This method returns the name of this node.
   *
   */

  public String getName() throws RemoteException;
}

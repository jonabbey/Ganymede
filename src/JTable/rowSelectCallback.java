/*

   rowSelectCallback.java

   An interface that objects can implement to allow csd.Table tables
   to report when a row is selected.

   Created: 19 June 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey -- jonabbey@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.Table;

/**
 * An interface that objects can implement to allow csd.Table tables
 * to report when a row is selected.
 *
 * @see csd.rowTable
 */

public interface rowSelectCallback {

  /**
   * Called when a row is selected in rowTable
   * 
   * @param key Hash key for the selected row
   */

  public void rowSelected(Object key);

  /**
   * Called when a row is double selected (double clicked) in rowTable
   * 
   * @param key Hash key for the selected row
   */

  public void rowDoubleSelected(Object key);
  
  /**
   * Called when a row is unselected in rowTable
   * 
   * @param key Hash key for the unselected row
   * @param endSelected false if the callback should assume that the final
   *                    state of the system due to the user's present 
   *                    action will have no row selected
   */

  public void rowUnSelected(Object key, boolean endSelected);
}
